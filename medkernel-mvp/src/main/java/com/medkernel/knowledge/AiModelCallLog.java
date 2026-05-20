package com.medkernel.knowledge;

import java.time.LocalDateTime;

/**
 * AI 模型调用记录。
 * 对应数据库表：ai_model_call_log
 */
public class AiModelCallLog {
    private Long id;
    private Long tenantId;
    private Long jobId;
    private String callType;
    private String modelProvider;
    private String modelName;
    private String modelVersion;
    private String promptTemplateId;
    private String promptVersion;
    private String promptHash;
    private String inputHash;
    private String outputHash;
    private Integer inputTokenCount;
    private Integer outputTokenCount;
    private Integer totalTokenCount;
    private String callStatus;
    private String errorCode;
    private String errorMessage;
    private String fallbackUsed;
    private String fallbackProvider;
    private String fallbackModel;
    private String traceId;
    private String patientId;
    private String encounterId;
    private Integer elapsedMs;
    private LocalDateTime calledTime;
    private String createdBy;
    private LocalDateTime createdTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getPromptTemplateId() { return promptTemplateId; }
    public void setPromptTemplateId(String promptTemplateId) { this.promptTemplateId = promptTemplateId; }

    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }

    public String getPromptHash() { return promptHash; }
    public void setPromptHash(String promptHash) { this.promptHash = promptHash; }

    public String getInputHash() { return inputHash; }
    public void setInputHash(String inputHash) { this.inputHash = inputHash; }

    public String getOutputHash() { return outputHash; }
    public void setOutputHash(String outputHash) { this.outputHash = outputHash; }

    public Integer getInputTokenCount() { return inputTokenCount; }
    public void setInputTokenCount(Integer inputTokenCount) { this.inputTokenCount = inputTokenCount; }

    public Integer getOutputTokenCount() { return outputTokenCount; }
    public void setOutputTokenCount(Integer outputTokenCount) { this.outputTokenCount = outputTokenCount; }

    public Integer getTotalTokenCount() { return totalTokenCount; }
    public void setTotalTokenCount(Integer totalTokenCount) { this.totalTokenCount = totalTokenCount; }

    public String getCallStatus() { return callStatus; }
    public void setCallStatus(String callStatus) { this.callStatus = callStatus; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(String fallbackUsed) { this.fallbackUsed = fallbackUsed; }

    public String getFallbackProvider() { return fallbackProvider; }
    public void setFallbackProvider(String fallbackProvider) { this.fallbackProvider = fallbackProvider; }

    public String getFallbackModel() { return fallbackModel; }
    public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }

    public Integer getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Integer elapsedMs) { this.elapsedMs = elapsedMs; }

    public LocalDateTime getCalledTime() { return calledTime; }
    public void setCalledTime(LocalDateTime calledTime) { this.calledTime = calledTime; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
