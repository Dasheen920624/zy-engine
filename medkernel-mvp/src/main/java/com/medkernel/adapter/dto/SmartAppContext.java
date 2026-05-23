package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * SMART on FHIR 应用上下文 DTO：替代 SmartAppQueryRequest 中的 Map&lt;String, Object&gt; context。
 */
@Schema(description = "SMART on FHIR 应用上下文")
public class SmartAppContext {

    @Schema(description = "患者ID", example = "P_AMI_001")
    private String patientId;

    @Schema(description = "就诊ID", example = "E_AMI_001")
    private String encounterId;

    @Schema(description = "用户ID", example = "DR_SMITH")
    private String userId;

    @Schema(description = "授权范围", example = "patient/*.read")
    private String scope;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
}
