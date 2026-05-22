package com.medkernel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 触发点注册请求 DTO：替代 TriggerPointController.registerTrigger 的 CdssTriggerPointEntity。
 */
@Schema(description = "触发点注册请求")
public class TriggerRegisterRequest {

    @NotBlank(message = "triggerCode 不能为空")
    @Schema(description = "触发点编码", example = "ORDER_AMI_CHECK", requiredMode = Schema.RequiredMode.REQUIRED)
    private String triggerCode;

    @NotBlank(message = "triggerName 不能为空")
    @Schema(description = "触发点名称", example = "医嘱-AMI筛查", requiredMode = Schema.RequiredMode.REQUIRED)
    private String triggerName;

    @Schema(description = "触发点类型", example = "ORDER")
    private String triggerType;

    @Schema(description = "业务场景", example = "ORDER")
    private String businessScenario;

    @Schema(description = "接入策略", example = "API")
    private String accessStrategy;

    @Schema(description = "适配器编码", example = "HIS_ADAPTER")
    private String adapterCode;

    @Schema(description = "端点URL")
    private String endpointUrl;

    @Schema(description = "关联规则编码（逗号分隔）")
    private String ruleCodes;

    @Schema(description = "关联路径编码（逗号分隔）")
    private String pathwayCodes;

    @Schema(description = "优先级", example = "10")
    private int priority;

    @Schema(description = "风险等级", example = "HIGH")
    private String riskLevel;

    @Schema(description = "超时毫秒数", example = "5000")
    private int timeoutMs;

    @Schema(description = "是否启用", example = "Y")
    private String enabled;

    @Schema(description = "描述")
    private String description;

    public String getTriggerCode() { return triggerCode; }
    public void setTriggerCode(String triggerCode) { this.triggerCode = triggerCode; }
    public String getTriggerName() { return triggerName; }
    public void setTriggerName(String triggerName) { this.triggerName = triggerName; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getBusinessScenario() { return businessScenario; }
    public void setBusinessScenario(String businessScenario) { this.businessScenario = businessScenario; }
    public String getAccessStrategy() { return accessStrategy; }
    public void setAccessStrategy(String accessStrategy) { this.accessStrategy = accessStrategy; }
    public String getAdapterCode() { return adapterCode; }
    public void setAdapterCode(String adapterCode) { this.adapterCode = adapterCode; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public String getRuleCodes() { return ruleCodes; }
    public void setRuleCodes(String ruleCodes) { this.ruleCodes = ruleCodes; }
    public String getPathwayCodes() { return pathwayCodes; }
    public void setPathwayCodes(String pathwayCodes) { this.pathwayCodes = pathwayCodes; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
