package com.medkernel.notification;

import com.medkernel.common.ErrorCode;
import com.medkernel.common.exception.BusinessException;
import com.medkernel.organization.OrganizationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 通知服务
 * 提供通知的创建、查询、标记已读、归档等功能
 * 优先使用 NotificationRepository 持久化，未启用时回退到内存存储
 */
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    // 内存回退存储（持久化未启用时使用）
    private final Map<String, Map<String, Object>> memoryStore = new ConcurrentHashMap<>();

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    private boolean usePersistence() {
        return notificationRepository.enabled();
    }

    /**
     * 创建通知
     */
    public Map<String, Object> createNotification(Map<String, Object> request, OrganizationContext orgContext) {
        String notificationCode = "NOTIFY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        Map<String, Object> notification = new LinkedHashMap<String, Object>();
        notification.put("notification_code", notificationCode);
        notification.put("tenant_id", orgContext.getTenantId());
        notification.put("org_code", orgContext.getOrgCode());
        notification.put("title", request.getOrDefault("title", ""));
        notification.put("content", request.getOrDefault("content", ""));
        notification.put("notification_type", request.getOrDefault("notificationType", "SYSTEM"));
        notification.put("priority", request.getOrDefault("priority", "NORMAL"));
        notification.put("status", "UNREAD");
        notification.put("sender_id", request.get("senderId"));
        notification.put("sender_name", request.get("senderName"));
        notification.put("recipient_id", request.getOrDefault("recipientId", ""));
        notification.put("recipient_name", request.get("recipientName"));
        notification.put("business_type", request.get("businessType"));
        notification.put("business_id", request.get("businessId"));
        notification.put("business_url", request.get("businessUrl"));
        notification.put("channel", request.getOrDefault("channel", "IN_APP"));
        notification.put("retry_count", 0);
        notification.put("max_retries", 3);
        notification.put("created_by", orgContext.getUserId());
        notification.put("created_time", now);
        notification.put("updated_time", now);

        if (request.containsKey("scheduledTime")) {
            notification.put("scheduled_time", request.get("scheduledTime"));
        }
        if (request.containsKey("expireTime")) {
            notification.put("expire_time", request.get("expireTime"));
        }

        if (usePersistence()) {
            notificationRepository.saveNotification(notification);
        } else {
            memoryStore.put(notificationCode, notification);
        }

        return notification;
    }

    /**
     * 查询通知列表
     */
    public List<Map<String, Object>> listNotifications(Map<String, String> filters) {
        if (usePersistence()) {
            return notificationRepository.listNotifications(filters);
        }

        // 内存回退
        String tenantId = filters.get("tenantId");
        String recipientId = filters.get("recipientId");
        String status = filters.get("status");
        String notificationType = filters.get("notificationType");
        String priority = filters.get("priority");

        return memoryStore.values().stream()
                .filter(n -> tenantId == null || tenantId.equals(n.get("tenant_id")))
                .filter(n -> recipientId == null || recipientId.equals(n.get("recipient_id")))
                .filter(n -> status == null || status.equals(n.get("status")))
                .filter(n -> notificationType == null || notificationType.equals(n.get("notification_type")))
                .filter(n -> priority == null || priority.equals(n.get("priority")))
                .sorted((a, b) -> String.valueOf(b.get("created_time")).compareTo(String.valueOf(a.get("created_time"))))
                .collect(Collectors.toList());
    }

    /**
     * 获取通知详情
     */
    public Map<String, Object> getNotification(String tenantId, String notificationCode) {
        if (usePersistence()) {
            Map<String, Object> notification = notificationRepository.getNotification(tenantId, notificationCode);
            if (notification == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "通知不存在: " + notificationCode);
            }
            return notification;
        }

        Map<String, Object> notification = memoryStore.get(notificationCode);
        if (notification == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "通知不存在: " + notificationCode);
        }
        return notification;
    }

    /**
     * 标记为已读
     */
    public Map<String, Object> markAsRead(String tenantId, String notificationCode) {
        if (usePersistence()) {
            Map<String, Object> extra = new LinkedHashMap<String, Object>();
            extra.put("read_time", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            notificationRepository.updateNotificationStatus(tenantId, notificationCode, "READ", extra);
            return notificationRepository.getNotification(tenantId, notificationCode);
        }

        Map<String, Object> notification = memoryStore.get(notificationCode);
        if (notification == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "通知不存在: " + notificationCode);
        }
        notification.put("status", "READ");
        notification.put("read_time", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        notification.put("updated_time", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return notification;
    }

    /**
     * 批量标记为已读
     */
    public int batchMarkAsRead(String tenantId, List<String> notificationCodes) {
        int count = 0;
        for (String code : notificationCodes) {
            try {
                markAsRead(tenantId, code);
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
    public Map<String, Object> archiveNotification(String tenantId, String notificationCode) {
        if (usePersistence()) {
            notificationRepository.updateNotificationStatus(tenantId, notificationCode, "ARCHIVED", null);
            return notificationRepository.getNotification(tenantId, notificationCode);
        }

        Map<String, Object> notification = memoryStore.get(notificationCode);
        if (notification == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "通知不存在: " + notificationCode);
        }
        notification.put("status", "ARCHIVED");
        notification.put("updated_time", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return notification;
    }

    /**
     * 获取未读通知数量
     */
    public long getUnreadCount(String tenantId, String recipientId) {
        if (usePersistence()) {
            return notificationRepository.getUnreadCount(tenantId, recipientId);
        }

        return memoryStore.values().stream()
                .filter(n -> tenantId.equals(n.get("tenant_id")))
                .filter(n -> recipientId.equals(n.get("recipient_id")))
                .filter(n -> "UNREAD".equals(n.get("status")))
                .count();
    }

    /**
     * 获取通知统计
     */
    public Map<String, Object> getNotificationSummary(String tenantId, String recipientId) {
        if (usePersistence()) {
            return notificationRepository.getNotificationSummary(tenantId, recipientId);
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        long total = memoryStore.values().stream()
                .filter(n -> tenantId.equals(n.get("tenant_id")))
                .filter(n -> recipientId.equals(n.get("recipient_id")))
                .count();
        long unread = getUnreadCount(tenantId, recipientId);
        long read = memoryStore.values().stream()
                .filter(n -> tenantId.equals(n.get("tenant_id")))
                .filter(n -> recipientId.equals(n.get("recipient_id")))
                .filter(n -> "READ".equals(n.get("status")))
                .count();
        long archived = memoryStore.values().stream()
                .filter(n -> tenantId.equals(n.get("tenant_id")))
                .filter(n -> recipientId.equals(n.get("recipient_id")))
                .filter(n -> "ARCHIVED".equals(n.get("status")))
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
        if (usePersistence()) {
            return notificationRepository.cleanupExpiredNotifications();
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> expiredCodes = memoryStore.values().stream()
                .filter(n -> n.get("expire_time") != null)
                .filter(n -> {
                    try {
                        return LocalDateTime.parse(String.valueOf(n.get("expire_time")).substring(0, 19)).isBefore(now);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(n -> String.valueOf(n.get("notification_code")))
                .collect(Collectors.toList());

        expiredCodes.forEach(memoryStore::remove);
        return expiredCodes.size();
    }

    // =========================================================================
    // 渠道配置
    // =========================================================================

    /**
     * 保存渠道配置
     */
    public void saveChannelConfig(Map<String, Object> config, OrganizationContext orgContext) {
        config.put("tenant_id", orgContext.getTenantId());
        if (usePersistence()) {
            notificationRepository.saveChannelConfig(config);
        }
    }

    /**
     * 查询渠道配置列表
     */
    public List<Map<String, Object>> listChannelConfigs(String tenantId) {
        if (usePersistence()) {
            return notificationRepository.listChannelConfigs(tenantId);
        }
        return new ArrayList<Map<String, Object>>();
    }

    /**
     * 获取单条渠道配置
     */
    public Map<String, Object> getChannelConfig(String tenantId, String channelCode) {
        if (usePersistence()) {
            return notificationRepository.getChannelConfig(tenantId, channelCode);
        }
        return null;
    }

    // =========================================================================
    // 通知模板
    // =========================================================================

    /**
     * 保存通知模板
     */
    public void saveTemplate(Map<String, Object> template, OrganizationContext orgContext) {
        template.put("tenant_id", orgContext.getTenantId());
        if (usePersistence()) {
            notificationRepository.saveTemplate(template);
        }
    }

    /**
     * 查询通知模板列表
     */
    public List<Map<String, Object>> listTemplates(String tenantId) {
        if (usePersistence()) {
            return notificationRepository.listTemplates(tenantId);
        }
        return new ArrayList<Map<String, Object>>();
    }

    /**
     * 获取单条通知模板
     */
    public Map<String, Object> getTemplate(String tenantId, String templateCode, String channel) {
        if (usePersistence()) {
            return notificationRepository.getTemplate(tenantId, templateCode, channel);
        }
        return null;
    }

    // =========================================================================
    // 用户订阅设置
    // =========================================================================

    /**
     * 保存用户订阅设置
     */
    public void saveSubscription(Map<String, Object> subscription, OrganizationContext orgContext) {
        subscription.put("tenant_id", orgContext.getTenantId());
        if (usePersistence()) {
            notificationRepository.saveSubscription(subscription);
        }
    }

    /**
     * 批量保存用户订阅设置
     */
    public void batchSaveSubscriptions(List<Map<String, Object>> subscriptions, OrganizationContext orgContext) {
        for (Map<String, Object> subscription : subscriptions) {
            saveSubscription(subscription, orgContext);
        }
    }

    /**
     * 查询用户订阅设置列表
     */
    public List<Map<String, Object>> listSubscriptions(String tenantId, String userId) {
        if (usePersistence()) {
            return notificationRepository.listSubscriptions(tenantId, userId);
        }
        return new ArrayList<Map<String, Object>>();
    }

    /**
     * 更新用户订阅设置
     */
    public void updateSubscription(String tenantId, String userId, String notificationType,
                                   String channel, boolean enabled) {
        if (usePersistence()) {
            notificationRepository.updateSubscription(tenantId, userId, notificationType, channel, enabled);
        }
    }

    // =========================================================================
    // 投递日志
    // =========================================================================

    /**
     * 保存投递日志
     */
    public void saveDeliveryLog(Map<String, Object> log) {
        if (usePersistence()) {
            notificationRepository.saveDeliveryLog(log);
        }
    }

    /**
     * 查询投递日志列表
     */
    public List<Map<String, Object>> listDeliveryLogs(long notificationId) {
        if (usePersistence()) {
            return notificationRepository.listDeliveryLogs(notificationId);
        }
        return new ArrayList<Map<String, Object>>();
    }

    // =========================================================================
    // 工作流联动
    // =========================================================================

    /**
     * 从工作流事件创建通知
     * 当待办任务创建时自动触发
     */
    public Map<String, Object> createWorkflowNotification(String tenantId, String orgCode,
                                                          String taskCode, String businessType,
                                                          String title, String description,
                                                          String assignedTo, String createdBy) {
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("title", title);
        request.put("content", description);
        request.put("notificationType", "WORKFLOW");
        request.put("priority", "HIGH");
        request.put("recipientId", assignedTo);
        request.put("businessType", businessType);
        request.put("businessId", taskCode);
        request.put("businessUrl", "/workflow/todos/" + taskCode);
        request.put("channel", "IN_APP");

        String notificationCode = "NOTIFY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        Map<String, Object> notification = new LinkedHashMap<String, Object>();
        notification.put("notification_code", notificationCode);
        notification.put("tenant_id", tenantId);
        notification.put("org_code", orgCode);
        notification.put("title", title);
        notification.put("content", description);
        notification.put("notification_type", "WORKFLOW");
        notification.put("priority", "HIGH");
        notification.put("status", "UNREAD");
        notification.put("sender_id", createdBy);
        notification.put("sender_name", createdBy);
        notification.put("recipient_id", assignedTo);
        notification.put("business_type", businessType);
        notification.put("business_id", taskCode);
        notification.put("business_url", "/workflow/todos/" + taskCode);
        notification.put("channel", "IN_APP");
        notification.put("retry_count", 0);
        notification.put("max_retries", 3);
        notification.put("created_by", createdBy);
        notification.put("created_time", now);
        notification.put("updated_time", now);

        if (usePersistence()) {
            notificationRepository.saveNotification(notification);
        } else {
            memoryStore.put(notificationCode, notification);
        }

        return notification;
    }
}
