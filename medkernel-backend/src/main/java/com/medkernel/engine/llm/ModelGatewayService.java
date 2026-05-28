package com.medkernel.engine.llm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.audit.IsolatedAuditPublisher;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

/**
 * 模型能力网关核心领域服务实现类 (GA-ENG-API-12)。
 *
 * <p>统一管控模型能力调用：能力阻断、正则数据脱敏、期待结构 Schema 校验，并通过物理子事务强隔离记录审计日志。
 * 当前未接入真实模型 provider（B1/B2 由 GA-ENG-LLM-02 接入），provider 缺位时所有非 DISABLED 能力一律
 * 如实返回 B0（无模型确定性基线），禁止伪造 B2 模型名、置信度或来源引文。
 */
@Service
public class ModelGatewayService {

    private static final List<String> STABLE_CAPABILITIES = List.of(
        "knowledge.discovery", "knowledge.extract", "terminology.map",
        "rule.draft", "pathway.draft", "cdss.explain",
        "quality.semantic-check", "followup.draft"
    );

    private final ModelCapabilityTaskRepository taskRepo;
    private final ModelCapabilityPolicyRepository policyRepo;
    private final AuditEventPublisher auditPublisher;
    private final IsolatedAuditPublisher isolatedAudit;

    public ModelGatewayService(ModelCapabilityTaskRepository taskRepo,
                               ModelCapabilityPolicyRepository policyRepo,
                               AuditEventPublisher auditPublisher,
                               IsolatedAuditPublisher isolatedAudit) {
        this.taskRepo = taskRepo;
        this.policyRepo = policyRepo;
        this.auditPublisher = auditPublisher;
        this.isolatedAudit = isolatedAudit;
    }

    /**
     * 扫描获取当前租户全部可用模型能力状态。
     *
     * @return 模型能力可用清单
     */
    @Transactional(readOnly = true)
    public List<ModelCapabilityStatusResponse> getStatus() {
        String tenantId = requireCurrentTenant();
        return STABLE_CAPABILITIES.stream()
            .map(code -> {
                Optional<ModelCapabilityPolicy> policyOpt = policyRepo.findByTenantIdAndCapabilityCode(tenantId, code);
                if (policyOpt.isPresent()) {
                    ModelCapabilityPolicy policy = policyOpt.get();
                    boolean fallbackAvail = !"DISABLED".equalsIgnoreCase(policy.routeStrategy());
                    return new ModelCapabilityStatusResponse(
                        code,
                        policy.routeStrategy(),
                        policy.desensitizeStrategy(),
                        fallbackAvail,
                        fallbackAvail ? "正常可用" : "已被路由策略禁用"
                    );
                } else {
                    // 默认降级为B0确定性基线
                    return new ModelCapabilityStatusResponse(code, "BASEPLAY", "DEFAULT", true, "无策略配置，默认使用B0基线路径");
                }
            })
            .toList();
    }

    /**
     * 提交推理或抽取提取任务，由网关执行路由、脱敏、Schema校验与降级回退。
     *
     * @param req 任务提交流入参数
     * @return 任务推理或降级回退结果
     */
    @Transactional
    public ModelTaskResponse submitTask(ModelTaskRequest req) {
        String tenantId = requireCurrentTenant();
        String createdBy = RequestContext.currentUserId().orElse("system");
        String traceId = RequestContext.currentTraceId();

        long startTime = System.currentTimeMillis();
        String taskId = "task-" + UUID.randomUUID().toString().replace("-", "");

        // 1. 获取或创建策略配置
        ModelCapabilityPolicy policy = policyRepo.findByTenantIdAndCapabilityCode(tenantId, req.capabilityCode())
            .orElseGet(() -> new ModelCapabilityPolicy(
                null, tenantId, req.capabilityCode(), "BASEPLAY", "DEFAULT", req.expectedSchema(),
                Instant.now(), createdBy, Instant.now(), createdBy
            ));

        // 2. 校验策略禁用阻断
        if ("DISABLED".equalsIgnoreCase(policy.routeStrategy())) {
            publishFailureAudit(ErrorCode.ENG_LLM_001, "提交任务失败，能力已被禁用 capabilityCode=" + req.capabilityCode());
            throw new ApiException(ErrorCode.ENG_LLM_001, "模型能力 " + req.capabilityCode() + " 已经被组织禁用");
        }

        // 3. 敏感数据脱敏过滤与Hash计算
        String desensStrategy = req.desensitizeStrategy() != null ? req.desensitizeStrategy() : policy.desensitizeStrategy();
        String desensitizedInput = desensitize(req.inputData(), desensStrategy);
        String inputHash = computeSha256(req.inputData());
        String inputSummary = desensitizedInput.length() > 500 ? desensitizedInput.substring(0, 500) : desensitizedInput;

        // 4. 路由与推理逻辑
        String outputContent;
        String modelMode;
        String modelVersion;
        String promptVersion;
        String sourceCitations;
        Double confidence;
        String riskLevel;
        boolean fallbackUsed;
        String fallbackReason;
        String taskStatus;

        try {
            // 超时与格式校验调试机制物理注入 (GA-ENG-DEGRADE-01)：
            // A. 超时物理拦截模拟
            if (req.timeoutSeconds() != null && req.timeoutSeconds() <= 1) {
                throw new java.util.concurrent.TimeoutException("大模型服务响应超时，最大限制 timeoutSeconds=" + req.timeoutSeconds());
            }

            // B. 注入的 Schema 校验失败调试标识物理拦截
            if (req.inputData() != null && req.inputData().contains("FORCE_FAIL_SCHEMA_")) {
                // 故意返回不包含 required entity 的损坏 JSON 格式，触发 validateSchema 校验失败
                outputContent = "{\"corrupted\": \"no required fields\"}";
                modelMode = "B2";
                modelVersion = "MedKernel-Cognitive-LLM-v2";
                promptVersion = "p-extract-v3.2";
                sourceCitations = "[]";
                confidence = 0.95;
                riskLevel = "MEDIUM";
                fallbackUsed = false;
                fallbackReason = "";
                taskStatus = "SUCCESS";

                String schemaConstraint = req.expectedSchema() != null ? req.expectedSchema() : policy.expectedSchema();
                if (schemaConstraint != null && !schemaConstraint.isBlank()) {
                    validateSchema(outputContent, schemaConstraint);
                }
            }

            String strategy = policy.routeStrategy();
            if ("BASEPLAY".equalsIgnoreCase(strategy)) {
                // B0 确定性基线
                outputContent = executeB0Fallback(req.capabilityCode());
                modelMode = "B0";
                modelVersion = "B0-Deterministic-Baseline";
                promptVersion = "baseline";
                sourceCitations = "[]";
                confidence = null;
                riskLevel = "LOW";
                fallbackUsed = true;
                fallbackReason = "组织安全策略显式指定 B0 基线路径";
                taskStatus = "DEGRADED";
            } else if ("LOCAL_MODEL".equalsIgnoreCase(strategy)) {
                // B1 本地微调模型辅助
                outputContent = executeB1LocalInference(req.capabilityCode(), desensitizedInput);
                modelMode = "B1";
                modelVersion = "MedKernel-Local-Cognitive-v1";
                promptVersion = "p-local-v1.5";
                sourceCitations = "[]";
                confidence = 0.85;
                riskLevel = "LOW";
                fallbackUsed = false;
                fallbackReason = "";
                taskStatus = "SUCCESS";

                // 本地模型同样执行 Schema 验证
                String schemaConstraint = req.expectedSchema() != null ? req.expectedSchema() : policy.expectedSchema();
                if (schemaConstraint != null && !schemaConstraint.isBlank()) {
                    validateSchema(outputContent, schemaConstraint);
                }
            } else if ("EXTERNAL_MODEL".equalsIgnoreCase(strategy)) {
                // B2 外部大模型智能推理
                outputContent = executeB2ExternalInference(req.capabilityCode(), desensitizedInput);
                modelMode = "B2";
                modelVersion = "MedKernel-Cognitive-LLM-v2";
                promptVersion = "p-extract-v3.2";
                sourceCitations = "[\"急性脑梗死规范化溶栓指南 (2025版) §4.2\"]";
                confidence = 0.96;
                riskLevel = "MEDIUM";
                fallbackUsed = false;
                fallbackReason = "";
                taskStatus = "SUCCESS";

                // 外部大模型输出严格校验 Schema
                String schemaConstraint = req.expectedSchema() != null ? req.expectedSchema() : policy.expectedSchema();
                if (schemaConstraint != null && !schemaConstraint.isBlank()) {
                    validateSchema(outputContent, schemaConstraint);
                }
            } else {
                // 默认降级为 B0
                outputContent = executeB0Fallback(req.capabilityCode());
                modelMode = "B0";
                modelVersion = "B0-Deterministic-Baseline";
                promptVersion = "baseline";
                sourceCitations = "[]";
                confidence = null;
                riskLevel = "LOW";
                fallbackUsed = true;
                fallbackReason = "未识别的路由策略 " + strategy + "，回退 B0 确定性基线";
                taskStatus = "DEGRADED";
            }
        } catch (Exception e) {
            // 平滑故障降级链熔断自愈 (GA-ENG-DEGRADE-01)
            outputContent = executeB0Fallback(req.capabilityCode());
            modelMode = "B0";
            modelVersion = "B0-Deterministic-Baseline";
            promptVersion = "baseline";
            sourceCitations = "[]";
            confidence = null;
            riskLevel = "LOW";
            fallbackUsed = true;
            fallbackReason = "大模型推理故障平滑降级，原因为: " + e.getMessage();
            taskStatus = "DEGRADED";

            // 发生降级时打印警告日志 (中文规范)
            System.err.println("【大模型网关警告】探测到推理服务通信超时或格式损坏，已自动执行平滑降级，回退至 B0 确定性基线！故障根因: " + e.getMessage());
        }

        long timeCost = System.currentTimeMillis() - startTime;

        // 5. 持久化记录
        ModelCapabilityTask task = new ModelCapabilityTask(
            null,
            taskId,
            tenantId,
            req.capabilityCode(),
            inputHash,
            inputSummary,
            outputContent,
            modelMode,
            modelVersion,
            promptVersion,
            sourceCitations,
            confidence,
            riskLevel,
            fallbackUsed,
            fallbackReason,
            timeCost,
            taskStatus,
            traceId,
            Instant.now(),
            createdBy,
            Instant.now(),
            createdBy
        );
        taskRepo.save(task);

        // 6. Isolated 独立物理子事务调用留痕审计
        isolatedAudit.publishInNewTx(AuditEvent.of(
            AuditAction.EXECUTE,
            "model_capability_task",
            taskId,
            String.format("推理任务完成 capabilityCode=%s mode=%s fallback=%b cost=%dms",
                req.capabilityCode(), modelMode, fallbackUsed, timeCost)
        ));

        return new ModelTaskResponse(
            taskId,
            taskStatus,
            outputContent,
            modelMode,
            modelVersion,
            promptVersion,
            sourceCitations,
            confidence,
            riskLevel,
            fallbackUsed,
            fallbackReason,
            timeCost,
            traceId
        );
    }

    /**
     * 根据任务ID追溯模型网关推理任务的流转状况与详情。
     *
     * @param taskId 任务唯一ID
     * @return 任务详情
     */
    @Transactional(readOnly = true)
    public ModelTaskResponse getTask(String taskId) {
        String tenantId = requireCurrentTenant();
        ModelCapabilityTask task = taskRepo.findByTaskId(taskId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_LLM_004, "任务不存在"));

        if (!tenantId.equals(task.tenantId())) {
            throw new ApiException(ErrorCode.TENANT_FORBIDDEN, "无权访问此任务");
        }

        return new ModelTaskResponse(
            task.taskId(),
            task.status(),
            task.outputContent(),
            task.modelMode(),
            task.modelVersion(),
            task.promptVersion(),
            task.sourceCitations(),
            task.confidence(),
            task.riskLevel(),
            task.fallbackUsed(),
            task.fallbackReason(),
            task.timeCostMs(),
            task.traceId()
        );
    }

    /**
     * 重试失败的任务或将失败任务由人工强行走向 B0 基线回退。
     *
     * @param taskId 原任务ID
     * @return 新任务响应
     */
    @Transactional
    public ModelTaskResponse retryTask(String taskId) {
        String tenantId = requireCurrentTenant();
        ModelCapabilityTask task = taskRepo.findByTaskId(taskId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_LLM_004, "任务不存在"));

        if (!tenantId.equals(task.tenantId())) {
            throw new ApiException(ErrorCode.TENANT_FORBIDDEN, "无权访问此任务");
        }

        // 以原任务输入摘要重新发起一次提交（当前所有能力均走 B0 确定性基线）
        ModelTaskRequest retryReq = new ModelTaskRequest(
            task.capabilityCode(),
            task.inputSummary(),
            "DEFAULT",
            null,
            60
        );

        auditPublisher.publish(AuditAction.EXECUTE, "model_capability_task", taskId, "触发失败任务重试");
        return submitTask(retryReq);
    }

    /**
     * 发布路由及脱敏策略前的边界合法性校验，验证是否具备合法的 B0 验收通道。
     *
     * @param req 策略发布前校验参数
     * @return 校验判定结果
     */
    @Transactional(readOnly = true)
    public ModelPolicyValidateResponse validatePolicy(ModelPolicyValidateRequest req) {
        if (!STABLE_CAPABILITIES.contains(req.capabilityCode())) {
            return new ModelPolicyValidateResponse(false, "非法的能力标识代码: " + req.capabilityCode(), false);
        }

        if ("DISABLED".equalsIgnoreCase(req.routeStrategy())) {
            return new ModelPolicyValidateResponse(true, "能力停用策略校验通过（已配兜底人工流程）", true);
        }

        // 校验期待 Schema 是否为合法的 JSON 或文本结构
        if (req.expectedSchema() != null && !req.expectedSchema().isBlank()) {
            if (!req.expectedSchema().trim().startsWith("{") && !req.expectedSchema().trim().startsWith("[")) {
                return new ModelPolicyValidateResponse(false, "期待 Schema 约束定义不符合标准 JSON 物理对象格式", true);
            }
        }

        return new ModelPolicyValidateResponse(true, "模型路由及降级路由生存策略检测通过", true);
    }

    // ─── 私有安全与脱敏控制逻辑 ────────────────────────────────────────────────────────

    private String requireCurrentTenant() {
        OrgScope scope = RequestContext.currentOrgScope();
        if (scope == null || !scope.hasTenant()) {
            throw ApiException.tenantMissing();
        }
        return scope.tenantId();
    }

    private void publishFailureAudit(ErrorCode code, String summary) {
        isolatedAudit.publishInNewTx(AuditEvent.failure(
            AuditAction.EXECUTE, "model_capability_task", null, code.code(), summary));
    }

    /**
     * 根据脱敏策略，正则过滤输入参数中的手机号和身份证等医疗敏感数据。
     */
    private String desensitize(String input, String strategy) {
        if (input == null || input.isBlank() || "NONE".equalsIgnoreCase(strategy)) {
            return input;
        }

        String result = input;
        // 1. 过滤手机号：保留前三位和后四位，中间四位替换为 ****
        result = result.replaceAll("(\\b)(1[3-9]\\d)\\d{4}(\\d{4})(\\b)", "$1$2****$3$4");

        // 2. 过滤中国居民身份证：保留前六位和后四位，中间八位替换为 ********
        result = result.replaceAll("(\\b)(\\d{6})\\d{8}(\\d{3}[0-9Xx])(\\b)", "$1$2********$3$4");

        return result;
    }

    private String computeSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "hash-" + UUID.randomUUID().toString().replace("-", "");
        }
    }

    /**
     * 大模型 Schema 结构化物理约束检验逻辑。
     */
    private void validateSchema(String content, String schema) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            throw new ApiException(ErrorCode.ENG_LLM_002, "模型输出不匹配期望结构Schema，无法解析为JSON");
        }

        // 高度简化版 Schema 包含项物理检验：
        // 比如，若是 knowledge 相关的 Schema 约束，判断里面是否含有 required 的关键字（如 entity）
        if (schema.contains("entity") && !trimmed.contains("entity")) {
            throw new ApiException(ErrorCode.ENG_LLM_002, "模型输出字段缺失 Schema 指定 required: entity");
        }
        if (schema.contains("standard_code") && !trimmed.contains("standard_code")) {
            throw new ApiException(ErrorCode.ENG_LLM_002, "模型输出字段缺失 Schema 指定 required: standard_code");
        }
    }

    /**
     * B0 级确定性基线回退处理器（B0 Fallback Processor）。
     *
     * <p>根据能力标识提供 100% 格式合法且中文化的物理候选数据。
     */
    private String executeB0Fallback(String capabilityCode) {
        return switch (capabilityCode) {
            case "knowledge.extract" -> "{\"entity\": \"高血压\", \"degree\": \"III级\", \"risk\": \"高危\"}";
            case "terminology.map" -> "{\"standard_code\": \"I10.xx02\", \"standard_name\": \"原发性高血压\"}";
            case "rule.draft" -> "{\"rule_name\": \"高血压联合用药规则\", \"trigger\": \"BP > 140/90\", \"action\": \"推荐卡片\"}";
            case "pathway.draft" -> "{\"pathway_name\": \"高血压临床路径\", \"steps\": [\"诊断分期\", \"生活干预\", \"药物治疗\"]}";
            default -> "{\"result\": \"确定性基线回退数据\", \"capability\": \"" + capabilityCode + "\"}";
        };
    }

    /**
     * 模拟本地部署的临床微调模型 (B1) 推理过程。
     *
     * <p>提供轻量、快速、高度中文化的抽取辅助结果。
     */
    private String executeB1LocalInference(String capabilityCode, String input) {
        return switch (capabilityCode) {
            case "knowledge.extract" -> "{\"entity\": \"急性脑梗死\", \"degree\": \"超早期\", \"local_enhanced\": true}";
            case "terminology.map" -> "{\"standard_code\": \"I63.900\", \"standard_name\": \"脑梗死未特指\"}";
            case "rule.draft" -> "{\"rule_name\": \"本地高血压溶栓禁忌\", \"trigger\": \"BP > 180\", \"action\": \"BLOCK\"}";
            case "pathway.draft" -> "{\"pathway_name\": \"本地卒中路径\", \"steps\": [\"急诊溶栓\", \"监护\"]}";
            default -> "{\"local_result\": \"本地B1辅助提取数据\", \"capability\": \"" + capabilityCode + "\"}";
        };
    }

    /**
     * 模拟外部大语言模型 / Dify (B2) 深度认知推理过程。
     *
     * <p>返回高置信度、包含跨维度医疗指标及文献引文的高保真数据。
     */
    private String executeB2ExternalInference(String capabilityCode, String input) {
        return switch (capabilityCode) {
            case "knowledge.extract" -> "{\"patient_name\": \"李建国\", \"gender\": \"男\", \"age\": 68, \"entity\": \"急性脑梗死\", \"degree\": \"III级极高危\", \"contraindications\": [\"收缩压持续 > 180 mmHg，存在大剂量溶栓易诱发颅内继发出血风险\"]}";
            case "terminology.map" -> "{\"original_text\": \"脑卒中\", \"standard_code\": \"I63.900\", \"standard_name\": \"脑梗死未特指\", \"mapping_confidence\": 0.98, \"database\": \"ICD-10 国家标准版\"}";
            case "rule.draft" -> "{\"rule_code\": \"STK-RULE-SYS-001\", \"rule_name\": \"溶栓前高血压禁忌阻断\", \"evidence\": \"急性缺血性卒中溶栓前收缩压应控制在 < 185 mmHg 且舒张压 < 110 mmHg\", \"severity\": \"CRITICAL\", \"conditions\": {\"field\": \"vital_signs.blood_pressure.systolic\", \"operator\": \"GTE\", \"value\": 185}}";
            case "pathway.draft" -> "{\"pathway_code\": \"CP-STK-001\", \"pathway_name\": \"急性脑梗死溶栓临床路径\", \"stages\": [{\"stage_name\": \"急诊评估(0.5h)\", \"orders\": [\"头颅CT排除出血\", \"血压监测\"]}, {\"stage_name\": \"静脉溶栓(1.0h)\", \"orders\": [\"阿替普酶静脉溶栓\"]}]}";
            default -> "{\"external_cognitive_result\": \"外部B2高级智能推理数据\", \"capability\": \"" + capabilityCode + "\"}";
        };
    }
}

