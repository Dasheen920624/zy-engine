package com.medkernel.knowledge.dto;

import java.util.List;

import com.medkernel.knowledge.KnowledgeSubscription;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识订阅响应 DTO。
 */
@Schema(description = "知识订阅响应")
public class SubscriptionResponse {

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "订阅ID")
    private String subscriptionId;

    @Schema(description = "订阅者ID")
    private String subscriberId;

    @Schema(description = "订阅者名称")
    private String subscriberName;

    @Schema(description = "主题类型")
    private String topicType;

    @Schema(description = "主题编码")
    private String topicCode;

    @Schema(description = "主题名称")
    private String topicName;

    @Schema(description = "来源类型列表")
    private List<String> sourceTypes;

    @Schema(description = "是否自动同步")
    private boolean autoSync;

    @Schema(description = "同步频率")
    private String syncFrequency;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private String createdTime;

    @Schema(description = "更新时间")
    private String updatedTime;

    public static SubscriptionResponse fromEntity(KnowledgeSubscription sub) {
        if (sub == null) {
            return null;
        }
        SubscriptionResponse resp = new SubscriptionResponse();
        resp.tenantId = sub.getTenantId();
        resp.subscriptionId = sub.getSubscriptionId();
        resp.subscriberId = sub.getSubscriberId();
        resp.subscriberName = sub.getSubscriberName();
        resp.topicType = sub.getTopicType();
        resp.topicCode = sub.getTopicCode();
        resp.topicName = sub.getTopicName();
        resp.sourceTypes = sub.getSourceTypes();
        resp.autoSync = sub.isAutoSync();
        resp.syncFrequency = sub.getSyncFrequency();
        resp.status = sub.getStatus();
        resp.createdBy = sub.getCreatedBy();
        resp.createdTime = sub.getCreatedTime();
        resp.updatedTime = sub.getUpdatedTime();
        return resp;
    }

    // Getters and Setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getSubscriberId() { return subscriberId; }
    public void setSubscriberId(String subscriberId) { this.subscriberId = subscriberId; }

    public String getSubscriberName() { return subscriberName; }
    public void setSubscriberName(String subscriberName) { this.subscriberName = subscriberName; }

    public String getTopicType() { return topicType; }
    public void setTopicType(String topicType) { this.topicType = topicType; }

    public String getTopicCode() { return topicCode; }
    public void setTopicCode(String topicCode) { this.topicCode = topicCode; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public List<String> getSourceTypes() { return sourceTypes; }
    public void setSourceTypes(List<String> sourceTypes) { this.sourceTypes = sourceTypes; }

    public boolean isAutoSync() { return autoSync; }
    public void setAutoSync(boolean autoSync) { this.autoSync = autoSync; }

    public String getSyncFrequency() { return syncFrequency; }
    public void setSyncFrequency(String syncFrequency) { this.syncFrequency = syncFrequency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }

    public String getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(String updatedTime) { this.updatedTime = updatedTime; }
}
