package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 扫描红线请求 DTO：用于 SafetyRedLineController.scanRedLines。
 */
@Schema(description = "扫描红线请求")
public class ScanRedLinesRequest {

    @Schema(description = "患者ID")
    private String patientId;

    @Schema(description = "就诊ID")
    private String encounterId;

    @Schema(description = "扫描类型")
    private String scanType;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public String getScanType() { return scanType; }
    public void setScanType(String scanType) { this.scanType = scanType; }
}
