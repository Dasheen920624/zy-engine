package com.medkernel.cdss;

import java.time.LocalDateTime;

/**
 * CDSS 覆盖审计日志实体。
 * 记录医生对 CDSS 告警的确认/覆盖/上报操作，用于审计追溯和疲劳治理。
 */
public class CdssOverrideLog {
    private Long id;
    private Long tenantId;
    private String alertId;
    private String triggerCode;
    private String ruleCode;
    private String riskLevel;
    private String alertLevel;        // NOTICE/SOFT/BLOCK/ESCALATE
    private String overrideType;      // ACKNOWLEDGE/OVERRIDE/ESCALATE
    private String overrideReason;
    private String overrideCategory;  // CLINICAL_JUDGEMENT/ALTERNATIVE_THERAPY/PATIENT_REQUEST/EMERGENCY/OTHER
    private String supervisorName;
    private String confirmedBy;       // 二次确认人
    private String patientId;
    private String encounterId;
    private String operatorId;
    private String departmentCode;
    private String isAuditRedLine;    // TRUE/FALSE
    private String fatigueSuppressed; // TRUE/FALSE 是否被疲劳抑制
    private LocalDateTime overrideTime;
    private LocalDateTime createdTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getTriggerCode() { return triggerCode; }
    public void setTriggerCode(String triggerCode) { this.triggerCode = triggerCode; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }

    public String getOverrideType() { return overrideType; }
    public void setOverrideType(String overrideType) { this.overrideType = overrideType; }

    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }

    public String getOverrideCategory() { return overrideCategory; }
    public void setOverrideCategory(String overrideCategory) { this.overrideCategory = overrideCategory; }

    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }

    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getIsAuditRedLine() { return isAuditRedLine; }
    public void setIsAuditRedLine(String isAuditRedLine) { this.isAuditRedLine = isAuditRedLine; }

    public String getFatigueSuppressed() { return fatigueSuppressed; }
    public void setFatigueSuppressed(String fatigueSuppressed) { this.fatigueSuppressed = fatigueSuppressed; }

    public LocalDateTime getOverrideTime() { return overrideTime; }
    public void setOverrideTime(LocalDateTime overrideTime) { this.overrideTime = overrideTime; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
