package com.medkernel.patient;

import java.time.LocalDateTime;

/**
 * 标识冲突实体：记录标识匹配过程中的冲突，支持人工处理。
 * 对应表 mpi_identity_conflict。
 */
public class IdentityConflict {
    private Long id;
    private String tenantId;
    private String conflictType;      // DUPLICATE_EXTERNAL / MULTIPLE_PLATFORM / HASH_MISMATCH / MANUAL_REVIEW
    private String severity;          // HIGH / MEDIUM / LOW
    private String patientIdentityIds; // JSON数组
    private String visitIdentityIds;   // JSON数组
    private String conflictDescription;
    private String status;            // PENDING / IN_PROGRESS / RESOLVED / DISMISSED
    private String resolutionType;    // MERGE / SPLIT / KEEP_BOTH / MANUAL_LINK
    private String resolutionNotes;
    private String resolvedBy;
    private LocalDateTime resolvedTime;
    private Long targetPatientIdentityId;
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

    public String getConflictType() {
        return conflictType;
    }

    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getPatientIdentityIds() {
        return patientIdentityIds;
    }

    public void setPatientIdentityIds(String patientIdentityIds) {
        this.patientIdentityIds = patientIdentityIds;
    }

    public String getVisitIdentityIds() {
        return visitIdentityIds;
    }

    public void setVisitIdentityIds(String visitIdentityIds) {
        this.visitIdentityIds = visitIdentityIds;
    }

    public String getConflictDescription() {
        return conflictDescription;
    }

    public void setConflictDescription(String conflictDescription) {
        this.conflictDescription = conflictDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolutionType() {
        return resolutionType;
    }

    public void setResolutionType(String resolutionType) {
        this.resolutionType = resolutionType;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }

    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    public Long getTargetPatientIdentityId() {
        return targetPatientIdentityId;
    }

    public void setTargetPatientIdentityId(Long targetPatientIdentityId) {
        this.targetPatientIdentityId = targetPatientIdentityId;
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