package com.medkernel.security;

import java.time.LocalDateTime;

/**
 * 外部身份绑定实体：院内工号/SSO subject 与平台用户关联。
 * 对应表 sec_identity_binding。
 */
public class IdentityBinding {
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long providerId;
    private String externalSubject;
    private String externalOrgCode;
    private String externalDisplayName;
    private String bindingStatus;
    private LocalDateTime lastVerifiedTime;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public String getExternalSubject() { return externalSubject; }
    public void setExternalSubject(String externalSubject) { this.externalSubject = externalSubject; }
    public String getExternalOrgCode() { return externalOrgCode; }
    public void setExternalOrgCode(String externalOrgCode) { this.externalOrgCode = externalOrgCode; }
    public String getExternalDisplayName() { return externalDisplayName; }
    public void setExternalDisplayName(String externalDisplayName) { this.externalDisplayName = externalDisplayName; }
    public String getBindingStatus() { return bindingStatus; }
    public void setBindingStatus(String bindingStatus) { this.bindingStatus = bindingStatus; }
    public LocalDateTime getLastVerifiedTime() { return lastVerifiedTime; }
    public void setLastVerifiedTime(LocalDateTime lastVerifiedTime) { this.lastVerifiedTime = lastVerifiedTime; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
