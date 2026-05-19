package com.medkernel.security;

import java.time.LocalDateTime;

/**
 * 外部身份绑定实体：将平台用户绑定到外部系统身份。
 * 对应表 sec_identity_binding。
 */
public class IdentityBinding {
    private Long id;
    private Long tenantId;
    private Long userId;              // 平台用户 ID
    private Long providerId;          // 身份源 ID
    private String externalSubject;   // 外部系统用户标识（工号）
    private String externalName;      // 外部系统用户姓名
    private String externalOrgCode;   // 外部系统组织编码
    private String externalOrgName;   // 外部系统组织名称
    private String externalPosition;  // 外部系统岗位
    private String status;            // ACTIVE/UNBOUND
    private LocalDateTime lastSyncTime;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getExternalSubject() {
        return externalSubject;
    }

    public void setExternalSubject(String externalSubject) {
        this.externalSubject = externalSubject;
    }

    public String getExternalName() {
        return externalName;
    }

    public void setExternalName(String externalName) {
        this.externalName = externalName;
    }

    public String getExternalOrgCode() {
        return externalOrgCode;
    }

    public void setExternalOrgCode(String externalOrgCode) {
        this.externalOrgCode = externalOrgCode;
    }

    public String getExternalOrgName() {
        return externalOrgName;
    }

    public void setExternalOrgName(String externalOrgName) {
        this.externalOrgName = externalOrgName;
    }

    public String getExternalPosition() {
        return externalPosition;
    }

    public void setExternalPosition(String externalPosition) {
        this.externalPosition = externalPosition;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(LocalDateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
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
