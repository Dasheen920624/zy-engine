package com.medkernel.security;

import com.medkernel.adapter.AdapterHubService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户同步服务：从院内身份源（HIS/EMR/OA/统一身份平台）同步用户到平台。
 * 支持全量同步、增量同步和手动同步三种模式。
 */
@Service
public class UserSyncService {

    private static final Logger log = LoggerFactory.getLogger(UserSyncService.class);

    private final SecurityPersistenceService persistenceService;
    private final AdapterHubService adapterHubService;
    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public UserSyncService(SecurityPersistenceService persistenceService,
                           AdapterHubService adapterHubService,
                           EnginePersistenceProperties properties,
                           DataSource dataSource) {
        this.persistenceService = persistenceService;
        this.adapterHubService = adapterHubService;
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 全量同步：从指定身份源读取所有用户，创建/更新/停用平台用户。
     */
    public SyncReport syncAll(Long tenantId, Long providerId, String operator) {
        IdentityProvider provider = findProvider(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("身份源不存在: " + providerId);
        }
        long startTime = System.currentTimeMillis();
        SyncReport report = new SyncReport();
        report.setTenantId(tenantId);
        report.setProviderId(providerId);
        report.setSyncType("FULL");

        try {
            List<Map<String, Object>> externalUsers = queryExternalUsers(provider);
            report.setTotalCount(externalUsers.size());

            for (Map<String, Object> extUser : externalUsers) {
                try {
                    processExternalUser(tenantId, providerId, extUser, operator, report);
                } catch (Exception ex) {
                    report.setErrorCount(report.getErrorCount() + 1);
                    log.error("同步用户失败: {}", extUser.get("employeeId"), ex);
                }
            }

            report.setSyncStatus("SUCCESS");
        } catch (Exception ex) {
            report.setSyncStatus("FAILED");
            log.error("全量同步失败: providerId={}", providerId, ex);
        }

        long duration = System.currentTimeMillis() - startTime;
        report.setDurationMs(duration);

        updateProviderSyncStatus(providerId, report);
        saveSyncLog(tenantId, providerId, "FULL", report, operator, duration);

        return report;
    }

    /**
     * 增量同步：仅同步上次同步后变更的用户。
     */
    public SyncReport syncIncremental(Long tenantId, Long providerId, String operator) {
        IdentityProvider provider = findProvider(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("身份源不存在: " + providerId);
        }
        long startTime = System.currentTimeMillis();
        SyncReport report = new SyncReport();
        report.setTenantId(tenantId);
        report.setProviderId(providerId);
        report.setSyncType("INCREMENTAL");

        try {
            List<Map<String, Object>> externalUsers = queryExternalUsersIncremental(provider);
            report.setTotalCount(externalUsers.size());

            for (Map<String, Object> extUser : externalUsers) {
                try {
                    processExternalUser(tenantId, providerId, extUser, operator, report);
                } catch (Exception ex) {
                    report.setErrorCount(report.getErrorCount() + 1);
                    log.error("增量同步用户失败: {}", extUser.get("employeeId"), ex);
                }
            }

            report.setSyncStatus("SUCCESS");
        } catch (Exception ex) {
            report.setSyncStatus("FAILED");
            log.error("增量同步失败: providerId={}", providerId, ex);
        }

        long duration = System.currentTimeMillis() - startTime;
        report.setDurationMs(duration);
        updateProviderSyncStatus(providerId, report);
        saveSyncLog(tenantId, providerId, "INCREMENTAL", report, operator, duration);

        return report;
    }

    /**
     * 手动同步：管理员手动触发，等同全量同步但审计标记为 MANUAL。
     */
    public SyncReport syncManual(Long tenantId, Long providerId, String operator) {
        IdentityProvider provider = findProvider(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("身份源不存在: " + providerId);
        }
        long startTime = System.currentTimeMillis();
        SyncReport report = new SyncReport();
        report.setTenantId(tenantId);
        report.setProviderId(providerId);
        report.setSyncType("MANUAL");

        try {
            List<Map<String, Object>> externalUsers = queryExternalUsers(provider);
            report.setTotalCount(externalUsers.size());

            for (Map<String, Object> extUser : externalUsers) {
                try {
                    processExternalUser(tenantId, providerId, extUser, operator, report);
                } catch (Exception ex) {
                    report.setErrorCount(report.getErrorCount() + 1);
                    log.error("手动同步用户失败: {}", extUser.get("employeeId"), ex);
                }
            }

            report.setSyncStatus("SUCCESS");
        } catch (Exception ex) {
            report.setSyncStatus("FAILED");
            log.error("手动同步失败: providerId={}", providerId, ex);
        }

        long duration = System.currentTimeMillis() - startTime;
        report.setDurationMs(duration);
        updateProviderSyncStatus(providerId, report);
        saveSyncLog(tenantId, providerId, "MANUAL", report, operator, duration);

        return report;
    }

    /**
     * 查询所有身份源配置。
     */
    public List<IdentityProvider> listProviders(Long tenantId) {
        return persistenceService.findAllIdentityProviders(tenantId);
    }

    /**
     * 保存身份源配置。
     */
    public IdentityProvider saveProvider(IdentityProvider provider) {
        if (provider.getId() == null) {
            provider.setId(Ids.next());
            provider.setCreatedTime(LocalDateTime.now());
            String sql = "INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, "
                    + "provider_type, adapter_code, sync_mode, sync_cron, priority, status, "
                    + "created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, provider.getId());
                ps.setLong(2, provider.getTenantId());
                ps.setString(3, provider.getProviderCode());
                ps.setString(4, provider.getProviderName());
                ps.setString(5, provider.getProviderType());
                ps.setString(6, provider.getAdapterCode());
                ps.setString(7, provider.getSyncMode());
                ps.setString(8, provider.getSyncCron());
                ps.setInt(9, provider.getPriority());
                ps.setString(10, provider.getStatus());
                ps.setString(11, provider.getCreatedBy());
                ps.setTimestamp(12, Timestamp.valueOf(provider.getCreatedTime()));
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("创建身份源失败: " + ex.getMessage(), ex);
            }
        } else {
            String sql = "UPDATE sec_identity_provider SET provider_name=?, provider_type=?, "
                    + "adapter_code=?, sync_mode=?, sync_cron=?, priority=?, status=?, "
                    + "updated_by=?, updated_time=? WHERE id=?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, provider.getProviderName());
                ps.setString(2, provider.getProviderType());
                ps.setString(3, provider.getAdapterCode());
                ps.setString(4, provider.getSyncMode());
                ps.setString(5, provider.getSyncCron());
                ps.setInt(6, provider.getPriority());
                ps.setString(7, provider.getStatus());
                ps.setString(8, provider.getUpdatedBy());
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(10, provider.getId());
                ps.executeUpdate();
            } catch (SQLException ex) {
                throw new IllegalStateException("更新身份源失败: " + ex.getMessage(), ex);
            }
        }
        return provider;
    }

    /**
     * 查询同步日志。
     */
    public List<Map<String, Object>> listSyncLogs(Long tenantId, Long providerId, int limit) {
        String sql = "SELECT id, tenant_id, provider_id, sync_type, sync_status, "
                + "total_count, created_count, updated_count, disabled_count, "
                + "conflict_count, error_count, started_time, finished_time, duration_ms "
                + "FROM sec_user_sync_log WHERE tenant_id = ? "
                + (providerId != null ? "AND provider_id = ? " : "")
                + "ORDER BY started_time DESC";
        List<Map<String, Object>> logs = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            if (providerId != null) {
                ps.setLong(2, providerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("syncType", rs.getString("sync_type"));
                    row.put("syncStatus", rs.getString("sync_status"));
                    row.put("totalCount", rs.getInt("total_count"));
                    row.put("createdCount", rs.getInt("created_count"));
                    row.put("updatedCount", rs.getInt("updated_count"));
                    row.put("disabledCount", rs.getInt("disabled_count"));
                    row.put("conflictCount", rs.getInt("conflict_count"));
                    row.put("errorCount", rs.getInt("error_count"));
                    row.put("durationMs", rs.getInt("duration_ms"));
                    Timestamp started = rs.getTimestamp("started_time");
                    if (started != null) row.put("startedTime", started.toLocalDateTime());
                    Timestamp finished = rs.getTimestamp("finished_time");
                    if (finished != null) row.put("finishedTime", finished.toLocalDateTime());
                    logs.add(row);
                    count++;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询同步日志失败: " + ex.getMessage(), ex);
        }
        return logs;
    }

    // ---- 内部方法 ----

    /**
     * 处理单个外部用户：创建/更新/停用平台用户。
     */
    private void processExternalUser(Long tenantId, Long providerId,
                                     Map<String, Object> extUser, String operator,
                                     SyncReport report) {
        String employeeId = String.valueOf(extUser.getOrDefault("employeeId", ""));
        String displayName = String.valueOf(extUser.getOrDefault("displayName", ""));
        String deptCode = String.valueOf(extUser.getOrDefault("deptCode", ""));
        String deptName = String.valueOf(extUser.getOrDefault("deptName", ""));
        String status = String.valueOf(extUser.getOrDefault("status", "ACTIVE"));

        // 查找已有身份绑定
        IdentityBinding binding = persistenceService.findIdentityBinding(tenantId, providerId, employeeId);

        if (binding != null) {
            // 已有绑定 → 更新用户
            SecurityUser existingUser = persistenceService.findById(binding.getUserId());
            if (existingUser == null) {
                // 绑定存在但用户不存在 → 数据异常，记录冲突
                report.setConflictCount(report.getConflictCount() + 1);
                return;
            }

            if ("RESIGNED".equalsIgnoreCase(status) || "DISABLED".equalsIgnoreCase(status)) {
                // 离职/停用 → 禁用登录，保留审计主体
                disableUser(existingUser.getId(), operator);
                report.setDisabledCount(report.getDisabledCount() + 1);
            } else {
                // 在职 → 更新显示名和组织范围
                updateUser(existingUser.getId(), displayName, deptCode, deptName, operator);
                report.setUpdatedCount(report.getUpdatedCount() + 1);
            }
        } else {
            // 无绑定 → 检查是否已有同 employeeId 的用户
            SecurityUser existingByEmpId = persistenceService.findUserByEmployeeId(tenantId, employeeId);
            if (existingByEmpId != null) {
                // 已有用户 → 创建身份绑定
                createBinding(tenantId, existingByEmpId.getId(), providerId, employeeId, deptCode, displayName, operator);
                if ("RESIGNED".equalsIgnoreCase(status) || "DISABLED".equalsIgnoreCase(status)) {
                    disableUser(existingByEmpId.getId(), operator);
                    report.setDisabledCount(report.getDisabledCount() + 1);
                } else {
                    updateUser(existingByEmpId.getId(), displayName, deptCode, deptName, operator);
                    report.setUpdatedCount(report.getUpdatedCount() + 1);
                }
            } else {
                // 新用户 → 创建平台用户 + 身份绑定
                if ("RESIGNED".equalsIgnoreCase(status) || "DISABLED".equalsIgnoreCase(status)) {
                    // 离职用户不同步创建
                    return;
                }
                SecurityUser newUser = createSyncedUser(tenantId, employeeId, displayName, deptCode, deptName, operator);
                createBinding(tenantId, newUser.getId(), providerId, employeeId, deptCode, displayName, operator);
                report.setCreatedCount(report.getCreatedCount() + 1);
            }
        }
    }

    /**
     * 通过适配器查询外部用户列表（全量）。
     */
    private List<Map<String, Object>> queryExternalUsers(IdentityProvider provider) {
        Map<String, Object> request = new HashMap<>();
        String adapterCode = provider.getAdapterCode() != null
                ? provider.getAdapterCode()
                : provider.getProviderType() + "_ADAPTER";
        request.put("adapter_code", adapterCode);
        request.put("query_code", "QUERY_USERS");
        Map<String, Object> params = new HashMap<>();
        params.put("tenant_id", String.valueOf(provider.getTenantId()));
        request.put("params", params);
        Map<String, Object> result = adapterHubService.query(request,
                String.valueOf(provider.getTenantId()), "DEFAULT_HOSPITAL");
        if (result == null) {
            return new ArrayList<>();
        }
        Object rows = result.get("rows");
        if (rows instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> userList = (List<Map<String, Object>>) rows;
            return userList;
        }
        return new ArrayList<>();
    }

    /**
     * 通过适配器查询外部用户列表（增量）。
     */
    private List<Map<String, Object>> queryExternalUsersIncremental(IdentityProvider provider) {
        Map<String, Object> request = new HashMap<>();
        String adapterCode = provider.getAdapterCode() != null
                ? provider.getAdapterCode()
                : provider.getProviderType() + "_ADAPTER";
        request.put("adapter_code", adapterCode);
        request.put("query_code", "QUERY_USERS_INCREMENTAL");
        Map<String, Object> params = new HashMap<>();
        params.put("tenant_id", String.valueOf(provider.getTenantId()));
        if (provider.getLastSyncTime() != null) {
            params.put("since", provider.getLastSyncTime().toString());
        }
        request.put("params", params);
        Map<String, Object> result = adapterHubService.query(request,
                String.valueOf(provider.getTenantId()), "DEFAULT_HOSPITAL");
        if (result == null) {
            return new ArrayList<>();
        }
        Object rows = result.get("rows");
        if (rows instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> userList = (List<Map<String, Object>>) rows;
            return userList;
        }
        return new ArrayList<>();
    }

    private IdentityProvider findProvider(Long providerId) {
        String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, adapter_code, "
                + "sync_mode, sync_cron, priority, status, "
                + "last_sync_time, last_sync_result, last_sync_summary, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_provider WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProvider(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询身份源失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    private SecurityUser createSyncedUser(Long tenantId, String employeeId, String displayName,
                                           String deptCode, String deptName, String operator) {
        long userId = Ids.next();
        String username = "sync_" + employeeId;
        // 默认密码 hash (password: changeme)
        String defaultPasswordHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String sql = "INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, "
                + "status, user_type, employee_id, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, 'ACTIVE', 'HOSPITAL', ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, tenantId);
            ps.setString(3, username);
            ps.setString(4, defaultPasswordHash);
            ps.setString(5, displayName);
            ps.setString(6, employeeId);
            ps.setString(7, operator);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建同步用户失败: " + ex.getMessage(), ex);
        }

        // 创建组织范围
        if (deptCode != null && !deptCode.isEmpty()) {
            long scopeId = Ids.next();
            String scopeSql = "INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by, created_time) "
                    + "VALUES (?, ?, ?, 'DEPARTMENT', ?, ?, ?, ?)";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(scopeSql)) {
                ps.setLong(1, scopeId);
                ps.setLong(2, tenantId);
                ps.setLong(3, userId);
                ps.setString(4, deptCode);
                ps.setString(5, deptName);
                ps.setString(6, operator);
                ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.error("创建用户组织范围失败: userId={}", userId, ex);
            }
        }

        SecurityUser user = new SecurityUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setStatus("ACTIVE");
        user.setUserType("HOSPITAL");
        user.setEmployeeId(employeeId);
        return user;
    }

    private void createBinding(Long tenantId, Long userId, Long providerId,
                                String externalSubject, String externalOrgCode,
                                String displayName, String operator) {
        long bindingId = Ids.next();
        String sql = "INSERT INTO sec_identity_binding (id, tenant_id, user_id, provider_id, "
                + "external_subject, external_org_code, external_display_name, binding_status, last_verified_time, "
                + "created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bindingId);
            ps.setLong(2, tenantId);
            ps.setLong(3, userId);
            ps.setLong(4, providerId);
            ps.setString(5, externalSubject);
            ps.setString(6, externalOrgCode);
            ps.setString(7, displayName);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(9, operator);
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建身份绑定失败: " + ex.getMessage(), ex);
        }
    }

    private void updateUser(Long userId, String displayName, String deptCode, String deptName, String operator) {
        String sql = "UPDATE sec_user SET display_name = ?, updated_by = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setString(2, operator);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(4, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("更新用户失败: userId={}", userId, ex);
        }

        // 更新组织范围（简化：删除旧的部门范围，插入新的）
        if (deptCode != null && !deptCode.isEmpty()) {
            String deleteSql = "DELETE FROM sec_user_org_scope WHERE user_id = ? AND scope_level = 'DEPARTMENT'";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(deleteSql)) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.error("删除旧组织范围失败: userId={}", userId, ex);
            }

            long scopeId = Ids.next();
            String insertSql = "INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by, created_time) "
                    + "SELECT ?, tenant_id, ?, 'DEPARTMENT', ?, ?, ?, ? FROM sec_user WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setLong(1, scopeId);
                ps.setLong(2, userId);
                ps.setString(3, deptCode);
                ps.setString(4, deptName);
                ps.setString(5, operator);
                ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(7, userId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.error("插入新组织范围失败: userId={}", userId, ex);
            }
        }
    }

    private void disableUser(Long userId, String operator) {
        String sql = "UPDATE sec_user SET status = 'DISABLED', updated_by = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, operator);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("停用用户失败: userId={}", userId, ex);
        }
    }

    private void updateProviderSyncStatus(Long providerId, SyncReport report) {
        String summary = String.format("总数:%d 新建:%d 更新:%d 停用:%d 冲突:%d 错误:%d",
                report.getTotalCount(), report.getCreatedCount(), report.getUpdatedCount(),
                report.getDisabledCount(), report.getConflictCount(), report.getErrorCount());
        String sql = "UPDATE sec_identity_provider SET last_sync_time = ?, last_sync_result = ?, "
                + "last_sync_summary = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, report.getSyncStatus());
            ps.setString(3, summary);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(5, providerId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("更新身份源同步状态失败: providerId={}", providerId, ex);
        }
    }

    private void saveSyncLog(Long tenantId, Long providerId, String syncType,
                              SyncReport report, String operator, long durationMs) {
        long logId = Ids.next();
        String sql = "INSERT INTO sec_user_sync_log (id, tenant_id, provider_id, sync_type, sync_status, "
                + "total_count, created_count, updated_count, disabled_count, conflict_count, error_count, "
                + "started_time, finished_time, duration_ms, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, logId);
            ps.setLong(2, tenantId);
            ps.setLong(3, providerId);
            ps.setString(4, syncType);
            ps.setString(5, report.getSyncStatus());
            ps.setInt(6, report.getTotalCount());
            ps.setInt(7, report.getCreatedCount());
            ps.setInt(8, report.getUpdatedCount());
            ps.setInt(9, report.getDisabledCount());
            ps.setInt(10, report.getConflictCount());
            ps.setInt(11, report.getErrorCount());
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(14, (int) durationMs);
            ps.setString(15, operator);
            ps.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("保存同步日志失败", ex);
        }
    }

    private IdentityProvider mapProvider(ResultSet rs) throws SQLException {
        IdentityProvider provider = new IdentityProvider();
        provider.setId(rs.getLong("id"));
        provider.setTenantId(rs.getLong("tenant_id"));
        provider.setProviderCode(rs.getString("provider_code"));
        provider.setProviderName(rs.getString("provider_name"));
        provider.setProviderType(rs.getString("provider_type"));
        provider.setAdapterCode(rs.getString("adapter_code"));
        provider.setSyncMode(rs.getString("sync_mode"));
        provider.setSyncCron(rs.getString("sync_cron"));
        provider.setPriority(rs.getInt("priority"));
        provider.setStatus(rs.getString("status"));
        Timestamp lastSync = rs.getTimestamp("last_sync_time");
        if (lastSync != null) provider.setLastSyncTime(lastSync.toLocalDateTime());
        provider.setLastSyncResult(rs.getString("last_sync_result"));
        provider.setLastSyncSummary(rs.getString("last_sync_summary"));
        provider.setCreatedBy(rs.getString("created_by"));
        Timestamp created = rs.getTimestamp("created_time");
        if (created != null) provider.setCreatedTime(created.toLocalDateTime());
        provider.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updated = rs.getTimestamp("updated_time");
        if (updated != null) provider.setUpdatedTime(updated.toLocalDateTime());
        return provider;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15b: 璧?HikariCP 杩炴帴姹狅紙EngineDataSourceConfig 鏆撮湶鐨?DataSource锛夈€?        return dataSource.getConnection();
    }
}
