package com.medkernel.security.audit;

import java.time.LocalDateTime;

/**
 * 审计链校验点实体：记录审计链完整性校验结果。
 */
public class AuditChainCheckpoint {
    private Long id;
    private LocalDateTime checkpointTime;
    private Long lastCheckedId;
    private String chainStatus; // VALID, BROKEN, IN_PROGRESS
    private long totalRecords;
    private long validRecords;
    private long brokenRecords;
    private Long firstBrokenId;
    private String details;
    private String createdBy;
    private LocalDateTime createdTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getCheckpointTime() { return checkpointTime; }
    public void setCheckpointTime(LocalDateTime checkpointTime) { this.checkpointTime = checkpointTime; }

    public Long getLastCheckedId() { return lastCheckedId; }
    public void setLastCheckedId(Long lastCheckedId) { this.lastCheckedId = lastCheckedId; }

    public String getChainStatus() { return chainStatus; }
    public void setChainStatus(String chainStatus) { this.chainStatus = chainStatus; }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public long getValidRecords() { return validRecords; }
    public void setValidRecords(long validRecords) { this.validRecords = validRecords; }

    public long getBrokenRecords() { return brokenRecords; }
    public void setBrokenRecords(long brokenRecords) { this.brokenRecords = brokenRecords; }

    public Long getFirstBrokenId() { return firstBrokenId; }
    public void setFirstBrokenId(Long firstBrokenId) { this.firstBrokenId = firstBrokenId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
