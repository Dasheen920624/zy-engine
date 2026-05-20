package com.medkernel.audit;

import com.medkernel.common.ErrorCode;
import com.medkernel.common.TraceContext;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.provenance.SourceAssetBinding;
import com.medkernel.provenance.SourceAssetBindingService;
import com.medkernel.provenance.SourceCitation;
import com.medkernel.provenance.SourceCitationService;
import com.medkernel.provenance.SourceDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一发布门禁服务：在医学/医保/质控资产发布前执行来源完整性校验。
 * 不满足以下任一条件则阻断发布：
 * 1. 关键资产必须有来源文档绑定（reference_document_code）
 * 2. 来源文档不能过期（expiry_date 已过）
 * 3. 来源文档必须已审核（review_status == REVIEWED）
 *
 * 对应产品不变量 H4：关键医疗/医保/质控配置必须有来源追溯。
 * 缺来源、来源过期、未审核不得发布。
 */
@Service
public class PublishGateService {
    private static final Logger log = LoggerFactory.getLogger(PublishGateService.class);

    private final EnginePersistenceService persistenceService;
    private final SourceCitationService citationService;
    private final SourceAssetBindingService bindingService;

    public PublishGateService(EnginePersistenceService persistenceService,
                              SourceCitationService citationService,
                              SourceAssetBindingService bindingService) {
        this.persistenceService = persistenceService;
        this.citationService = citationService;
        this.bindingService = bindingService;
    }

    /**
     * 校验单个资产的来源绑定是否满足发布要求。
     *
     * @param assetType    资产类型（RULE / PATHWAY / GRAPH / CONFIG_PACKAGE 等）
     * @param assetCode    资产编码
     * @param referenceDoc 来源文档编码（reference_document_code），可为 null
     * @return 校验结果，包含 issues 列表和 readyToPublish 标志
     */
    public GateCheckResult checkSingle(String assetType, String assetCode, String referenceDoc) {
        GateCheckResult result = new GateCheckResult();
        if (referenceDoc == null || referenceDoc.trim().isEmpty()) {
            result.addIssue("ERROR", "reference_document_code",
                    "资产缺少来源文档绑定（reference_document_code），发布将被阻断",
                    assetCode, assetType);
            return result;
        }

        // 检查来源文档是否存在
        SourceDocument doc = findSourceDocument(referenceDoc);
        if (doc == null) {
            result.addIssue("ERROR", "reference_document_code",
                    "来源文档不存在：" + referenceDoc,
                    assetCode, assetType);
            return result;
        }

        // 检查来源文档是否过期
        if (isExpired(doc)) {
            result.addIssue("ERROR", "source_document.expiry_date",
                    "来源文档已过期：" + referenceDoc,
                    assetCode, assetType);
        }

        // 检查来源文档是否已审核
        if (!isReviewed(doc)) {
            result.addIssue("ERROR", "source_document.review_status",
                    "来源文档未经审核：" + referenceDoc,
                    assetCode, assetType);
        }

        return result;
    }

    /**
     * 批量校验多个资产的来源绑定。
     *
     * @param assetType 资产类型
     * @param assets    每个元素需包含 "asset_code" 和 "reference_document_code"
     * @return 所有资产的合并校验结果
     */
    public GateCheckResult checkBatch(String assetType, List<Map<String, Object>> assets) {
        GateCheckResult merged = new GateCheckResult();
        if (assets == null || assets.isEmpty()) {
            return merged;
        }
        for (Map<String, Object> asset : assets) {
            String code = string(asset.get("asset_code"), string(asset.get("rule_code"),
                    string(asset.get("node_code"), "unknown")));
            String refDoc = string(asset.get("reference_document_code"), null);
            GateCheckResult single = checkSingle(assetType, code, refDoc);
            merged.merge(single);
        }
        return merged;
    }

    /**
     * 校验配置包内的来源审查状态（从 manifest 或 source_review 字段读取）。
     *
     * @param sourceReview 来源审查结果（通常由 ConfigPackageService.buildReview() 生成）
     * @return 校验结果
     */
    public GateCheckResult checkConfigPackageSourceReview(Map<String, Object> sourceReview) {
        GateCheckResult result = new GateCheckResult();
        if (sourceReview == null) {
            return result;
        }
        boolean enabled = bool(sourceReview.get("enabled"), false);
        if (!enabled) {
            // 来源审查未启用时不阻断，但记录 warning
            result.addIssue("WARN", "source_review.enabled",
                    "配置包来源审查未启用", null, "CONFIG_PACKAGE");
            return result;
        }
        boolean blocked = bool(sourceReview.get("blocked"), false);
        if (blocked) {
            result.addIssue("ERROR", "source_review.blocked",
                    "来源审查已阻断发布", null, "CONFIG_PACKAGE");
        }
        int missing = intValue(sourceReview.get("missing_count"), 0);
        int expired = intValue(sourceReview.get("expired_count"), 0);
        int unreviewed = intValue(sourceReview.get("unreviewed_count"), 0);
        if (missing > 0) {
            result.addIssue("ERROR", "source_review.missing_count",
                    "来源审查发现缺失来源：" + missing + " 项", null, "CONFIG_PACKAGE");
        }
        if (expired > 0) {
            result.addIssue("ERROR", "source_review.expired_count",
                    "来源审查发现过期来源：" + expired + " 项", null, "CONFIG_PACKAGE");
        }
        if (unreviewed > 0) {
            result.addIssue("ERROR", "source_review.unreviewed_count",
                    "来源审查发现未审核来源：" + unreviewed + " 项", null, "CONFIG_PACKAGE");
        }
        if (!bool(sourceReview.get("allow_publish"), true)) {
            result.addIssue("ERROR", "source_review.allow_publish",
                    "来源审查不允许发布", null, "CONFIG_PACKAGE");
        }
        return result;
    }

    /**
     * 校验规则定义的来源绑定。
     */
    public GateCheckResult checkRuleReference(String ruleCode, String referenceDocumentCode) {
        return checkSingle("RULE", ruleCode, referenceDocumentCode);
    }

    /**
     * 校验路径模板中所有节点的来源绑定。
     */
    public GateCheckResult checkPathwayReferences(List<Map<String, Object>> missingReferences) {
        GateCheckResult result = new GateCheckResult();
        if (missingReferences == null || missingReferences.isEmpty()) {
            return result;
        }
        for (Map<String, Object> ref : missingReferences) {
            String elementCode = string(ref.get("element_code"), "unknown");
            String severity = string(ref.get("severity"), "WARN");
            String message = string(ref.get("message"), "缺少来源文档绑定");
            // 路径节点缺少来源视为 ERROR（阻断发布）
            result.addIssue("ERROR", "reference_document_code",
                    "路径元素 " + elementCode + "：" + message,
                    elementCode, "PATHWAY");
        }
        return result;
    }

    /**
     * 校验图谱版本的来源绑定。
     */
    public GateCheckResult checkGraphReference(String graphVersion, String referenceDocumentCode) {
        return checkSingle("GRAPH", graphVersion, referenceDocumentCode);
    }

    /**
     * 检查资产是否满足发布门禁要求（兼容 provenance 包接口）。
     *
     * @param assetType 资产类型（RULE, PATHWAY, CONFIG_PACKAGE, ADAPTER, GRAPH, QC_METRIC）
     * @param assetCode 资产编码
     * @param tenantId  租户ID（可选，默认为 "default"）
     * @return 检查结果 Map，包含 issues/warnings/passed 字段
     */
    public Map<String, Object> checkPublishGate(String assetType, String assetCode, String tenantId) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("asset_type", assetType);
        result.put("asset_code", assetCode);
        result.put("tenant_id", tenantId == null ? "default" : tenantId);

        // 使用 checkSingle 进行来源绑定检查（基于 reference_document_code）
        // 注意：此方法为兼容接口，实际来源检查应使用 checkSingle/checkBatch
        GateCheckResult gateResult = checkSingle(assetType, assetCode, null);
        result.put("issues", gateResult.toMapList());
        result.put("warnings", new ArrayList<Map<String, Object>>());
        result.put("passed", gateResult.isReadyToPublish());
        return result;
    }

    /**
     * 验证资产是否满足发布门禁，如果不满足则抛出异常。
     *
     * @param assetType 资产类型
     * @param assetCode 资产编码
     * @param tenantId  租户ID（可选）
     * @throws IllegalStateException 如果不满足门禁要求
     */
    public void requirePublishGate(String assetType, String assetCode, String tenantId) {
        Map<String, Object> result = checkPublishGate(assetType, assetCode, tenantId);
        if (!Boolean.TRUE.equals(result.get("passed"))) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
            StringBuilder sb = new StringBuilder();
            sb.append("资产 ").append(assetCode).append(" 不满足发布门禁要求:");
            for (Map<String, Object> issue : issues) {
                sb.append("\n  - ").append(issue.get("message"));
            }
            throw new IllegalStateException(sb.toString());
        }
    }

    /**
     * 将 GateCheckResult 转为可抛出的异常消息。
     */
    public String formatBlockingMessage(GateCheckResult result) {
        if (result.isReadyToPublish()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("发布门禁检查未通过：");
        for (GateCheckResult.Issue issue : result.getIssues()) {
            if ("ERROR".equals(issue.severity)) {
                sb.append("\n  - ").append(issue.message);
            }
        }
        return sb.toString();
    }

    /**
     * 写入发布门禁审计日志。
     */
    public void auditGateCheck(String engineType, String actionType, String targetType,
                                String targetCode, String operatorId, GateCheckResult result) {
        try {
            Map<String, Object> detail = new LinkedHashMap<String, Object>();
            detail.put("gate_check_ready", result.isReadyToPublish());
            detail.put("issue_count", result.getIssues().size());
            detail.put("issues", result.toMapList());
            persistenceService.saveAuditLog(engineType, actionType, targetType, targetCode,
                    null, null, operatorId, detail);
        } catch (RuntimeException ignored) {
            // 审计写入失败不阻断主流程
            log.warn("[traceId={}] publish gate audit failed: {}", TraceContext.getTraceId(), targetCode);
        }
    }

    private SourceDocument findSourceDocument(String documentCode) {
        // 优先从持久化层查找
        try {
            List<SourceDocument> docs = persistenceService.listSourceDocuments();
            for (SourceDocument doc : docs) {
                if (documentCode.equals(doc.getDocumentCode())) {
                    return doc;
                }
            }
        } catch (RuntimeException ex) {
            log.warn("查找来源文档失败: {}", ex.getMessage());
        }
        return null;
    }

    private boolean isExpired(SourceDocument doc) {
        if (doc == null) {
            return false;
        }
        String expiryDate = doc.getExpiryDate();
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            return false;
        }
        try {
            OffsetDateTime expiry = OffsetDateTime.parse(expiryDate);
            return expiry.isBefore(OffsetDateTime.now());
        } catch (DateTimeParseException ex) {
            // 日期格式异常不视为过期
            return false;
        }
    }

    private boolean isReviewed(SourceDocument doc) {
        if (doc == null) {
            return false;
        }
        String status = doc.getReviewStatus();
        return "REVIEWED".equalsIgnoreCase(status)
                || "APPROVED".equalsIgnoreCase(status);
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text.trim();
    }

    private boolean bool(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return "true".equalsIgnoreCase(String.valueOf(value).trim());
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 发布门禁校验结果。
     */
    public static class GateCheckResult {
        private final List<Issue> issues = new ArrayList<Issue>();

        public boolean isReadyToPublish() {
            for (Issue issue : issues) {
                if ("ERROR".equals(issue.severity)) {
                    return false;
                }
            }
            return true;
        }

        public List<Issue> getIssues() {
            return issues;
        }

        public void addIssue(String severity, String field, String message,
                             String assetCode, String assetType) {
            issues.add(new Issue(severity, field, message, assetCode, assetType));
        }

        public void merge(GateCheckResult other) {
            if (other != null) {
                this.issues.addAll(other.issues);
            }
        }

        public List<Map<String, Object>> toMapList() {
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (Issue issue : issues) {
                Map<String, Object> map = new LinkedHashMap<String, Object>();
                map.put("severity", issue.severity);
                map.put("field", issue.field);
                map.put("message", issue.message);
                map.put("asset_code", issue.assetCode);
                map.put("asset_type", issue.assetType);
                list.add(map);
            }
            return list;
        }

        public static class Issue {
            public final String severity;
            public final String field;
            public final String message;
            public final String assetCode;
            public final String assetType;

            public Issue(String severity, String field, String message,
                         String assetCode, String assetType) {
                this.severity = severity;
                this.field = field;
                this.message = message;
                this.assetCode = assetCode;
                this.assetType = assetType;
            }
        }
    }
}
