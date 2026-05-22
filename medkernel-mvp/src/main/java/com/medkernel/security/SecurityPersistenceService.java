package com.medkernel.security;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 安全模块持久化服务：使用 raw JDBC 操作 SEC 表。
 * 复用 EnginePersistenceProperties 获取数据库连接配置。
 */
@Service
public class SecurityPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(SecurityPersistenceService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public SecurityPersistenceService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initializeSecuritySchema() {
        if (!properties.isEnabled() || !properties.localFileDatabase()) {
            return;
        }
        List<String> statements = loadSchemaStatements("/db/local/sec_ddl.sql");
        if (statements.isEmpty()) {
            return;
        }
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
            log.info("SEC schema initialized successfully");
        } catch (SQLException ex) {
            log.error("initialize SEC schema failed", ex);
            throw new IllegalStateException("initialize SEC schema failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 根据用户名查找用户（含密码哈希）。
     */
    public SecurityUser findByUsername(String username) {
        String sql = "SELECT u.id, u.tenant_id, u.username, u.password_hash, u.display_name, "
                + "u.email, u.phone, u.avatar_url, u.status, u.last_login_time, u.last_login_ip, "
                + "u.login_attempts, u.locked_until, u.user_type, u.employee_id "
                + "FROM sec_user u WHERE u.username = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SecurityUser user = mapUser(rs);
                    user.setRoles(findUserRoles(user.getId()));
                    user.setPermissions(findUserPermissions(user.getId()));
                    user.setOrgScopes(findUserOrgScopes(user.getId()));
                    return user;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find user by username failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据用户 ID 查找用户。
     */
    public SecurityUser findById(Long userId) {
        String sql = "SELECT u.id, u.tenant_id, u.username, u.password_hash, u.display_name, "
                + "u.email, u.phone, u.avatar_url, u.status, u.last_login_time, u.last_login_ip, "
                + "u.login_attempts, u.locked_until, u.user_type, u.employee_id "
                + "FROM sec_user u WHERE u.id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SecurityUser user = mapUser(rs);
                    user.setRoles(findUserRoles(user.getId()));
                    user.setPermissions(findUserPermissions(user.getId()));
                    user.setOrgScopes(findUserOrgScopes(user.getId()));
                    return user;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find user by id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 更新用户登录状态（成功或失败）。
     */
    public void updateLoginStatus(Long userId, boolean success, String ip) {
        if (success) {
            String sql = "UPDATE sec_user SET last_login_time = ?, last_login_ip = ?, "
                    + "login_attempts = 0, locked_until = NULL WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(2, ip);
                ps.setLong(3, userId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.error("update login status failed for user {}", userId, ex);
            }
        } else {
            String sql = "UPDATE sec_user SET login_attempts = login_attempts + 1 WHERE id = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.error("update login attempts failed for user {}", userId, ex);
            }
        }
    }

    /**
     * 锁定用户账户。
     */
    public void lockUser(Long userId, int durationMinutes) {
        String sql = "UPDATE sec_user SET locked_until = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().plusMinutes(durationMinutes)));
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("lock user failed for user {}", userId, ex);
        }
    }

    /**
     * 写审计日志。
     */
    public void writeAuditLog(Long userId, Long tenantId, String action, String ip, String detail) {
        String eventResult = (action != null && (action.endsWith("FAILED") || action.endsWith("LOCKED")))
                ? "FAILURE" : "SUCCESS";
        String sql = "INSERT INTO sec_auth_audit_log (id, user_id, tenant_id, username, event_type, "
                + "event_result, ip_address, failure_reason, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Ids.next());
            ps.setLong(2, userId);
            ps.setLong(3, tenantId);
            ps.setString(4, null);
            ps.setString(5, action);
            ps.setString(6, eventResult);
            ps.setString(7, ip);
            ps.setString(8, detail);
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("write audit log failed for user {} action {}", userId, action, ex);
        }
    }

    private List<String> findUserRoles(Long userId) {
        String sql = "SELECT r.role_code FROM sec_role r "
                + "INNER JOIN sec_user_role ur ON r.id = ur.role_id "
                + "WHERE ur.user_id = ?";
        List<String> roles = new ArrayList<String>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getString("role_code"));
                }
            }
        } catch (SQLException ex) {
            log.error("find user roles failed for user {}", userId, ex);
        }
        return roles;
    }

    private List<String> findUserPermissions(Long userId) {
        String sql = "SELECT DISTINCT p.permission_code FROM sec_permission p "
                + "INNER JOIN sec_role_permission rp ON p.id = rp.permission_id "
                + "INNER JOIN sec_user_role ur ON rp.role_id = ur.role_id "
                + "WHERE ur.user_id = ?";
        List<String> permissions = new ArrayList<String>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permissions.add(rs.getString("permission_code"));
                }
            }
        } catch (SQLException ex) {
            log.error("find user permissions failed for user {}", userId, ex);
        }
        return permissions;
    }

    private List<SecurityUser.OrgScope> findUserOrgScopes(Long userId) {
        String sql = "SELECT scope_level, scope_code, scope_name FROM sec_user_org_scope WHERE user_id = ?";
        List<SecurityUser.OrgScope> scopes = new ArrayList<SecurityUser.OrgScope>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    scopes.add(new SecurityUser.OrgScope(
                            rs.getString("scope_level"),
                            rs.getString("scope_code"),
                            rs.getString("scope_name")
                    ));
                }
            }
        } catch (SQLException ex) {
            log.error("find user org scopes failed for user {}", userId, ex);
        }
        return scopes;
    }

    private SecurityUser mapUser(ResultSet rs) throws SQLException {
        SecurityUser user = new SecurityUser();
        user.setId(rs.getLong("id"));
        user.setTenantId(rs.getLong("tenant_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setDisplayName(rs.getString("display_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setStatus(rs.getString("status"));
        Timestamp lastLogin = rs.getTimestamp("last_login_time");
        if (lastLogin != null) {
            user.setLastLoginTime(lastLogin.toLocalDateTime());
        }
        user.setLastLoginIp(rs.getString("last_login_ip"));
        user.setLoginAttempts(rs.getInt("login_attempts"));
        Timestamp lockedUntil = rs.getTimestamp("locked_until");
        if (lockedUntil != null) {
            user.setLockedUntil(lockedUntil.toLocalDateTime());
        }
        user.setUserType(rs.getString("user_type"));
        user.setEmployeeId(rs.getString("employee_id"));
        return user;
    }

    /**
     * 根据租户和身份源类型查找身份源配置。
     */
    public IdentityProvider findIdentityProviderByType(Long tenantId, String providerType) {
        String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, adapter_code, "
                + "sync_mode, sync_cron, priority, status, "
                + "last_sync_time, last_sync_result, last_sync_summary, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_provider WHERE tenant_id = ? AND provider_type = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, providerType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapIdentityProvider(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find identity provider failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 查找租户下所有身份源配置。
     */
    public List<IdentityProvider> findAllIdentityProviders(Long tenantId) {
        String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, adapter_code, "
                + "sync_mode, sync_cron, priority, status, "
                + "last_sync_time, last_sync_result, last_sync_summary, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_provider WHERE tenant_id = ? ORDER BY priority";
        List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    providers.add(mapIdentityProvider(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find identity providers failed: " + ex.getMessage(), ex);
        }
        return providers;
    }

    /**
     * 根据外部身份查找绑定关系。
     */
    public IdentityBinding findIdentityBinding(Long tenantId, Long providerId, String externalSubject) {
        String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, external_org_code, "
                + "external_display_name, binding_status, last_verified_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_binding WHERE tenant_id = ? AND provider_id = ? AND external_subject = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, providerId);
            ps.setString(3, externalSubject);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapIdentityBinding(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find identity binding failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 查找用户的所有身份绑定。
     */
    public List<IdentityBinding> findIdentityBindingsByUser(Long tenantId, Long userId) {
        String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, external_org_code, "
                + "external_display_name, binding_status, last_verified_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_binding WHERE tenant_id = ? AND user_id = ?";
        List<IdentityBinding> bindings = new ArrayList<IdentityBinding>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bindings.add(mapIdentityBinding(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find identity bindings by user failed: " + ex.getMessage(), ex);
        }
        return bindings;
    }

    /**
     * 创建平台用户（同步时使用）。
     */
    public Long createUser(Long tenantId, String username, String displayName, String email, String phone,
                           String userType, String employeeId, String status) {
        Long userId = Ids.next();
        String sql = "INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, email, phone, "
                + "status, user_type, employee_id, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, tenantId);
            ps.setString(3, username);
            ps.setString(4, "");
            ps.setString(5, displayName);
            ps.setString(6, email);
            ps.setString(7, phone);
            ps.setString(8, status);
            ps.setString(9, userType);
            ps.setString(10, employeeId);
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create user failed: " + ex.getMessage(), ex);
        }
        return userId;
    }

    /**
     * 更新用户同步信息（显示名、邮箱、电话、状态、工号）。
     */
    public void updateUserSync(Long userId, String displayName, String email, String phone,
                               String status, String employeeId) {
        String sql = "UPDATE sec_user SET display_name = ?, email = ?, phone = ?, status = ?, "
                + "employee_id = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, status);
            ps.setString(5, employeeId);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(7, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update user sync failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 创建身份绑定。
     */
    public void createIdentityBinding(Long tenantId, Long userId, Long providerId,
                                       String externalSubject, String externalOrgCode,
                                       String externalDisplayName) {
        Long bindingId = Ids.next();
        String sql = "INSERT INTO sec_identity_binding (id, tenant_id, user_id, provider_id, external_subject, "
                + "external_org_code, external_display_name, binding_status, "
                + "last_verified_time, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bindingId);
            ps.setLong(2, tenantId);
            ps.setLong(3, userId);
            ps.setLong(4, providerId);
            ps.setString(5, externalSubject);
            ps.setString(6, externalOrgCode);
            ps.setString(7, externalDisplayName);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create identity binding failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 保存同步日志。
     */
    public void saveSyncLog(Long tenantId, Long providerId, String syncType, String syncStatus,
                            int totalCount, int createdCount, int updatedCount, int disabledCount,
                            int conflictCount, int errorCount, String errorDetail) {
        Long logId = Ids.next();
        String sql = "INSERT INTO sec_user_sync_log (id, tenant_id, provider_id, sync_type, sync_status, "
                + "total_count, created_count, updated_count, disabled_count, conflict_count, error_count, "
                + "error_detail, started_time, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, logId);
            ps.setLong(2, tenantId);
            ps.setLong(3, providerId);
            ps.setString(4, syncType);
            ps.setString(5, syncStatus);
            ps.setInt(6, totalCount);
            ps.setInt(7, createdCount);
            ps.setInt(8, updatedCount);
            ps.setInt(9, disabledCount);
            ps.setInt(10, conflictCount);
            ps.setInt(11, errorCount);
            ps.setString(12, errorDetail);
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("save sync log failed", ex);
        }
    }

    /**
     * 更新身份源同步状态。
     */
    public void updateProviderSyncStatus(Long providerId, String syncResult, String syncSummary) {
        String sql = "UPDATE sec_identity_provider SET last_sync_time = ?, last_sync_result = ?, "
                + "last_sync_summary = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, syncResult);
            ps.setString(3, syncSummary);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(5, providerId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("update provider sync status failed", ex);
        }
    }

    /**
     * 根据工号查找用户。
     */
    public SecurityUser findUserByEmployeeId(Long tenantId, String employeeId) {
        String sql = "SELECT u.id, u.tenant_id, u.username, u.password_hash, u.display_name, "
                + "u.email, u.phone, u.avatar_url, u.status, u.last_login_time, u.last_login_ip, "
                + "u.login_attempts, u.locked_until, u.user_type, u.employee_id "
                + "FROM sec_user u WHERE u.tenant_id = ? AND u.employee_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SecurityUser user = mapUser(rs);
                    user.setRoles(findUserRoles(user.getId()));
                    user.setPermissions(findUserPermissions(user.getId()));
                    user.setOrgScopes(findUserOrgScopes(user.getId()));
                    return user;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find user by employee id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    private IdentityProvider mapIdentityProvider(ResultSet rs) throws SQLException {
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
        if (lastSync != null) { provider.setLastSyncTime(lastSync.toLocalDateTime()); }
        provider.setLastSyncResult(rs.getString("last_sync_result"));
        provider.setLastSyncSummary(rs.getString("last_sync_summary"));
        provider.setCreatedBy(rs.getString("created_by"));
        Timestamp created = rs.getTimestamp("created_time");
        if (created != null) { provider.setCreatedTime(created.toLocalDateTime()); }
        provider.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updated = rs.getTimestamp("updated_time");
        if (updated != null) { provider.setUpdatedTime(updated.toLocalDateTime()); }
        return provider;
    }

    private IdentityBinding mapIdentityBinding(ResultSet rs) throws SQLException {
        IdentityBinding binding = new IdentityBinding();
        binding.setId(rs.getLong("id"));
        binding.setTenantId(rs.getLong("tenant_id"));
        binding.setUserId(rs.getLong("user_id"));
        binding.setProviderId(rs.getLong("provider_id"));
        binding.setExternalSubject(rs.getString("external_subject"));
        binding.setExternalOrgCode(rs.getString("external_org_code"));
        binding.setExternalDisplayName(rs.getString("external_display_name"));
        binding.setBindingStatus(rs.getString("binding_status"));
        Timestamp verified = rs.getTimestamp("last_verified_time");
        if (verified != null) { binding.setLastVerifiedTime(verified.toLocalDateTime()); }
        binding.setCreatedBy(rs.getString("created_by"));
        Timestamp created = rs.getTimestamp("created_time");
        if (created != null) { binding.setCreatedTime(created.toLocalDateTime()); }
        binding.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updated = rs.getTimestamp("updated_time");
        if (updated != null) { binding.setUpdatedTime(updated.toLocalDateTime()); }
        return binding;
    }

    private List<String> loadSchemaStatements(String resourcePath) {
        List<String> statements = new ArrayList<String>();
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("SEC schema resource not found: {}", resourcePath);
                return statements;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                        continue;
                    }
                    sb.append(line).append("\n");
                    if (trimmed.endsWith(";")) {
                        statements.add(sb.toString().trim());
                        sb.setLength(0);
                    }
                }
                if (sb.length() > 0) {
                    statements.add(sb.toString().trim());
                }
            }
        } catch (IOException ex) {
            log.error("load SEC schema failed", ex);
        }
        return statements;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15: 走 HikariCP 连接池（EngineDataSourceConfig 暴露的 DataSource）。
        return dataSource.getConnection();
    }

    // ============================================================
    // SEC-006: 院内用户体系同步相关方法
    // ============================================================

    // --- Identity Provider CRUD ---

    /**
     * 保存或更新身份源配置（UPDATE + INSERT 两阶段 upsert，兼容 Oracle/DM/PG/H2）。
     */
    public void saveIdentityProvider(IdentityProvider provider) {
        String updateSql = "UPDATE sec_identity_provider SET provider_name = ?, provider_type = ?, "
                + "adapter_code = ?, sync_mode = ?, sync_cron = ?, priority = ?, status = ?, "
                + "updated_by = ?, updated_time = ? WHERE tenant_id = ? AND provider_code = ?";
        String insertSql = "INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, "
                + "provider_type, adapter_code, sync_mode, sync_cron, priority, status, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = connection()) {
            // Try UPDATE first
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setString(1, provider.getProviderName());
                ps.setString(2, provider.getProviderType());
                ps.setString(3, provider.getAdapterCode());
                ps.setString(4, provider.getSyncMode());
                ps.setString(5, provider.getSyncCron());
                ps.setInt(6, provider.getPriority());
                ps.setString(7, provider.getStatus());
                ps.setString(8, provider.getUpdatedBy());
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(10, provider.getTenantId());
                ps.setString(11, provider.getProviderCode());
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    return;
                }
            }

            // If no rows updated, INSERT
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setLong(1, provider.getId() != null ? provider.getId() : Ids.next());
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
                ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save identity provider failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 按租户查询所有身份源配置。
     */
    public List<IdentityProvider> findIdentityProvidersByTenant(Long tenantId) {
        String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, "
                + "adapter_code, sync_mode, sync_cron, priority, status, "
                + "last_sync_time, last_sync_result, last_sync_summary, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_provider WHERE tenant_id = ? ORDER BY priority";
        List<IdentityProvider> providers = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    providers.add(mapIdentityProvider(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find identity providers by tenant failed: " + ex.getMessage(), ex);
        }
        return providers;
    }

    /**
     * 按 ID 查询身份源配置。
     */
    public IdentityProvider findIdentityProviderById(Long providerId) {
        String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, "
                + "adapter_code, sync_mode, sync_cron, priority, status, "
                + "last_sync_time, last_sync_result, last_sync_summary, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_provider WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapIdentityProvider(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find identity provider by id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 删除身份源配置。
     */
    public void deleteIdentityProvider(Long providerId) {
        String sql = "DELETE FROM sec_identity_provider WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, providerId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("delete identity provider failed: " + ex.getMessage(), ex);
        }
    }

    // --- Identity Binding CRUD ---

    /**
     * 按 (tenantId, providerId, externalSubject) 查找绑定。
     */
    public IdentityBinding findBinding(Long tenantId, Long providerId, String externalSubject) {
        String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, external_org_code, "
                + "external_display_name, binding_status, last_verified_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_binding WHERE tenant_id = ? AND provider_id = ? AND external_subject = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, providerId);
            ps.setString(3, externalSubject);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapIdentityBinding(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find binding failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 按用户 ID 查找所有绑定。
     */
    public List<IdentityBinding> findBindingsByUserId(Long userId) {
        String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, external_org_code, "
                + "external_display_name, binding_status, last_verified_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_binding WHERE user_id = ?";
        List<IdentityBinding> bindings = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bindings.add(mapIdentityBinding(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find bindings by user id failed: " + ex.getMessage(), ex);
        }
        return bindings;
    }

    /**
     * 保存绑定（UPDATE + INSERT 两阶段 upsert）。
     */
    public void saveIdentityBinding(IdentityBinding binding) {
        String updateSql = "UPDATE sec_identity_binding SET user_id = ?, "
                + "external_org_code = ?, external_display_name = ?, binding_status = ?, "
                + "last_verified_time = ?, updated_by = ?, updated_time = ? "
                + "WHERE tenant_id = ? AND provider_id = ? AND external_subject = ?";
        String insertSql = "INSERT INTO sec_identity_binding (id, tenant_id, user_id, provider_id, "
                + "external_subject, external_org_code, external_display_name, "
                + "binding_status, last_verified_time, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = connection()) {
            // Try UPDATE first
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setLong(1, binding.getUserId());
                ps.setString(2, binding.getExternalOrgCode());
                ps.setString(3, binding.getExternalDisplayName());
                ps.setString(4, binding.getBindingStatus());
                ps.setTimestamp(5, binding.getLastVerifiedTime() != null ? Timestamp.valueOf(binding.getLastVerifiedTime()) : null);
                ps.setString(6, binding.getUpdatedBy());
                ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(8, binding.getTenantId());
                ps.setLong(9, binding.getProviderId());
                ps.setString(10, binding.getExternalSubject());
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    return;
                }
            }

            // If no rows updated, INSERT
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setLong(1, binding.getId() != null ? binding.getId() : Ids.next());
                ps.setLong(2, binding.getTenantId());
                ps.setLong(3, binding.getUserId());
                ps.setLong(4, binding.getProviderId());
                ps.setString(5, binding.getExternalSubject());
                ps.setString(6, binding.getExternalOrgCode());
                ps.setString(7, binding.getExternalDisplayName());
                ps.setString(8, binding.getBindingStatus());
                ps.setTimestamp(9, binding.getLastVerifiedTime() != null ? Timestamp.valueOf(binding.getLastVerifiedTime()) : null);
                ps.setString(10, binding.getCreatedBy());
                ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save identity binding failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 更新绑定的最近同步时间。
     */
    public void updateBindingSyncTime(Long bindingId) {
        String sql = "UPDATE sec_identity_binding SET last_verified_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, bindingId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("update binding sync time failed for binding {}", bindingId, ex);
        }
    }

    // --- User CRUD for Sync ---

    /**
     * 创建用户（INSERT），返回生成的用户 ID。
     */
    public Long createUser(Long tenantId, String username, String displayName,
                           String email, String phone, String status, String createdBy) {
        String sql = "INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, "
                + "email, phone, status, created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Long userId = Ids.next();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, tenantId);
            ps.setString(3, username);
            ps.setString(4, "$2a$10$SYNC_PLACEHOLDER_NOT_FOR_LOGIN"); // 同步用户不能密码登录
            ps.setString(5, displayName);
            ps.setString(6, email);
            ps.setString(7, phone);
            ps.setString(8, status);
            ps.setString(9, createdBy);
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create user failed: " + ex.getMessage(), ex);
        }
        return userId;
    }

    /**
     * 更新用户快照字段（display_name, email, phone, status）。
     */
    public void updateUserSnapshot(Long userId, String displayName, String email, String phone, String status) {
        String sql = "UPDATE sec_user SET display_name = ?, email = ?, phone = ?, status = ?, "
                + "updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, status);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(6, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("update user snapshot failed for user {}", userId, ex);
        }
    }

    /**
     * 禁用用户（设置 status = 'DISABLED'）。
     */
    public void disableUser(Long userId) {
        String sql = "UPDATE sec_user SET status = 'DISABLED', updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("disable user failed for user {}", userId, ex);
        }
    }

    /**
     * 按租户和用户名查找用户（用于同步去重）。
     */
    public SecurityUser findByTenantAndUsername(Long tenantId, String username) {
        String sql = "SELECT id, tenant_id, username, password_hash, display_name, "
                + "email, phone, avatar_url, status, last_login_time, last_login_ip, "
                + "login_attempts, locked_until FROM sec_user WHERE tenant_id = ? AND username = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find user by tenant and username failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // --- Org Scope Management for Sync ---

    /**
     * 删除用户的所有组织范围（同步前清理）。
     */
    public void clearUserOrgScopes(Long userId) {
        String sql = "DELETE FROM sec_user_org_scope WHERE user_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("clear user org scopes failed for user {}", userId, ex);
        }
    }

    /**
     * 批量插入用户组织范围。
     */
    public void insertUserOrgScopes(Long tenantId, Long userId, List<SecurityUser.OrgScope> scopes) {
        String sql = "INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, "
                + "scope_name, created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (SecurityUser.OrgScope scope : scopes) {
                ps.setLong(1, Ids.next());
                ps.setLong(2, tenantId);
                ps.setLong(3, userId);
                ps.setString(4, scope.getScopeLevel());
                ps.setString(5, scope.getScopeCode());
                ps.setString(6, scope.getScopeName());
                ps.setString(7, "sync");
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            log.error("insert user org scopes failed for user {}", userId, ex);
        }
    }

    // ==================== 管理员用户查询（PR-FINAL-08a）====================

    /**
     * 分页查询用户列表（支持关键字 / 状态 / 角色筛选）。
     *
     * <p>page 从 1 开始；role 为角色编码 role_code。
     */
    public List<SecurityUser> listUsers(Long tenantId, String keyword, String status, String role,
                                        int page, int size) {
        StringBuilder sql = new StringBuilder(
                "SELECT u.id, u.tenant_id, u.username, u.password_hash, u.display_name, "
                        + "u.email, u.phone, u.avatar_url, u.status, u.last_login_time, "
                        + "u.last_login_ip, u.login_attempts, u.locked_until, u.user_type, u.employee_id "
                        + "FROM sec_user u");
        if (role != null && !role.isEmpty()) {
            sql.append(" JOIN sec_user_role ur ON ur.user_id = u.id "
                    + "JOIN sec_role r ON r.id = ur.role_id AND r.role_code = ?");
        }
        sql.append(" WHERE u.tenant_id = ?");
        if (status != null && !status.isEmpty()) sql.append(" AND u.status = ?");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (u.username LIKE ? OR u.display_name LIKE ? OR u.email LIKE ?)");
        }
        sql.append(" ORDER BY u.id DESC LIMIT ? OFFSET ?");
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            if (role != null && !role.isEmpty()) ps.setString(idx++, role);
            ps.setLong(idx++, tenantId);
            if (status != null && !status.isEmpty()) ps.setString(idx++, status);
            if (keyword != null && !keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            ps.setInt(idx++, size);
            ps.setInt(idx, Math.max(0, page - 1) * size);
            try (ResultSet rs = ps.executeQuery()) {
                List<SecurityUser> users = new ArrayList<SecurityUser>();
                while (rs.next()) {
                    SecurityUser u = mapUser(rs);
                    u.setRoles(findUserRoles(u.getId()));
                    users.add(u);
                }
                return users;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list users failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 统计符合条件的用户总数。
     */
    public long countUsers(Long tenantId, String keyword, String status, String role) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sec_user u");
        if (role != null && !role.isEmpty()) {
            sql.append(" JOIN sec_user_role ur ON ur.user_id = u.id "
                    + "JOIN sec_role r ON r.id = ur.role_id AND r.role_code = ?");
        }
        sql.append(" WHERE u.tenant_id = ?");
        if (status != null && !status.isEmpty()) sql.append(" AND u.status = ?");
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (u.username LIKE ? OR u.display_name LIKE ? OR u.email LIKE ?)");
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            if (role != null && !role.isEmpty()) ps.setString(idx++, role);
            ps.setLong(idx++, tenantId);
            if (status != null && !status.isEmpty()) ps.setString(idx++, status);
            if (keyword != null && !keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("count users failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 更新用户状态（ACTIVE / DISABLED）。
     */
    public void updateUserStatus(Long userId, String status, String operatorUsername) {
        String sql = "UPDATE sec_user SET status = ?, updated_by = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, operatorUsername);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(4, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update user status failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 解锁用户（清空锁定时间 + 重置登录失败次数）。
     */
    public void unlockUser(Long userId, String operatorUsername) {
        String sql = "UPDATE sec_user SET locked_until = NULL, login_attempts = 0, "
                + "updated_by = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, operatorUsername);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("unlock user failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 替换用户角色（先删后插）。roleCodes 为空时清空所有角色。
     */
    public void replaceUserRoles(Long userId, Long tenantId, List<String> roleCodes, String operatorUsername) {
        try (Connection connection = connection()) {
            try (PreparedStatement del = connection.prepareStatement(
                    "DELETE FROM sec_user_role WHERE user_id = ?")) {
                del.setLong(1, userId);
                del.executeUpdate();
            }
            if (roleCodes != null && !roleCodes.isEmpty()) {
                String insertSql = "INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, "
                        + "created_by, created_time) "
                        + "SELECT ?, ?, ?, r.id, ?, ? FROM sec_role r "
                        + "WHERE r.tenant_id = ? AND r.role_code = ?";
                try (PreparedStatement ins = connection.prepareStatement(insertSql)) {
                    for (String roleCode : roleCodes) {
                        ins.setLong(1, Ids.next());
                        ins.setLong(2, tenantId);
                        ins.setLong(3, userId);
                        ins.setString(4, operatorUsername);
                        ins.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                        ins.setLong(6, tenantId);
                        ins.setString(7, roleCode);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("replace user roles failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 重置用户密码（直接写入新哈希，同时清除锁定状态）。
     */
    public void resetPassword(Long userId, String newPasswordHash, String operatorUsername) {
        String sql = "UPDATE sec_user SET password_hash = ?, login_attempts = 0, "
                + "locked_until = NULL, updated_by = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setString(2, operatorUsername);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(4, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("reset password failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 查询租户下所有激活角色（用于前端角色分配下拉）。
     */
    public List<Map<String, String>> listRoles(Long tenantId) {
        String sql = "SELECT role_code, role_name, role_type FROM sec_role "
                + "WHERE tenant_id = ? AND status = 'ACTIVE' ORDER BY role_type, role_code";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, String>> roles = new ArrayList<Map<String, String>>();
                while (rs.next()) {
                    java.util.LinkedHashMap<String, String> role =
                            new java.util.LinkedHashMap<String, String>();
                    role.put("role_code", rs.getString("role_code"));
                    role.put("role_name", rs.getString("role_name"));
                    role.put("role_type", rs.getString("role_type"));
                    roles.add(role);
                }
                return roles;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list roles failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 检查用户名在租户内是否已存在。
     */
    public boolean usernameExists(Long tenantId, String username) {
        String sql = "SELECT COUNT(*) FROM sec_user WHERE tenant_id = ? AND username = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("check username exists failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 创建用户并指定密码哈希（管理员批量导入 / 手动创建专用）。
     */
    public Long createUserWithPassword(Long tenantId, String username, String displayName,
                                        String email, String phone, String userType,
                                        String employeeId, String passwordHash, String createdBy) {
        Long userId = Ids.next();
        String sql = "INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, "
                + "email, phone, status, user_type, employee_id, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, tenantId);
            ps.setString(3, username);
            ps.setString(4, passwordHash);
            ps.setString(5, displayName);
            ps.setString(6, email);
            ps.setString(7, phone);
            ps.setString(8, userType != null ? userType : "STAFF");
            ps.setString(9, employeeId);
            ps.setString(10, createdBy);
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("createUserWithPassword failed: " + ex.getMessage(), ex);
        }
        return userId;
    }

}
