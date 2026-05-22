package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 覆盖扫描结果请求 DTO：用于 SafetyRedLineController.overrideScanResult。
 */
@Schema(description = "覆盖扫描结果请求")
public class OverrideScanResultRequest {

    @NotBlank(message = "overriddenBy 不能为空")
    @Schema(description = "覆盖人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String overriddenBy;

    @NotBlank(message = "overrideReason 不能为空")
    @Schema(description = "覆盖原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private String overrideReason;

    public String getOverriddenBy() { return overriddenBy; }
    public void setOverriddenBy(String overriddenBy) { this.overriddenBy = overriddenBy; }
    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
}
