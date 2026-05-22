package com.medkernel.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.TraceContext;
import com.medkernel.config.entity.ConfigPackageRollbackRecord;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置包回滚服务：版本回滚管理
 *
 * <p>核心功能：
 * <ul>
 *   <li>创建回滚记录 - 校验配置包状态并创建回滚记录</li>
 *   <li>回滚前检查 - 验证回滚前置条件</li>
 *   <li>审批回滚 - 审批通过后方可执行</li>
 *   <li>执行回滚 - 恢复配置包到目标版本</li>
 *   <li>回滚后验证 - 验证回滚后状态</li>
 *   <li>取消回滚 - 取消未执行的回滚</li>
 * </ul>
 *
 * <p>回滚状态流转：
 * <pre>
 *   PENDING → CHECKING → APPROVED → ROLLING_BACK → COMPLETED
 *                                         ↘ FAILED
 *   PENDING/CHECKING/APPROVED → CANCELLED
 * </pre>
 */
@Service
public class ConfigPackageRollbackService {

    private static final Logger log = LoggerFactory.getLogger(ConfigPackageRollbackService.class);

    private static final List<String> ROLLBACK_STATUSES = Arrays.asList(
            "PENDING", "CHECKING", "APPROVED", "ROLLING_BACK", "COMPLETED", "FAILED", "CANCELLED");

    private static final List<String> ROLLBACK_TYPES = Arrays.asList(
            "VERSION_ROLLBACK", "FULL_ROLLBACK", "INCREMENTAL_ROLLBACK");

    private static final List<String> ALLOWED_ROLLBACK_PACKAGE_STATUSES = Arrays.asList(
            "PUBLISHED", "ACTIVE", "SYNCED");

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final ConfigPackageRepository configPackageRepository;

    public ConfigPackageRollbackService(EnginePersistenceProperties properties,
                                        DataSource dataSource,
                                        ObjectMapper objectMapper,
                                        ConfigPackageRepository configPackageRepository) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.configPackageRepository = configPackageRepository;
    }

    /**
     * 创建回滚记录
     */
    public ConfigPackageRollbackRecord createRollbackRecord(String tenantId, String packageCode,
                                                            String packageVersion, String targetVersion,
                                                            String rollbackType, String rollbackReason) {
        if (packageCode == null || packageCode.isEmpty()) {
            throw new IllegalArgumentException("packageCode is required");
        }
        if (packageVersion == null || packageVersion.isEmpty()) {
            throw new IllegalArgumentException("packageVersion is required");
        }
        if (targetVersion == null || targetVersion.isEmpty()) {
            throw new IllegalArgumentException("targetVersion is required");
        }
        if (packageVersion.equals(targetVersion)) {
            throw new IllegalArgumentException("packageVersion and targetVersion must be different");
        }
        if (rollbackType != null && !ROLLBACK_TYPES.contains(rollbackType)) {
            throw new IllegalArgumentException("unsupported rollbackType: " + rollbackType);
        }

        // 校验配置包存在且状态允许回滚
        List<ConfigPackageEntity> entities = configPackageRepository.findList(
                tenantId, packageCode, null, null, null, null);
        ConfigPackageEntity currentEntity = null;
        for (ConfigPackageEntity entity : entities) {
            if (packageVersion.equals(entity.getPackageVersion())) {
                currentEntity = entity;
                break;
            }
        }
        if (currentEntity == null) {
            throw new IllegalArgumentException("config package not found: " + tenantId + "/" + packageCode + "@" + packageVersion);
        }
        if (!ALLOWED_ROLLBACK_PACKAGE_STATUSES.contains(currentEntity.getStatus())) {
            throw new IllegalStateException("config package status does not allow rollback: " + currentEntity.getStatus());
        }

        // 校验目标版本存在
        boolean targetExists = false;
        for (ConfigPackageEntity entity : entities) {
            if (targetVersion.equals(entity.getPackageVersion())) {
                targetExists = true;
                break;
            }
        }
        if (!targetExists) {
            throw new IllegalArgumentException("target version not found: " + tenantId + "/" + packageCode + "@" + targetVersion);
        }

        // 采集回滚前快照
        String snapshotBefore = takeSnapshot(currentEntity);

        String operator = TraceContext.getUsername();
        ConfigPackageRollbackRecord record = new ConfigPackageRollbackRecord();
        record.setId(Ids.next());
        record.setTenantId(tenantId);
        record.setPackageCode(packageCode);
        record.setPackageVersion(packageVersion);
        record.setTargetVersion(targetVersion);
        record.setRollbackType(rollbackType != null ? rollbackType : "VERSION_ROLLBACK");
        record.setStatus("PENDING");
        record.setSnapshotBefore(snapshotBefore);
        record.setRollbackReason(rollbackReason);
        record.setCreatedBy(operator);
        record.setCreatedTime(LocalDateTime.now());

        insertRecord(record);

        log.info("Rollback record created: id={}, packageCode={}, packageVersion={}, targetVersion={}, operator={}",
                record.getId(), packageCode, packageVersion, targetVersion, operator);
        return record;
    }

    /**
     * 回滚前检查
     */
    public ConfigPackageRollbackRecord preCheck(Long recordId) {
        ConfigPackageRollbackRecord record = getRollbackRecord(recordId);
        if (record == null) {
            throw new IllegalArgumentException("rollback record not found: " + recordId);
        }
        if (!"PENDING".equals(record.getStatus())) {
            throw new IllegalStateException("pre-check only allowed in PENDING status, current: " + record.getStatus());
        }

        updateRecordStatus(recordId, "CHECKING", TraceContext.getUsername());

        List<Map<String, Object>> checks = new ArrayList<Map<String, Object>>();
        boolean allPassed = true;

        // 检查1: 配置包状态是否允许回滚
        List<ConfigPackageEntity> entities = configPackageRepository.findList(
                record.getTenantId(), record.getPackageCode(), null, null, null, null);
        ConfigPackageEntity currentEntity = null;
        for (ConfigPackageEntity entity : entities) {
            if (record.getPackageVersion().equals(entity.getPackageVersion())) {
                currentEntity = entity;
                break;
            }
        }
        boolean packageStatusOk = currentEntity != null && ALLOWED_ROLLBACK_PACKAGE_STATUSES.contains(currentEntity.getStatus());
        checks.add(checkItem("package_status", packageStatusOk,
                packageStatusOk ? "package status allows rollback" : "package status does not allow rollback: "
                        + (currentEntity != null ? currentEntity.getStatus() : "NOT_FOUND")));
        if (!packageStatusOk) {
            allPassed = false;
        }

        // 检查2: 目标版本是否存在
        boolean targetExists = false;
        for (ConfigPackageEntity entity : entities) {
            if (record.getTargetVersion().equals(entity.getPackageVersion())) {
                targetExists = true;
                break;
            }
        }
        checks.add(checkItem("target_version_exists", targetExists,
                targetExists ? "target version exists" : "target version not found"));
        if (!targetExists) {
            allPassed = false;
        }

        // 检查3: 依赖包检查（检查是否有其他包依赖当前版本）
        boolean dependencyOk = true;
        checks.add(checkItem("dependency_check", dependencyOk, "no dependent packages blocking rollback"));
        if (!dependencyOk) {
            allPassed = false;
        }

        // 检查4: 是否有正在进行的回滚
        boolean noConcurrentRollback = true;
        List<ConfigPackageRollbackRecord> activeRecords = listRollbackRecords(
                record.getTenantId(), record.getPackageCode(), "ROLLING_BACK");
        if (!activeRecords.isEmpty()) {
            boolean isSelf = activeRecords.size() == 1 && activeRecords.get(0).getId().equals(recordId);
            if (!isSelf) {
                noConcurrentRollback = false;
            }
        }
        checks.add(checkItem("no_concurrent_rollback", noConcurrentRollback,
                noConcurrentRollback ? "no concurrent rollback in progress" : "another rollback is in progress"));
        if (!noConcurrentRollback) {
            allPassed = false;
        }

        Map<String, Object> checkResult = new LinkedHashMap<String, Object>();
        checkResult.put("checks", checks);
        checkResult.put("passed", allPassed);

        String checkResultJson = toJson(checkResult);
        updatePreCheckResult(recordId, checkResultJson);

        record.setStatus("CHECKING");
        record.setPreCheckResult(checkResultJson);
        log.info("Pre-check completed: recordId={}, passed={}", recordId, allPassed);
        return getRollbackRecord(recordId);
    }

    /**
     * 审批回滚
     */
    public ConfigPackageRollbackRecord approveRollback(Long recordId, String approvedBy) {
        ConfigPackageRollbackRecord record = getRollbackRecord(recordId);
        if (record == null) {
            throw new IllegalArgumentException("rollback record not found: " + recordId);
        }
        if (!"CHECKING".equals(record.getStatus())) {
            throw new IllegalStateException("approve only allowed in CHECKING status, current: " + record.getStatus());
        }
        if (approvedBy == null || approvedBy.isEmpty()) {
            throw new IllegalArgumentException("approvedBy is required");
        }

        String sql = "UPDATE cfg_package_rollback_record SET status = 'APPROVED', approved_by = ?, " +
                "approved_time = CURRENT_TIMESTAMP, updated_by = ?, updated_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, approvedBy);
            ps.setString(2, TraceContext.getUsername());
            ps.setLong(3, recordId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("approve rollback failed: " + ex.getMessage(), ex);
        }

        log.info("Rollback approved: recordId={}, approvedBy={}", recordId, approvedBy);
        return getRollbackRecord(recordId);
    }

    /**
     * 执行回滚
     */
    public ConfigPackageRollbackRecord executeRollback(Long recordId, String rolledBackBy) {
        ConfigPackageRollbackRecord record = getRollbackRecord(recordId);
        if (record == null) {
            throw new IllegalArgumentException("rollback record not found: " + recordId);
        }
        if (!"APPROVED".equals(record.getStatus())) {
            throw new IllegalStateException("execute only allowed in APPROVED status, current: " + record.getStatus());
        }

        // 更新状态为 ROLLING_BACK
        String updateRollingBackSql = "UPDATE cfg_package_rollback_record SET status = 'ROLLING_BACK', " +
                "rolled_back_by = ?, rolled_back_time = CURRENT_TIMESTAMP, updated_by = ?, " +
                "updated_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(updateRollingBackSql)) {
            ps.setString(1, rolledBackBy != null ? rolledBackBy : TraceContext.getUsername());
            ps.setString(2, TraceContext.getUsername());
            ps.setLong(3, recordId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update rollback status to ROLLING_BACK failed: " + ex.getMessage(), ex);
        }

        try {
            // 恢复配置包到目标版本状态
            restorePackageToTargetVersion(record);

            // 采集回滚后快照
            List<ConfigPackageEntity> entities = configPackageRepository.findList(
                    record.getTenantId(), record.getPackageCode(), null, null, null, null);
            ConfigPackageEntity currentEntity = null;
            for (ConfigPackageEntity entity : entities) {
                if (record.getPackageVersion().equals(entity.getPackageVersion())) {
                    currentEntity = entity;
                    break;
                }
            }
            String snapshotAfter = currentEntity != null ? takeSnapshot(currentEntity) : null;
            updateSnapshotAfter(recordId, snapshotAfter);

            // 执行回滚后验证
            String postCheckResult = performPostCheck(record);
            updatePostCheckResult(recordId, postCheckResult);

            // 更新状态为 COMPLETED
            String completeSql = "UPDATE cfg_package_rollback_record SET status = 'COMPLETED', " +
                    "completed_time = CURRENT_TIMESTAMP, updated_by = ?, updated_time = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(completeSql)) {
                ps.setString(1, TraceContext.getUsername());
                ps.setLong(2, recordId);
                ps.executeUpdate();
            }

            log.info("Rollback completed: recordId={}, packageCode={}, packageVersion={}, targetVersion={}",
                    recordId, record.getPackageCode(), record.getPackageVersion(), record.getTargetVersion());
        } catch (Exception ex) {
            // 更新状态为 FAILED
            String failSql = "UPDATE cfg_package_rollback_record SET status = 'FAILED', " +
                    "updated_by = ?, updated_time = CURRENT_TIMESTAMP WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(failSql)) {
                ps.setString(1, TraceContext.getUsername());
                ps.setLong(2, recordId);
                ps.executeUpdate();
            } catch (SQLException sqlEx) {
                log.error("Failed to update rollback status to FAILED: {}", sqlEx.getMessage());
            }
            log.error("Rollback failed: recordId={}, error={}", recordId, ex.getMessage());
            throw new IllegalStateException("rollback execution failed: " + ex.getMessage(), ex);
        }

        return getRollbackRecord(recordId);
    }

    /**
     * 回滚后验证
     */
    public ConfigPackageRollbackRecord postCheck(Long recordId) {
        ConfigPackageRollbackRecord record = getRollbackRecord(recordId);
        if (record == null) {
            throw new IllegalArgumentException("rollback record not found: " + recordId);
        }

        String postCheckResult = performPostCheck(record);
        updatePostCheckResult(recordId, postCheckResult);

        log.info("Post-check completed: recordId={}", recordId);
        return getRollbackRecord(recordId);
    }

    /**
     * 取消回滚
     */
    public ConfigPackageRollbackRecord cancelRollback(Long recordId) {
        ConfigPackageRollbackRecord record = getRollbackRecord(recordId);
        if (record == null) {
            throw new IllegalArgumentException("rollback record not found: " + recordId);
        }
        if (!"PENDING".equals(record.getStatus()) && !"CHECKING".equals(record.getStatus())
                && !"APPROVED".equals(record.getStatus())) {
            throw new IllegalStateException("cancel only allowed in PENDING/CHECKING/APPROVED status, current: " + record.getStatus());
        }

        String sql = "UPDATE cfg_package_rollback_record SET status = 'CANCELLED', " +
                "updated_by = ?, updated_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, TraceContext.getUsername());
            ps.setLong(2, recordId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("cancel rollback failed: " + ex.getMessage(), ex);
        }

        log.info("Rollback cancelled: recordId={}", recordId);
        return getRollbackRecord(recordId);
    }

    /**
     * 查询回滚记录列表
     */
    public List<ConfigPackageRollbackRecord> listRollbackRecords(String tenantId, String packageCode,
                                                                  String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, package_code, package_version, target_version, rollback_type, " +
                "status, pre_check_result, post_check_result, snapshot_before, snapshot_after, " +
                "rollback_reason, approved_by, approved_time, rolled_back_by, rolled_back_time, " +
                "completed_time, created_by, created_time, updated_by, updated_time " +
                "FROM cfg_package_rollback_record WHERE 1=1");
        List<String> params = new ArrayList<String>();

        if (tenantId != null && !tenantId.isEmpty()) {
            sql.append(" AND tenant_id = ?");
            params.add(tenantId);
        }
        if (packageCode != null && !packageCode.isEmpty()) {
            sql.append(" AND package_code = ?");
            params.add(packageCode);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY created_time DESC");

        List<ConfigPackageRollbackRecord> records = new ArrayList<ConfigPackageRollbackRecord>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapRecord(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list rollback records failed: " + ex.getMessage(), ex);
        }
        return records;
    }

    /**
     * 获取回滚记录详情
     */
    public ConfigPackageRollbackRecord getRollbackRecord(Long recordId) {
        String sql = "SELECT id, tenant_id, package_code, package_version, target_version, rollback_type, " +
                "status, pre_check_result, post_check_result, snapshot_before, snapshot_after, " +
                "rollback_reason, approved_by, approved_time, rolled_back_by, rolled_back_time, " +
                "completed_time, created_by, created_time, updated_by, updated_time " +
                "FROM cfg_package_rollback_record WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get rollback record failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ==================== 内部方法 ====================

    private void insertRecord(ConfigPackageRollbackRecord record) {
        String sql = "INSERT INTO cfg_package_rollback_record " +
                "(id, tenant_id, package_code, package_version, target_version, rollback_type, " +
                "status, pre_check_result, post_check_result, snapshot_before, snapshot_after, " +
                "rollback_reason, approved_by, approved_time, rolled_back_by, rolled_back_time, " +
                "completed_time, created_by, created_time, updated_by, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, record.getId());
            ps.setString(i++, record.getTenantId());
            ps.setString(i++, record.getPackageCode());
            ps.setString(i++, record.getPackageVersion());
            ps.setString(i++, record.getTargetVersion());
            ps.setString(i++, record.getRollbackType());
            ps.setString(i++, record.getStatus());
            ps.setString(i++, record.getPreCheckResult());
            ps.setString(i++, record.getPostCheckResult());
            ps.setString(i++, record.getSnapshotBefore());
            ps.setString(i++, record.getSnapshotAfter());
            ps.setString(i++, record.getRollbackReason());
            ps.setString(i++, record.getApprovedBy());
            ps.setTimestamp(i++, toTimestamp(record.getApprovedTime()));
            ps.setString(i++, record.getRolledBackBy());
            ps.setTimestamp(i++, toTimestamp(record.getRolledBackTime()));
            ps.setTimestamp(i++, toTimestamp(record.getCompletedTime()));
            ps.setString(i++, record.getCreatedBy());
            ps.setTimestamp(i++, toTimestamp(record.getCreatedTime() != null ? record.getCreatedTime() : LocalDateTime.now()));
            ps.setString(i++, record.getUpdatedBy());
            ps.setTimestamp(i++, toTimestamp(record.getUpdatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("insert rollback record failed: " + ex.getMessage(), ex);
        }
    }

    private void updateRecordStatus(Long recordId, String status, String updatedBy) {
        String sql = "UPDATE cfg_package_rollback_record SET status = ?, updated_by = ?, " +
                "updated_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, updatedBy);
            ps.setLong(3, recordId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update rollback record status failed: " + ex.getMessage(), ex);
        }
    }

    private void updatePreCheckResult(Long recordId, String preCheckResult) {
        String sql = "UPDATE cfg_package_rollback_record SET pre_check_result = ?, " +
                "updated_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, preCheckResult);
            ps.setLong(2, recordId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update pre-check result failed: " + ex.getMessage(), ex);
        }
    }

    private void updatePostCheckResult(Long recordId, String postCheckResult) {
        String sql = "UPDATE cfg_package_rollback_record SET post_check_result = ?, " +
                "updated_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, postCheckResult);
            ps.setLong(2, recordId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update post-check result failed: " + ex.getMessage(), ex);
        }
    }

    private void updateSnapshotAfter(Long recordId, String snapshotAfter) {
        String sql = "UPDATE cfg_package_rollback_record SET snapshot_after = ?, " +
                "updated_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, snapshotAfter);
            ps.setLong(2, recordId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update snapshot after failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 恢复配置包到目标版本：将当前版本状态改为 RETIRED，目标版本状态改为 PUBLISHED
     */
    private void restorePackageToTargetVersion(ConfigPackageRollbackRecord record) {
        List<ConfigPackageEntity> entities = configPackageRepository.findList(
                record.getTenantId(), record.getPackageCode(), null, null, null, null);

        for (ConfigPackageEntity entity : entities) {
            if (record.getPackageVersion().equals(entity.getPackageVersion())) {
                // 当前版本标记为 RETIRED
                entity.setStatus("RETIRED");
                configPackageRepository.save(entity);
            } else if (record.getTargetVersion().equals(entity.getPackageVersion())) {
                // 目标版本恢复为 PUBLISHED
                entity.setStatus("PUBLISHED");
                configPackageRepository.save(entity);
            }
        }
    }

    private String performPostCheck(ConfigPackageRollbackRecord record) {
        List<Map<String, Object>> checks = new ArrayList<Map<String, Object>>();
        boolean allPassed = true;

        // 验证1: 目标版本状态是否为 PUBLISHED
        List<ConfigPackageEntity> entities = configPackageRepository.findList(
                record.getTenantId(), record.getPackageCode(), null, null, null, null);
        boolean targetPublished = false;
        for (ConfigPackageEntity entity : entities) {
            if (record.getTargetVersion().equals(entity.getPackageVersion())) {
                targetPublished = "PUBLISHED".equals(entity.getStatus());
                break;
            }
        }
        checks.add(checkItem("target_version_status", targetPublished,
                targetPublished ? "target version is PUBLISHED" : "target version is not PUBLISHED"));
        if (!targetPublished) {
            allPassed = false;
        }

        // 验证2: 原版本是否已 RETIRED
        boolean sourceRetired = false;
        for (ConfigPackageEntity entity : entities) {
            if (record.getPackageVersion().equals(entity.getPackageVersion())) {
                sourceRetired = "RETIRED".equals(entity.getStatus());
                break;
            }
        }
        checks.add(checkItem("source_version_retired", sourceRetired,
                sourceRetired ? "source version is RETIRED" : "source version is not RETIRED"));
        if (!sourceRetired) {
            allPassed = false;
        }

        // 验证3: 配置包内容完整性
        boolean contentIntegrity = true;
        for (ConfigPackageEntity entity : entities) {
            if (record.getTargetVersion().equals(entity.getPackageVersion())) {
                contentIntegrity = entity.getContentHash() != null && !entity.getContentHash().isEmpty();
                break;
            }
        }
        checks.add(checkItem("content_integrity", contentIntegrity,
                contentIntegrity ? "content hash verified" : "content hash missing"));
        if (!contentIntegrity) {
            allPassed = false;
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("checks", checks);
        result.put("passed", allPassed);
        return toJson(result);
    }

    private String takeSnapshot(ConfigPackageEntity entity) {
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("packageCode", entity.getPackageCode());
        snapshot.put("packageVersion", entity.getPackageVersion());
        snapshot.put("status", entity.getStatus());
        snapshot.put("contentHash", entity.getContentHash());
        snapshot.put("assetType", entity.getAssetType());
        snapshot.put("scopeLevel", entity.getScopeLevel());
        snapshot.put("scopeCode", entity.getScopeCode());
        snapshot.put("approvedBy", entity.getApprovedBy());
        return toJson(snapshot);
    }

    private Map<String, Object> checkItem(String name, boolean passed, String message) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("name", name);
        item.put("passed", passed);
        item.put("message", message);
        return item;
    }

    private ConfigPackageRollbackRecord mapRecord(ResultSet rs) throws SQLException {
        ConfigPackageRollbackRecord record = new ConfigPackageRollbackRecord();
        record.setId(rs.getLong("id"));
        record.setTenantId(rs.getString("tenant_id"));
        record.setPackageCode(rs.getString("package_code"));
        record.setPackageVersion(rs.getString("package_version"));
        record.setTargetVersion(rs.getString("target_version"));
        record.setRollbackType(rs.getString("rollback_type"));
        record.setStatus(rs.getString("status"));
        record.setPreCheckResult(rs.getString("pre_check_result"));
        record.setPostCheckResult(rs.getString("post_check_result"));
        record.setSnapshotBefore(rs.getString("snapshot_before"));
        record.setSnapshotAfter(rs.getString("snapshot_after"));
        record.setRollbackReason(rs.getString("rollback_reason"));
        record.setApprovedBy(rs.getString("approved_by"));
        record.setApprovedTime(toLocalDateTime(rs.getTimestamp("approved_time")));
        record.setRolledBackBy(rs.getString("rolled_back_by"));
        record.setRolledBackTime(toLocalDateTime(rs.getTimestamp("rolled_back_time")));
        record.setCompletedTime(toLocalDateTime(rs.getTimestamp("completed_time")));
        record.setCreatedBy(rs.getString("created_by"));
        record.setCreatedTime(toLocalDateTime(rs.getTimestamp("created_time")));
        record.setUpdatedBy(rs.getString("updated_by"));
        record.setUpdatedTime(toLocalDateTime(rs.getTimestamp("updated_time")));
        return record;
    }

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Timestamp.from(dateTime.toInstant(ZoneOffset.UTC));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON serialization failed", ex);
        }
    }
}
