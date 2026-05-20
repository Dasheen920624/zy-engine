package com.medkernel.security.sso;

import java.time.LocalDateTime;

/**
 * SSO 审计日志实体：记录 SSO 相关操作。
 * 对应表 sec_sso_audit_log。
 */
public class SsoAuditLog {
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long configId;
    private String eventType;
    private String eventResult;
    private String externalSubject;
    private String errorCode;
    private String errorMessage;
    private String ipAddress;
    private String userAgent;
    private String traceId;
    private LocalDateTime createdTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getEventResult() { return eventResult; }
    public void setEventResult(String eventResult) { this.eventResult = eventResult; }
    public String getExternalSubject() { return externalSubject; }
    public void setExternalSubject(String externalSubject) { this.externalSubject = externalSubject; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}