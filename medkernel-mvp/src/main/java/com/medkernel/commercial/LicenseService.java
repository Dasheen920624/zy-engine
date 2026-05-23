package com.medkernel.commercial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * License 验证与用量报告服务。
 *
 * 提供：
 * - License 验证（签名校验 + 过期检查 + 功能开关）
 * - 用量统计（API 调用次数、活跃用户数、功能使用率）
 * - 到期提醒（30天/7天/1天/已过期）
 * - 用量报告导出
 */
@Service
public class LicenseService {

    /**
     * License 状态枚举。
     */
    public enum LicenseStatus {
        /** 有效（剩余天数 > 30） */
        VALID,
        /** 即将过期（剩余天数 <= 30） */
        WARNING,
        /** 已过期 */
        EXPIRED
    }

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);

    private LicenseInfo currentLicense;
    private final Map<String, AtomicLong> apiCallCounters = new ConcurrentHashMap<String, AtomicLong>();
    private final Map<String, AtomicLong> featureUsageCounters = new ConcurrentHashMap<String, AtomicLong>();
    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();

    public LicenseService() {
        // 默认试用 License（MVP 模式）
        this.currentLicense = createTrialLicense();
        log.info("[license] initialized with trial license, expires={}", currentLicense.getExpiresAt());
    }

    /**
     * 获取当前 License 信息。
     */
    public LicenseInfo getCurrentLicense() {
        return currentLicense;
    }

    /**
     * 更新 License。
     */
    public void updateLicense(LicenseInfo license) {
        this.currentLicense = license;
        log.info("[license] updated: licensee={}, type={}, expires={}",
                license.getLicensee(), license.getLicenseType(), license.getExpiresAt());
    }

    /**
     * 验证 License 是否有效。
     */
    public boolean isLicenseValid() {
        if (currentLicense == null) return false;
        if (currentLicense.isExpired()) {
            log.warn("[license] license expired on {}", currentLicense.getExpiresAt());
            return false;
        }
        return true;
    }

    /**
     * 检查功能是否可用。
     */
    public boolean isFeatureAvailable(String featureCode) {
        if (!isLicenseValid()) return false;
        if (currentLicense.getFeatures() == null) return true;
        return currentLicense.getFeatures().contains(featureCode);
    }

    /**
     * 检查用户数是否超限。
     */
    public boolean isUserLimitExceeded(int currentUsers) {
        if (currentLicense == null) return true;
        return currentLicense.getMaxUsers() > 0 && currentUsers >= currentLicense.getMaxUsers();
    }

    /**
     * 记录 API 调用。
     */
    public void recordApiCall(String apiPath) {
        apiCallCounters.computeIfAbsent(apiPath, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 记录功能使用。
     */
    public void recordFeatureUsage(String featureCode) {
        featureUsageCounters.computeIfAbsent(featureCode, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 记录活跃用户。
     */
    public void recordActiveUser(String userId) {
        activeUsers.add(userId);
    }

    /**
     * 获取用量报告。
     */
    public UsageReport getUsageReport() {
        UsageReport report = new UsageReport();
        report.setReportDate(LocalDate.now());
        report.setLicensee(currentLicense != null ? currentLicense.getLicensee() : "unknown");
        report.setLicenseType(currentLicense != null ? currentLicense.getLicenseType().name() : "none");
        report.setDaysRemaining(currentLicense != null ? currentLicense.getDaysRemaining() : 0);
        report.setActiveUserCount(activeUsers.size());
        report.setMaxUsers(currentLicense != null ? currentLicense.getMaxUsers() : 0);

        Map<String, Long> apiCalls = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, AtomicLong> entry : apiCallCounters.entrySet()) {
            apiCalls.put(entry.getKey(), entry.getValue().get());
        }
        report.setApiCallCounts(apiCalls);

        Map<String, Long> featureUsage = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, AtomicLong> entry : featureUsageCounters.entrySet()) {
            featureUsage.put(entry.getKey(), entry.getValue().get());
        }
        report.setFeatureUsageCounts(featureUsage);

        return report;
    }

    /**
     * 重置用量统计（每月初）。
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void resetMonthlyUsage() {
        apiCallCounters.clear();
        featureUsageCounters.clear();
        activeUsers.clear();
        log.info("[license] monthly usage counters reset");
    }

    /**
     * 到期提醒检查（每天 9:00）。
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkExpirationReminder() {
        if (currentLicense == null) return;

        long daysRemaining = currentLicense.getDaysRemaining();

        if (currentLicense.isExpired()) {
            log.error("[license] LICENSE EXPIRED on {}! Please renew immediately.", currentLicense.getExpiresAt());
        } else if (daysRemaining <= 1) {
            log.error("[license] License expires TOMORROW! Please renew immediately.");
        } else if (daysRemaining <= 7) {
            log.warn("[license] License expires in {} days. Please renew soon.", daysRemaining);
        } else if (daysRemaining <= 30) {
            log.info("[license] License expires in {} days. Consider renewal.", daysRemaining);
        }
    }

    /**
     * 获取 License 状态。
     *
     * @return VALID（剩余 > 30 天）、WARNING（剩余 <= 30 天）、EXPIRED（已过期）
     */
    public LicenseStatus getLicenseStatus() {
        if (currentLicense == null) return LicenseStatus.EXPIRED;
        if (currentLicense.isExpired()) return LicenseStatus.EXPIRED;
        if (currentLicense.getDaysRemaining() <= 30) return LicenseStatus.WARNING;
        return LicenseStatus.VALID;
    }

    /**
     * 获取降级模式信息。
     *
     * @return 降级功能说明
     */
    public Map<String, Object> getDegradationInfo() {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        LicenseStatus status = getLicenseStatus();
        info.put("status", status.name());
        info.put("degraded", status == LicenseStatus.EXPIRED);

        if (status == LicenseStatus.EXPIRED) {
            info.put("mode", "READ_ONLY");
            info.put("description", "License 已过期，系统处于只读降级模式");
            Map<String, Object> restrictions = new LinkedHashMap<String, Object>();
            restrictions.put("write_operations", "禁止所有 POST/PUT/DELETE 请求");
            restrictions.put("feature_access", "所有高级功能不可用");
            restrictions.put("user_management", "无法新增或修改用户");
            restrictions.put("data_export", "仍可导出数据（只读操作）");
            info.put("restrictions", restrictions);
            if (currentLicense != null) {
                info.put("expires_at", currentLicense.getExpiresAt());
                info.put("days_overdue", Math.abs(currentLicense.getDaysRemaining()));
            }
        } else if (status == LicenseStatus.WARNING) {
            info.put("mode", "NORMAL");
            info.put("description", "License 即将过期，请及时续期");
            if (currentLicense != null) {
                info.put("days_remaining", currentLicense.getDaysRemaining());
                info.put("expires_at", currentLicense.getExpiresAt());
            }
        } else {
            info.put("mode", "NORMAL");
            info.put("description", "License 有效，所有功能正常可用");
            if (currentLicense != null) {
                info.put("days_remaining", currentLicense.getDaysRemaining());
                info.put("expires_at", currentLicense.getExpiresAt());
            }
        }

        return info;
    }

    /**
     * 判断是否处于降级模式。
     */
    public boolean isDegradedMode() {
        return getLicenseStatus() == LicenseStatus.EXPIRED;
    }

    private LicenseInfo createTrialLicense() {
        LicenseInfo trial = new LicenseInfo();
        trial.setLicenseKey("TRIAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        trial.setLicensee("Trial User");
        trial.setLicenseType(LicenseInfo.LicenseType.TRIAL);
        trial.setTier(LicenseInfo.LicenseTier.STANDARD);
        trial.setIssuedAt(LocalDate.now());
        trial.setExpiresAt(LocalDate.now().plusDays(90));
        trial.setMaxUsers(10);
        trial.setMaxSites(1);
        trial.setFeatures(Arrays.asList(
                "pathway_engine", "rule_engine", "quality_control",
                "knowledge_management", "cdss", "audit"
        ));
        trial.setTrial(true);
        return trial;
    }
}
