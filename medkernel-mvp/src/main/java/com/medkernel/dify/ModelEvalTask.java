package com.medkernel.dify;

import java.time.LocalDateTime;

public class ModelEvalTask {
    private Long id;
    private Long tenantId;
    private String taskCode;
    private String taskName;
    private String modelCode;
    private String modelVersion;
    private String promptTemplateCode;
    private String promptVersion;
    private String benchmarkCode;      // 评测基准编码
    private String benchmarkName;
    private int sampleSize;
    private String status;             // PENDING/RUNNING/COMPLETED/FAILED
    private Double accuracyScore;
    private Double latencyMs;
    private Double passRate;
    private String resultSummary;
    private String detailJson;
    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime completedTime;

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

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
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

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getBenchmarkCode() {
        return benchmarkCode;
    }

    public void setBenchmarkCode(String benchmarkCode) {
        this.benchmarkCode = benchmarkCode;
    }

    public String getBenchmarkName() {
        return benchmarkName;
    }

    public void setBenchmarkName(String benchmarkName) {
        this.benchmarkName = benchmarkName;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getAccuracyScore() {
        return accuracyScore;
    }

    public void setAccuracyScore(Double accuracyScore) {
        this.accuracyScore = accuracyScore;
    }

    public Double getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Double latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Double getPassRate() {
        return passRate;
    }

    public void setPassRate(Double passRate) {
        this.passRate = passRate;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
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

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }
}
