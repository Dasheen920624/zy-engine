package com.medkernel.engine.llm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final Logger log = LoggerFactory.getLogger(ModelGatewayService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /** required 字段名回退解析（兼容 "required: [entity]" 这类非标准 JSON Schema 写法）。 */
    private static final Pattern REQUIRED_LOOSE = Pattern.compile("required\"?\\s*:?\\s*\\[([^\\]]*)\\]");

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

        // 4. 路由与推理：当前未接入任何真实模型 provider（B1 本地 / B2 外部 / Dify 由 GA-ENG-LLM-02 落地）。
        //    据宪法 #9/#13，provider 缺位时一律诚实降级到 B0 确定性基线，
        //    禁止伪造 B1/B2 模型名、置信度、来源引文或患者数据。
        String strategy = policy.routeStrategy();
        String outputContent = executeB0Fallback(req.capabilityCode());
        String modelMode = "B0";
        String modelVersion = "B0-Deterministic-Baseline";
        String promptVersion = "baseline";
        String sourceCitations = "[]";
        Double confidence = null;
        String riskLevel = "LOW";
        boolean fallbackUsed = true;
        String fallbackReason = baselineReason(strategy);
        String taskStatus = "DEGRADED";

        // 结构化输出 Schema 校验：真实解析 JSON + required 字段存在性校验（GA-ENG-LLM-01）。
        // 校验对象为本次实际产出（当前恒为 B0 基线），未来接入 provider 后对模型输出复用同一校验。
        String schemaConstraint = req.expectedSchema() != null ? req.expectedSchema() : policy.expectedSchema();
        if (schemaConstraint != null && !schemaConstraint.isBlank()) {
            try {
                validateSchema(outputContent, schemaConstraint);
            } catch (ApiException schemaError) {
                log.warn("结构化输出 Schema 校验失败 capabilityCode={}：{}",
                    req.capabilityCode(), schemaError.getMessage());
                publishFailureAudit(schemaError.errorCode(),
                    "结构化输出 Schema 校验失败 capabilityCode=" + req.capabilityCode() + "：" + schemaError.getMessage());
                throw schemaError;
            }
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

        // 6. 成功留痕：成功路径走 AuditEventPublisher（AFTER_COMMIT 同事务一致性，符合 IsolatedAuditPublisher
        //    契约——isolated 仅用于失败留痕）；retryTask 亦走 AuditEventPublisher，模块内统一（LLM-M-04）。
        auditPublisher.publish(
            AuditAction.EXECUTE,
            "model_capability_task",
            taskId,
            String.format("推理任务完成 capabilityCode=%s mode=%s fallback=%b cost=%dms",
                req.capabilityCode(), modelMode, fallbackUsed, timeCost)
        );

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
     * 根据脱敏策略正则过滤输入中的患者敏感数据。
     *
     * <p>{@code DEFAULT}：手机号、身份证、银行卡、邮箱；{@code MASK_ALL}：在 DEFAULT 基础上再对
     * 「患者/姓名」标注的中文姓名与「病历号/就诊号/住院号/门诊号」标注的编号脱敏。
     * 中文正文无词边界，故不用 {@code \\b}，改用数字串前后非数字断言，避免漏脱敏或误伤普通数字。
     */
    private String desensitize(String input, String strategy) {
        if (input == null || input.isBlank() || "NONE".equalsIgnoreCase(strategy)) {
            return input;
        }

        String result = input;
        // 1. 手机号：保留前 3 后 4，中间 4 位掩码。
        result = result.replaceAll("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)", "$1****$2");
        // 2. 中国居民身份证：保留前 6 后 4，中间 8 位掩码（先于银行卡，避免 18 位被误判为卡号）。
        result = result.replaceAll("(?<!\\d)(\\d{6})\\d{8}(\\d{3}[0-9Xx])(?!\\d)", "$1********$2");
        // 3. 银行卡：16-19 位连续数字，仅保留后 4 位。
        result = result.replaceAll("(?<!\\d)\\d{12,15}(\\d{4})(?!\\d)", "************$1");
        // 4. 邮箱：保留首字符与域名，掩码本地部分其余字符。
        result = result.replaceAll("([\\w.+-])[\\w.+-]*(@[\\w.-]+)", "$1***$2");

        if ("MASK_ALL".equalsIgnoreCase(strategy)) {
            // 5. 「患者/姓名」标注后的 2-4 位中文姓名。
            result = result.replaceAll("(患者|姓名)([:：]?\\s*)[\\u4e00-\\u9fa5]{2,4}", "$1$2**");
            // 6. 「病历号/就诊号/住院号/门诊号」标注后的字母数字编号。
            result = result.replaceAll("(病历号|就诊号|住院号|门诊号)([:：]?\\s*)[A-Za-z0-9-]{3,}", "$1$2****");
        }

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
            throw new IllegalStateException("SHA-256 摘要计算失败", e);
        }
    }

    /**
     * 结构化输出 Schema 校验：用 Jackson 将输出解析为 JSON（对象/数组），
     * 再按 expectedSchema 声明的 required 字段集做存在性校验；任一不满足抛 {@code ENG_LLM_002}。
     *
     * <p>相较旧实现的字符串 {@code contains}，此处对输出做真实 JSON 解析，杜绝"看起来含某关键字即通过"的伪校验。
     */
    private void validateSchema(String content, String schema) {
        JsonNode output;
        try {
            output = OBJECT_MAPPER.readTree(content);
        } catch (Exception parseError) {
            throw new ApiException(ErrorCode.ENG_LLM_002, "模型输出无法解析为合法 JSON，结构化 Schema 校验失败");
        }
        if (output == null || !(output.isObject() || output.isArray())) {
            throw new ApiException(ErrorCode.ENG_LLM_002, "模型输出不是 JSON 对象或数组，无法满足结构化 Schema");
        }
        for (String required : extractRequiredFields(schema)) {
            if (!hasField(output, required)) {
                throw new ApiException(ErrorCode.ENG_LLM_002, "模型输出字段缺失 Schema 指定 required: " + required);
            }
        }
    }

    /**
     * 从 expectedSchema 提取 required 字段名：优先按标准 JSON Schema 的 {@code required} 数组解析，
     * 失败再回退到 {@code required: [a, b]} 宽松写法的正则解析。
     */
    private Set<String> extractRequiredFields(String schema) {
        Set<String> fields = new LinkedHashSet<>();
        try {
            JsonNode schemaNode = OBJECT_MAPPER.readTree(schema);
            JsonNode requiredNode = schemaNode == null ? null : schemaNode.get("required");
            if (requiredNode != null && requiredNode.isArray()) {
                requiredNode.forEach(node -> {
                    String name = node.asText().trim();
                    if (!name.isBlank()) {
                        fields.add(name);
                    }
                });
                return fields;
            }
        } catch (Exception ignored) {
            // 非标准 JSON Schema（如 "required: [entity]"），走下方正则回退。
        }
        Matcher matcher = REQUIRED_LOOSE.matcher(schema);
        if (matcher.find()) {
            for (String token : matcher.group(1).split(",")) {
                String name = token.replaceAll("[\"'\\[\\]\\s]", "").trim();
                if (!name.isBlank()) {
                    fields.add(name);
                }
            }
        }
        return fields;
    }

    /** 判断 JSON 节点是否含某字段：对象看自身键，数组要求每个对象元素均含该字段。 */
    private boolean hasField(JsonNode node, String field) {
        if (node.isObject()) {
            return node.has(field);
        }
        if (node.isArray()) {
            if (node.isEmpty()) {
                return false;
            }
            for (JsonNode element : node) {
                if (!element.isObject() || !element.has(field)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** 根据路由策略给出诚实的 B0 基线归因说明（绝不伪造模型推理）。 */
    private String baselineReason(String strategy) {
        if ("BASEPLAY".equalsIgnoreCase(strategy)) {
            return "组织安全策略显式指定 B0 确定性基线路径";
        }
        if ("LOCAL_MODEL".equalsIgnoreCase(strategy)) {
            return "未接入本地微调模型(B1)，按 B0 确定性基线执行；模型接入由 GA-ENG-LLM-02 落地";
        }
        if ("EXTERNAL_MODEL".equalsIgnoreCase(strategy)) {
            return "未接入外部大模型/Dify(B2)，按 B0 确定性基线执行；模型接入由 GA-ENG-LLM-02 落地";
        }
        return "未识别的路由策略 " + strategy + "，回退 B0 确定性基线";
    }

    /**
     * B0 级确定性基线回退处理器（B0 Fallback Processor）。
     *
     * <p>根据能力标识提供 100% 格式合法且中文化的物理候选数据。
     */
    private String executeB0Fallback(String capabilityCode) {
        return switch (capabilityCode) {
            case "knowledge.extract" -> "{\"entity\": \"临床概念A\", \"degree\": \"分级A\", \"risk\": \"风险级别A\"}";
            case "terminology.map" -> "{\"standard_code\": \"STANDARD-CODE\", \"standard_name\": \"标准术语A\"}";
            case "rule.draft" -> "{\"rule_name\": \"用药安全规则草案\", \"trigger\": \"结构化条件A\", \"action\": \"推荐卡片\"}";
            case "pathway.draft" -> "{\"pathway_name\": \"专科路径草案\", \"steps\": [\"入径评估\", \"执行节点\", \"出径评估\"]}";
            default -> "{\"result\": \"确定性基线回退数据\", \"capability\": \"" + capabilityCode + "\"}";
        };
    }
}
