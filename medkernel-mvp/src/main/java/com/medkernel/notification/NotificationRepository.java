package com.medkernel.notification;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class NotificationRepository {

    private final NotificationCrudRepository notificationCrudRepository;
    private final ChannelConfigRepository channelConfigRepository;
    private final TemplateRepository templateRepository;
    private final SubscriptionAndDeliveryRepository subscriptionAndDeliveryRepository;

    public NotificationRepository(NotificationCrudRepository notificationCrudRepository,
                                  ChannelConfigRepository channelConfigRepository,
                                  TemplateRepository templateRepository,
                                  SubscriptionAndDeliveryRepository subscriptionAndDeliveryRepository) {
        this.notificationCrudRepository = notificationCrudRepository;
        this.channelConfigRepository = channelConfigRepository;
        this.templateRepository = templateRepository;
        this.subscriptionAndDeliveryRepository = subscriptionAndDeliveryRepository;
    }

    // =========================================================================
    // NOTIFY_NOTIFICATION CRUD（委托 NotificationCrudRepository）
    // =========================================================================

    /** 保存通知（UPSERT），唯一键：tenant_id + notification_code。 */
    public void saveNotification(Map<String, Object> notification) {
        notificationCrudRepository.saveNotification(notification);
    }

    /** 查询通知列表，支持 tenantId/recipientId/status/notificationType/priority 过滤，按 created_time DESC 排序。 */
    public List<Map<String, Object>> listNotifications(Map<String, String> filters) {
        return notificationCrudRepository.listNotifications(filters);
    }

    /** 获取单条通知，找不到返回 null。 */
    public Map<String, Object> getNotification(String tenantId, String notificationCode) {
        return notificationCrudRepository.getNotification(tenantId, notificationCode);
    }

    /** 更新通知状态，支持额外字段（如 read_time, sent_time 等），更新 updated_time。 */
    public void updateNotificationStatus(String tenantId, String notificationCode, String status,
                                         Map<String, Object> extraFields) {
        notificationCrudRepository.updateNotificationStatus(tenantId, notificationCode, status, extraFields);
    }

    /** 获取未读通知数量。 */
    public long getUnreadCount(String tenantId, String recipientId) {
        return notificationCrudRepository.getUnreadCount(tenantId, recipientId);
    }

    /** 获取通知统计：total/unread/read/archived。 */
    public Map<String, Object> getNotificationSummary(String tenantId, String recipientId) {
        return notificationCrudRepository.getNotificationSummary(tenantId, recipientId);
    }

    /** 删除过期通知，返回删除数量。 */
    public int cleanupExpiredNotifications() {
        return notificationCrudRepository.cleanupExpiredNotifications();
    }

    // =========================================================================
    // NOTIFY_CHANNEL_CONFIG CRUD（委托 ChannelConfigRepository）
    // =========================================================================

    /** 保存渠道配置（UPSERT），唯一键：tenant_id + channel_code。 */
    public void saveChannelConfig(Map<String, Object> config) {
        channelConfigRepository.saveChannelConfig(config);
    }

    /** 查询渠道配置列表。 */
    public List<Map<String, Object>> listChannelConfigs(String tenantId) {
        return channelConfigRepository.listChannelConfigs(tenantId);
    }

    /** 获取单条渠道配置。 */
    public Map<String, Object> getChannelConfig(String tenantId, String channelCode) {
        return channelConfigRepository.getChannelConfig(tenantId, channelCode);
    }

    // =========================================================================
    // NOTIFY_TEMPLATE CRUD（委托 TemplateRepository）
    // =========================================================================

    /** 保存通知模板（UPSERT），唯一键：tenant_id + template_code + channel。 */
    public void saveTemplate(Map<String, Object> template) {
        templateRepository.saveTemplate(template);
    }

    /** 查询通知模板列表。 */
    public List<Map<String, Object>> listTemplates(String tenantId) {
        return templateRepository.listTemplates(tenantId);
    }

    /** 获取单条通知模板。 */
    public Map<String, Object> getTemplate(String tenantId, String templateCode, String channel) {
        return templateRepository.getTemplate(tenantId, templateCode, channel);
    }

    // =========================================================================
    // NOTIFY_SUBSCRIPTION CRUD（委托 SubscriptionAndDeliveryRepository）
    // =========================================================================

    /** 保存用户订阅设置（UPSERT），唯一键：tenant_id + user_id + notification_type + channel。 */
    public void saveSubscription(Map<String, Object> subscription) {
        subscriptionAndDeliveryRepository.saveSubscription(subscription);
    }

    /** 查询用户订阅设置列表。 */
    public List<Map<String, Object>> listSubscriptions(String tenantId, String userId) {
        return subscriptionAndDeliveryRepository.listSubscriptions(tenantId, userId);
    }

    /** 更新用户订阅设置（enabled 字段）。 */
    public void updateSubscription(String tenantId, String userId, String notificationType,
                                   String channel, boolean enabled) {
        subscriptionAndDeliveryRepository.updateSubscription(tenantId, userId, notificationType, channel, enabled);
    }

    // =========================================================================
    // NOTIFY_DELIVERY_LOG CRUD（委托 SubscriptionAndDeliveryRepository）
    // =========================================================================

    /** 保存投递日志（仅 INSERT）。 */
    public void saveDeliveryLog(Map<String, Object> log) {
        subscriptionAndDeliveryRepository.saveDeliveryLog(log);
    }

    /** 查询投递日志列表。 */
    public List<Map<String, Object>> listDeliveryLogs(long notificationId) {
        return subscriptionAndDeliveryRepository.listDeliveryLogs(notificationId);
    }
}
