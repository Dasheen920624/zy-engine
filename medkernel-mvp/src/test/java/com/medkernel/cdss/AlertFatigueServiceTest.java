package com.medkernel.cdss;

import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertFatigueServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private OrganizationContextService organizationContextService;

    private AlertFatigueService service;

    private OrganizationContext orgContext;

    @BeforeEach
    void setUp() {
        service = new AlertFatigueService(persistenceService, organizationContextService);
        orgContext = new OrganizationContext();
        orgContext.setTenantId("tenant1");
        orgContext.setHospitalCode("HOSP001");
    }

    // ==================== createConfig ====================

    @Test
    void shouldCreateConfig_withDefaults() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("trigger_point", "ORDER_PLACED");
        request.put("risk_level", "HIGH");
        request.put("created_by", "admin");

        AlertFatigueConfig config = service.createConfig(request, orgContext);

        assertNotNull(config.getConfigId());
        assertEquals("tenant1", config.getTenantId());
        assertEquals("ORDER_PLACED", config.getTriggerPoint());
        assertEquals("HIGH", config.getRiskLevel());
        assertTrue(config.isDeduplicationEnabled());
        assertEquals(30, config.getDeduplicationWindowMinutes());
        assertTrue(config.isSuppressionEnabled());
        assertEquals(20, config.getSuppressionMaxAlertsPerHour());
        assertTrue(config.isQuietPeriodEnabled());
        assertEquals(60, config.getQuietPeriodMinutes());
        assertTrue(config.isSmartFilterEnabled());
        assertEquals(0.8, config.getOverrideRateThreshold(), 0.001);
        assertEquals("ACTIVE", config.getStatus());
        assertEquals("admin", config.getCreatedBy());
    }

    @Test
    void shouldCreateConfig_withCustomValues() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("trigger_point", "EMR_SAVED");
        request.put("deduplication_enabled", "false");
        request.put("deduplication_window_minutes", "60");
        request.put("suppression_enabled", false);
        request.put("suppression_max_alerts_per_hour", 10);
        request.put("quiet_period_enabled", "true");
        request.put("quiet_period_minutes", "120");
        request.put("smart_filter_enabled", "false");
        request.put("override_rate_threshold", "0.6");

        AlertFatigueConfig config = service.createConfig(request, orgContext);

        assertFalse(config.isDeduplicationEnabled());
        assertEquals(60, config.getDeduplicationWindowMinutes());
        assertFalse(config.isSuppressionEnabled());
        assertEquals(10, config.getSuppressionMaxAlertsPerHour());
        assertTrue(config.isQuietPeriodEnabled());
        assertEquals(120, config.getQuietPeriodMinutes());
        assertFalse(config.isSmartFilterEnabled());
        assertEquals(0.6, config.getOverrideRateThreshold(), 0.001);
    }

    // ==================== listConfigs ====================

    @Test
    void shouldListConfigs_forTenant() {
        Map<String, Object> request1 = new HashMap<String, Object>();
        request1.put("trigger_point", "ORDER_PLACED");
        service.createConfig(request1, orgContext);

        Map<String, Object> request2 = new HashMap<String, Object>();
        request2.put("trigger_point", "EMR_SAVED");
        service.createConfig(request2, orgContext);

        List<AlertFatigueConfig> configs = service.listConfigs(orgContext);
        assertEquals(2, configs.size());
    }

    @Test
    void shouldNotListConfigs_forOtherTenant() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("trigger_point", "ORDER_PLACED");
        service.createConfig(request, orgContext);

        OrganizationContext otherTenant = new OrganizationContext();
        otherTenant.setTenantId("tenant2");

        List<AlertFatigueConfig> configs = service.listConfigs(otherTenant);
        assertTrue(configs.isEmpty());
    }

    // ==================== updateConfig ====================

    @Test
    void shouldUpdateConfig() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("trigger_point", "ORDER_PLACED");
        AlertFatigueConfig config = service.createConfig(createReq, orgContext);

        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("deduplication_window_minutes", 45);
        updateReq.put("suppression_max_alerts_per_hour", 15);
        updateReq.put("status", "DISABLED");

        AlertFatigueConfig updated = service.updateConfig(config.getConfigId(), updateReq, orgContext);
        assertEquals(45, updated.getDeduplicationWindowMinutes());
        assertEquals(15, updated.getSuppressionMaxAlertsPerHour());
        assertEquals("DISABLED", updated.getStatus());
    }

    @Test
    void shouldThrowException_whenUpdateNonexistentConfig() {
        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("status", "DISABLED");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateConfig("NONEXISTENT", updateReq, orgContext));
    }

    @Test
    void shouldThrowException_whenUpdateOtherTenantConfig() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("trigger_point", "ORDER_PLACED");
        AlertFatigueConfig config = service.createConfig(createReq, orgContext);

        OrganizationContext otherTenant = new OrganizationContext();
        otherTenant.setTenantId("tenant2");

        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("status", "DISABLED");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateConfig(config.getConfigId(), updateReq, otherTenant));
    }

    // ==================== shouldFilterAlert ====================

    @Test
    void shouldNotFilter_whenNoMatchingConfig() {
        CdssAlert alert = new CdssAlert();
        alert.setAlertId("alert-1");
        alert.setTriggerPoint("ORDER_PLACED");
        alert.setRuleCode("RULE-001");
        alert.setPatientId("P001");
        alert.setRiskLevel(CdssRiskLevel.HIGH);

        assertFalse(service.shouldFilterAlert(alert, "tenant1"));
    }

    @Test
    void shouldNotFilter_whenConfigIsDisabled() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("trigger_point", "ORDER_PLACED");
        AlertFatigueConfig config = service.createConfig(createReq, orgContext);

        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("status", "DISABLED");
        service.updateConfig(config.getConfigId(), updateReq, orgContext);

        CdssAlert alert = new CdssAlert();
        alert.setAlertId("alert-1");
        alert.setTriggerPoint("ORDER_PLACED");
        alert.setRuleCode("RULE-001");
        alert.setPatientId("P001");
        alert.setRiskLevel(CdssRiskLevel.HIGH);

        assertFalse(service.shouldFilterAlert(alert, "tenant1"));
    }

    @Test
    void shouldFilterDuplicateAlert() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("trigger_point", "ORDER_PLACED");
        createReq.put("deduplication_enabled", "true");
        createReq.put("deduplication_window_minutes", "30");
        service.createConfig(createReq, orgContext);

        // Record first alert
        CdssAlert firstAlert = new CdssAlert();
        firstAlert.setAlertId("alert-1");
        firstAlert.setTriggerPoint("ORDER_PLACED");
        firstAlert.setRuleCode("RULE-001");
        firstAlert.setPatientId("P001");
        firstAlert.setRiskLevel(CdssRiskLevel.HIGH);
        service.recordAlert(firstAlert);

        // Same rule + same patient should be filtered
        CdssAlert duplicateAlert = new CdssAlert();
        duplicateAlert.setAlertId("alert-2");
        duplicateAlert.setTriggerPoint("ORDER_PLACED");
        duplicateAlert.setRuleCode("RULE-001");
        duplicateAlert.setPatientId("P001");
        duplicateAlert.setRiskLevel(CdssRiskLevel.HIGH);

        assertTrue(service.shouldFilterAlert(duplicateAlert, "tenant1"));
    }

    @Test
    void shouldNotFilter_whenDeduplicationDisabled() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("trigger_point", "ORDER_PLACED");
        createReq.put("deduplication_enabled", "false");
        createReq.put("suppression_enabled", "false");
        createReq.put("quiet_period_enabled", "false");
        createReq.put("smart_filter_enabled", "false");
        service.createConfig(createReq, orgContext);

        CdssAlert firstAlert = new CdssAlert();
        firstAlert.setAlertId("alert-1");
        firstAlert.setTriggerPoint("ORDER_PLACED");
        firstAlert.setRuleCode("RULE-001");
        firstAlert.setPatientId("P001");
        firstAlert.setRiskLevel(CdssRiskLevel.HIGH);
        service.recordAlert(firstAlert);

        CdssAlert secondAlert = new CdssAlert();
        secondAlert.setAlertId("alert-2");
        secondAlert.setTriggerPoint("ORDER_PLACED");
        secondAlert.setRuleCode("RULE-001");
        secondAlert.setPatientId("P001");
        secondAlert.setRiskLevel(CdssRiskLevel.HIGH);

        assertFalse(service.shouldFilterAlert(secondAlert, "tenant1"));
    }

    // ==================== recordAlert ====================

    @Test
    void shouldRecordAlert() {
        CdssAlert alert = new CdssAlert();
        alert.setAlertId("alert-rec");
        alert.setRuleCode("RULE-001");
        alert.setPatientId("P001");
        alert.setTriggerPoint("ORDER_PLACED");
        alert.setRiskLevel(CdssRiskLevel.HIGH);

        service.recordAlert(alert);
        // No exception means success
    }

    // ==================== recordOverride ====================

    @Test
    void shouldRecordOverride() {
        CdssAlert alert = new CdssAlert();
        alert.setAlertId("alert-override");
        alert.setRuleCode("RULE-001");
        alert.setPatientId("P001");
        alert.setTriggerPoint("ORDER_PLACED");
        alert.setRiskLevel(CdssRiskLevel.HIGH);

        service.recordOverride(alert, "OVERRIDE", "Dr. Zhang");
        // No exception means success
    }

    // ==================== getOverrideAnalysis ====================

    @Test
    void shouldReturnEmptyAnalysis_whenNoHistory() {
        Map<String, Object> analysis = service.getOverrideAnalysis("tenant1");
        assertEquals(0, analysis.get("total_alerts"));
        assertEquals(0, analysis.get("total_overrides"));
        assertEquals(0, analysis.get("total_acknowledges"));
        assertEquals(0, analysis.get("total_escalations"));
        assertEquals(0.0, analysis.get("override_rate"));
    }

    @Test
    void shouldReturnAnalysis_withOverrideHistory() {
        CdssAlert alert = new CdssAlert();
        alert.setAlertId("alert-1");
        alert.setRuleCode("RULE-001");
        alert.setPatientId("P001");
        alert.setTriggerPoint("ORDER_PLACED");
        alert.setRiskLevel(CdssRiskLevel.HIGH);
        service.recordAlert(alert);

        service.recordOverride(alert, "OVERRIDE", "Dr. Zhang");

        Map<String, Object> analysis = service.getOverrideAnalysis("tenant1");
        assertEquals(1, analysis.get("total_alerts"));
        assertEquals(1, analysis.get("total_overrides"));
        assertEquals(100.0, analysis.get("override_rate"));
    }

    @Test
    void shouldReturnAnalysis_withMultipleOverrideTypes() {
        CdssAlert alert1 = new CdssAlert();
        alert1.setAlertId("a1");
        alert1.setRuleCode("R1");
        alert1.setPatientId("P001");
        alert1.setTriggerPoint("ORDER_PLACED");
        alert1.setRiskLevel(CdssRiskLevel.HIGH);
        service.recordAlert(alert1);
        service.recordOverride(alert1, "OVERRIDE", "Dr. A");

        CdssAlert alert2 = new CdssAlert();
        alert2.setAlertId("a2");
        alert2.setRuleCode("R2");
        alert2.setPatientId("P002");
        alert2.setTriggerPoint("EMR_SAVED");
        alert2.setRiskLevel(CdssRiskLevel.MEDIUM);
        service.recordAlert(alert2);
        service.recordOverride(alert2, "ACKNOWLEDGE", "Dr. B");

        CdssAlert alert3 = new CdssAlert();
        alert3.setAlertId("a3");
        alert3.setRuleCode("R3");
        alert3.setPatientId("P003");
        alert3.setTriggerPoint("ORDER_PLACED");
        alert3.setRiskLevel(CdssRiskLevel.CRITICAL);
        service.recordAlert(alert3);
        service.recordOverride(alert3, "ESCALATE", "Dr. C");

        Map<String, Object> analysis = service.getOverrideAnalysis("tenant1");
        assertEquals(3, analysis.get("total_alerts"));
        assertEquals(1, analysis.get("total_overrides"));
        assertEquals(1, analysis.get("total_acknowledges"));
        assertEquals(1, analysis.get("total_escalations"));
    }

    @Test
    void shouldIdentifyHighOverrideRules() {
        // Record 5 alerts for same rule
        for (int i = 0; i < 5; i++) {
            CdssAlert alert = new CdssAlert();
            alert.setAlertId("alert-" + i);
            alert.setRuleCode("RULE-HIGH-OVERRIDE");
            alert.setPatientId("P00" + i);
            alert.setTriggerPoint("ORDER_PLACED");
            alert.setRiskLevel(CdssRiskLevel.HIGH);
            service.recordAlert(alert);
            service.recordOverride(alert, "OVERRIDE", "Dr. Zhang");
        }

        Map<String, Object> analysis = service.getOverrideAnalysis("tenant1");
        List<Map<String, Object>> highOverrideRules =
                (List<Map<String, Object>>) analysis.get("high_override_rules");
        assertFalse(highOverrideRules.isEmpty());
        assertEquals("RULE-HIGH-OVERRIDE", highOverrideRules.get(0).get("rule_code"));
    }

    // ==================== quiet period ====================

    @Test
    void shouldFilterAlertInQuietPeriod() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("trigger_point", "ORDER_PLACED");
        createReq.put("quiet_period_enabled", "true");
        createReq.put("quiet_period_minutes", "60");
        createReq.put("deduplication_enabled", "false");
        createReq.put("suppression_enabled", "false");
        createReq.put("smart_filter_enabled", "false");
        service.createConfig(createReq, orgContext);

        // Record an override for a rule
        CdssAlert overrideAlert = new CdssAlert();
        overrideAlert.setAlertId("alert-override-1");
        overrideAlert.setRuleCode("RULE-QP");
        overrideAlert.setPatientId("P001");
        overrideAlert.setTriggerPoint("ORDER_PLACED");
        overrideAlert.setRiskLevel(CdssRiskLevel.HIGH);
        service.recordOverride(overrideAlert, "OVERRIDE", "Dr. Zhang");

        // Same rule + same patient should be in quiet period
        CdssAlert newAlert = new CdssAlert();
        newAlert.setAlertId("alert-new");
        newAlert.setRuleCode("RULE-QP");
        newAlert.setPatientId("P001");
        newAlert.setTriggerPoint("ORDER_PLACED");
        newAlert.setRiskLevel(CdssRiskLevel.HIGH);

        assertTrue(service.shouldFilterAlert(newAlert, "tenant1"));
    }
}
