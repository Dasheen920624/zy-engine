package com.medkernel.knowledge.dto;

import javax.validation.constraints.NotBlank;

/**
 * 触发同步请求 DTO。
 */
public class TriggerSyncRequest {

    @NotBlank(message = "来源编码不能为空")
    private String sourceCode;

    private String subscriptionId;

    @NotBlank(message = "同步模式不能为空")
    private String syncMode;

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
}
