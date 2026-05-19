package com.medkernel.cdss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CDSS 告警模型。
 * 包含风险分级、来源证据、医生确认和人工覆盖信息。
 */
public class CdssAlert {
    private String alertId;
    private String triggerPoint;
    private CdssRiskLevel riskLevel;
    private String title;
    private String message;
    private List<Map<String, Object>> evidence;
    private Map<String, Object> source;
    private boolean requiresConfirmation;
    private boolean isBlocking;
    private CdssOverride override;
    private String patientId;
    private String encounterId;
    private String ruleCode;
    private String ruleVersion;
    private String createdAt;

    public CdssAlert() {
        this.evidence = new ArrayList<>();
        this.source = new HashMap<>();
    }

    // Getters and Setters
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getTriggerPoint() { return triggerPoint; }
    public void setTriggerPoint(String triggerPoint) { this.triggerPoint = triggerPoint; }

    public CdssRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(CdssRiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Map<String, Object>> getEvidence() { return evidence; }
    public void setEvidence(List<Map<String, Object>> evidence) { this.evidence = evidence; }

    public Map<String, Object> getSource() { return source; }
    public void setSource(Map<String, Object> source) { this.source = source; }

    public boolean isRequiresConfirmation() { return requiresConfirmation; }
    public void setRequiresConfirmation(boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; }

    public boolean isBlocking() { return isBlocking; }
    public void setBlocking(boolean blocking) { isBlocking = blocking; }

    public CdssOverride getOverride() { return override; }
    public void setOverride(CdssOverride override) { this.override = override; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /**
     * 医生覆盖信息。
     * 对标 CDSS 标准：医生可覆盖非阻断告警，但覆盖原因必须记录。
     * 阻断级告警需要上级确认。
     */
    public static class CdssOverride {
        private String overrideReason;
        private String overrideType; // ACKNOWLEDGE, OVERRIDE, ESCALATE
        private String overriddenBy;
        private String overriddenAt;
        private String supervisorName; // 上级确认人（阻断级必需）
        private boolean isAuditRedLine; // 审计红线：覆盖是否触发审计告警

        public String getOverrideReason() { return overrideReason; }
        public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }

        public String getOverrideType() { return overrideType; }
        public void setOverrideType(String overrideType) { this.overrideType = overrideType; }

        public String getOverriddenBy() { return overriddenBy; }
        public void setOverriddenBy(String overriddenBy) { this.overriddenBy = overriddenBy; }

        public String getOverriddenAt() { return overriddenAt; }
        public void setOverriddenAt(String overriddenAt) { this.overriddenAt = overriddenAt; }

        public String getSupervisorName() { return supervisorName; }
        public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }

        public boolean isAuditRedLine() { return isAuditRedLine; }
        public void setAuditRedLine(boolean auditRedLine) { isAuditRedLine = auditRedLine; }
    }
}
