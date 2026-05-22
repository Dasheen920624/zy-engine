package com.medkernel.notification;

import com.medkernel.common.ErrorCode;
import com.medkernel.common.exception.BusinessException;
import com.medkernel.organization.OrganizationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 通知服务
 * 提供通知的创建、查询、标记已读、归档等功能
 */
@Service
public class NotificationService {
    private final Map<String, Notification> notificationStore = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    /**
     * 创建通知
     */
    public Notification createNotification(Map<String, Object> request, OrganizationContext orgContext) {
        Notification notification = new Notification();
        notification.setId(sequence.incrementAndGet());
        notification.setTenantId(orgContext.getTenantId());
        notification.setOrgCode(orgContext.getOrgCode());
        notification.setNotificationCode("NOTIFY-" + System.currentTimeMillis());
        notification.setTitle((String) request.getOrDefault("title", ""));
        notification.setContent((String) request.getOrDefault("content", ""));
        notification.setNotificationType((String) request.getOrDefault("notificationType", "SYSTEM"));
        notification.setPriority((String) request.getOrDefault("priority", "NORMAL"));
        notification.setStatus("UNREAD");
        notification.setSenderId((String) request.get("senderId"));
        notification.setSenderName((String) request.get("senderName"));
        notification.setRecipientId((String) request.getOrDefault("recipientId", ""));
        notification.setRecipientName((String) request.get("recipientName"));
        notification.setBusinessType((String) request.get("businessType"));
        notification.setBusinessId((String) request.get("businessId"));
        notification.setBusinessUrl((String) request.get("businessUrl"));
        notification.setChannel((String) request.getOrDefault("channel", "IN_APP"));
        notification.setRetryCount(0);
        notification.setMaxRetries(3);
        notification.setCreatedBy(orgContext.getUserId());
        notification.setCreatedTime(LocalDateTime.now());

        // 处理定时发送
        if (request.containsKey("scheduledTime")) {
            notification.setScheduledTime(LocalDateTime.parse((String) request.get("scheduledTime")));
        }

        // 处理过期时间
        if (request.containsKey("expireTime")) {
            notification.setExpireTime(LocalDateTime.parse((String) request.get("expireTime")));
        }

        notificationStore.put(notification.getNotificationCode(), notification);
        return notification;
    }

    /**
     * 查询通知列表
     */
    public List<Notification> listNotifications(Map<String, String> filters) {
        String tenantId = filters.get("tenantId");
        String recipientId = filters.get("recipientId");
        String status = filters.get("status");
        String notificationType = filters.get("notificationType");
        String priority = filters.get("priority");

        return notificationStore.values().stream()
                .filter(n -> tenantId == null || tenantId.equals(n.getTenantId()))
                .filter(n -> recipientId == null || recipientId.equals(n.getRecipientId()))
                .filter(n -> status == null || status.equals(n.getStatus()))
                .filter(n -> notificationType == null || notificationType.equals(n.getNotificationType()))
                .filter(n -> priority == null || priority.equals(n.getPriority()))
                .sorted(Comparator.comparing(Notification::getCreatedTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 获取通知详情
     */
    public Notification getNotification(String notificationCode) {
        Notification notification = notificationStore.get(notificationCode);
        if (notification == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "通知不存在: " + notificationCode);
        }
        return notification;
    }

    /**
     * 标记为已读
     */
    public Notification markAsRead(String notificationCode) {
        Notification notification = getNotification(notificationCode);
        notification.setStatus("READ");
        notification.setReadTime(LocalDateTime.now());
        notification.setUpdatedTime(LocalDateTime.now());
        return notification;
    }

    /**
     * 批量标记为已读
     */
    public int batchMarkAsRead(List<String> notificationCodes) {
        int count = 0;
        for (String code : notificationCodes) {
            try {
                markAsRead(code);
                count++;
            } catch (Exception e) {
                // 忽略不存在的通知
            }
        }
        return count;
    }

    /**
     * 归档通知
     */
    public Notification archiveNotification(String notificationCode) {
        Notification notification = getNotification(notificationCode);
        notification.setStatus("ARCHIVED");
        notification.setUpdatedTime(LocalDateTime.now());
        return notification;
    }

    /**
     * 获取未读通知数量
     */
    public long getUnreadCount(String tenantId, String recipientId) {
        return notificationStore.values().stream()
                .filter(n -> tenantId.equals(n.getTenantId()))
                .filter(n -> recipientId.equals(n.getRecipientId()))
                .filter(n -> "UNREAD".equals(n.getStatus()))
                .count();
    }

    /**
     * 获取通知统计
     */
    public Map<String, Object> getNotificationSummary(String tenantId, String recipientId) {
        Map<String, Object> summary = new HashMap<>();
        long total = notificationStore.values().stream()
                .filter(n -> tenantId.equals(n.getTenantId()))
                .filter(n -> recipientId.equals(n.getRecipientId()))
                .count();
        long unread = getUnreadCount(tenantId, recipientId);
        long read = notificationStore.values().stream()
                .filter(n -> tenantId.equals(n.getTenantId()))
                .filter(n -> recipientId.equals(n.getRecipientId()))
                .filter(n -> "READ".equals(n.getStatus()))
                .count();
        long archived = notificationStore.values().stream()
                .filter(n -> tenantId.equals(n.getTenantId()))
                .filter(n -> recipientId.equals(n.getRecipientId()))
                .filter(n -> "ARCHIVED".equals(n.getStatus()))
                .count();

        summary.put("total", total);
        summary.put("unread", unread);
        summary.put("read", read);
        summary.put("archived", archived);
        return summary;
    }

    /**
     * 删除过期通知
     */
    public int cleanupExpiredNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredCodes = notificationStore.values().stream()
                .filter(n -> n.getExpireTime() != null && n.getExpireTime().isBefore(now))
                .map(Notification::getNotificationCode)
                .collect(Collectors.toList());

        expiredCodes.forEach(notificationStore::remove);
        return expiredCodes.size();
    }
}