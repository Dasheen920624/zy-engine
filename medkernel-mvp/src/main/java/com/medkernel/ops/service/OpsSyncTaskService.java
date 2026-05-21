package com.medkernel.ops.service;

import com.medkernel.common.TraceContext;
import com.medkernel.ops.entity.OpsSyncTask;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步任务服务：统一的异步任务执行、重试、审计机制
 *
 * <p>核心功能：
 * <ul>
 *   <li>任务创建与调度 - 支持立即执行和定时执行</li>
 *   <li>异步执行 - 使用线程池异步执行任务</li>
 *   <li>自动重试 - 失败任务自动重试，支持可配置的最大重试次数</li>
 *   <li>状态管理 - 完整的任务状态流转</li>
 *   <li>审计日志 - 记录任务状态变更</li>
 * </ul>
 */
@Service
public class OpsSyncTaskService {

    private static final Logger log = LoggerFactory.getLogger(OpsSyncTaskService.class);

    private final EnginePersistenceProperties properties;
    private final ExecutorService executorService;
    private final DataSource dataSource;

    public OpsSyncTaskService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.executorService = Executors.newFixedThreadPool(4);
        this.dataSource = dataSource;
    }

    // ==================== 任务查询 ====================

    /**
     * 获取租户下所有任务
     */
    public List<OpsSyncTask> listTasks(Long tenantId) {
        String sql = "SELECT id, tenant_id, task_code, task_type, status, retry_count, "
                + "max_retries, error_message, result_summary, scheduled_time, started_time, "
                + "completed_time, triggered_by, created_by, created_time, updated_by, updated_time "
                + "FROM ops_sync_task WHERE tenant_id = ? ORDER BY created_time DESC";
        List<OpsSyncTask> tasks = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapTask(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list ops tasks failed: " + ex.getMessage(), ex);
        }
        return tasks;
    }

    /**
     * 根据状态筛选任务
     */
    public List<OpsSyncTask> listTasksByStatus(Long tenantId, String status) {
        String sql = "SELECT id, tenant_id, task_code, task_type, status, retry_count, "
                + "max_retries, error_message, result_summary, scheduled_time, started_time, "
                + "completed_time, triggered_by, created_by, created_time, updated_by, updated_time "
                + "FROM ops_sync_task WHERE tenant_id = ? AND status = ? ORDER BY created_time DESC";
        List<OpsSyncTask> tasks = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapTask(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list ops tasks by status failed: " + ex.getMessage(), ex);
        }
        return tasks;
    }

    /**
     * 根据任务类型筛选任务
     */
    public List<OpsSyncTask> listTasksByType(Long tenantId, String taskType) {
        String sql = "SELECT id, tenant_id, task_code, task_type, status, retry_count, "
                + "max_retries, error_message, result_summary, scheduled_time, started_time, "
                + "completed_time, triggered_by, created_by, created_time, updated_by, updated_time "
                + "FROM ops_sync_task WHERE tenant_id = ? AND task_type = ? ORDER BY created_time DESC";
        List<OpsSyncTask> tasks = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, taskType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapTask(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list ops tasks by type failed: " + ex.getMessage(), ex);
        }
        return tasks;
    }

    /**
     * 根据ID获取任务
     */
    public OpsSyncTask getTask(Long tenantId, Long taskId) {
        String sql = "SELECT id, tenant_id, task_code, task_type, status, retry_count, "
                + "max_retries, error_message, result_summary, scheduled_time, started_time, "
                + "completed_time, triggered_by, created_by, created_time, updated_by, updated_time "
                + "FROM ops_sync_task WHERE tenant_id = ? AND id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTask(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get ops task failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据任务编码获取任务
     */
    public OpsSyncTask getTaskByCode(Long tenantId, String taskCode) {
        String sql = "SELECT id, tenant_id, task_code, task_type, status, retry_count, "
                + "max_retries, error_message, result_summary, scheduled_time, started_time, "
                + "completed_time, triggered_by, created_by, created_time, updated_by, updated_time "
                + "FROM ops_sync_task WHERE tenant_id = ? AND task_code = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, taskCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTask(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get ops task by code failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ==================== 任务创建 ====================

    /**
     * 创建异步任务（立即执行）
     *
     * @param tenantId  租户ID
     * @param taskCode  任务编码
     * @param taskType  任务类型
     * @param maxRetries 最大重试次数
     * @return 创建的任务
     */
    public OpsSyncTask createTask(Long tenantId, String taskCode, String taskType, Integer maxRetries) {
        return createTask(tenantId, taskCode, taskType, maxRetries, null);
    }

    /**
     * 创建异步任务（支持定时执行）
     *
     * @param tenantId      租户ID
     * @param taskCode      任务编码
     * @param taskType      任务类型
     * @param maxRetries    最大重试次数
     * @param scheduledTime 计划执行时间（null表示立即执行）
     * @return 创建的任务
     */
    public OpsSyncTask createTask(Long tenantId, String taskCode, String taskType,
                                   Integer maxRetries, LocalDateTime scheduledTime) {
        String sql = "INSERT INTO ops_sync_task (id, tenant_id, task_code, task_type, status, "
                + "retry_count, max_retries, scheduled_time, triggered_by, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            ps.setLong(1, id);
            ps.setLong(2, tenantId);
            ps.setString(3, taskCode);
            ps.setString(4, taskType);
            ps.setString(5, OpsSyncTask.STATUS_PENDING);
            ps.setInt(6, 0);
            ps.setInt(7, maxRetries != null ? maxRetries : 3);
            ps.setTimestamp(8, scheduledTime != null ? Timestamp.valueOf(scheduledTime) : null);
            ps.setString(9, TraceContext.getUsername());
            ps.setString(10, TraceContext.getUsername());
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            OpsSyncTask task = new OpsSyncTask();
            task.setId(id);
            task.setTenantId(tenantId);
            task.setTaskCode(taskCode);
            task.setTaskType(taskType);
            task.setStatus(OpsSyncTask.STATUS_PENDING);
            task.setRetryCount(0);
            task.setMaxRetries(maxRetries != null ? maxRetries : 3);
            task.setScheduledTime(scheduledTime);
            task.setTriggeredBy(TraceContext.getUsername());
            task.setCreatedBy(TraceContext.getUsername());
            task.setCreatedTime(LocalDateTime.now());

            log.info("Created ops task: id={}, code={}, type={}", id, taskCode, taskType);
            return task;
        } catch (SQLException ex) {
            throw new IllegalStateException("create ops task failed: " + ex.getMessage(), ex);
        }
    }

    // ==================== 任务执行 ====================

    /**
     * 异步执行任务
     *
     * @param taskId 任务ID
     * @param taskExecutor 任务执行器（业务逻辑）
     */
    public void executeAsync(Long taskId, TaskExecutor taskExecutor) {
        CompletableFuture.runAsync(() -> {
            try {
                executeTask(taskId, taskExecutor);
            } catch (Exception ex) {
                log.error("Async task execution failed: taskId={}", taskId, ex);
            }
        }, executorService);
    }

    /**
     * 同步执行任务（用于测试或需要等待结果的场景）
     *
     * @param taskId 任务ID
     * @param taskExecutor 任务执行器（业务逻辑）
     */
    public void executeSync(Long taskId, TaskExecutor taskExecutor) {
        executeTask(taskId, taskExecutor);
    }

    /**
     * 执行任务核心逻辑
     */
    private void executeTask(Long taskId, TaskExecutor taskExecutor) {
        OpsSyncTask task = getTaskById(taskId);
        if (task == null) {
            log.error("Task not found: taskId={}", taskId);
            return;
        }

        // 检查任务状态
        if (!task.isPending()) {
            log.warn("Task is not in pending state: taskId={}, status={}", taskId, task.getStatus());
            return;
        }

        // 更新为运行中
        updateTaskStatus(taskId, OpsSyncTask.STATUS_RUNNING, null, null);
        log.info("Task started: taskId={}, code={}", taskId, task.getTaskCode());

        try {
            // 执行业务逻辑
            String result = taskExecutor.execute();

            // 执行成功
            updateTaskStatus(taskId, OpsSyncTask.STATUS_COMPLETED, result, null);
            log.info("Task completed: taskId={}, code={}", taskId, task.getTaskCode());

        } catch (Exception ex) {
            log.error("Task failed: taskId={}, code={}", taskId, task.getTaskCode(), ex);

            // 检查是否可以重试
            if (task.canRetry()) {
                // 增加重试次数并设置为重试中
                incrementRetryCount(taskId);
                updateTaskStatus(taskId, OpsSyncTask.STATUS_RETRYING, null, ex.getMessage());
                log.info("Task will retry: taskId={}, retryCount={}", taskId, task.getRetryCount() + 1);

                // 重新执行
                executeTask(taskId, taskExecutor);
            } else {
                // 超过最大重试次数，标记为失败
                updateTaskStatus(taskId, OpsSyncTask.STATUS_FAILED, null, ex.getMessage());
                log.error("Task failed permanently: taskId={}, code={}, maxRetries={}",
                        taskId, task.getTaskCode(), task.getMaxRetries());
            }
        }
    }

    // ==================== 任务状态管理 ====================

    /**
     * 取消任务
     */
    public void cancelTask(Long tenantId, Long taskId) {
        OpsSyncTask task = getTask(tenantId, taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        if (task.isTerminal()) {
            throw new IllegalStateException("Cannot cancel task in terminal state: " + task.getStatus());
        }
        updateTaskStatus(taskId, OpsSyncTask.STATUS_CANCELLED, null, null);
        log.info("Task cancelled: taskId={}, code={}", taskId, task.getTaskCode());
    }

    /**
     * 重试失败的任务
     */
    public void retryTask(Long tenantId, Long taskId) {
        OpsSyncTask task = getTask(tenantId, taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        if (!OpsSyncTask.STATUS_FAILED.equals(task.getStatus())) {
            throw new IllegalStateException("Only failed tasks can be retried: " + task.getStatus());
        }
        if (task.getRetryCount() >= task.getMaxRetries()) {
            throw new IllegalStateException("Task has reached max retries: " + task.getMaxRetries());
        }

        // 重置为待执行状态
        updateTaskStatus(taskId, OpsSyncTask.STATUS_PENDING, null, null);
        log.info("Task reset for retry: taskId={}, code={}", taskId, task.getTaskCode());
    }

    // ==================== 内部方法 ====================

    /**
     * 根据ID获取任务（内部使用，不校验租户）
     */
    private OpsSyncTask getTaskById(Long taskId) {
        String sql = "SELECT id, tenant_id, task_code, task_type, status, retry_count, "
                + "max_retries, error_message, result_summary, scheduled_time, started_time, "
                + "completed_time, triggered_by, created_by, created_time, updated_by, updated_time "
                + "FROM ops_sync_task WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTask(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get ops task by id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(Long taskId, String status, String resultSummary, String errorMessage) {
        String sql = "UPDATE ops_sync_task SET status = ?, result_summary = ?, error_message = ?, "
                + "started_time = CASE WHEN ? = 'RUNNING' AND started_time IS NULL THEN CURRENT_TIMESTAMP ELSE started_time END, "
                + "completed_time = CASE WHEN ? IN ('COMPLETED', 'FAILED', 'CANCELLED') THEN CURRENT_TIMESTAMP ELSE completed_time END, "
                + "updated_by = ?, updated_time = CURRENT_TIMESTAMP "
                + "WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, resultSummary);
            ps.setString(3, errorMessage);
            ps.setString(4, status);
            ps.setString(5, status);
            ps.setString(6, TraceContext.getUsername());
            ps.setLong(7, taskId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update ops task status failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 增加重试次数
     */
    private void incrementRetryCount(Long taskId) {
        String sql = "UPDATE ops_sync_task SET retry_count = retry_count + 1, updated_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("increment retry count failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 映射结果集到实体
     */
    private OpsSyncTask mapTask(ResultSet rs) throws SQLException {
        OpsSyncTask task = new OpsSyncTask();
        task.setId(rs.getLong("id"));
        task.setTenantId(rs.getLong("tenant_id"));
        task.setTaskCode(rs.getString("task_code"));
        task.setTaskType(rs.getString("task_type"));
        task.setStatus(rs.getString("status"));
        task.setRetryCount(rs.getInt("retry_count"));
        task.setMaxRetries(rs.getInt("max_retries"));
        task.setErrorMessage(rs.getString("error_message"));
        task.setResultSummary(rs.getString("result_summary"));

        Timestamp scheduledTime = rs.getTimestamp("scheduled_time");
        if (scheduledTime != null) {
            task.setScheduledTime(scheduledTime.toLocalDateTime());
        }

        Timestamp startedTime = rs.getTimestamp("started_time");
        if (startedTime != null) {
            task.setStartedTime(startedTime.toLocalDateTime());
        }

        Timestamp completedTime = rs.getTimestamp("completed_time");
        if (completedTime != null) {
            task.setCompletedTime(completedTime.toLocalDateTime());
        }

        task.setTriggeredBy(rs.getString("triggered_by"));
        task.setCreatedBy(rs.getString("created_by"));

        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            task.setCreatedTime(createdTime.toLocalDateTime());
        }

        task.setUpdatedBy(rs.getString("updated_by"));

        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            task.setUpdatedTime(updatedTime.toLocalDateTime());
        }

        return task;
    }

    /**
     * 获取数据库连接
     */
    private Connection connection() throws SQLException {
        // PR-FINAL-15b: 璧?HikariCP 杩炴帴姹狅紙EngineDataSourceConfig 鏆撮湶鐨?DataSource锛夈€?        return dataSource.getConnection();
    }

    // ==================== 任务执行器接口 ====================

    /**
     * 任务执行器接口：业务逻辑实现此接口
     */
    @FunctionalInterface
    public interface TaskExecutor {
        /**
         * 执行任务业务逻辑
         *
         * @return 执行结果摘要
         * @throws Exception 执行失败时抛出异常
         */
        String execute() throws Exception;
    }
}
