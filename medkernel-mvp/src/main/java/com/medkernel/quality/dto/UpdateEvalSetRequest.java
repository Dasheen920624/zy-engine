package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新评估指标集请求 DTO：替代 EvalController.updateSet 的参数。
 */
@Schema(description = "更新评估指标集请求")
public class UpdateEvalSetRequest {

    @Schema(description = "指标集名称", example = "AI诊断准确性评估集")
    private String setName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "版本", example = "1.1")
    private String version;

    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
}
