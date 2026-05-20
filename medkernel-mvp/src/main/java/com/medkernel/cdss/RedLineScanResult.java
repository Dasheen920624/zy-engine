package com.medkernel.cdss;

import java.time.LocalDateTime;

public class RedLineScanResult {
    private Long id;
    private Long tenantId;
    private String scanCode;
    private String scanType;           // MANUAL/SCHEDULED/REALTIME
    private String redLineCode;
    private String redLineName;
    private String category;
    private String patientId;
    private String encounterId;
    private String triggerContext;     // 触发上下文 JSON
    private String violationDetail;    // 违反详情
    private String severity;
    private String blockingAction;
    private String status;             // DETECTED/BLOCKED/OVERRIDDEN/RESOLVED
    private String overriddenBy;
    private String overrideReason;
    private String resolvedBy;
    private String resolutionNote;
    private LocalDateTime resolvedTime;
    private String scanBy;
    private LocalDateTime scanTime;
    private LocalDateTime createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getScanCode() {
        return scanCode;
    }

    public void setScanCode(String scanCode) {
        this.scanCode = scanCode;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public String getRedLineCode() {
        return redLineCode;
    }

    public void setRedLineCode(String redLineCode) {
        this.redLineCode = redLineCode;
    }

    public String getRedLineName() {
        return redLineName;
    }

    public void setRedLineName(String redLineName) {
        this.redLineName = redLineName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getTriggerContext() {
        return triggerContext;
    }

    public void setTriggerContext(String triggerContext) {
        this.triggerContext = triggerContext;
    }

    public String getViolationDetail() {
        return violationDetail;
    }

    public void setViolationDetail(String violationDetail) {
        this.violationDetail = violationDetail;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getBlockingAction() {
        return blockingAction;
    }

    public void setBlockingAction(String blockingAction) {
        this.blockingAction = blockingAction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOverriddenBy() {
        return overriddenBy;
    }

    public void setOverriddenBy(String overriddenBy) {
        this.overriddenBy = overriddenBy;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }

    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    public String getScanBy() {
        return scanBy;
    }

    public void setScanBy(String scanBy) {
        this.scanBy = scanBy;
    }

    public LocalDateTime getScanTime() {
        return scanTime;
    }

    public void setScanTime(LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
