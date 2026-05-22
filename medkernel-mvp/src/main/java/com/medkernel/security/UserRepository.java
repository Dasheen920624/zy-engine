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
public class UserRepository extends SecurityRepositorySupport {
    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);
    private final RoleRepository roleRepository;
    public UserRepository(EnginePersistenceProperties properties, DataSource dataSource, RoleRepository roleRepository) {
        super(properties, dataSource);
        this.roleRepository = roleRepository;
    }
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
                        SecurityUser user = SecurityUserRowMapper.mapUser(rs);
                        user.setRoles(roleRepository.findUserRoles(user.getId()));
                        user.setPermissions(roleRepository.findUserPermissions(user.getId()));
                        user.setOrgScopes(findUserOrgScopes(user.getId()));
                        return user;
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find user by username failed: " + ex.getMessage(), ex);
            }
            return null;
        }
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
                        SecurityUser user = SecurityUserRowMapper.mapUser(rs);
                        user.setRoles(roleRepository.findUserRoles(user.getId()));
                        user.setPermissions(roleRepository.findUserPermissions(user.getId()));
                        user.setOrgScopes(findUserOrgScopes(user.getId()));
                        return user;
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find user by id failed: " + ex.getMessage(), ex);
            }
            return null;
        }
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
        public Long createUser(Long tenantId, String username, String displayName, String email, String phone,
                               String userType, String employeeId, String status) {
            Long userId = nextId();
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
                        SecurityUser user = SecurityUserRowMapper.mapUser(rs);
                        user.setRoles(roleRepository.findUserRoles(user.getId()));
                        user.setPermissions(roleRepository.findUserPermissions(user.getId()));
                        user.setOrgScopes(findUserOrgScopes(user.getId()));
                        return user;
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("find user by employee id failed: " + ex.getMessage(), ex);
            }
            return null;
        }
}
