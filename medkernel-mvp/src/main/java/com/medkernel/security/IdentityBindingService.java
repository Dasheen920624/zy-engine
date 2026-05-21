package com.medkernel.security;

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
 * 身份绑定管理服务：多身份源绑定、合并和解绑。
 * 支持同一用户绑定多个身份源，重复外部账号阻断，人工合并，解绑保留审计。
 */
@Service
public class IdentityBindingService {

    private static final Logger log = LoggerFactory.getLogger(IdentityBindingService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public IdentityBindingService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 查询用户的所有身份绑定。
     */
    public List<IdentityBinding> listBindingsByUser(Long tenantId, Long userId) {
        String sql = "SELECT id, tenant_id, platform_user_id AS user_id, source_id AS provider_id, "
                + "external_id AS external_subject, external_username AS external_org_code, "
                + "external_display_name, binding_status, "
                + "last_sync_time AS last_verified_time, created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_binding WHERE tenant_id = ? AND platform_user_id = ? ORDER BY created_time";
        List<IdentityBinding> bindings = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bindings.add(mapBinding(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询身份绑定失败: " + ex.getMessage(), ex);
        }
        return bindings;
    }

    /**
     * 绑定外部身份到平台用户。
     * 如果同一 provider + external_subject 已绑定到其他用户，则阻断。
     */
    public IdentityBinding bindIdentity(Long tenantId, Long userId, Long providerId,
                                         String externalSubject, String externalDisplayName,
                                         String operator) {
        // 检查重复绑定
        IdentityBinding existing = findActiveBinding(tenantId, providerId, externalSubject);
        if (existing != null) {
            if (existing.getUserId().equals(userId)) {
                throw new IllegalStateException("该外部身份已绑定到当前用户");
            }
            throw new IllegalStateException("该外部身份已绑定到其他用户(userId=" + existing.getUserId() + ")，请使用合并功能");
        }

        // 检查同一用户是否已绑定同一身份源
        List<IdentityBinding> userBindings = listBindingsByUser(tenantId, userId);
        for (IdentityBinding b : userBindings) {
            if (b.getProviderId().equals(providerId) && b.getExternalSubject().equals(externalSubject)) {
                throw new IllegalStateException("当前用户已绑定该外部身份");
            }
        }

        long bindingId = Ids.next();
        String sql = "INSERT INTO sec_identity_binding (id, tenant_id, platform_user_id, source_id, "
                + "external_id, external_username, external_display_name, binding_status, last_sync_time, "
                + "created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bindingId);
            ps.setLong(2, tenantId);
            ps.setLong(3, userId);
            ps.setLong(4, providerId);
            ps.setString(5, externalSubject);
            ps.setString(6, externalSubject);
            ps.setString(7, externalDisplayName);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(9, operator);
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建身份绑定失败: " + ex.getMessage(), ex);
        }

        IdentityBinding binding = new IdentityBinding();
        binding.setId(bindingId);
        binding.setTenantId(tenantId);
        binding.setUserId(userId);
        binding.setProviderId(providerId);
        binding.setExternalSubject(externalSubject);
        binding.setExternalOrgCode(externalSubject);
        binding.setExternalDisplayName(externalDisplayName);
        binding.setBindingStatus("ACTIVE");
        binding.setCreatedBy(operator);
        binding.setCreatedTime(LocalDateTime.now());
        return binding;
    }

    /**
     * 解绑：标记 binding_status=DETACHED，保留历史审计。
     */
    public void unbindIdentity(Long bindingId, String operator) {
        IdentityBinding binding = getBindingById(bindingId);
        if (binding == null) {
            throw new IllegalArgumentException("绑定不存在: " + bindingId);
        }
        String sql = "UPDATE sec_identity_binding SET binding_status = 'DETACHED', "
                + "updated_by = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, operator);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, bindingId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new IllegalArgumentException("绑定不存在: " + bindingId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("解绑失败: " + ex.getMessage(), ex);
        }
        recordUnbind(binding, operator);
    }

    /**
     * 合并绑定：将源用户的所有绑定转移到目标用户，源用户标记为 MERGED。
     */
    public Map<String, Object> mergeBindings(Long tenantId, Long sourceUserId, Long targetUserId,
                                             String mergeReason, String operator) {
        if (sourceUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("源用户和目标用户不能相同");
        }

        Map<String, Object> result = new HashMap<String, Object>();
        int transferredCount = 0;
        int conflictCount = 0;

        // 获取源用户的所有活跃绑定
        List<IdentityBinding> sourceBindings = listBindingsByUser(tenantId, sourceUserId);
        List<IdentityBinding> targetBindings = listBindingsByUser(tenantId, targetUserId);

        for (IdentityBinding sourceBinding : sourceBindings) {
            if (!"ACTIVE".equals(sourceBinding.getBindingStatus())) continue;

            // 检查目标用户是否已有相同 provider + subject 的绑定
            boolean conflict = false;
            for (IdentityBinding targetBinding : targetBindings) {
                if (targetBinding.getProviderId().equals(sourceBinding.getProviderId())
                        && targetBinding.getExternalSubject().equals(sourceBinding.getExternalSubject())) {
                    conflict = true;
                    break;
                }
            }

            if (conflict) {
                // 冲突：标记源绑定为 DETACHED
                unbindIdentity(sourceBinding.getId(), operator);
                conflictCount++;
            } else {
                // 转移：更新平台用户为目标用户
                String updateSql = "UPDATE sec_identity_binding SET platform_user_id = ?, updated_by = ?, updated_time = ? WHERE id = ?";
                try (Connection connection = connection();
                     PreparedStatement ps = connection.prepareStatement(updateSql)) {
                    ps.setLong(1, targetUserId);
                    ps.setString(2, operator);
                    ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setLong(4, sourceBinding.getId());
                    ps.executeUpdate();
                    transferredCount++;
                } catch (SQLException ex) {
                    log.error("转移绑定失败: bindingId={}", sourceBinding.getId(), ex);
                    conflictCount++;
                }
            }
        }

        // 标记源用户为 MERGED
        String updateUserSql = "UPDATE sec_user SET status = 'MERGED', updated_by = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(updateUserSql)) {
            ps.setString(1, operator);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, sourceUserId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("标记源用户为MERGED失败: userId={}", sourceUserId, ex);
        }
        recordMerge(tenantId, sourceUserId, targetUserId, mergeReason, operator);

        result.put("transferredCount", transferredCount);
        result.put("conflictCount", conflictCount);
        result.put("sourceUserId", sourceUserId);
        result.put("targetUserId", targetUserId);
        return result;
    }

    /**
     * 查找冲突绑定：同一外部身份绑定到多个用户。
     */
    public List<Map<String, Object>> findConflicts(Long tenantId) {
        String sql = "SELECT source_id AS provider_id, external_id AS external_subject, COUNT(DISTINCT platform_user_id) as user_count "
                + "FROM sec_identity_binding WHERE tenant_id = ? AND binding_status = 'ACTIVE' "
                + "GROUP BY source_id, external_id HAVING COUNT(DISTINCT platform_user_id) > 1";
        List<Map<String, Object>> conflicts = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<String, Object>();
                    row.put("providerId", rs.getLong("provider_id"));
                    row.put("externalSubject", rs.getString("external_subject"));
                    row.put("userCount", rs.getInt("user_count"));
                    conflicts.add(row);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询冲突绑定失败: " + ex.getMessage(), ex);
        }
        return conflicts;
    }

    public IdentityBinding getBindingById(Long bindingId) {
        String sql = "SELECT id, tenant_id, platform_user_id AS user_id, source_id AS provider_id, "
                + "external_id AS external_subject, external_username AS external_org_code, "
                + "external_display_name, binding_status, last_sync_time AS last_verified_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_binding WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bindingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBinding(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询身份绑定详情失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    public List<Map<String, Object>> listMergeRecordsByUser(Long tenantId, Long userId) {
        String sql = "SELECT id, tenant_id, source_user_id, target_user_id, merge_reason, merge_status, "
                + "merged_by, merged_at, created_time FROM sec_user_merge "
                + "WHERE tenant_id = ? AND (source_user_id = ? OR target_user_id = ?) ORDER BY created_time DESC";
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<String, Object>();
                    row.put("id", rs.getLong("id"));
                    row.put("tenant_id", rs.getLong("tenant_id"));
                    row.put("source_user_id", rs.getLong("source_user_id"));
                    row.put("target_user_id", rs.getLong("target_user_id"));
                    row.put("merge_reason", rs.getString("merge_reason"));
                    row.put("merge_status", rs.getString("merge_status"));
                    row.put("merged_by", rs.getString("merged_by"));
                    row.put("merged_at", rs.getTimestamp("merged_at"));
                    row.put("created_time", rs.getTimestamp("created_time"));
                    records.add(row);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询用户合并记录失败: " + ex.getMessage(), ex);
        }
        return records;
    }

    public List<Map<String, Object>> listUnbindRecordsByUser(Long tenantId, Long userId) {
        String sql = "SELECT id, tenant_id, binding_id, user_id, unbind_reason, unbind_status, "
                + "previous_status, new_status, unbound_by, unbound_at, created_time FROM sec_user_unbind "
                + "WHERE tenant_id = ? AND user_id = ? ORDER BY created_time DESC";
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<String, Object>();
                    row.put("id", rs.getLong("id"));
                    row.put("tenant_id", rs.getLong("tenant_id"));
                    row.put("binding_id", rs.getLong("binding_id"));
                    row.put("user_id", rs.getLong("user_id"));
                    row.put("unbind_reason", rs.getString("unbind_reason"));
                    row.put("unbind_status", rs.getString("unbind_status"));
                    row.put("previous_status", rs.getString("previous_status"));
                    row.put("new_status", rs.getString("new_status"));
                    row.put("unbound_by", rs.getString("unbound_by"));
                    row.put("unbound_at", rs.getTimestamp("unbound_at"));
                    row.put("created_time", rs.getTimestamp("created_time"));
                    records.add(row);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询用户解绑记录失败: " + ex.getMessage(), ex);
        }
        return records;
    }

    // ---- 内部方法 ----

    private IdentityBinding findActiveBinding(Long tenantId, Long providerId, String externalSubject) {
        String sql = "SELECT id, tenant_id, platform_user_id AS user_id, source_id AS provider_id, "
                + "external_id AS external_subject, external_username AS external_org_code, "
                + "external_display_name, binding_status, last_sync_time AS last_verified_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_binding "
                + "WHERE tenant_id = ? AND source_id = ? AND external_id = ? AND binding_status = 'ACTIVE'";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, providerId);
            ps.setString(3, externalSubject);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBinding(rs);
                }
            }
        } catch (SQLException ex) {
            log.error("查询活跃绑定失败", ex);
        }
        return null;
    }

    private void recordMerge(Long tenantId, Long sourceUserId, Long targetUserId, String mergeReason, String operator) {
        String sql = "INSERT INTO sec_user_merge (id, tenant_id, source_user_id, target_user_id, merge_reason, "
                + "merge_status, merged_by, merged_at, created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Ids.next());
            ps.setLong(2, tenantId);
            ps.setLong(3, sourceUserId);
            ps.setLong(4, targetUserId);
            ps.setString(5, mergeReason);
            ps.setString(6, "COMPLETED");
            ps.setString(7, operator);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(9, operator);
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("record merge history failed: {}", ex.getMessage());
        }
    }

    private void recordUnbind(IdentityBinding binding, String operator) {
        String sql = "INSERT INTO sec_user_unbind (id, tenant_id, binding_id, user_id, unbind_reason, "
                + "unbind_status, previous_status, new_status, unbound_by, unbound_at, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Ids.next());
            ps.setLong(2, binding.getTenantId());
            ps.setLong(3, binding.getId());
            ps.setLong(4, binding.getUserId());
            ps.setString(5, "manual unbind");
            ps.setString(6, "COMPLETED");
            ps.setString(7, binding.getBindingStatus());
            ps.setString(8, "DETACHED");
            ps.setString(9, operator);
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(11, operator);
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("record unbind history failed: {}", ex.getMessage());
        }
    }

    private IdentityBinding mapBinding(ResultSet rs) throws SQLException {
        IdentityBinding binding = new IdentityBinding();
        binding.setId(rs.getLong("id"));
        binding.setTenantId(rs.getLong("tenant_id"));
        binding.setUserId(rs.getLong("user_id"));
        binding.setProviderId(rs.getLong("provider_id"));
        binding.setExternalSubject(rs.getString("external_subject"));
        binding.setExternalOrgCode(rs.getString("external_org_code"));
        binding.setExternalDisplayName(rs.getString("external_display_name"));
        binding.setBindingStatus(rs.getString("binding_status"));
        Timestamp lastVerified = rs.getTimestamp("last_verified_time");
        if (lastVerified != null) binding.setLastVerifiedTime(lastVerified.toLocalDateTime());
        binding.setCreatedBy(rs.getString("created_by"));
        Timestamp created = rs.getTimestamp("created_time");
        if (created != null) binding.setCreatedTime(created.toLocalDateTime());
        binding.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updated = rs.getTimestamp("updated_time");
        if (updated != null) binding.setUpdatedTime(updated.toLocalDateTime());
        return binding;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15b: 璧?HikariCP 杩炴帴姹狅紙EngineDataSourceConfig 鏆撮湶鐨?DataSource锛夈€?        return dataSource.getConnection();
    }
}
