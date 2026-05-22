package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 解除警报请求 DTO：用于 CdssController.resolveAlert。
 */
@Schema(description = "解除警报请求")
public class ResolveAlertRequest {

    @NotBlank(message = "overrideType 不能为空")
    @Schema(description = "覆盖类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String overrideType;

    @Schema(description = "覆盖原因")
    private String overrideReason;

    @Schema(description = "操作人")
    private String operatorName;

    @Schema(description = "主管确认人")
    private String supervisorName;

    public String getOverrideType() { return overrideType; }
    public void setOverrideType(String overrideType) { this.overrideType = overrideType; }
    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }
}
