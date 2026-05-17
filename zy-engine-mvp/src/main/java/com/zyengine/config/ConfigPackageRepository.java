package com.zyengine.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyengine.persistence.EnginePersistenceProperties;
import com.zyengine.persistence.Ids;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置包数据库访问层
 */
@Repository
public class ConfigPackageRepository {
    private final EnginePersistenceProperties properties;
    private final ObjectMapper objectMapper;

    public ConfigPackageRepository(EnginePersistenceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存配置包（新增或更新）
     */
    public void save(ConfigPackageEntity entity) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return;
        }

        if (entity.getId() == null) {
            entity.setId(Ids.next());
        }

        String sql = "INSERT INTO cfg_config_package " +
                "(id, tenant_id, package_code, package_version, asset_type, scope_level, scope_code, " +
                "status, base_version, target_version, content_hash, declared_content_hash, " +
                "manifest_json, diff_json, full_snapshot_json, created_by, reviewed_by, approved_by, " +
                "created_time, reviewed_time, published_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "status = VALUES(status), " +
                "base_version = VALUES(base_version), " +
                "target_version = VALUES(target_version), " +
                "content_hash = VALUES(content_hash), " +
                "declared_content_hash = VALUES(declared_content_hash), " +
                "manifest_json = VALUES(manifest_json), " +
                "diff_json = VALUES(diff_json), " +
                "full_snapshot_json = VALUES(full_snapshot_json), " +
                "reviewed_by = VALUES(reviewed_by), " +
                "approved_by = VALUES(approved_by), " +
                "reviewed_time = VALUES(reviewed_time), " +
                "published_time = VALUES(published_time)";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, entity.getId());
            ps.setString(i++, entity.getTenantId());
            ps.setString(i++, entity.getPackageCode());
            ps.setString(i++, entity.getPackageVersion());
            ps.setString(i++, entity.getAssetType());
            ps.setString(i++, entity.getScopeLevel());
            ps.setString(i++, entity.getScopeCode());
            ps.setString(i++, entity.getStatus());
            ps.setString(i++, entity.getBaseVersion());
            ps.setString(i++, entity.getTargetVersion());
            ps.setString(i++, entity.getContentHash());
            ps.setString(i++, entity.getDeclaredContentHash());
            ps.setString(i++, entity.getManifestJson());
            ps.setString(i++, entity.getDiffJson());
            ps.setString(i++, entity.getFullSnapshotJson());
            ps.setString(i++, entity.getCreatedBy());
            ps.setString(i++, entity.getReviewedBy());
            ps.setString(i++, entity.getApprovedBy());
            ps.setTimestamp(i++, toTimestamp(entity.getCreatedTime()));
            ps.setTimestamp(i++, toTimestamp(entity.getReviewedTime()));
            ps.setTimestamp(i++, toTimestamp(entity.getPublishedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save config package failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 根据唯一键查找配置包
     */
    public ConfigPackageEntity findByUniqueKey(String tenantId, String packageCode, String packageVersion,
                                               String assetType, String scopeLevel, String scopeCode) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return null;
        }

        String sql = "SELECT * FROM cfg_config_package " +
                "WHERE tenant_id = ? AND package_code = ? AND package_version = ? " +
                "AND asset_type = ? AND scope_level = ? AND scope_code = ?";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, packageCode);
            ps.setString(i++, packageVersion);
            ps.setString(i++, assetType);
            ps.setString(i++, scopeLevel);
            ps.setString(i++, scopeCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find config package failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据ID查找配置包
     */
    public ConfigPackageEntity findById(Long id) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return null;
        }

        String sql = "SELECT * FROM cfg_config_package WHERE id = ?";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find config package by id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 查找配置包列表（支持过滤）
     */
    public List<ConfigPackageEntity> findList(String tenantId, String packageCode, String assetType,
                                               String status, String scopeLevel, String scopeCode) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM cfg_config_package WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (tenantId != null) {
            sql.append(" AND tenant_id = ?");
            params.add(tenantId);
        }
        if (packageCode != null) {
            sql.append(" AND package_code = ?");
            params.add(packageCode);
        }
        if (assetType != null) {
            sql.append(" AND asset_type = ?");
            params.add(assetType);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (scopeLevel != null) {
            sql.append(" AND scope_level = ?");
            params.add(scopeLevel);
        }
        if (scopeCode != null) {
            sql.append(" AND scope_code = ?");
            params.add(scopeCode);
        }

        sql.append(" ORDER BY package_code, package_version");

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            List<ConfigPackageEntity> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSet(rs));
                }
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("find config package list failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 删除配置包
     */
    public void delete(Long id) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return;
        }

        String sql = "DELETE FROM cfg_config_package WHERE id = ?";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("delete config package failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 检查配置包是否存在
     */
    public boolean exists(String tenantId, String packageCode, String packageVersion,
                          String assetType, String scopeLevel, String scopeCode) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM cfg_config_package " +
                "WHERE tenant_id = ? AND package_code = ? AND package_version = ? " +
                "AND asset_type = ? AND scope_level = ? AND scope_code = ?";

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, packageCode);
            ps.setString(i++, packageVersion);
            ps.setString(i++, assetType);
            ps.setString(i++, scopeLevel);
            ps.setString(i++, scopeCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("check config package exists failed: " + ex.getMessage(), ex);
        }
        return false;
    }

    private Connection connection() throws SQLException {
        // PKG-004 历史调用 properties.getConnection() 实际不存在，REVIEW-FIX-001 改为与项目其他 Repository 一致的 DriverManager 直连。
        String driverClass = properties.localFileDatabase() ? "org.h2.Driver" : "oracle.jdbc.OracleDriver";
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException ex) {
            throw new SQLException(driverClass + " not found", ex);
        }
        return DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword());
    }

    private ConfigPackageEntity mapResultSet(ResultSet rs) throws SQLException {
        ConfigPackageEntity entity = new ConfigPackageEntity();
        entity.setId(rs.getLong("id"));
        entity.setTenantId(rs.getString("tenant_id"));
        entity.setPackageCode(rs.getString("package_code"));
        entity.setPackageVersion(rs.getString("package_version"));
        entity.setAssetType(rs.getString("asset_type"));
        entity.setScopeLevel(rs.getString("scope_level"));
        entity.setScopeCode(rs.getString("scope_code"));
        entity.setStatus(rs.getString("status"));
        entity.setBaseVersion(rs.getString("base_version"));
        entity.setTargetVersion(rs.getString("target_version"));
        entity.setContentHash(rs.getString("content_hash"));
        entity.setDeclaredContentHash(rs.getString("declared_content_hash"));
        entity.setManifestJson(rs.getString("manifest_json"));
        entity.setDiffJson(rs.getString("diff_json"));
        entity.setFullSnapshotJson(rs.getString("full_snapshot_json"));
        entity.setCreatedBy(rs.getString("created_by"));
        entity.setReviewedBy(rs.getString("reviewed_by"));
        entity.setApprovedBy(rs.getString("approved_by"));
        entity.setCreatedTime(toLocalDateTime(rs.getTimestamp("created_time")));
        entity.setReviewedTime(toLocalDateTime(rs.getTimestamp("reviewed_time")));
        entity.setPublishedTime(toLocalDateTime(rs.getTimestamp("published_time")));
        return entity;
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Timestamp.from(dateTime.toInstant(ZoneOffset.UTC));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }
}