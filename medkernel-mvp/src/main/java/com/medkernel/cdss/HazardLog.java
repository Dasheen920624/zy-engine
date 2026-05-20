package com.medkernel.cdss;

import java.time.LocalDateTime;

public class HazardLog {
    private Long id;
    private Long tenantId;
    private String hazardCode;
    private String hazardName;
    private String hazardCategory;     // CLINICAL/TECHNICAL/ORGANIZATIONAL/AI_SPECIFIC
    private String hazardDescription;
    private String affectedProcess;    // ORDER/PRESCRIPTION/DIAGNOSIS/PATHWAY/CDSS
    private String likelihood;         // RARE/UNLIKELY/POSSIBLE/LIKELY/ALMOST_CERTAIN
    private String severity;           // NEGLIGIBLE/MINOR/MODERATE/MAJOR/CATASTROPHIC
    private String riskLevel;          // LOW/MEDIUM/HIGH/CRITICAL
    private String controlMeasures;    // 控制措施描述
    private String residualRisk;       // 剩余风险
    private String status;             // IDENTIFIED/ANALYZED/CONTROLLED/ACCEPTED/CLOSED
    private String acceptedBy;         // 院方风险接受人
    private LocalDateTime acceptedTime;
    private String acceptanceNote;
    private String blockingStrategy;   // WARN/BLOCK/ESCALATE/REQUIRE_DUAL_CONFIRM
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

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

    public String getHazardCode() {
        return hazardCode;
    }

    public void setHazardCode(String hazardCode) {
        this.hazardCode = hazardCode;
    }

    public String getHazardName() {
        return hazardName;
    }

    public void setHazardName(String hazardName) {
        this.hazardName = hazardName;
    }

    public String getHazardCategory() {
        return hazardCategory;
    }

    public void setHazardCategory(String hazardCategory) {
        this.hazardCategory = hazardCategory;
    }

    public String getHazardDescription() {
        return hazardDescription;
    }

    public void setHazardDescription(String hazardDescription) {
        this.hazardDescription = hazardDescription;
    }

    public String getAffectedProcess() {
        return affectedProcess;
    }

    public void setAffectedProcess(String affectedProcess) {
        this.affectedProcess = affectedProcess;
    }

    public String getLikelihood() {
        return likelihood;
    }

    public void setLikelihood(String likelihood) {
        this.likelihood = likelihood;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getControlMeasures() {
        return controlMeasures;
    }

    public void setControlMeasures(String controlMeasures) {
        this.controlMeasures = controlMeasures;
    }

    public String getResidualRisk() {
        return residualRisk;
    }

    public void setResidualRisk(String residualRisk) {
        this.residualRisk = residualRisk;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAcceptedBy() {
        return acceptedBy;
    }

    public void setAcceptedBy(String acceptedBy) {
        this.acceptedBy = acceptedBy;
    }

    public LocalDateTime getAcceptedTime() {
        return acceptedTime;
    }

    public void setAcceptedTime(LocalDateTime acceptedTime) {
        this.acceptedTime = acceptedTime;
    }

    public String getAcceptanceNote() {
        return acceptanceNote;
    }

    public void setAcceptanceNote(String acceptanceNote) {
        this.acceptanceNote = acceptanceNote;
    }

    public String getBlockingStrategy() {
        return blockingStrategy;
    }

    public void setBlockingStrategy(String blockingStrategy) {
        this.blockingStrategy = blockingStrategy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
