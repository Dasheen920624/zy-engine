package com.medkernel.commercial;

import com.medkernel.notification.NotificationService;
import com.medkernel.organization.OrganizationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * License 到期通知服务。
 *
 * 定时检查 License 到期情况，通过 NotificationService 发送通知：
 * - 30 天到期：INFO 通知
 * - 15 天到期：WARNING 通知
 * - 7 天到期：URGENT 通知
 * - 已过期：CRITICAL 通知（每天发送）
 *
 * 每个阈值只通知一次，避免重复发送。
 */
@Service
public class LicenseExpirationNotifier {

    private static final Logger log = LoggerFactory.getLogger(LicenseExpirationNotifier.class);

    private static final long THRESHOLD_30 = 30;
    private static final long THRESHOLD_15 = 15;
    private static final long THRESHOLD_7 = 7;

    private final LicenseService licenseService;
    private final NotificationService notificationService;

    /**
     * 记录已发送通知的阈值，避免重复通知。
     * key = 阈值标识（如 "30", "15", "7", "EXPIRED"），value = 发送日期
     */
    private final Map<String, LocalDate> lastNotificationDates = new ConcurrentHashMap<String, LocalDate>();

    public LicenseExpirationNotifier(LicenseService licenseService, NotificationService notificationService) {
        this.licenseService = licenseService;
        this.notificationService = notificationService;
    }

    /**
     * 每天 9:00 检查 License 到期情况并发送通知。
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkAndNotify() {
        LicenseInfo license = licenseService.getCurrentLicense();
        if (license == null) {
            log.warn("[license-notifier] no license found, skipping notification");
            return;
        }

        long daysRemaining = license.getDaysRemaining();
        boolean expired = license.isExpired();

        if (expired) {
            sendNotificationIfNeeded("EXPIRED", "CRITICAL",
                    "License 已过期",
                    "License 已于 " + license.getExpiresAt() + " 过期，系统已进入只读降级模式。请立即续期以恢复完整功能。");
        } else if (daysRemaining <= THRESHOLD_7) {
            sendNotificationIfNeeded("7", "URGENT",
                    "License 即将过期（" + daysRemaining + " 天）",
                    "License 将在 " + daysRemaining + " 天后过期（到期日：" + license.getExpiresAt() + "），请尽快续期。过期后系统将进入只读降级模式。");
        } else if (daysRemaining <= THRESHOLD_15) {
            sendNotificationIfNeeded("15", "WARNING",
                    "License 即将过期（" + daysRemaining + " 天）",
                    "License 将在 " + daysRemaining + " 天后过期（到期日：" + license.getExpiresAt() + "），建议尽快安排续期。");
        } else if (daysRemaining <= THRESHOLD_30) {
            sendNotificationIfNeeded("30", "INFO",
                    "License 临近过期（" + daysRemaining + " 天）",
                    "License 将在 " + daysRemaining + " 天后过期（到期日：" + license.getExpiresAt() + "），请考虑续期。");
        }
    }

    /**
     * 重置通知记录（License 更新后调用）。
     */
    public void resetNotifications() {
        lastNotificationDates.clear();
        log.info("[license-notifier] notification records reset");
    }

    private void sendNotificationIfNeeded(String thresholdKey, String priority,
                                           String title, String content) {
        LocalDate today = LocalDate.now();
        LocalDate lastDate = lastNotificationDates.get(thresholdKey);

        // EXPIRED 每天都发，其他阈值只发一次
        if (!"EXPIRED".equals(thresholdKey) && lastDate != null) {
            log.debug("[license-notifier] already notified for threshold={}, skipping", thresholdKey);
            return;
        }

        // EXPIRED 每天发一次
        if ("EXPIRED".equals(thresholdKey) && lastDate != null && lastDate.equals(today)) {
            log.debug("[license-notifier] already sent expired notification today, skipping");
            return;
        }

        try {
            Map<String, Object> request = new LinkedHashMap<String, Object>();
            request.put("title", title);
            request.put("content", content);
            request.put("notificationType", "LICENSE");
            request.put("priority", priority);
            request.put("channel", "IN_APP");

            OrganizationContext orgContext = createSystemOrgContext();
            notificationService.createNotification(request, orgContext);

            lastNotificationDates.put(thresholdKey, today);
            log.info("[license-notifier] sent {} notification: {}", priority, title);
        } catch (Exception e) {
            // 通知发送失败不应影响主流程，仅记录日志
            log.error("[license-notifier] failed to send notification: {} - {}", title, e.getMessage());
        }
    }

    private OrganizationContext createSystemOrgContext() {
        OrganizationContext ctx = new OrganizationContext();
        ctx.setTenantId("SYSTEM");
        ctx.setLegacyOrgCode("SYSTEM");
        return ctx;
    }
}
