package com.medkernel.knowledge.dto;

import javax.validation.constraints.NotBlank;

/**
 * 更新任务状态请求 DTO。
 */
public class UpdateJobStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;

    private String errorCode;
    private String errorMessage;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
