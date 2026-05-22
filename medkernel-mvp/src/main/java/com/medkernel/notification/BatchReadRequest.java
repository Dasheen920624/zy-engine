package com.medkernel.notification;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 批量标记已读请求 DTO
 */
public class BatchReadRequest {
    private java.util.List<@NotBlank String> notificationCodes;

    public java.util.List<String> getNotificationCodes() { return notificationCodes; }
    public void setNotificationCodes(java.util.List<String> notificationCodes) { this.notificationCodes = notificationCodes; }
}
