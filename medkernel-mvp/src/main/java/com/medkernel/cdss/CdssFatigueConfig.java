package com.medkernel.cdss;

import java.time.LocalDateTime;

/**
 * CDSS 疲劳配置实体。
 * 定义告警疲劳治理策略：当医生在时间窗口内覆盖次数超过阈值时触发抑制动作。
 */
public class CdssFatigueConfig {
    private Long id;
    private Long tenantId;
    private String configCode;
    private String configName;
    private String ruleCode;          // 适用的规则编码（空表示全局）
    private String departmentCode;    // 适用的科室（空表示全局）
    private int timeWindowHours;      // 时间窗口（小时）
    private int overrideThreshold;    // 覆盖次数阈值
    private String suppressAction;    // SUPPRESS/DOWNGRADE/NOTIFY_SUPERVISOR
    private String suppressLevel;     // 降级目标（DOWNGRADE时使用）
    private String enabled;
    private String description;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getConfigCode() { return configCode; }
    public void setConfigCode(String configCode) { this.configCode = configCode; }

    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public int getTimeWindowHours() { return timeWindowHours; }
    public void setTimeWindowHours(int timeWindowHours) { this.timeWindowHours = timeWindowHours; }

    public int getOverrideThreshold() { return overrideThreshold; }
    public void setOverrideThreshold(int overrideThreshold) { this.overrideThreshold = overrideThreshold; }

    public String getSuppressAction() { return suppressAction; }
    public void setSuppressAction(String suppressAction) { this.suppressAction = suppressAction; }

    public String getSuppressLevel() { return suppressLevel; }
    public void setSuppressLevel(String suppressLevel) { this.suppressLevel = suppressLevel; }

    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
