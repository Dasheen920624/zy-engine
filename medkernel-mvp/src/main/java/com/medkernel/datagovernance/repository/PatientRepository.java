package com.medkernel.datagovernance.repository;

import com.medkernel.datagovernance.entity.PatientEntity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 患者主数据数据库访问层
 */
@Repository
public class PatientRepository {
    private static final Logger log = LoggerFactory.getLogger(PatientRepository.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public PatientRepository(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 保存患者主数据（新增或更新）
     */
    public void save(PatientEntity entity) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return;
        }

        if (entity.getId() == null) {
            entity.setId(Ids.next());
        }

        try (Connection connection = connection()) {
            Long existingId = findIdByUniqueKey(connection, entity.getTenantId(), entity.getPatientId());
            if (existingId != null) {
                entity.setId(existingId);
                updateExisting(connection, entity);
            } else {
                insertNew(connection, entity);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save patient failed: " + ex.getMessage(), ex);
        }
    }

    private Long findIdByUniqueKey(Connection connection, String tenantId, String patientId) throws SQLException {
        String sql = "SELECT id FROM md_patient WHERE tenant_id = ? AND patient_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private void insertNew(Connection connection, PatientEntity entity) throws SQLException {
        String sql = "INSERT INTO md_patient " +
                "(id, tenant_id, patient_id, patient_name, gender, birth_date, id_card_no, phone, address, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, entity.getId());
            ps.setString(i++, entity.getTenantId());
            ps.setString(i++, entity.getPatientId());
            ps.setString(i++, entity.getPatientName());
            ps.setString(i++, entity.getGender());
            ps.setDate(i++, entity.getBirthDate() != null ? java.sql.Date.valueOf(entity.getBirthDate()) : null);
            ps.setString(i++, entity.getIdCardNo());
            ps.setString(i++, entity.getPhone());
            ps.setString(i++, entity.getAddress());
            ps.setString(i++, entity.getStatus());
            ps.setTimestamp(i++, entity.getCreatedTime() != null ? Timestamp.valueOf(entity.getCreatedTime()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    private void updateExisting(Connection connection, PatientEntity entity) throws SQLException {
        String sql = "UPDATE md_patient SET " +
                "patient_name = ?, gender = ?, birth_date = ?, id_card_no = ?, phone = ?, address = ?, " +
                "status = ?, updated_time = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, entity.getPatientName());
            ps.setString(i++, entity.getGender());
            ps.setDate(i++, entity.getBirthDate() != null ? java.sql.Date.valueOf(entity.getBirthDate()) : null);
            ps.setString(i++, entity.getIdCardNo());
            ps.setString(i++, entity.getPhone());
            ps.setString(i++, entity.getAddress());
            ps.setString(i++, entity.getStatus());
            ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(i++, entity.getId());
            ps.executeUpdate();
        }
    }

    /**
     * 根据租户ID和患者ID查找患者
     */
    public PatientEntity findByPatientId(String tenantId, String patientId) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return null;
        }

        String sql = "SELECT * FROM md_patient WHERE tenant_id = ? AND patient_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find patient failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据租户ID查找所有患者
     */
    public List<PatientEntity> findAllByTenantId(String tenantId) {
        if (!properties.isEnabled() || !properties.hasRequiredCredentials()) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM md_patient WHERE tenant_id = ? ORDER BY created_time DESC";
        List<PatientEntity> result = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find patients failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    private PatientEntity mapRow(ResultSet rs) throws SQLException {
        PatientEntity entity = new PatientEntity();
        entity.setId(rs.getLong("id"));
        entity.setTenantId(rs.getString("tenant_id"));
        entity.setPatientId(rs.getString("patient_id"));
        entity.setPatientName(rs.getString("patient_name"));
        entity.setGender(rs.getString("gender"));
        java.sql.Date birthDate = rs.getDate("birth_date");
        entity.setBirthDate(birthDate != null ? birthDate.toLocalDate() : null);
        entity.setIdCardNo(rs.getString("id_card_no"));
        entity.setPhone(rs.getString("phone"));
        entity.setAddress(rs.getString("address"));
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