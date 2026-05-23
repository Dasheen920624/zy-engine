package com.medkernel.quality;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptanceTestServiceTest {

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

    private AcceptanceTestService service;

    @BeforeEach
    void setUp() throws SQLException {
        service = new AcceptanceTestService(properties, dataSource);
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    // ==================== createTestCase (persistence disabled) ====================

    @Test
    void shouldReturnTestCase_whenPersistenceDisabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(false);

        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setCaseName("Test Drug Safety");
        testCase.setFeatureCode("CDSS-001");

        AcceptanceTestCase result = service.createTestCase(testCase);
        assertEquals("Test Drug Safety", result.getCaseName());
        verify(dataSource, never()).getConnection();
    }

    // ==================== createTestCase (persistence enabled) ====================

    @Test
    void shouldCreateTestCase_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setTenantId(1L);
        testCase.setCaseCode("TC-001");
        testCase.setCaseName("Test Drug Safety");
        testCase.setFeatureCode("CDSS-001");
        testCase.setFeatureName("CDSS Module");
        testCase.setCategory("SAFETY");
        testCase.setDescription("Verify drug safety checks");
        testCase.setPreconditions("Patient exists");
        testCase.setSteps("1. Place order\n2. Check alert");
        testCase.setExpectedResult("Alert shown");
        testCase.setPriority("HIGH");
        testCase.setStatus("DRAFT");
        testCase.setCreatedBy("admin");
        testCase.setUpdatedBy("admin");

        AcceptanceTestCase result = service.createTestCase(testCase);
        assertNotNull(result);
        assertEquals("Test Drug Safety", result.getCaseName());
        assertEquals("DRAFT", result.getStatus());
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void shouldSetDefaultStatus_whenStatusIsNull() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setTenantId(1L);
        testCase.setCaseCode("TC-002");
        testCase.setCaseName("Test Case");
        testCase.setStatus(null);
        testCase.setCreatedBy("admin");
        testCase.setUpdatedBy("admin");

        AcceptanceTestCase result = service.createTestCase(testCase);
        assertEquals("DRAFT", result.getStatus());
    }

    @Test
    void shouldThrowException_whenCreateTestCaseFails() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setTenantId(1L);
        testCase.setCaseCode("TC-FAIL");
        testCase.setCaseName("Fail Case");
        testCase.setCreatedBy("admin");
        testCase.setUpdatedBy("admin");

        assertThrows(IllegalStateException.class, () -> service.createTestCase(testCase));
    }

    // ==================== updateTestCase (persistence disabled) ====================

    @Test
    void shouldReturnTestCaseOnUpdate_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setId(1L);
        testCase.setCaseName("Updated Case");

        AcceptanceTestCase result = service.updateTestCase(testCase);
        assertEquals("Updated Case", result.getCaseName());
    }

    // ==================== updateTestCase (persistence enabled) ====================

    @Test
    void shouldThrowException_whenTestCaseNotFound() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setId(999L);
        testCase.setTenantId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.updateTestCase(testCase));
    }

    // ==================== listTestCases (persistence disabled) ====================

    @Test
    void shouldReturnEmptyList_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        List<AcceptanceTestCase> result = service.listTestCases(1L, null, null, null);
        assertTrue(result.isEmpty());
    }

    // ==================== listTestCases (persistence enabled) ====================

    @Test
    void shouldListTestCases_withFilters() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("case_code")).thenReturn("TC-001");
        when(resultSet.getString("case_name")).thenReturn("Test Case");
        when(resultSet.getString("feature_code")).thenReturn("CDSS-001");
        when(resultSet.getString("feature_name")).thenReturn("CDSS");
        when(resultSet.getString("category")).thenReturn("SAFETY");
        when(resultSet.getString("description")).thenReturn("Desc");
        when(resultSet.getString("preconditions")).thenReturn("Pre");
        when(resultSet.getString("steps")).thenReturn("Steps");
        when(resultSet.getString("expected_result")).thenReturn("Result");
        when(resultSet.getString("priority")).thenReturn("HIGH");
        when(resultSet.getString("status")).thenReturn("ACTIVE");
        when(resultSet.getString("created_by")).thenReturn("admin");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);

        List<AcceptanceTestCase> result = service.listTestCases(1L, "SAFETY", "CDSS-001", "ACTIVE");
        assertEquals(1, result.size());
        assertEquals("Test Case", result.get(0).getCaseName());
    }

    // ==================== recordResult (persistence disabled) ====================

    @Test
    void shouldReturnResult_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        AcceptanceTestResult testResult = new AcceptanceTestResult();
        testResult.setVerdict("PASS");

        AcceptanceTestResult result = service.recordResult(testResult);
        assertEquals("PASS", result.getVerdict());
    }

    // ==================== recordResult (persistence enabled) ====================

    @Test
    void shouldRecordResult_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        AcceptanceTestResult testResult = new AcceptanceTestResult();
        testResult.setTenantId(1L);
        testResult.setTestCaseId(1L);
        testResult.setCaseCode("TC-001");
        testResult.setCaseName("Test Case");
        testResult.setFeatureCode("CDSS-001");
        testResult.setCategory("SAFETY");
        testResult.setVerdict("PASS");
        testResult.setActualResult("Alert shown as expected");
        testResult.setDeviation("None");
        testResult.setEvidenceRefs("EV-001");
        testResult.setEnvironment("STAGING");
        testResult.setExecutedBy("tester1");
        testResult.setExecutedTime(LocalDateTime.now());

        AcceptanceTestResult result = service.recordResult(testResult);
        assertNotNull(result);
        assertEquals("PASS", result.getVerdict());
        assertEquals("EXECUTED", result.getStatus());
        assertNotNull(result.getResultCode());
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void shouldGenerateResultCode_whenNull() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        AcceptanceTestResult testResult = new AcceptanceTestResult();
        testResult.setTenantId(1L);
        testResult.setCaseCode("TC-001");
        testResult.setVerdict("PASS");
        testResult.setResultCode(null);

        AcceptanceTestResult result = service.recordResult(testResult);
        assertNotNull(result.getResultCode());
        assertTrue(result.getResultCode().startsWith("AT-"));
    }

    @Test
    void shouldThrowException_whenRecordResultFails() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        AcceptanceTestResult testResult = new AcceptanceTestResult();
        testResult.setTenantId(1L);
        testResult.setCaseCode("TC-FAIL");
        testResult.setVerdict("PASS");

        assertThrows(IllegalStateException.class, () -> service.recordResult(testResult));
    }

    // ==================== listResults (persistence disabled) ====================

    @Test
    void shouldReturnEmptyList_whenPersistenceDisabledForResults() {
        when(properties.isEnabled()).thenReturn(false);

        List<AcceptanceTestResult> result = service.listResults(1L, null, null, null);
        assertTrue(result.isEmpty());
    }

    // ==================== reviewResult (persistence disabled) ====================

    @Test
    void shouldReviewResult_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        AcceptanceTestResult result = service.reviewResult(1L, "Dr. Zhang", "Looks good", "REVIEWED");
        assertEquals(1L, result.getId());
        assertEquals("Dr. Zhang", result.getReviewedBy());
        assertEquals("Looks good", result.getReviewNote());
        assertEquals("REVIEWED", result.getStatus());
    }

    // ==================== reviewResult (persistence enabled) ====================

    @Test
    void shouldThrowException_whenReviewResultNotFound() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> service.reviewResult(999L, "Dr. Zhang", "Note", "REVIEWED"));
    }

    // ==================== attachEvidence (persistence disabled) ====================

    @Test
    void shouldReturnEvidence_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        AcceptanceEvidence evidence = new AcceptanceEvidence();
        evidence.setEvidenceType("SCREENSHOT");

        AcceptanceEvidence result = service.attachEvidence(evidence);
        assertEquals("SCREENSHOT", result.getEvidenceType());
    }

    // ==================== attachEvidence (persistence enabled) ====================

    @Test
    void shouldAttachEvidence_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        AcceptanceEvidence evidence = new AcceptanceEvidence();
        evidence.setTenantId(1L);
        evidence.setResultCode("AT-001");
        evidence.setCaseCode("TC-001");
        evidence.setEvidenceType("SCREENSHOT");
        evidence.setDescription("Screenshot of alert");
        evidence.setFilePath("/evidence/screenshot.png");
        evidence.setFileHash("abc123");
        evidence.setFileSize(1024L);
        evidence.setMimeType("image/png");
        evidence.setCapturedBy("tester1");
        evidence.setCapturedTime(LocalDateTime.now());

        AcceptanceEvidence result = service.attachEvidence(evidence);
        assertNotNull(result);
        assertEquals("SCREENSHOT", result.getEvidenceType());
        assertNotNull(result.getEvidenceCode());
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void shouldGenerateEvidenceCode_whenNull() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        AcceptanceEvidence evidence = new AcceptanceEvidence();
        evidence.setTenantId(1L);
        evidence.setResultCode("AT-001");
        evidence.setCaseCode("TC-001");
        evidence.setEvidenceType("LOG");
        evidence.setEvidenceCode(null);

        AcceptanceEvidence result = service.attachEvidence(evidence);
        assertNotNull(result.getEvidenceCode());
        assertTrue(result.getEvidenceCode().startsWith("EV-"));
    }

    @Test
    void shouldThrowException_whenAttachEvidenceFails() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        AcceptanceEvidence evidence = new AcceptanceEvidence();
        evidence.setTenantId(1L);
        evidence.setResultCode("AT-001");
        evidence.setCaseCode("TC-001");
        evidence.setEvidenceType("LOG");

        assertThrows(IllegalStateException.class, () -> service.attachEvidence(evidence));
    }

    // ==================== listEvidence (persistence disabled) ====================

    @Test
    void shouldReturnEmptyList_whenPersistenceDisabledForEvidence() {
        when(properties.isEnabled()).thenReturn(false);

        List<AcceptanceEvidence> result = service.listEvidence(1L, null);
        assertTrue(result.isEmpty());
    }

    // ==================== getAcceptanceSummary (persistence disabled) ====================

    @Test
    void shouldReturnDefaultSummary_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        Map<String, Object> summary = service.getAcceptanceSummary(1L, null);
        assertEquals(1L, summary.get("tenantId"));
        assertEquals(0, summary.get("totalCases"));
        assertEquals(0, summary.get("totalResults"));
        assertNotNull(summary.get("verdictDistribution"));
        assertNotNull(summary.get("statusDistribution"));
    }

    // ==================== getAcceptanceSummary (persistence enabled) ====================

    @Test
    void shouldReturnSummary_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Map<String, Object> summary = service.getAcceptanceSummary(1L, null);
        assertEquals(0, summary.get("totalCases"));
    }

    @Test
    void shouldReturnSummary_withData() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // First query: case count (one row)
        // Second query: result count with verdict/status (one row)
        when(resultSet.next()).thenReturn(true, false, true, false);
        when(resultSet.getInt("total")).thenReturn(10);
        when(resultSet.getInt("active_count")).thenReturn(8);
        lenient().when(resultSet.getString("verdict")).thenReturn("PASS");
        lenient().when(resultSet.getString("status")).thenReturn("EXECUTED");
        lenient().when(resultSet.getInt("cnt")).thenReturn(5);

        Map<String, Object> summary = service.getAcceptanceSummary(1L, "CDSS-001");
        assertEquals(10, summary.get("totalCases"));
        assertEquals(8, summary.get("activeCases"));
        assertNotNull(summary.get("totalResults"));
        assertNotNull(summary.get("verdictDistribution"));
        assertNotNull(summary.get("statusDistribution"));
    }

    // ==================== generateAcceptanceReport (persistence disabled) ====================

    @Test
    void shouldGenerateReport_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        Map<String, Object> report = service.generateAcceptanceReport(1L, "CDSS-001");
        assertEquals(1L, report.get("tenantId"));
        assertEquals("CDSS-001", report.get("featureCode"));
        assertNotNull(report.get("generatedTime"));
        assertNotNull(report.get("summary"));
    }

    // ==================== generateAcceptanceReport (persistence enabled) ====================

    @Test
    void shouldGenerateReport_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Map<String, Object> report = service.generateAcceptanceReport(1L, null);
        assertNotNull(report);
        assertNotNull(report.get("generatedTime"));
    }

    // ==================== updateTestCase (persistence enabled, success) ====================

    @Test
    void shouldUpdateTestCase_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setId(1L);
        testCase.setTenantId(1L);
        testCase.setCaseName("Updated Case");
        testCase.setUpdatedBy("admin");

        AcceptanceTestCase result = service.updateTestCase(testCase);
        assertNotNull(result);
        assertEquals("Updated Case", result.getCaseName());
    }

    // ==================== reviewResult (persistence enabled, success) ====================

    @Test
    void shouldReviewResult_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("result_code")).thenReturn("AT-001");
        when(resultSet.getString("case_code")).thenReturn("TC-001");
        when(resultSet.getString("case_name")).thenReturn("Test Case");
        when(resultSet.getString("feature_code")).thenReturn("CDSS-001");
        when(resultSet.getString("category")).thenReturn("SAFETY");
        when(resultSet.getString("verdict")).thenReturn("PASS");
        when(resultSet.getString("actual_result")).thenReturn("OK");
        when(resultSet.getString("deviation")).thenReturn(null);
        when(resultSet.getString("evidence_refs")).thenReturn(null);
        when(resultSet.getString("environment")).thenReturn("STAGING");
        when(resultSet.getString("status")).thenReturn("REVIEWED");
        when(resultSet.getString("reviewed_by")).thenReturn("Dr. Zhang");
        when(resultSet.getTimestamp("reviewed_time")).thenReturn(null);
        when(resultSet.getString("review_note")).thenReturn("Good");
        when(resultSet.getString("executed_by")).thenReturn("tester1");
        when(resultSet.getTimestamp("executed_time")).thenReturn(null);
        when(resultSet.getTimestamp("created_time")).thenReturn(null);

        AcceptanceTestResult result = service.reviewResult(1L, "Dr. Zhang", "Good", "REVIEWED");
        assertNotNull(result);
        assertEquals("REVIEWED", result.getStatus());
    }

    // ==================== listResults (persistence enabled) ====================

    @Test
    void shouldListResults_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("result_code")).thenReturn("AT-001");
        when(resultSet.getString("case_code")).thenReturn("TC-001");
        when(resultSet.getString("case_name")).thenReturn("Test Case");
        when(resultSet.getString("feature_code")).thenReturn("CDSS-001");
        when(resultSet.getString("category")).thenReturn("SAFETY");
        when(resultSet.getString("verdict")).thenReturn("PASS");
        when(resultSet.getString("actual_result")).thenReturn("OK");
        when(resultSet.getString("deviation")).thenReturn(null);
        when(resultSet.getString("evidence_refs")).thenReturn(null);
        when(resultSet.getString("environment")).thenReturn("STAGING");
        when(resultSet.getString("status")).thenReturn("EXECUTED");
        when(resultSet.getString("reviewed_by")).thenReturn(null);
        when(resultSet.getTimestamp("reviewed_time")).thenReturn(null);
        when(resultSet.getString("review_note")).thenReturn(null);
        when(resultSet.getString("executed_by")).thenReturn("tester1");
        when(resultSet.getTimestamp("executed_time")).thenReturn(null);
        when(resultSet.getTimestamp("created_time")).thenReturn(null);

        List<AcceptanceTestResult> result = service.listResults(1L, "TC-001", "PASS", "EXECUTED");
        assertEquals(1, result.size());
        assertEquals("AT-001", result.get(0).getResultCode());
    }

    // ==================== listEvidence (persistence enabled) ====================

    @Test
    void shouldListEvidence_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("evidence_code")).thenReturn("EV-001");
        when(resultSet.getString("result_code")).thenReturn("AT-001");
        when(resultSet.getString("case_code")).thenReturn("TC-001");
        when(resultSet.getString("evidence_type")).thenReturn("SCREENSHOT");
        when(resultSet.getString("description")).thenReturn("Screenshot");
        when(resultSet.getString("file_path")).thenReturn("/evidence/screenshot.png");
        when(resultSet.getString("file_hash")).thenReturn("abc123");
        when(resultSet.getLong("file_size")).thenReturn(1024L);
        when(resultSet.getString("mime_type")).thenReturn("image/png");
        when(resultSet.getString("captured_by")).thenReturn("tester1");
        when(resultSet.getTimestamp("captured_time")).thenReturn(null);
        when(resultSet.getTimestamp("created_time")).thenReturn(null);

        List<AcceptanceEvidence> result = service.listEvidence(1L, "AT-001");
        assertEquals(1, result.size());
        assertEquals("EV-001", result.get(0).getEvidenceCode());
    }

    // ==================== Oracle dialect ====================

    @Test
    void shouldUseOracleTimestamp_whenNotLocalFileDatabase() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(false);

        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setTenantId(1L);
        testCase.setCaseCode("TC-ORA");
        testCase.setCaseName("Oracle Test");
        testCase.setCreatedBy("admin");
        testCase.setUpdatedBy("admin");

        service.createTestCase(testCase);
        verify(connection).prepareStatement(contains("SYSTIMESTAMP"));
    }
}
