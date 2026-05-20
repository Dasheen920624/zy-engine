package com.medkernel.knowledge;

import java.time.LocalDateTime;

/**
 * AI 知识生产任务。
 * 对应数据库表：ai_knowledge_job
 */
public class AiKnowledgeJob {
    private Long id;
    private Long tenantId;
    private String jobCode;
    private String jobName;
    private String jobType;
    private String sourceCode;
    private String subscriptionId;
    private String modelProvider;
    private String modelName;
    private String promptVersion;
    private String inputHash;
    private String outputHash;
    private String inputSummary;
    private String outputSummary;
    private String evidenceIds;
    private String status;
    private String reviewStatus;
    private String reviewedBy;
    private LocalDateTime reviewedTime;
    private String reviewComment;
    private String errorCode;
    private String errorMessage;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime startedTime;
    private LocalDateTime finishedTime;
    private int durationMs;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getJobCode() { return jobCode; }
    public void setJobCode(String jobCode) { this.jobCode = jobCode; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }

    public String getInputHash() { return inputHash; }
    public void setInputHash(String inputHash) { this.inputHash = inputHash; }

    public String getOutputHash() { return outputHash; }
    public void setOutputHash(String outputHash) { this.outputHash = outputHash; }

    public String getInputSummary() { return inputSummary; }
    public void setInputSummary(String inputSummary) { this.inputSummary = inputSummary; }

    public String getOutputSummary() { return outputSummary; }
    public void setOutputSummary(String outputSummary) { this.outputSummary = outputSummary; }

    public String getEvidenceIds() { return evidenceIds; }
    public void setEvidenceIds(String evidenceIds) { this.evidenceIds = evidenceIds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedTime() { return reviewedTime; }
    public void setReviewedTime(LocalDateTime reviewedTime) { this.reviewedTime = reviewedTime; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public LocalDateTime getStartedTime() { return startedTime; }
    public void setStartedTime(LocalDateTime startedTime) { this.startedTime = startedTime; }

    public LocalDateTime getFinishedTime() { return finishedTime; }
    public void setFinishedTime(LocalDateTime finishedTime) { this.finishedTime = finishedTime; }

    public int getDurationMs() { return durationMs; }
    public void setDurationMs(int durationMs) { this.durationMs = durationMs; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
