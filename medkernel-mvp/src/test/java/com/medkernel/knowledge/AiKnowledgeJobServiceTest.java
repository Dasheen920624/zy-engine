package com.medkernel.knowledge;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiKnowledgeJobServiceTest {

    @Mock
    private EnginePersistenceProperties properties;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private AiKnowledgeJobService jobService;

    @BeforeEach
    void setUp() throws SQLException {
        jobService = new AiKnowledgeJobService(properties, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    // ==================== createJob ====================

    @Test
    void createJob_shouldCreateWithDefaults() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        AiKnowledgeJob job = new AiKnowledgeJob();
        job.setTenantId(1L);
        job.setJobName("Test Job");
        job.setJobType("TERMINOLOGY_EXTRACTION");
        job.setSourceCode("SRC-001");
        job.setModelProvider("OpenAI");
        job.setModelName("gpt-4");
        job.setCreatedBy("admin");

        AiKnowledgeJob result = jobService.createJob(job);

        assertNotNull(result.getId());
        assertNotNull(result.getJobCode());
        assertTrue(result.getJobCode().startsWith("JOB-"));
        assertEquals("PENDING", result.getStatus());
        assertEquals("PENDING", result.getReviewStatus());
        assertEquals(3, result.getMaxRetries());
        assertNotNull(result.getCreatedTime());
    }

    @Test
    void createJob_shouldUseProvidedJobCode() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        AiKnowledgeJob job = new AiKnowledgeJob();
        job.setTenantId(1L);
        job.setJobCode("CUSTOM-001");
        job.setJobName("Custom Job");
        job.setCreatedBy("admin");

        AiKnowledgeJob result = jobService.createJob(job);
        assertEquals("CUSTOM-001", result.getJobCode());
    }

    @Test
    void createJob_shouldPreserveProvidedStatus() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        AiKnowledgeJob job = new AiKnowledgeJob();
        job.setTenantId(1L);
        job.setJobName("Pre-set Status Job");
        job.setStatus("RUNNING");
        job.setReviewStatus("APPROVED");
        job.setMaxRetries(5);
        job.setCreatedBy("admin");

        AiKnowledgeJob result = jobService.createJob(job);
        assertEquals("RUNNING", result.getStatus());
        assertEquals("APPROVED", result.getReviewStatus());
        assertEquals(5, result.getMaxRetries());
    }

    @Test
    void createJob_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        AiKnowledgeJob job = new AiKnowledgeJob();
        job.setTenantId(1L);
        job.setJobName("Fail Job");

        assertThrows(IllegalStateException.class, () -> jobService.createJob(job));
    }

    // ==================== updateJobStatus ====================

    @Test
    void updateJobStatus_shouldUpdateToRunning() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        jobService.updateJobStatus(1L, "RUNNING", null, null);
        verify(preparedStatement).setString(1, "RUNNING");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void updateJobStatus_shouldUpdateToSuccess() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        jobService.updateJobStatus(1L, "SUCCESS", null, null);
        verify(preparedStatement).setString(1, "SUCCESS");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void updateJobStatus_shouldUpdateToFailed() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        jobService.updateJobStatus(1L, "FAILED", "TIMEOUT", "Request timed out");
        verify(preparedStatement).setString(1, "FAILED");
        verify(preparedStatement).setString(2, "TIMEOUT");
        verify(preparedStatement).setString(3, "Request timed out");
    }

    @Test
    void updateJobStatus_shouldUpdateToRetry() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        jobService.updateJobStatus(1L, "RETRY", null, null);
        verify(preparedStatement).setString(1, "PENDING"); // RETRY resets to PENDING
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void updateJobStatus_shouldUpdateToCancelled() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        jobService.updateJobStatus(1L, "CANCELLED", null, null);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void updateJobStatus_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        assertThrows(IllegalStateException.class,
                () -> jobService.updateJobStatus(1L, "RUNNING", null, null));
    }

    // ==================== reviewJob ====================

    @Test
    void reviewJob_shouldUpdateReviewStatus() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        jobService.reviewJob(1L, "APPROVED", "reviewer", "LGTM");
        verify(preparedStatement).setString(1, "APPROVED");
        verify(preparedStatement).setString(2, "reviewer");
        verify(preparedStatement).setString(4, "LGTM");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void reviewJob_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        assertThrows(IllegalStateException.class,
                () -> jobService.reviewJob(1L, "APPROVED", "reviewer", "ok"));
    }

    // ==================== listJobs ====================

    @Test
    void listJobs_shouldReturnEmptyList() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<AiKnowledgeJob> jobs = jobService.listJobs(1L, null, null, null, 10);
        assertTrue(jobs.isEmpty());
    }

    @Test
    void listJobs_shouldFilterByJobType() throws SQLException {
        when(connection.prepareStatement(contains("job_type"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        jobService.listJobs(1L, "TERMINOLOGY_EXTRACTION", null, null, 10);
        verify(preparedStatement).setString(2, "TERMINOLOGY_EXTRACTION");
    }

    @Test
    void listJobs_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        assertThrows(IllegalStateException.class,
                () -> jobService.listJobs(1L, null, null, null, 10));
    }

    // ==================== getJob ====================

    @Test
    void getJob_shouldReturnNullWhenNotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(jobService.getJob(99999L));
    }

    @Test
    void getJob_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        assertThrows(IllegalStateException.class, () -> jobService.getJob(1L));
    }

    // ==================== logModelCall ====================

    @Test
    void logModelCall_shouldCreateLogWithDefaults() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        AiModelCallLog callLog = new AiModelCallLog();
        callLog.setTenantId(1L);
        callLog.setCallType("EXTRACTION");
        callLog.setModelProvider("OpenAI");
        callLog.setModelName("gpt-4");
        callLog.setCallStatus("SUCCESS");
        callLog.setCreatedBy("admin");

        AiModelCallLog result = jobService.logModelCall(callLog);

        assertNotNull(result.getId());
        assertNotNull(result.getCreatedTime());
        assertNotNull(result.getCalledTime());
    }

    @Test
    void logModelCall_shouldPreserveCalledTime() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        LocalDateTime customTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        AiModelCallLog callLog = new AiModelCallLog();
        callLog.setTenantId(1L);
        callLog.setCallType("EXTRACTION");
        callLog.setCalledTime(customTime);

        AiModelCallLog result = jobService.logModelCall(callLog);
        assertEquals(customTime, result.getCalledTime());
    }

    @Test
    void logModelCall_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        AiModelCallLog callLog = new AiModelCallLog();
        callLog.setTenantId(1L);

        assertThrows(IllegalStateException.class, () -> jobService.logModelCall(callLog));
    }

    // ==================== listModelCallLogs ====================

    @Test
    void listModelCallLogs_shouldReturnEmptyList() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<AiModelCallLog> logs = jobService.listModelCallLogs(1L, null, null, null, 10);
        assertTrue(logs.isEmpty());
    }

    @Test
    void listModelCallLogs_shouldFilterByJobId() throws SQLException {
        when(connection.prepareStatement(contains("job_id"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        jobService.listModelCallLogs(1L, 100L, null, null, 10);
        verify(preparedStatement).setString(2, "100");
    }

    // ==================== summarizeModelCalls ====================

    @Test
    void summarizeModelCalls_shouldReturnSummary() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("call_status")).thenReturn("SUCCESS");
        when(resultSet.getLong("cnt")).thenReturn(10L);
        when(resultSet.getDouble("avg_ms")).thenReturn(150.5);

        Map<String, Object> summary = jobService.summarizeModelCalls(1L);

        assertEquals(1L, summary.get("tenantId"));
        assertNotNull(summary.get("SUCCESS"));
    }

    @Test
    void summarizeModelCalls_shouldHandleSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        // Should not throw, just log error
        Map<String, Object> summary = jobService.summarizeModelCalls(1L);
        assertNotNull(summary);
        assertEquals(1L, summary.get("tenantId"));
    }
}
