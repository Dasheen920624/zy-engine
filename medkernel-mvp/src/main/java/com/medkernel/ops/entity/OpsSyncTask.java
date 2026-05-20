package com.medkernel.ops.entity;

import java.time.LocalDateTime;

/**
 * 异步任务实体：记录异步任务执行状态和重试信息
 *
 * <p>任务状态流转：
 * <pre>
 *   PENDING → RUNNING → COMPLETED
 *                    ↘ FAILED → RETRYING → RUNNING (重试)
 *                           ↘ CANCELLED (手动取消)
 * </pre>
 *
 * <p>任务类型枚举：
 * <ul>
 *   <li>KNOWLEDGE_SYNC - 知识库同步</li>
 *   <li>GRAPH_SYNC - 图谱同步</li>
 *   <li>CONFIG_SYNC - 配置同步</li>
 *   <li>EXPORT - 数据导出</li>
 *   <li>IMPORT - 数据导入</li>
 * </ul>
 */
public class OpsSyncTask {

    /** 主键ID */
    private Long id;

    /** 所属租户ID */
    private Long tenantId;

    /** 任务编码，租户内唯一 */
    private String taskCode;

    /** 任务类型：KNOWLEDGE_SYNC/GRAPH_SYNC/CONFIG_SYNC/EXPORT/IMPORT */
    private String taskType;

    /** 任务状态：PENDING/RUNNING/COMPLETED/FAILED/RETRYING/CANCELLED */
    private String status;

    /** 当前重试次数 */
    private Integer retryCount;

    /** 最大重试次数，默认3 */
    private Integer maxRetries;

    /** 错误信息（最后一次执行失败时） */
    private String errorMessage;

    /** 执行结果摘要 */
    private String resultSummary;

    /** 计划执行时间 */
    private LocalDateTime scheduledTime;

    /** 实际开始时间 */
    private LocalDateTime startedTime;

    /** 完成时间 */
    private LocalDateTime completedTime;

    /** 触发人（user_id 或 SCHEDULED/SYSTEM） */
    private String triggeredBy;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;

    // ==================== 状态常量 ====================

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_RETRYING = "RETRYING";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // ==================== 任务类型常量 ====================

    public static final String TYPE_KNOWLEDGE_SYNC = "KNOWLEDGE_SYNC";
    public static final String TYPE_GRAPH_SYNC = "GRAPH_SYNC";
    public static final String TYPE_CONFIG_SYNC = "CONFIG_SYNC";
    public static final String TYPE_EXPORT = "EXPORT";
    public static final String TYPE_IMPORT = "IMPORT";

    // ==================== Getter/Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public LocalDateTime getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(LocalDateTime startedTime) {
        this.startedTime = startedTime;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    // ==================== 业务方法 ====================

    /**
     * 判断任务是否处于终态（已完成、失败、已取消）
     */
    public boolean isTerminal() {
        return STATUS_COMPLETED.equals(status)
                || STATUS_FAILED.equals(status)
                || STATUS_CANCELLED.equals(status);
    }

    /**
     * 判断任务是否可以重试
     */
    public boolean canRetry() {
        return STATUS_FAILED.equals(status)
                && retryCount != null
                && maxRetries != null
                && retryCount < maxRetries;
    }

    /**
     * 判断任务是否正在执行中
     */
    public boolean isRunning() {
        return STATUS_RUNNING.equals(status);
    }

    /**
     * 判断任务是否待执行
     */
    public boolean isPending() {
        return STATUS_PENDING.equals(status) || STATUS_RETRYING.equals(status);
    }
}
