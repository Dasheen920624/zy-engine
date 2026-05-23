package com.medkernel.notification;

import com.medkernel.common.ErrorCode;
import com.medkernel.common.exception.BusinessException;
import com.medkernel.organization.OrganizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 单元测试")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DataSource dataSource;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, dataSource);
    }

    // ──────────────────────── 辅助方法 ────────────────────────

    private OrganizationContext buildOrgContext() {
        OrganizationContext ctx = new OrganizationContext();
        ctx.setTenantId("tenant1");
        ctx.setLegacyOrgCode("ORG001");
        return ctx;
    }

    private Map<String, Object> buildCreateRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", "测试通知");
        request.put("content", "这是一条测试通知内容");
        request.put("notificationType", "SYSTEM");
        request.put("priority", "NORMAL");
        request.put("senderId", "user001");
        request.put("senderName", "张三");
        request.put("recipientId", "user002");
        request.put("recipientName", "李四");
        request.put("businessType", "ORDER");
        request.put("businessId", "ORD-001");
        request.put("businessUrl", "/orders/ORD-001");
        request.put("channel", "IN_APP");
        return request;
    }

    // ──────────────────────── 通知创建 ────────────────────────

    @Nested
    @DisplayName("通知创建")
    class CreateNotificationTests {

        @Test
        @DisplayName("内存模式下创建通知成功")
        void createNotificationInMemoryMode() {
            when(notificationRepository.enabled()).thenReturn(false);

            Map<String, Object> result = notificationService.createNotification(
                    buildCreateRequest(), buildOrgContext());

            assertNotNull(result);
            assertTrue(((String) result.get("notification_code")).startsWith("NOTIFY-"));
            assertEquals("tenant1", result.get("tenant_id"));
            assertEquals("ORG001", result.get("org_code"));
            assertEquals("测试通知", result.get("title"));
            assertEquals("这是一条测试通知内容", result.get("content"));
            assertEquals("SYSTEM", result.get("notification_type"));
            assertEquals("NORMAL", result.get("priority"));
            assertEquals("UNREAD", result.get("status"));
            assertEquals("user002", result.get("recipient_id"));
            assertEquals("IN_APP", result.get("channel"));
            assertEquals(0, result.get("retry_count"));
            assertEquals(3, result.get("max_retries"));
            assertNotNull(result.get("created_time"));
            assertNotNull(result.get("updated_time"));
        }

        @Test
        @DisplayName("持久化模式下创建通知调用仓储保存")
        void createNotificationWithPersistence() {
            when(notificationRepository.enabled()).thenReturn(true);

            Map<String, Object> result = notificationService.createNotification(
                    buildCreateRequest(), buildOrgContext());

            verify(notificationRepository).saveNotification(any(Map.class));
            assertNotNull(result);
            assertEquals("UNREAD", result.get("status"));
        }

        @Test
        @DisplayName("创建通知时使用默认值")
        void createNotificationWithDefaults() {
            when(notificationRepository.enabled()).thenReturn(false);
            Map<String, Object> request = new LinkedHashMap<>();

            Map<String, Object> result = notificationService.createNotification(
                    request, buildOrgContext());

            assertEquals("", result.get("title"));
            assertEquals("", result.get("content"));
            assertEquals("SYSTEM", result.get("notification_type"));
            assertEquals("NORMAL", result.get("priority"));
            assertEquals("", result.get("recipient_id"));
            assertEquals("IN_APP", result.get("channel"));
        }

        @Test
        @DisplayName("创建通知时支持定时发送和过期时间")
        void createNotificationWithScheduledAndExpireTime() {
            when(notificationRepository.enabled()).thenReturn(false);
            Map<String, Object> request = buildCreateRequest();
            request.put("scheduledTime", "2026-06-01T10:00:00+08:00");
            request.put("expireTime", "2026-06-02T10:00:00+08:00");

            Map<String, Object> result = notificationService.createNotification(
                    request, buildOrgContext());

            assertEquals("2026-06-01T10:00:00+08:00", result.get("scheduled_time"));
            assertEquals("2026-06-02T10:00:00+08:00", result.get("expire_time"));
        }
    }

    // ──────────────────────── 通知查询 ────────────────────────

    @Nested
    @DisplayName("通知查询")
    class ListNotificationTests {

        @Test
        @DisplayName("内存模式下查询通知列表")
        void listNotificationsInMemoryMode() {
            when(notificationRepository.enabled()).thenReturn(false);
            notificationService.createNotification(buildCreateRequest(), buildOrgContext());

            Map<String, String> filters = new HashMap<>();
            filters.put("tenantId", "tenant1");
            List<Map<String, Object>> result = notificationService.listNotifications(filters);

            assertEquals(1, result.size());
            assertEquals("测试通知", result.get(0).get("title"));
        }

        @Test
        @DisplayName("内存模式下按收件人过滤通知")
        void listNotificationsFilterByRecipient() {
            when(notificationRepository.enabled()).thenReturn(false);
            notificationService.createNotification(buildCreateRequest(), buildOrgContext());

            Map<String, String> filters = new HashMap<>();
            filters.put("tenantId", "tenant1");
            filters.put("recipientId", "user002");
            List<Map<String, Object>> result = notificationService.listNotifications(filters);

            assertEquals(1, result.size());

            filters.put("recipientId", "user999");
            result = notificationService.listNotifications(filters);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("内存模式下按状态过滤通知")
        void listNotificationsFilterByStatus() {
            when(notificationRepository.enabled()).thenReturn(false);
            notificationService.createNotification(buildCreateRequest(), buildOrgContext());

            Map<String, String> filters = new HashMap<>();
            filters.put("tenantId", "tenant1");
            filters.put("status", "UNREAD");
            List<Map<String, Object>> result = notificationService.listNotifications(filters);

            assertEquals(1, result.size());

            filters.put("status", "READ");
            result = notificationService.listNotifications(filters);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("持久化模式下查询通知列表委托给仓储")
        void listNotificationsWithPersistence() {
            when(notificationRepository.enabled()).thenReturn(true);
            Map<String, String> filters = new HashMap<>();
            filters.put("tenantId", "tenant1");

            notificationService.listNotifications(filters);

            verify(notificationRepository).listNotifications(filters);
        }
    }

    // ──────────────────────── 获取通知详情 ────────────────────────

    @Nested
    @DisplayName("获取通知详情")
    class GetNotificationTests {

        @Test
        @DisplayName("内存模式下获取存在的通知")
        void getExistingNotificationInMemory() {
            when(notificationRepository.enabled()).thenReturn(false);
            Map<String, Object> created = notificationService.createNotification(
                    buildCreateRequest(), buildOrgContext());
            String code = (String) created.get("notification_code");

            Map<String, Object> result = notificationService.getNotification("tenant1", code);

            assertNotNull(result);
            assertEquals(code, result.get("notification_code"));
        }

        @Test
        @DisplayName("内存模式下获取不存在的通知抛出异常")
        void getNonExistentNotificationThrows() {
            when(notificationRepository.enabled()).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> notificationService.getNotification("tenant1", "NOTIFY-NONEXIST"));
            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("持久化模式下获取通知委托给仓储")
        void getNotificationWithPersistence() {
            when(notificationRepository.enabled()).thenReturn(true);
            when(notificationRepository.getNotification("tenant1", "NOTIFY-001"))
                    .thenReturn(buildCreateRequest());

            Map<String, Object> result = notificationService.getNotification("tenant1", "NOTIFY-001");

            verify(notificationRepository).getNotification("tenant1", "NOTIFY-001");
            assertNotNull(result);
        }

        @Test
        @DisplayName("持久化模式下通知不存在抛出异常")
        void getNotificationPersistenceNotFound() {
            when(notificationRepository.enabled()).thenReturn(true);
            when(notificationRepository.getNotification("tenant1", "NOTIFY-001")).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> notificationService.getNotification("tenant1", "NOTIFY-001"));
            assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ──────────────────────── 标记已读 ────────────────────────

    @Nested
    @DisplayName("标记已读")
    class MarkAsReadTests {

        @Test
        @DisplayName("内存模式下标记通知为已读")
        void markAsReadInMemory() {
            when(notificationRepository.enabled()).thenReturn(false);
            Map<String, Object> created = notificationService.createNotification(
                    buildCreateRequest(), buildOrgContext());
            String code = (String) created.get("notification_code");

            Map<String, Object> result = notificationService.markAsRead("tenant1", code);

            assertEquals("READ", result.get("status"));
            assertNotNull(result.get("read_time"));
            assertNotNull(result.get("updated_time"));
        }

        @Test
        @DisplayName("内存模式下标记不存在的通知为已读抛出异常")
        void markAsReadNonExistentThrows() {
            when(notificationRepository.enabled()).thenReturn(false);

            assertThrows(BusinessException.class,
                    () -> notificationService.markAsRead("tenant1", "NOTIFY-NONEXIST"));
        }

        @Test
        @DisplayName("持久化模式下标记已读委托给仓储")
        void markAsReadWithPersistence() {
            when(notificationRepository.enabled()).thenReturn(true);
            Map<String, Object> mockNotification = new LinkedHashMap<>();
            mockNotification.put("notification_code", "NOTIFY-001");
            mockNotification.put("status", "READ");
            when(notificationRepository.getNotification("tenant1", "NOTIFY-001"))
                    .thenReturn(mockNotification);

            Map<String, Object> result = notificationService.markAsRead("tenant1", "NOTIFY-001");

            verify(notificationRepository).updateNotificationStatus(
                    eq("tenant1"), eq("NOTIFY-001"), eq("READ"), any(Map.class));
            assertEquals("READ", result.get("status"));
        }

        @Test
        @DisplayName("批量标记已读成功")
        void batchMarkAsReadSuccess() {
            when(notificationRepository.enabled()).thenReturn(false);
            Map<String, Object> n1 = notificationService.createNotification(
                    buildCreateRequest(), buildOrgContext());
            Map<String, Object> n2 = notificationService.createNotification(
                    buildCreateRequest(), buildOrgContext());
            String code1 = (String) n1.get("notification_code");
            String code2 = (String) n2.get("notification_code");

            List<String> codes = new ArrayList<>();
            codes.add(code1);
            codes.add(code2);
            int count = notificationService.batchMarkAsRead("tenant1", codes);

            assertEquals(2, count);
        }

        @Test
        @DisplayName("批量标记已读时忽略不存在的通知")
        void batchMarkAsReadIgnoreNonExistent() {
            when(notificationRepository.enabled()).thenReturn(false);
            Map<String, Object> n1 = notificationService.createNotification(
                    buildCreateRequest(), buildOrgContext());
            String code1 = (String) n1.get("notification_code");

            List<String> codes = new ArrayList<>();
            codes.add(code1);
            codes.add("NOTIFY-NONEXIST");
            int count = notificationService.batchMarkAsRead("tenant1", codes);

            assertEquals(1, count);
        }
    }

    // ──────────────────────── 归档通知 ────────────────────────

    @Nested
    @DisplayName("归档通知")
    class ArchiveNotificationTests {

        @Test
        @DisplayName("内存模式下归档通知成功")
        void archiveNotificationInMemory() {
            when(notificationRepository.enabled()).thenReturn(false);
            Map<String, Object> created = notificationService.createNotification(
                    buildCreateRequest(), buildOrgContext());
            String code = (String) created.get("notification_code");

            Map<String, Object> result = notificationService.archiveNotification("tenant1", code);

            assertEquals("ARCHIVED", result.get("status"));
        }

        @Test
        @DisplayName("归档不存在的通知抛出异常")
        void archiveNonExistentNotificationThrows() {
            when(notificationRepository.enabled()).thenReturn(false);

            assertThrows(BusinessException.class,
                    () -> notificationService.archiveNotification("tenant1", "NOTIFY-NONEXIST"));
        }
    }

    // ──────────────────────── 未读数量与统计 ────────────────────────

    @Nested
    @DisplayName("未读数量与统计")
    class UnreadAndSummaryTests {

        @Test
        @DisplayName("内存模式下获取未读通知数量")
        void getUnreadCountInMemory() {
            when(notificationRepository.enabled()).thenReturn(false);
            notificationService.createNotification(buildCreateRequest(), buildOrgContext());

            long count = notificationService.getUnreadCount("tenant1", "user002");

            assertEquals(1, count);
        }

        @Test
        @DisplayName("标记已读后未读数量减少")
        void unreadCountDecreasesAfterMarkAsRead() {
            when(notificationRepository.enabled()).thenReturn(false);
            Map<String, Object> created = notificationService.createNotification(
                    buildCreateRequest(), buildOrgContext());
            String code = (String) created.get("notification_code");

            notificationService.markAsRead("tenant1", code);
            long count = notificationService.getUnreadCount("tenant1", "user002");

            assertEquals(0, count);
        }

        @Test
        @DisplayName("内存模式下获取通知统计")
        void getNotificationSummaryInMemory() {
            when(notificationRepository.enabled()).thenReturn(false);
            notificationService.createNotification(buildCreateRequest(), buildOrgContext());

            Map<String, Object> summary = notificationService.getNotificationSummary("tenant1", "user002");

            assertEquals(1L, summary.get("total"));
            assertEquals(1L, summary.get("unread"));
            assertEquals(0L, summary.get("read"));
            assertEquals(0L, summary.get("archived"));
        }

        @Test
        @DisplayName("持久化模式下获取未读数量委托给仓储")
        void getUnreadCountWithPersistence() {
            when(notificationRepository.enabled()).thenReturn(true);
            when(notificationRepository.getUnreadCount("tenant1", "user002")).thenReturn(5L);

            long count = notificationService.getUnreadCount("tenant1", "user002");

            assertEquals(5L, count);
            verify(notificationRepository).getUnreadCount("tenant1", "user002");
        }
    }

    // ──────────────────────── 渠道配置 ────────────────────────

    @Nested
    @DisplayName("渠道配置")
    class ChannelConfigTests {

        @Test
        @DisplayName("保存渠道配置注入租户ID")
        void saveChannelConfigSetsTenantId() {
            when(notificationRepository.enabled()).thenReturn(true);
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("channel_code", "EMAIL");

            notificationService.saveChannelConfig(config, buildOrgContext());

            assertEquals("tenant1", config.get("tenant_id"));
            verify(notificationRepository).saveChannelConfig(config);
        }

        @Test
        @DisplayName("持久化模式下查询渠道配置列表")
        void listChannelConfigs() {
            when(notificationRepository.enabled()).thenReturn(true);
            when(notificationRepository.listChannelConfigs("tenant1"))
                    .thenReturn(new ArrayList<>());

            List<Map<String, Object>> result = notificationService.listChannelConfigs("tenant1");

            verify(notificationRepository).listChannelConfigs("tenant1");
        }

        @Test
        @DisplayName("内存模式下查询渠道配置返回空列表")
        void listChannelConfigsInMemoryReturnsEmpty() {
            when(notificationRepository.enabled()).thenReturn(false);

            List<Map<String, Object>> result = notificationService.listChannelConfigs("tenant1");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("内存模式下获取单条渠道配置返回null")
        void getChannelConfigInMemoryReturnsNull() {
            when(notificationRepository.enabled()).thenReturn(false);

            Map<String, Object> result = notificationService.getChannelConfig("tenant1", "EMAIL");

            assertNull(result);
        }
    }

    // ──────────────────────── 订阅管理 ────────────────────────

    @Nested
    @DisplayName("订阅管理")
    class SubscriptionTests {

        @Test
        @DisplayName("保存订阅设置注入租户ID")
        void saveSubscriptionSetsTenantId() {
            when(notificationRepository.enabled()).thenReturn(true);
            Map<String, Object> subscription = new LinkedHashMap<>();
            subscription.put("user_id", "user002");
            subscription.put("notification_type", "WORKFLOW");
            subscription.put("channel", "IN_APP");
            subscription.put("enabled", true);

            notificationService.saveSubscription(subscription, buildOrgContext());

            assertEquals("tenant1", subscription.get("tenant_id"));
            verify(notificationRepository).saveSubscription(subscription);
        }

        @Test
        @DisplayName("批量保存订阅设置")
        void batchSaveSubscriptions() {
            when(notificationRepository.enabled()).thenReturn(true);
            List<Map<String, Object>> subscriptions = new ArrayList<>();
            Map<String, Object> sub1 = new LinkedHashMap<>();
            sub1.put("notification_type", "WORKFLOW");
            Map<String, Object> sub2 = new LinkedHashMap<>();
            sub2.put("notification_type", "SYSTEM");
            subscriptions.add(sub1);
            subscriptions.add(sub2);

            notificationService.batchSaveSubscriptions(subscriptions, buildOrgContext());

            assertEquals("tenant1", sub1.get("tenant_id"));
            assertEquals("tenant1", sub2.get("tenant_id"));
            verify(notificationRepository).saveSubscription(sub1);
            verify(notificationRepository).saveSubscription(sub2);
        }

        @Test
        @DisplayName("查询订阅设置列表")
        void listSubscriptions() {
            when(notificationRepository.enabled()).thenReturn(true);
            when(notificationRepository.listSubscriptions("tenant1", "user002"))
                    .thenReturn(new ArrayList<>());

            List<Map<String, Object>> result = notificationService.listSubscriptions("tenant1", "user002");

            verify(notificationRepository).listSubscriptions("tenant1", "user002");
        }

        @Test
        @DisplayName("更新订阅设置")
        void updateSubscription() {
            when(notificationRepository.enabled()).thenReturn(true);

            notificationService.updateSubscription("tenant1", "user002", "WORKFLOW", "IN_APP", false);

            verify(notificationRepository).updateSubscription("tenant1", "user002", "WORKFLOW", "IN_APP", false);
        }

        @Test
        @DisplayName("内存模式下查询订阅返回空列表")
        void listSubscriptionsInMemoryReturnsEmpty() {
            when(notificationRepository.enabled()).thenReturn(false);

            List<Map<String, Object>> result = notificationService.listSubscriptions("tenant1", "user002");

            assertTrue(result.isEmpty());
        }
    }

    // ──────────────────────── 工作流联动通知 ────────────────────────

    @Nested
    @DisplayName("工作流联动通知")
    class WorkflowNotificationTests {

        @Test
        @DisplayName("内存模式下创建工作流通知成功")
        void createWorkflowNotificationInMemory() {
            when(notificationRepository.enabled()).thenReturn(false);

            Map<String, Object> result = notificationService.createWorkflowNotification(
                    "tenant1", "ORG001", "WF-001", "REVIEW",
                    "审核任务通知", "配置包待审核", "user002", "admin");

            assertNotNull(result);
            assertTrue(((String) result.get("notification_code")).startsWith("NOTIFY-"));
            assertEquals("tenant1", result.get("tenant_id"));
            assertEquals("ORG001", result.get("org_code"));
            assertEquals("WORKFLOW", result.get("notification_type"));
            assertEquals("HIGH", result.get("priority"));
            assertEquals("UNREAD", result.get("status"));
            assertEquals("user002", result.get("recipient_id"));
            assertEquals("REVIEW", result.get("business_type"));
            assertEquals("WF-001", result.get("business_id"));
            assertEquals("/workflow/todos/WF-001", result.get("business_url"));
        }

        @Test
        @DisplayName("持久化模式下创建工作流通知调用仓储保存")
        void createWorkflowNotificationWithPersistence() {
            when(notificationRepository.enabled()).thenReturn(true);

            notificationService.createWorkflowNotification(
                    "tenant1", "ORG001", "WF-001", "REVIEW",
                    "审核任务通知", "配置包待审核", "user002", "admin");

            verify(notificationRepository).saveNotification(any(Map.class));
        }
    }

    // ──────────────────────── 清理过期通知 ────────────────────────

    @Nested
    @DisplayName("清理过期通知")
    class CleanupExpiredTests {

        @Test
        @DisplayName("持久化模式下清理过期通知委托给仓储")
        void cleanupExpiredWithPersistence() {
            when(notificationRepository.enabled()).thenReturn(true);
            when(notificationRepository.cleanupExpiredNotifications()).thenReturn(3);

            int count = notificationService.cleanupExpiredNotifications();

            assertEquals(3, count);
            verify(notificationRepository).cleanupExpiredNotifications();
        }

        @Test
        @DisplayName("内存模式下无过期通知返回0")
        void cleanupExpiredInMemoryNoExpired() {
            when(notificationRepository.enabled()).thenReturn(false);
            Map<String, Object> request = buildCreateRequest();
            notificationService.createNotification(request, buildOrgContext());

            int count = notificationService.cleanupExpiredNotifications();

            assertEquals(0, count);
        }
    }
}
