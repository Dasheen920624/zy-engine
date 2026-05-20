package com.medkernel.patient;

import java.time.LocalDateTime;

/**
 * 患者标识映射实体：记录同一患者在不同系统中的标识映射关系。
 * 对应表 mpi_patient_identity。
 */
public class PatientIdentity {
    private Long id;
    private String tenantId;
    private String platformPatientId;
    private String identityType;      // HIS_PATIENT_ID / EMR_PATIENT_ID / INSURANCE_ID / OUTPATIENT_ID / INPATIENT_ID / PHYSICAL_CARD_NO
    private String externalId;
    private String idHash;            // SHA-256 hash
    private String sourceSystem;
    private String status;            // ACTIVE / INACTIVE / MERGED / CONFLICT
    private Integer confidence;       // 0-100
    private Boolean manuallyVerified;
    private String verifiedBy;
    private LocalDateTime verifiedTime;
    private Long mergedToId;
    private String remarks;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPlatformPatientId() {
        return platformPatientId;
    }

    public void setPlatformPatientId(String platformPatientId) {
        this.platformPatientId = platformPatientId;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getIdHash() {
        return idHash;
    }

    public void setIdHash(String idHash) {
        this.idHash = idHash;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public Boolean getManuallyVerified() {
        return manuallyVerified;
    }

    public void setManuallyVerified(Boolean manuallyVerified) {
        this.manuallyVerified = manuallyVerified;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getVerifiedTime() {
        return verifiedTime;
    }

    public void setVerifiedTime(LocalDateTime verifiedTime) {
        this.verifiedTime = verifiedTime;
    }

    public Long getMergedToId() {
        return mergedToId;
    }

    public void setMergedToId(Long mergedToId) {
        this.mergedToId = mergedToId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}