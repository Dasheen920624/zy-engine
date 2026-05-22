package com.medkernel.patient;

import com.medkernel.persistence.EnginePersistenceProperties;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * MPI模块持久化服务门面：委托三个 Repository 完成实际 CRUD，仅保留 schema 初始化。
 */
@Service
public class MpiPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(MpiPersistenceService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;
    private final PatientIdentityRepository patientIdentityRepository;
    private final VisitIdentityRepository visitIdentityRepository;
    private final IdentityConflictRepository identityConflictRepository;

    public MpiPersistenceService(EnginePersistenceProperties properties,
                                 DataSource dataSource,
                                 PatientIdentityRepository patientIdentityRepository,
                                 VisitIdentityRepository visitIdentityRepository,
                                 IdentityConflictRepository identityConflictRepository) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.patientIdentityRepository = patientIdentityRepository;
        this.visitIdentityRepository = visitIdentityRepository;
        this.identityConflictRepository = identityConflictRepository;
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
        try (Connection connection = dataSourceConnection();
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
    // 患者标识映射操作（委托 PatientIdentityRepository）
    // ============================================================================

    public PatientIdentity savePatientIdentity(PatientIdentity identity) {
        return patientIdentityRepository.save(identity);
    }

    public PatientIdentity findPatientIdentityById(Long id) {
        return patientIdentityRepository.findById(id);
    }

    public List<PatientIdentity> findPatientIdentitiesByPlatformId(String tenantId, String platformPatientId) {
        return patientIdentityRepository.findByPlatformId(tenantId, platformPatientId);
    }

    public PatientIdentity findPatientIdentityByExternalId(String tenantId, String identityType, String sourceSystem, String externalId) {
        return patientIdentityRepository.findByExternalId(tenantId, identityType, sourceSystem, externalId);
    }

    public List<PatientIdentity> findPatientIdentitiesByHash(String tenantId, String idHash) {
        return patientIdentityRepository.findByHash(tenantId, idHash);
    }

    public void updatePatientIdentityStatus(Long id, String status, String updatedBy) {
        patientIdentityRepository.updateStatus(id, status, updatedBy);
    }

    public void verifyPatientIdentity(Long id, String verifiedBy) {
        patientIdentityRepository.verify(id, verifiedBy);
    }

    // ============================================================================
    // 就诊标识映射操作（委托 VisitIdentityRepository）
    // ============================================================================

    public VisitIdentity saveVisitIdentity(VisitIdentity identity) {
        return visitIdentityRepository.save(identity);
    }

    public VisitIdentity findVisitIdentityById(Long id) {
        return visitIdentityRepository.findById(id);
    }

    public List<VisitIdentity> findVisitIdentitiesByPlatformId(String tenantId, String platformVisitId) {
        return visitIdentityRepository.findByPlatformId(tenantId, platformVisitId);
    }

    public List<VisitIdentity> findVisitIdentitiesByPatientId(String tenantId, String platformPatientId) {
        return visitIdentityRepository.findByPatientId(tenantId, platformPatientId);
    }

    public VisitIdentity findVisitIdentityByExternalId(String tenantId, String identityType, String sourceSystem, String externalId) {
        return visitIdentityRepository.findByExternalId(tenantId, identityType, sourceSystem, externalId);
    }

    // ============================================================================
    // 标识冲突操作（委托 IdentityConflictRepository）
    // ============================================================================

    public IdentityConflict saveIdentityConflict(IdentityConflict conflict) {
        return identityConflictRepository.save(conflict);
    }

    public IdentityConflict findIdentityConflictById(Long id) {
        return identityConflictRepository.findById(id);
    }

    public List<IdentityConflict> findPendingConflicts(String tenantId) {
        return identityConflictRepository.findPendingConflicts(tenantId);
    }

    public void resolveConflict(Long id, String resolutionType, String resolutionNotes, String resolvedBy, Long targetPatientIdentityId) {
        identityConflictRepository.resolveConflict(id, resolutionType, resolutionNotes, resolvedBy, targetPatientIdentityId);
    }

    public List<IdentityConflict> detectPatientIdentityConflicts(String tenantId) {
        return identityConflictRepository.detectPatientIdentityConflicts(tenantId);
    }

    // ============================================================================
    // 工具方法
    // ============================================================================

    public String hashId(String externalId) {
        return MpiHashUtil.hashId(externalId);
    }

    private Connection dataSourceConnection() {
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
