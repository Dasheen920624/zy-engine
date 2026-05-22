package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 记录覆盖请求 DTO：用于 CdssOverrideController.recordOverride。
 */
@Schema(description = "记录覆盖请求")
public class RecordOverrideRequest {

    @NotBlank(message = "alertId 不能为空")
    @Schema(description = "警报ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String alertId;

    @Schema(description = "触发编码")
    private String triggerCode;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "风险等级")
    private String riskLevel;

    @Schema(description = "警报等级")
    private String alertLevel;

    @NotBlank(message = "overrideType 不能为空")
    @Schema(description = "覆盖类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String overrideType;

    @Schema(description = "覆盖原因")
    private String overrideReason;

    @Schema(description = "覆盖分类")
    private String overrideCategory;

    @Schema(description = "主管确认人")
    private String supervisorName;

    @Schema(description = "确认人")
    private String confirmedBy;

    @Schema(description = "患者ID")
    private String patientId;

    @Schema(description = "就诊ID")
    private String encounterId;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "是否审计红线")
    private String isAuditRedLine;

    @Schema(description = "疲劳抑制")
    private String fatigueSuppressed;

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
    public String getIsAuditRedLine() { return isAuditRedLine; }
    public void setIsAuditRedLine(String isAuditRedLine) { this.isAuditRedLine = isAuditRedLine; }
    public String getFatigueSuppressed() { return fatigueSuppressed; }
    public void setFatigueSuppressed(String fatigueSuppressed) { this.fatigueSuppressed = fatigueSuppressed; }
}
