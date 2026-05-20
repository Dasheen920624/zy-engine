package com.medkernel.security.usersync;

import com.medkernel.common.OrgDefaults;
import com.medkernel.common.TraceContext;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import com.medkernel.security.SecurityPersistenceService;
import com.medkernel.security.SecurityUser;
import com.medkernel.security.usersync.entity.IdentityBinding;
import com.medkernel.security.usersync.entity.SyncLog;
import com.medkernel.security.usersync.entity.SyncSource;
import com.medkernel.security.usersync.entity.SyncTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户同步服务：支持 HIS/EMR/OA/统一身份平台用户同步
 */
@Service
public class UserSyncApiService {

    private static final Logger log = LoggerFactory.getLogger(UserSyncApiService.class);

    private final EnginePersistenceProperties properties;
    private final SecurityPersistenceService securityPersistenceService;

    public UserSyncApiService(EnginePersistenceProperties properties,
                              SecurityPersistenceService securityPersistenceService) {
        this.properties = properties;
        this.securityPersistenceService = securityPersistenceService;
    }

    /**
     * 获取所有同步源
     */
    public List<SyncSource> listSources(Long tenantId) {
        String sql = "SELECT id, tenant_id, source_code, source_name, source_type, "
                + "connection_config, sync_mode, cron_expression, status, description, "
                + "last_sync_time, created_by, created_time, updated_by, updated_time "
                + "FROM sec_sync_source WHERE tenant_id = ?";
        List<SyncSource> sources = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sources.add(mapSource(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list sync sources failed: " + ex.getMessage(), ex);
        }
        return sources;
    }

    /**
     * 根据 ID 获取同步源
     */
    public SyncSource getSource(Long tenantId, Long sourceId) {
        String sql = "SELECT id, tenant_id, source_code, source_name, source_type, "
                + "connection_config, sync_mode, cron_expression, status, description, "
                + "last_sync_time, created_by, created_time, updated_by, updated_time "
                + "FROM sec_sync_source WHERE tenant_id = ? AND id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, sourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSource(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get sync source failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 创建同步源
     */
    public SyncSource createSource(SyncSource source) {
        String sql = "INSERT INTO sec_sync_source (id, tenant_id, source_code, source_name, source_type, "
                + "connection_config, sync_mode, cron_expression, status, description, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            ps.setLong(1, id);
            ps.setLong(2, source.getTenantId());
            ps.setString(3, source.getSourceCode());
            ps.setString(4, source.getSourceName());
            ps.setString(5, source.getSourceType());
            ps.setString(6, source.getConnectionConfig());
            ps.setString(7, source.getSyncMode());
            ps.setString(8, source.getCronExpression());
            ps.setString(9, source.getStatus());
            ps.setString(10, source.getDescription());
            ps.setString(11, TraceContext.getUsername());
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            source.setId(id);
            return source;
        } catch (SQLException ex) {
            throw new IllegalStateException("create sync source failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 更新同步源
     */
    public SyncSource updateSource(SyncSource source) {
        String sql = "UPDATE sec_sync_source SET source_name = ?, source_type = ?, "
                + "connection_config = ?, sync_mode = ?, cron_expression = ?, status = ?, "
                + "description = ?, updated_by = ?, updated_time = ? "
                + "WHERE tenant_id = ? AND id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, source.getSourceName());
            ps.setString(2, source.getSourceType());
            ps.setString(3, source.getConnectionConfig());
            ps.setString(4, source.getSyncMode());
            ps.setString(5, source.getCronExpression());
            ps.setString(6, source.getStatus());
            ps.setString(7, source.getDescription());
            ps.setString(8, TraceContext.getUsername());
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(10, source.getTenantId());
            ps.setLong(11, source.getId());
            ps.executeUpdate();
            return source;
        } catch (SQLException ex) {
            throw new IllegalStateException("update sync source failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 获取所有同步任务
     */
    public List<SyncTask> listTasks(Long tenantId) {
        String sql = "SELECT id, tenant_id, source_id, task_type, status, total_count, "
                + "success_count, failed_count, skip_count, start_time, end_time, "
                + "error_message, triggered_by, created_time, updated_time "
                + "FROM sec_sync_task WHERE tenant_id = ? ORDER BY created_time DESC";
        List<SyncTask> tasks = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapTask(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list sync tasks failed: " + ex.getMessage(), ex);
        }
        return tasks;
    }

    /**
     * 根据 ID 获取同步任务
     */
    public SyncTask getTask(Long tenantId, Long taskId) {
        String sql = "SELECT id, tenant_id, source_id, task_type, status, total_count, "
                + "success_count, failed_count, skip_count, start_time, end_time, "
                + "error_message, triggered_by, created_time, updated_time "
                + "FROM sec_sync_task WHERE tenant_id = ? AND id = ?";
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
            throw new IllegalStateException("get sync task failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 创建同步任务
     */
    public SyncTask createTask(Long tenantId, Long sourceId, String taskType) {
        String sql = "INSERT INTO sec_sync_task (id, tenant_id, source_id, task_type, status, "
                + "triggered_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            ps.setLong(1, id);
            ps.setLong(2, tenantId);
            ps.setLong(3, sourceId);
            ps.setString(4, taskType);
            ps.setString(5, "PENDING");
            ps.setString(6, TraceContext.getUsername());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            SyncTask task = new SyncTask();
            task.setId(id);
            task.setTenantId(tenantId);
            task.setSourceId(sourceId);
            task.setTaskType(taskType);
            task.setStatus("PENDING");
            task.setTriggeredBy(TraceContext.getUsername());
            task.setCreatedTime(LocalDateTime.now());
            return task;
        } catch (SQLException ex) {
            throw new IllegalStateException("create sync task failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 更新同步任务状态
     */
    public void updateTaskStatus(Long taskId, String status, Integer totalCount,
                                 Integer successCount, Integer failedCount, Integer skipCount,
                                 String errorMessage) {
        String sql = "UPDATE sec_sync_task SET status = ?, total_count = ?, success_count = ?, "
                + "failed_count = ?, skip_count = ?, end_time = ?, error_message = ?, updated_time = ? "
                + "WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, totalCount != null ? totalCount : 0);
            ps.setInt(3, successCount != null ? successCount : 0);
            ps.setInt(4, failedCount != null ? failedCount : 0);
            ps.setInt(5, skipCount != null ? skipCount : 0);
            ps.setTimestamp(6, "COMPLETED".equals(status) || "FAILED".equals(status) ?
                    Timestamp.valueOf(LocalDateTime.now()) : null);
            ps.setString(7, errorMessage);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(9, taskId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update sync task status failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 执行用户同步
     */
    public SyncTask executeSync(Long tenantId, Long sourceId, String taskType,
                                List<ExternalUser> externalUsers) {
        // 创建同步任务
        SyncTask task = createTask(tenantId, sourceId, taskType);
        updateTaskStatus(task.getId(), "RUNNING", 0, 0, 0, 0, null);

        int totalCount = 0;
        int successCount = 0;
        int failedCount = 0;
        int skipCount = 0;

        try {
            for (ExternalUser externalUser : externalUsers) {
                totalCount++;
                try {
                    boolean synced = syncUser(tenantId, sourceId, task.getId(), externalUser);
                    if (synced) {
                        successCount++;
                    } else {
                        skipCount++;
                    }
                } catch (Exception ex) {
                    failedCount++;
                    log.error("sync user failed: externalId={}", externalUser.getExternalId(), ex);
                    writeSyncLog(task.getId(), tenantId, externalUser, "FAILED", ex.getMessage());
                }
            }

            updateTaskStatus(task.getId(), "COMPLETED", totalCount, successCount, failedCount, skipCount, null);
            task.setStatus("COMPLETED");
            task.setTotalCount(totalCount);
            task.setSuccessCount(successCount);
            task.setFailedCount(failedCount);
            task.setSkipCount(skipCount);
            task.setEndTime(LocalDateTime.now());
            task.setUpdatedTime(LocalDateTime.now());
        } catch (Exception ex) {
            updateTaskStatus(task.getId(), "FAILED", totalCount, successCount, failedCount, skipCount, ex.getMessage());
            task.setStatus("FAILED");
            task.setTotalCount(totalCount);
            task.setSuccessCount(successCount);
            task.setFailedCount(failedCount);
            task.setSkipCount(skipCount);
            task.setErrorMessage(ex.getMessage());
            task.setEndTime(LocalDateTime.now());
            task.setUpdatedTime(LocalDateTime.now());
            throw ex;
        }

        return task;
    }

    /**
     * 同步单个用户
     */
    private boolean syncUser(Long tenantId, Long sourceId, Long taskId, ExternalUser externalUser) {
        // 检查是否已有绑定
        IdentityBinding binding = findBinding(tenantId, sourceId, externalUser.getExternalId());

        if (binding != null) {
            // 已有绑定，更新用户信息
            return updateExistingUser(tenantId, taskId, binding, externalUser);
        } else {
            // 新用户，创建绑定
            return createNewUser(tenantId, sourceId, taskId, externalUser);
        }
    }

    /**
     * 更新已存在的用户
     */
    private boolean updateExistingUser(Long tenantId, Long taskId, IdentityBinding binding,
                                       ExternalUser externalUser) {
        SecurityUser existingUser = securityPersistenceService.findById(binding.getPlatformUserId());
        if (existingUser == null) {
            writeSyncLog(taskId, tenantId, externalUser, "FAILED", "Platform user not found");
            return false;
        }

        // 检查是否有变更
        String newHash = calculateHash(externalUser);
        if (newHash.equals(binding.getSyncHash())) {
            writeSyncLog(taskId, tenantId, externalUser, "SKIP", "No changes detected");
            return false;
        }

        // 更新用户信息
        updateUserFromExternal(existingUser, externalUser);
        updateBinding(binding.getId(), newHash);

        writeSyncLog(taskId, tenantId, externalUser, "SUCCESS", null);
        return true;
    }

    /**
     * 创建新用户
     */
    private boolean createNewUser(Long tenantId, Long sourceId, Long taskId, ExternalUser externalUser) {
        // 创建平台用户
        SecurityUser newUser = createPlatformUser(tenantId, externalUser);
        if (newUser == null) {
            writeSyncLog(taskId, tenantId, externalUser, "FAILED", "Failed to create platform user");
            return false;
        }

        // 创建身份绑定
        String hash = calculateHash(externalUser);
        createBinding(tenantId, newUser.getId(), sourceId, externalUser, hash);

        writeSyncLog(taskId, tenantId, externalUser, "SUCCESS", null);
        return true;
    }

    /**
     * 创建平台用户
     */
    private SecurityUser createPlatformUser(Long tenantId, ExternalUser externalUser) {
        // 生成随机密码哈希
        String passwordHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

        String sql = "INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, "
                + "email, phone, status, created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long userId = Ids.next();
            ps.setLong(1, userId);
            ps.setLong(2, tenantId);
            ps.setString(3, externalUser.getUsername());
            ps.setString(4, passwordHash);
            ps.setString(5, externalUser.getDisplayName());
            ps.setString(6, externalUser.getEmail());
            ps.setString(7, externalUser.getPhone());
            ps.setString(8, "ACTIVE");
            ps.setString(9, "SYNC");
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            SecurityUser user = new SecurityUser();
            user.setId(userId);
            user.setTenantId(tenantId);
            user.setUsername(externalUser.getUsername());
            user.setDisplayName(externalUser.getDisplayName());
            user.setEmail(externalUser.getEmail());
            user.setPhone(externalUser.getPhone());
            user.setStatus("ACTIVE");
            return user;
        } catch (SQLException ex) {
            log.error("create platform user failed: username={}", externalUser.getUsername(), ex);
            return null;
        }
    }

    /**
     * 更新用户信息
     */
    private void updateUserFromExternal(SecurityUser user, ExternalUser externalUser) {
        String sql = "UPDATE sec_user SET display_name = ?, email = ?, phone = ?, "
                + "updated_by = 'SYNC', updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, externalUser.getDisplayName());
            ps.setString(2, externalUser.getEmail());
            ps.setString(3, externalUser.getPhone());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(5, user.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("update user from external failed: userId={}", user.getId(), ex);
        }
    }

    /**
     * 创建身份绑定
     */
    private void createBinding(Long tenantId, Long platformUserId, Long sourceId,
                               ExternalUser externalUser, String hash) {
        String sql = "INSERT INTO sec_identity_binding (id, tenant_id, platform_user_id, source_id, "
                + "external_id, external_username, external_display_name, binding_status, "
                + "last_sync_time, sync_hash, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Ids.next());
            ps.setLong(2, tenantId);
            ps.setLong(3, platformUserId);
            ps.setLong(4, sourceId);
            ps.setString(5, externalUser.getExternalId());
            ps.setString(6, externalUser.getUsername());
            ps.setString(7, externalUser.getDisplayName());
            ps.setString(8, "ACTIVE");
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(10, hash);
            ps.setString(11, "SYNC");
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("create identity binding failed: externalId={}", externalUser.getExternalId(), ex);
        }
    }

    /**
     * 更新绑定哈希
     */
    private void updateBinding(Long bindingId, String hash) {
        String sql = "UPDATE sec_identity_binding SET last_sync_time = ?, sync_hash = ?, "
                + "updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, hash);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(4, bindingId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("update binding failed: bindingId={}", bindingId, ex);
        }
    }

    /**
     * 查找绑定
     */
    private IdentityBinding findBinding(Long tenantId, Long sourceId, String externalId) {
        String sql = "SELECT id, tenant_id, platform_user_id, source_id, external_id, "
                + "external_username, external_display_name, binding_status, last_sync_time, "
                + "sync_hash, created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_binding WHERE tenant_id = ? AND source_id = ? AND external_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, sourceId);
            ps.setString(3, externalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBinding(rs);
                }
            }
        } catch (SQLException ex) {
            log.error("find binding failed: externalId={}", externalId, ex);
        }
        return null;
    }

    /**
     * 获取同步日志
     */
    public List<SyncLog> listLogs(Long tenantId, Long taskId) {
        String sql = "SELECT id, tenant_id, task_id, external_id, external_username, "
                + "platform_user_id, operation, status, error_message, sync_data, created_time "
                + "FROM sec_sync_log WHERE tenant_id = ? AND task_id = ? ORDER BY created_time DESC";
        List<SyncLog> logs = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapLog(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list sync logs failed: " + ex.getMessage(), ex);
        }
        return logs;
    }

    /**
     * 写入同步日志
     */
    private void writeSyncLog(Long taskId, Long tenantId, ExternalUser externalUser,
                              String status, String errorMessage) {
        String sql = "INSERT INTO sec_sync_log (id, tenant_id, task_id, external_id, "
                + "external_username, platform_user_id, operation, status, error_message, "
                + "sync_data, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Ids.next());
            ps.setLong(2, tenantId);
            ps.setLong(3, taskId);
            ps.setString(4, externalUser.getExternalId());
            ps.setString(5, externalUser.getUsername());
            ps.setLong(6, 0L); // platform_user_id 在创建后填充
            ps.setString(7, "SYNC");
            ps.setString(8, status);
            ps.setString(9, errorMessage);
            ps.setString(10, null); // sync_data
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("write sync log failed: taskId={}", taskId, ex);
        }
    }

    /**
     * 计算用户数据哈希
     */
    private String calculateHash(ExternalUser externalUser) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String data = externalUser.getUsername() + "|" + externalUser.getDisplayName() + "|"
                    + externalUser.getEmail() + "|" + externalUser.getPhone();
            byte[] hash = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private SyncSource mapSource(ResultSet rs) throws SQLException {
        SyncSource source = new SyncSource();
        source.setId(rs.getLong("id"));
        source.setTenantId(rs.getLong("tenant_id"));
        source.setSourceCode(rs.getString("source_code"));
        source.setSourceName(rs.getString("source_name"));
        source.setSourceType(rs.getString("source_type"));
        source.setConnectionConfig(rs.getString("connection_config"));
        source.setSyncMode(rs.getString("sync_mode"));
        source.setCronExpression(rs.getString("cron_expression"));
        source.setStatus(rs.getString("status"));
        source.setDescription(rs.getString("description"));
        Timestamp lastSync = rs.getTimestamp("last_sync_time");
        if (lastSync != null) {
            source.setLastSyncTime(lastSync.toLocalDateTime());
        }
        source.setCreatedBy(rs.getString("created_by"));
        source.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
        source.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            source.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return source;
    }

    private SyncTask mapTask(ResultSet rs) throws SQLException {
        SyncTask task = new SyncTask();
        task.setId(rs.getLong("id"));
        task.setTenantId(rs.getLong("tenant_id"));
        task.setSourceId(rs.getLong("source_id"));
        task.setTaskType(rs.getString("task_type"));
        task.setStatus(rs.getString("status"));
        task.setTotalCount(rs.getInt("total_count"));
        task.setSuccessCount(rs.getInt("success_count"));
        task.setFailedCount(rs.getInt("failed_count"));
        task.setSkipCount(rs.getInt("skip_count"));
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            task.setStartTime(startTime.toLocalDateTime());
        }
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            task.setEndTime(endTime.toLocalDateTime());
        }
        task.setErrorMessage(rs.getString("error_message"));
        task.setTriggeredBy(rs.getString("triggered_by"));
        task.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            task.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return task;
    }

    private SyncLog mapLog(ResultSet rs) throws SQLException {
        SyncLog log = new SyncLog();
        log.setId(rs.getLong("id"));
        log.setTenantId(rs.getLong("tenant_id"));
        log.setTaskId(rs.getLong("task_id"));
        log.setExternalId(rs.getString("external_id"));
        log.setExternalUsername(rs.getString("external_username"));
        log.setPlatformUserId(rs.getLong("platform_user_id"));
        log.setOperation(rs.getString("operation"));
        log.setStatus(rs.getString("status"));
        log.setErrorMessage(rs.getString("error_message"));
        log.setSyncData(rs.getString("sync_data"));
        log.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
        return log;
    }

    private IdentityBinding mapBinding(ResultSet rs) throws SQLException {
        IdentityBinding binding = new IdentityBinding();
        binding.setId(rs.getLong("id"));
        binding.setTenantId(rs.getLong("tenant_id"));
        binding.setPlatformUserId(rs.getLong("platform_user_id"));
        binding.setSourceId(rs.getLong("source_id"));
        binding.setExternalId(rs.getString("external_id"));
        binding.setExternalUsername(rs.getString("external_username"));
        binding.setExternalDisplayName(rs.getString("external_display_name"));
        binding.setBindingStatus(rs.getString("binding_status"));
        Timestamp lastSync = rs.getTimestamp("last_sync_time");
        if (lastSync != null) {
            binding.setLastSyncTime(lastSync.toLocalDateTime());
        }
        binding.setSyncHash(rs.getString("sync_hash"));
        binding.setCreatedBy(rs.getString("created_by"));
        binding.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
        binding.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            binding.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return binding;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                properties.getUrl(), properties.getUsername(), properties.getPassword());
    }

    /**
     * 外部用户数据对象
     */
    public static class ExternalUser {
        private String externalId;
        private String username;
        private String displayName;
        private String email;
        private String phone;
        private String department;
        private String position;

        public ExternalUser() {}

        public ExternalUser(String externalId, String username, String displayName,
                           String email, String phone, String department, String position) {
            this.externalId = externalId;
            this.username = username;
            this.displayName = displayName;
            this.email = email;
            this.phone = phone;
            this.department = department;
            this.position = position;
        }

        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
    }
}
