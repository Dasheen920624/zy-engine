package com.medkernel.knowledge;

import com.medkernel.ops.entity.OpsSyncTask;
import com.medkernel.ops.service.OpsSyncTaskService;
import com.medkernel.persistence.EnginePersistenceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KnowledgeSyncServiceTest {

    @Mock
    private EnginePersistenceProperties properties;

    @Mock
    private OpsSyncTaskService opsSyncTaskService;

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private KnowledgeSyncService syncService;

    @BeforeEach
    void setUp() throws SQLException {
        syncService = new KnowledgeSyncService(properties, opsSyncTaskService, knowledgeService, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    // ==================== triggerManualSync ====================

    @Test
    void triggerManualSync_shouldCreateLogForDryRun() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        KnowledgeSyncLog log = syncService.triggerManualSync(1L, "SRC-001", null, "DRY_RUN", "admin");

        assertNotNull(log);
        assertEquals(1L, log.getTenantId());
        assertEquals("SRC-001", log.getSourceCode());
        assertEquals("MANUAL", log.getSyncType());
        assertEquals("DRY_RUN", log.getSyncMode());
        assertEquals("DIFF_READY", log.getStatus());
    }

    @Test
    void triggerManualSync_shouldCreateLogForFullSync() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        OpsSyncTask mockTask = new OpsSyncTask();
        mockTask.setId(100L);
        when(opsSyncTaskService.createTask(anyLong(), anyString(), anyString(), anyInt())).thenReturn(mockTask);
        doNothing().when(opsSyncTaskService).executeAsync(anyLong(), any(OpsSyncTaskService.TaskExecutor.class));

        KnowledgeSyncLog log = syncService.triggerManualSync(1L, "SRC-002", null, "FULL", "admin");

        assertNotNull(log);
        assertEquals("FULL", log.getSyncMode());
        verify(opsSyncTaskService).createTask(eq(1L), anyString(), eq("KNOWLEDGE_SYNC"), eq(3));
    }

    @Test
    void triggerManualSync_shouldThrowWhenSourceCodeEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> syncService.triggerManualSync(1L, "", null, "FULL", "admin"));
    }

    @Test
    void triggerManualSync_shouldThrowWhenSourceCodeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> syncService.triggerManualSync(1L, null, null, "FULL", "admin"));
    }

    // ==================== triggerAutoSync ====================

    @Test
    void triggerAutoSync_shouldReturnEmptyWhenRunningSyncExists() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        // Generic stubs first
        when(resultSet.getString(anyString())).thenReturn(null);
        when(resultSet.getInt(anyString())).thenReturn(0);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getTimestamp(anyString())).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(false);
        // Specific stubs after generic
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("tenant_id")).thenReturn(1L);
        when(resultSet.getString("sync_code")).thenReturn("SYN-000001");
        when(resultSet.getString("source_code")).thenReturn("SRC-001");
        when(resultSet.getString("sync_type")).thenReturn("AUTO");
        when(resultSet.getString("sync_mode")).thenReturn("FULL");
        when(resultSet.getString("status")).thenReturn("SYNCING");
        when(resultSet.getString("review_status")).thenReturn("PENDING");

        List<KnowledgeSyncLog> result = syncService.triggerAutoSync(1L, "scheduler");
        assertTrue(result.isEmpty());
    }

    // ==================== previewDiff ====================

    @Test
    void previewDiff_shouldThrowWhenLogNotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> syncService.previewDiff(99999L));
    }

    @Test
    void previewDiff_shouldThrowWhenStatusNotAllowed() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        // Generic stubs first
        when(resultSet.getString(anyString())).thenReturn(null);
        when(resultSet.getInt(anyString())).thenReturn(0);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getTimestamp(anyString())).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(false);
        // Specific stubs after generic
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("tenant_id")).thenReturn(1L);
        when(resultSet.getString("sync_code")).thenReturn("SYN-000001");
        when(resultSet.getString("source_code")).thenReturn("SRC-001");
        when(resultSet.getString("sync_type")).thenReturn("MANUAL");
        when(resultSet.getString("sync_mode")).thenReturn("FULL");
        when(resultSet.getString("status")).thenReturn("COMPLETED");

        assertThrows(IllegalStateException.class,
                () -> syncService.previewDiff(1L));
    }

    // ==================== reviewSync ====================

    @Test
    void reviewSync_shouldThrowWhenLogNotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> syncService.reviewSync(99999L, "APPROVED", "admin", "ok"));
    }

    @Test
    void reviewSync_shouldThrowWhenNotDiffReady() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        // Generic stubs first
        when(resultSet.getString(anyString())).thenReturn(null);
        when(resultSet.getInt(anyString())).thenReturn(0);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getTimestamp(anyString())).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(false);
        // Specific stubs after generic
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("tenant_id")).thenReturn(1L);
        when(resultSet.getString("sync_code")).thenReturn("SYN-000001");
        when(resultSet.getString("source_code")).thenReturn("SRC-001");
        when(resultSet.getString("sync_type")).thenReturn("MANUAL");
        when(resultSet.getString("sync_mode")).thenReturn("FULL");
        when(resultSet.getString("status")).thenReturn("PENDING");

        assertThrows(IllegalStateException.class,
                () -> syncService.reviewSync(1L, "APPROVED", "admin", "ok"));
    }

    @Test
    void reviewSync_shouldThrowForInvalidReviewStatus() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        // Generic stubs first
        when(resultSet.getString(anyString())).thenReturn(null);
        when(resultSet.getInt(anyString())).thenReturn(0);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getTimestamp(anyString())).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(false);
        // Specific stubs after generic
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("tenant_id")).thenReturn(1L);
        when(resultSet.getString("sync_code")).thenReturn("SYN-000001");
        when(resultSet.getString("source_code")).thenReturn("SRC-001");
        when(resultSet.getString("sync_type")).thenReturn("MANUAL");
        when(resultSet.getString("sync_mode")).thenReturn("DRY_RUN");
        when(resultSet.getString("status")).thenReturn("DIFF_READY");

        assertThrows(IllegalArgumentException.class,
                () -> syncService.reviewSync(1L, "INVALID", "admin", "ok"));
    }

    @Test
    void reviewSync_shouldApproveSync() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(resultSet.next()).thenReturn(true);
        // Generic stubs first
        when(resultSet.getString(anyString())).thenReturn(null);
        when(resultSet.getInt(anyString())).thenReturn(0);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getTimestamp(anyString())).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(false);
        // Specific stubs after generic
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("tenant_id")).thenReturn(1L);
        when(resultSet.getString("sync_code")).thenReturn("SYN-000001");
        when(resultSet.getString("source_code")).thenReturn("SRC-001");
        when(resultSet.getString("sync_type")).thenReturn("MANUAL");
        when(resultSet.getString("sync_mode")).thenReturn("DRY_RUN");
        when(resultSet.getString("status")).thenReturn("DIFF_READY");

        syncService.reviewSync(1L, "APPROVED", "admin", "LGTM");
        verify(preparedStatement, atLeastOnce()).executeUpdate();
    }

    // ==================== cancelSync ====================

    @Test
    void cancelSync_shouldCancelRunningSync() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(resultSet.next()).thenReturn(true);
        // Generic stubs first
        when(resultSet.getString(anyString())).thenReturn(null);
        when(resultSet.getInt(anyString())).thenReturn(0);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getTimestamp(anyString())).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(true).thenReturn(false);
        // Specific stubs after generic
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("tenant_id")).thenReturn(1L);
        when(resultSet.getString("sync_code")).thenReturn("SYN-000001");
        when(resultSet.getString("source_code")).thenReturn("SRC-001");
        when(resultSet.getString("sync_type")).thenReturn("MANUAL");
        when(resultSet.getString("sync_mode")).thenReturn("FULL");
        when(resultSet.getString("status")).thenReturn("SYNCING");

        syncService.cancelSync(1L, "admin");
        verify(preparedStatement, atLeastOnce()).executeUpdate();
    }

    @Test
    void cancelSync_shouldThrowWhenTerminal() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        // Set generic stubs first, then specific ones (last matching stub wins in Mockito)
        when(resultSet.getString(anyString())).thenReturn(null);
        when(resultSet.getInt(anyString())).thenReturn(0);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getTimestamp(anyString())).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(false);
        // Specific stubs after generic ones
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("tenant_id")).thenReturn(1L);
        when(resultSet.getString("sync_code")).thenReturn("SYN-000001");
        when(resultSet.getString("source_code")).thenReturn("SRC-001");
        when(resultSet.getString("sync_type")).thenReturn("MANUAL");
        when(resultSet.getString("sync_mode")).thenReturn("FULL");
        when(resultSet.getString("status")).thenReturn("COMPLETED");

        assertThrows(IllegalStateException.class,
                () -> syncService.cancelSync(1L, "admin"));
    }

    @Test
    void cancelSync_shouldCancelOpsTask() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(resultSet.next()).thenReturn(true);
        // Generic stubs first
        when(resultSet.getString(anyString())).thenReturn(null);
        when(resultSet.getInt(anyString())).thenReturn(0);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getTimestamp(anyString())).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(false);
        // Specific stubs after generic
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("tenant_id")).thenReturn(1L);
        when(resultSet.getString("sync_code")).thenReturn("SYN-000001");
        when(resultSet.getString("source_code")).thenReturn("SRC-001");
        when(resultSet.getString("sync_type")).thenReturn("MANUAL");
        when(resultSet.getString("sync_mode")).thenReturn("FULL");
        when(resultSet.getString("status")).thenReturn("SYNCING");
        when(resultSet.getString("ops_task_id")).thenReturn("100");
        when(resultSet.getLong("ops_task_id")).thenReturn(100L);

        syncService.cancelSync(1L, "admin");
        verify(opsSyncTaskService).cancelTask(1L, 100L);
    }

    // ==================== retrySync ====================

    @Test
    void retrySync_shouldThrowWhenLogNotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> syncService.retrySync(99999L));
    }

    @Test
    void retrySync_shouldThrowWhenNotFailed() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        // Generic stubs first
        when(resultSet.getString(anyString())).thenReturn(null);
        when(resultSet.getInt(anyString())).thenReturn(0);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getTimestamp(anyString())).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(false);
        // Specific stubs after generic
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("tenant_id")).thenReturn(1L);
        when(resultSet.getString("sync_code")).thenReturn("SYN-000001");
        when(resultSet.getString("source_code")).thenReturn("SRC-001");
        when(resultSet.getString("sync_type")).thenReturn("MANUAL");
        when(resultSet.getString("sync_mode")).thenReturn("FULL");
        when(resultSet.getString("status")).thenReturn("COMPLETED");
        when(resultSet.wasNull()).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> syncService.retrySync(1L));
    }

    // ==================== getSyncLog ====================

    @Test
    void getSyncLog_shouldReturnNullWhenNotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(syncService.getSyncLog(99999L));
    }

    // ==================== listSyncLogs ====================

    @Test
    void listSyncLogs_shouldReturnEmptyList() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<KnowledgeSyncLog> logs = syncService.listSyncLogs(1L, null, null, null, 10);
        assertTrue(logs.isEmpty());
    }

    // ==================== summarizeSync ====================

    @Test
    void summarizeSync_shouldReturnSummary() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("status")).thenReturn("COMPLETED");
        when(resultSet.getLong("cnt")).thenReturn(5L);

        Map<String, Object> summary = syncService.summarizeSync(1L);

        assertEquals(1L, summary.get("tenantId"));
        assertEquals(5L, summary.get("COMPLETED"));
        assertEquals(5L, summary.get("total"));
    }

    // ==================== KnowledgeSyncLog state methods ====================

    @Test
    void syncLog_isTerminal_shouldReturnTrueForCompleted() {
        KnowledgeSyncLog log = new KnowledgeSyncLog();
        log.setStatus(KnowledgeSyncLog.STATUS_COMPLETED);
        assertTrue(log.isTerminal());
    }

    @Test
    void syncLog_isTerminal_shouldReturnTrueForFailed() {
        KnowledgeSyncLog log = new KnowledgeSyncLog();
        log.setStatus(KnowledgeSyncLog.STATUS_FAILED);
        assertTrue(log.isTerminal());
    }

    @Test
    void syncLog_isTerminal_shouldReturnTrueForCancelled() {
        KnowledgeSyncLog log = new KnowledgeSyncLog();
        log.setStatus(KnowledgeSyncLog.STATUS_CANCELLED);
        assertTrue(log.isTerminal());
    }

    @Test
    void syncLog_isTerminal_shouldReturnFalseForPending() {
        KnowledgeSyncLog log = new KnowledgeSyncLog();
        log.setStatus(KnowledgeSyncLog.STATUS_PENDING);
        assertFalse(log.isTerminal());
    }

    @Test
    void syncLog_canRetry_shouldReturnTrueForFailed() {
        KnowledgeSyncLog log = new KnowledgeSyncLog();
        log.setStatus(KnowledgeSyncLog.STATUS_FAILED);
        assertTrue(log.canRetry());
    }

    @Test
    void syncLog_canRetry_shouldReturnFalseForCompleted() {
        KnowledgeSyncLog log = new KnowledgeSyncLog();
        log.setStatus(KnowledgeSyncLog.STATUS_COMPLETED);
        assertFalse(log.canRetry());
    }

    @Test
    void syncLog_canCancel_shouldReturnTrueForNonTerminal() {
        KnowledgeSyncLog log = new KnowledgeSyncLog();
        log.setStatus(KnowledgeSyncLog.STATUS_PENDING);
        assertTrue(log.canCancel());
    }

    @Test
    void syncLog_canCancel_shouldReturnFalseForTerminal() {
        KnowledgeSyncLog log = new KnowledgeSyncLog();
        log.setStatus(KnowledgeSyncLog.STATUS_COMPLETED);
        assertFalse(log.canCancel());
    }

    @Test
    void syncLog_isDiffReady_shouldReturnTrueForDiffReady() {
        KnowledgeSyncLog log = new KnowledgeSyncLog();
        log.setStatus(KnowledgeSyncLog.STATUS_DIFF_READY);
        assertTrue(log.isDiffReady());
    }

    @Test
    void syncLog_isDiffReady_shouldReturnFalseForPending() {
        KnowledgeSyncLog log = new KnowledgeSyncLog();
        log.setStatus(KnowledgeSyncLog.STATUS_PENDING);
        assertFalse(log.isDiffReady());
    }
}
