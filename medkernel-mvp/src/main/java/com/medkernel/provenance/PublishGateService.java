package com.medkernel.provenance;

import com.medkernel.common.ErrorCode;
import com.medkernel.common.TraceContext;
import com.medkernel.persistence.EnginePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发布门禁服务：统一检查资产发布前的来源文档绑定状态。
 * 
 * 医疗/质控/医保资产发布时必须满足：
 * 1. 存在有效的 SourceAssetBinding（资产-来源文档绑定）
 * 2. 绑定的 SourceDocument 存在且状态为 APPROVED 或 REVIEWED
 * 3. 来源文档未过期（如果设置了 expiry_date）
 * 
 * REFIT-003: 来源/审计/traceId/发布门禁统一改造
 */
@Service
public class PublishGateService {
    private static final Logger log = LoggerFactory.getLogger(PublishGateService.class);
    private static final List<String> VALID_REVIEW_STATUS = Arrays.asList("APPROVED", "REVIEWED");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SourceAssetBindingService bindingService;
    private final ProvenanceService provenanceService;
    private final EnginePersistenceService persistenceService;

    public PublishGateService(SourceAssetBindingService bindingService, ProvenanceService provenanceService,
                              EnginePersistenceService persistenceService) {
        this.bindingService = bindingService;
        this.provenanceService = provenanceService;
        this.persistenceService = persistenceService;
    }

    /**
     * 检查资产是否满足发布门禁要求。
     * 
     * @param assetType 资产类型（RULE, PATHWAY, CONFIG_PACKAGE, ADAPTER, GRAPH, QC_METRIC）
     * @param assetCode 资产编码
     * @param tenantId  租户ID（可选，默认为 "default"）
     * @return 检查结果，包含 issues 列表；如果 issues 为空则表示通过
     */
    public Map<String, Object> checkPublishGate(String assetType, String assetCode, String tenantId) {
        String resolvedTenantId = (tenantId == null || tenantId.trim().isEmpty()) ? "default" : tenantId.trim();
        String resolvedAssetType = assetType == null ? null : assetType.trim().toUpperCase();

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("asset_type", resolvedAssetType);
        result.put("asset_code", assetCode);
        result.put("tenant_id", resolvedTenantId);

        List<Map<String, Object>> issues = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> warnings = new ArrayList<Map<String, Object>>();

        // 1. 检查是否存在绑定
        List<Map<String, Object>> bindings = bindingService.getBindingsByAsset(resolvedAssetType, assetCode, resolvedTenantId);
        if (bindings.isEmpty()) {
            issues.add(issue("ERROR", "MISSING_SOURCE", "binding",
                    "资产 " + assetCode + " 没有来源文档绑定（SourceAssetBinding），无法发布"));
            result.put("issues", issues);
            result.put("warnings", warnings);
            result.put("passed", false);
            return result;
        }

        // 2. 检查每个绑定的来源文档状态
        boolean hasValidBinding = false;
        for (Map<String, Object> binding : bindings) {
            String documentCode = string(binding.get("document_code"), null);
            if (documentCode == null) {
                issues.add(issue("ERROR", "MISSING_SOURCE", "document_code",
                        "绑定 " + binding.get("binding_id") + " 缺少 document_code"));
                continue;
            }

            try {
                Map<String, Object> document = provenanceService.getDocument(documentCode, resolvedTenantId);
                String reviewStatus = upper(string(document.get("review_status"), null));
                String expiryDate = string(document.get("expiry_date"), null);

                // 检查审核状态
                if (!VALID_REVIEW_STATUS.contains(reviewStatus)) {
                    issues.add(issue("ERROR", "SOURCE_NOT_REVIEWED", "review_status",
                            "来源文档 " + documentCode + " 审核状态为 " + reviewStatus + "，需要 APPROVED 或 REVIEWED"));
                    continue;
                }

                // 检查是否过期
                if (expiryDate != null && !expiryDate.trim().isEmpty()) {
                    try {
                        LocalDate expiry = LocalDate.parse(expiryDate, DATE_FORMATTER);
                        if (expiry.isBefore(LocalDate.now())) {
                            issues.add(issue("ERROR", "SOURCE_EXPIRED", "expiry_date",
                                    "来源文档 " + documentCode + " 已过期（过期日期: " + expiryDate + "）"));
                            continue;
                        }
                    } catch (DateTimeParseException e) {
                        warnings.add(issue("WARN", "INVALID_DATE", "expiry_date",
                                "来源文档 " + documentCode + " 的 expiry_date 格式无效: " + expiryDate));
                    }
                }

                hasValidBinding = true;
            } catch (IllegalArgumentException e) {
                issues.add(issue("ERROR", "SOURCE_NOT_FOUND", "document_code",
                        "来源文档 " + documentCode + " 不存在: " + e.getMessage()));
            }
        }

        if (!hasValidBinding && issues.isEmpty()) {
            issues.add(issue("ERROR", "MISSING_SOURCE", "binding",
                    "资产 " + assetCode + " 没有有效的来源文档绑定"));
        }

        result.put("issues", issues);
        result.put("warnings", warnings);
        result.put("passed", issues.isEmpty());

        // REFIT-003: 审计日志 - 记录发布门禁检查结果
        auditPublishGate(resolvedAssetType, assetCode, resolvedTenantId, result);

        return result;
    }

    /**
     * 验证资产是否满足发布门禁，如果不满足则抛出异常。
     * 
     * @param assetType 资产类型
     * @param assetCode 资产编码
     * @param tenantId  租户ID（可选）
     * @throws IllegalArgumentException 如果不满足门禁要求
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
            throw new IllegalArgumentException(sb.toString());
        }
    }

    /**
     * 收集资产的来源问题（兼容现有代码风格）。
     * 
     * @param assetType 资产类型
     * @param assetCode 资产编码
     * @param tenantId  租户ID（可选）
     * @param issues    问题列表（输出）
     */
    public void collectSourceIssues(String assetType, String assetCode, String tenantId, List<Map<String, Object>> issues) {
        Map<String, Object> result = checkPublishGate(assetType, assetCode, tenantId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultIssues = (List<Map<String, Object>>) result.get("issues");
        if (resultIssues != null) {
            issues.addAll(resultIssues);
        }
    }

    private Map<String, Object> issue(String severity, String code, String field, String message) {
        Map<String, Object> issue = new LinkedHashMap<String, Object>();
        issue.put("severity", severity);
        issue.put("code", code);
        issue.put("field", field);
        issue.put("message", message);
        return issue;
    }

    private void auditPublishGate(String assetType, String assetCode, String tenantId, Map<String, Object> result) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("asset_type", assetType);
        detail.put("asset_code", assetCode);
        detail.put("tenant_id", tenantId);
        detail.put("passed", result.get("passed"));
        detail.put("issue_count", ((List<?>) result.get("issues")).size());
        detail.put("warning_count", ((List<?>) result.get("warnings")).size());
        detail.put("trace_id", TraceContext.getTraceId());
        try {
            persistenceService.saveAuditLog("PUBLISH_GATE", "CHECK", assetType, assetCode,
                    null, null, null, detail);
        } catch (RuntimeException ex) {
            // 审计写入失败不应影响发布门禁检查。
            log.warn("[traceId={}] publish gate audit log persistence failed: {}",
                    TraceContext.getTraceId(), ex.getMessage());
        }
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text.trim();
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
