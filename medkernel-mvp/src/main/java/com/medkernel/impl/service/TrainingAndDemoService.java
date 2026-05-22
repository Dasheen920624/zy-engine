package com.medkernel.impl.service;

import com.medkernel.common.TraceContext;
import com.medkernel.impl.entity.DemoDataPackage;
import com.medkernel.impl.entity.TrainingMaterial;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 培训与演示服务：培训材料 + 演示数据包 CRUD
 */
@Service
public class TrainingAndDemoService {

    private static final Logger log = LoggerFactory.getLogger(TrainingAndDemoService.class);

    private final DataSource dataSource;

    public TrainingAndDemoService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ==================== 培训材料管理 ====================

    public TrainingMaterial createMaterial(TrainingMaterial material) {
        String sql = "INSERT INTO impl_training_material (id, tenant_id, material_code, material_name, "
                + "description, material_type, category, content_path, version, duration_minutes, "
                + "target_audience, is_published, published_by, published_time, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            material.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, material.getTenantId());
            ps.setString(3, material.getMaterialCode());
            ps.setString(4, material.getMaterialName());
            ps.setString(5, material.getDescription());
            ps.setString(6, material.getMaterialType());
            ps.setString(7, material.getCategory());
            ps.setString(8, material.getContentPath());
            ps.setString(9, material.getVersion());
            if (material.getDurationMinutes() != null) {
                ps.setInt(10, material.getDurationMinutes());
            } else {
                ps.setNull(10, Types.INTEGER);
            }
            ps.setString(11, material.getTargetAudience());
            ps.setBoolean(12, material.getIsPublished() != null ? material.getIsPublished() : false);
            ps.setString(13, material.getPublishedBy());
            if (material.getPublishedTime() != null) {
                ps.setTimestamp(14, Timestamp.valueOf(material.getPublishedTime()));
            } else {
                ps.setNull(14, Types.TIMESTAMP);
            }
            ps.setString(15, TraceContext.getUsername());
            ps.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Created training material: id={}, code={}", id, material.getMaterialCode());
        } catch (SQLException ex) {
            throw new IllegalStateException("create training material failed: " + ex.getMessage(), ex);
        }
        return material;
    }

    public TrainingMaterial updateMaterial(TrainingMaterial material) {
        String sql = "UPDATE impl_training_material SET material_name = ?, description = ?, "
                + "material_type = ?, category = ?, content_path = ?, version = ?, "
                + "duration_minutes = ?, target_audience = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, material.getMaterialName());
            ps.setString(2, material.getDescription());
            ps.setString(3, material.getMaterialType());
            ps.setString(4, material.getCategory());
            ps.setString(5, material.getContentPath());
            ps.setString(6, material.getVersion());
            if (material.getDurationMinutes() != null) {
                ps.setInt(7, material.getDurationMinutes());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, material.getTargetAudience());
            ps.setString(9, TraceContext.getUsername());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(11, material.getId());
            ps.setLong(12, material.getTenantId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update training material failed: " + ex.getMessage(), ex);
        }
        return material;
    }

    public List<TrainingMaterial> listMaterials(Long tenantId, String category, Boolean isPublished) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, material_code, material_name, "
                + "description, material_type, category, content_path, version, duration_minutes, "
                + "target_audience, is_published, published_by, published_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM impl_training_material WHERE tenant_id = ?");
        List<Object> params = new ArrayList<>();
        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (isPublished != null) {
            sql.append(" AND is_published = ?");
            params.add(isPublished);
        }
        sql.append(" ORDER BY created_time DESC");

        List<TrainingMaterial> materials = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setLong(1, tenantId);
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    ps.setString(i + 2, (String) param);
                } else if (param instanceof Boolean) {
                    ps.setBoolean(i + 2, (Boolean) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    materials.add(mapMaterial(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list training materials failed: " + ex.getMessage(), ex);
        }
        return materials;
    }

    public TrainingMaterial getMaterial(Long materialId) {
        String sql = "SELECT id, tenant_id, material_code, material_name, "
                + "description, material_type, category, content_path, version, duration_minutes, "
                + "target_audience, is_published, published_by, published_time, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM impl_training_material WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, materialId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMaterial(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get training material failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    public TrainingMaterial publishMaterial(Long materialId, Long tenantId) {
        String sql = "UPDATE impl_training_material SET is_published = TRUE, "
                + "published_by = ?, published_time = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String username = TraceContext.getUsername();
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, username);
            ps.setTimestamp(2, Timestamp.valueOf(now));
            ps.setString(3, username);
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setLong(5, materialId);
            ps.setLong(6, tenantId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("publish training material failed: " + ex.getMessage(), ex);
        }
        return getMaterial(materialId);
    }

    // ==================== 演示数据包管理 ====================

    public DemoDataPackage createDemoDataPackage(DemoDataPackage pkg) {
        String sql = "INSERT INTO impl_demo_data_package (id, tenant_id, package_code, package_name, "
                + "description, data_scope, data_version, record_count, artifact_path, artifact_hash, "
                + "status, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            Long id = Ids.next();
            pkg.setId(id);
            ps.setLong(1, id);
            ps.setLong(2, pkg.getTenantId());
            ps.setString(3, pkg.getPackageCode());
            ps.setString(4, pkg.getPackageName());
            ps.setString(5, pkg.getDescription());
            ps.setString(6, pkg.getDataScope());
            ps.setString(7, pkg.getDataVersion());
            ps.setLong(8, pkg.getRecordCount() != null ? pkg.getRecordCount() : 0L);
            ps.setString(9, pkg.getArtifactPath());
            ps.setString(10, pkg.getArtifactHash());
            ps.setString(11, pkg.getStatus() != null ? pkg.getStatus() : "DRAFT");
            ps.setString(12, TraceContext.getUsername());
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            log.info("Created demo data package: id={}, code={}", id, pkg.getPackageCode());
        } catch (SQLException ex) {
            throw new IllegalStateException("create demo data package failed: " + ex.getMessage(), ex);
        }
        return pkg;
    }

    public DemoDataPackage updateDemoDataPackage(DemoDataPackage pkg) {
        String sql = "UPDATE impl_demo_data_package SET package_name = ?, description = ?, "
                + "data_scope = ?, data_version = ?, record_count = ?, artifact_path = ?, "
                + "artifact_hash = ?, status = ?, updated_by = ?, updated_time = ? "
                + "WHERE id = ? AND tenant_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pkg.getPackageName());
            ps.setString(2, pkg.getDescription());
            ps.setString(3, pkg.getDataScope());
            ps.setString(4, pkg.getDataVersion());
            ps.setLong(5, pkg.getRecordCount() != null ? pkg.getRecordCount() : 0L);
            ps.setString(6, pkg.getArtifactPath());
            ps.setString(7, pkg.getArtifactHash());
            ps.setString(8, pkg.getStatus());
            ps.setString(9, TraceContext.getUsername());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(11, pkg.getId());
            ps.setLong(12, pkg.getTenantId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update demo data package failed: " + ex.getMessage(), ex);
        }
        return pkg;
    }

    public List<DemoDataPackage> listDemoDataPackages(Long tenantId, String status) {
        StringBuilder sql = new StringBuilder("SELECT id, tenant_id, package_code, package_name, "
                + "description, data_scope, data_version, record_count, artifact_path, artifact_hash, "
                + "status, created_by, created_time, updated_by, updated_time "
                + "FROM impl_demo_data_package WHERE tenant_id = ?");
        List<String> params = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<DemoDataPackage> packages = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setLong(1, tenantId);
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 2, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    packages.add(mapDemoDataPackage(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list demo data packages failed: " + ex.getMessage(), ex);
        }
        return packages;
    }

    public DemoDataPackage getDemoDataPackage(Long packageId) {
        String sql = "SELECT id, tenant_id, package_code, package_name, "
                + "description, data_scope, data_version, record_count, artifact_path, artifact_hash, "
                + "status, created_by, created_time, updated_by, updated_time "
                + "FROM impl_demo_data_package WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, packageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapDemoDataPackage(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get demo data package failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ==================== ResultSet 映射 ====================

    private TrainingMaterial mapMaterial(ResultSet rs) throws SQLException {
        TrainingMaterial m = new TrainingMaterial();
        m.setId(rs.getLong("id"));
        m.setTenantId(rs.getLong("tenant_id"));
        m.setMaterialCode(rs.getString("material_code"));
        m.setMaterialName(rs.getString("material_name"));
        m.setDescription(rs.getString("description"));
        m.setMaterialType(rs.getString("material_type"));
        m.setCategory(rs.getString("category"));
        m.setContentPath(rs.getString("content_path"));
        m.setVersion(rs.getString("version"));
        m.setDurationMinutes(rs.getInt("duration_minutes"));
        m.setTargetAudience(rs.getString("target_audience"));
        m.setIsPublished(rs.getBoolean("is_published"));
        m.setPublishedBy(rs.getString("published_by"));
        Timestamp publishedTime = rs.getTimestamp("published_time");
        if (publishedTime != null) {
            m.setPublishedTime(publishedTime.toLocalDateTime());
        }
        m.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            m.setCreatedTime(createdTime.toLocalDateTime());
        }
        m.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            m.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return m;
    }

    private DemoDataPackage mapDemoDataPackage(ResultSet rs) throws SQLException {
        DemoDataPackage p = new DemoDataPackage();
        p.setId(rs.getLong("id"));
        p.setTenantId(rs.getLong("tenant_id"));
        p.setPackageCode(rs.getString("package_code"));
        p.setPackageName(rs.getString("package_name"));
        p.setDescription(rs.getString("description"));
        p.setDataScope(rs.getString("data_scope"));
        p.setDataVersion(rs.getString("data_version"));
        p.setRecordCount(rs.getLong("record_count"));
        p.setArtifactPath(rs.getString("artifact_path"));
        p.setArtifactHash(rs.getString("artifact_hash"));
        p.setStatus(rs.getString("status"));
        p.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            p.setCreatedTime(createdTime.toLocalDateTime());
        }
        p.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            p.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return p;
    }

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}
