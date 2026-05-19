package com.medkernel.notification;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 通知控制器
 * 提供通知的 REST API
 */
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

    /**
     * 创建通知
     */
    @PostMapping
    public ApiResult<Map<String, Object>> createNotification(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest request) {
        organizationContextService.applyExplicitFilters(body, request);
        Notification notification = notificationService.createNotification(body, 
                organizationContextService.getContext(request));
        return ApiResult.success(notificationToMap(notification));
    }

    /**
     * 查询通知列表
     */
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
        
        List<Notification> notifications = notificationService.listNotifications(filters);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification notification : notifications) {
            result.add(notificationToMap(notification));
        }
        return ApiResult.success(result);
    }

    /**
     * 获取通知详情
     */
    @GetMapping("/{notificationCode}")
    public ApiResult<Map<String, Object>> getNotification(@PathVariable String notificationCode,
                                                           HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("notificationCode", notificationCode);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        Notification notification = notificationService.getNotification(notificationCode);
        return ApiResult.success(notificationToMap(notification));
    }

    /**
     * 标记为已读
     */
    @PostMapping("/{notificationCode}/read")
    public ApiResult<Map<String, Object>> markAsRead(@PathVariable String notificationCode,
                                                      HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        Notification notification = notificationService.markAsRead(notificationCode);
        return ApiResult.success(notificationToMap(notification));
    }

    /**
     * 批量标记为已读
     */
    @PostMapping("/batch-read")
    public ApiResult<Map<String, Object>> batchMarkAsRead(@RequestBody Map<String, Object> body,
                                                           HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, body);
        List<String> notificationCodes = (List<String>) body.get("notificationCodes");
        int count = notificationService.batchMarkAsRead(notificationCodes);
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", count);
        return ApiResult.success(result);
    }

    /**
     * 归档通知
     */
    @PostMapping("/{notificationCode}/archive")
    public ApiResult<Map<String, Object>> archiveNotification(@PathVariable String notificationCode,
                                                               HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        Notification notification = notificationService.archiveNotification(notificationCode);
        return ApiResult.success(notificationToMap(notification));
    }

    /**
     * 获取未读通知数量
     */
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
    @PostMapping("/cleanup")
    public ApiResult<Map<String, Object>> cleanupExpiredNotifications(HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        int count = notificationService.cleanupExpiredNotifications();
        Map<String, Object> result = new HashMap<>();
        result.put("cleanedCount", count);
        return ApiResult.success(result);
    }

    /**
     * 将通知实体转换为Map
     */
    private Map<String, Object> notificationToMap(Notification notification) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", notification.getId());
        map.put("notificationCode", notification.getNotificationCode());
        map.put("title", notification.getTitle());
        map.put("content", notification.getContent());
        map.put("notificationType", notification.getNotificationType());
        map.put("priority", notification.getPriority());
        map.put("status", notification.getStatus());
        map.put("senderId", notification.getSenderId());
        map.put("senderName", notification.getSenderName());
        map.put("recipientId", notification.getRecipientId());
        map.put("recipientName", notification.getRecipientName());
        map.put("businessType", notification.getBusinessType());
        map.put("businessId", notification.getBusinessId());
        map.put("businessUrl", notification.getBusinessUrl());
        map.put("channel", notification.getChannel());
        map.put("scheduledTime", notification.getScheduledTime());
        map.put("sentTime", notification.getSentTime());
        map.put("readTime", notification.getReadTime());
        map.put("expireTime", notification.getExpireTime());
        map.put("createdTime", notification.getCreatedTime());
        map.put("updatedTime", notification.getUpdatedTime());
        return map;
    }
}