package com.medkernel.security.usersync.entity;

import java.time.LocalDateTime;

/**
 * 同步日志实体：记录每个用户的同步结果
 */
public class SyncLog {
    private Long id;
    private Long tenantId;
    private Long taskId;
    private String externalId;
    private String externalUsername;
    private Long platformUserId;
    private String operation;
    private String status;
    private String errorMessage;
    private String syncData;
    private LocalDateTime createdTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getExternalUsername() { return externalUsername; }
    public void setExternalUsername(String externalUsername) { this.externalUsername = externalUsername; }

    public Long getPlatformUserId() { return platformUserId; }
    public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getSyncData() { return syncData; }
    public void setSyncData(String syncData) { this.syncData = syncData; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
