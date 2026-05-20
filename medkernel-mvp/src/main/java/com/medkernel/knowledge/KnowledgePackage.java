package com.medkernel.knowledge;

import java.time.LocalDateTime;

public class KnowledgePackage {
    private Long id;
    private Long tenantId;
    private String packageCode;
    private String packageName;
    private String packageVersion;
    private String description;
    private String exportType;         // FULL/INCREMENTAL
    private String status;             // DRAFT/EXPORTED/IMPORTED/SYNCED/FAILED
    private String sourceTenantId;     // 来源租户ID
    private String sourceTenantName;
    private String targetTenantId;     // 目标租户ID
    private String targetTenantName;
    private int ruleCount;
    private int terminologyCount;
    private int pathwayCount;
    private int graphCount;
    private int sourceCount;
    private String contentHash;        // 内容哈希
    private String contentJson;        // 导出内容 JSON
    private String conflictStrategy;   // SKIP/OVERWRITE/MERGE
    private String syncMode;           // MANUAL/SCHEDULED/REALTIME
    private String syncStatus;         // IDLE/SYNCING/SYNCED/ERROR
    private String syncError;
    private LocalDateTime syncTime;
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

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceTenantId() {
        return sourceTenantId;
    }

    public void setSourceTenantId(String sourceTenantId) {
        this.sourceTenantId = sourceTenantId;
    }

    public String getSourceTenantName() {
        return sourceTenantName;
    }

    public void setSourceTenantName(String sourceTenantName) {
        this.sourceTenantName = sourceTenantName;
    }

    public String getTargetTenantId() {
        return targetTenantId;
    }

    public void setTargetTenantId(String targetTenantId) {
        this.targetTenantId = targetTenantId;
    }

    public String getTargetTenantName() {
        return targetTenantName;
    }

    public void setTargetTenantName(String targetTenantName) {
        this.targetTenantName = targetTenantName;
    }

    public int getRuleCount() {
        return ruleCount;
    }

    public void setRuleCount(int ruleCount) {
        this.ruleCount = ruleCount;
    }

    public int getTerminologyCount() {
        return terminologyCount;
    }

    public void setTerminologyCount(int terminologyCount) {
        this.terminologyCount = terminologyCount;
    }

    public int getPathwayCount() {
        return pathwayCount;
    }

    public void setPathwayCount(int pathwayCount) {
        this.pathwayCount = pathwayCount;
    }

    public int getGraphCount() {
        return graphCount;
    }

    public void setGraphCount(int graphCount) {
        this.graphCount = graphCount;
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public String getConflictStrategy() {
        return conflictStrategy;
    }

    public void setConflictStrategy(String conflictStrategy) {
        this.conflictStrategy = conflictStrategy;
    }

    public String getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(String syncMode) {
        this.syncMode = syncMode;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getSyncError() {
        return syncError;
    }

    public void setSyncError(String syncError) {
        this.syncError = syncError;
    }

    public LocalDateTime getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(LocalDateTime syncTime) {
        this.syncTime = syncTime;
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
