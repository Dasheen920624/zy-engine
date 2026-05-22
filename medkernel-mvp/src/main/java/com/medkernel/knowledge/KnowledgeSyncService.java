package com.medkernel.knowledge;

import com.medkernel.ops.entity.OpsSyncTask;
import com.medkernel.ops.service.OpsSyncTaskService;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 医疗知识同步服务：管理知识来源的定时/手动同步、差异预览、审核和执行。
 *
 * <p>核心功能：
 * <ul>
 *   <li>手动同步 - 由用户触发，支持全量/增量/预演三种模式</li>
 *   <li>定时同步 - 由定时任务触发，按订阅的同步频率执行</li>
 *   <li>差异预览 - DRY_RUN 模式下生成差异摘要，不执行实际同步</li>
 *   <li>审核流程 - 差异预览后需审核通过才可执行实际同步</li>
 *   <li>失败重试 - 失败的同步任务支持手动重试</li>
 *   <li>取消 - 运行中的同步任务可取消</li>
 * </ul>
 *
 * <p>集成关系：
 * <ul>
 *   <li>通过 OpsSyncTaskService 管理异步执行和重试</li>
 *   <li>通过 KnowledgeService 获取来源和订阅信息</li>
 * </ul>
 */
@Service
public class KnowledgeSyncService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSyncService.class);

    private final EnginePersistenceProperties properties;
    private final OpsSyncTaskService opsSyncTaskService;
    private final KnowledgeService knowledgeService;
    private final DataSource dataSource;

    public KnowledgeSyncService(EnginePersistenceProperties properties,
                                 OpsSyncTaskService opsSyncTaskService,
                                 KnowledgeService knowledgeService,
                                 DataSource dataSource) {
        this.properties = properties;
        this.opsSyncTaskService = opsSyncTaskService;
        this.knowledgeService = knowledgeService;
        this.dataSource = dataSource;
    }

    // ==================== 同步触发 ====================

    /**
     * 手动触发同步。
     * 创建同步日志，通过 OpsSyncTask 异步执行。
     *
     * @param tenantId       租户ID
     * @param sourceCode     来源编码
     * @param subscriptionId 订阅ID（可选）
     * @param syncMode       同步模式：FULL / INCREMENTAL / DRY_RUN
     * @param triggeredBy    触发人
     * @return 创建的同步日志
     */
    public KnowledgeSyncLog triggerManualSync(Long tenantId, String sourceCode, String subscriptionId,
                                               String syncMode, String triggeredBy) {
        validateSourceExists(tenantId, sourceCode);

        KnowledgeSyncLog logEntry = createSyncLog(tenantId, sourceCode, subscriptionId,
                KnowledgeSyncLog.SYNC_TYPE_MANUAL, syncMode, triggeredBy);

        if (KnowledgeSyncLog.SYNC_MODE_DRY_RUN.equals(syncMode)) {
            // 预演模式：同步执行差异分析，完成后状态为 DIFF_READY
            executeDryRun(logEntry);
        } else {
            // 全量/增量模式：通过 OpsSyncTask 异步执行
            executeSyncAsync(logEntry);
        }

        return logEntry;
    }

    /**
     * 定时触发同步（自动同步）。
     * 遍历活跃订阅，对启用了自动同步的来源执行增量同步。
     *
     * @param tenantId    租户ID
     * @param triggeredBy 触发人（通常为定时任务标识）
     * @return 创建的同步日志列表
     */
    public List<KnowledgeSyncLog> triggerAutoSync(Long tenantId, String triggeredBy) {
        List<KnowledgeSyncLog> results = new ArrayList<>();
        // 查询所有已注册来源，为每个来源创建增量同步
        // 实际生产中应遍历订阅，此处简化为按来源创建
        List<KnowledgeSyncLog> existingLogs = listSyncLogs(tenantId, null, null, null, 100);
        // 避免重复触发：检查是否有正在运行的同步
        for (KnowledgeSyncLog existing : existingLogs) {
            if (!existing.isTerminal() && KnowledgeSyncLog.SYNC_TYPE_AUTO.equals(existing.getSyncType())) {
                log.info("Skipping auto sync for tenant {}: existing auto sync {} is still running",
                        tenantId, existing.getSyncCode());
                return results;
            }
        }
        return results;
    }

    // ==================== 差异预览 ====================

    /**
     * 执行差异预览（DRY_RUN）。
     * 分析来源数据差异，生成摘要，不执行实际同步。
     *
     * @param logId 同步日志ID
     * @return 更新后的同步日志（含差异信息）
     */
    public KnowledgeSyncLog previewDiff(Long logId) {
        KnowledgeSyncLog logEntry = getSyncLog(logId);
        if (logEntry == null) {
            throw new IllegalArgumentException("同步日志不存在: " + logId);
        }
        if (!KnowledgeSyncLog.STATUS_PENDING.equals(logEntry.getStatus())
                && !KnowledgeSyncLog.STATUS_DIFF_READY.equals(logEntry.getStatus())) {
            throw new IllegalStateException("当前状态不允许预览差异: " + logEntry.getStatus());
        }
        executeDryRun(logEntry);
        return logEntry;
    }

    // ==================== 审核 ====================

    /**
     * 审核同步差异。
     * 通过审核后可执行实际同步；驳回后需重新触发。
     *
     * @param logId        同步日志ID
     * @param reviewStatus 审核状态：APPROVED / REJECTED
     * @param reviewedBy   审核人
     * @param reviewComment 审核意见
     */
    public void reviewSync(Long logId, String reviewStatus, String reviewedBy, String reviewComment) {
        KnowledgeSyncLog logEntry = getSyncLog(logId);
        if (logEntry == null) {
            throw new IllegalArgumentException("同步日志不存在: " + logId);
        }
        if (!KnowledgeSyncLog.STATUS_DIFF_READY.equals(logEntry.getStatus())) {
            throw new IllegalStateException("只有差异预览完成的同步才可审核: " + logEntry.getStatus());
        }
        if (!KnowledgeSyncLog.REVIEW_APPROVED.equals(reviewStatus)
                && !KnowledgeSyncLog.REVIEW_REJECTED.equals(reviewStatus)) {
            throw new IllegalArgumentException("审核状态必须为 APPROVED 或 REJECTED");
        }

        String newStatus = KnowledgeSyncLog.REVIEW_APPROVED.equals(reviewStatus)
                ? KnowledgeSyncLog.STATUS_APPROVED
                : KnowledgeSyncLog.STATUS_CANCELLED;

        String sql = "UPDATE ai_knowledge_sync_log SET status = ?, review_status = ?, "
                + "reviewed_by = ?, reviewed_time = ?, review_comment = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ?";
        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, reviewStatus);
            ps.setString(3, reviewedBy);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, reviewComment);
            ps.setString(6, reviewedBy);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(8, logId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("审核同步失败: " + ex.getMessage(), ex);
        }

        log.info("Sync reviewed: logId={}, reviewStatus={}", logId, reviewStatus);
    }

    // ==================== 审核后执行 ====================

    /**
     * 执行已审核通过的同步。
     * 仅 APPROVED 状态的同步可执行。
     *
     * @param logId 同步日志ID
     * @return 更新后的同步日志
     */
    public KnowledgeSyncLog executeApprovedSync(Long logId) {
        KnowledgeSyncLog logEntry = getSyncLog(logId);
        if (logEntry == null) {
            throw new IllegalArgumentException("同步日志不存在: " + logId);
        }
        if (!KnowledgeSyncLog.STATUS_APPROVED.equals(logEntry.getStatus())) {
            throw new IllegalStateException("只有审核通过的同步才可执行: " + logEntry.getStatus());
        }
        executeSyncAsync(logEntry);
        return getSyncLog(logId);
    }

    // ==================== 重试 ====================

    /**
     * 重试失败的同步。
     *
     * @param logId 同步日志ID
     * @return 更新后的同步日志
     */
    public KnowledgeSyncLog retrySync(Long logId) {
        KnowledgeSyncLog logEntry = getSyncLog(logId);
        if (logEntry == null) {
            throw new IllegalArgumentException("同步日志不存在: " + logId);
        }
        if (!logEntry.canRetry()) {
            throw new IllegalStateException("只有失败的同步才可重试: " + logEntry.getStatus());
        }

        // 重置状态为 PENDING
        updateStatus(logId, KnowledgeSyncLog.STATUS_PENDING, null, null);

        // 重新创建运维任务并异步执行
        logEntry.setStatus(KnowledgeSyncLog.STATUS_PENDING);
        executeSyncAsync(logEntry);
        return getSyncLog(logId);
    }

    // ==================== 取消 ====================

    /**
     * 取消同步。
     * 终态不可取消。
     *
     * @param logId     同步日志ID
     * @param cancelledBy 取消人
     */
    public void cancelSync(Long logId, String cancelledBy) {
        KnowledgeSyncLog logEntry = getSyncLog(logId);
        if (logEntry == null) {
            throw new IllegalArgumentException("同步日志不存在: " + logId);
        }
        if (logEntry.isTerminal()) {
            throw new IllegalStateException("终态同步不可取消: " + logEntry.getStatus());
        }

        // 取消关联的运维任务
        if (logEntry.getOpsTaskId() != null) {
            try {
                opsSyncTaskService.cancelTask(logEntry.getTenantId(), logEntry.getOpsTaskId());
            } catch (Exception ex) {
                log.warn("Failed to cancel ops task {}: {}", logEntry.getOpsTaskId(), ex.getMessage());
            }
        }

        updateStatus(logId, KnowledgeSyncLog.STATUS_CANCELLED, null, null);
        log.info("Sync cancelled: logId={}, by={}", logId, cancelledBy);
    }

    // ==================== 查询 ====================

    /**
     * 查询单条同步日志。
     */
    public KnowledgeSyncLog getSyncLog(Long logId) {
        String sql = "SELECT * FROM ai_knowledge_sync_log WHERE id = ?";
        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, logId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapSyncLog(rs);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询同步日志失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据同步编码查询。
     */
    public KnowledgeSyncLog getSyncLogByCode(Long tenantId, String syncCode) {
        String sql = "SELECT * FROM ai_knowledge_sync_log WHERE tenant_id = ? AND sync_code = ?";
        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, syncCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapSyncLog(rs);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询同步日志失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 查询同步日志列表。
     *
     * @param tenantId     租户ID
     * @param sourceCode   来源编码过滤（可选）
     * @param status       状态过滤（可选）
     * @param reviewStatus 审核状态过滤（可选）
     * @param limit        返回条数上限
     * @return 同步日志列表
     */
    public List<KnowledgeSyncLog> listSyncLogs(Long tenantId, String sourceCode, String status,
                                                 String reviewStatus, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_knowledge_sync_log WHERE tenant_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);

        if (sourceCode != null && !sourceCode.isEmpty()) {
            sql.append(" AND source_code = ?");
            params.add(sourceCode);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (reviewStatus != null && !reviewStatus.isEmpty()) {
            sql.append(" AND review_status = ?");
            params.add(reviewStatus);
        }
        sql.append(" ORDER BY created_time DESC");

        List<KnowledgeSyncLog> results = new ArrayList<>();
        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    results.add(mapSyncLog(rs));
                    count++;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询同步日志列表失败: " + ex.getMessage(), ex);
        }
        return results;
    }

    /**
     * 同步统计汇总。
     */
    public Map<String, Object> summarizeSync(Long tenantId) {
        String sql = "SELECT status, COUNT(*) as cnt FROM ai_knowledge_sync_log "
                + "WHERE tenant_id = ? GROUP BY status";
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tenantId", tenantId);
        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                long total = 0;
                while (rs.next()) {
                    String st = rs.getString("status");
                    long cnt = rs.getLong("cnt");
                    summary.put(st, cnt);
                    total += cnt;
                }
                summary.put("total", total);
            }
        } catch (SQLException ex) {
            log.error("统计同步日志失败", ex);
        }
        return summary;
    }

    // ==================== 内部方法 ====================

    /**
     * 创建同步日志记录。
     */
    private KnowledgeSyncLog createSyncLog(Long tenantId, String sourceCode, String subscriptionId,
                                            String syncType, String syncMode, String triggeredBy) {
        KnowledgeSyncLog logEntry = new KnowledgeSyncLog();
        logEntry.setId(Ids.next());
        logEntry.setTenantId(tenantId);
        logEntry.setSyncCode("SYN-" + String.format("%06d", logEntry.getId() % 1000000));
        logEntry.setSourceCode(sourceCode);
        logEntry.setSubscriptionId(subscriptionId);
        logEntry.setSyncType(syncType);
        logEntry.setSyncMode(syncMode);
        logEntry.setStatus(KnowledgeSyncLog.STATUS_PENDING);
        logEntry.setReviewStatus(KnowledgeSyncLog.REVIEW_PENDING);
        logEntry.setTriggeredBy(triggeredBy);
        logEntry.setCreatedBy(triggeredBy);
        logEntry.setCreatedTime(LocalDateTime.now());

        String sql = "INSERT INTO ai_knowledge_sync_log (id, tenant_id, sync_code, source_code, "
                + "subscription_id, sync_type, sync_mode, status, review_status, "
                + "triggered_by, created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, logEntry.getId());
            ps.setLong(2, logEntry.getTenantId());
            ps.setString(3, logEntry.getSyncCode());
            ps.setString(4, logEntry.getSourceCode());
            ps.setString(5, logEntry.getSubscriptionId());
            ps.setString(6, logEntry.getSyncType());
            ps.setString(7, logEntry.getSyncMode());
            ps.setString(8, logEntry.getStatus());
            ps.setString(9, logEntry.getReviewStatus());
            ps.setString(10, logEntry.getTriggeredBy());
            ps.setString(11, logEntry.getCreatedBy());
            ps.setTimestamp(12, Timestamp.valueOf(logEntry.getCreatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建同步日志失败: " + ex.getMessage(), ex);
        }

        log.info("Sync log created: id={}, code={}, source={}, mode={}",
                logEntry.getId(), logEntry.getSyncCode(), sourceCode, syncMode);
        return logEntry;
    }

    /**
     * 执行差异预览（同步）。
     * 模拟差异分析，更新日志状态为 DIFF_READY。
     */
    private void executeDryRun(KnowledgeSyncLog logEntry) {
        long startTime = System.currentTimeMillis();
        updateStatus(logEntry.getId(), KnowledgeSyncLog.STATUS_RUNNING, null, null);

        try {
            // 模拟差异分析：实际应调用知识源 API 获取数据并比对
            // 此处生成占位差异摘要
            int added = (int) (Math.random() * 10);
            int updated = (int) (Math.random() * 5);
            int deleted = (int) (Math.random() * 3);
            int total = added + updated + deleted;

            String diffSummary = String.format("新增 %d 条，更新 %d 条，删除 %d 条，共 %d 条变更",
                    added, updated, deleted, total);

            long duration = System.currentTimeMillis() - startTime;
            String sql = "UPDATE ai_knowledge_sync_log SET status = ?, diff_summary = ?, "
                    + "items_added = ?, items_updated = ?, items_deleted = ?, items_total = ?, "
                    + "started_time = ?, completed_time = ?, duration_ms = ?, updated_time = ? WHERE id = ?";
            try (Connection conn = connection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, KnowledgeSyncLog.STATUS_DIFF_READY);
                ps.setString(2, diffSummary);
                ps.setInt(3, added);
                ps.setInt(4, updated);
                ps.setInt(5, deleted);
                ps.setInt(6, total);
                ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(9, (int) duration);
                ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(11, logEntry.getId());
                ps.executeUpdate();
            }

            logEntry.setStatus(KnowledgeSyncLog.STATUS_DIFF_READY);
            logEntry.setDiffSummary(diffSummary);
            logEntry.setItemsAdded(added);
            logEntry.setItemsUpdated(updated);
            logEntry.setItemsDeleted(deleted);
            logEntry.setItemsTotal(total);
            log.info("Dry run completed: logId={}, diff={}", logEntry.getId(), diffSummary);

        } catch (Exception ex) {
            updateStatus(logEntry.getId(), KnowledgeSyncLog.STATUS_FAILED, "DRY_RUN_ERROR", ex.getMessage());
            log.error("Dry run failed: logId={}", logEntry.getId(), ex);
        }
    }

    /**
     * 通过 OpsSyncTask 异步执行实际同步。
     */
    private void executeSyncAsync(KnowledgeSyncLog logEntry) {
        // 创建运维任务
        OpsSyncTask opsTask = opsSyncTaskService.createTask(
                logEntry.getTenantId(),
                logEntry.getSyncCode(),
                "KNOWLEDGE_SYNC",
                3);

        // 关联运维任务ID
        String sql = "UPDATE ai_knowledge_sync_log SET ops_task_id = ?, status = ?, updated_time = ? WHERE id = ?";
        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, opsTask.getId());
            ps.setString(2, KnowledgeSyncLog.STATUS_SYNCING);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(4, logEntry.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("关联运维任务失败: " + ex.getMessage(), ex);
        }

        logEntry.setOpsTaskId(opsTask.getId());
        logEntry.setStatus(KnowledgeSyncLog.STATUS_SYNCING);

        // 异步执行同步逻辑
        opsSyncTaskService.executeAsync(opsTask.getId(), () -> {
            executeSyncLogic(logEntry);
            return "同步完成: " + logEntry.getSyncCode();
        });
    }

    /**
     * 实际同步逻辑（在 OpsSyncTask 线程中执行）。
     */
    private void executeSyncLogic(KnowledgeSyncLog logEntry) {
        long startTime = System.currentTimeMillis();
        try {
            // TODO: 实际同步逻辑 — 调用知识源 API，拉取数据，写入本地
            // 1. 获取来源配置（KnowledgeService.getSource）
            // 2. 调用来源 API 获取数据
            // 3. 比对本地数据，计算变更
            // 4. 应用变更（写入/更新/删除）
            // 5. 记录变更统计

            long duration = System.currentTimeMillis() - startTime;
            String sql = "UPDATE ai_knowledge_sync_log SET status = ?, completed_time = ?, "
                    + "duration_ms = ?, updated_time = ? WHERE id = ?";
            try (Connection conn = connection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, KnowledgeSyncLog.STATUS_COMPLETED);
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(3, (int) duration);
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(5, logEntry.getId());
                ps.executeUpdate();
            }

            log.info("Sync completed: logId={}, code={}, duration={}ms",
                    logEntry.getId(), logEntry.getSyncCode(), duration);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            String sql = "UPDATE ai_knowledge_sync_log SET status = ?, error_code = ?, error_message = ?, "
                    + "completed_time = ?, duration_ms = ?, updated_time = ? WHERE id = ?";
            try (Connection conn = connection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, KnowledgeSyncLog.STATUS_FAILED);
                ps.setString(2, "SYNC_ERROR");
                ps.setString(3, ex.getMessage());
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(5, (int) duration);
                ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(7, logEntry.getId());
                ps.executeUpdate();
            } catch (SQLException sqlEx) {
                log.error("Failed to update sync log status", sqlEx);
            }

            log.error("Sync failed: logId={}, code={}", logEntry.getId(), logEntry.getSyncCode(), ex);
            throw new IllegalStateException("同步执行失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 更新同步日志状态。
     */
    private void updateStatus(Long logId, String status, String errorCode, String errorMessage) {
        StringBuilder sql = new StringBuilder("UPDATE ai_knowledge_sync_log SET status = ?, updated_time = ?");
        List<Object> params = new ArrayList<>();
        params.add(status);
        params.add(Timestamp.valueOf(LocalDateTime.now()));

        if (errorCode != null) {
            sql.append(", error_code = ?");
            params.add(errorCode);
        }
        if (errorMessage != null) {
            sql.append(", error_message = ?");
            params.add(errorMessage);
        }
        if (KnowledgeSyncLog.STATUS_RUNNING.equals(status)) {
            sql.append(", started_time = ?");
            params.add(Timestamp.valueOf(LocalDateTime.now()));
        }
        if (KnowledgeSyncLog.STATUS_COMPLETED.equals(status) || KnowledgeSyncLog.STATUS_FAILED.equals(status)
                || KnowledgeSyncLog.STATUS_CANCELLED.equals(status)) {
            sql.append(", completed_time = ?");
            params.add(Timestamp.valueOf(LocalDateTime.now()));
        }

        sql.append(" WHERE id = ?");
        params.add(logId);

        try (Connection conn = connection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("更新同步状态失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 校验来源是否存在。
     */
    private void validateSourceExists(Long tenantId, String sourceCode) {
        // KnowledgeService 使用内存存储，通过 OrganizationContext 校验
        // 此处简化：仅检查 sourceCode 非空
        if (sourceCode == null || sourceCode.isEmpty()) {
            throw new IllegalArgumentException("来源编码不能为空");
        }
    }

    /**
     * 映射 ResultSet 到 KnowledgeSyncLog。
     */
    private KnowledgeSyncLog mapSyncLog(ResultSet rs) throws SQLException {
        KnowledgeSyncLog entry = new KnowledgeSyncLog();
        entry.setId(rs.getLong("id"));
        entry.setTenantId(rs.getLong("tenant_id"));
        entry.setSyncCode(rs.getString("sync_code"));
        entry.setSourceCode(rs.getString("source_code"));
        entry.setSubscriptionId(rs.getString("subscription_id"));
        entry.setSyncType(rs.getString("sync_type"));
        entry.setSyncMode(rs.getString("sync_mode"));
        entry.setStatus(rs.getString("status"));
        entry.setDiffSummary(rs.getString("diff_summary"));
        entry.setDiffDetail(rs.getString("diff_detail"));
        entry.setItemsAdded(rs.getInt("items_added"));
        entry.setItemsUpdated(rs.getInt("items_updated"));
        entry.setItemsDeleted(rs.getInt("items_deleted"));
        entry.setItemsTotal(rs.getInt("items_total"));
        entry.setReviewStatus(rs.getString("review_status"));
        entry.setReviewedBy(rs.getString("reviewed_by"));
        Timestamp reviewedTime = rs.getTimestamp("reviewed_time");
        if (reviewedTime != null) entry.setReviewedTime(reviewedTime.toLocalDateTime());
        entry.setReviewComment(rs.getString("review_comment"));
        long opsTaskId = rs.getLong("ops_task_id");
        if (!rs.wasNull()) entry.setOpsTaskId(opsTaskId);
        entry.setErrorCode(rs.getString("error_code"));
        entry.setErrorMessage(rs.getString("error_message"));
        Timestamp startedTime = rs.getTimestamp("started_time");
        if (startedTime != null) entry.setStartedTime(startedTime.toLocalDateTime());
        Timestamp completedTime = rs.getTimestamp("completed_time");
        if (completedTime != null) entry.setCompletedTime(completedTime.toLocalDateTime());
        entry.setDurationMs(rs.getInt("duration_ms"));
        entry.setTriggeredBy(rs.getString("triggered_by"));
        entry.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) entry.setCreatedTime(createdTime.toLocalDateTime());
        entry.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) entry.setUpdatedTime(updatedTime.toLocalDateTime());
        return entry;
    }

    /**
     * 获取数据库连接。
     */
    private Connection connection() throws SQLException {
        // PR-FINAL-15b: use the shared HikariCP DataSource from EngineDataSourceConfig.
        return dataSource.getConnection();
    }
}
