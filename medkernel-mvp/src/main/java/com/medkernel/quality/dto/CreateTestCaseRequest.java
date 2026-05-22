package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 创建验收测试用例请求 DTO：替代 AcceptanceTestController.createTestCase 的 AcceptanceTestCase 实体。
 */
@Schema(description = "创建验收测试用例请求")
public class CreateTestCaseRequest {

    @NotBlank(message = "caseCode 不能为空")
    @Schema(description = "用例编码", example = "TC_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String caseCode;

    @NotBlank(message = "caseName 不能为空")
    @Schema(description = "用例名称", example = "药物推荐准确性测试", requiredMode = Schema.RequiredMode.REQUIRED)
    private String caseName;

    @NotBlank(message = "category 不能为空")
    @Schema(description = "用例分类", example = "FUNCTIONAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    @NotBlank(message = "featureCode 不能为空")
    @Schema(description = "功能编码", example = "FEAT_DRUG_REC", requiredMode = Schema.RequiredMode.REQUIRED)
    private String featureCode;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "测试步骤")
    private String steps;

    @Schema(description = "预期结果")
    private String expectedResult;

    public String getCaseCode() { return caseCode; }
    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }
    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFeatureCode() { return featureCode; }
    public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSteps() { return steps; }
    public void setSteps(String steps) { this.steps = steps; }
    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
}
