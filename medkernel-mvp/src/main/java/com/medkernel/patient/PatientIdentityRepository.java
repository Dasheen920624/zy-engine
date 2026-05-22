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
 * mpi_patient_identity 表 CRUD 仓库。
 */
@Repository
public class PatientIdentityRepository {

    private final DataSource dataSource;

    public PatientIdentityRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 保存患者标识映射。
     */
    public PatientIdentity save(PatientIdentity identity) {
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
            Long existingId = findPatientIdentityIdByUniqueKey(connection,
                    identity.getTenantId(), identity.getIdentityType(),
                    identity.getSourceSystem(), identity.getExternalId());
            if (existingId != null) {
                identity.setId(existingId);
                updatePatientIdentity(connection, identity);
            } else {
                insertPatientIdentity(connection, identity);
            }
            return identity;
        } catch (SQLException ex) {
            throw new IllegalStateException("save patient identity failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 根据ID查找患者标识。
     */
    public PatientIdentity findById(Long id) {
        String sql = "SELECT * FROM mpi_patient_identity WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPatientIdentity(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find patient identity by id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据平台患者ID查找所有标识。
     */
    public List<PatientIdentity> findByPlatformId(String tenantId, String platformPatientId) {
        String sql = "SELECT * FROM mpi_patient_identity WHERE tenant_id = ? AND platform_patient_id = ? ORDER BY created_time";
        List<PatientIdentity> identities = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, platformPatientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    identities.add(mapPatientIdentity(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find patient identities by platform id failed: " + ex.getMessage(), ex);
        }
        return identities;
    }

    /**
     * 根据外部标识查找患者标识。
     */
    public PatientIdentity findByExternalId(String tenantId, String identityType, String sourceSystem, String externalId) {
        String sql = "SELECT * FROM mpi_patient_identity WHERE tenant_id = ? AND identity_type = ? AND source_system = ? AND external_id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, identityType);
            ps.setString(3, sourceSystem);
            ps.setString(4, externalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPatientIdentity(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find patient identity by external id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 根据hash查找患者标识。
     */
    public List<PatientIdentity> findByHash(String tenantId, String idHash) {
        String sql = "SELECT * FROM mpi_patient_identity WHERE tenant_id = ? AND id_hash = ? ORDER BY created_time";
        List<PatientIdentity> identities = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, idHash);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    identities.add(mapPatientIdentity(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find patient identities by hash failed: " + ex.getMessage(), ex);
        }
        return identities;
    }

    /**
     * 更新患者标识状态。
     */
    public void updateStatus(Long id, String status, String updatedBy) {
        String sql = "UPDATE mpi_patient_identity SET status = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update patient identity status failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 验证患者标识。
     */
    public void verify(Long id, String verifiedBy) {
        String sql = "UPDATE mpi_patient_identity SET manually_verified = 1, verified_by = ?, verified_time = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, verifiedBy);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(4, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("verify patient identity failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 根据外部标识查找所有患者标识列表（供冲突检测使用）。
     */
    List<PatientIdentity> findPatientIdentitiesByExternalIdList(String tenantId, String identityType, String sourceSystem, String externalId) {
        String sql = "SELECT * FROM mpi_patient_identity WHERE tenant_id = ? AND identity_type = ? AND source_system = ? AND external_id = ?";
        List<PatientIdentity> identities = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, identityType);
            ps.setString(3, sourceSystem);
            ps.setString(4, externalId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    identities.add(mapPatientIdentity(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find patient identities by external id list failed: " + ex.getMessage(), ex);
        }
        return identities;
    }

    // ============================================================================
    // 私有方法
    // ============================================================================

    private Long findPatientIdentityIdByUniqueKey(Connection connection, String tenantId,
                                                  String identityType, String sourceSystem,
                                                  String externalId) throws SQLException {
        String sql = "SELECT id FROM mpi_patient_identity "
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

    private void insertPatientIdentity(Connection connection, PatientIdentity identity) throws SQLException {
        String sql = "INSERT INTO mpi_patient_identity (id, tenant_id, platform_patient_id, identity_type, "
                + "external_id, id_hash, source_system, status, confidence, manually_verified, "
                + "verified_by, verified_time, merged_to_id, remarks, created_time, updated_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, identity.getId());
            ps.setString(2, identity.getTenantId());
            ps.setString(3, identity.getPlatformPatientId());
            ps.setString(4, identity.getIdentityType());
            ps.setString(5, identity.getExternalId());
            ps.setString(6, identity.getIdHash());
            ps.setString(7, identity.getSourceSystem());
            ps.setString(8, identity.getStatus());
            ps.setObject(9, identity.getConfidence());
            ps.setObject(10, identity.getManuallyVerified() != null ? (identity.getManuallyVerified() ? 1 : 0) : null);
            ps.setString(11, identity.getVerifiedBy());
            ps.setTimestamp(12, identity.getVerifiedTime() != null ? Timestamp.valueOf(identity.getVerifiedTime()) : null);
            ps.setObject(13, identity.getMergedToId());
            ps.setString(14, identity.getRemarks());
            ps.setTimestamp(15, Timestamp.valueOf(identity.getCreatedTime()));
            ps.setTimestamp(16, Timestamp.valueOf(identity.getUpdatedTime()));
            ps.executeUpdate();
        }
    }

    private void updatePatientIdentity(Connection connection, PatientIdentity identity) throws SQLException {
        String sql = "UPDATE mpi_patient_identity SET platform_patient_id = ?, status = ?, "
                + "confidence = ?, manually_verified = ?, verified_by = ?, verified_time = ?, "
                + "merged_to_id = ?, remarks = ?, updated_time = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, identity.getPlatformPatientId());
            ps.setString(2, identity.getStatus());
            ps.setObject(3, identity.getConfidence());
            ps.setObject(4, identity.getManuallyVerified() != null ? (identity.getManuallyVerified() ? 1 : 0) : null);
            ps.setString(5, identity.getVerifiedBy());
            ps.setTimestamp(6, identity.getVerifiedTime() != null ? Timestamp.valueOf(identity.getVerifiedTime()) : null);
            ps.setObject(7, identity.getMergedToId());
            ps.setString(8, identity.getRemarks());
            ps.setTimestamp(9, Timestamp.valueOf(identity.getUpdatedTime()));
            ps.setLong(10, identity.getId());
            ps.executeUpdate();
        }
    }

    private PatientIdentity mapPatientIdentity(ResultSet rs) throws SQLException {
        PatientIdentity identity = new PatientIdentity();
        identity.setId(rs.getLong("id"));
        identity.setTenantId(rs.getString("tenant_id"));
        identity.setPlatformPatientId(rs.getString("platform_patient_id"));
        identity.setIdentityType(rs.getString("identity_type"));
        identity.setExternalId(rs.getString("external_id"));
        identity.setIdHash(rs.getString("id_hash"));
        identity.setSourceSystem(rs.getString("source_system"));
        identity.setStatus(rs.getString("status"));
        identity.setConfidence(rs.getObject("confidence") != null ? rs.getInt("confidence") : null);
        identity.setManuallyVerified(rs.getObject("manually_verified") != null ? rs.getInt("manually_verified") == 1 : null);
        identity.setVerifiedBy(rs.getString("verified_by"));
        Timestamp verifiedTime = rs.getTimestamp("verified_time");
        identity.setVerifiedTime(verifiedTime != null ? verifiedTime.toLocalDateTime() : null);
        identity.setMergedToId(rs.getObject("merged_to_id") != null ? rs.getLong("merged_to_id") : null);
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
