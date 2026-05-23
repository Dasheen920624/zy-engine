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
class CdssOverrideServiceTest {

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

    private CdssOverrideService service;

    @BeforeEach
    void setUp() throws SQLException {
        service = new CdssOverrideService(properties, dataSource);
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    // ==================== recordOverride (persistence disabled) ====================

    @Test
    void shouldReturnLog_whenPersistenceDisabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(false);

        CdssOverrideLog log = new CdssOverrideLog();
        log.setAlertId("alert-1");
        log.setOverrideType("OVERRIDE");
        log.setOverrideReason("Clinical necessity");

        CdssOverrideLog result = service.recordOverride(log);
        assertEquals("alert-1", result.getAlertId());
        verify(dataSource, never()).getConnection();
    }

    // ==================== recordOverride (persistence enabled) ====================

    @Test
    void shouldRecordOverride_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        CdssOverrideLog log = new CdssOverrideLog();
        log.setTenantId(1L);
        log.setAlertId("alert-1");
        log.setTriggerCode("ORDER_PLACED");
        log.setRuleCode("RULE-001");
        log.setRiskLevel("HIGH");
        log.setAlertLevel("HIGH");
        log.setOverrideType("OVERRIDE");
        log.setOverrideReason("Clinical necessity");
        log.setOverrideCategory("CLINICAL");
        log.setSupervisorName("Dr. Supervisor");
        log.setConfirmedBy("Dr. Confirmer");
        log.setPatientId("P001");
        log.setEncounterId("E001");
        log.setOperatorId("OP001");
        log.setDepartmentCode("DEPT001");
        log.setIsAuditRedLine("Y");
        log.setFatigueSuppressed("N");
        log.setOverrideTime(LocalDateTime.now());

        CdssOverrideLog result = service.recordOverride(log);
        assertNotNull(result);
        assertEquals("alert-1", result.getAlertId());
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void shouldThrowException_whenRecordOverrideFails() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        CdssOverrideLog log = new CdssOverrideLog();
        log.setTenantId(1L);
        log.setAlertId("alert-fail");
        log.setOverrideType("OVERRIDE");

        assertThrows(IllegalStateException.class, () -> service.recordOverride(log));
    }

    @Test
    void shouldUseCurrentTimestamp_whenOverrideTimeIsNull() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);

        CdssOverrideLog log = new CdssOverrideLog();
        log.setTenantId(1L);
        log.setAlertId("alert-no-time");
        log.setOverrideType("ACKNOWLEDGE");
        log.setOverrideTime(null);

        service.recordOverride(log);
        verify(preparedStatement).setTimestamp(anyInt(), any());
    }

    // ==================== listOverrides (persistence disabled) ====================

    @Test
    void shouldReturnEmptyList_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        List<CdssOverrideLog> result = service.listOverrides(1L, null, null, 10);
        assertTrue(result.isEmpty());
    }

    // ==================== listOverrides (persistence enabled) ====================

    @Test
    void shouldListOverrides_withFilters() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<CdssOverrideLog> result = service.listOverrides(1L, "P001", "OP001", 10);
        assertTrue(result.isEmpty());
        verify(connection).prepareStatement(contains("patient_id"));
        verify(connection).prepareStatement(contains("operator_id"));
    }

    @Test
    void shouldListOverrides_withoutFilters() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<CdssOverrideLog> result = service.listOverrides(1L, null, null, 0);
        assertTrue(result.isEmpty());
    }

    // ==================== getOverrideStatistics (persistence disabled) ====================

    @Test
    void shouldReturnDefaultStats_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        Map<String, Object> stats = service.getOverrideStatistics(1L, "OP001", "RULE-001", 24);
        assertEquals(1L, stats.get("tenant_id"));
        assertEquals(0, stats.get("override_count"));
        assertEquals(24, stats.get("time_window_hours"));
    }

    // ==================== getOverrideStatistics (persistence enabled) ====================

    @Test
    void shouldReturnStats_whenPersistenceEnabled() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("cnt")).thenReturn(5);

        Map<String, Object> stats = service.getOverrideStatistics(1L, "OP001", "RULE-001", 24);
        assertEquals(5, stats.get("override_count"));
    }

    // ==================== checkFatigue (persistence disabled) ====================

    @Test
    void shouldReturnNotTriggered_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        Map<String, Object> result = service.checkFatigue(1L, "OP001", "RULE-001");
        assertFalse((Boolean) result.get("fatigue_triggered"));
    }

    // ==================== checkFatigue (persistence enabled, no config) ====================

    @Test
    void shouldReturnNotTriggered_whenNoFatigueConfig() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Map<String, Object> result = service.checkFatigue(1L, "OP001", "RULE-001");
        assertFalse((Boolean) result.get("fatigue_triggered"));
    }

    // ==================== saveFatigueConfig (persistence disabled) ====================

    @Test
    void shouldReturnConfig_whenPersistenceDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        CdssFatigueConfig config = new CdssFatigueConfig();
        config.setConfigCode("FC-001");
        config.setConfigName("Test Config");

        CdssFatigueConfig result = service.saveFatigueConfig(config);
        assertEquals("FC-001", result.getConfigCode());
    }

    // ==================== saveFatigueConfig (persistence enabled, update) ====================

    @Test
    void shouldUpdateFatigueConfig_whenExists() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        CdssFatigueConfig config = new CdssFatigueConfig();
        config.setTenantId(1L);
        config.setConfigCode("FC-001");
        config.setConfigName("Updated Config");
        config.setRuleCode("RULE-001");
        config.setTimeWindowHours(24);
        config.setOverrideThreshold(10);
        config.setSuppressAction("BLOCK");
        config.setSuppressLevel("OPERATOR");
        config.setEnabled("TRUE");
        config.setUpdatedBy("admin");

        CdssFatigueConfig result = service.saveFatigueConfig(config);
        assertEquals("Updated Config", result.getConfigName());
    }

    // ==================== saveFatigueConfig (persistence enabled, insert) ====================

    @Test
    void shouldInsertFatigueConfig_whenNotExists() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        // First update returns 0 (not found), then insert succeeds
        when(preparedStatement.executeUpdate()).thenReturn(0);

        CdssFatigueConfig config = new CdssFatigueConfig();
        config.setTenantId(1L);
        config.setConfigCode("FC-NEW");
        config.setConfigName("New Config");
        config.setRuleCode("RULE-002");
        config.setTimeWindowHours(12);
        config.setOverrideThreshold(5);
        config.setSuppressAction("WARN");
        config.setSuppressLevel("DEPARTMENT");
        config.setEnabled("TRUE");
        config.setCreatedBy("admin");

        CdssFatigueConfig result = service.saveFatigueConfig(config);
        assertNotNull(result);
        // Should have called executeUpdate twice (update + insert)
        verify(preparedStatement, atLeast(2)).executeUpdate();
    }

    // ==================== listFatigueConfigs (persistence disabled) ====================

    @Test
    void shouldReturnEmptyList_whenPersistenceDisabledForFatigueConfigs() {
        when(properties.isEnabled()).thenReturn(false);

        List<CdssFatigueConfig> result = service.listFatigueConfigs(1L);
        assertTrue(result.isEmpty());
    }

    // ==================== listFatigueConfigs (persistence enabled) ====================

    @Test
    void shouldListFatigueConfigs() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getObject("tenant_id")).thenReturn(1L);
        when(resultSet.getString("config_code")).thenReturn("FC-001");
        when(resultSet.getString("config_name")).thenReturn("Test Config");
        when(resultSet.getString("rule_code")).thenReturn("RULE-001");
        when(resultSet.getString("department_code")).thenReturn(null);
        when(resultSet.getInt("time_window_hours")).thenReturn(24);
        when(resultSet.getInt("override_threshold")).thenReturn(10);
        when(resultSet.getString("suppress_action")).thenReturn("BLOCK");
        when(resultSet.getString("suppress_level")).thenReturn("OPERATOR");
        when(resultSet.getString("enabled")).thenReturn("TRUE");
        when(resultSet.getString("description")).thenReturn("Test");
        when(resultSet.getString("created_by")).thenReturn("admin");
        when(resultSet.getTimestamp("created_time")).thenReturn(null);
        when(resultSet.getString("updated_by")).thenReturn(null);
        when(resultSet.getTimestamp("updated_time")).thenReturn(null);

        List<CdssFatigueConfig> result = service.listFatigueConfigs(1L);
        assertEquals(1, result.size());
        assertEquals("FC-001", result.get(0).getConfigCode());
    }

    // ==================== updateFatigueConfig (persistence disabled) ====================

    @Test
    void shouldReturnConfig_whenUpdateDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        CdssFatigueConfig config = new CdssFatigueConfig();
        config.setId(1L);
        config.setConfigName("Updated");

        CdssFatigueConfig result = service.updateFatigueConfig(config);
        assertEquals("Updated", result.getConfigName());
    }

    // ==================== updateFatigueConfig (persistence enabled) ====================

    @Test
    void shouldThrowException_whenFatigueConfigNotFound() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(true);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        CdssFatigueConfig config = new CdssFatigueConfig();
        config.setId(999L);
        config.setTenantId(1L);

        assertThrows(IllegalArgumentException.class, () -> service.updateFatigueConfig(config));
    }

    // ==================== Oracle dialect ====================

    @Test
    void shouldUseOracleTimestamp_whenNotLocalFileDatabase() throws SQLException {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.localFileDatabase()).thenReturn(false);

        CdssOverrideLog log = new CdssOverrideLog();
        log.setTenantId(1L);
        log.setAlertId("alert-ora");
        log.setOverrideType("OVERRIDE");
        log.setOverrideTime(LocalDateTime.now());

        service.recordOverride(log);
        verify(connection).prepareStatement(contains("SYSTIMESTAMP"));
    }
}
