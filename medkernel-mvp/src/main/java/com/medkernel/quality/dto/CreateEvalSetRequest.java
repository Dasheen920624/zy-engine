package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 创建评估指标集请求 DTO：替代 EvalController.createSet 的 EvalIndicatorSet 实体。
 */
@Schema(description = "创建评估指标集请求")
public class CreateEvalSetRequest {

    @NotBlank(message = "setCode 不能为空")
    @Schema(description = "指标集编码", example = "EVAL_SET_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String setCode;

    @NotBlank(message = "setName 不能为空")
    @Schema(description = "指标集名称", example = "AI诊断准确性评估集", requiredMode = Schema.RequiredMode.REQUIRED)
    private String setName;

    @NotBlank(message = "subjectType 不能为空")
    @Schema(description = "评估主体类型", example = "AI_MODEL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subjectType;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "版本", example = "1.0")
    private String version;

    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }
    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
