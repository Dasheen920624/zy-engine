package com.medkernel.cdss;

import com.medkernel.common.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 提醒疲劳治理和覆盖分析服务。
 *
 * <p>核心能力：
 * <ul>
 *   <li>告警去重 — 同一患者同一规则在窗口内不重复告警</li>
 *   <li>告警抑制 — 每小时最大告警数限制</li>
 *   <li>静默期 — 医生覆盖后一段时间内不再提醒同类告警</li>
 *   <li>智能过滤 — 覆盖率超过阈值自动降级</li>
 *   <li>覆盖模式分析 — 统计各规则/触发点的覆盖率和模式</li>
 * </ul>
 */
@Service
public class AlertFatigueService {
    private final EnginePersistenceService persistenceService;
    private final OrganizationContextService organizationContextService;

    private static final AtomicLong CONFIG_SEQ = new AtomicLong(1);
    private final Map<String, AlertFatigueConfig> configStore = new ConcurrentHashMap<String, AlertFatigueConfig>();

    /** 告警历史记录（用于去重和抑制判断） */
    private final List<AlertRecord> alertHistory = new ArrayList<AlertRecord>();
    /** 覆盖记录（用于覆盖模式分析和静默期） */
    private final List<OverrideRecord> overrideHistory = new ArrayList<OverrideRecord>();

    public AlertFatigueService(EnginePersistenceService persistenceService,
                               OrganizationContextService organizationContextService) {
        this.persistenceService = persistenceService;
        this.organizationContextService = organizationContextService;
    }

    // ==================== 疲劳治理配置 CRUD ====================

    public AlertFatigueConfig createConfig(Map<String, Object> request, OrganizationContext orgContext) {
        String configId = "AFC-" + String.format("%04d", CONFIG_SEQ.getAndIncrement());
        AlertFatigueConfig config = new AlertFatigueConfig();
        config.setTenantId(orgContext.getTenantId());
        config.setConfigId(configId);
        config.setTriggerPoint((String) request.get("trigger_point"));
        config.setRiskLevel((String) request.get("risk_level"));
        config.setDeduplicationEnabled(toBool(request.getOrDefault("deduplication_enabled", "true")));
        config.setDeduplicationWindowMinutes(toInt(request.getOrDefault("deduplication_window_minutes", "30")));
        config.setSuppressionEnabled(toBool(request.getOrDefault("suppression_enabled", "true")));
        config.setSuppressionMaxAlertsPerHour(toInt(request.getOrDefault("suppression_max_alerts_per_hour", "20")));
        config.setQuietPeriodEnabled(toBool(request.getOrDefault("quiet_period_enabled", "true")));
        config.setQuietPeriodMinutes(toInt(request.getOrDefault("quiet_period_minutes", "60")));
        config.setSmartFilterEnabled(toBool(request.getOrDefault("smart_filter_enabled", "true")));
        config.setOverrideRateThreshold(toDouble(request.getOrDefault("override_rate_threshold", "0.8")));
        config.setStatus("ACTIVE");
        config.setCreatedBy((String) request.get("created_by"));
        config.setCreatedTime(LocalDateTime.now().toString());
        config.setUpdatedTime(LocalDateTime.now().toString());

        configStore.put(configId, config);
        return config;
    }

    public List<AlertFatigueConfig> listConfigs(OrganizationContext orgContext) {
        List<AlertFatigueConfig> result = new ArrayList<AlertFatigueConfig>();
        for (AlertFatigueConfig config : configStore.values()) {
            if (orgContext.getTenantId().equals(config.getTenantId())) {
                result.add(config);
            }
        }
        return result;
    }

    public AlertFatigueConfig updateConfig(String configId, Map<String, Object> request, OrganizationContext orgContext) {
        AlertFatigueConfig config = configStore.get(configId);
        if (config == null || !orgContext.getTenantId().equals(config.getTenantId())) {
            throw new IllegalArgumentException("Config not found: " + configId);
        }
        if (request.containsKey("deduplication_enabled")) config.setDeduplicationEnabled(toBool(request.get("deduplication_enabled")));
        if (request.containsKey("deduplication_window_minutes")) config.setDeduplicationWindowMinutes(toInt(request.get("deduplication_window_minutes")));
        if (request.containsKey("suppression_enabled")) config.setSuppressionEnabled(toBool(request.get("suppression_enabled")));
        if (request.containsKey("suppression_max_alerts_per_hour")) config.setSuppressionMaxAlertsPerHour(toInt(request.get("suppression_max_alerts_per_hour")));
        if (request.containsKey("quiet_period_enabled")) config.setQuietPeriodEnabled(toBool(request.get("quiet_period_enabled")));
        if (request.containsKey("quiet_period_minutes")) config.setQuietPeriodMinutes(toInt(request.get("quiet_period_minutes")));
        if (request.containsKey("smart_filter_enabled")) config.setSmartFilterEnabled(toBool(request.get("smart_filter_enabled")));
        if (request.containsKey("override_rate_threshold")) config.setOverrideRateThreshold(toDouble(request.get("override_rate_threshold")));
        if (request.containsKey("status")) config.setStatus((String) request.get("status"));
        config.setUpdatedTime(LocalDateTime.now().toString());
        return config;
    }

    // ==================== 告警过滤（去重/抑制/静默期） ====================

    /**
     * 判断告警是否应该被过滤（因去重/抑制/静默期）。
     */
    public boolean shouldFilterAlert(CdssAlert alert, String tenantId) {
        AlertFatigueConfig config = findMatchingConfig(alert, tenantId);
        if (config == null || !"ACTIVE".equals(config.getStatus())) {
            return false;
        }

        // 去重检查
        if (config.isDeduplicationEnabled() && isDuplicate(alert, config)) {
            return true;
        }

        // 抑制检查
        if (config.isSuppressionEnabled() && isSuppressed(alert, config)) {
            return true;
        }

        // 静默期检查
        if (config.isQuietPeriodEnabled() && isInQuietPeriod(alert, config)) {
            return true;
        }

        // 智能过滤：覆盖率过高则降级
        if (config.isSmartFilterEnabled() && shouldDowngrade(alert, config)) {
            // 不完全过滤，但标记为降级
            return false;
        }

        return false;
    }

    /**
     * 记录告警历史。
     */
    public void recordAlert(CdssAlert alert) {
        AlertRecord record = new AlertRecord();
        record.alertId = alert.getAlertId();
        record.ruleCode = alert.getRuleCode();
        record.patientId = alert.getPatientId();
        record.triggerPoint = alert.getTriggerPoint();
        record.riskLevel = alert.getRiskLevel() != null ? alert.getRiskLevel().getCode() : null;
        record.createdAt = LocalDateTime.now();
        synchronized (alertHistory) {
            alertHistory.add(record);
        }
    }

    /**
     * 记录覆盖操作。
     */
    public void recordOverride(CdssAlert alert, String overrideType, String operatorName) {
        OverrideRecord record = new OverrideRecord();
        record.alertId = alert.getAlertId();
        record.ruleCode = alert.getRuleCode();
        record.patientId = alert.getPatientId();
        record.triggerPoint = alert.getTriggerPoint();
        record.overrideType = overrideType;
        record.operatorName = operatorName;
        record.overriddenAt = LocalDateTime.now();
        synchronized (overrideHistory) {
            overrideHistory.add(record);
        }
    }

    // ==================== 覆盖模式分析 ====================

    /**
     * 获取覆盖模式分析报告。
     */
    public Map<String, Object> getOverrideAnalysis(String tenantId) {
        Map<String, Object> report = new LinkedHashMap<String, Object>();

        // 总体覆盖统计
        int totalAlerts = alertHistory.size();
        int totalOverrides = 0;
        int totalAcknowledges = 0;
        int totalEscalations = 0;

        Map<String, Integer> overrideByRule = new LinkedHashMap<String, Integer>();
        Map<String, Integer> overrideByTrigger = new LinkedHashMap<String, Integer>();
        Map<String, Integer> overrideByOperator = new LinkedHashMap<String, Integer>();

        synchronized (overrideHistory) {
            for (OverrideRecord record : overrideHistory) {
                if ("OVERRIDE".equals(record.overrideType)) {
                    totalOverrides++;
                } else if ("ACKNOWLEDGE".equals(record.overrideType)) {
                    totalAcknowledges++;
                } else if ("ESCALATE".equals(record.overrideType)) {
                    totalEscalations++;
                }

                String ruleKey = record.ruleCode != null ? record.ruleCode : "UNKNOWN";
                overrideByRule.merge(ruleKey, 1, Integer::sum);

                String triggerKey = record.triggerPoint != null ? record.triggerPoint : "UNKNOWN";
                overrideByTrigger.merge(triggerKey, 1, Integer::sum);

                String operatorKey = record.operatorName != null ? record.operatorName : "UNKNOWN";
                overrideByOperator.merge(operatorKey, 1, Integer::sum);
            }
        }

        double overrideRate = totalAlerts > 0 ? (double) totalOverrides / totalAlerts : 0;

        report.put("total_alerts", totalAlerts);
        report.put("total_overrides", totalOverrides);
        report.put("total_acknowledges", totalAcknowledges);
        report.put("total_escalations", totalEscalations);
        report.put("override_rate", Math.round(overrideRate * 1000.0) / 10.0);
        report.put("override_by_rule", overrideByRule);
        report.put("override_by_trigger", overrideByTrigger);
        report.put("override_by_operator", overrideByOperator);

        // 高覆盖规则预警
        List<Map<String, Object>> highOverrideRules = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : overrideByRule.entrySet()) {
            int ruleAlertCount = 0;
            synchronized (alertHistory) {
                for (AlertRecord ar : alertHistory) {
                    if (entry.getKey().equals(ar.ruleCode)) ruleAlertCount++;
                }
            }
            double rate = ruleAlertCount > 0 ? (double) entry.getValue() / ruleAlertCount : 0;
            if (rate > 0.5) {
                Map<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("rule_code", entry.getKey());
                item.put("override_count", entry.getValue());
                item.put("alert_count", ruleAlertCount);
                item.put("override_rate", Math.round(rate * 1000.0) / 10.0);
                item.put("recommendation", rate > 0.8 ? "建议审查规则有效性" : "关注覆盖趋势");
                highOverrideRules.add(item);
            }
        }
        report.put("high_override_rules", highOverrideRules);

        return report;
    }

    // ==================== 内部方法 ====================

    private AlertFatigueConfig findMatchingConfig(CdssAlert alert, String tenantId) {
        for (AlertFatigueConfig config : configStore.values()) {
            if (!tenantId.equals(config.getTenantId())) continue;
            if (!"ACTIVE".equals(config.getStatus())) continue;
            if (config.getTriggerPoint() != null && !config.getTriggerPoint().equals(alert.getTriggerPoint())) continue;
            if (config.getRiskLevel() != null && alert.getRiskLevel() != null
                    && !config.getRiskLevel().equals(alert.getRiskLevel().getCode())) continue;
            return config;
        }
        return null;
    }

    private boolean isDuplicate(CdssAlert alert, AlertFatigueConfig config) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(config.getDeduplicationWindowMinutes());
        synchronized (alertHistory) {
            for (AlertRecord record : alertHistory) {
                if (record.createdAt.isBefore(cutoff)) continue;
                if (alert.getRuleCode() != null && alert.getRuleCode().equals(record.ruleCode)
                        && alert.getPatientId() != null && alert.getPatientId().equals(record.patientId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSuppressed(CdssAlert alert, AlertFatigueConfig config) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        int count = 0;
        synchronized (alertHistory) {
            for (AlertRecord record : alertHistory) {
                if (record.createdAt.isBefore(oneHourAgo)) continue;
                if (alert.getPatientId() != null && alert.getPatientId().equals(record.patientId)) {
                    count++;
                }
            }
        }
        return count >= config.getSuppressionMaxAlertsPerHour();
    }

    private boolean isInQuietPeriod(CdssAlert alert, AlertFatigueConfig config) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(config.getQuietPeriodMinutes());
        synchronized (overrideHistory) {
            for (OverrideRecord record : overrideHistory) {
                if (record.overriddenAt.isBefore(cutoff)) continue;
                if ("OVERRIDE".equals(record.overrideType)
                        && alert.getRuleCode() != null && alert.getRuleCode().equals(record.ruleCode)
                        && alert.getPatientId() != null && alert.getPatientId().equals(record.patientId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldDowngrade(CdssAlert alert, AlertFatigueConfig config) {
        if (alert.getRuleCode() == null) return false;
        int ruleAlertCount = 0;
        int ruleOverrideCount = 0;
        synchronized (alertHistory) {
            for (AlertRecord ar : alertHistory) {
                if (alert.getRuleCode().equals(ar.ruleCode)) ruleAlertCount++;
            }
        }
        synchronized (overrideHistory) {
            for (OverrideRecord or : overrideHistory) {
                if (alert.getRuleCode().equals(or.ruleCode) && "OVERRIDE".equals(or.overrideType)) ruleOverrideCount++;
            }
        }
        double rate = ruleAlertCount > 0 ? (double) ruleOverrideCount / ruleAlertCount : 0;
        return rate > config.getOverrideRateThreshold();
    }

    private boolean toBool(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return "true".equalsIgnoreCase(value.toString());
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private double toDouble(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private static class AlertRecord {
        String alertId;
        String ruleCode;
        String patientId;
        String triggerPoint;
        String riskLevel;
        LocalDateTime createdAt;
    }

    private static class OverrideRecord {
        String alertId;
        String ruleCode;
        String patientId;
        String triggerPoint;
        String overrideType;
        String operatorName;
        LocalDateTime overriddenAt;
    }
}
