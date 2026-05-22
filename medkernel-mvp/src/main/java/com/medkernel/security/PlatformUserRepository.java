package com.medkernel.security;
import com.medkernel.persistence.EnginePersistenceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Repository
public class PlatformUserRepository extends SecurityRepositorySupport {
    private static final Logger log = LoggerFactory.getLogger(PlatformUserRepository.class);
    private final RoleRepository roleRepository;
    public PlatformUserRepository(EnginePersistenceProperties properties, DataSource dataSource,
                                  RoleRepository roleRepository) {
        super(properties, dataSource);
        this.roleRepository = roleRepository;
    }
        public Long createUser(Long tenantId, String username, String displayName,
                               String email, String phone, String status, String createdBy) {
            String sql = "INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, "
                    + "email, phone, status, created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            Long userId = nextId();
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
        public SecurityUser findByTenantAndUsername(Long tenantId, String username) {
            String sql = "SELECT id, tenant_id, username, password_hash, display_name, "
                    + "email, phone, avatar_url, status, last_login_time, last_login_ip, "
                    + "login_attempts, locked_until, user_type, employee_id FROM sec_user WHERE tenant_id = ? AND username = ?";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, tenantId);
                ps.setString(2, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return SecurityUserRowMapper.mapUser(rs);
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find user by tenant and username failed: " + ex.getMessage(), ex);
            }
            return null;
        }
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
        public void insertUserOrgScopes(Long tenantId, Long userId, List<SecurityUser.OrgScope> scopes) {
            String sql = "INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, "
                    + "scope_name, created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                for (SecurityUser.OrgScope scope : scopes) {
                    ps.setLong(1, nextId());
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
                        SecurityUser u = SecurityUserRowMapper.mapUser(rs);
                        u.setRoles(roleRepository.findUserRoles(u.getId()));
                        users.add(u);
                    }
                    return users;
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("list users failed: " + ex.getMessage(), ex);
            }
        }
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
        public Long createUserWithPassword(Long tenantId, String username, String displayName,
                                            String email, String phone, String userType,
                                            String employeeId, String passwordHash, String createdBy) {
            Long userId = nextId();
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
