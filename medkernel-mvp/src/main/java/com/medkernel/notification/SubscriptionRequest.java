package com.medkernel.notification;

import javax.validation.constraints.NotBlank;

/**
 * 用户订阅设置请求 DTO
 */
public class SubscriptionRequest {
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "通知类型不能为空")
    private String notificationType;

    @NotBlank(message = "渠道不能为空")
    private String channel;

    private boolean enabled;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
