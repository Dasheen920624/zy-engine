package com.medkernel.security.sso;

import java.time.LocalDateTime;

/**
 * SSO 会话实体：存储 SSO 登录会话信息。
 * 对应表 sec_sso_session。
 */
public class SsoSession {
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long configId;
    private String externalSubject;
    private String externalName;
    private String externalEmail;
    private String sessionToken;
    private String accessToken;
    private String refreshToken;
    private String idToken;
    private String tokenType;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime refreshExpiresAt;
    private String status;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime lastAccessTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    public String getExternalSubject() { return externalSubject; }
    public void setExternalSubject(String externalSubject) { this.externalSubject = externalSubject; }
    public String getExternalName() { return externalName; }
    public void setExternalName(String externalName) { this.externalName = externalName; }
    public String getExternalEmail() { return externalEmail; }
    public void setExternalEmail(String externalEmail) { this.externalEmail = externalEmail; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getRefreshExpiresAt() { return refreshExpiresAt; }
    public void setRefreshExpiresAt(LocalDateTime refreshExpiresAt) { this.refreshExpiresAt = refreshExpiresAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getLastAccessTime() { return lastAccessTime; }
    public void setLastAccessTime(LocalDateTime lastAccessTime) { this.lastAccessTime = lastAccessTime; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}