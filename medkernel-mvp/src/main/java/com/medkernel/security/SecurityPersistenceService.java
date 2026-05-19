package com.medkernel.security;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 安全模块持久化服务：使用 raw JDBC 操作 SEC 表。
 * 复用 EnginePersistenceProperties 获取数据库连接配置。
 */
@Service
public class SecurityPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(SecurityPersistenceService.class);

    private final EnginePersistenceProperties properties;

    public SecurityPersistenceService(EnginePersistenceProperties properties) {
        this.properties = properties;
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
                + "u.login_attempts, u.locked_until "
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
                + "u.login_attempts, u.locked_until "
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
        String sql = "INSERT INTO sec_auth_audit_log (id, user_id, tenant_id, action, ip_address, detail, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Ids.next());
            ps.setLong(2, userId);
            ps.setLong(3, tenantId);
            ps.setString(4, action);
            ps.setString(5, ip);
            ps.setString(6, detail);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
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
        return user;
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
        return DriverManager.getConnection(
                properties.getUrl(), properties.getUsername(), properties.getPassword());
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
                + "adapter_code = ?, query_code = ?, priority = ?, status = ?, config_json = ?, "
                + "updated_by = ?, updated_time = ? WHERE tenant_id = ? AND provider_code = ?";
        String insertSql = "INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, "
                + "provider_type, adapter_code, query_code, priority, status, config_json, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = connection()) {
            // Try UPDATE first
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setString(1, provider.getProviderName());
                ps.setString(2, provider.getProviderType());
                ps.setString(3, provider.getAdapterCode());
                ps.setString(4, provider.getQueryCode());
                ps.setInt(5, provider.getPriority());
                ps.setString(6, provider.getStatus());
                ps.setString(7, provider.getConfigJson());
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
                ps.setString(7, provider.getQueryCode());
                ps.setInt(8, provider.getPriority());
                ps.setString(9, provider.getStatus());
                ps.setString(10, provider.getConfigJson());
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
                + "adapter_code, query_code, priority, status, config_json, created_by, created_time, "
                + "updated_by, updated_time FROM sec_identity_provider WHERE tenant_id = ? ORDER BY priority";
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
                + "adapter_code, query_code, priority, status, config_json, created_by, created_time, "
                + "updated_by, updated_time FROM sec_identity_provider WHERE id = ?";
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
        String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, external_name, "
                + "external_org_code, external_org_name, external_position, status, last_sync_time, "
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
        String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, external_name, "
                + "external_org_code, external_org_name, external_position, status, last_sync_time, "
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
        String updateSql = "UPDATE sec_identity_binding SET user_id = ?, external_name = ?, "
                + "external_org_code = ?, external_org_name = ?, external_position = ?, status = ?, "
                + "last_sync_time = ?, updated_by = ?, updated_time = ? "
                + "WHERE tenant_id = ? AND provider_id = ? AND external_subject = ?";
        String insertSql = "INSERT INTO sec_identity_binding (id, tenant_id, user_id, provider_id, "
                + "external_subject, external_name, external_org_code, external_org_name, external_position, "
                + "status, last_sync_time, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = connection()) {
            // Try UPDATE first
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setLong(1, binding.getUserId());
                ps.setString(2, binding.getExternalName());
                ps.setString(3, binding.getExternalOrgCode());
                ps.setString(4, binding.getExternalOrgName());
                ps.setString(5, binding.getExternalPosition());
                ps.setString(6, binding.getStatus());
                ps.setTimestamp(7, binding.getLastSyncTime() != null ? Timestamp.valueOf(binding.getLastSyncTime()) : null);
                ps.setString(8, binding.getUpdatedBy());
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(10, binding.getTenantId());
                ps.setLong(11, binding.getProviderId());
                ps.setString(12, binding.getExternalSubject());
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
                ps.setString(6, binding.getExternalName());
                ps.setString(7, binding.getExternalOrgCode());
                ps.setString(8, binding.getExternalOrgName());
                ps.setString(9, binding.getExternalPosition());
                ps.setString(10, binding.getStatus());
                ps.setTimestamp(11, binding.getLastSyncTime() != null ? Timestamp.valueOf(binding.getLastSyncTime()) : null);
                ps.setString(12, binding.getCreatedBy());
                ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
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
        String sql = "UPDATE sec_identity_binding SET last_sync_time = ? WHERE id = ?";
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

    // --- Sync Job and Detail CRUD ---

    /**
     * 创建同步任务记录，返回生成的 job ID。
     */
    public Long createSyncJob(UserSyncJob job) {
        String sql = "INSERT INTO sec_user_sync_job (id, tenant_id, provider_id, sync_type, status, "
                + "started_at, triggered_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Long jobId = Ids.next();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            ps.setLong(2, job.getTenantId());
            ps.setLong(3, job.getProviderId());
            ps.setString(4, job.getSyncType());
            ps.setString(5, job.getStatus());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(7, job.getTriggeredBy());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create sync job failed: " + ex.getMessage(), ex);
        }
        return jobId;
    }

    /**
     * 更新同步任务状态和统计。
     */
    public void updateSyncJob(UserSyncJob job) {
        String sql = "UPDATE sec_user_sync_job SET status = ?, total_count = ?, created_count = ?, "
                + "updated_count = ?, disabled_count = ?, skipped_count = ?, error_count = ?, "
                + "finished_at = ?, error_message = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, job.getStatus());
            ps.setInt(2, job.getTotalCount());
            ps.setInt(3, job.getCreatedCount());
            ps.setInt(4, job.getUpdatedCount());
            ps.setInt(5, job.getDisabledCount());
            ps.setInt(6, job.getSkippedCount());
            ps.setInt(7, job.getErrorCount());
            ps.setTimestamp(8, job.getFinishedAt() != null ? Timestamp.valueOf(job.getFinishedAt()) : null);
            ps.setString(9, job.getErrorMessage());
            ps.setLong(10, job.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update sync job failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 按租户查询同步任务列表（最近 N 条）。
     */
    public List<UserSyncJob> findSyncJobsByTenant(Long tenantId, int limit) {
        String sql = "SELECT id, tenant_id, provider_id, sync_type, status, total_count, "
                + "created_count, updated_count, disabled_count, skipped_count, error_count, "
                + "started_at, finished_at, triggered_by, error_message "
                + "FROM sec_user_sync_job WHERE tenant_id = ? ORDER BY started_at DESC LIMIT ?";
        List<UserSyncJob> jobs = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    jobs.add(mapSyncJob(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find sync jobs by tenant failed: " + ex.getMessage(), ex);
        }
        return jobs;
    }

    /**
     * 按 job ID 查询同步任务。
     */
    public UserSyncJob findSyncJobById(Long jobId) {
        String sql = "SELECT id, tenant_id, provider_id, sync_type, status, total_count, "
                + "created_count, updated_count, disabled_count, skipped_count, error_count, "
                + "started_at, finished_at, triggered_by, error_message "
                + "FROM sec_user_sync_job WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSyncJob(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find sync job by id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 批量插入同步明细。
     */
    public void insertSyncDetails(List<UserSyncDetail> details) {
        String sql = "INSERT INTO sec_user_sync_detail (id, job_id, tenant_id, external_subject, "
                + "external_name, action, platform_user_id, message, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (UserSyncDetail detail : details) {
                ps.setLong(1, detail.getId() != null ? detail.getId() : Ids.next());
                ps.setLong(2, detail.getJobId());
                ps.setLong(3, detail.getTenantId());
                ps.setString(4, detail.getExternalSubject());
                ps.setString(5, detail.getExternalName());
                ps.setString(6, detail.getAction());
                if (detail.getPlatformUserId() != null) {
                    ps.setLong(7, detail.getPlatformUserId());
                } else {
                    ps.setNull(7, java.sql.Types.BIGINT);
                }
                ps.setString(8, detail.getMessage());
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException ex) {
            throw new IllegalStateException("insert sync details failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 按 job ID 查询同步明细。
     */
    public List<UserSyncDetail> findSyncDetailsByJobId(Long jobId) {
        String sql = "SELECT id, job_id, tenant_id, external_subject, external_name, action, "
                + "platform_user_id, message, created_time FROM sec_user_sync_detail WHERE job_id = ?";
        List<UserSyncDetail> details = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    details.add(mapSyncDetail(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find sync details by job id failed: " + ex.getMessage(), ex);
        }
        return details;
    }

    /**
     * 按 job ID 查询同步明细（带分页）。
     */
    public List<UserSyncDetail> findSyncDetailsByJobId(Long jobId, int limit) {
        String sql = "SELECT id, job_id, tenant_id, external_subject, external_name, action, "
                + "platform_user_id, message, created_time FROM sec_user_sync_detail WHERE job_id = ? ORDER BY created_time DESC LIMIT ?";
        List<UserSyncDetail> details = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, jobId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    details.add(mapSyncDetail(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find sync details by job id with limit failed: " + ex.getMessage(), ex);
        }
        return details;
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

    // --- Mapper methods ---

    private IdentityProvider mapIdentityProvider(ResultSet rs) throws SQLException {
        IdentityProvider provider = new IdentityProvider();
        provider.setId(rs.getLong("id"));
        provider.setTenantId(rs.getLong("tenant_id"));
        provider.setProviderCode(rs.getString("provider_code"));
        provider.setProviderName(rs.getString("provider_name"));
        provider.setProviderType(rs.getString("provider_type"));
        provider.setAdapterCode(rs.getString("adapter_code"));
        provider.setQueryCode(rs.getString("query_code"));
        provider.setPriority(rs.getInt("priority"));
        provider.setStatus(rs.getString("status"));
        provider.setConfigJson(rs.getString("config_json"));
        provider.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            provider.setCreatedTime(createdTime.toLocalDateTime());
        }
        provider.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            provider.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return provider;
    }

    private IdentityBinding mapIdentityBinding(ResultSet rs) throws SQLException {
        IdentityBinding binding = new IdentityBinding();
        binding.setId(rs.getLong("id"));
        binding.setTenantId(rs.getLong("tenant_id"));
        binding.setUserId(rs.getLong("user_id"));
        binding.setProviderId(rs.getLong("provider_id"));
        binding.setExternalSubject(rs.getString("external_subject"));
        binding.setExternalName(rs.getString("external_name"));
        binding.setExternalOrgCode(rs.getString("external_org_code"));
        binding.setExternalOrgName(rs.getString("external_org_name"));
        binding.setExternalPosition(rs.getString("external_position"));
        binding.setStatus(rs.getString("status"));
        Timestamp lastSyncTime = rs.getTimestamp("last_sync_time");
        if (lastSyncTime != null) {
            binding.setLastSyncTime(lastSyncTime.toLocalDateTime());
        }
        binding.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            binding.setCreatedTime(createdTime.toLocalDateTime());
        }
        binding.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            binding.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return binding;
    }

    private UserSyncJob mapSyncJob(ResultSet rs) throws SQLException {
        UserSyncJob job = new UserSyncJob();
        job.setId(rs.getLong("id"));
        job.setTenantId(rs.getLong("tenant_id"));
        job.setProviderId(rs.getLong("provider_id"));
        job.setSyncType(rs.getString("sync_type"));
        job.setStatus(rs.getString("status"));
        job.setTotalCount(rs.getInt("total_count"));
        job.setCreatedCount(rs.getInt("created_count"));
        job.setUpdatedCount(rs.getInt("updated_count"));
        job.setDisabledCount(rs.getInt("disabled_count"));
        job.setSkippedCount(rs.getInt("skipped_count"));
        job.setErrorCount(rs.getInt("error_count"));
        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            job.setStartedAt(startedAt.toLocalDateTime());
        }
        Timestamp finishedAt = rs.getTimestamp("finished_at");
        if (finishedAt != null) {
            job.setFinishedAt(finishedAt.toLocalDateTime());
        }
        job.setTriggeredBy(rs.getString("triggered_by"));
        job.setErrorMessage(rs.getString("error_message"));
        return job;
    }

    private UserSyncDetail mapSyncDetail(ResultSet rs) throws SQLException {
        UserSyncDetail detail = new UserSyncDetail();
        detail.setId(rs.getLong("id"));
        detail.setJobId(rs.getLong("job_id"));
        detail.setTenantId(rs.getLong("tenant_id"));
        detail.setExternalSubject(rs.getString("external_subject"));
        detail.setExternalName(rs.getString("external_name"));
        detail.setAction(rs.getString("action"));
        long platformUserId = rs.getLong("platform_user_id");
        if (!rs.wasNull()) {
            detail.setPlatformUserId(platformUserId);
        }
        detail.setMessage(rs.getString("message"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            detail.setCreatedTime(createdTime.toLocalDateTime());
        }
        return detail;
    }
}
