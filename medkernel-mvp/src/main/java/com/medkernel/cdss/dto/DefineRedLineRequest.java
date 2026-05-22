package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 定义红线请求 DTO：用于 SafetyRedLineController.defineRedLine。
 */
@Schema(description = "定义红线请求")
public class DefineRedLineRequest {

    @NotBlank(message = "redLineCode 不能为空")
    @Schema(description = "红线编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String redLineCode;

    @NotBlank(message = "redLineName 不能为空")
    @Schema(description = "红线名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String redLineName;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "条件表达式")
    private String conditionExpression;

    @Schema(description = "阻断动作")
    private String blockingAction;

    @Schema(description = "严重程度")
    private String severity;

    @Schema(description = "适用场景")
    private String applicableScenarios;

    @Schema(description = "是否启用")
    private String enabled;

    @Schema(description = "创建人")
    private String createdBy;

    public String getRedLineCode() { return redLineCode; }
    public void setRedLineCode(String redLineCode) { this.redLineCode = redLineCode; }
    public String getRedLineName() { return redLineName; }
    public void setRedLineName(String redLineName) { this.redLineName = redLineName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getConditionExpression() { return conditionExpression; }
    public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }
    public String getBlockingAction() { return blockingAction; }
    public void setBlockingAction(String blockingAction) { this.blockingAction = blockingAction; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getApplicableScenarios() { return applicableScenarios; }
    public void setApplicableScenarios(String applicableScenarios) { this.applicableScenarios = applicableScenarios; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
