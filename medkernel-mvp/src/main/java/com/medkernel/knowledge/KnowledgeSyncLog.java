package com.medkernel.knowledge;

import java.time.LocalDateTime;

/**
 * 医疗知识同步日志。
 * 记录知识来源的同步历史、差异分析和审核状态。
 * 支持定时同步（AUTO）、手动同步（MANUAL）两种触发方式；
 * 支持全量（FULL）、增量（INCREMENTAL）、预演（DRY_RUN）三种同步模式。
 *
 * <p>状态机：PENDING → RUNNING → DIFF_READY → APPROVED → SYNCING → COMPLETED / FAILED / CANCELLED
 *
 * <p>对应数据库表：ai_knowledge_sync_log
 */
public class KnowledgeSyncLog {

    // ---- 同步状态常量 ----
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_DIFF_READY = "DIFF_READY";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_SYNCING = "SYNCING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // ---- 同步类型常量 ----
    public static final String SYNC_TYPE_AUTO = "AUTO";
    public static final String SYNC_TYPE_MANUAL = "MANUAL";

    // ---- 同步模式常量 ----
    public static final String SYNC_MODE_FULL = "FULL";
    public static final String SYNC_MODE_INCREMENTAL = "INCREMENTAL";
    public static final String SYNC_MODE_DRY_RUN = "DRY_RUN";

    // ---- 审核状态常量 ----
    public static final String REVIEW_PENDING = "PENDING";
    public static final String REVIEW_APPROVED = "APPROVED";
    public static final String REVIEW_REJECTED = "REJECTED";

    // ---- 字段 ----
    private Long id;
    private Long tenantId;
    private String syncCode;
    private String sourceCode;
    private String subscriptionId;
    private String syncType;        // AUTO / MANUAL
    private String syncMode;        // FULL / INCREMENTAL / DRY_RUN
    private String status;          // 8 种状态
    private String diffSummary;
    private String diffDetail;
    private int itemsAdded;
    private int itemsUpdated;
    private int itemsDeleted;
    private int itemsTotal;
    private String reviewStatus;    // PENDING / APPROVED / REJECTED
    private String reviewedBy;
    private LocalDateTime reviewedTime;
    private String reviewComment;
    private Long opsTaskId;         // 关联 OpsSyncTask
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private int durationMs;
    private String triggeredBy;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    // ---- 状态判断方法 ----

    /** 是否处于终态（完成、失败、取消） */
    public boolean isTerminal() {
        return STATUS_COMPLETED.equals(status)
                || STATUS_FAILED.equals(status)
                || STATUS_CANCELLED.equals(status);
    }

    /** 是否可以重试（仅失败状态可重试） */
    public boolean canRetry() {
        return STATUS_FAILED.equals(status);
    }

    /** 是否可以取消（非终态可取消） */
    public boolean canCancel() {
        return !isTerminal();
    }

    /** 是否已到差异预览阶段 */
    public boolean isDiffReady() {
        return STATUS_DIFF_READY.equals(status)
                || STATUS_APPROVED.equals(status)
                || STATUS_SYNCING.equals(status)
                || STATUS_COMPLETED.equals(status);
    }

    // ---- Getters and Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getSyncCode() { return syncCode; }
    public void setSyncCode(String syncCode) { this.syncCode = syncCode; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }

    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }

    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDiffSummary() { return diffSummary; }
    public void setDiffSummary(String diffSummary) { this.diffSummary = diffSummary; }

    public String getDiffDetail() { return diffDetail; }
    public void setDiffDetail(String diffDetail) { this.diffDetail = diffDetail; }

    public int getItemsAdded() { return itemsAdded; }
    public void setItemsAdded(int itemsAdded) { this.itemsAdded = itemsAdded; }

    public int getItemsUpdated() { return itemsUpdated; }
    public void setItemsUpdated(int itemsUpdated) { this.itemsUpdated = itemsUpdated; }

    public int getItemsDeleted() { return itemsDeleted; }
    public void setItemsDeleted(int itemsDeleted) { this.itemsDeleted = itemsDeleted; }

    public int getItemsTotal() { return itemsTotal; }
    public void setItemsTotal(int itemsTotal) { this.itemsTotal = itemsTotal; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedTime() { return reviewedTime; }
    public void setReviewedTime(LocalDateTime reviewedTime) { this.reviewedTime = reviewedTime; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public Long getOpsTaskId() { return opsTaskId; }
    public void setOpsTaskId(Long opsTaskId) { this.opsTaskId = opsTaskId; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getStartedTime() { return startedTime; }
    public void setStartedTime(LocalDateTime startedTime) { this.startedTime = startedTime; }

    public LocalDateTime getCompletedTime() { return completedTime; }
    public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }

    public int getDurationMs() { return durationMs; }
    public void setDurationMs(int durationMs) { this.durationMs = durationMs; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
