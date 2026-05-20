package com.medkernel.security;

import java.time.LocalDateTime;

/**
 * 同步明细实体：记录每个外部用户的同步动作和结果。
 * 对应表 sec_user_sync_detail。
 */
public class UserSyncDetail {
    private Long id;
    private Long jobId;
    private Long tenantId;
    private String externalSubject;
    private String externalName;
    private String action;            // CREATED/UPDATED/DISABLED/SKIPPED/ERROR
    private Long platformUserId;
    private String message;
    private LocalDateTime createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getPlatformUserId() {
        return platformUserId;
    }

    public void setPlatformUserId(Long platformUserId) {
        this.platformUserId = platformUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
