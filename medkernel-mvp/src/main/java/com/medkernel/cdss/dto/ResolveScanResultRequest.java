package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 解除扫描结果请求 DTO：用于 SafetyRedLineController.resolveScanResult。
 */
@Schema(description = "解除扫描结果请求")
public class ResolveScanResultRequest {

    @NotBlank(message = "resolvedBy 不能为空")
    @Schema(description = "解除人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resolvedBy;

    @Schema(description = "解除备注")
    private String resolutionNote;

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
}
