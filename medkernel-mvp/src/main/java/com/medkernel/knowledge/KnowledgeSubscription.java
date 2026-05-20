package com.medkernel.knowledge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识订阅。
 * 用户可按疾病、科室、指南、医保、药品、质控主题订阅知识。
 * 对应数据库表：aik_knowledge_subscription
 */
public class KnowledgeSubscription {
    private String tenantId;
    private String subscriptionId;
    private String subscriberId;
    private String subscriberName;
    private String topicType;    // DISEASE, DEPARTMENT, GUIDELINE, INSURANCE, DRUG, QUALITY
    private String topicCode;
    private String topicName;
    private List<String> sourceTypes;
    private boolean autoSync;
    private String syncFrequency; // DAILY, WEEKLY, MONTHLY, MANUAL
    private String status;        // ACTIVE, PAUSED, CANCELLED
    private String createdBy;
    private String createdTime;
    private String updatedTime;

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

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("tenant_id", tenantId);
        view.put("subscription_id", subscriptionId);
        view.put("subscriber_id", subscriberId);
        view.put("subscriber_name", subscriberName);
        view.put("topic_type", topicType);
        view.put("topic_code", topicCode);
        view.put("topic_name", topicName);
        view.put("source_types", sourceTypes);
        view.put("auto_sync", autoSync);
        view.put("sync_frequency", syncFrequency);
        view.put("status", status);
        view.put("created_by", createdBy);
        view.put("created_time", createdTime);
        view.put("updated_time", updatedTime);
        return view;
    }
}
