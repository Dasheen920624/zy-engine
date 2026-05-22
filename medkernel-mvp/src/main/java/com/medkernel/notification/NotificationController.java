package com.medkernel.notification;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 * 提供通知的 REST API，包含通知主 CRUD、渠道配置、模板、订阅设置、投递日志
 */
@Tag(name = "Notification")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final OrganizationContextService organizationContextService;

    public NotificationController(NotificationService notificationService,
                                  OrganizationContextService organizationContextService) {
        this.notificationService = notificationService;
        this.organizationContextService = organizationContextService;
    }

    // =========================================================================
    // 通知主 CRUD
    // =========================================================================

    /**
     * 创建通知
     */
    @Operation(summary = "Create notification")
    @PostMapping
    public ApiResult<Map<String, Object>> createNotification(
            @Valid @RequestBody CreateNotificationRequest body,
            HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        Map<String, Object> params = toMap(body);
        Map<String, Object> notification = notificationService.createNotification(params, orgContext);
        return ApiResult.success(camelCaseNotification(notification));
    }

    /**
     * 查询通知列表
     */
    @Operation(summary = "List notifications")
    @GetMapping
    public ApiResult<List<Map<String, Object>>> listNotifications(
            @RequestParam(required = false) String recipientId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String notificationType,
            @RequestParam(required = false) String priority,
            HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("recipientId", recipientId);
        filters.put("status", status);
        filters.put("notificationType", notificationType);
        filters.put("priority", priority);
        organizationContextService.applyExplicitFilters(filters, request);

        List<Map<String, Object>> notifications = notificationService.listNotifications(filters);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> notification : notifications) {
            result.add(camelCaseNotification(notification));
        }
        return ApiResult.success(result);
    }

    /**
     * 获取通知详情
     */
    @Operation(summary = "Get notification")
    @GetMapping("/{notificationCode}")
    public ApiResult<Map<String, Object>> getNotification(@PathVariable String notificationCode,
                                                           HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        Map<String, Object> notification = notificationService.getNotification(tenantId, notificationCode);
        return ApiResult.success(camelCaseNotification(notification));
    }

    /**
     * 标记为已读
     */
    @Operation(summary = "Mark as read")
    @PostMapping("/{notificationCode}/read")
    public ApiResult<Map<String, Object>> markAsRead(@PathVariable String notificationCode,
                                                      HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Map<String, Object> notification = notificationService.markAsRead(orgContext.getTenantId(), notificationCode);
        return ApiResult.success(camelCaseNotification(notification));
    }

    /**
     * 批量标记为已读
     */
    @Operation(summary = "Batch mark as read")
    @PostMapping("/batch-read")
    public ApiResult<Map<String, Object>> batchMarkAsRead(
            @Valid @RequestBody BatchReadRequest body,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        int count = notificationService.batchMarkAsRead(orgContext.getTenantId(), body.getNotificationCodes());
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", count);
        return ApiResult.success(result);
    }

    /**
     * 归档通知
     */
    @Operation(summary = "Archive notification")
    @PostMapping("/{notificationCode}/archive")
    public ApiResult<Map<String, Object>> archiveNotification(@PathVariable String notificationCode,
                                                               HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Map<String, Object> notification = notificationService.archiveNotification(orgContext.getTenantId(), notificationCode);
        return ApiResult.success(camelCaseNotification(notification));
    }

    /**
     * 获取未读通知数量
     */
    @Operation(summary = "Get unread count")
    @GetMapping("/unread-count")
    public ApiResult<Map<String, Object>> getUnreadCount(
            @RequestParam String recipientId,
            HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, request);
        String tenantId = filters.getOrDefault("tenantId", "default");

        long count = notificationService.getUnreadCount(tenantId, recipientId);
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", count);
        return ApiResult.success(result);
    }

    /**
     * 获取通知统计
     */
    @Operation(summary = "Get notification summary")
    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> getNotificationSummary(
            @RequestParam String recipientId,
            HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, request);
        String tenantId = filters.getOrDefault("tenantId", "default");

        Map<String, Object> summary = notificationService.getNotificationSummary(tenantId, recipientId);
        return ApiResult.success(summary);
    }

    /**
     * 清理过期通知
     */
    @Operation(summary = "Cleanup expired notifications")
    @PostMapping("/cleanup")
    public ApiResult<Map<String, Object>> cleanupExpiredNotifications(HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        int count = notificationService.cleanupExpiredNotifications();
        Map<String, Object> result = new HashMap<>();
        result.put("cleanedCount", count);
        return ApiResult.success(result);
    }

    // =========================================================================
    // 渠道配置
    // =========================================================================

    /**
     * 保存渠道配置
     */
    @Operation(summary = "Save channel config")
    @PostMapping("/channels")
    public ApiResult<Map<String, Object>> saveChannelConfig(
            @Valid @RequestBody ChannelConfigRequest body,
            HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        Map<String, Object> params = toMap(body);
        notificationService.saveChannelConfig(params, orgContext);
        return ApiResult.success(params);
    }

    /**
     * 查询渠道配置列表
     */
    @Operation(summary = "List channel configs")
    @GetMapping("/channels")
    public ApiResult<List<Map<String, Object>>> listChannelConfigs(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, request);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(notificationService.listChannelConfigs(tenantId));
    }

    /**
     * 获取单条渠道配置
     */
    @Operation(summary = "Get channel config")
    @GetMapping("/channels/{channelCode}")
    public ApiResult<Map<String, Object>> getChannelConfig(@PathVariable String channelCode,
                                                            HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, request);
        String tenantId = filters.getOrDefault("tenantId", "default");
        Map<String, Object> config = notificationService.getChannelConfig(tenantId, channelCode);
        return ApiResult.success(config);
    }

    // =========================================================================
    // 通知模板
    // =========================================================================

    /**
     * 保存通知模板
     */
    @Operation(summary = "Save notification template")
    @PostMapping("/templates")
    public ApiResult<Map<String, Object>> saveTemplate(
            @Valid @RequestBody TemplateRequest body,
            HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        Map<String, Object> params = toMap(body);
        notificationService.saveTemplate(params, orgContext);
        return ApiResult.success(params);
    }

    /**
     * 查询通知模板列表
     */
    @Operation(summary = "List notification templates")
    @GetMapping("/templates")
    public ApiResult<List<Map<String, Object>>> listTemplates(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, request);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(notificationService.listTemplates(tenantId));
    }

    /**
     * 获取单条通知模板
     */
    @Operation(summary = "Get notification template")
    @GetMapping("/templates/{templateCode}")
    public ApiResult<Map<String, Object>> getTemplate(@PathVariable String templateCode,
                                                       @RequestParam(required = false, defaultValue = "IN_APP") String channel,
                                                       HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, request);
        String tenantId = filters.getOrDefault("tenantId", "default");
        Map<String, Object> template = notificationService.getTemplate(tenantId, templateCode, channel);
        return ApiResult.success(template);
    }

    // =========================================================================
    // 用户订阅设置
    // =========================================================================

    /**
     * 保存用户订阅设置
     */
    @Operation(summary = "Save subscription")
    @PostMapping("/subscriptions")
    public ApiResult<Map<String, Object>> saveSubscription(
            @Valid @RequestBody SubscriptionRequest body,
            HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        Map<String, Object> params = toMap(body);
        notificationService.saveSubscription(params, orgContext);
        return ApiResult.success(params);
    }

    /**
     * 批量保存用户订阅设置
     */
    @Operation(summary = "Batch save subscriptions")
    @PostMapping("/subscriptions/batch")
    public ApiResult<Map<String, Object>> batchSaveSubscriptions(
            @Valid @RequestBody BatchSubscriptionRequest body,
            HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        List<Map<String, Object>> subscriptionMaps = new ArrayList<>();
        if (body.getSubscriptions() != null) {
            for (SubscriptionRequest sub : body.getSubscriptions()) {
                subscriptionMaps.add(toMap(sub));
            }
        }
        notificationService.batchSaveSubscriptions(subscriptionMaps, orgContext);
        Map<String, Object> result = new HashMap<>();
        result.put("savedCount", subscriptionMaps.size());
        return ApiResult.success(result);
    }

    /**
     * 查询用户订阅设置列表
     */
    @Operation(summary = "List subscriptions")
    @GetMapping("/subscriptions")
    public ApiResult<List<Map<String, Object>>> listSubscriptions(
            @RequestParam String userId,
            HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, request);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(notificationService.listSubscriptions(tenantId, userId));
    }

    /**
     * 更新用户订阅设置
     */
    @Operation(summary = "Update subscription")
    @PutMapping("/subscriptions/{notificationType}/{channel}")
    public ApiResult<Map<String, Object>> updateSubscription(
            @PathVariable String notificationType,
            @PathVariable String channel,
            @Valid @RequestBody UpdateSubscriptionRequest body,
            HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        notificationService.updateSubscription(orgContext.getTenantId(), body.getUserId(),
                notificationType, channel, body.isEnabled());
        Map<String, Object> result = new HashMap<>();
        result.put("updated", true);
        return ApiResult.success(result);
    }

    // =========================================================================
    // 投递日志
    // =========================================================================

    /**
     * 查询投递日志
     */
    @Operation(summary = "List delivery logs")
    @GetMapping("/{notificationId}/delivery-logs")
    public ApiResult<List<Map<String, Object>>> listDeliveryLogs(@PathVariable Long notificationId,
                                                                   HttpServletRequest request) {
        organizationContextService.resolve(request);
        return ApiResult.success(notificationService.listDeliveryLogs(notificationId));
    }

    // =========================================================================
    // 工作流联动
    // =========================================================================

    /**
     * 从工作流事件创建通知
     */
    @Operation(summary = "Create workflow notification")
    @PostMapping("/workflow")
    public ApiResult<Map<String, Object>> createWorkflowNotification(
            @Valid @RequestBody WorkflowNotificationRequest body,
            HttpServletRequest request) {
        OrganizationContext orgContext = organizationContextService.resolve(request);
        String createdBy = body.getCreatedBy() != null ? body.getCreatedBy() : orgContext.getUserId();

        Map<String, Object> notification = notificationService.createWorkflowNotification(
                orgContext.getTenantId(), orgContext.getOrgCode(),
                body.getTaskCode(), body.getBusinessType() != null ? body.getBusinessType() : "REVIEW",
                body.getTitle() != null ? body.getTitle() : "新待办任务",
                body.getDescription() != null ? body.getDescription() : "",
                body.getAssignedTo(), createdBy);
        return ApiResult.success(camelCaseNotification(notification));
    }

    // =========================================================================
    // DTO → Map 转换
    // =========================================================================

    private Map<String, Object> toMap(CreateNotificationRequest req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", req.getTitle());
        map.put("content", req.getContent());
        map.put("notificationType", req.getNotificationType());
        map.put("priority", req.getPriority());
        map.put("senderId", req.getSenderId());
        map.put("senderName", req.getSenderName());
        map.put("recipientId", req.getRecipientId());
        map.put("recipientName", req.getRecipientName());
        map.put("businessType", req.getBusinessType());
        map.put("businessId", req.getBusinessId());
        map.put("businessUrl", req.getBusinessUrl());
        map.put("channel", req.getChannel());
        map.put("scheduledTime", req.getScheduledTime());
        map.put("expireTime", req.getExpireTime());
        return map;
    }

    private Map<String, Object> toMap(ChannelConfigRequest req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("channel_code", req.getChannelCode());
        map.put("channel_name", req.getChannelName());
        map.put("channel_type", req.getChannelType());
        map.put("enabled", req.isEnabled());
        map.put("config_json", req.getConfigJson());
        return map;
    }

    private Map<String, Object> toMap(TemplateRequest req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("template_code", req.getTemplateCode());
        map.put("template_name", req.getTemplateName());
        map.put("template_type", req.getTemplateType());
        map.put("title_template", req.getTitleTemplate());
        map.put("content_template", req.getContentTemplate());
        map.put("channel", req.getChannel());
        map.put("enabled", req.isEnabled());
        return map;
    }

    private Map<String, Object> toMap(SubscriptionRequest req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user_id", req.getUserId());
        map.put("notification_type", req.getNotificationType());
        map.put("channel", req.getChannel());
        map.put("enabled", req.isEnabled());
        return map;
    }

    // =========================================================================
    // 字段名转换（snake_case → camelCase）
    // =========================================================================

    private Map<String, Object> camelCaseNotification(Map<String, Object> notification) {
        if (notification == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", notification.get("id"));
        map.put("notificationCode", notification.get("notification_code"));
        map.put("tenantId", notification.get("tenant_id"));
        map.put("orgCode", notification.get("org_code"));
        map.put("title", notification.get("title"));
        map.put("content", notification.get("content"));
        map.put("notificationType", notification.get("notification_type"));
        map.put("priority", notification.get("priority"));
        map.put("status", notification.get("status"));
        map.put("senderId", notification.get("sender_id"));
        map.put("senderName", notification.get("sender_name"));
        map.put("recipientId", notification.get("recipient_id"));
        map.put("recipientName", notification.get("recipient_name"));
        map.put("businessType", notification.get("business_type"));
        map.put("businessId", notification.get("business_id"));
        map.put("businessUrl", notification.get("business_url"));
        map.put("channel", notification.get("channel"));
        map.put("scheduledTime", notification.get("scheduled_time"));
        map.put("sentTime", notification.get("sent_time"));
        map.put("readTime", notification.get("read_time"));
        map.put("expireTime", notification.get("expire_time"));
        map.put("retryCount", notification.get("retry_count"));
        map.put("maxRetries", notification.get("max_retries"));
        map.put("errorMessage", notification.get("error_message"));
        map.put("createdBy", notification.get("created_by"));
        map.put("createdTime", notification.get("created_time"));
        map.put("updatedTime", notification.get("updated_time"));
        return map;
    }
}
