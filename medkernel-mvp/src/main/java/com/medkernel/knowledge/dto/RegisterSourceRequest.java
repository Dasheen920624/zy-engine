package com.medkernel.knowledge.dto;

import javax.validation.constraints.NotBlank;

/**
 * 注册来源请求 DTO。
 */
public class RegisterSourceRequest {

    @NotBlank(message = "来源编码不能为空")
    private String sourceCode;

    @NotBlank(message = "来源名称不能为空")
    private String sourceName;

    private String sourceType;
    private String authorityLevel;
    private String language;
    private String region;
    private String licenseScope;
    private String description;
    private String endpointUrl;
    private String authType;
    private String authCredentials;
    private String syncInterval;
    private String tenantId;

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getAuthorityLevel() { return authorityLevel; }
    public void setAuthorityLevel(String authorityLevel) { this.authorityLevel = authorityLevel; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getLicenseScope() { return licenseScope; }
    public void setLicenseScope(String licenseScope) { this.licenseScope = licenseScope; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }

    public String getAuthCredentials() { return authCredentials; }
    public void setAuthCredentials(String authCredentials) { this.authCredentials = authCredentials; }

    public String getSyncInterval() { return syncInterval; }
    public void setSyncInterval(String syncInterval) { this.syncInterval = syncInterval; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
