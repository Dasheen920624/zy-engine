package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新验收测试用例请求 DTO：替代 AcceptanceTestController.updateTestCase 的参数。
 */
@Schema(description = "更新验收测试用例请求")
public class UpdateTestCaseRequest {

    @Schema(description = "用例名称", example = "药物推荐准确性测试")
    private String caseName;

    @Schema(description = "用例分类", example = "FUNCTIONAL")
    private String category;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "测试步骤")
    private String steps;

    @Schema(description = "预期结果")
    private String expectedResult;

    @Schema(description = "状态", example = "ACTIVE")
    private String status;

    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSteps() { return steps; }
    public void setSteps(String steps) { this.steps = steps; }
    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
