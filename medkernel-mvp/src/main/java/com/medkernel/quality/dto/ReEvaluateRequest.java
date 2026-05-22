package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 重新评估请求 DTO：替代 EvalReportController.reEvaluate 的参数。
 */
@Schema(description = "重新评估请求")
public class ReEvaluateRequest {

    @NotBlank(message = "evalId 不能为空")
    @Schema(description = "评估记录ID", example = "EVAL_20240101_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String evalId;

    @NotNull(message = "inputData 不能为空")
    @Schema(description = "输入数据", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> inputData;

    public String getEvalId() { return evalId; }
    public void setEvalId(String evalId) { this.evalId = evalId; }
    public Map<String, Object> getInputData() { return inputData; }
    public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }
}
