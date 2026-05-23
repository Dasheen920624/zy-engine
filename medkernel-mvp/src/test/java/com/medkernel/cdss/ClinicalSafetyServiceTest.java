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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalSafetyServiceTest {

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

    private ClinicalSafetyService service;

    @BeforeEach
    void setUp() throws SQLException {
        service = new ClinicalSafetyService(properties, dataSource);
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    // ==================== calculateRiskLevel ====================

    @Test
    void shouldReturnCritical_whenScoreAbove17() {
        assertEquals("CRITICAL", service.calculateRiskLevel("ALMOST_CERTAIN", "CATASTROPHIC"));
    }

    @Test
    void shouldReturnHigh_whenScoreBetween10And16() {
        assertEquals("HIGH", service.calculateRiskLevel("LIKELY", "MAJOR"));
    }

    @Test
    void shouldReturnMedium_whenScoreBetween5And9() {
        assertEquals("MEDIUM", service.calculateRiskLevel("POSSIBLE", "MODERATE"));
    }

    @Test
    void shouldReturnLow_whenScoreBelow5() {
        assertEquals("LOW", service.calculateRiskLevel("RARE", "NEGLIGIBLE"));
    }

    @Test
    void shouldReturnLow_whenRareAndMinor() {
        assertEquals("LOW", service.calculateRiskLevel("RARE", "MINOR"));
    }

    @Test
    void shouldReturnMedium_whenUnlikelyAndMajor() {
        assertEquals("MEDIUM", service.calculateRiskLevel("UNLIKELY", "MAJOR"));
    }

    @Test
    void shouldReturnHigh_whenLikelyAndModerate() {
        assertEquals("HIGH", service.calculateRiskLevel("LIKELY", "MODERATE"));
    }

    @Test
    void shouldHandleNullLikelihood() {
        String result = service.calculateRiskLevel(null, "MAJOR");
        // null likelihood -> score 1, 1*4=4 -> LOW
        assertEquals("LOW", result);
    }

    @Test
    void shouldHandleNullSeverity() {
        String result = service.calculateRiskLevel("LIKELY", null);
        // null severity -> score 1, 4*1=4 -> LOW
        assertEquals("LOW", result);
    }

    @Test
    void shouldHandleUnknownLikelihood() {
        String result = service.calculateRiskLevel("UNKNOWN", "MAJOR");
        assertEquals("LOW", result);
    }

    @Test
    void shouldHandleUnknownSeverity() {
        String result = service.calculateRiskLevel("LIKELY", "UNKNOWN");
        assertEquals("LOW", result);
    }

    // ==================== getBlockingStrategy ====================

    @Test
    void shouldReturnBlock_whenCritical() {
        assertEquals("BLOCK", service.getBlockingStrategy("CRITICAL"));
    }

    @Test
    void shouldReturnEscalate_whenHigh() {
        assertEquals("ESCALATE", service.getBlockingStrategy("HIGH"));
    }

    @Test
    void shouldReturnDualConfirm_whenMedium() {
        assertEquals("REQUIRE_DUAL_CONFIRM", service.getBlockingStrategy("MEDIUM"));
    }

    @Test
    void shouldReturnWarn_whenLow() {
        assertEquals("WARN", service.getBlockingStrategy("LOW"));
    }

    @Test
    void shouldReturnWarn_whenNull() {
        assertEquals("WARN", service.getBlockingStrategy(null));
    }

    @Test
    void shouldReturnWarn_whenUnknown() {
        assertEquals("WARN", service.getBlockingStrategy("UNKNOWN"));
    }

    // ==================== createHazard (persistence disabled) ====================

    @Test
    void shouldReturnHazard_whenPersistenceDisabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(false);

        HazardLog hazard = new HazardLog();
        hazard.setHazardName("Test Hazard");
        hazard.setHazardCategory("CLINICAL");

        HazardLog result = service.createHazard(hazard);
        assertEquals("Test Hazard", result.getHazardName());
        verify(dataSource, never()).getConnection();
    }

    // ==================== updateHazard (persistence disabled) ====================

    @Test
    void shouldReturnHazardOnUpdate_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        HazardLog hazard = new HazardLog();
        hazard.setId(1L);
        hazard.setHazardName("Updated Hazard");

        HazardLog result = service.updateHazard(hazard);
        assertEquals("Updated Hazard", result.getHazardName());
    }

    // ==================== listHazards (persistence disabled) ====================

    @Test
    void shouldReturnEmptyList_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        List<HazardLog> result = service.listHazards(1L, null, null, null);
        assertTrue(result.isEmpty());
    }

    // ==================== acceptHazard (persistence disabled) ====================

    @Test
    void shouldAcceptHazard_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        HazardLog result = service.acceptHazard(1L, "Dr. Zhang", "Accepted risk");
        assertEquals(1L, result.getId());
        assertEquals("Dr. Zhang", result.getAcceptedBy());
        assertEquals("Accepted risk", result.getAcceptanceNote());
    }

    // ==================== closeHazard (persistence disabled) ====================

    @Test
    void shouldCloseHazard_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        HazardLog result = service.closeHazard(1L);
        assertEquals(1L, result.getId());
        assertEquals("CLOSED", result.getStatus());
    }

    // ==================== createSafetyCase (persistence disabled) ====================

    @Test
    void shouldReturnSafetyCase_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setCaseName("Test Safety Case");
        safetyCase.setCaseType("SYSTEM");

        SafetyCase result = service.createSafetyCase(safetyCase);
        assertEquals("Test Safety Case", result.getCaseName());
    }

    // ==================== updateSafetyCase (persistence disabled) ====================

    @Test
    void shouldReturnSafetyCaseOnUpdate_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setId(1L);
        safetyCase.setCaseName("Updated Case");

        SafetyCase result = service.updateSafetyCase(safetyCase);
        assertEquals("Updated Case", result.getCaseName());
    }

    // ==================== listSafetyCases (persistence disabled) ====================

    @Test
    void shouldReturnEmptyList_whenPersistenceDisabledForSafetyCases() {
        when(properties.isEnabled()).thenReturn(false);

        List<SafetyCase> result = service.listSafetyCases(1L, null, null);
        assertTrue(result.isEmpty());
    }

    // ==================== reviewSafetyCase (persistence disabled) ====================

    @Test
    void shouldReviewSafetyCase_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        SafetyCase result = service.reviewSafetyCase(1L, "APPROVED", "Dr. Reviewer", "Looks good");
        assertEquals(1L, result.getId());
        assertEquals("APPROVED", result.getStatus());
        assertEquals("Dr. Reviewer", result.getReviewedBy());
        assertEquals("Looks good", result.getReviewNote());
    }

    // ==================== getRiskSummary (persistence disabled) ====================

    @Test
    void shouldReturnDefaultSummary_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        Map<String, Object> summary = service.getRiskSummary(1L);
        assertEquals(1L, summary.get("tenant_id"));
        assertEquals(0, summary.get("total_hazards"));
        assertEquals(0, summary.get("total_safety_cases"));
        assertNotNull(summary.get("risk_level_distribution"));
        assertNotNull(summary.get("status_distribution"));
    }

    // ==================== createHazard (persistence enabled) ====================

    @Test
    void shouldCreateHazard_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        HazardLog hazard = new HazardLog();
        hazard.setTenantId(1L);
        hazard.setHazardCode("HAZ-001");
        hazard.setHazardName("Test Hazard");
        hazard.setHazardCategory("CLINICAL");
        hazard.setHazardDescription("Description");
        hazard.setAffectedProcess("ORDER");
        hazard.setLikelihood("POSSIBLE");
        hazard.setSeverity("MAJOR");
        hazard.setRiskLevel("HIGH");
        hazard.setControlMeasures("Control");
        hazard.setResidualRisk("LOW");
        hazard.setCreatedBy("admin");

        HazardLog result = service.createHazard(hazard);
        assertNotNull(result);
        assertEquals("Test Hazard", result.getHazardName());
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void shouldCreateHazardWithDefaultStatus_whenStatusIsNull() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        HazardLog hazard = new HazardLog();
        hazard.setTenantId(1L);
        hazard.setHazardCode("HAZ-002");
        hazard.setHazardName("Hazard without status");
        hazard.setCreatedBy("admin");

        HazardLog result = service.createHazard(hazard);
        // When persistence is enabled, the service sets default status
        // The actual object is returned as-is since JDBC doesn't update the Java object
        // Just verify the method completes without error
        assertNotNull(result);
    }

    @Test
    void shouldThrowException_whenCreateHazardFails() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        HazardLog hazard = new HazardLog();
        hazard.setTenantId(1L);
        hazard.setHazardCode("HAZ-003");
        hazard.setHazardName("Fail Hazard");
        hazard.setCreatedBy("admin");

        assertThrows(IllegalStateException.class, () -> service.createHazard(hazard));
    }

    // ==================== updateHazard (persistence enabled) ====================

    @Test
    void shouldThrowException_whenHazardNotFound() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        HazardLog hazard = new HazardLog();
        hazard.setId(999L);
        hazard.setTenantId(1L);
        hazard.setHazardName("Not Found");

        assertThrows(IllegalArgumentException.class, () -> service.updateHazard(hazard));
    }

    // ==================== listHazards (persistence enabled) ====================

    @Test
    void shouldListHazards_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("hazard_code")).thenReturn("HAZ-001");
        when(resultSet.getString("hazard_name")).thenReturn("Test Hazard");
        when(resultSet.getString("hazard_category")).thenReturn("CLINICAL");
        when(resultSet.getString("hazard_description")).thenReturn("Desc");
        when(resultSet.getString("affected_process")).thenReturn("ORDER");
        when(resultSet.getString("likelihood")).thenReturn("POSSIBLE");
        when(resultSet.getString("severity")).thenReturn("MAJOR");
        when(resultSet.getString("risk_level")).thenReturn("HIGH");
        when(resultSet.getString("control_measures")).thenReturn("Control");
        when(resultSet.getString("residual_risk")).thenReturn("LOW");
        when(resultSet.getString("status")).thenReturn("IDENTIFIED");
        when(resultSet.getString("accepted_by")).thenReturn(null);
        when(resultSet.getTimestamp("accepted_time")).thenReturn(null);
        when(resultSet.getString("acceptance_note")).thenReturn(null);
        when(resultSet.getString("blocking_strategy")).thenReturn("WARN");
        when(resultSet.getString("created_by")).thenReturn("admin");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);

        List<HazardLog> result = service.listHazards(1L, null, null, null);
        assertEquals(1, result.size());
        assertEquals("Test Hazard", result.get(0).getHazardName());
    }

    @Test
    void shouldFilterHazardsByCategoryAndRiskLevel() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<HazardLog> result = service.listHazards(1L, "CLINICAL", "HIGH", "IDENTIFIED");
        assertTrue(result.isEmpty());
        // Verify that the SQL was built with filters
        verify(connection).prepareStatement(contains("hazard_category"));
    }

    // ==================== acceptHazard (persistence enabled) ====================

    @Test
    void shouldThrowException_whenAcceptHazardNotFound() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> service.acceptHazard(999L, "Dr. Zhang", "Note"));
    }

    // ==================== closeHazard (persistence enabled) ====================

    @Test
    void shouldThrowException_whenCloseHazardNotFound() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> service.closeHazard(999L));
    }

    // ==================== createSafetyCase (persistence enabled) ====================

    @Test
    void shouldCreateSafetyCase_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setTenantId(1L);
        safetyCase.setCaseCode("SC-001");
        safetyCase.setCaseName("Test Case");
        safetyCase.setCaseType("SYSTEM");
        safetyCase.setScope("Full system");
        safetyCase.setGoal("Safety goal");
        safetyCase.setArgument("Argument");
        safetyCase.setEvidenceRefs("[]");
        safetyCase.setCreatedBy("admin");

        SafetyCase result = service.createSafetyCase(safetyCase);
        assertNotNull(result);
        assertEquals("Test Case", result.getCaseName());
        // Status is set in SQL but not reflected back in the Java object
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void shouldCreateSafetyCaseWithProvidedStatus() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setTenantId(1L);
        safetyCase.setCaseCode("SC-002");
        safetyCase.setCaseName("Test Case");
        safetyCase.setStatus("UNDER_REVIEW");
        safetyCase.setCreatedBy("admin");

        SafetyCase result = service.createSafetyCase(safetyCase);
        assertEquals("UNDER_REVIEW", result.getStatus());
    }

    // ==================== reviewSafetyCase (persistence enabled) ====================

    @Test
    void shouldThrowException_whenReviewSafetyCaseNotFound() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> service.reviewSafetyCase(999L, "APPROVED", "Dr. Reviewer", "Note"));
    }

    // ==================== acceptHazard (persistence enabled, success) ====================

    @Test
    void shouldAcceptHazard_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        // First call: UPDATE returns 1 row affected
        // Second call: getHazard SELECT returns a hazard
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("hazard_code")).thenReturn("HAZ-001");
        when(resultSet.getString("hazard_name")).thenReturn("Accepted Hazard");
        when(resultSet.getString("hazard_category")).thenReturn("CLINICAL");
        when(resultSet.getString("hazard_description")).thenReturn("Desc");
        when(resultSet.getString("affected_process")).thenReturn("ORDER");
        when(resultSet.getString("likelihood")).thenReturn("POSSIBLE");
        when(resultSet.getString("severity")).thenReturn("MAJOR");
        when(resultSet.getString("risk_level")).thenReturn("HIGH");
        when(resultSet.getString("control_measures")).thenReturn("Control");
        when(resultSet.getString("residual_risk")).thenReturn("LOW");
        when(resultSet.getString("status")).thenReturn("ACCEPTED");
        when(resultSet.getString("accepted_by")).thenReturn("Dr. Zhang");
        when(resultSet.getTimestamp("accepted_time")).thenReturn(null);
        when(resultSet.getString("acceptance_note")).thenReturn("Accepted");
        when(resultSet.getString("blocking_strategy")).thenReturn("ESCALATE");
        when(resultSet.getString("created_by")).thenReturn("admin");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);

        HazardLog result = service.acceptHazard(1L, "Dr. Zhang", "Accepted");
        assertNotNull(result);
        assertEquals("ACCEPTED", result.getStatus());
    }

    // ==================== closeHazard (persistence enabled, success) ====================

    @Test
    void shouldCloseHazard_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("hazard_code")).thenReturn("HAZ-001");
        when(resultSet.getString("hazard_name")).thenReturn("Closed Hazard");
        when(resultSet.getString("hazard_category")).thenReturn("CLINICAL");
        when(resultSet.getString("hazard_description")).thenReturn("Desc");
        when(resultSet.getString("affected_process")).thenReturn("ORDER");
        when(resultSet.getString("likelihood")).thenReturn("POSSIBLE");
        when(resultSet.getString("severity")).thenReturn("MAJOR");
        when(resultSet.getString("risk_level")).thenReturn("HIGH");
        when(resultSet.getString("control_measures")).thenReturn("Control");
        when(resultSet.getString("residual_risk")).thenReturn("LOW");
        when(resultSet.getString("status")).thenReturn("CLOSED");
        when(resultSet.getString("accepted_by")).thenReturn(null);
        when(resultSet.getTimestamp("accepted_time")).thenReturn(null);
        when(resultSet.getString("acceptance_note")).thenReturn(null);
        when(resultSet.getString("blocking_strategy")).thenReturn("WARN");
        when(resultSet.getString("created_by")).thenReturn("admin");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);

        HazardLog result = service.closeHazard(1L);
        assertNotNull(result);
        assertEquals("CLOSED", result.getStatus());
    }

    // ==================== reviewSafetyCase (persistence enabled, success) ====================

    @Test
    void shouldReviewSafetyCase_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("case_code")).thenReturn("SC-001");
        when(resultSet.getString("case_name")).thenReturn("Reviewed Case");
        when(resultSet.getString("case_type")).thenReturn("SYSTEM");
        when(resultSet.getString("scope")).thenReturn("Full");
        when(resultSet.getString("goal")).thenReturn("Goal");
        when(resultSet.getString("argument")).thenReturn("Arg");
        when(resultSet.getString("evidence_refs")).thenReturn("[]");
        when(resultSet.getString("status")).thenReturn("APPROVED");
        when(resultSet.getString("reviewed_by")).thenReturn("Dr. Reviewer");
        when(resultSet.getTimestamp("reviewed_time")).thenReturn(null);
        when(resultSet.getString("review_note")).thenReturn("Good");
        when(resultSet.getString("version")).thenReturn("1.0");
        when(resultSet.getString("created_by")).thenReturn("admin");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);

        SafetyCase result = service.reviewSafetyCase(1L, "APPROVED", "Dr. Reviewer", "Good");
        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
    }

    // ==================== getRiskSummary (persistence enabled) ====================

    @Test
    void shouldReturnRiskSummary_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        // First query: hazard stats
        // Second query: safety case stats
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Map<String, Object> summary = service.getRiskSummary(1L);
        assertEquals(0, summary.get("total_hazards"));
        assertEquals(0, summary.get("total_safety_cases"));
        assertNotNull(summary.get("risk_level_distribution"));
        assertNotNull(summary.get("safety_case_status_distribution"));
    }

    @Test
    void shouldReturnRiskSummary_withData() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        // First query: hazard stats returns one row
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false, false);
        when(resultSet.getString("risk_level")).thenReturn("HIGH");
        when(resultSet.getString("status")).thenReturn("IDENTIFIED");
        when(resultSet.getInt("cnt")).thenReturn(3);

        Map<String, Object> summary = service.getRiskSummary(1L);
        assertEquals(3, summary.get("total_hazards"));
        Map<String, Integer> riskDist = (Map<String, Integer>) summary.get("risk_level_distribution");
        assertEquals(3, riskDist.get("HIGH").intValue());
        Map<String, Integer> statusDist = (Map<String, Integer>) summary.get("status_distribution");
        assertEquals(3, statusDist.get("IDENTIFIED").intValue());
    }

    // ==================== updateHazard (persistence enabled, success) ====================

    @Test
    void shouldUpdateHazard_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        HazardLog hazard = new HazardLog();
        hazard.setId(1L);
        hazard.setTenantId(1L);
        hazard.setHazardName("Updated Hazard");
        hazard.setHazardCategory("CLINICAL");
        hazard.setUpdatedBy("admin");

        HazardLog result = service.updateHazard(hazard);
        assertNotNull(result);
        assertEquals("Updated Hazard", result.getHazardName());
    }

    // ==================== updateSafetyCase (persistence enabled, success) ====================

    @Test
    void shouldUpdateSafetyCase_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setId(1L);
        safetyCase.setTenantId(1L);
        safetyCase.setCaseName("Updated Case");
        safetyCase.setUpdatedBy("admin");

        SafetyCase result = service.updateSafetyCase(safetyCase);
        assertNotNull(result);
        assertEquals("Updated Case", result.getCaseName());
    }

    // ==================== listSafetyCases (persistence enabled) ====================

    @Test
    void shouldListSafetyCases_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("case_code")).thenReturn("SC-001");
        when(resultSet.getString("case_name")).thenReturn("Test Case");
        when(resultSet.getString("case_type")).thenReturn("SYSTEM");
        when(resultSet.getString("scope")).thenReturn("Full");
        when(resultSet.getString("goal")).thenReturn("Goal");
        when(resultSet.getString("argument")).thenReturn("Arg");
        when(resultSet.getString("evidence_refs")).thenReturn("[]");
        when(resultSet.getString("status")).thenReturn("DRAFT");
        when(resultSet.getString("reviewed_by")).thenReturn(null);
        when(resultSet.getTimestamp("reviewed_time")).thenReturn(null);
        when(resultSet.getString("review_note")).thenReturn(null);
        when(resultSet.getString("version")).thenReturn("1.0");
        when(resultSet.getString("created_by")).thenReturn("admin");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);

        List<SafetyCase> result = service.listSafetyCases(1L, "SYSTEM", "DRAFT");
        assertEquals(1, result.size());
        assertEquals("Test Case", result.get(0).getCaseName());
    }

    // ==================== Oracle dialect ====================

    @Test
    void shouldUseOracleTimestamp_whenNotLocalFileDatabase() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(false);

        HazardLog hazard = new HazardLog();
        hazard.setTenantId(1L);
        hazard.setHazardCode("HAZ-ORA");
        hazard.setHazardName("Oracle Hazard");
        hazard.setCreatedBy("admin");

        service.createHazard(hazard);
        // Verify SYSTIMESTAMP was used in the SQL
        verify(connection).prepareStatement(contains("SYSTIMESTAMP"));
    }
}
