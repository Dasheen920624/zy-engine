package com.medkernel.patient;

import com.medkernel.persistence.Ids;
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

/**
 * mpi_visit_identity 表 CRUD 仓库。
 */
@Repository
public class VisitIdentityRepository {

    private final DataSource dataSource;

    public VisitIdentityRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 保存就诊标识映射。
     */
    public VisitIdentity save(VisitIdentity identity) {
        if (identity.getId() == null) {
            identity.setId(Ids.next());
        }
        if (identity.getExternalId() != null && identity.getIdHash() == null) {
            identity.setIdHash(MpiHashUtil.hashId(identity.getExternalId()));
        }
        if (identity.getCreatedTime() == null) {
            identity.setCreatedTime(LocalDateTime.now());
        }
        identity.setUpdatedTime(LocalDateTime.now());

        try (Connection connection = connection()) {
            Long existingId = findVisitIdentityIdByUniqueKey(connection,
                    identity.getTenantId(), identity.getIdentityType(),
                    identity.getSourceSystem(), identity.getExternalId());
            if (existingId != null) {
                identity.setId(existingId);
                updateVisitIdentity(connection, identity);
            } else {
                insertVisitIdentity(connection, identity);
            }
            return identity;
        } catch (SQLException ex) {
            throw new IllegalStateException("save visit identity failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 根据ID查找就诊标识。
     */
    public VisitIdentity findById(Long id) {
        String sql = "SELECT * FROM mpi_visit_identity WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapVisitIdentity(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find visit identity by id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据平台就诊ID查找所有标识。
     */
    public List<VisitIdentity> findByPlatformId(String tenantId, String platformVisitId) {
        String sql = "SELECT * FROM mpi_visit_identity WHERE tenant_id = ? AND platform_visit_id = ? ORDER BY created_time";
        List<VisitIdentity> identities = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, platformVisitId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    identities.add(mapVisitIdentity(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find visit identities by platform id failed: " + ex.getMessage(), ex);
        }
        return identities;
    }

    /**
     * 根据平台患者ID查找所有就诊标识。
     */
    public List<VisitIdentity> findByPatientId(String tenantId, String platformPatientId) {
        String sql = "SELECT * FROM mpi_visit_identity WHERE tenant_id = ? AND platform_patient_id = ? ORDER BY visit_date DESC, created_time";
        List<VisitIdentity> identities = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, platformPatientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    identities.add(mapVisitIdentity(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find visit identities by patient id failed: " + ex.getMessage(), ex);
        }
        return identities;
    }

    /**
     * 根据外部标识查找就诊标识。
     */
    public VisitIdentity findByExternalId(String tenantId, String identityType, String sourceSystem, String externalId) {
        String sql = "SELECT * FROM mpi_visit_identity WHERE tenant_id = ? AND identity_type = ? AND source_system = ? AND external_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, identityType);
            ps.setString(3, sourceSystem);
            ps.setString(4, externalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapVisitIdentity(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find visit identity by external id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    // ============================================================================
    // 私有方法
    // ============================================================================

    private Long findVisitIdentityIdByUniqueKey(Connection connection, String tenantId,
                                                String identityType, String sourceSystem,
                                                String externalId) throws SQLException {
        String sql = "SELECT id FROM mpi_visit_identity "
                + "WHERE tenant_id = ? AND identity_type = ? AND source_system = ? AND external_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, identityType);
            ps.setString(3, sourceSystem);
            ps.setString(4, externalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    private void insertVisitIdentity(Connection connection, VisitIdentity identity) throws SQLException {
        String sql = "INSERT INTO mpi_visit_identity (id, tenant_id, platform_visit_id, platform_patient_id, "
                + "visit_type, identity_type, external_id, id_hash, source_system, visit_date, "
                + "department_code, status, remarks, created_time, updated_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, identity.getId());
            ps.setString(2, identity.getTenantId());
            ps.setString(3, identity.getPlatformVisitId());
            ps.setString(4, identity.getPlatformPatientId());
            ps.setString(5, identity.getVisitType());
            ps.setString(6, identity.getIdentityType());
            ps.setString(7, identity.getExternalId());
            ps.setString(8, identity.getIdHash());
            ps.setString(9, identity.getSourceSystem());
            ps.setObject(10, identity.getVisitDate() != null ? java.sql.Date.valueOf(identity.getVisitDate()) : null);
            ps.setString(11, identity.getDepartmentCode());
            ps.setString(12, identity.getStatus());
            ps.setString(13, identity.getRemarks());
            ps.setTimestamp(14, Timestamp.valueOf(identity.getCreatedTime()));
            ps.setTimestamp(15, Timestamp.valueOf(identity.getUpdatedTime()));
            ps.executeUpdate();
        }
    }

    private void updateVisitIdentity(Connection connection, VisitIdentity identity) throws SQLException {
        String sql = "UPDATE mpi_visit_identity SET platform_patient_id = ?, status = ?, "
                + "visit_date = ?, department_code = ?, remarks = ?, updated_time = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, identity.getPlatformPatientId());
            ps.setString(2, identity.getStatus());
            ps.setObject(3, identity.getVisitDate() != null ? java.sql.Date.valueOf(identity.getVisitDate()) : null);
            ps.setString(4, identity.getDepartmentCode());
            ps.setString(5, identity.getRemarks());
            ps.setTimestamp(6, Timestamp.valueOf(identity.getUpdatedTime()));
            ps.setLong(7, identity.getId());
            ps.executeUpdate();
        }
    }

    private VisitIdentity mapVisitIdentity(ResultSet rs) throws SQLException {
        VisitIdentity identity = new VisitIdentity();
        identity.setId(rs.getLong("id"));
        identity.setTenantId(rs.getString("tenant_id"));
        identity.setPlatformVisitId(rs.getString("platform_visit_id"));
        identity.setPlatformPatientId(rs.getString("platform_patient_id"));
        identity.setVisitType(rs.getString("visit_type"));
        identity.setIdentityType(rs.getString("identity_type"));
        identity.setExternalId(rs.getString("external_id"));
        identity.setIdHash(rs.getString("id_hash"));
        identity.setSourceSystem(rs.getString("source_system"));
        java.sql.Date visitDate = rs.getDate("visit_date");
        identity.setVisitDate(visitDate != null ? visitDate.toLocalDate() : null);
        identity.setDepartmentCode(rs.getString("department_code"));
        identity.setStatus(rs.getString("status"));
        identity.setRemarks(rs.getString("remarks"));
        identity.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        identity.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return identity;
    }

    private Connection connection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            throw new IllegalStateException("get connection failed: " + ex.getMessage(), ex);
        }
    }
}
