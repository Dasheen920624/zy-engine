package com.medkernel.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 就诊标识映射实体：记录同一就诊事件在不同系统中的标识映射关系。
 * 对应表 mpi_visit_identity。
 */
public class VisitIdentity {
    private Long id;
    private String tenantId;
    private String platformVisitId;
    private String platformPatientId;
    private String visitType;         // OUTPATIENT / INPATIENT / EMERGENCY / PHYSICAL_EXAM
    private String identityType;      // HIS_VISIT_ID / EMR_VISIT_ID / INSURANCE_SETTLEMENT_ID / OUTPATIENT_NO / INPATIENT_NO
    private String externalId;
    private String idHash;            // SHA-256 hash
    private String sourceSystem;
    private LocalDate visitDate;
    private String departmentCode;
    private String status;            // ACTIVE / INACTIVE / MERGED
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

    public String getPlatformVisitId() {
        return platformVisitId;
    }

    public void setPlatformVisitId(String platformVisitId) {
        this.platformVisitId = platformVisitId;
    }

    public String getPlatformPatientId() {
        return platformPatientId;
    }

    public void setPlatformPatientId(String platformPatientId) {
        this.platformPatientId = platformPatientId;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
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

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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