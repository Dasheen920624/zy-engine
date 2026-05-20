package com.medkernel.adapter.entity;

import com.medkernel.persistence.Ids;

import java.time.LocalDateTime;

/**
 * 适配器认证凭据实体
 * 对应表：adp_credential
 */
public class AdapterCredentialEntity {

    private Long id;
    private String tenantId;
    private String hospitalCode;
    private String adapterCode;
    private String credentialType;
    private String credentialKey;
    private String credentialValue;
    private LocalDateTime expiresAt;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public AdapterCredentialEntity() {
    }

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

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
    }

    public String getAdapterCode() {
        return adapterCode;
    }

    public void setAdapterCode(String adapterCode) {
        this.adapterCode = adapterCode;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public String getCredentialKey() {
        return credentialKey;
    }

    public void setCredentialKey(String credentialKey) {
        this.credentialKey = credentialKey;
    }

    public String getCredentialValue() {
        return credentialValue;
    }

    public void setCredentialValue(String credentialValue) {
        this.credentialValue = credentialValue;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    /**
     * 初始化默认值
     */
    public void initDefaults() {
        if (this.id == null) {
            this.id = Ids.next();
        }
        if (this.tenantId == null) {
            this.tenantId = "default";
        }
        if (this.hospitalCode == null) {
            this.hospitalCode = "DEFAULT_HOSPITAL";
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
        if (this.createdTime == null) {
            this.createdTime = LocalDateTime.now();
        }
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 检查凭据是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "AdapterCredentialEntity{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", hospitalCode='" + hospitalCode + '\'' +
                ", adapterCode='" + adapterCode + '\'' +
                ", credentialType='" + credentialType + '\'' +
                ", credentialKey='" + credentialKey + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}