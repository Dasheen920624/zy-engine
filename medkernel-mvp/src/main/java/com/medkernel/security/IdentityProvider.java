package com.medkernel.security;

import java.time.LocalDateTime;

/**
 * 身份源配置实体：HIS/EMR/OA/统一身份平台等。
 * 对应表 sec_identity_provider。
 */
public class IdentityProvider {
    private Long id;
    private Long tenantId;
    private String providerCode;
    private String providerName;
    private String providerType;
    private String adapterCode;
    private String syncMode;
    private String syncCron;
    private int priority;
    private String status;
    private LocalDateTime lastSyncTime;
    private String lastSyncResult;
    private String lastSyncSummary;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getAdapterCode() { return adapterCode; }
    public void setAdapterCode(String adapterCode) { this.adapterCode = adapterCode; }
    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
    public String getSyncCron() { return syncCron; }
    public void setSyncCron(String syncCron) { this.syncCron = syncCron; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
    public String getLastSyncResult() { return lastSyncResult; }
    public void setLastSyncResult(String lastSyncResult) { this.lastSyncResult = lastSyncResult; }
    public String getLastSyncSummary() { return lastSyncSummary; }
    public void setLastSyncSummary(String lastSyncSummary) { this.lastSyncSummary = lastSyncSummary; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
