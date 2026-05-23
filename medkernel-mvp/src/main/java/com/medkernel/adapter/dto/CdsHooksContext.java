package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * CDS Hooks 上下文 DTO：替代 CdsHooksQueryRequest 中的 Map&lt;String, Object&gt; context。
 */
@Schema(description = "CDS Hooks 上下文")
public class CdsHooksContext {

    @Schema(description = "患者ID", example = "P_AMI_001")
    private String patientId;

    @Schema(description = "就诊ID", example = "E_AMI_001")
    private String encounterId;

    @Schema(description = "用户ID", example = "DR_SMITH")
    private String userId;

    @Schema(description = "选中资源列表（FHIR资源引用）")
    private List<String> selections;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<String> getSelections() { return selections; }
    public void setSelections(List<String> selections) { this.selections = selections; }
}
