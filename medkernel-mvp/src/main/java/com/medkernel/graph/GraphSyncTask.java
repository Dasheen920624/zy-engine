package com.medkernel.graph;

import java.time.LocalDateTime;

/**
 * 图谱同步任务实体：记录图谱数据同步到 Neo4j 的执行状态。
 * 对应表 OPS_SYNC_TASK，GRAPH-005 核心交付。
 */
public class GraphSyncTask {
    private Long id;
    private String tenantId;
    private String taskCode;
    private String taskType;
    private String targetSystem;
    private String targetVersion;
    private String status;
    private boolean dryRun;
    private int totalCount;
    private int successCount;
    private int failedCount;
    private int skipCount;
    private String errorMessage;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime startedTime;
    private LocalDateTime finishedTime;
    private Long durationMs;
    private String triggeredBy;
    private String detailJson;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }

    public String getTargetVersion() { return targetVersion; }
    public void setTargetVersion(String targetVersion) { this.targetVersion = targetVersion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public int getSkipCount() { return skipCount; }
    public void setSkipCount(int skipCount) { this.skipCount = skipCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public LocalDateTime getStartedTime() { return startedTime; }
    public void setStartedTime(LocalDateTime startedTime) { this.startedTime = startedTime; }

    public LocalDateTime getFinishedTime() { return finishedTime; }
    public void setFinishedTime(LocalDateTime finishedTime) { this.finishedTime = finishedTime; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
