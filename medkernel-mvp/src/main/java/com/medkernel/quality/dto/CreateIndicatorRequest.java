package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 创建评估指标请求 DTO：替代 EvalController.createIndicator 的 EvalIndicator 实体。
 */
@Schema(description = "创建评估指标请求")
public class CreateIndicatorRequest {

    @NotBlank(message = "indicatorCode 不能为空")
    @Schema(description = "指标编码", example = "IND_ACC_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String indicatorCode;

    @NotBlank(message = "indicatorName 不能为空")
    @Schema(description = "指标名称", example = "诊断准确率", requiredMode = Schema.RequiredMode.REQUIRED)
    private String indicatorName;

    @Schema(description = "权重", example = "0.3")
    private String weight;

    @Schema(description = "阈值", example = "0.8")
    private String threshold;

    @Schema(description = "风险映射", example = "DIAGNOSIS_RISK")
    private String riskMapping;

    @Schema(description = "计算表达式", example = "correctCount / totalCount")
    private String calculationExpression;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "来源文档编码", example = "DOC_001")
    private String sourceDocumentCode;

    public String getIndicatorCode() { return indicatorCode; }
    public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
    public String getIndicatorName() { return indicatorName; }
    public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }
    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
    public String getThreshold() { return threshold; }
    public void setThreshold(String threshold) { this.threshold = threshold; }
    public String getRiskMapping() { return riskMapping; }
    public void setRiskMapping(String riskMapping) { this.riskMapping = riskMapping; }
    public String getCalculationExpression() { return calculationExpression; }
    public void setCalculationExpression(String calculationExpression) { this.calculationExpression = calculationExpression; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceDocumentCode() { return sourceDocumentCode; }
    public void setSourceDocumentCode(String sourceDocumentCode) { this.sourceDocumentCode = sourceDocumentCode; }
}
