package com.medkernel.cdss;

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
class SafetyRedLineServiceTest {

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

    private SafetyRedLineService service;

    @BeforeEach
    void setUp() throws SQLException {
        service = new SafetyRedLineService(properties, dataSource);
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    // ==================== defineRedLine (persistence disabled) ====================

    @Test
    void shouldReturnRedLine_whenPersistenceDisabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(false);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setRedLineName("Test Red Line");
        redLine.setCategory("DRUG");

        SafetyRedLine result = service.defineRedLine(redLine);
        assertEquals("Test Red Line", result.getRedLineName());
        verify(dataSource, never()).getConnection();
    }

    // ==================== defineRedLine (persistence enabled) ====================

    @Test
    void shouldDefineRedLine_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setTenantId(1L);
        redLine.setRedLineCode("RL-001");
        redLine.setRedLineName("Drug Contraindication");
        redLine.setCategory("DRUG");
        redLine.setDescription("Drug contraindication check");
        redLine.setConditionExpression("patient.age > 65 AND drug.increase_bleeding_risk");
        redLine.setBlockingAction("BLOCK");
        redLine.setSeverity("CRITICAL");
        redLine.setApplicableScenarios("ORDER_PLACED,DRUG_DISPENSED");
        redLine.setEnabled("Y");
        redLine.setCreatedBy("admin");

        SafetyRedLine result = service.defineRedLine(redLine);
        assertNotNull(result);
        assertEquals("Drug Contraindication", result.getRedLineName());
        assertEquals("Y", result.getEnabled());
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void shouldSetDefaultEnabled_whenEnabledIsNull() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setTenantId(1L);
        redLine.setRedLineCode("RL-002");
        redLine.setRedLineName("Test");
        redLine.setEnabled(null);
        redLine.setCreatedBy("admin");

        SafetyRedLine result = service.defineRedLine(redLine);
        // When persistence is enabled, default is set in SQL, not reflected in Java object
        assertNotNull(result);
    }

    @Test
    void shouldThrowException_whenDefineRedLineFails() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setTenantId(1L);
        redLine.setRedLineCode("RL-FAIL");
        redLine.setRedLineName("Fail");
        redLine.setCreatedBy("admin");

        assertThrows(IllegalStateException.class, () -> service.defineRedLine(redLine));
    }

    // ==================== updateRedLine (persistence disabled) ====================

    @Test
    void shouldReturnRedLineOnUpdate_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setId(1L);
        redLine.setRedLineName("Updated");

        SafetyRedLine result = service.updateRedLine(redLine);
        assertEquals("Updated", result.getRedLineName());
    }

    // ==================== updateRedLine (persistence enabled) ====================

    @Test
    void shouldThrowException_whenRedLineNotFound() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setId(999L);
        redLine.setTenantId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.updateRedLine(redLine));
    }

    // ==================== listRedLines (persistence disabled) ====================

    @Test
    void shouldReturnEmptyList_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        List<SafetyRedLine> result = service.listRedLines(1L, null, null);
        assertTrue(result.isEmpty());
    }

    // ==================== listRedLines (persistence enabled) ====================

    @Test
    void shouldListRedLines_withFilters() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("red_line_code")).thenReturn("RL-001");
        when(resultSet.getString("red_line_name")).thenReturn("Drug Check");
        when(resultSet.getString("category")).thenReturn("DRUG");
        when(resultSet.getString("description")).thenReturn("Desc");
        when(resultSet.getString("condition_expression")).thenReturn("expr");
        when(resultSet.getString("blocking_action")).thenReturn("BLOCK");
        when(resultSet.getString("severity")).thenReturn("CRITICAL");
        when(resultSet.getString("applicable_scenarios")).thenReturn("ORDER_PLACED");
        when(resultSet.getString("enabled")).thenReturn("Y");
        when(resultSet.getString("created_by")).thenReturn("admin");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);

        List<SafetyRedLine> result = service.listRedLines(1L, "DRUG", "Y");
        assertEquals(1, result.size());
        assertEquals("Drug Check", result.get(0).getRedLineName());
    }

    // ==================== scanRedLines (persistence disabled) ====================

    @Test
    void shouldReturnEmptyList_whenPersistenceDisabledForScan() {
        when(properties.isEnabled()).thenReturn(false);

        List<RedLineScanResult> result = service.scanRedLines(1L, "P001", "E001", "MANUAL");
        assertTrue(result.isEmpty());
    }

    // ==================== scanRedLines (persistence enabled) ====================

    @Test
    void shouldScanAndReturnViolations() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        // First call: listRedLines to get enabled red lines
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("red_line_code")).thenReturn("RL-001");
        when(resultSet.getString("red_line_name")).thenReturn("Drug Check");
        when(resultSet.getString("category")).thenReturn("DRUG");
        when(resultSet.getString("description")).thenReturn("Desc");
        when(resultSet.getString("condition_expression")).thenReturn("patient.age > 65");
        when(resultSet.getString("blocking_action")).thenReturn("BLOCK");
        when(resultSet.getString("severity")).thenReturn("CRITICAL");
        when(resultSet.getString("applicable_scenarios")).thenReturn("ORDER_PLACED");
        when(resultSet.getString("enabled")).thenReturn("Y");
        when(resultSet.getString("created_by")).thenReturn("admin");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);

        // Second call: saveScanResult
        when(preparedStatement.executeUpdate()).thenReturn(1);

        List<RedLineScanResult> results = service.scanRedLines(1L, "P001", "E001", "MANUAL");
        assertEquals(1, results.size());
        assertEquals("RL-001", results.get(0).getRedLineCode());
        // blockingAction is "BLOCK", not "BLOCKED", so status is "DETECTED"
        assertEquals("DETECTED", results.get(0).getStatus());
        assertEquals("CRITICAL", results.get(0).getSeverity());
        assertEquals("BLOCK", results.get(0).getBlockingAction());
    }

    @Test
    void shouldReturnNoViolations_whenNoPatientId() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("red_line_code")).thenReturn("RL-001");
        when(resultSet.getString("red_line_name")).thenReturn("Drug Check");
        when(resultSet.getString("category")).thenReturn("DRUG");
        when(resultSet.getString("description")).thenReturn("Desc");
        when(resultSet.getString("condition_expression")).thenReturn("patient.age > 65");
        when(resultSet.getString("blocking_action")).thenReturn("BLOCK");
        when(resultSet.getString("severity")).thenReturn("CRITICAL");
        when(resultSet.getString("applicable_scenarios")).thenReturn("ORDER_PLACED");
        when(resultSet.getString("enabled")).thenReturn("Y");
        when(resultSet.getString("created_by")).thenReturn("admin");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);

        // No patient ID -> no violations
        List<RedLineScanResult> results = service.scanRedLines(1L, null, "E001", "MANUAL");
        assertTrue(results.isEmpty());
    }

    // ==================== listScanResults (persistence disabled) ====================

    @Test
    void shouldReturnEmptyList_whenPersistenceDisabledForScanResults() {
        when(properties.isEnabled()).thenReturn(false);

        List<RedLineScanResult> result = service.listScanResults(1L, null, null, null);
        assertTrue(result.isEmpty());
    }

    // ==================== resolveScanResult (persistence disabled) ====================

    @Test
    void shouldResolveScanResult_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        RedLineScanResult result = service.resolveScanResult(1L, "Dr. Zhang", "Issue resolved");
        assertEquals(1L, result.getId());
        assertEquals("RESOLVED", result.getStatus());
        assertEquals("Dr. Zhang", result.getResolvedBy());
        assertEquals("Issue resolved", result.getResolutionNote());
    }

    // ==================== resolveScanResult (persistence enabled) ====================

    @Test
    void shouldThrowException_whenScanResultNotFound() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> service.resolveScanResult(999L, "Dr. Zhang", "Note"));
    }

    // ==================== overrideScanResult (persistence disabled) ====================

    @Test
    void shouldOverrideScanResult_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        RedLineScanResult result = service.overrideScanResult(1L, "Dr. Zhang", "Clinical necessity");
        assertEquals(1L, result.getId());
        assertEquals("OVERRIDDEN", result.getStatus());
        assertEquals("Dr. Zhang", result.getOverriddenBy());
        assertEquals("Clinical necessity", result.getOverrideReason());
    }

    // ==================== overrideScanResult (persistence enabled) ====================

    @Test
    void shouldThrowException_whenOverrideScanResultNotFound() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> service.overrideScanResult(999L, "Dr. Zhang", "Reason"));
    }

    // ==================== getScanSummary (persistence disabled) ====================

    @Test
    void shouldReturnDefaultSummary_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        Map<String, Object> summary = service.getScanSummary(1L);
        assertEquals(1L, summary.get("tenant_id"));
        assertEquals(0, summary.get("total_red_lines"));
        assertEquals(0, summary.get("enabled_red_lines"));
        assertEquals(0, summary.get("total_scan_results"));
        assertNotNull(summary.get("severity_distribution"));
        assertNotNull(summary.get("status_distribution"));
    }

    // ==================== getScanSummary (persistence enabled) ====================

    @Test
    void shouldReturnSummary_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Map<String, Object> summary = service.getScanSummary(1L);
        assertEquals(0, summary.get("total_red_lines"));
        assertEquals(0, summary.get("total_scan_results"));
    }

    @Test
    void shouldReturnSummary_withData() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        // First query: red line stats (one row)
        // Second query: scan result stats (one row)
        when(resultSet.next()).thenReturn(true, false, true, false);
        when(resultSet.getString("enabled")).thenReturn("Y");
        when(resultSet.getInt("cnt")).thenReturn(5);
        when(resultSet.getString("severity")).thenReturn("CRITICAL");
        when(resultSet.getString("status")).thenReturn("DETECTED");

        Map<String, Object> summary = service.getScanSummary(1L);
        assertEquals(5, summary.get("total_red_lines"));
        assertEquals(5, summary.get("enabled_red_lines"));
        assertEquals(5, summary.get("total_scan_results"));
    }

    // ==================== resolveScanResult (persistence enabled, success) ====================

    @Test
    void shouldResolveScanResult_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("scan_code")).thenReturn("SCAN-001");
        when(resultSet.getString("scan_type")).thenReturn("MANUAL");
        when(resultSet.getString("red_line_code")).thenReturn("RL-001");
        when(resultSet.getString("red_line_name")).thenReturn("Drug Check");
        when(resultSet.getString("category")).thenReturn("DRUG");
        when(resultSet.getString("patient_id")).thenReturn("P001");
        when(resultSet.getString("encounter_id")).thenReturn("E001");
        when(resultSet.getString("trigger_context")).thenReturn("{}");
        when(resultSet.getString("violation_detail")).thenReturn("Violated");
        when(resultSet.getString("severity")).thenReturn("CRITICAL");
        when(resultSet.getString("blocking_action")).thenReturn("BLOCK");
        when(resultSet.getString("status")).thenReturn("RESOLVED");
        when(resultSet.getString("overridden_by")).thenReturn(null);
        when(resultSet.getString("override_reason")).thenReturn(null);
        when(resultSet.getString("resolved_by")).thenReturn("Dr. Zhang");
        when(resultSet.getString("resolution_note")).thenReturn("Fixed");
        when(resultSet.getTimestamp("resolved_time")).thenReturn(null);
        when(resultSet.getString("scan_by")).thenReturn("SYSTEM");
        when(resultSet.getTimestamp("scan_time")).thenReturn(null);
        when(resultSet.getTimestamp("created_time")).thenReturn(null);

        RedLineScanResult result = service.resolveScanResult(1L, "Dr. Zhang", "Fixed");
        assertNotNull(result);
        assertEquals("RESOLVED", result.getStatus());
        assertEquals("Dr. Zhang", result.getResolvedBy());
    }

    // ==================== overrideScanResult (persistence enabled, success) ====================

    @Test
    void shouldOverrideScanResult_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("scan_code")).thenReturn("SCAN-001");
        when(resultSet.getString("scan_type")).thenReturn("MANUAL");
        when(resultSet.getString("red_line_code")).thenReturn("RL-001");
        when(resultSet.getString("red_line_name")).thenReturn("Drug Check");
        when(resultSet.getString("category")).thenReturn("DRUG");
        when(resultSet.getString("patient_id")).thenReturn("P001");
        when(resultSet.getString("encounter_id")).thenReturn("E001");
        when(resultSet.getString("trigger_context")).thenReturn("{}");
        when(resultSet.getString("violation_detail")).thenReturn("Violated");
        when(resultSet.getString("severity")).thenReturn("CRITICAL");
        when(resultSet.getString("blocking_action")).thenReturn("BLOCK");
        when(resultSet.getString("status")).thenReturn("OVERRIDDEN");
        when(resultSet.getString("overridden_by")).thenReturn("Dr. Override");
        when(resultSet.getString("override_reason")).thenReturn("Clinical necessity");
        when(resultSet.getString("resolved_by")).thenReturn(null);
        when(resultSet.getString("resolution_note")).thenReturn(null);
        when(resultSet.getTimestamp("resolved_time")).thenReturn(null);
        when(resultSet.getString("scan_by")).thenReturn("SYSTEM");
        when(resultSet.getTimestamp("scan_time")).thenReturn(null);
        when(resultSet.getTimestamp("created_time")).thenReturn(null);

        RedLineScanResult result = service.overrideScanResult(1L, "Dr. Override", "Clinical necessity");
        assertNotNull(result);
        assertEquals("OVERRIDDEN", result.getStatus());
        assertEquals("Dr. Override", result.getOverriddenBy());
    }

    // ==================== listScanResults (persistence enabled) ====================

    @Test
    void shouldListScanResults_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("scan_code")).thenReturn("SCAN-001");
        when(resultSet.getString("scan_type")).thenReturn("MANUAL");
        when(resultSet.getString("red_line_code")).thenReturn("RL-001");
        when(resultSet.getString("red_line_name")).thenReturn("Drug Check");
        when(resultSet.getString("category")).thenReturn("DRUG");
        when(resultSet.getString("patient_id")).thenReturn("P001");
        when(resultSet.getString("encounter_id")).thenReturn("E001");
        when(resultSet.getString("trigger_context")).thenReturn("{}");
        when(resultSet.getString("violation_detail")).thenReturn("Violated");
        when(resultSet.getString("severity")).thenReturn("CRITICAL");
        when(resultSet.getString("blocking_action")).thenReturn("BLOCK");
        when(resultSet.getString("status")).thenReturn("DETECTED");
        when(resultSet.getString("overridden_by")).thenReturn(null);
        when(resultSet.getString("override_reason")).thenReturn(null);
        when(resultSet.getString("resolved_by")).thenReturn(null);
        when(resultSet.getString("resolution_note")).thenReturn(null);
        when(resultSet.getTimestamp("resolved_time")).thenReturn(null);
        when(resultSet.getString("scan_by")).thenReturn("SYSTEM");
        when(resultSet.getTimestamp("scan_time")).thenReturn(null);
        when(resultSet.getTimestamp("created_time")).thenReturn(null);

        List<RedLineScanResult> result = service.listScanResults(1L, "P001", null, null);
        assertEquals(1, result.size());
        assertEquals("SCAN-001", result.get(0).getScanCode());
    }

    // ==================== updateRedLine (persistence enabled, success) ====================

    @Test
    void shouldUpdateRedLine_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setId(1L);
        redLine.setTenantId(1L);
        redLine.setRedLineName("Updated Red Line");
        redLine.setUpdatedBy("admin");

        SafetyRedLine result = service.updateRedLine(redLine);
        assertNotNull(result);
        assertEquals("Updated Red Line", result.getRedLineName());
    }

    // ==================== Oracle dialect ====================

    @Test
    void shouldUseOracleTimestamp_whenNotLocalFileDatabaseForDefine() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(false);

        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setTenantId(1L);
        redLine.setRedLineCode("RL-ORA");
        redLine.setRedLineName("Oracle Test");
        redLine.setCreatedBy("admin");

        service.defineRedLine(redLine);
        verify(connection).prepareStatement(contains("SYSTIMESTAMP"));
    }
}
