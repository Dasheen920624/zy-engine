package com.medkernel.cdss;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 提醒疲劳治理配置。
 * 定义告警去重、抑制、静默期策略，减少医生提醒疲劳。
 */
public class AlertFatigueConfig {
    private String tenantId;
    private String configId;
    private String triggerPoint;       // 适用的触发点，null 表示全部
    private String riskLevel;          // 适用的风险等级，null 表示全部
    private boolean deduplicationEnabled;
    private int deduplicationWindowMinutes; // 去重窗口（分钟）
    private boolean suppressionEnabled;
    private int suppressionMaxAlertsPerHour; // 每小时最大告警数
    private boolean quietPeriodEnabled;
    private int quietPeriodMinutes;    // 静默期（分钟）
    private boolean smartFilterEnabled;
    private double overrideRateThreshold; // 覆盖率阈值，超过则自动降级
    private String status;             // ACTIVE, DISABLED
    private String createdBy;
    private String createdTime;
    private String updatedTime;

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("tenant_id", tenantId);
        view.put("config_id", configId);
        view.put("trigger_point", triggerPoint);
        view.put("risk_level", riskLevel);
        view.put("deduplication_enabled", deduplicationEnabled);
        view.put("deduplication_window_minutes", deduplicationWindowMinutes);
        view.put("suppression_enabled", suppressionEnabled);
        view.put("suppression_max_alerts_per_hour", suppressionMaxAlertsPerHour);
        view.put("quiet_period_enabled", quietPeriodEnabled);
        view.put("quiet_period_minutes", quietPeriodMinutes);
        view.put("smart_filter_enabled", smartFilterEnabled);
        view.put("override_rate_threshold", overrideRateThreshold);
        view.put("status", status);
        view.put("created_by", createdBy);
        view.put("created_time", createdTime);
        view.put("updated_time", updatedTime);
        return view;
    }

    // Getters and Setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }
    public String getTriggerPoint() { return triggerPoint; }
    public void setTriggerPoint(String triggerPoint) { this.triggerPoint = triggerPoint; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public boolean isDeduplicationEnabled() { return deduplicationEnabled; }
    public void setDeduplicationEnabled(boolean deduplicationEnabled) { this.deduplicationEnabled = deduplicationEnabled; }
    public int getDeduplicationWindowMinutes() { return deduplicationWindowMinutes; }
    public void setDeduplicationWindowMinutes(int deduplicationWindowMinutes) { this.deduplicationWindowMinutes = deduplicationWindowMinutes; }
    public boolean isSuppressionEnabled() { return suppressionEnabled; }
    public void setSuppressionEnabled(boolean suppressionEnabled) { this.suppressionEnabled = suppressionEnabled; }
    public int getSuppressionMaxAlertsPerHour() { return suppressionMaxAlertsPerHour; }
    public void setSuppressionMaxAlertsPerHour(int suppressionMaxAlertsPerHour) { this.suppressionMaxAlertsPerHour = suppressionMaxAlertsPerHour; }
    public boolean isQuietPeriodEnabled() { return quietPeriodEnabled; }
    public void setQuietPeriodEnabled(boolean quietPeriodEnabled) { this.quietPeriodEnabled = quietPeriodEnabled; }
    public int getQuietPeriodMinutes() { return quietPeriodMinutes; }
    public void setQuietPeriodMinutes(int quietPeriodMinutes) { this.quietPeriodMinutes = quietPeriodMinutes; }
    public boolean isSmartFilterEnabled() { return smartFilterEnabled; }
    public void setSmartFilterEnabled(boolean smartFilterEnabled) { this.smartFilterEnabled = smartFilterEnabled; }
    public double getOverrideRateThreshold() { return overrideRateThreshold; }
    public void setOverrideRateThreshold(double overrideRateThreshold) { this.overrideRateThreshold = overrideRateThreshold; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }
    public String getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(String updatedTime) { this.updatedTime = updatedTime; }
}
