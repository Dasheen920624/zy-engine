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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiCandidateReviewServiceTest {

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

    private AiCandidateReviewService reviewService;

    @BeforeEach
    void setUp() throws SQLException {
        reviewService = new AiCandidateReviewService(properties, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    // ==================== submitCandidate ====================

    @Test
    void submitCandidate_shouldCreateWithDefaults() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        AiCandidateReview candidate = new AiCandidateReview();
        candidate.setTenantId(1L);
        candidate.setCandidateType("TERMINOLOGY_MAPPING");
        candidate.setCandidateName("ICD-10 Mapping");
        candidate.setSourceCode("SRC-001");
        candidate.setModelProvider("OpenAI");
        candidate.setModelName("gpt-4");
        candidate.setConfidence(0.85);
        candidate.setCandidateContent("{\"code\":\"I10\",\"name\":\"Hypertension\"}");
        candidate.setCreatedBy("admin");

        AiCandidateReview result = reviewService.submitCandidate(candidate);

        assertNotNull(result.getId());
        assertNotNull(result.getCandidateCode());
        assertTrue(result.getCandidateCode().startsWith("CAND-"));
        assertEquals("PENDING", result.getReviewStatus());
        assertEquals("MEDIUM", result.getPriority());
        assertNotNull(result.getCreatedTime());
    }

    @Test
    void submitCandidate_shouldUseProvidedValues() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        AiCandidateReview candidate = new AiCandidateReview();
        candidate.setTenantId(1L);
        candidate.setCandidateCode("CUSTOM-CAND-001");
        candidate.setCandidateType("RULE");
        candidate.setCandidateName("Custom Candidate");
        candidate.setReviewStatus("APPROVED");
        candidate.setPriority("HIGH");
        candidate.setCreatedBy("admin");

        AiCandidateReview result = reviewService.submitCandidate(candidate);
        assertEquals("CUSTOM-CAND-001", result.getCandidateCode());
        assertEquals("APPROVED", result.getReviewStatus());
        assertEquals("HIGH", result.getPriority());
    }

    @Test
    void submitCandidate_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        AiCandidateReview candidate = new AiCandidateReview();
        candidate.setTenantId(1L);

        assertThrows(IllegalStateException.class, () -> reviewService.submitCandidate(candidate));
    }

    // ==================== listCandidates ====================

    @Test
    void listCandidates_shouldReturnEmptyList() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<AiCandidateReview> candidates = reviewService.listCandidates(1L, null, null, null, 10);
        assertTrue(candidates.isEmpty());
    }

    @Test
    void listCandidates_shouldFilterByCandidateType() throws SQLException {
        when(connection.prepareStatement(contains("candidate_type"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        reviewService.listCandidates(1L, "TERMINOLOGY_MAPPING", null, null, 10);
        verify(preparedStatement).setString(2, "TERMINOLOGY_MAPPING");
    }

    @Test
    void listCandidates_shouldFilterByReviewStatus() throws SQLException {
        when(connection.prepareStatement(contains("review_status"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        reviewService.listCandidates(1L, null, "PENDING", null, 10);
        verify(preparedStatement).setString(2, "PENDING");
    }

    @Test
    void listCandidates_shouldFilterByPriority() throws SQLException {
        when(connection.prepareStatement(contains("priority"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        reviewService.listCandidates(1L, null, null, "HIGH", 10);
        verify(preparedStatement).setString(2, "HIGH");
    }

    @Test
    void listCandidates_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        assertThrows(IllegalStateException.class,
                () -> reviewService.listCandidates(1L, null, null, null, 10));
    }

    // ==================== getCandidate ====================

    @Test
    void getCandidate_shouldReturnNullWhenNotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(reviewService.getCandidate(99999L));
    }

    @Test
    void getCandidate_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        assertThrows(IllegalStateException.class, () -> reviewService.getCandidate(1L));
    }

    // ==================== reviewCandidate ====================

    @Test
    void reviewCandidate_shouldApproveCandidate() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        reviewService.reviewCandidate(1L, "APPROVED", "reviewer", "LGTM", null);
        verify(preparedStatement).setString(1, "APPROVED");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void reviewCandidate_shouldRejectCandidate() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        reviewService.reviewCandidate(1L, "REJECTED", "reviewer", "Bad quality", null);
        verify(preparedStatement).setString(1, "REJECTED");
    }

    @Test
    void reviewCandidate_shouldModifyContent() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        reviewService.reviewCandidate(1L, "MODIFIED", "reviewer", "Adjusted", "{\"modified\":true}");
        verify(preparedStatement).setString(1, "MODIFIED");
        // With modifiedContent: reviewStatus, reviewedBy, reviewNote, modifiedContent = 4 setString calls
        verify(preparedStatement, atLeast(4)).setString(anyInt(), anyString());
    }

    @Test
    void reviewCandidate_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        assertThrows(IllegalStateException.class,
                () -> reviewService.reviewCandidate(1L, "APPROVED", "reviewer", "ok", null));
    }

    // ==================== batchReview ====================

    @Test
    void batchReview_shouldReviewAllCandidates() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        reviewService.batchReview(ids, "APPROVED", "reviewer", "Batch approved");

        verify(preparedStatement, times(3)).executeUpdate();
    }

    // ==================== getReviewSummary ====================

    @Test
    void getReviewSummary_shouldReturnSummary() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Three queries: byStatus, byType, byPriority
        when(resultSet.next()).thenReturn(true).thenReturn(false)
                .thenReturn(true).thenReturn(false)
                .thenReturn(true).thenReturn(false);
        when(resultSet.getString(anyString())).thenReturn("PENDING").thenReturn("RULE").thenReturn("HIGH");
        when(resultSet.getLong("cnt")).thenReturn(5L);

        Map<String, Object> summary = reviewService.getReviewSummary(1L);

        assertNotNull(summary.get("byReviewStatus"));
        assertNotNull(summary.get("byCandidateType"));
        assertNotNull(summary.get("byPriority"));
        assertEquals(5L, summary.get("total"));
    }

    @Test
    void getReviewSummary_shouldHandleSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        Map<String, Object> summary = reviewService.getReviewSummary(1L);
        assertNotNull(summary);
    }

    // ==================== getReviewHistory ====================

    @Test
    void getReviewHistory_shouldReturnEmptyList() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<AiCandidateReview> history = reviewService.getReviewHistory(1L, null, 10);
        assertTrue(history.isEmpty());
    }

    @Test
    void getReviewHistory_shouldFilterByReviewedBy() throws SQLException {
        when(connection.prepareStatement(contains("reviewed_by"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        reviewService.getReviewHistory(1L, "admin", 10);
        verify(preparedStatement).setString(2, "admin");
    }

    @Test
    void getReviewHistory_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        assertThrows(IllegalStateException.class,
                () -> reviewService.getReviewHistory(1L, null, 10));
    }

    // ==================== AiCandidateReview entity ====================

    @Test
    void candidateReview_shouldSetAndGetAllFields() {
        LocalDateTime now = LocalDateTime.now();
        AiCandidateReview c = new AiCandidateReview();
        c.setId(1L);
        c.setTenantId(1L);
        c.setCandidateCode("CAND-0001");
        c.setCandidateType("TERMINOLOGY_MAPPING");
        c.setCandidateName("Test Candidate");
        c.setSourceCode("SRC-001");
        c.setSourceName("Source 1");
        c.setModelProvider("OpenAI");
        c.setModelName("gpt-4");
        c.setConfidence(0.85);
        c.setCandidateContent("{\"code\":\"I10\"}");
        c.setReviewStatus("PENDING");
        c.setReviewedBy("reviewer");
        c.setReviewedTime(now);
        c.setReviewNote("Looks good");
        c.setModifiedContent("{\"code\":\"I10\",\"modified\":true}");
        c.setQualityFindings("No issues");
        c.setPriority("HIGH");
        c.setCreatedBy("admin");
        c.setCreatedTime(now);
        c.setUpdatedBy("admin");
        c.setUpdatedTime(now);

        assertEquals(1L, c.getId().longValue());
        assertEquals("CAND-0001", c.getCandidateCode());
        assertEquals("TERMINOLOGY_MAPPING", c.getCandidateType());
        assertEquals("Test Candidate", c.getCandidateName());
        assertEquals("SRC-001", c.getSourceCode());
        assertEquals("Source 1", c.getSourceName());
        assertEquals("OpenAI", c.getModelProvider());
        assertEquals("gpt-4", c.getModelName());
        assertEquals(0.85, c.getConfidence(), 0.001);
        assertEquals("{\"code\":\"I10\"}", c.getCandidateContent());
        assertEquals("PENDING", c.getReviewStatus());
        assertEquals("reviewer", c.getReviewedBy());
        assertEquals(now, c.getReviewedTime());
        assertEquals("Looks good", c.getReviewNote());
        assertEquals("{\"code\":\"I10\",\"modified\":true}", c.getModifiedContent());
        assertEquals("No issues", c.getQualityFindings());
        assertEquals("HIGH", c.getPriority());
        assertEquals("admin", c.getCreatedBy());
        assertEquals(now, c.getCreatedTime());
        assertEquals("admin", c.getUpdatedBy());
        assertEquals(now, c.getUpdatedTime());
    }
}
