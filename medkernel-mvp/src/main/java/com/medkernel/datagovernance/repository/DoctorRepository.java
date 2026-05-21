package com.medkernel.datagovernance.repository;

import com.medkernel.datagovernance.entity.DoctorEntity;
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
 * 医生主数据数据库访问层
 */
@Repository
public class DoctorRepository {
    private static final Logger log = LoggerFactory.getLogger(DoctorRepository.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public DoctorRepository(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 保存医生主数据（新增或更新）
     */
    public void save(DoctorEntity entity) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return;
        }

        if (entity.getId() == null) {
            entity.setId(Ids.next());
        }

        try (Connection connection = connection()) {
            Long existingId = findIdByUniqueKey(connection, entity.getTenantId(), entity.getDoctorId());
            if (existingId != null) {
                entity.setId(existingId);
                updateExisting(connection, entity);
            } else {
                insertNew(connection, entity);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save doctor failed: " + ex.getMessage(), ex);
        }
    }

    private Long findIdByUniqueKey(Connection connection, String tenantId, String doctorId) throws SQLException {
        String sql = "SELECT id FROM md_doctor WHERE tenant_id = ? AND doctor_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private void insertNew(Connection connection, DoctorEntity entity) throws SQLException {
        String sql = "INSERT INTO md_doctor " +
                "(id, tenant_id, doctor_id, doctor_name, gender, title, specialty_code, department_code, license_no, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, entity.getId());
            ps.setString(i++, entity.getTenantId());
            ps.setString(i++, entity.getDoctorId());
            ps.setString(i++, entity.getDoctorName());
            ps.setString(i++, entity.getGender());
            ps.setString(i++, entity.getTitle());
            ps.setString(i++, entity.getSpecialtyCode());
            ps.setString(i++, entity.getDepartmentCode());
            ps.setString(i++, entity.getLicenseNo());
            ps.setString(i++, entity.getStatus());
            ps.setTimestamp(i++, entity.getCreatedTime() != null ? Timestamp.valueOf(entity.getCreatedTime()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    private void updateExisting(Connection connection, DoctorEntity entity) throws SQLException {
        String sql = "UPDATE md_doctor SET " +
                "doctor_name = ?, gender = ?, title = ?, specialty_code = ?, department_code = ?, license_no = ?, " +
                "status = ?, updated_time = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, entity.getDoctorName());
            ps.setString(i++, entity.getGender());
            ps.setString(i++, entity.getTitle());
            ps.setString(i++, entity.getSpecialtyCode());
            ps.setString(i++, entity.getDepartmentCode());
            ps.setString(i++, entity.getLicenseNo());
            ps.setString(i++, entity.getStatus());
            ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(i++, entity.getId());
            ps.executeUpdate();
        }
    }

    /**
     * 根据租户ID和医生ID查找医生
     */
    public DoctorEntity findByDoctorId(String tenantId, String doctorId) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return null;
        }

        String sql = "SELECT * FROM md_doctor WHERE tenant_id = ? AND doctor_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find doctor failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据租户ID查找所有医生
     */
    public List<DoctorEntity> findAllByTenantId(String tenantId) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM md_doctor WHERE tenant_id = ? ORDER BY created_time DESC";
        List<DoctorEntity> result = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find doctors failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    private DoctorEntity mapRow(ResultSet rs) throws SQLException {
        DoctorEntity entity = new DoctorEntity();
        entity.setId(rs.getLong("id"));
        entity.setTenantId(rs.getString("tenant_id"));
        entity.setDoctorId(rs.getString("doctor_id"));
        entity.setDoctorName(rs.getString("doctor_name"));
        entity.setGender(rs.getString("gender"));
        entity.setTitle(rs.getString("title"));
        entity.setSpecialtyCode(rs.getString("specialty_code"));
        entity.setDepartmentCode(rs.getString("department_code"));
        entity.setLicenseNo(rs.getString("license_no"));
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