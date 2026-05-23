package com.medkernel.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识订阅查询请求 DTO：用于 KnowledgeController.listSubscriptions 查询参数。
 */
@Schema(description = "知识订阅查询请求")
public class SubscriptionQueryRequest {

    @Schema(description = "主题类型")
    private String topicType;

    @Schema(description = "订阅状态")
    private String status;

    @Schema(description = "订阅者ID")
    private String subscriberId;

    public String getTopicType() { return topicType; }
    public void setTopicType(String topicType) { this.topicType = topicType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubscriberId() { return subscriberId; }
    public void setSubscriberId(String subscriberId) { this.subscriberId = subscriberId; }
}
