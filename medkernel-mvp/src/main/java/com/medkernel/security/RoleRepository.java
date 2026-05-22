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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Repository
public class RoleRepository extends SecurityRepositorySupport {
    private static final Logger log = LoggerFactory.getLogger(RoleRepository.class);
    public RoleRepository(EnginePersistenceProperties properties, DataSource dataSource) {
        super(properties, dataSource);
    }
        List<String> findUserRoles(Long userId) {
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
        List<String> findUserPermissions(Long userId) {
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
                            ins.setLong(1, nextId());
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
        public List<Map<String, String>> listRoles(Long tenantId) {
            String sql = "SELECT role_code, role_name, role_type FROM sec_role "
                    + "WHERE tenant_id = ? AND status = 'ACTIVE' ORDER BY role_type, role_code";
            try (Connection connection = connection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, tenantId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Map<String, String>> roles = new ArrayList<Map<String, String>>();
                    while (rs.next()) {
                        LinkedHashMap<String, String> role =
                                new LinkedHashMap<String, String>();
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
}
