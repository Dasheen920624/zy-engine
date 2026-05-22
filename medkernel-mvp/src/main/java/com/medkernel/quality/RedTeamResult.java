package com.medkernel.quality;

import java.time.LocalDateTime;

public class RedTeamResult {
    private Long id;
    private Long tenantId;
    private String resultCode;
    private Long scenarioId;
    private String scenarioCode;
    private String scenarioName;
    private String category;
    private String modelCode;
    private String modelVersion;
    private String promptTemplateCode;
    private String actualResponse;    // 实际响应
    private String verdict;           // PASS/FAIL/UNCERTAIN
    private String vulnerabilityType; // 漏洞类型
    private String vulnerabilityDetail;
    private String remediation;       // 修复建议
    private String severity;
    private String executedBy;
    private LocalDateTime executedTime;
    private LocalDateTime createdTime;

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

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getScenarioCode() {
        return scenarioCode;
    }

    public void setScenarioCode(String scenarioCode) {
        this.scenarioCode = scenarioCode;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getPromptTemplateCode() {
        return promptTemplateCode;
    }

    public void setPromptTemplateCode(String promptTemplateCode) {
        this.promptTemplateCode = promptTemplateCode;
    }

    public String getActualResponse() {
        return actualResponse;
    }

    public void setActualResponse(String actualResponse) {
        this.actualResponse = actualResponse;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getVulnerabilityType() {
        return vulnerabilityType;
    }

    public void setVulnerabilityType(String vulnerabilityType) {
        this.vulnerabilityType = vulnerabilityType;
    }

    public String getVulnerabilityDetail() {
        return vulnerabilityDetail;
    }

    public void setVulnerabilityDetail(String vulnerabilityDetail) {
        this.vulnerabilityDetail = vulnerabilityDetail;
    }

    public String getRemediation() {
        return remediation;
    }

    public void setRemediation(String remediation) {
        this.remediation = remediation;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public LocalDateTime getExecutedTime() {
        return executedTime;
    }

    public void setExecutedTime(LocalDateTime executedTime) {
        this.executedTime = executedTime;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
