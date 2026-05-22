package com.medkernel.config.entity;

import java.time.LocalDateTime;

/**
 * 配置包回滚记录实体
 * 对应表：cfg_package_rollback_record
 */
public class ConfigPackageRollbackRecord {
    private Long id;
    private String tenantId;
    private String packageCode;
    private String packageVersion;
    private String targetVersion;
    private String rollbackType;
    private String status;
    private String preCheckResult;
    private String postCheckResult;
    private String snapshotBefore;
    private String snapshotAfter;
    private String rollbackReason;
    private String approvedBy;
    private LocalDateTime approvedTime;
    private String rolledBackBy;
    private LocalDateTime rolledBackTime;
    private LocalDateTime completedTime;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public String getRollbackType() {
        return rollbackType;
    }

    public void setRollbackType(String rollbackType) {
        this.rollbackType = rollbackType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPreCheckResult() {
        return preCheckResult;
    }

    public void setPreCheckResult(String preCheckResult) {
        this.preCheckResult = preCheckResult;
    }

    public String getPostCheckResult() {
        return postCheckResult;
    }

    public void setPostCheckResult(String postCheckResult) {
        this.postCheckResult = postCheckResult;
    }

    public String getSnapshotBefore() {
        return snapshotBefore;
    }

    public void setSnapshotBefore(String snapshotBefore) {
        this.snapshotBefore = snapshotBefore;
    }

    public String getSnapshotAfter() {
        return snapshotAfter;
    }

    public void setSnapshotAfter(String snapshotAfter) {
        this.snapshotAfter = snapshotAfter;
    }

    public String getRollbackReason() {
        return rollbackReason;
    }

    public void setRollbackReason(String rollbackReason) {
        this.rollbackReason = rollbackReason;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedTime() {
        return approvedTime;
    }

    public void setApprovedTime(LocalDateTime approvedTime) {
        this.approvedTime = approvedTime;
    }

    public String getRolledBackBy() {
        return rolledBackBy;
    }

    public void setRolledBackBy(String rolledBackBy) {
        this.rolledBackBy = rolledBackBy;
    }

    public LocalDateTime getRolledBackTime() {
        return rolledBackTime;
    }

    public void setRolledBackTime(LocalDateTime rolledBackTime) {
        this.rolledBackTime = rolledBackTime;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
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
