package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 接受危险请求 DTO：用于 ClinicalSafetyController.acceptHazard。
 */
@Schema(description = "接受危险请求")
public class AcceptHazardRequest {

    @NotBlank(message = "acceptedBy 不能为空")
    @Schema(description = "接受人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String acceptedBy;

    @Schema(description = "接受备注")
    private String acceptanceNote;

    public String getAcceptedBy() { return acceptedBy; }
    public void setAcceptedBy(String acceptedBy) { this.acceptedBy = acceptedBy; }
    public String getAcceptanceNote() { return acceptanceNote; }
    public void setAcceptanceNote(String acceptanceNote) { this.acceptanceNote = acceptanceNote; }
}
