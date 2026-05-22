package com.medkernel.security;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SEC-002 数据权限服务：策略管理、权限分配、权限检查。
 * 使用 raw JDBC 操作 sec_data_permission_policy / sec_data_permission_assignment 表。
 */
@Service
public class DataPermissionService {

    private static final Logger log = LoggerFactory.getLogger(DataPermissionService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public DataPermissionService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    // ============================================================
    // 策略管理
    // ============================================================

    /**
     * 创建数据权限策略。
     */
    public DataPermissionPolicy createPolicy(DataPermissionPolicy policy) {
        if (policy.getId() == null) {
            policy.setId(Ids.next());
        }
        if (policy.getEnabled() == null) {
            policy.setEnabled("Y");
        }
        policy.setCreatedTime(LocalDateTime.now());
        policy.setUpdatedTime(LocalDateTime.now());

        String sql = "INSERT INTO sec_data_permission_policy "
                + "(id, tenant_id, policy_code, policy_name, policy_type, description, "
                + "scope_expression, filter_expression, priority, enabled, "
                + "created_by, created_time, updated_by, updated_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, policy.getId());
            ps.setLong(2, policy.getTenantId());
            ps.setString(3, policy.getPolicyCode());
            ps.setString(4, policy.getPolicyName());
            ps.setString(5, policy.getPolicyType());
            ps.setString(6, policy.getDescription());
            ps.setString(7, policy.getScopeExpression());
            ps.setString(8, policy.getFilterExpression());
            ps.setString(9, policy.getPriority());
            ps.setString(10, policy.getEnabled());
            ps.setString(11, policy.getCreatedBy());
            ps.setTimestamp(12, Timestamp.valueOf(policy.getCreatedTime()));
            ps.setString(13, policy.getUpdatedBy());
            ps.setTimestamp(14, Timestamp.valueOf(policy.getUpdatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create data permission policy failed: " + ex.getMessage(), ex);
        }
        return policy;
    }

    /**
     * 更新数据权限策略。
     */
    public DataPermissionPolicy updatePolicy(DataPermissionPolicy policy) {
        String sql = "UPDATE sec_data_permission_policy SET "
                + "policy_name = ?, policy_type = ?, description = ?, "
                + "scope_expression = ?, filter_expression = ?, priority = ?, enabled = ?, "
                + "updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";

        policy.setUpdatedTime(LocalDateTime.now());

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, policy.getPolicyName());
            ps.setString(2, policy.getPolicyType());
            ps.setString(3, policy.getDescription());
            ps.setString(4, policy.getScopeExpression());
            ps.setString(5, policy.getFilterExpression());
            ps.setString(6, policy.getPriority());
            ps.setString(7, policy.getEnabled());
            ps.setString(8, policy.getUpdatedBy());
            ps.setTimestamp(9, Timestamp.valueOf(policy.getUpdatedTime()));
            ps.setLong(10, policy.getId());
            ps.setLong(11, policy.getTenantId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("policy not found: id=" + policy.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("update data permission policy failed: " + ex.getMessage(), ex);
        }
        return policy;
    }

    /**
     * 查询数据权限策略列表。
     */
    public List<DataPermissionPolicy> listPolicies(Long tenantId, String policyType, String enabled) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, policy_code, policy_name, policy_type, description, "
                        + "scope_expression, filter_expression, priority, enabled, "
                        + "created_by, created_time, updated_by, updated_time "
                        + "FROM sec_data_permission_policy WHERE tenant_id = ?");
        if (policyType != null && !policyType.isEmpty()) {
            sql.append(" AND policy_type = ?");
        }
        if (enabled != null && !enabled.isEmpty()) {
            sql.append(" AND enabled = ?");
        }
        sql.append(" ORDER BY priority DESC, id ASC");

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setLong(idx++, tenantId);
            if (policyType != null && !policyType.isEmpty()) {
                ps.setString(idx++, policyType);
            }
            if (enabled != null && !enabled.isEmpty()) {
                ps.setString(idx++, enabled);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<DataPermissionPolicy> policies = new ArrayList<DataPermissionPolicy>();
                while (rs.next()) {
                    policies.add(mapPolicy(rs));
                }
                return policies;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list data permission policies failed: " + ex.getMessage(), ex);
        }
    }

    // ============================================================
    // 权限分配
    // ============================================================

    /**
     * 分配数据权限。
     */
    public DataPermissionAssignment assignPermission(DataPermissionAssignment assignment) {
        if (assignment.getId() == null) {
            assignment.setId(Ids.next());
        }
        if (assignment.getEnabled() == null) {
            assignment.setEnabled("Y");
        }
        assignment.setCreatedTime(LocalDateTime.now());
        assignment.setUpdatedTime(LocalDateTime.now());

        // 补充策略名称
        if (assignment.getPolicyName() == null && assignment.getPolicyId() != null) {
            DataPermissionPolicy policy = findPolicyById(assignment.getPolicyId());
            if (policy != null) {
                assignment.setPolicyCode(policy.getPolicyCode());
                assignment.setPolicyName(policy.getPolicyName());
            }
        }

        String sql = "INSERT INTO sec_data_permission_assignment "
                + "(id, tenant_id, assignment_code, principal_type, principal_code, principal_name, "
                + "policy_id, policy_code, policy_name, resource_type, effect, conditions, enabled, "
                + "created_by, created_time, updated_by, updated_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, assignment.getId());
            ps.setLong(2, assignment.getTenantId());
            ps.setString(3, assignment.getAssignmentCode());
            ps.setString(4, assignment.getPrincipalType());
            ps.setString(5, assignment.getPrincipalCode());
            ps.setString(6, assignment.getPrincipalName());
            ps.setLong(7, assignment.getPolicyId());
            ps.setString(8, assignment.getPolicyCode());
            ps.setString(9, assignment.getPolicyName());
            ps.setString(10, assignment.getResourceType());
            ps.setString(11, assignment.getEffect());
            ps.setString(12, assignment.getConditions());
            ps.setString(13, assignment.getEnabled());
            ps.setString(14, assignment.getCreatedBy());
            ps.setTimestamp(15, Timestamp.valueOf(assignment.getCreatedTime()));
            ps.setString(16, assignment.getUpdatedBy());
            ps.setTimestamp(17, Timestamp.valueOf(assignment.getUpdatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("assign data permission failed: " + ex.getMessage(), ex);
        }
        return assignment;
    }

    /**
     * 更新权限分配。
     */
    public DataPermissionAssignment updateAssignment(DataPermissionAssignment assignment) {
        String sql = "UPDATE sec_data_permission_assignment SET "
                + "principal_type = ?, principal_code = ?, principal_name = ?, "
                + "policy_id = ?, policy_code = ?, policy_name = ?, "
                + "resource_type = ?, effect = ?, conditions = ?, enabled = ?, "
                + "updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";

        assignment.setUpdatedTime(LocalDateTime.now());

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, assignment.getPrincipalType());
            ps.setString(2, assignment.getPrincipalCode());
            ps.setString(3, assignment.getPrincipalName());
            ps.setLong(4, assignment.getPolicyId());
            ps.setString(5, assignment.getPolicyCode());
            ps.setString(6, assignment.getPolicyName());
            ps.setString(7, assignment.getResourceType());
            ps.setString(8, assignment.getEffect());
            ps.setString(9, assignment.getConditions());
            ps.setString(10, assignment.getEnabled());
            ps.setString(11, assignment.getUpdatedBy());
            ps.setTimestamp(12, Timestamp.valueOf(assignment.getUpdatedTime()));
            ps.setLong(13, assignment.getId());
            ps.setLong(14, assignment.getTenantId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalStateException("assignment not found: id=" + assignment.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("update data permission assignment failed: " + ex.getMessage(), ex);
        }
        return assignment;
    }

    /**
     * 查询权限分配列表。
     */
    public List<DataPermissionAssignment> listAssignments(Long tenantId, String principalType, String principalCode) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, assignment_code, principal_type, principal_code, principal_name, "
                        + "policy_id, policy_code, policy_name, resource_type, effect, conditions, enabled, "
                        + "created_by, created_time, updated_by, updated_time "
                        + "FROM sec_data_permission_assignment WHERE tenant_id = ?");
        if (principalType != null && !principalType.isEmpty()) {
            sql.append(" AND principal_type = ?");
        }
        if (principalCode != null && !principalCode.isEmpty()) {
            sql.append(" AND principal_code = ?");
        }
        sql.append(" ORDER BY id ASC");

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setLong(idx++, tenantId);
            if (principalType != null && !principalType.isEmpty()) {
                ps.setString(idx++, principalType);
            }
            if (principalCode != null && !principalCode.isEmpty()) {
                ps.setString(idx++, principalCode);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<DataPermissionAssignment> assignments = new ArrayList<DataPermissionAssignment>();
                while (rs.next()) {
                    assignments.add(mapAssignment(rs));
                }
                return assignments;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list data permission assignments failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 移除权限分配。
     */
    public void removeAssignment(Long assignmentId) {
        String sql = "DELETE FROM sec_data_permission_assignment WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, assignmentId);
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                throw new IllegalStateException("assignment not found: id=" + assignmentId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("remove data permission assignment failed: " + ex.getMessage(), ex);
        }
    }

    // ============================================================
    // 权限检查
    // ============================================================

    /**
     * 检查权限：判断主体是否有权访问指定资源类型的 action。
     *
     * @return true 表示允许，false 表示拒绝
     */
    public boolean checkPermission(Long tenantId, String principalType, String principalCode,
                                   String resourceType, String action) {
        // 查找该主体的所有有效分配
        List<DataPermissionAssignment> assignments = findEffectiveAssignments(tenantId, principalType, principalCode);

        // 先检查是否有显式 DENY
        for (DataPermissionAssignment assignment : assignments) {
            if (!"Y".equals(assignment.getEnabled())) {
                continue;
            }
            if (matchesResource(assignment.getResourceType(), resourceType)) {
                if ("DENY".equals(assignment.getEffect())) {
                    return false;
                }
            }
        }

        // 再检查是否有显式 ALLOW
        for (DataPermissionAssignment assignment : assignments) {
            if (!"Y".equals(assignment.getEnabled())) {
                continue;
            }
            if (matchesResource(assignment.getResourceType(), resourceType)) {
                if ("ALLOW".equals(assignment.getEffect())) {
                    return true;
                }
            }
        }

        // 默认拒绝
        return false;
    }

    /**
     * 获取数据过滤条件：返回该主体在指定资源类型上的所有有效过滤表达式。
     */
    public List<Map<String, Object>> filterData(Long tenantId, String principalType,
                                                 String principalCode, String resourceType) {
        List<DataPermissionAssignment> assignments = findEffectiveAssignments(tenantId, principalType, principalCode);
        List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();

        for (DataPermissionAssignment assignment : assignments) {
            if (!"Y".equals(assignment.getEnabled())) {
                continue;
            }
            if (!"ALLOW".equals(assignment.getEffect())) {
                continue;
            }
            if (!matchesResource(assignment.getResourceType(), resourceType)) {
                continue;
            }
            // 获取关联策略的过滤表达式
            if (assignment.getPolicyId() != null) {
                DataPermissionPolicy policy = findPolicyById(assignment.getPolicyId());
                if (policy != null && policy.getFilterExpression() != null) {
                    Map<String, Object> filterEntry = new LinkedHashMap<String, Object>();
                    filterEntry.put("assignment_id", assignment.getId());
                    filterEntry.put("assignment_code", assignment.getAssignmentCode());
                    filterEntry.put("policy_id", policy.getId());
                    filterEntry.put("policy_code", policy.getPolicyCode());
                    filterEntry.put("filter_expression", policy.getFilterExpression());
                    filterEntry.put("scope_expression", policy.getScopeExpression());
                    filterEntry.put("conditions", assignment.getConditions());
                    filters.add(filterEntry);
                }
            }
        }
        return filters;
    }

    /**
     * 获取有效策略：返回该主体关联的所有有效策略。
     */
    public List<Map<String, Object>> getEffectivePolicies(Long tenantId, String principalType, String principalCode) {
        List<DataPermissionAssignment> assignments = findEffectiveAssignments(tenantId, principalType, principalCode);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        for (DataPermissionAssignment assignment : assignments) {
            if (!"Y".equals(assignment.getEnabled())) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("assignment_id", assignment.getId());
            entry.put("assignment_code", assignment.getAssignmentCode());
            entry.put("principal_type", assignment.getPrincipalType());
            entry.put("principal_code", assignment.getPrincipalCode());
            entry.put("principal_name", assignment.getPrincipalName());
            entry.put("policy_id", assignment.getPolicyId());
            entry.put("policy_code", assignment.getPolicyCode());
            entry.put("policy_name", assignment.getPolicyName());
            entry.put("resource_type", assignment.getResourceType());
            entry.put("effect", assignment.getEffect());
            entry.put("conditions", assignment.getConditions());

            // 附加策略详情
            if (assignment.getPolicyId() != null) {
                DataPermissionPolicy policy = findPolicyById(assignment.getPolicyId());
                if (policy != null) {
                    entry.put("policy_type", policy.getPolicyType());
                    entry.put("scope_expression", policy.getScopeExpression());
                    entry.put("filter_expression", policy.getFilterExpression());
                    entry.put("policy_priority", policy.getPriority());
                }
            }
            result.add(entry);
        }
        return result;
    }

    // ============================================================
    // 内部方法
    // ============================================================

    private List<DataPermissionAssignment> findEffectiveAssignments(Long tenantId, String principalType,
                                                                     String principalCode) {
        // 查询直接分配给主体的权限 + 分配给主体所属角色的权限
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, assignment_code, principal_type, principal_code, principal_name, "
                        + "policy_id, policy_code, policy_name, resource_type, effect, conditions, enabled, "
                        + "created_by, created_time, updated_by, updated_time "
                        + "FROM sec_data_permission_assignment WHERE tenant_id = ? AND enabled = 'Y' "
                        + "AND (principal_type = ? AND principal_code = ?)");

        // 如果主体类型是 USER，也查询其角色关联的权限
        if ("USER".equals(principalType)) {
            sql.insert(sql.length(), " OR (principal_type = 'ROLE' AND principal_code IN "
                    + "(SELECT r.role_code FROM sec_user_role ur JOIN sec_role r ON r.id = ur.role_id "
                    + "WHERE ur.user_id = (SELECT u.id FROM sec_user u WHERE u.tenant_id = ? AND u.username = ?)))");
        }

        sql.append(" ORDER BY id ASC");

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setLong(idx++, tenantId);
            ps.setString(idx++, principalType);
            ps.setString(idx++, principalCode);
            if ("USER".equals(principalType)) {
                ps.setLong(idx++, tenantId);
                ps.setString(idx++, principalCode);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<DataPermissionAssignment> assignments = new ArrayList<DataPermissionAssignment>();
                while (rs.next()) {
                    assignments.add(mapAssignment(rs));
                }
                return assignments;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find effective assignments failed: " + ex.getMessage(), ex);
        }
    }

    private DataPermissionPolicy findPolicyById(Long policyId) {
        String sql = "SELECT id, tenant_id, policy_code, policy_name, policy_type, description, "
                + "scope_expression, filter_expression, priority, enabled, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_data_permission_policy WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, policyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPolicy(rs);
                }
            }
            return null;
        } catch (SQLException ex) {
            throw new IllegalStateException("find policy by id failed: " + ex.getMessage(), ex);
        }
    }

    private boolean matchesResource(String assignmentResourceType, String targetResourceType) {
        return "ALL".equals(assignmentResourceType) || assignmentResourceType.equals(targetResourceType);
    }

    private DataPermissionPolicy mapPolicy(ResultSet rs) throws SQLException {
        DataPermissionPolicy policy = new DataPermissionPolicy();
        policy.setId(rs.getLong("id"));
        policy.setTenantId(rs.getLong("tenant_id"));
        policy.setPolicyCode(rs.getString("policy_code"));
        policy.setPolicyName(rs.getString("policy_name"));
        policy.setPolicyType(rs.getString("policy_type"));
        policy.setDescription(rs.getString("description"));
        policy.setScopeExpression(rs.getString("scope_expression"));
        policy.setFilterExpression(rs.getString("filter_expression"));
        policy.setPriority(rs.getString("priority"));
        policy.setEnabled(rs.getString("enabled"));
        policy.setCreatedBy(rs.getString("created_by"));
        Timestamp created = rs.getTimestamp("created_time");
        if (created != null) {
            policy.setCreatedTime(created.toLocalDateTime());
        }
        policy.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updated = rs.getTimestamp("updated_time");
        if (updated != null) {
            policy.setUpdatedTime(updated.toLocalDateTime());
        }
        return policy;
    }

    private DataPermissionAssignment mapAssignment(ResultSet rs) throws SQLException {
        DataPermissionAssignment assignment = new DataPermissionAssignment();
        assignment.setId(rs.getLong("id"));
        assignment.setTenantId(rs.getLong("tenant_id"));
        assignment.setAssignmentCode(rs.getString("assignment_code"));
        assignment.setPrincipalType(rs.getString("principal_type"));
        assignment.setPrincipalCode(rs.getString("principal_code"));
        assignment.setPrincipalName(rs.getString("principal_name"));
        assignment.setPolicyId(rs.getLong("policy_id"));
        assignment.setPolicyCode(rs.getString("policy_code"));
        assignment.setPolicyName(rs.getString("policy_name"));
        assignment.setResourceType(rs.getString("resource_type"));
        assignment.setEffect(rs.getString("effect"));
        assignment.setConditions(rs.getString("conditions"));
        assignment.setEnabled(rs.getString("enabled"));
        assignment.setCreatedBy(rs.getString("created_by"));
        Timestamp created = rs.getTimestamp("created_time");
        if (created != null) {
            assignment.setCreatedTime(created.toLocalDateTime());
        }
        assignment.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updated = rs.getTimestamp("updated_time");
        if (updated != null) {
            assignment.setUpdatedTime(updated.toLocalDateTime());
        }
        return assignment;
    }

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}
