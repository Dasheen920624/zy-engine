package com.medkernel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.config.entity.ConfigPackageRollbackRecord;
import com.medkernel.persistence.EnginePersistenceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigPackageRollbackServiceTest {

    @Mock
    private EnginePersistenceProperties properties;

    @Mock
    private DataSource dataSource;

    @Mock
    private ConfigPackageRepository configPackageRepository;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private ConfigPackageRollbackService rollbackService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws SQLException {
        objectMapper = new ObjectMapper();
        rollbackService = new ConfigPackageRollbackService(
                properties, dataSource, objectMapper, configPackageRepository);
    }

    private void setupConnectionMock() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    // ==================== createRollbackRecord ====================

    @Test
    void createRollbackRecord_shouldCreateRecordSuccessfully() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);

        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("PUBLISHED");

        ConfigPackageEntity targetEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);
        entities.add(targetEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.createRollbackRecord(
                "default", "PKG001", "2.0.0", "1.0.0", "VERSION_ROLLBACK", "bug fix");

        assertNotNull(result);
        assertEquals("PKG001", result.getPackageCode());
        assertEquals("2.0.0", result.getPackageVersion());
        assertEquals("1.0.0", result.getTargetVersion());
        assertEquals("VERSION_ROLLBACK", result.getRollbackType());
        assertEquals("PENDING", result.getStatus());
        assertEquals("bug fix", result.getRollbackReason());
        assertNotNull(result.getSnapshotBefore());
    }

    @Test
    void createRollbackRecord_shouldDefaultToVersionRollbackType() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);

        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("PUBLISHED");

        ConfigPackageEntity targetEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);
        entities.add(targetEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.createRollbackRecord(
                "default", "PKG001", "2.0.0", "1.0.0", null, "reason");

        assertEquals("VERSION_ROLLBACK", result.getRollbackType());
    }

    @Test
    void createRollbackRecord_shouldThrowWhenPackageCodeIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.createRollbackRecord("default", null, "2.0.0", "1.0.0", "VERSION_ROLLBACK", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldThrowWhenPackageCodeIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.createRollbackRecord("default", "", "2.0.0", "1.0.0", "VERSION_ROLLBACK", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldThrowWhenPackageVersionIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.createRollbackRecord("default", "PKG001", null, "1.0.0", "VERSION_ROLLBACK", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldThrowWhenPackageVersionIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.createRollbackRecord("default", "PKG001", "", "1.0.0", "VERSION_ROLLBACK", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldThrowWhenTargetVersionIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.createRollbackRecord("default", "PKG001", "2.0.0", null, "VERSION_ROLLBACK", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldThrowWhenTargetVersionIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.createRollbackRecord("default", "PKG001", "2.0.0", "", "VERSION_ROLLBACK", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldThrowWhenPackageVersionEqualsTargetVersion() {
        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.createRollbackRecord("default", "PKG001", "1.0.0", "1.0.0", "VERSION_ROLLBACK", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldThrowWhenRollbackTypeIsUnsupported() {
        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.createRollbackRecord("default", "PKG001", "2.0.0", "1.0.0", "INVALID_TYPE", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldThrowWhenPackageNotFound() {
        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.createRollbackRecord("default", "PKG001", "2.0.0", "1.0.0", "VERSION_ROLLBACK", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldThrowWhenPackageStatusDoesNotAllowRollback() {
        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("DRAFT");

        ConfigPackageEntity targetEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);
        entities.add(targetEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.createRollbackRecord("default", "PKG001", "2.0.0", "1.0.0", "VERSION_ROLLBACK", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldThrowWhenTargetVersionNotFound() {
        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("PUBLISHED");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.createRollbackRecord("default", "PKG001", "2.0.0", "1.0.0", "VERSION_ROLLBACK", "reason");
        });
    }

    @Test
    void createRollbackRecord_shouldAllowActiveStatusRollback() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);

        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("ACTIVE");

        ConfigPackageEntity targetEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);
        entities.add(targetEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.createRollbackRecord(
                "default", "PKG001", "2.0.0", "1.0.0", "VERSION_ROLLBACK", "reason");

        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void createRollbackRecord_shouldAllowSyncedStatusRollback() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);

        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("SYNCED");

        ConfigPackageEntity targetEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);
        entities.add(targetEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.createRollbackRecord(
                "default", "PKG001", "2.0.0", "1.0.0", "VERSION_ROLLBACK", "reason");

        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void createRollbackRecord_shouldSupportFullRollbackType() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);

        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("PUBLISHED");

        ConfigPackageEntity targetEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);
        entities.add(targetEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.createRollbackRecord(
                "default", "PKG001", "2.0.0", "1.0.0", "FULL_ROLLBACK", "reason");

        assertEquals("FULL_ROLLBACK", result.getRollbackType());
    }

    @Test
    void createRollbackRecord_shouldSupportIncrementalRollbackType() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);

        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("PUBLISHED");

        ConfigPackageEntity targetEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);
        entities.add(targetEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.createRollbackRecord(
                "default", "PKG001", "2.0.0", "1.0.0", "INCREMENTAL_ROLLBACK", "reason");

        assertEquals("INCREMENTAL_ROLLBACK", result.getRollbackType());
    }

    // ==================== preCheck ====================

    @Test
    void preCheck_shouldPassWhenAllChecksOk() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        setupRollbackRecordResultSet(1L, "PENDING");

        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("PUBLISHED");

        ConfigPackageEntity targetEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);
        entities.add(targetEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.preCheck(1L);

        assertNotNull(result);
        assertEquals("CHECKING", result.getStatus());
        assertNotNull(result.getPreCheckResult());
    }

    @Test
    void preCheck_shouldThrowWhenRecordNotFound() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.preCheck(999L);
        });
    }

    @Test
    void preCheck_shouldThrowWhenStatusNotPending() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "CHECKING");

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.preCheck(1L);
        });
    }

    @Test
    void preCheck_shouldFailWhenPackageStatusNotAllowRollback() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        setupRollbackRecordResultSet(1L, "PENDING");

        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("DRAFT");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.preCheck(1L);

        assertNotNull(result.getPreCheckResult());
        assertTrue(result.getPreCheckResult().contains("package_status"));
    }

    @Test
    void preCheck_shouldFailWhenTargetVersionNotFound() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        setupRollbackRecordResultSet(1L, "PENDING");

        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("PUBLISHED");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.preCheck(1L);

        assertNotNull(result.getPreCheckResult());
        assertTrue(result.getPreCheckResult().contains("target_version_exists"));
    }

    // ==================== approveRollback ====================

    @Test
    void approveRollback_shouldApproveWhenInCheckingStatus() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "CHECKING");

        ConfigPackageRollbackRecord result = rollbackService.approveRollback(1L, "admin1");

        assertNotNull(result);
        verify(preparedStatement, atLeastOnce()).executeUpdate();
    }

    @Test
    void approveRollback_shouldThrowWhenRecordNotFound() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.approveRollback(999L, "admin1");
        });
    }

    @Test
    void approveRollback_shouldThrowWhenStatusNotChecking() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "PENDING");

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.approveRollback(1L, "admin1");
        });
    }

    @Test
    void approveRollback_shouldThrowWhenApprovedByIsNull() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "CHECKING");

        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.approveRollback(1L, null);
        });
    }

    @Test
    void approveRollback_shouldThrowWhenApprovedByIsEmpty() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "CHECKING");

        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.approveRollback(1L, "");
        });
    }

    @Test
    void approveRollback_shouldThrowOnSqlException() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "CHECKING");
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("db error"));

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.approveRollback(1L, "admin1");
        });
    }

    // ==================== cancelRollback ====================

    @Test
    void cancelRollback_shouldCancelWhenInPendingStatus() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "PENDING");

        ConfigPackageRollbackRecord result = rollbackService.cancelRollback(1L);

        assertNotNull(result);
        verify(preparedStatement, atLeastOnce()).executeUpdate();
    }

    @Test
    void cancelRollback_shouldCancelWhenInCheckingStatus() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "CHECKING");

        ConfigPackageRollbackRecord result = rollbackService.cancelRollback(1L);

        assertNotNull(result);
    }

    @Test
    void cancelRollback_shouldCancelWhenInApprovedStatus() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "APPROVED");

        ConfigPackageRollbackRecord result = rollbackService.cancelRollback(1L);

        assertNotNull(result);
    }

    @Test
    void cancelRollback_shouldThrowWhenRecordNotFound() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.cancelRollback(999L);
        });
    }

    @Test
    void cancelRollback_shouldThrowWhenStatusIsRollingBack() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "ROLLING_BACK");

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.cancelRollback(1L);
        });
    }

    @Test
    void cancelRollback_shouldThrowWhenStatusIsCompleted() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "COMPLETED");

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.cancelRollback(1L);
        });
    }

    @Test
    void cancelRollback_shouldThrowWhenStatusIsFailed() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "FAILED");

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.cancelRollback(1L);
        });
    }

    @Test
    void cancelRollback_shouldThrowWhenStatusIsCancelled() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "CANCELLED");

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.cancelRollback(1L);
        });
    }

    // ==================== executeRollback ====================

    @Test
    void executeRollback_shouldCompleteSuccessfully() throws SQLException {
        setupConnectionMock();
        // Multiple executeUpdate and executeQuery calls
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // First query returns the APPROVED record, subsequent queries return updated record
        when(resultSet.next()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);

        setupRollbackRecordResultSet(1L, "APPROVED");

        ConfigPackageEntity currentEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        currentEntity.setStatus("PUBLISHED");

        ConfigPackageEntity targetEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");
        targetEntity.setStatus("PUBLISHED");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(currentEntity);
        entities.add(targetEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.executeRollback(1L, "operator1");

        assertNotNull(result);
    }

    @Test
    void executeRollback_shouldThrowWhenRecordNotFound() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.executeRollback(999L, "operator1");
        });
    }

    @Test
    void executeRollback_shouldThrowWhenStatusNotApproved() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "PENDING");

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.executeRollback(1L, "operator1");
        });
    }

    @Test
    void executeRollback_shouldSetFailedOnRestoreException() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false).thenReturn(true).thenReturn(false);

        setupRollbackRecordResultSet(1L, "APPROVED");

        // Make findList throw to simulate restore failure
        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenThrow(new RuntimeException("db error"));

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.executeRollback(1L, "operator1");
        });
    }

    // ==================== postCheck ====================

    @Test
    void postCheck_shouldReturnCheckResult() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        setupRollbackRecordResultSet(1L, "COMPLETED");

        ConfigPackageEntity targetEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");
        targetEntity.setStatus("PUBLISHED");

        ConfigPackageEntity sourceEntity = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        sourceEntity.setStatus("RETIRED");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(targetEntity);
        entities.add(sourceEntity);

        when(configPackageRepository.findList("default", "PKG001", null, null, null, null))
                .thenReturn(entities);

        ConfigPackageRollbackRecord result = rollbackService.postCheck(1L);

        assertNotNull(result);
        assertNotNull(result.getPostCheckResult());
    }

    @Test
    void postCheck_shouldThrowWhenRecordNotFound() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            rollbackService.postCheck(999L);
        });
    }

    // ==================== getRollbackRecord ====================

    @Test
    void getRollbackRecord_shouldReturnRecord() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupRollbackRecordResultSet(1L, "PENDING");

        ConfigPackageRollbackRecord result = rollbackService.getRollbackRecord(1L);

        assertNotNull(result);
        assertEquals("PKG001", result.getPackageCode());
    }

    @Test
    void getRollbackRecord_shouldReturnNullWhenNotFound() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        ConfigPackageRollbackRecord result = rollbackService.getRollbackRecord(999L);

        assertEquals(null, result);
    }

    @Test
    void getRollbackRecord_shouldThrowOnSqlException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection failed"));

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.getRollbackRecord(1L);
        });
    }

    // ==================== listRollbackRecords ====================

    @Test
    void listRollbackRecords_shouldReturnRecords() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        setupRollbackRecordResultSet(1L, "PENDING");

        List<ConfigPackageRollbackRecord> result = rollbackService.listRollbackRecords(
                "default", "PKG001", "PENDING");

        assertNotNull(result);
    }

    @Test
    void listRollbackRecords_shouldHandleNoRecords() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<ConfigPackageRollbackRecord> result = rollbackService.listRollbackRecords(
                "default", "PKG001", null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void listRollbackRecords_shouldHandleNullParameters() throws SQLException {
        setupConnectionMock();
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<ConfigPackageRollbackRecord> result = rollbackService.listRollbackRecords(
                null, null, null);

        assertNotNull(result);
    }

    @Test
    void listRollbackRecords_shouldThrowOnSqlException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection failed"));

        assertThrows(IllegalStateException.class, () -> {
            rollbackService.listRollbackRecords("default", "PKG001", null);
        });
    }

    // ==================== Helper methods ====================

    private ConfigPackageEntity createEntity(String tenantId, String packageCode, String packageVersion,
                                              String assetType, String scopeLevel, String scopeCode,
                                              String contentHash) {
        ConfigPackageEntity entity = new ConfigPackageEntity();
        entity.setId(1L);
        entity.setTenantId(tenantId);
        entity.setPackageCode(packageCode);
        entity.setPackageVersion(packageVersion);
        entity.setAssetType(assetType);
        entity.setScopeLevel(scopeLevel);
        entity.setScopeCode(scopeCode);
        entity.setStatus("DRAFT");
        entity.setContentHash(contentHash);
        entity.setCreatedBy("test-user");
        return entity;
    }

    private void setupRollbackRecordResultSet(Long id, String status) throws SQLException {
        when(resultSet.getLong("id")).thenReturn(id);
        when(resultSet.getString("tenant_id")).thenReturn("default");
        when(resultSet.getString("package_code")).thenReturn("PKG001");
        when(resultSet.getString("package_version")).thenReturn("2.0.0");
        when(resultSet.getString("target_version")).thenReturn("1.0.0");
        when(resultSet.getString("rollback_type")).thenReturn("VERSION_ROLLBACK");
        when(resultSet.getString("status")).thenReturn(status);
        when(resultSet.getString("pre_check_result")).thenReturn(null);
        when(resultSet.getString("post_check_result")).thenReturn(null);
        when(resultSet.getString("snapshot_before")).thenReturn(null);
        when(resultSet.getString("snapshot_after")).thenReturn(null);
        when(resultSet.getString("rollback_reason")).thenReturn("test reason");
        when(resultSet.getString("approved_by")).thenReturn(null);
        when(resultSet.getTimestamp("approved_time")).thenReturn(null);
        when(resultSet.getString("rolled_back_by")).thenReturn(null);
        when(resultSet.getTimestamp("rolled_back_time")).thenReturn(null);
        when(resultSet.getTimestamp("completed_time")).thenReturn(null);
        when(resultSet.getString("created_by")).thenReturn("system");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);
    }
}
