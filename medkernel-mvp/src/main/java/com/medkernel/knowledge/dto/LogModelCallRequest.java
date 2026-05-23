package com.medkernel.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 记录模型调用请求 DTO：用于 AiKnowledgeJobController.logModelCall。
 */
@Schema(description = "记录模型调用请求")
public class LogModelCallRequest {

    @Schema(description = "关联任务ID")
    private Long jobId;

    @NotBlank(message = "调用类型不能为空")
    @Schema(description = "调用类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String callType;

    @NotBlank(message = "模型提供商不能为空")
    @Schema(description = "模型提供商", requiredMode = Schema.RequiredMode.REQUIRED)
    private String modelProvider;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "输入Token数")
    private Integer inputTokens;

    @Schema(description = "输出Token数")
    private Integer outputTokens;

    @NotNull(message = "耗时不能为空")
    @Schema(description = "耗时（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long elapsedMs;

    @NotBlank(message = "调用状态不能为空")
    @Schema(description = "调用状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String callStatus;

    @Schema(description = "错误编码")
    private String errorCode;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "请求载荷")
    private String requestPayload;

    @Schema(description = "响应载荷")
    private String responsePayload;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }

    public String getCallStatus() { return callStatus; }
    public void setCallStatus(String callStatus) { this.callStatus = callStatus; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }

    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
}
