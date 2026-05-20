package com.medkernel.cdss;

import java.time.LocalDateTime;

public class SafetyRedLine {
    private Long id;
    private Long tenantId;
    private String redLineCode;
    private String redLineName;
    private String category;          // MEDICATION/DIAGNOSIS/PROCEDURE/PATHWAY/AI_OUTPUT
    private String description;
    private String conditionExpression; // 红线触发条件表达式
    private String blockingAction;     // WARN/BLOCK/ESCALATE/REQUIRE_DUAL_CONFIRM
    private String severity;           // HIGH/CRITICAL
    private String applicableScenarios; // 适用场景 JSON
    private String enabled;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getRedLineCode() {
        return redLineCode;
    }

    public void setRedLineCode(String redLineCode) {
        this.redLineCode = redLineCode;
    }

    public String getRedLineName() {
        return redLineName;
    }

    public void setRedLineName(String redLineName) {
        this.redLineName = redLineName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public String getBlockingAction() {
        return blockingAction;
    }

    public void setBlockingAction(String blockingAction) {
        this.blockingAction = blockingAction;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getApplicableScenarios() {
        return applicableScenarios;
    }

    public void setApplicableScenarios(String applicableScenarios) {
        this.applicableScenarios = applicableScenarios;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
