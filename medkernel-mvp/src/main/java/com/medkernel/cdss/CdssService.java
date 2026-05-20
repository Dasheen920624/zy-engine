package com.medkernel.cdss;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.common.TraceContext;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.rule.RuleService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 临床决策支持底座服务。
 *
 * <p>对标 CDSS 标准：
 * <ul>
 *   <li>触发点 — 医嘱、病历、检查、入径、医保结算等</li>
 *   <li>风险分级 — INFO/LOW/MEDIUM/HIGH/CRITICAL</li>
 *   <li>来源证据 — 每条告警必须携带来源文档和引用</li>
 *   <li>医生确认 — 非阻断告警可确认/覆盖，阻断告警需上级确认</li>
 *   <li>人工覆盖 — 覆盖原因必须记录，阻断级覆盖触发审计红线</li>
 *   <li>审计红线 — CRITICAL 级别覆盖自动写入审计日志</li>
 * </ul>
 */
@Service
public class CdssService {

    private final RuleService ruleService;
    private final EnginePersistenceService persistenceService;
    private final OrganizationContextService organizationContextService;

    /** 活动告警存储（生产环境应替换为数据库） */
    private final Map<String, CdssAlert> activeAlerts = new ConcurrentHashMap<>();

    public CdssService(RuleService ruleService,
                       EnginePersistenceService persistenceService,
                       OrganizationContextService organizationContextService) {
        this.ruleService = ruleService;
        this.persistenceService = persistenceService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * CDSS 触发评估。
     * 根据触发点和患者上下文，调用规则引擎评估并生成 CDSS 告警。
     */
    public List<CdssAlert> evaluate(String triggerPoint, Map<String, Object> patientContext,
                                     String tenantId) {
        // 1. 将触发点映射到规则引擎场景码
        String scenarioCode = mapTriggerToScenario(triggerPoint);
        if (scenarioCode == null) {
            return Collections.emptyList();
        }

        // 2. 调用规则引擎评估
        Map<String, Object> evaluateRequest = new HashMap<>();
        evaluateRequest.put("scenario_code", scenarioCode);
        evaluateRequest.put("patient_context", patientContext);

        List<Map<String, Object>> ruleResults;
        try {
            OrganizationContext orgContext = new OrganizationContext();
            orgContext.setTenantId(tenantId);
            Map<String, Object> result = ruleService.evaluateForScenario(evaluateRequest, orgContext);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawResults = result.get("rules") instanceof List
                    ? (List<Map<String, Object>>) result.get("rules") : Collections.<Map<String, Object>>emptyList();
            ruleResults = rawResults;
        } catch (Exception e) {
            // 规则引擎异常不阻断临床流程
            return Collections.emptyList();
        }

        // 3. 将规则结果转换为 CDSS 告警
        List<CdssAlert> alerts = new ArrayList<>();
        for (Map<String, Object> ruleResult : ruleResults) {
            Boolean hit = (Boolean) ruleResult.get("hit");
            if (hit == null || !hit) continue;

            CdssAlert alert = convertToAlert(triggerPoint, ruleResult, patientContext);
            alerts.add(alert);

            // 存储活动告警
            activeAlerts.put(alert.getAlertId(), alert);
        }

        return alerts;
    }

    /**
     * 医生确认/覆盖告警。
     *
     * <p>对标 CDSS 标准：
     * <ul>
     *   <li>ACKNOWLEDGE — 确认知悉，不改变操作</li>
     *   <li>OVERRIDE — 覆盖告警，继续操作（需记录原因）</li>
     *   <li>ESCALATE — 上报上级，需上级确认（阻断级必需）</li>
     * </ul>
     */
    public CdssAlert resolveAlert(String alertId, String overrideType, String overrideReason,
                                   String operatorName, String supervisorName, String tenantId) {
        CdssAlert alert = activeAlerts.get(alertId);
        if (alert == null) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }

        // 阻断级告警必须提供上级确认
        if (alert.isBlocking() && "OVERRIDE".equals(overrideType)) {
            if (supervisorName == null || supervisorName.trim().isEmpty()) {
                throw new IllegalArgumentException("CRITICAL alert override requires supervisor confirmation");
            }
        }

        // 记录覆盖信息
        CdssAlert.CdssOverride overrideInfo = new CdssAlert.CdssOverride();
        overrideInfo.setOverrideType(overrideType);
        overrideInfo.setOverrideReason(overrideReason);
        overrideInfo.setOverriddenBy(operatorName);
        overrideInfo.setOverriddenAt(LocalDateTime.now().toString());
        overrideInfo.setSupervisorName(supervisorName);

        // 审计红线：CRITICAL 级别覆盖自动标记
        boolean isAuditRedLine = alert.getRiskLevel() == CdssRiskLevel.CRITICAL && "OVERRIDE".equals(overrideType);
        overrideInfo.setAuditRedLine(isAuditRedLine);

        alert.setOverride(overrideInfo);

        // 审计红线写入审计日志
        if (isAuditRedLine) {
            persistenceService.saveAuditLog("CDSS", "CRITICAL_OVERRIDE", "ALERT",
                    alertId, alert.getPatientId(), null, operatorName,
                    mapOf6(
                            "triggerPoint", alert.getTriggerPoint() != null ? alert.getTriggerPoint() : "",
                            "riskLevel", alert.getRiskLevel() != null ? alert.getRiskLevel().getCode() : "",
                            "overrideReason", overrideReason != null ? overrideReason : "",
                            "supervisorName", supervisorName != null ? supervisorName : "",
                            "patientId", alert.getPatientId() != null ? alert.getPatientId() : "",
                            "ruleCode", alert.getRuleCode() != null ? alert.getRuleCode() : ""
                    ));
        }

        // 记录普通确认日志
        if (!isAuditRedLine) {
            persistenceService.saveAuditLog("CDSS", overrideType, "ALERT",
                    alertId, alert.getPatientId(), null, operatorName,
                    mapOf3(
                            "triggerPoint", alert.getTriggerPoint() != null ? alert.getTriggerPoint() : "",
                            "riskLevel", alert.getRiskLevel() != null ? alert.getRiskLevel().getCode() : "",
                            "overrideReason", overrideReason != null ? overrideReason : ""
                    ));
        }

        // 从活动告警中移除
        activeAlerts.remove(alertId);

        return alert;
    }

    /**
     * 获取活动告警列表。
     */
    public List<CdssAlert> listActiveAlerts(String patientId) {
        if (patientId != null && !patientId.isEmpty()) {
            List<CdssAlert> result = new ArrayList<>();
            for (CdssAlert alert : activeAlerts.values()) {
                if (patientId.equals(alert.getPatientId())) {
                    result.add(alert);
                }
            }
            return result;
        }
        return new ArrayList<>(activeAlerts.values());
    }

    /**
     * 获取告警详情。
     */
    public CdssAlert getAlert(String alertId) {
        return activeAlerts.get(alertId);
    }

    // ─── 内部方法 ────────────────────────────────────────────────────

    private String mapTriggerToScenario(String triggerPoint) {
        if (triggerPoint == null) return null;
        switch (triggerPoint) {
            case "ORDER_PLACED":
            case "DRUG_DISPENSED":
                return "ORDER_SAFETY";
            case "EMR_SAVED":
                return "EMR_QC";
            case "EXAM_REQUESTED":
                return "EXAM_RATIONALITY";
            case "PATHWAY_ENTRY":
                return "PATHWAY_ENTRY";
            case "INSURANCE_SETTLEMENT":
                return "INSURANCE_QC";
            case "DISCHARGE":
                return "EMR_QC";
            default:
                return null;
        }
    }

    /** Java 8 兼容的 Map.of 替代：3 个键值对 */
    private static Map<String, Object> mapOf3(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        return m;
    }

    /** Java 8 兼容的 Map.of 替代：6 个键值对 */
    private static Map<String, Object> mapOf6(String k1, Object v1, String k2, Object v2,
                                              String k3, Object v3, String k4, Object v4,
                                              String k5, Object v5, String k6, Object v6) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        m.put(k4, v4);
        m.put(k5, v5);
        m.put(k6, v6);
        return m;
    }

    private CdssAlert convertToAlert(String triggerPoint, Map<String, Object> ruleResult,
                                      Map<String, Object> patientContext) {
        CdssAlert alert = new CdssAlert();
        alert.setAlertId("cdss-" + UUID.randomUUID().toString().substring(0, 8));
        alert.setTriggerPoint(triggerPoint);

        // 风险分级
        String severity = (String) ruleResult.get("severity");
        CdssRiskLevel riskLevel = CdssRiskLevel.fromSeverity(severity);
        alert.setRiskLevel(riskLevel);
        alert.setBlocking(riskLevel.isBlocking());
        alert.setRequiresConfirmation(riskLevel == CdssRiskLevel.HIGH || riskLevel == CdssRiskLevel.CRITICAL);

        // 基本信息
        alert.setTitle((String) ruleResult.getOrDefault("message", "临床决策建议"));
        alert.setMessage((String) ruleResult.getOrDefault("message", ""));

        // 规则信息
        alert.setRuleCode((String) ruleResult.get("ruleCode"));
        alert.setRuleVersion((String) ruleResult.get("versionNo"));

        // 来源证据
        Object evidenceObj = ruleResult.get("evidence");
        if (evidenceObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> evidence = (List<Map<String, Object>>) evidenceObj;
            alert.setEvidence(evidence);
        }

        // 来源文档
        Map<String, Object> source = new HashMap<>();
        source.put("documentCode", ruleResult.get("referenceDocumentCode"));
        source.put("citationId", ruleResult.get("referenceCitationId"));
        source.put("bindingType", ruleResult.get("referenceBindingType"));
        alert.setSource(source);

        // 患者信息
        if (patientContext != null) {
            alert.setPatientId((String) patientContext.get("patient_id"));
            alert.setEncounterId((String) patientContext.get("encounter_id"));
        }

        alert.setCreatedAt(LocalDateTime.now().toString());

        return alert;
    }
}
