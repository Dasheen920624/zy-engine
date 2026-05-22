package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 保存疲劳配置请求 DTO：用于 CdssOverrideController.saveFatigueConfig。
 */
@Schema(description = "保存疲劳配置请求")
public class SaveFatigueConfigRequest {

    @NotBlank(message = "configCode 不能为空")
    @Schema(description = "配置编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configCode;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "科室编码")
    private String departmentCode;

    @Schema(description = "时间窗口（小时）")
    private Integer timeWindowHours;

    @Schema(description = "覆盖阈值")
    private Integer overrideThreshold;

    @Schema(description = "抑制动作")
    private String suppressAction;

    @Schema(description = "抑制等级")
    private String suppressLevel;

    @Schema(description = "是否启用")
    private String enabled;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建人")
    private String createdBy;

    public String getConfigCode() { return configCode; }
    public void setConfigCode(String configCode) { this.configCode = configCode; }
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
    public Integer getTimeWindowHours() { return timeWindowHours; }
    public void setTimeWindowHours(Integer timeWindowHours) { this.timeWindowHours = timeWindowHours; }
    public Integer getOverrideThreshold() { return overrideThreshold; }
    public void setOverrideThreshold(Integer overrideThreshold) { this.overrideThreshold = overrideThreshold; }
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
}
