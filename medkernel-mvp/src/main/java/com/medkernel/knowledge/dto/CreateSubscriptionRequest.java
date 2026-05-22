package com.medkernel.knowledge.dto;

import javax.validation.constraints.NotBlank;

/**
 * 创建知识订阅请求 DTO。
 */
public class CreateSubscriptionRequest {

    @NotBlank(message = "订阅主题不能为空")
    private String topicType;

    private String topicId;
    private String subscriberId;
    private String syncMode;
    private String tenantId;

    public String getTopicType() { return topicType; }
    public void setTopicType(String topicType) { this.topicType = topicType; }

    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }

    public String getSubscriberId() { return subscriberId; }
    public void setSubscriberId(String subscriberId) { this.subscriberId = subscriberId; }

    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
