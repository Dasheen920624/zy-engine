package com.medkernel.cdss;

import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.rule.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CdssServiceTest {

    @Mock
    private RuleService ruleService;

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private OrganizationContextService organizationContextService;

    private CdssService cdssService;

    @BeforeEach
    void setUp() {
        cdssService = new CdssService(ruleService, persistenceService, organizationContextService);
    }

    // ==================== evaluate ====================

    @Test
    void shouldReturnEmptyList_whenTriggerPointIsNull() {
        Map<String, Object> patientContext = new HashMap<String, Object>();
        List<CdssAlert> result = cdssService.evaluate(null, patientContext, "tenant1");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyList_whenTriggerPointNotMapped() {
        Map<String, Object> patientContext = new HashMap<String, Object>();
        List<CdssAlert> result = cdssService.evaluate("UNKNOWN_TRIGGER", patientContext, "tenant1");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyList_whenRuleEngineThrowsException() {
        Map<String, Object> patientContext = new HashMap<String, Object>();
        patientContext.put("patient_id", "P001");

        when(ruleService.evaluateForScenario(anyMap(), any(OrganizationContext.class)))
                .thenThrow(new RuntimeException("Rule engine error"));

        List<CdssAlert> result = cdssService.evaluate("ORDER_PLACED", patientContext, "tenant1");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyList_whenNoRulesHit() {
        Map<String, Object> patientContext = new HashMap<String, Object>();
        patientContext.put("patient_id", "P001");

        Map<String, Object> evalResult = new HashMap<String, Object>();
        List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();
        Map<String, Object> rule1 = new HashMap<String, Object>();
        rule1.put("hit", false);
        rule1.put("message", "No issue");
        rules.add(rule1);
        evalResult.put("rules", rules);

        when(ruleService.evaluateForScenario(anyMap(), any(OrganizationContext.class)))
                .thenReturn(evalResult);

        List<CdssAlert> result = cdssService.evaluate("ORDER_PLACED", patientContext, "tenant1");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnAlerts_whenRulesHit() {
        Map<String, Object> patientContext = new HashMap<String, Object>();
        patientContext.put("patient_id", "P001");
        patientContext.put("encounter_id", "E001");

        Map<String, Object> evalResult = new HashMap<String, Object>();
        List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();
        Map<String, Object> rule1 = new HashMap<String, Object>();
        rule1.put("hit", true);
        rule1.put("severity", "HIGH");
        rule1.put("message", "Drug interaction detected");
        rule1.put("ruleCode", "RULE-001");
        rule1.put("versionNo", "1.0");
        rules.add(rule1);
        evalResult.put("rules", rules);

        when(ruleService.evaluateForScenario(anyMap(), any(OrganizationContext.class)))
                .thenReturn(evalResult);

        List<CdssAlert> result = cdssService.evaluate("ORDER_PLACED", patientContext, "tenant1");
        assertEquals(1, result.size());
        assertEquals("ORDER_PLACED", result.get(0).getTriggerPoint());
        assertEquals(CdssRiskLevel.HIGH, result.get(0).getRiskLevel());
        assertEquals("P001", result.get(0).getPatientId());
        assertEquals("E001", result.get(0).getEncounterId());
        assertFalse(result.get(0).isBlocking());
        assertTrue(result.get(0).isRequiresConfirmation());
    }

    @Test
    void shouldReturnCriticalAlert_whenSeverityIsCritical() {
        Map<String, Object> patientContext = new HashMap<String, Object>();
        patientContext.put("patient_id", "P001");

        Map<String, Object> evalResult = new HashMap<String, Object>();
        List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();
        Map<String, Object> rule1 = new HashMap<String, Object>();
        rule1.put("hit", true);
        rule1.put("severity", "CRITICAL");
        rule1.put("message", "Contraindication");
        rule1.put("ruleCode", "RULE-002");
        rules.add(rule1);
        evalResult.put("rules", rules);

        when(ruleService.evaluateForScenario(anyMap(), any(OrganizationContext.class)))
                .thenReturn(evalResult);

        List<CdssAlert> result = cdssService.evaluate("DRUG_DISPENSED", patientContext, "tenant1");
        assertEquals(1, result.size());
        assertEquals(CdssRiskLevel.CRITICAL, result.get(0).getRiskLevel());
        assertTrue(result.get(0).isBlocking());
        assertTrue(result.get(0).isRequiresConfirmation());
    }

    @Test
    void shouldMapTriggersCorrectly() {
        Map<String, Object> patientContext = new HashMap<String, Object>();
        patientContext.put("patient_id", "P001");

        Map<String, Object> evalResult = new HashMap<String, Object>();
        evalResult.put("rules", new ArrayList<Map<String, Object>>());

        when(ruleService.evaluateForScenario(anyMap(), any(OrganizationContext.class)))
                .thenReturn(evalResult);

        // Test various trigger mappings
        cdssService.evaluate("ORDER_PLACED", patientContext, "t1");
        cdssService.evaluate("DRUG_DISPENSED", patientContext, "t1");
        cdssService.evaluate("EMR_SAVED", patientContext, "t1");
        cdssService.evaluate("EXAM_REQUESTED", patientContext, "t1");
        cdssService.evaluate("PATHWAY_ENTRY", patientContext, "t1");
        cdssService.evaluate("INSURANCE_SETTLEMENT", patientContext, "t1");
        cdssService.evaluate("DISCHARGE", patientContext, "t1");

        verify(ruleService, times(7)).evaluateForScenario(anyMap(), any(OrganizationContext.class));
    }

    @Test
    void shouldReturnEmptyList_whenRulesKeyIsMissing() {
        Map<String, Object> patientContext = new HashMap<String, Object>();
        Map<String, Object> evalResult = new HashMap<String, Object>();
        // No "rules" key

        when(ruleService.evaluateForScenario(anyMap(), any(OrganizationContext.class)))
                .thenReturn(evalResult);

        List<CdssAlert> result = cdssService.evaluate("ORDER_PLACED", patientContext, "tenant1");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleMultipleAlerts() {
        Map<String, Object> patientContext = new HashMap<String, Object>();
        patientContext.put("patient_id", "P001");

        Map<String, Object> evalResult = new HashMap<String, Object>();
        List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();

        Map<String, Object> rule1 = new HashMap<String, Object>();
        rule1.put("hit", true);
        rule1.put("severity", "HIGH");
        rule1.put("message", "Alert 1");
        rule1.put("ruleCode", "R1");
        rules.add(rule1);

        Map<String, Object> rule2 = new HashMap<String, Object>();
        rule2.put("hit", true);
        rule2.put("severity", "LOW");
        rule2.put("message", "Alert 2");
        rule2.put("ruleCode", "R2");
        rules.add(rule2);

        evalResult.put("rules", rules);

        when(ruleService.evaluateForScenario(anyMap(), any(OrganizationContext.class)))
                .thenReturn(evalResult);

        List<CdssAlert> result = cdssService.evaluate("ORDER_PLACED", patientContext, "tenant1");
        assertEquals(2, result.size());
    }

    // ==================== resolveAlert ====================

    @Test
    void shouldThrowException_whenAlertNotFound() {
        assertThrows(IllegalArgumentException.class, () ->
                cdssService.resolveAlert("nonexistent", "ACKNOWLEDGE", "reason", "doctor", null, "tenant1"));
    }

    @Test
    void shouldResolveAlertWithAcknowledge() {
        // First create an alert via evaluate
        CdssAlert alert = createAndStoreAlert("alert-1", "ORDER_PLACED", CdssRiskLevel.MEDIUM, "P001", false);

        CdssAlert resolved = cdssService.resolveAlert("alert-1", "ACKNOWLEDGE", "Noted", "Dr. Zhang", null, "tenant1");
        assertNotNull(resolved.getOverride());
        assertEquals("ACKNOWLEDGE", resolved.getOverride().getOverrideType());
        assertEquals("Dr. Zhang", resolved.getOverride().getOverriddenBy());
        assertFalse(resolved.getOverride().isAuditRedLine());
    }

    @Test
    void shouldThrowException_whenCriticalOverrideWithoutSupervisor() {
        createAndStoreAlert("alert-crit", "ORDER_PLACED", CdssRiskLevel.CRITICAL, "P001", true);

        assertThrows(IllegalArgumentException.class, () ->
                cdssService.resolveAlert("alert-crit", "OVERRIDE", "Need to proceed", "Dr. Zhang", null, "tenant1"));
    }

    @Test
    void shouldThrowException_whenCriticalOverrideWithEmptySupervisor() {
        createAndStoreAlert("alert-crit2", "ORDER_PLACED", CdssRiskLevel.CRITICAL, "P001", true);

        assertThrows(IllegalArgumentException.class, () ->
                cdssService.resolveAlert("alert-crit2", "OVERRIDE", "Need to proceed", "Dr. Zhang", "  ", "tenant1"));
    }

    @Test
    void shouldResolveCriticalOverrideWithSupervisor() {
        createAndStoreAlert("alert-crit3", "ORDER_PLACED", CdssRiskLevel.CRITICAL, "P001", true);

        CdssAlert resolved = cdssService.resolveAlert("alert-crit3", "OVERRIDE", "Urgent need",
                "Dr. Zhang", "Dr. Supervisor", "tenant1");
        assertNotNull(resolved.getOverride());
        assertEquals("OVERRIDE", resolved.getOverride().getOverrideType());
        assertTrue(resolved.getOverride().isAuditRedLine());
        assertEquals("Dr. Supervisor", resolved.getOverride().getSupervisorName());
    }

    @Test
    void shouldWriteAuditLog_whenCriticalOverride() {
        createAndStoreAlert("alert-audit", "ORDER_PLACED", CdssRiskLevel.CRITICAL, "P001", true);

        cdssService.resolveAlert("alert-audit", "OVERRIDE", "Reason", "Dr. Zhang", "Dr. Supervisor", "tenant1");

        verify(persistenceService).saveAuditLog(
                eq("CDSS"), eq("CRITICAL_OVERRIDE"), eq("ALERT"),
                eq("alert-audit"), eq("P001"), isNull(), eq("Dr. Zhang"),
                anyMap());
    }

    @Test
    void shouldWriteNormalAuditLog_whenNonCriticalOverride() {
        createAndStoreAlert("alert-normal", "ORDER_PLACED", CdssRiskLevel.MEDIUM, "P001", false);

        cdssService.resolveAlert("alert-normal", "ACKNOWLEDGE", "Noted", "Dr. Zhang", null, "tenant1");

        verify(persistenceService).saveAuditLog(
                eq("CDSS"), eq("ACKNOWLEDGE"), eq("ALERT"),
                eq("alert-normal"), eq("P001"), isNull(), eq("Dr. Zhang"),
                anyMap());
    }

    @Test
    void shouldRemoveAlertFromActiveAfterResolve() {
        createAndStoreAlert("alert-rm", "ORDER_PLACED", CdssRiskLevel.LOW, "P001", false);

        cdssService.resolveAlert("alert-rm", "ACKNOWLEDGE", "Noted", "Dr. Zhang", null, "tenant1");

        assertNull(cdssService.getAlert("alert-rm"));
    }

    // ==================== listActiveAlerts ====================

    @Test
    void shouldListAllActiveAlerts() {
        createAndStoreAlert("a1", "ORDER_PLACED", CdssRiskLevel.LOW, "P001", false);
        createAndStoreAlert("a2", "EMR_SAVED", CdssRiskLevel.HIGH, "P002", false);

        List<CdssAlert> alerts = cdssService.listActiveAlerts(null);
        assertEquals(2, alerts.size());
    }

    @Test
    void shouldListActiveAlertsByPatientId() {
        createAndStoreAlert("a3", "ORDER_PLACED", CdssRiskLevel.LOW, "P001", false);
        createAndStoreAlert("a4", "EMR_SAVED", CdssRiskLevel.HIGH, "P002", false);
        createAndStoreAlert("a5", "ORDER_PLACED", CdssRiskLevel.MEDIUM, "P001", false);

        List<CdssAlert> alerts = cdssService.listActiveAlerts("P001");
        assertEquals(2, alerts.size());
    }

    @Test
    void shouldReturnEmptyList_whenNoActiveAlerts() {
        List<CdssAlert> alerts = cdssService.listActiveAlerts(null);
        assertTrue(alerts.isEmpty());
    }

    // ==================== getAlert ====================

    @Test
    void shouldReturnAlert_whenExists() {
        createAndStoreAlert("a-get", "ORDER_PLACED", CdssRiskLevel.LOW, "P001", false);

        CdssAlert alert = cdssService.getAlert("a-get");
        assertNotNull(alert);
        assertEquals("a-get", alert.getAlertId());
    }

    @Test
    void shouldReturnNull_whenAlertNotExists() {
        CdssAlert alert = cdssService.getAlert("nonexistent");
        assertNull(alert);
    }

    // ==================== Helper ====================

    private CdssAlert createAndStoreAlert(String alertId, String triggerPoint, CdssRiskLevel riskLevel,
                                           String patientId, boolean blocking) {
        CdssAlert alert = new CdssAlert();
        alert.setAlertId(alertId);
        alert.setTriggerPoint(triggerPoint);
        alert.setRiskLevel(riskLevel);
        alert.setPatientId(patientId);
        alert.setBlocking(blocking);
        alert.setRuleCode("RULE-TEST");

        // Use reflection to access the private activeAlerts map and store the alert directly
        try {
            java.lang.reflect.Field field = CdssService.class.getDeclaredField("activeAlerts");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, CdssAlert> activeAlerts = (Map<String, CdssAlert>) field.get(cdssService);
            activeAlerts.put(alertId, alert);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject alert via reflection", e);
        }

        return alert;
    }
}
