package com.medkernel.security.sso;

import java.time.LocalDateTime;

/**
 * SSO 配置实体：CAS/OIDC/SAML/LDAP-AD 接入策略。
 * 对应表 sec_sso_config。
 */
public class SsoConfig {
    private Long id;
    private Long tenantId;
    private String configCode;
    private String configName;
    private String protocolType;
    private String status;
    private int priority;

    // CAS 配置
    private String casServerUrl;
    private String casServiceUrl;
    private String casCallbackUrl;

    // OIDC 配置
    private String oidcIssuer;
    private String oidcClientId;
    private String oidcClientSecret;
    private String oidcRedirectUri;
    private String oidcScope;
    private String oidcResponseType;
    private String oidcJwksUri;

    // SAML 配置
    private String samlEntityId;
    private String samlSsoUrl;
    private String samlSloUrl;
    private String samlCertificate;
    private String samlMetadataUrl;

    // LDAP-AD 配置
    private String ldapUrl;
    private String ldapBaseDn;
    private String ldapBindDn;
    private String ldapBindPassword;
    private String ldapUserSearchBase;
    private String ldapUserSearchFilter;
    private String ldapGroupSearchBase;
    private String ldapGroupSearchFilter;
    private boolean ldapUseSsl;
    private boolean ldapUseStarttls;

    // 通用配置
    private String attributeMapping;
    private String roleMapping;
    private boolean autoCreateUser;
    private boolean autoUpdateUser;
    private int sessionTimeoutMinutes;

    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getConfigCode() { return configCode; }
    public void setConfigCode(String configCode) { this.configCode = configCode; }
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }
    public String getProtocolType() { return protocolType; }
    public void setProtocolType(String protocolType) { this.protocolType = protocolType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    // CAS
    public String getCasServerUrl() { return casServerUrl; }
    public void setCasServerUrl(String casServerUrl) { this.casServerUrl = casServerUrl; }
    public String getCasServiceUrl() { return casServiceUrl; }
    public void setCasServiceUrl(String casServiceUrl) { this.casServiceUrl = casServiceUrl; }
    public String getCasCallbackUrl() { return casCallbackUrl; }
    public void setCasCallbackUrl(String casCallbackUrl) { this.casCallbackUrl = casCallbackUrl; }

    // OIDC
    public String getOidcIssuer() { return oidcIssuer; }
    public void setOidcIssuer(String oidcIssuer) { this.oidcIssuer = oidcIssuer; }
    public String getOidcClientId() { return oidcClientId; }
    public void setOidcClientId(String oidcClientId) { this.oidcClientId = oidcClientId; }
    public String getOidcClientSecret() { return oidcClientSecret; }
    public void setOidcClientSecret(String oidcClientSecret) { this.oidcClientSecret = oidcClientSecret; }
    public String getOidcRedirectUri() { return oidcRedirectUri; }
    public void setOidcRedirectUri(String oidcRedirectUri) { this.oidcRedirectUri = oidcRedirectUri; }
    public String getOidcScope() { return oidcScope; }
    public void setOidcScope(String oidcScope) { this.oidcScope = oidcScope; }
    public String getOidcResponseType() { return oidcResponseType; }
    public void setOidcResponseType(String oidcResponseType) { this.oidcResponseType = oidcResponseType; }
    public String getOidcJwksUri() { return oidcJwksUri; }
    public void setOidcJwksUri(String oidcJwksUri) { this.oidcJwksUri = oidcJwksUri; }

    // SAML
    public String getSamlEntityId() { return samlEntityId; }
    public void setSamlEntityId(String samlEntityId) { this.samlEntityId = samlEntityId; }
    public String getSamlSsoUrl() { return samlSsoUrl; }
    public void setSamlSsoUrl(String samlSsoUrl) { this.samlSsoUrl = samlSsoUrl; }
    public String getSamlSloUrl() { return samlSloUrl; }
    public void setSamlSloUrl(String samlSloUrl) { this.samlSloUrl = samlSloUrl; }
    public String getSamlCertificate() { return samlCertificate; }
    public void setSamlCertificate(String samlCertificate) { this.samlCertificate = samlCertificate; }
    public String getSamlMetadataUrl() { return samlMetadataUrl; }
    public void setSamlMetadataUrl(String samlMetadataUrl) { this.samlMetadataUrl = samlMetadataUrl; }

    // LDAP
    public String getLdapUrl() { return ldapUrl; }
    public void setLdapUrl(String ldapUrl) { this.ldapUrl = ldapUrl; }
    public String getLdapBaseDn() { return ldapBaseDn; }
    public void setLdapBaseDn(String ldapBaseDn) { this.ldapBaseDn = ldapBaseDn; }
    public String getLdapBindDn() { return ldapBindDn; }
    public void setLdapBindDn(String ldapBindDn) { this.ldapBindDn = ldapBindDn; }
    public String getLdapBindPassword() { return ldapBindPassword; }
    public void setLdapBindPassword(String ldapBindPassword) { this.ldapBindPassword = ldapBindPassword; }
    public String getLdapUserSearchBase() { return ldapUserSearchBase; }
    public void setLdapUserSearchBase(String ldapUserSearchBase) { this.ldapUserSearchBase = ldapUserSearchBase; }
    public String getLdapUserSearchFilter() { return ldapUserSearchFilter; }
    public void setLdapUserSearchFilter(String ldapUserSearchFilter) { this.ldapUserSearchFilter = ldapUserSearchFilter; }
    public String getLdapGroupSearchBase() { return ldapGroupSearchBase; }
    public void setLdapGroupSearchBase(String ldapGroupSearchBase) { this.ldapGroupSearchBase = ldapGroupSearchBase; }
    public String getLdapGroupSearchFilter() { return ldapGroupSearchFilter; }
    public void setLdapGroupSearchFilter(String ldapGroupSearchFilter) { this.ldapGroupSearchFilter = ldapGroupSearchFilter; }
    public boolean isLdapUseSsl() { return ldapUseSsl; }
    public void setLdapUseSsl(boolean ldapUseSsl) { this.ldapUseSsl = ldapUseSsl; }
    public boolean isLdapUseStarttls() { return ldapUseStarttls; }
    public void setLdapUseStarttls(boolean ldapUseStarttls) { this.ldapUseStarttls = ldapUseStarttls; }

    // 通用
    public String getAttributeMapping() { return attributeMapping; }
    public void setAttributeMapping(String attributeMapping) { this.attributeMapping = attributeMapping; }
    public String getRoleMapping() { return roleMapping; }
    public void setRoleMapping(String roleMapping) { this.roleMapping = roleMapping; }
    public boolean isAutoCreateUser() { return autoCreateUser; }
    public void setAutoCreateUser(boolean autoCreateUser) { this.autoCreateUser = autoCreateUser; }
    public boolean isAutoUpdateUser() { return autoUpdateUser; }
    public void setAutoUpdateUser(boolean autoUpdateUser) { this.autoUpdateUser = autoUpdateUser; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}