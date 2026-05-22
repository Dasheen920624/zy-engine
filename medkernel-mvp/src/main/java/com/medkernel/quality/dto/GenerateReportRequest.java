package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 生成评估报告请求 DTO：替代 EvalReportController.generateReport 的参数。
 */
@Schema(description = "生成评估报告请求")
public class GenerateReportRequest {

    @NotBlank(message = "evalId 不能为空")
    @Schema(description = "评估记录ID", example = "EVAL_20240101_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String evalId;

    public String getEvalId() { return evalId; }
    public void setEvalId(String evalId) { this.evalId = evalId; }
}
