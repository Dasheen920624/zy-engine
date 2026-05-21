package com.medkernel.datagovernance.repository;

import com.medkernel.datagovernance.entity.DepartmentEntity;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 科室主数据数据库访问层
 */
@Repository
public class DepartmentRepository {
    private static final Logger log = LoggerFactory.getLogger(DepartmentRepository.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public DepartmentRepository(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 保存科室主数据（新增或更新）
     */
    public void save(DepartmentEntity entity) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return;
        }

        if (entity.getId() == null) {
            entity.setId(Ids.next());
        }

        try (Connection connection = connection()) {
            Long existingId = findIdByUniqueKey(connection, entity.getTenantId(), entity.getDeptCode());
            if (existingId != null) {
                entity.setId(existingId);
                updateExisting(connection, entity);
            } else {
                insertNew(connection, entity);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save department failed: " + ex.getMessage(), ex);
        }
    }

    private Long findIdByUniqueKey(Connection connection, String tenantId, String deptCode) throws SQLException {
        String sql = "SELECT id FROM md_department WHERE tenant_id = ? AND dept_code = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, deptCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private void insertNew(Connection connection, DepartmentEntity entity) throws SQLException {
        String sql = "INSERT INTO md_department " +
                "(id, tenant_id, dept_code, dept_name, dept_type, parent_dept_code, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, entity.getId());
            ps.setString(i++, entity.getTenantId());
            ps.setString(i++, entity.getDeptCode());
            ps.setString(i++, entity.getDeptName());
            ps.setString(i++, entity.getDeptType());
            ps.setString(i++, entity.getParentDeptCode());
            ps.setString(i++, entity.getStatus());
            ps.setTimestamp(i++, entity.getCreatedTime() != null ? Timestamp.valueOf(entity.getCreatedTime()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    private void updateExisting(Connection connection, DepartmentEntity entity) throws SQLException {
        String sql = "UPDATE md_department SET " +
                "dept_name = ?, dept_type = ?, parent_dept_code = ?, status = ?, updated_time = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, entity.getDeptName());
            ps.setString(i++, entity.getDeptType());
            ps.setString(i++, entity.getParentDeptCode());
            ps.setString(i++, entity.getStatus());
            ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(i++, entity.getId());
            ps.executeUpdate();
        }
    }

    /**
     * 根据租户ID和科室编码查找科室
     */
    public DepartmentEntity findByDeptCode(String tenantId, String deptCode) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return null;
        }

        String sql = "SELECT * FROM md_department WHERE tenant_id = ? AND dept_code = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, deptCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find department failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据租户ID查找所有科室
     */
    public List<DepartmentEntity> findAllByTenantId(String tenantId) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM md_department WHERE tenant_id = ? ORDER BY created_time DESC";
        List<DepartmentEntity> result = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find departments failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    private DepartmentEntity mapRow(ResultSet rs) throws SQLException {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setId(rs.getLong("id"));
        entity.setTenantId(rs.getString("tenant_id"));
        entity.setDeptCode(rs.getString("dept_code"));
        entity.setDeptName(rs.getString("dept_name"));
        entity.setDeptType(rs.getString("dept_type"));
        entity.setParentDeptCode(rs.getString("parent_dept_code"));
        entity.setStatus(rs.getString("status"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        entity.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        entity.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return entity;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15b: 走 HikariCP 连接池（EngineDataSourceConfig 暴露的 DataSource）。
        return dataSource.getConnection();
    }
}