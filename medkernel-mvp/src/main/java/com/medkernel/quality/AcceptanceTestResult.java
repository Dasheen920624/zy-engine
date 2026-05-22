package com.medkernel.quality;

import java.time.LocalDateTime;

public class AcceptanceTestResult {
    private Long id;
    private Long tenantId;
    private String resultCode;
    private Long testCaseId;
    private String caseCode;
    private String caseName;
    private String featureCode;
    private String category;
    private String verdict;
    private String actualResult;
    private String deviation;
    private String evidenceRefs;
    private String environment;
    private String executedBy;
    private LocalDateTime executedTime;
    private String reviewedBy;
    private LocalDateTime reviewedTime;
    private String reviewNote;
    private String status;
    private LocalDateTime createdTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public Long getTestCaseId() { return testCaseId; }
    public void setTestCaseId(Long testCaseId) { this.testCaseId = testCaseId; }
    public String getCaseCode() { return caseCode; }
    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }
    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public String getFeatureCode() { return featureCode; }
    public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }
    public String getDeviation() { return deviation; }
    public void setDeviation(String deviation) { this.deviation = deviation; }
    public String getEvidenceRefs() { return evidenceRefs; }
    public void setEvidenceRefs(String evidenceRefs) { this.evidenceRefs = evidenceRefs; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
    public LocalDateTime getExecutedTime() { return executedTime; }
    public void setExecutedTime(LocalDateTime executedTime) { this.executedTime = executedTime; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedTime() { return reviewedTime; }
    public void setReviewedTime(LocalDateTime reviewedTime) { this.reviewedTime = reviewedTime; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
