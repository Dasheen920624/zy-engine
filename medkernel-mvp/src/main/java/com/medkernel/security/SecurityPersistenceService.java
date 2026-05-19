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
}
