package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 记录测试结果请求 DTO：替代 AcceptanceTestController.recordResult 的 AcceptanceTestResult 实体。
 */
@Schema(description = "记录测试结果请求")
public class RecordTestResultRequest {

    @NotBlank(message = "caseCode 不能为空")
    @Schema(description = "用例编码", example = "TC_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String caseCode;

    @NotBlank(message = "verdict 不能为空")
    @Schema(description = "判定结果", example = "PASS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String verdict;

    @Schema(description = "实际结果")
    private String actualResult;

    @Schema(description = "证据引用（逗号分隔）", example = "EVD_001,EVD_002")
    private String evidenceRefs;

    @NotBlank(message = "testedBy 不能为空")
    @Schema(description = "测试人", example = "测试工程师D", requiredMode = Schema.RequiredMode.REQUIRED)
    private String testedBy;

    public String getCaseCode() { return caseCode; }
    public void setCaseCode(String caseCode) { this.caseCode = caseCode; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public String getActualResult() { return actualResult; }
    public void setActualResult(String actualResult) { this.actualResult = actualResult; }
    public String getEvidenceRefs() { return evidenceRefs; }
    public void setEvidenceRefs(String evidenceRefs) { this.evidenceRefs = evidenceRefs; }
    public String getTestedBy() { return testedBy; }
    public void setTestedBy(String testedBy) { this.testedBy = testedBy; }
}
