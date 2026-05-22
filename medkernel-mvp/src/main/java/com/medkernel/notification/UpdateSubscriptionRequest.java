package com.medkernel.notification;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 更新订阅设置请求 DTO
 */
public class UpdateSubscriptionRequest {
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    private boolean enabled;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
