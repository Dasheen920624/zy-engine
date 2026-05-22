package com.medkernel.notification;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建通知请求 DTO
 */
public class CreateNotificationRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长200字符")
    private String title;

    @Size(max = 2000, message = "内容最长2000字符")
    private String content;

    private String notificationType;
    private String priority;
    private String senderId;
    private String senderName;

    @NotBlank(message = "接收人不能为空")
    private String recipientId;

    private String recipientName;
    private String businessType;
    private String businessId;
    private String businessUrl;
    private String channel;
    private String scheduledTime;
    private String expireTime;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public String getBusinessUrl() { return businessUrl; }
    public void setBusinessUrl(String businessUrl) { this.businessUrl = businessUrl; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }
    public String getExpireTime() { return expireTime; }
    public void setExpireTime(String expireTime) { this.expireTime = expireTime; }
}
