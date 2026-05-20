package com.medkernel.security.usersync.entity;

import java.time.LocalDateTime;

/**
 * 身份绑定实体：外部用户到平台用户的映射
 */
public class IdentityBinding {
    private Long id;
    private Long tenantId;
    private Long platformUserId;
    private Long sourceId;
    private String externalId;
    private String externalUsername;
    private String externalDisplayName;
    private String bindingStatus;
    private LocalDateTime lastSyncTime;
    private String syncHash;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getPlatformUserId() { return platformUserId; }
    public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getExternalUsername() { return externalUsername; }
    public void setExternalUsername(String externalUsername) { this.externalUsername = externalUsername; }

    public String getExternalDisplayName() { return externalDisplayName; }
    public void setExternalDisplayName(String externalDisplayName) { this.externalDisplayName = externalDisplayName; }

    public String getBindingStatus() { return bindingStatus; }
    public void setBindingStatus(String bindingStatus) { this.bindingStatus = bindingStatus; }

    public LocalDateTime getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }

    public String getSyncHash() { return syncHash; }
    public void setSyncHash(String syncHash) { this.syncHash = syncHash; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
