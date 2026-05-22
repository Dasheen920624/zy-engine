package com.medkernel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志保留期管理（等保 2.0 三级 - 安全审计）。
 *
 * 要求：
 * - 审计日志保留不少于 180 天
 * - 过期日志可归档后删除
 * - 提供保留期状态查询
 */
@Service
public class AuditLogRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogRetentionService.class);

    private static final int RETENTION_DAYS = 180;

    private final SecurityPersistenceService persistenceService;

    public AuditLogRetentionService(SecurityPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    /**
     * 获取审计日志保留期状态。
     */
    public Map<String, Object> getRetentionStatus() {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("retention_days", RETENTION_DAYS);
        status.put("compliance_standard", "等保 2.0 三级");
        status.put("cutoff_date", LocalDate.now().minusDays(RETENTION_DAYS).toString());
        status.put("last_cleanup_time", getLastCleanupTime());
        return status;
    }

    /**
     * 定时清理过期审计日志（每天凌晨 2 点）。
     * 仅清理超过保留期的日志，保留 180 天内的记录。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        log.info("[audit-retention] starting cleanup, cutoff={}", cutoff);

        try {
            int authLogsDeleted = persistenceService.deleteAuthAuditLogsBefore(cutoff);
            int ssoLogsDeleted = persistenceService.deleteSsoAuditLogsBefore(cutoff);

            log.info("[audit-retention] cleanup completed: authLogsDeleted={}, ssoLogsDeleted={}",
                    authLogsDeleted, ssoLogsDeleted);
        } catch (Exception e) {
            log.error("[audit-retention] cleanup failed", e);
        }
    }

    private String getLastCleanupTime() {
        // 返回最近一次清理时间（MVP：返回当前时间减 24 小时模拟）
        return LocalDateTime.now().minusHours(24).toString();
    }
}
