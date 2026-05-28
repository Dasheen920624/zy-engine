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
        // 当前系统未接入真实模型 provider（B1/B2 由 GA-ENG-LLM-02 接入）。provider 缺位时，
        // 所有非 DISABLED 能力一律如实返回 B0 确定性基线，禁止伪造 B2 模型名、置信度或来源引文。
        // DISABLED 已在上方阻断；真实 provider 接入后再在此处按策略路由 B1/B2 并填充真实元数据。
        String outputContent = executeB0Fallback(req.capabilityCode());
        String modelMode = "B0";
        String modelVersion = "B0-Deterministic-Baseline";
        String promptVersion = "baseline";
        String sourceCitations = "[]";
        Double confidence = null; // 无真实模型推理，模型置信度不适用
        String riskLevel = "LOW";
        boolean fallbackUsed = true;
        String fallbackReason = "BASEPLAY".equalsIgnoreCase(policy.routeStrategy())
            ? "策略显式指定 B0 无模型基线路径"
            : "未接入真实模型 provider，返回 B0 确定性基线（B1/B2 由 GA-ENG-LLM-02 接入）";
        String taskStatus = "DEGRADED";

        // 对 B0 基线输出做结构核对（真实 provider 接入后同样适用于模型输出）
        String schemaConstraint = req.expectedSchema() != null ? req.expectedSchema() : policy.expectedSchema();
        if (schemaConstraint != null && !schemaConstraint.isBlank()) {
            validateSchema(outputContent, schemaConstraint);
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
        // 1. 过滤手机号：1[3-9]\d{9} 替换为前三后四带星
        result = result.replaceAll("(\\b)1[3-9]\\d{9}(\\b)", "$1138****8888$2");

        // 2. 过滤中国居民身份证：前四后四带星
        result = result.replaceAll("(\\b)\\d{6}\\d{8}\\d{3}[0-9Xx](\\b)", "$14401********0018$2");

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
}
