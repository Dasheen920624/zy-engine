package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 执行评估请求 DTO：替代 EvalController.evaluate 的参数。
 */
@Schema(description = "执行评估请求")
public class EvaluateRequest {

    @NotBlank(message = "setCode 不能为空")
    @Schema(description = "指标集编码", example = "EVAL_SET_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String setCode;

    @NotBlank(message = "subjectId 不能为空")
    @Schema(description = "评估主体ID", example = "MODEL_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subjectId;

    @Schema(description = "评估主体名称", example = "AI诊断模型V1")
    private String subjectName;

    @NotNull(message = "inputData 不能为空")
    @Schema(description = "输入数据", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> inputData;

    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public Map<String, Object> getInputData() { return inputData; }
    public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }
}
