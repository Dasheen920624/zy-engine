package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 创建红队场景请求 DTO：替代 AiSafetyController.createScenario 的 RedTeamScenario 实体。
 */
@Schema(description = "创建红队场景请求")
public class CreateScenarioRequest {

    @NotBlank(message = "scenarioCode 不能为空")
    @Schema(description = "场景编码", example = "RTS_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scenarioCode;

    @NotBlank(message = "scenarioName 不能为空")
    @Schema(description = "场景名称", example = "药物过敏忽略测试", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scenarioName;

    @NotBlank(message = "category 不能为空")
    @Schema(description = "场景分类", example = "SAFETY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "输入模板")
    private String inputTemplate;

    @Schema(description = "期望行为")
    private String expectedBehavior;

    @Schema(description = "风险等级", example = "HIGH")
    private String riskLevel;

    @Schema(description = "是否启用", example = "Y")
    private String enabled;

    public String getScenarioCode() { return scenarioCode; }
    public void setScenarioCode(String scenarioCode) { this.scenarioCode = scenarioCode; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInputTemplate() { return inputTemplate; }
    public void setInputTemplate(String inputTemplate) { this.inputTemplate = inputTemplate; }
    public String getExpectedBehavior() { return expectedBehavior; }
    public void setExpectedBehavior(String expectedBehavior) { this.expectedBehavior = expectedBehavior; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
}
