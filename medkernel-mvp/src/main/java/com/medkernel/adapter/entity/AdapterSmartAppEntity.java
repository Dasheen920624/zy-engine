package com.medkernel.adapter.entity;

import com.medkernel.persistence.Ids;

import java.time.LocalDateTime;

/**
 * SMART on FHIR应用配置实体
 * 对应表：adp_smart_app
 */
public class AdapterSmartAppEntity {

    private Long id;
    private String tenantId;
    private String hospitalCode;
    private String appId;
    private String appName;
    private String appType;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;
    private String launchUrl;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public AdapterSmartAppEntity() {
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

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getLaunchUrl() {
        return launchUrl;
    }

    public void setLaunchUrl(String launchUrl) {
        this.launchUrl = launchUrl;
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

    @Override
    public String toString() {
        return "AdapterSmartAppEntity{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", hospitalCode='" + hospitalCode + '\'' +
                ", appId='" + appId + '\'' +
                ", appName='" + appName + '\'' +
                ", appType='" + appType + '\'' +
                ", clientId='" + clientId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}