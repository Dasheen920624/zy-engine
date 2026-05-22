package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;

/**
 * 执行红队测试请求 DTO：替代 AiSafetyController.executeRedTeamTest 的参数。
 */
@Schema(description = "执行红队测试请求")
public class ExecuteRedTeamTestRequest {

    @NotNull(message = "scenarioId 不能为空")
    @Schema(description = "场景ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long scenarioId;

    @Schema(description = "模型编码", example = "GLM_5")
    private String modelCode;

    @Schema(description = "模型版本", example = "1.0")
    private String modelVersion;

    @Schema(description = "执行人", example = "测试工程师A")
    private String executedBy;

    public Long getScenarioId() { return scenarioId; }
    public void setScenarioId(Long scenarioId) { this.scenarioId = scenarioId; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
}
