package com.medkernel.patient;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MPI模块持久化服务：使用 raw JDBC 操作 MPI 表。
 * 复用 EnginePersistenceProperties 获取数据库连接配置。
 */
@Service
public class MpiPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(MpiPersistenceService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public MpiPersistenceService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initializeMpiSchema() {
        if (!properties.isEnabled() || !properties.localFileDatabase()) {
            return;
        }
        List<String> statements = loadSchemaStatements("/db/local/mpi_ddl.sql");
        if (statements.isEmpty()) {
            return;
        }
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
            log.info("MPI schema initialized successfully");
        } catch (SQLException ex) {
            log.error("initialize MPI schema failed", ex);
            throw new IllegalStateException("initialize MPI schema failed: " + ex.getMessage(), ex);
        }
    }

    // ============================================================================
    // 患者标识映射操作
    // ============================================================================

    /**
     * 保存患者标识映射。
     */
    public PatientIdentity savePatientIdentity(PatientIdentity identity) {
        if (identity.getId() == null) {
            identity.setId(Ids.next());
        }
        if (identity.getExternalId() != null && identity.getIdHash() == null) {
            identity.setIdHash(hashId(identity.getExternalId()));
        }
        if (identity.getCreatedTime() == null) {
            identity.setCreatedTime(LocalDateTime.now());
        }
        identity.setUpdatedTime(LocalDateTime.now());

        // 原实现用 MySQL 专有 ON DUPLICATE KEY UPDATE，Oracle/DM/PG/Kingbase/H2 均不支持。
        // 改为同事务内"按唯一键 SELECT → UPDATE 或 INSERT"二选一（AUDIT-20260520 §2.2 已要求统一修）。
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
        // 与原 ON DUPLICATE KEY UPDATE 子句列清单一致，id/tenant_id/identity_type/source_system/external_id/id_hash/created_time 不动。
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

    /**
     * 根据ID查找患者标识。
     */
    public PatientIdentity findPatientIdentityById(Long id) {
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
    public List<PatientIdentity> findPatientIdentitiesByPlatformId(String tenantId, String platformPatientId) {
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
    public PatientIdentity findPatientIdentityByExternalId(String tenantId, String identityType, String sourceSystem, String externalId) {
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
    public List<PatientIdentity> findPatientIdentitiesByHash(String tenantId, String idHash) {
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
    public void updatePatientIdentityStatus(Long id, String status, String updatedBy) {
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
    public void verifyPatientIdentity(Long id, String verifiedBy) {
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

    // ============================================================================
    // 就诊标识映射操作
    // ============================================================================

    /**
     * 保存就诊标识映射。
     */
    public VisitIdentity saveVisitIdentity(VisitIdentity identity) {
        if (identity.getId() == null) {
            identity.setId(Ids.next());
        }
        if (identity.getExternalId() != null && identity.getIdHash() == null) {
            identity.setIdHash(hashId(identity.getExternalId()));
        }
        if (identity.getCreatedTime() == null) {
            identity.setCreatedTime(LocalDateTime.now());
        }
        identity.setUpdatedTime(LocalDateTime.now());

        // 跨数据库通用 upsert（见 savePatientIdentity 注释）。
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

    /**
     * 根据ID查找就诊标识。
     */
    public VisitIdentity findVisitIdentityById(Long id) {
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
    public List<VisitIdentity> findVisitIdentitiesByPlatformId(String tenantId, String platformVisitId) {
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
    public List<VisitIdentity> findVisitIdentitiesByPatientId(String tenantId, String platformPatientId) {
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
    public VisitIdentity findVisitIdentityByExternalId(String tenantId, String identityType, String sourceSystem, String externalId) {
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
    // 标识冲突操作
    // ============================================================================

    /**
     * 保存标识冲突。
     */
    public IdentityConflict saveIdentityConflict(IdentityConflict conflict) {
        if (conflict.getId() == null) {
            conflict.setId(Ids.next());
        }
        if (conflict.getCreatedTime() == null) {
            conflict.setCreatedTime(LocalDateTime.now());
        }
        conflict.setUpdatedTime(LocalDateTime.now());

        // 跨数据库通用 upsert：mpi_identity_conflict 业务唯一键即 id 主键（DDL 无其它 UNIQUE 约束）。
        // 行为：传入 id 已存在 → UPDATE 可变列；不存在 → INSERT 全列。
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
        // 与原 ON DUPLICATE KEY UPDATE 子句列清单一致：id/tenant_id/conflict_type/patient_identity_ids/visit_identity_ids/conflict_description/created_time 不动。
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

    /**
     * 根据ID查找标识冲突。
     */
    public IdentityConflict findIdentityConflictById(Long id) {
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

    // ============================================================================
    // 冲突检测
    // ============================================================================

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

                    // 查找重复的标识
                    List<PatientIdentity> duplicates = findPatientIdentitiesByExternalIdList(tenantId, identityType, sourceSystem, externalId);
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

                    List<PatientIdentity> identities = findPatientIdentitiesByHash(tenantId, idHash);
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

    private List<PatientIdentity> findPatientIdentitiesByExternalIdList(String tenantId, String identityType, String sourceSystem, String externalId) {
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
    // 工具方法
    // ============================================================================

    /**
     * 计算标识的SHA-256 hash。
     */
    public String hashId(String externalId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(externalId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
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
        // PR-FINAL-15: 走 HikariCP 连接池（EngineDataSourceConfig 暴露的 DataSource）。
        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            throw new IllegalStateException("get connection failed: " + ex.getMessage(), ex);
        }
    }

    private List<String> loadSchemaStatements(String resourcePath) {
        List<String> statements = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("MPI DDL resource not found: {}", resourcePath);
                return statements;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder current = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                        continue;
                    }
                    current.append(line).append("\n");
                    if (trimmed.endsWith(";")) {
                        statements.add(current.toString().trim());
                        current.setLength(0);
                    }
                }
                if (current.length() > 0) {
                    statements.add(current.toString().trim());
                }
            }
        } catch (IOException ex) {
            log.error("load MPI DDL failed", ex);
        }
        return statements;
    }
}