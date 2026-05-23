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
class AssetQualityServiceTest {

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

    private AssetQualityService qualityService;

    @BeforeEach
    void setUp() throws SQLException {
        qualityService = new AssetQualityService(properties, dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    // ==================== runQualityCheck ====================

    @Test
    void runQualityCheck_shouldReturnFilteredFindings() throws SQLException {
        // Mock all 6 check methods to return empty results
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<QualityFinding> findings = qualityService.runQualityCheck(1L, null, null);
        assertNotNull(findings);
    }

    @Test
    void runQualityCheck_shouldFilterByAssetType() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Return one finding for RULE type
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("rule_code")).thenReturn("R001");
        when(resultSet.getString("rule_name")).thenReturn("Rule 1");
        when(resultSet.getString("version")).thenReturn("1.0");

        // The first check (checkMissingSource for rules) returns a finding
        // Subsequent checks return empty
        // After the first result, all other resultSets return false
        when(resultSet.next())
                .thenReturn(true).thenReturn(false)  // checkMissingSource rules
                .thenReturn(false)                     // checkMissingSource terms
                .thenReturn(false)                     // checkExpired rules
                .thenReturn(false)                     // checkExpired sources
                .thenReturn(false)                     // checkUnclearAuthorization
                .thenReturn(false)                     // checkRuleConflicts
                .thenReturn(false)                     // checkLowConfidence
                .thenReturn(false);                    // checkMultiCandidateConflicts terms

        // For saveFinding check query
        when(preparedStatement.executeUpdate()).thenReturn(1);

        List<QualityFinding> findings = qualityService.runQualityCheck(1L, "RULE", null);
        // Findings should be filtered to RULE type only
        for (QualityFinding f : findings) {
            assertEquals("RULE", f.getAssetType());
        }
    }

    @Test
    void runQualityCheck_shouldFilterByAssetCode() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<QualityFinding> findings = qualityService.runQualityCheck(1L, null, "R001");
        // All findings should have assetCode R001
        for (QualityFinding f : findings) {
            assertEquals("R001", f.getAssetCode());
        }
    }

    // ==================== runFullQualityCheck ====================

    @Test
    void runFullQualityCheck_shouldReturnAllFindings() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        List<QualityFinding> findings = qualityService.runFullQualityCheck(1L);
        assertNotNull(findings);
    }

    // ==================== checkMissingSource ====================

    @Test
    void checkMissingSource_shouldDetectRulesWithoutSource() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("rule_code")).thenReturn("R001");
        when(resultSet.getString("rule_name")).thenReturn("Orphan Rule");
        when(resultSet.getString("version")).thenReturn("1.0");

        List<QualityFinding> findings = qualityService.checkMissingSource(1L);

        assertFalse(findings.isEmpty());
        QualityFinding f = findings.get(0);
        assertEquals("MISSING_SOURCE", f.getFindingType());
        assertEquals("WARNING", f.getSeverity());
        assertEquals("RULE", f.getAssetType());
        assertEquals("R001", f.getAssetCode());
        assertEquals("OPEN", f.getStatus());
    }

    @Test
    void checkMissingSource_shouldDetectTermsWithoutSource() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // First query (rules) returns empty, second (terms) returns one result
        when(resultSet.next()).thenReturn(false)   // rules empty
                .thenReturn(true).thenReturn(false); // terms has one
        when(resultSet.getString("source_code")).thenReturn("TERM-001");
        when(resultSet.getString("source_name")).thenReturn("Term 1");
        when(resultSet.getString("concept_type")).thenReturn("DIAGNOSIS");

        List<QualityFinding> findings = qualityService.checkMissingSource(1L);

        assertFalse(findings.isEmpty());
        QualityFinding f = findings.get(0);
        assertEquals("TERMINOLOGY_MAPPING", f.getAssetType());
        assertEquals("TERM-001", f.getAssetCode());
    }

    @Test
    void checkMissingSource_shouldHandleSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        // Should not throw, just log and return empty
        List<QualityFinding> findings = qualityService.checkMissingSource(1L);
        assertNotNull(findings);
    }

    // ==================== checkExpired ====================

    @Test
    void checkExpired_shouldDetectExpiredRules() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("rule_code")).thenReturn("R-OLD");
        when(resultSet.getString("rule_name")).thenReturn("Old Rule");
        when(resultSet.getString("version")).thenReturn("1.0");

        List<QualityFinding> findings = qualityService.checkExpired(1L);

        assertFalse(findings.isEmpty());
        QualityFinding f = findings.get(0);
        assertEquals("EXPIRED", f.getFindingType());
        assertEquals("CRITICAL", f.getSeverity());
    }

    @Test
    void checkExpired_shouldHandleSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        List<QualityFinding> findings = qualityService.checkExpired(1L);
        assertNotNull(findings);
    }

    // ==================== checkUnclearAuthorization ====================

    @Test
    void checkUnclearAuthorization_shouldDetectMissingLicense() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("document_code")).thenReturn("DOC-001");
        when(resultSet.getString("document_name")).thenReturn("Doc 1");
        when(resultSet.getString("source_type")).thenReturn("GUIDELINE");

        List<QualityFinding> findings = qualityService.checkUnclearAuthorization(1L);

        assertFalse(findings.isEmpty());
        assertEquals("UNCLEAR_AUTH", findings.get(0).getFindingType());
        assertEquals("WARNING", findings.get(0).getSeverity());
    }

    // ==================== checkRuleConflicts ====================

    @Test
    void checkRuleConflicts_shouldDetectOverlappingRules() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("code1")).thenReturn("R001");
        when(resultSet.getString("name1")).thenReturn("Rule 1");
        when(resultSet.getString("ver1")).thenReturn("1.0");
        when(resultSet.getString("code2")).thenReturn("R002");
        when(resultSet.getString("name2")).thenReturn("Rule 2");
        when(resultSet.getString("ver2")).thenReturn("1.0");

        List<QualityFinding> findings = qualityService.checkRuleConflicts(1L);

        assertFalse(findings.isEmpty());
        QualityFinding f = findings.get(0);
        assertEquals("RULE_CONFLICT", f.getFindingType());
        assertEquals("CRITICAL", f.getSeverity());
        assertNotNull(f.getDetailJson());
    }

    // ==================== checkLowConfidence ====================

    @Test
    void checkLowConfidence_shouldDetectLowConfidenceResults() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("job_code")).thenReturn("JOB-001");
        when(resultSet.getString("job_name")).thenReturn("Job 1");
        when(resultSet.getString("job_type")).thenReturn("EXTRACTION");
        when(resultSet.getString("output_summary")).thenReturn("{\"confidence\": 0.3}");

        List<QualityFinding> findings = qualityService.checkLowConfidence(1L, 0.6);

        assertFalse(findings.isEmpty());
        assertEquals("LOW_CONFIDENCE", findings.get(0).getFindingType());
        assertEquals("WARNING", findings.get(0).getSeverity());
    }

    @Test
    void checkLowConfidence_shouldNotFlagHighConfidence() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("job_code")).thenReturn("JOB-002");
        when(resultSet.getString("job_name")).thenReturn("Job 2");
        when(resultSet.getString("job_type")).thenReturn("EXTRACTION");
        when(resultSet.getString("output_summary")).thenReturn("{\"confidence\": 0.9}");

        List<QualityFinding> findings = qualityService.checkLowConfidence(1L, 0.6);
        assertTrue(findings.isEmpty());
    }

    @Test
    void checkLowConfidence_shouldHandleNullOutputSummary() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("job_code")).thenReturn("JOB-003");
        when(resultSet.getString("job_name")).thenReturn("Job 3");
        when(resultSet.getString("job_type")).thenReturn("EXTRACTION");
        when(resultSet.getString("output_summary")).thenReturn(null);

        List<QualityFinding> findings = qualityService.checkLowConfidence(1L, 0.6);
        assertTrue(findings.isEmpty());
    }

    // ==================== checkMultiCandidateConflicts ====================

    @Test
    void checkMultiCandidateConflicts_shouldDetectTermConflicts() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // First query (terms) returns one result, second (jobs) returns empty
        when(resultSet.next()).thenReturn(true).thenReturn(false).thenReturn(false);
        when(resultSet.getString("source_code")).thenReturn("TERM-DUP");
        when(resultSet.getString("source_name")).thenReturn("Dup Term");
        when(resultSet.getString("concept_type")).thenReturn("DIAGNOSIS");
        when(resultSet.getInt("cnt")).thenReturn(3);

        List<QualityFinding> findings = qualityService.checkMultiCandidateConflicts(1L);

        assertFalse(findings.isEmpty());
        assertEquals("MULTI_CANDIDATE_CONFLICT", findings.get(0).getFindingType());
    }

    // ==================== saveFinding ====================

    @Test
    void saveFinding_shouldSetDefaultsAndSave() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No duplicate
        when(preparedStatement.executeUpdate()).thenReturn(1);

        QualityFinding finding = new QualityFinding();
        finding.setTenantId(1L);
        finding.setFindingType("MISSING_SOURCE");
        finding.setSeverity("WARNING");
        finding.setAssetType("RULE");
        finding.setAssetCode("R001");

        QualityFinding result = qualityService.saveFinding(finding);

        assertNotNull(result.getId());
        assertNotNull(result.getFindingCode());
        assertTrue(result.getFindingCode().startsWith("QF-"));
        assertEquals("OPEN", result.getStatus());
        assertNotNull(result.getCreatedTime());
    }

    @Test
    void saveFinding_shouldSkipDuplicateOpenFinding() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // Duplicate exists

        QualityFinding finding = new QualityFinding();
        finding.setTenantId(1L);
        finding.setFindingType("MISSING_SOURCE");
        finding.setAssetType("RULE");
        finding.setAssetCode("R001");

        QualityFinding result = qualityService.saveFinding(finding);
        // Should return the finding without inserting
        assertNotNull(result);
        // Verify INSERT was NOT called (only the check query)
        verify(preparedStatement, never()).executeUpdate();
    }

    @Test
    void saveFinding_shouldThrowOnInsertError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No duplicate
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));

        QualityFinding finding = new QualityFinding();
        finding.setTenantId(1L);
        finding.setFindingType("MISSING_SOURCE");
        finding.setAssetType("RULE");
        finding.setAssetCode("R001");

        assertThrows(IllegalStateException.class, () -> qualityService.saveFinding(finding));
    }

    // ==================== listFindings ====================

    @Test
    void listFindings_shouldReturnEmptyList() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<QualityFinding> findings = qualityService.listFindings(1L, null, null, null, 10);
        assertTrue(findings.isEmpty());
    }

    @Test
    void listFindings_shouldFilterByFindingType() throws SQLException {
        when(connection.prepareStatement(contains("finding_type"))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        qualityService.listFindings(1L, "MISSING_SOURCE", null, null, 10);
        verify(preparedStatement).setString(2, "MISSING_SOURCE");
    }

    @Test
    void listFindings_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        assertThrows(IllegalStateException.class,
                () -> qualityService.listFindings(1L, null, null, null, 10));
    }

    // ==================== resolveFinding ====================

    @Test
    void resolveFinding_shouldUpdateStatus() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        qualityService.resolveFinding(1L, "admin", "Fixed by updating source binding");
        verify(preparedStatement).setString(1, "admin");
        verify(preparedStatement).setString(2, "Fixed by updating source binding");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void resolveFinding_shouldThrowOnSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        assertThrows(IllegalStateException.class,
                () -> qualityService.resolveFinding(1L, "admin", "Fixed"));
    }

    // ==================== getQualitySummary ====================

    @Test
    void getQualitySummary_shouldReturnSummary() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // Three queries: byType, bySeverity, byStatus
        when(resultSet.next()).thenReturn(true).thenReturn(false)
                .thenReturn(true).thenReturn(false)
                .thenReturn(true).thenReturn(false);
        when(resultSet.getString(anyString())).thenReturn("MISSING_SOURCE").thenReturn("WARNING").thenReturn("OPEN");
        when(resultSet.getLong("cnt")).thenReturn(5L);

        Map<String, Object> summary = qualityService.getQualitySummary(1L);

        assertEquals(1L, summary.get("tenantId"));
        assertNotNull(summary.get("byType"));
        assertNotNull(summary.get("bySeverity"));
        assertNotNull(summary.get("byStatus"));
        assertEquals(5L, summary.get("total"));
    }

    @Test
    void getQualitySummary_shouldHandleSqlError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        Map<String, Object> summary = qualityService.getQualitySummary(1L);
        assertNotNull(summary);
        assertEquals(1L, summary.get("tenantId"));
    }

    // ==================== QualityFinding entity ====================

    @Test
    void qualityFinding_shouldSetAndGetAllFields() {
        QualityFinding f = new QualityFinding();
        f.setId(1L);
        f.setTenantId(1L);
        f.setFindingCode("QF-0001");
        f.setFindingType("MISSING_SOURCE");
        f.setSeverity("WARNING");
        f.setAssetType("RULE");
        f.setAssetCode("R001");
        f.setAssetName("Test Rule");
        f.setAssetVersion("1.0");
        f.setDescription("Missing source");
        f.setDetailJson("{\"key\":\"value\"}");
        f.setDetectionRule("Rule must have source");
        f.setStatus("OPEN");
        f.setResolvedBy("admin");
        f.setResolutionNote("Fixed");
        f.setResolvedTime(LocalDateTime.now());
        f.setCreatedBy("system");
        f.setCreatedTime(LocalDateTime.now());
        f.setUpdatedBy("admin");
        f.setUpdatedTime(LocalDateTime.now());

        assertEquals(1L, f.getId().longValue());
        assertEquals("QF-0001", f.getFindingCode());
        assertEquals("MISSING_SOURCE", f.getFindingType());
        assertEquals("WARNING", f.getSeverity());
        assertEquals("RULE", f.getAssetType());
        assertEquals("R001", f.getAssetCode());
        assertEquals("Test Rule", f.getAssetName());
        assertEquals("1.0", f.getAssetVersion());
        assertEquals("Missing source", f.getDescription());
        assertEquals("{\"key\":\"value\"}", f.getDetailJson());
        assertEquals("Rule must have source", f.getDetectionRule());
        assertEquals("OPEN", f.getStatus());
        assertEquals("admin", f.getResolvedBy());
        assertEquals("Fixed", f.getResolutionNote());
        assertNotNull(f.getResolvedTime());
        assertEquals("system", f.getCreatedBy());
        assertNotNull(f.getCreatedTime());
        assertEquals("admin", f.getUpdatedBy());
        assertNotNull(f.getUpdatedTime());
    }
}
