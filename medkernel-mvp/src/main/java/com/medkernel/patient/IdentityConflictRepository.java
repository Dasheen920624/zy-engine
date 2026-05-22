package com.medkernel.patient;

import com.medkernel.persistence.Ids;
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
import java.util.List;

/**
 * mpi_identity_conflict 表 CRUD + 冲突检测仓库。
 */
@Repository
public class IdentityConflictRepository {

    private static final Logger log = LoggerFactory.getLogger(IdentityConflictRepository.class);

    private final DataSource dataSource;
    private final PatientIdentityRepository patientIdentityRepository;

    public IdentityConflictRepository(DataSource dataSource, PatientIdentityRepository patientIdentityRepository) {
        this.dataSource = dataSource;
        this.patientIdentityRepository = patientIdentityRepository;
    }

    /**
     * 保存标识冲突。
     */
    public IdentityConflict save(IdentityConflict conflict) {
        if (conflict.getId() == null) {
            conflict.setId(Ids.next());
        }
        if (conflict.getCreatedTime() == null) {
            conflict.setCreatedTime(LocalDateTime.now());
        }
        conflict.setUpdatedTime(LocalDateTime.now());

        try (Connection connection = connection()) {
            if (existsIdentityConflictById(connection, conflict.getId())) {
                updateIdentityConflict(connection, conflict);
            } else {
                insertIdentityConflict(connection, conflict);
            }
            return conflict;
        } catch (SQLException ex) {
            throw new IllegalStateException("save identity conflict failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 根据ID查找标识冲突。
     */
    public IdentityConflict findById(Long id) {
        String sql = "SELECT * FROM mpi_identity_conflict WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapIdentityConflict(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find identity conflict by id failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 查找待处理的冲突。
     */
    public List<IdentityConflict> findPendingConflicts(String tenantId) {
        String sql = "SELECT * FROM mpi_identity_conflict WHERE tenant_id = ? AND status = 'PENDING' ORDER BY severity DESC, created_time";
        List<IdentityConflict> conflicts = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    conflicts.add(mapIdentityConflict(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find pending conflicts failed: " + ex.getMessage(), ex);
        }
        return conflicts;
    }

    /**
     * 解决冲突。
     */
    public void resolveConflict(Long id, String resolutionType, String resolutionNotes, String resolvedBy, Long targetPatientIdentityId) {
        String sql = "UPDATE mpi_identity_conflict SET status = 'RESOLVED', resolution_type = ?, "
                + "resolution_notes = ?, resolved_by = ?, resolved_time = ?, "
                + "target_patient_identity_id = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, resolutionType);
            ps.setString(2, resolutionNotes);
            ps.setString(3, resolvedBy);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setObject(5, targetPatientIdentityId);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(7, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("resolve conflict failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 检测患者标识冲突。
     */
    public List<IdentityConflict> detectPatientIdentityConflicts(String tenantId) {
        List<IdentityConflict> conflicts = new ArrayList<>();

        // 检测重复外部标识
        String sql = "SELECT tenant_id, identity_type, source_system, external_id, COUNT(*) as cnt "
                + "FROM mpi_patient_identity WHERE tenant_id = ? AND status = 'ACTIVE' "
                + "GROUP BY tenant_id, identity_type, source_system, external_id HAVING COUNT(*) > 1";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String identityType = rs.getString("identity_type");
                    String sourceSystem = rs.getString("source_system");
                    String externalId = rs.getString("external_id");
                    int count = rs.getInt("cnt");

                    List<PatientIdentity> duplicates = patientIdentityRepository.findPatientIdentitiesByExternalIdList(tenantId, identityType, sourceSystem, externalId);
                    if (duplicates.size() > 1) {
                        IdentityConflict conflict = new IdentityConflict();
                        conflict.setTenantId(tenantId);
                        conflict.setConflictType("DUPLICATE_EXTERNAL");
                        conflict.setSeverity("HIGH");
                        conflict.setConflictDescription(String.format("发现重复外部标识: %s/%s/%s, 共%d条记录",
                                identityType, sourceSystem, externalId, count));
                        conflict.setStatus("PENDING");
                        conflicts.add(conflict);
                    }
                }
            }
        } catch (SQLException ex) {
            log.error("detect patient identity conflicts failed", ex);
        }

        // 检测hash冲突
        String hashSql = "SELECT tenant_id, id_hash, COUNT(DISTINCT platform_patient_id) as cnt "
                + "FROM mpi_patient_identity WHERE tenant_id = ? AND status = 'ACTIVE' "
                + "GROUP BY tenant_id, id_hash HAVING COUNT(DISTINCT platform_patient_id) > 1";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(hashSql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String idHash = rs.getString("id_hash");
                    int count = rs.getInt("cnt");

                    List<PatientIdentity> identities = patientIdentityRepository.findByHash(tenantId, idHash);
                    if (identities.size() > 1) {
                        IdentityConflict conflict = new IdentityConflict();
                        conflict.setTenantId(tenantId);
                        conflict.setConflictType("HASH_MISMATCH");
                        conflict.setSeverity("MEDIUM");
                        conflict.setConflictDescription(String.format("相同hash对应多个患者: %s, 共%d个患者", idHash, count));
                        conflict.setStatus("PENDING");
                        conflicts.add(conflict);
                    }
                }
            }
        } catch (SQLException ex) {
            log.error("detect hash conflicts failed", ex);
        }

        return conflicts;
    }

    // ============================================================================
    // 私有方法
    // ============================================================================

    private boolean existsIdentityConflictById(Connection connection, Long id) throws SQLException {
        if (id == null) {
            return false;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM mpi_identity_conflict WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertIdentityConflict(Connection connection, IdentityConflict conflict) throws SQLException {
        String sql = "INSERT INTO mpi_identity_conflict (id, tenant_id, conflict_type, severity, "
                + "patient_identity_ids, visit_identity_ids, conflict_description, status, "
                + "resolution_type, resolution_notes, resolved_by, resolved_time, "
                + "target_patient_identity_id, created_time, updated_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, conflict.getId());
            ps.setString(2, conflict.getTenantId());
            ps.setString(3, conflict.getConflictType());
            ps.setString(4, conflict.getSeverity());
            ps.setString(5, conflict.getPatientIdentityIds());
            ps.setString(6, conflict.getVisitIdentityIds());
            ps.setString(7, conflict.getConflictDescription());
            ps.setString(8, conflict.getStatus());
            ps.setString(9, conflict.getResolutionType());
            ps.setString(10, conflict.getResolutionNotes());
            ps.setString(11, conflict.getResolvedBy());
            ps.setTimestamp(12, conflict.getResolvedTime() != null ? Timestamp.valueOf(conflict.getResolvedTime()) : null);
            ps.setObject(13, conflict.getTargetPatientIdentityId());
            ps.setTimestamp(14, Timestamp.valueOf(conflict.getCreatedTime()));
            ps.setTimestamp(15, Timestamp.valueOf(conflict.getUpdatedTime()));
            ps.executeUpdate();
        }
    }

    private void updateIdentityConflict(Connection connection, IdentityConflict conflict) throws SQLException {
        String sql = "UPDATE mpi_identity_conflict SET severity = ?, status = ?, "
                + "resolution_type = ?, resolution_notes = ?, resolved_by = ?, resolved_time = ?, "
                + "target_patient_identity_id = ?, updated_time = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conflict.getSeverity());
            ps.setString(2, conflict.getStatus());
            ps.setString(3, conflict.getResolutionType());
            ps.setString(4, conflict.getResolutionNotes());
            ps.setString(5, conflict.getResolvedBy());
            ps.setTimestamp(6, conflict.getResolvedTime() != null ? Timestamp.valueOf(conflict.getResolvedTime()) : null);
            ps.setObject(7, conflict.getTargetPatientIdentityId());
            ps.setTimestamp(8, Timestamp.valueOf(conflict.getUpdatedTime()));
            ps.setLong(9, conflict.getId());
            ps.executeUpdate();
        }
    }

    private IdentityConflict mapIdentityConflict(ResultSet rs) throws SQLException {
        IdentityConflict conflict = new IdentityConflict();
        conflict.setId(rs.getLong("id"));
        conflict.setTenantId(rs.getString("tenant_id"));
        conflict.setConflictType(rs.getString("conflict_type"));
        conflict.setSeverity(rs.getString("severity"));
        conflict.setPatientIdentityIds(rs.getString("patient_identity_ids"));
        conflict.setVisitIdentityIds(rs.getString("visit_identity_ids"));
        conflict.setConflictDescription(rs.getString("conflict_description"));
        conflict.setStatus(rs.getString("status"));
        conflict.setResolutionType(rs.getString("resolution_type"));
        conflict.setResolutionNotes(rs.getString("resolution_notes"));
        conflict.setResolvedBy(rs.getString("resolved_by"));
        Timestamp resolvedTime = rs.getTimestamp("resolved_time");
        conflict.setResolvedTime(resolvedTime != null ? resolvedTime.toLocalDateTime() : null);
        conflict.setTargetPatientIdentityId(rs.getObject("target_patient_identity_id") != null ? rs.getLong("target_patient_identity_id") : null);
        conflict.setCreatedTime(rs.getTimestamp("created_time").toLocalDateTime());
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        conflict.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return conflict;
    }

    private Connection connection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            throw new IllegalStateException("get connection failed: " + ex.getMessage(), ex);
        }
    }
}
