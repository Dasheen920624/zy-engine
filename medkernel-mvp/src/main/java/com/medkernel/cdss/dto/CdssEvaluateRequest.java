package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * CDSS评估请求 DTO：用于 CdssController.evaluate。
 */
@Schema(description = "CDSS评估请求")
public class CdssEvaluateRequest {

    @NotBlank(message = "triggerPoint 不能为空")
    @Schema(description = "触发点", requiredMode = Schema.RequiredMode.REQUIRED)
    private String triggerPoint;

    @Schema(description = "患者上下文")
    private Map<String, Object> patientContext;

    public String getTriggerPoint() { return triggerPoint; }
    public void setTriggerPoint(String triggerPoint) { this.triggerPoint = triggerPoint; }
    public Map<String, Object> getPatientContext() { return patientContext; }
    public void setPatientContext(Map<String, Object> patientContext) { this.patientContext = patientContext; }
}
