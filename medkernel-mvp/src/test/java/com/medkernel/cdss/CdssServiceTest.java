package com.medkernel.cdss;

import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.rule.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CdssService 单元测试")
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

    // ──────────────────────── 辅助方法 ────────────────────────

    private Map<String, Object> buildPatientContext() {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("patient_id", "P001");
        ctx.put("encounter_id", "E001");
        return ctx;
    }

    private Map<String, Object> buildRuleResult(String ruleCode, String severity, boolean hit) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ruleCode", ruleCode);
        result.put("severity", severity);
        result.put("hit", hit);
        result.put("message", "临床决策建议");
        result.put("versionNo", "1.0.0");
        result.put("referenceDocumentCode", "DOC001");
        result.put("referenceCitationId", "CITE001");
        result.put("referenceBindingType", "MANDATORY");
        return result;
    }

    private Map<String, Object> buildRuleResultWithEvidence(String ruleCode, String severity,
                                                             boolean hit, List<Map<String, Object>> evidence) {
        Map<String, Object> result = buildRuleResult(ruleCode, severity, hit);
        result.put("evidence", evidence);
        return result;
    }

    private Map<String, Object> buildEvaluateResponse(List<Map<String, Object>> rules) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("rules", rules);
        return response;
    }

    private List<Map<String, Object>> buildRuleResultList(Map<String, Object>... results) {
        List<Map<String, Object>> list = new ArrayList<>();
        Collections.addAll(list, results);
        return list;
    }

    private CdssAlert createAndStoreAlert(String alertId, String triggerPoint, CdssRiskLevel riskLevel,
                                           String patientId, boolean blocking) {
        CdssAlert alert = new CdssAlert();
        alert.setAlertId(alertId);
        alert.setTriggerPoint(triggerPoint);
        alert.setRiskLevel(riskLevel);
        alert.setPatientId(patientId);
        alert.setBlocking(blocking);
        alert.setRuleCode("R_TEST_001");
        // 通过 evaluate 间接存储到 activeAlerts 中比较困难，直接用反射或改用 resolveAlert 前先 evaluate
        // 这里我们通过 evaluate 方法来产生 alert
        return alert;
    }

    /**
     * 通过 evaluate 方法产生并存储告警到 activeAlerts，返回产生的告警列表。
     */
    private List<CdssAlert> evaluateAndStore(String triggerPoint, String tenantId,
                                              List<Map<String, Object>> ruleResults) {
        Map<String, Object> response = buildEvaluateResponse(ruleResults);
        when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);
        return cdssService.evaluate(triggerPoint, buildPatientContext(), tenantId);
    }

    // ──────────────────────── evaluate 触发点匹配 ────────────────────────

    @Nested
    @DisplayName("evaluate 触发点匹配与评估")
    class EvaluateTests {

        @Test
        @DisplayName("医嘱下达触发点映射到 ORDER_SAFETY 场景")
        void orderPlacedMapsToOrderSafety() {
            Map<String, Object> ruleResult = buildRuleResult("R_001", "HIGH", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertFalse(alerts.isEmpty());
            verify(ruleService).evaluateForScenario(any(), any());
        }

        @Test
        @DisplayName("药品调配触发点映射到 ORDER_SAFETY 场景")
        void drugDispensedMapsToOrderSafety() {
            Map<String, Object> ruleResult = buildRuleResult("R_DRUG", "MEDIUM", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("DRUG_DISPENSED", buildPatientContext(), "tenant1");

            assertFalse(alerts.isEmpty());
        }

        @Test
        @DisplayName("病历保存触发点映射到 EMR_QC 场景")
        void emrSavedMapsToEmrQc() {
            Map<String, Object> ruleResult = buildRuleResult("R_EMR", "LOW", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("EMR_SAVED", buildPatientContext(), "tenant1");

            assertFalse(alerts.isEmpty());
        }

        @Test
        @DisplayName("出院前触发点映射到 EMR_QC 场景")
        void dischargeMapsToEmrQc() {
            Map<String, Object> ruleResult = buildRuleResult("R_DISCHARGE", "MEDIUM", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("DISCHARGE", buildPatientContext(), "tenant1");

            assertFalse(alerts.isEmpty());
        }

        @Test
        @DisplayName("检查申请触发点映射到 EXAM_RATIONALITY 场景")
        void examRequestedMapsToExamRationality() {
            Map<String, Object> ruleResult = buildRuleResult("R_EXAM", "LOW", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("EXAM_REQUESTED", buildPatientContext(), "tenant1");

            assertFalse(alerts.isEmpty());
        }

        @Test
        @DisplayName("入径评估触发点映射到 PATHWAY_ENTRY 场景")
        void pathwayEntryMapsToPathwayEntry() {
            Map<String, Object> ruleResult = buildRuleResult("R_PATH", "HIGH", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("PATHWAY_ENTRY", buildPatientContext(), "tenant1");

            assertFalse(alerts.isEmpty());
        }

        @Test
        @DisplayName("医保结算触发点映射到 INSURANCE_QC 场景")
        void insuranceSettlementMapsToInsuranceQc() {
            Map<String, Object> ruleResult = buildRuleResult("R_INS", "MEDIUM", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("INSURANCE_SETTLEMENT", buildPatientContext(), "tenant1");

            assertFalse(alerts.isEmpty());
        }

        @Test
        @DisplayName("不支持的触发点返回空列表")
        void unsupportedTriggerPointReturnsEmpty() {
            List<CdssAlert> alerts = cdssService.evaluate("UNKNOWN_TRIGGER", buildPatientContext(), "tenant1");

            assertTrue(alerts.isEmpty());
            verify(ruleService, never()).evaluateForScenario(any(), any());
        }

        @Test
        @DisplayName("触发点为 null 时返回空列表")
        void nullTriggerPointReturnsEmpty() {
            List<CdssAlert> alerts = cdssService.evaluate(null, buildPatientContext(), "tenant1");

            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("规则引擎异常时不阻断临床流程，返回空列表")
        void ruleEngineExceptionReturnsEmpty() {
            when(ruleService.evaluateForScenario(any(), any())).thenThrow(new RuntimeException("引擎异常"));

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("规则未命中时不生成告警")
        void noHitRulesProduceNoAlerts() {
            Map<String, Object> ruleResult = buildRuleResult("R_MISS", "HIGH", false);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("hit 为 null 时不生成告警")
        void nullHitProducesNoAlert() {
            Map<String, Object> ruleResult = new LinkedHashMap<>();
            ruleResult.put("ruleCode", "R_NULL_HIT");
            ruleResult.put("severity", "HIGH");
            ruleResult.put("hit", null);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("多条命中规则生成多条告警")
        void multipleHitRulesProduceMultipleAlerts() {
            Map<String, Object> rule1 = buildRuleResult("R_001", "HIGH", true);
            Map<String, Object> rule2 = buildRuleResult("R_002", "CRITICAL", true);
            Map<String, Object> rule3 = buildRuleResult("R_003", "LOW", false);
            Map<String, Object> response = buildEvaluateResponse(buildRuleResultList(rule1, rule2, rule3));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(2, alerts.size());
        }

        @Test
        @DisplayName("规则结果中 rules 字段为空列表时返回空列表")
        void emptyRulesListReturnsEmpty() {
            Map<String, Object> response = buildEvaluateResponse(Collections.emptyList());
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertTrue(alerts.isEmpty());
        }

        @Test
        @DisplayName("规则结果中无 rules 字段时返回空列表")
        void missingRulesFieldReturnsEmpty() {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("other_key", "value");
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertTrue(alerts.isEmpty());
        }
    }

    // ──────────────────────── 风险分级 ────────────────────────

    @Nested
    @DisplayName("风险分级与告警属性")
    class RiskLevelTests {

        @Test
        @DisplayName("CRITICAL 级别告警标记为阻断")
        void criticalAlertIsBlocking() {
            Map<String, Object> ruleResult = buildRuleResult("R_CRIT", "CRITICAL", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(1, alerts.size());
            assertTrue(alerts.get(0).isBlocking());
            assertEquals(CdssRiskLevel.CRITICAL, alerts.get(0).getRiskLevel());
        }

        @Test
        @DisplayName("HIGH 级别告警需要二次确认但不阻断")
        void highAlertRequiresConfirmationButNotBlocking() {
            Map<String, Object> ruleResult = buildRuleResult("R_HIGH", "HIGH", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(1, alerts.size());
            assertFalse(alerts.get(0).isBlocking());
            assertTrue(alerts.get(0).isRequiresConfirmation());
            assertEquals(CdssRiskLevel.HIGH, alerts.get(0).getRiskLevel());
        }

        @Test
        @DisplayName("MEDIUM 级别告警不阻断且不需二次确认")
        void mediumAlertNotBlockingNoConfirmation() {
            Map<String, Object> ruleResult = buildRuleResult("R_MED", "MEDIUM", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(1, alerts.size());
            assertFalse(alerts.get(0).isBlocking());
            assertFalse(alerts.get(0).isRequiresConfirmation());
        }

        @Test
        @DisplayName("WARNING severity 映射为 MEDIUM 风险等级")
        void warningSeverityMapsToMedium() {
            Map<String, Object> ruleResult = buildRuleResult("R_WARN", "WARNING", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(1, alerts.size());
            assertEquals(CdssRiskLevel.MEDIUM, alerts.get(0).getRiskLevel());
        }

        @Test
        @DisplayName("LOW 级别告警不阻断不需确认")
        void lowAlertNotBlockingNoConfirmation() {
            Map<String, Object> ruleResult = buildRuleResult("R_LOW", "LOW", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(1, alerts.size());
            assertFalse(alerts.get(0).isBlocking());
            assertFalse(alerts.get(0).isRequiresConfirmation());
        }

        @Test
        @DisplayName("severity 为 null 时默认为 INFO 级别")
        void nullSeverityDefaultsToInfo() {
            Map<String, Object> ruleResult = new LinkedHashMap<>();
            ruleResult.put("ruleCode", "R_NO_SEV");
            ruleResult.put("severity", null);
            ruleResult.put("hit", true);
            ruleResult.put("message", "测试");
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(1, alerts.size());
            assertEquals(CdssRiskLevel.INFO, alerts.get(0).getRiskLevel());
            assertFalse(alerts.get(0).isBlocking());
        }

        @Test
        @DisplayName("告警携带来源证据")
        void alertCarriesEvidence() {
            List<Map<String, Object>> evidence = new ArrayList<>();
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("documentCode", "DOC_E001");
            ev.put("citation", "临床指南2024版");
            evidence.add(ev);

            Map<String, Object> ruleResult = buildRuleResultWithEvidence("R_EVID", "HIGH", true, evidence);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(1, alerts.size());
            assertNotNull(alerts.get(0).getEvidence());
            assertEquals(1, alerts.get(0).getEvidence().size());
            assertEquals("DOC_E001", alerts.get(0).getEvidence().get(0).get("documentCode"));
        }

        @Test
        @DisplayName("告警携带来源文档信息")
        void alertCarriesSourceDocument() {
            Map<String, Object> ruleResult = buildRuleResult("R_SRC", "HIGH", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(1, alerts.size());
            assertNotNull(alerts.get(0).getSource());
            assertEquals("DOC001", alerts.get(0).getSource().get("documentCode"));
            assertEquals("CITE001", alerts.get(0).getSource().get("citationId"));
            assertEquals("MANDATORY", alerts.get(0).getSource().get("bindingType"));
        }

        @Test
        @DisplayName("告警携带患者和就诊信息")
        void alertCarriesPatientInfo() {
            Map<String, Object> ruleResult = buildRuleResult("R_PAT", "HIGH", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            Map<String, Object> ctx = buildPatientContext();
            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", ctx, "tenant1");

            assertEquals(1, alerts.size());
            assertEquals("P001", alerts.get(0).getPatientId());
            assertEquals("E001", alerts.get(0).getEncounterId());
        }

        @Test
        @DisplayName("告警 ID 以 cdss- 前缀生成")
        void alertIdStartsWithCdssPrefix() {
            Map<String, Object> ruleResult = buildRuleResult("R_ID", "HIGH", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(1, alerts.size());
            assertTrue(alerts.get(0).getAlertId().startsWith("cdss-"));
        }

        @Test
        @DisplayName("告警携带规则编码和版本号")
        void alertCarriesRuleInfo() {
            Map<String, Object> ruleResult = buildRuleResult("R_RULE_INFO", "HIGH", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            List<CdssAlert> alerts = cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            assertEquals(1, alerts.size());
            assertEquals("R_RULE_INFO", alerts.get(0).getRuleCode());
            assertEquals("1.0.0", alerts.get(0).getRuleVersion());
        }
    }

    // ──────────────────────── resolveAlert 覆盖处理 ────────────────────────

    @Nested
    @DisplayName("resolveAlert 告警确认与覆盖处理")
    class ResolveAlertTests {

        @Test
        @DisplayName("确认（ACKNOWLEDGE）非阻断告警成功")
        void acknowledgeNonBlockingAlert() {
            Map<String, Object> ruleResult = buildRuleResult("R_ACK", "MEDIUM", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            CdssAlert resolved = cdssService.resolveAlert(alertId, "ACKNOWLEDGE",
                    "已知晓", "张医生", null, "tenant1");

            assertNotNull(resolved.getOverride());
            assertEquals("ACKNOWLEDGE", resolved.getOverride().getOverrideType());
            assertEquals("已知晓", resolved.getOverride().getOverrideReason());
            assertEquals("张医生", resolved.getOverride().getOverriddenBy());
            assertFalse(resolved.getOverride().isAuditRedLine());
        }

        @Test
        @DisplayName("覆盖（OVERRIDE）非阻断告警成功")
        void overrideNonBlockingAlert() {
            Map<String, Object> ruleResult = buildRuleResult("R_OVERRIDE", "HIGH", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            CdssAlert resolved = cdssService.resolveAlert(alertId, "OVERRIDE",
                    "临床判断不需要", "李医生", null, "tenant1");

            assertNotNull(resolved.getOverride());
            assertEquals("OVERRIDE", resolved.getOverride().getOverrideType());
            assertEquals("临床判断不需要", resolved.getOverride().getOverrideReason());
            assertEquals("李医生", resolved.getOverride().getOverriddenBy());
            // HIGH 级别 OVERRIDE 不是审计红线
            assertFalse(resolved.getOverride().isAuditRedLine());
        }

        @Test
        @DisplayName("上报（ESCALATE）告警成功")
        void escalateAlert() {
            Map<String, Object> ruleResult = buildRuleResult("R_ESC", "HIGH", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            CdssAlert resolved = cdssService.resolveAlert(alertId, "ESCALATE",
                    "需上级确认", "王医生", "赵主任", "tenant1");

            assertNotNull(resolved.getOverride());
            assertEquals("ESCALATE", resolved.getOverride().getOverrideType());
            assertEquals("赵主任", resolved.getOverride().getSupervisorName());
        }

        @Test
        @DisplayName("不存在的告警 ID 抛出异常")
        void resolveNonExistentAlertThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> cdssService.resolveAlert("nonexistent-id", "ACKNOWLEDGE",
                            "原因", "张医生", null, "tenant1"));
        }

        @Test
        @DisplayName("阻断级告警覆盖必须提供上级确认人")
        void blockingAlertOverrideRequiresSupervisor() {
            Map<String, Object> ruleResult = buildRuleResult("R_BLOCK", "CRITICAL", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            assertThrows(IllegalArgumentException.class,
                    () -> cdssService.resolveAlert(alertId, "OVERRIDE",
                            "紧急情况", "张医生", null, "tenant1"));
        }

        @Test
        @DisplayName("阻断级告警覆盖上级确认人为空字符串时抛出异常")
        void blockingAlertOverrideEmptySupervisorThrows() {
            Map<String, Object> ruleResult = buildRuleResult("R_BLOCK2", "CRITICAL", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            assertThrows(IllegalArgumentException.class,
                    () -> cdssService.resolveAlert(alertId, "OVERRIDE",
                            "紧急情况", "张医生", "  ", "tenant1"));
        }

        @Test
        @DisplayName("阻断级告警覆盖提供上级确认人后成功")
        void blockingAlertOverrideWithSupervisorSucceeds() {
            Map<String, Object> ruleResult = buildRuleResult("R_BLOCK_OK", "CRITICAL", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            CdssAlert resolved = cdssService.resolveAlert(alertId, "OVERRIDE",
                    "紧急抢救", "张医生", "赵主任", "tenant1");

            assertNotNull(resolved.getOverride());
            assertEquals("OVERRIDE", resolved.getOverride().getOverrideType());
            assertEquals("赵主任", resolved.getOverride().getSupervisorName());
        }

        @Test
        @DisplayName("阻断级告警 ACKNOWLEDGE 不需要上级确认人")
        void blockingAlertAcknowledgeNoSupervisorNeeded() {
            Map<String, Object> ruleResult = buildRuleResult("R_BLOCK_ACK", "CRITICAL", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            CdssAlert resolved = cdssService.resolveAlert(alertId, "ACKNOWLEDGE",
                    "已知晓", "张医生", null, "tenant1");

            assertNotNull(resolved.getOverride());
            assertEquals("ACKNOWLEDGE", resolved.getOverride().getOverrideType());
        }

        @Test
        @DisplayName("覆盖后告警从活动列表中移除")
        void resolvedAlertRemovedFromActiveList() {
            Map<String, Object> ruleResult = buildRuleResult("R_REMOVE", "MEDIUM", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            cdssService.resolveAlert(alertId, "ACKNOWLEDGE", "已知晓", "张医生", null, "tenant1");

            assertNull(cdssService.getAlert(alertId));
        }
    }

    // ──────────────────────── 审计红线 ────────────────────────

    @Nested
    @DisplayName("审计红线：CRITICAL 级别覆盖自动写入审计日志")
    class AuditRedLineTests {

        @Test
        @DisplayName("CRITICAL 级别 OVERRIDE 标记为审计红线")
        void criticalOverrideMarkedAsAuditRedLine() {
            Map<String, Object> ruleResult = buildRuleResult("R_AUDIT", "CRITICAL", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            CdssAlert resolved = cdssService.resolveAlert(alertId, "OVERRIDE",
                    "临床判断", "张医生", "赵主任", "tenant1");

            assertTrue(resolved.getOverride().isAuditRedLine());
        }

        @Test
        @DisplayName("CRITICAL 级别 OVERRIDE 写入审计日志")
        void criticalOverrideWritesAuditLog() {
            Map<String, Object> ruleResult = buildRuleResult("R_AUDIT_LOG", "CRITICAL", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            cdssService.resolveAlert(alertId, "OVERRIDE",
                    "临床判断", "张医生", "赵主任", "tenant1");

            verify(persistenceService).saveAuditLog(
                    eq("CDSS"), eq("CRITICAL_OVERRIDE"), eq("ALERT"),
                    eq(alertId), eq("P001"), eq(null), eq("张医生"),
                    any(Map.class));
        }

        @Test
        @DisplayName("非 CRITICAL 级别 OVERRIDE 不标记为审计红线")
        void nonCriticalOverrideNotAuditRedLine() {
            Map<String, Object> ruleResult = buildRuleResult("R_NO_AUDIT", "HIGH", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            CdssAlert resolved = cdssService.resolveAlert(alertId, "OVERRIDE",
                    "临床判断", "张医生", null, "tenant1");

            assertFalse(resolved.getOverride().isAuditRedLine());
        }

        @Test
        @DisplayName("非 CRITICAL 级别操作写入普通确认日志")
        void nonCriticalOverrideWritesNormalLog() {
            Map<String, Object> ruleResult = buildRuleResult("R_NORMAL_LOG", "HIGH", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            cdssService.resolveAlert(alertId, "OVERRIDE",
                    "临床判断", "张医生", null, "tenant1");

            verify(persistenceService).saveAuditLog(
                    eq("CDSS"), eq("OVERRIDE"), eq("ALERT"),
                    eq(alertId), eq("P001"), eq(null), eq("张医生"),
                    any(Map.class));
        }

        @Test
        @DisplayName("ACKNOWLEDGE 操作写入普通确认日志")
        void acknowledgeWritesNormalLog() {
            Map<String, Object> ruleResult = buildRuleResult("R_ACK_LOG", "MEDIUM", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            cdssService.resolveAlert(alertId, "ACKNOWLEDGE",
                    "已知晓", "张医生", null, "tenant1");

            verify(persistenceService).saveAuditLog(
                    eq("CDSS"), eq("ACKNOWLEDGE"), eq("ALERT"),
                    eq(alertId), eq("P001"), eq(null), eq("张医生"),
                    any(Map.class));
        }

        @Test
        @DisplayName("CRITICAL 级别 ACKNOWLEDGE 不触发审计红线日志")
        void criticalAcknowledgeNotAuditRedLine() {
            Map<String, Object> ruleResult = buildRuleResult("R_CRIT_ACK", "CRITICAL", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            CdssAlert resolved = cdssService.resolveAlert(alertId, "ACKNOWLEDGE",
                    "已知晓", "张医生", null, "tenant1");

            assertFalse(resolved.getOverride().isAuditRedLine());
            verify(persistenceService).saveAuditLog(
                    eq("CDSS"), eq("ACKNOWLEDGE"), eq("ALERT"),
                    eq(alertId), eq("P001"), eq(null), eq("张医生"),
                    any(Map.class));
        }
    }

    // ──────────────────────── listActiveAlerts / getAlert ────────────────────────

    @Nested
    @DisplayName("活动告警查询")
    class ActiveAlertQueryTests {

        @Test
        @DisplayName("查询全部活动告警")
        void listAllActiveAlerts() {
            Map<String, Object> rule1 = buildRuleResult("R_001", "HIGH", true);
            Map<String, Object> rule2 = buildRuleResult("R_002", "MEDIUM", true);
            Map<String, Object> response = buildEvaluateResponse(buildRuleResultList(rule1, rule2));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            List<CdssAlert> active = cdssService.listActiveAlerts(null);

            assertEquals(2, active.size());
        }

        @Test
        @DisplayName("按患者 ID 过滤活动告警")
        void listActiveAlertsByPatientId() {
            Map<String, Object> ctx1 = new LinkedHashMap<>();
            ctx1.put("patient_id", "P001");
            ctx1.put("encounter_id", "E001");

            Map<String, Object> ctx2 = new LinkedHashMap<>();
            ctx2.put("patient_id", "P002");
            ctx2.put("encounter_id", "E002");

            Map<String, Object> ruleResult = buildRuleResult("R_001", "HIGH", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));

            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);
            cdssService.evaluate("ORDER_PLACED", ctx1, "tenant1");
            cdssService.evaluate("EMR_SAVED", ctx2, "tenant1");

            List<CdssAlert> p001Alerts = cdssService.listActiveAlerts("P001");

            assertEquals(1, p001Alerts.size());
            assertEquals("P001", p001Alerts.get(0).getPatientId());
        }

        @Test
        @DisplayName("空患者 ID 返回全部告警")
        void emptyPatientIdReturnsAll() {
            Map<String, Object> ruleResult = buildRuleResult("R_001", "HIGH", true);
            Map<String, Object> response = buildEvaluateResponse(Collections.singletonList(ruleResult));
            when(ruleService.evaluateForScenario(any(), any())).thenReturn(response);

            cdssService.evaluate("ORDER_PLACED", buildPatientContext(), "tenant1");

            List<CdssAlert> active = cdssService.listActiveAlerts("");

            assertEquals(1, active.size());
        }

        @Test
        @DisplayName("无活动告警时返回空列表")
        void noActiveAlertsReturnsEmpty() {
            List<CdssAlert> active = cdssService.listActiveAlerts(null);

            assertTrue(active.isEmpty());
        }

        @Test
        @DisplayName("按告警 ID 获取告警详情")
        void getAlertById() {
            Map<String, Object> ruleResult = buildRuleResult("R_GET", "HIGH", true);
            List<CdssAlert> alerts = evaluateAndStore("ORDER_PLACED", "tenant1",
                    Collections.singletonList(ruleResult));

            String alertId = alerts.get(0).getAlertId();
            CdssAlert found = cdssService.getAlert(alertId);

            assertNotNull(found);
            assertEquals(alertId, found.getAlertId());
        }

        @Test
        @DisplayName("获取不存在的告警返回 null")
        void getNonExistentAlertReturnsNull() {
            assertNull(cdssService.getAlert("nonexistent-id"));
        }
    }

    // ──────────────────────── AlertFatigueService 疲劳治理 ────────────────────────

    @Nested
    @DisplayName("AlertFatigueService 疲劳治理")
    class AlertFatigueTests {

        private AlertFatigueService fatigueService;

        @BeforeEach
        void setUpFatigue() {
            fatigueService = new AlertFatigueService(persistenceService, organizationContextService);
        }

        private OrganizationContext buildOrgContext() {
            OrganizationContext ctx = new OrganizationContext();
            ctx.setTenantId("tenant1");
            return ctx;
        }

        private CdssAlert buildAlert(String alertId, String ruleCode, String patientId,
                                      String triggerPoint, CdssRiskLevel riskLevel) {
            CdssAlert alert = new CdssAlert();
            alert.setAlertId(alertId);
            alert.setRuleCode(ruleCode);
            alert.setPatientId(patientId);
            alert.setTriggerPoint(triggerPoint);
            alert.setRiskLevel(riskLevel);
            return alert;
        }

        @Test
        @DisplayName("创建疲劳治理配置")
        void createFatigueConfig() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("trigger_point", "ORDER_PLACED");
            request.put("risk_level", "HIGH");
            request.put("deduplication_enabled", "true");
            request.put("deduplication_window_minutes", "30");
            request.put("suppression_enabled", "true");
            request.put("suppression_max_alerts_per_hour", "20");
            request.put("quiet_period_enabled", "true");
            request.put("quiet_period_minutes", "60");
            request.put("smart_filter_enabled", "true");
            request.put("override_rate_threshold", "0.8");
            request.put("created_by", "admin");

            AlertFatigueConfig config = fatigueService.createConfig(request, orgContext);

            assertNotNull(config.getConfigId());
            assertTrue(config.getConfigId().startsWith("AFC-"));
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
        }

        @Test
        @DisplayName("创建配置时使用默认值")
        void createConfigWithDefaults() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> request = new LinkedHashMap<>();

            AlertFatigueConfig config = fatigueService.createConfig(request, orgContext);

            assertTrue(config.isDeduplicationEnabled());
            assertEquals(30, config.getDeduplicationWindowMinutes());
            assertTrue(config.isSuppressionEnabled());
            assertEquals(20, config.getSuppressionMaxAlertsPerHour());
            assertTrue(config.isQuietPeriodEnabled());
            assertEquals(60, config.getQuietPeriodMinutes());
            assertTrue(config.isSmartFilterEnabled());
            assertEquals(0.8, config.getOverrideRateThreshold(), 0.001);
        }

        @Test
        @DisplayName("查询租户下的疲劳治理配置列表")
        void listConfigsByTenant() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("trigger_point", "ORDER_PLACED");
            fatigueService.createConfig(request, orgContext);

            List<AlertFatigueConfig> configs = fatigueService.listConfigs(orgContext);

            assertEquals(1, configs.size());
        }

        @Test
        @DisplayName("不同租户的配置互不可见")
        void listConfigsIsolatedByTenant() {
            OrganizationContext orgCtx1 = buildOrgContext();
            OrganizationContext orgCtx2 = new OrganizationContext();
            orgCtx2.setTenantId("tenant2");

            fatigueService.createConfig(new LinkedHashMap<>(), orgCtx1);
            fatigueService.createConfig(new LinkedHashMap<>(), orgCtx1);
            fatigueService.createConfig(new LinkedHashMap<>(), orgCtx2);

            assertEquals(2, fatigueService.listConfigs(orgCtx1).size());
            assertEquals(1, fatigueService.listConfigs(orgCtx2).size());
        }

        @Test
        @DisplayName("更新疲劳治理配置")
        void updateFatigueConfig() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> createReq = new LinkedHashMap<>();
            AlertFatigueConfig config = fatigueService.createConfig(createReq, orgContext);

            Map<String, Object> updateReq = new LinkedHashMap<>();
            updateReq.put("deduplication_window_minutes", "60");
            updateReq.put("suppression_max_alerts_per_hour", "10");
            updateReq.put("status", "DISABLED");

            AlertFatigueConfig updated = fatigueService.updateConfig(config.getConfigId(), updateReq, orgContext);

            assertEquals(60, updated.getDeduplicationWindowMinutes());
            assertEquals(10, updated.getSuppressionMaxAlertsPerHour());
            assertEquals("DISABLED", updated.getStatus());
        }

        @Test
        @DisplayName("更新不存在的配置抛出异常")
        void updateNonExistentConfigThrows() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> updateReq = new LinkedHashMap<>();

            assertThrows(IllegalArgumentException.class,
                    () -> fatigueService.updateConfig("AFC-9999", updateReq, orgContext));
        }

        @Test
        @DisplayName("无匹配配置时告警不被过滤")
        void noConfigNoFilter() {
            CdssAlert alert = buildAlert("a1", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);

            assertFalse(fatigueService.shouldFilterAlert(alert, "tenant1"));
        }

        @Test
        @DisplayName("DISABLED 状态的配置不生效")
        void disabledConfigNotEffective() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> createReq = new LinkedHashMap<>();
            createReq.put("trigger_point", "ORDER_PLACED");
            createReq.put("deduplication_enabled", "true");
            AlertFatigueConfig config = fatigueService.createConfig(createReq, orgContext);

            Map<String, Object> updateReq = new LinkedHashMap<>();
            updateReq.put("status", "DISABLED");
            fatigueService.updateConfig(config.getConfigId(), updateReq, orgContext);

            CdssAlert alert = buildAlert("a1", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            assertFalse(fatigueService.shouldFilterAlert(alert, "tenant1"));
        }

        @Test
        @DisplayName("告警去重：同一患者同一规则在窗口内被过滤")
        void deduplicationFiltersDuplicate() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> createReq = new LinkedHashMap<>();
            createReq.put("trigger_point", "ORDER_PLACED");
            createReq.put("deduplication_enabled", "true");
            createReq.put("deduplication_window_minutes", "30");
            fatigueService.createConfig(createReq, orgContext);

            CdssAlert alert1 = buildAlert("a1", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            fatigueService.recordAlert(alert1);

            CdssAlert alert2 = buildAlert("a2", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            assertTrue(fatigueService.shouldFilterAlert(alert2, "tenant1"));
        }

        @Test
        @DisplayName("告警去重：不同患者不被去重")
        void deduplicationDifferentPatientNotFiltered() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> createReq = new LinkedHashMap<>();
            createReq.put("trigger_point", "ORDER_PLACED");
            createReq.put("deduplication_enabled", "true");
            createReq.put("deduplication_window_minutes", "30");
            fatigueService.createConfig(createReq, orgContext);

            CdssAlert alert1 = buildAlert("a1", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            fatigueService.recordAlert(alert1);

            CdssAlert alert2 = buildAlert("a2", "R_001", "P002", "ORDER_PLACED", CdssRiskLevel.HIGH);
            assertFalse(fatigueService.shouldFilterAlert(alert2, "tenant1"));
        }

        @Test
        @DisplayName("告警抑制：每小时最大告警数限制")
        void suppressionLimitsAlertsPerHour() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> createReq = new LinkedHashMap<>();
            createReq.put("trigger_point", "ORDER_PLACED");
            createReq.put("suppression_enabled", "true");
            createReq.put("suppression_max_alerts_per_hour", "2");
            fatigueService.createConfig(createReq, orgContext);

            CdssAlert alert1 = buildAlert("a1", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            CdssAlert alert2 = buildAlert("a2", "R_002", "P001", "ORDER_PLACED", CdssRiskLevel.MEDIUM);
            fatigueService.recordAlert(alert1);
            fatigueService.recordAlert(alert2);

            CdssAlert alert3 = buildAlert("a3", "R_003", "P001", "ORDER_PLACED", CdssRiskLevel.LOW);
            assertTrue(fatigueService.shouldFilterAlert(alert3, "tenant1"));
        }

        @Test
        @DisplayName("静默期：覆盖后同类告警暂时不提醒")
        void quietPeriodSuppressesAfterOverride() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> createReq = new LinkedHashMap<>();
            createReq.put("trigger_point", "ORDER_PLACED");
            createReq.put("quiet_period_enabled", "true");
            createReq.put("quiet_period_minutes", "60");
            fatigueService.createConfig(createReq, orgContext);

            CdssAlert alert1 = buildAlert("a1", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            fatigueService.recordOverride(alert1, "OVERRIDE", "张医生");

            CdssAlert alert2 = buildAlert("a2", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            assertTrue(fatigueService.shouldFilterAlert(alert2, "tenant1"));
        }

        @Test
        @DisplayName("静默期：ACKNOWLEDGE 操作不触发静默期")
        void quietPeriodNotTriggeredByAcknowledge() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> createReq = new LinkedHashMap<>();
            createReq.put("trigger_point", "ORDER_PLACED");
            createReq.put("quiet_period_enabled", "true");
            createReq.put("quiet_period_minutes", "60");
            fatigueService.createConfig(createReq, orgContext);

            CdssAlert alert1 = buildAlert("a1", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            fatigueService.recordOverride(alert1, "ACKNOWLEDGE", "张医生");

            CdssAlert alert2 = buildAlert("a2", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            assertFalse(fatigueService.shouldFilterAlert(alert2, "tenant1"));
        }

        @Test
        @DisplayName("记录告警历史")
        void recordAlertHistory() {
            CdssAlert alert = buildAlert("a1", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            fatigueService.recordAlert(alert);

            // 验证记录后能影响去重判断
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> createReq = new LinkedHashMap<>();
            createReq.put("trigger_point", "ORDER_PLACED");
            createReq.put("deduplication_enabled", "true");
            createReq.put("deduplication_window_minutes", "30");
            fatigueService.createConfig(createReq, orgContext);

            CdssAlert duplicate = buildAlert("a2", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            assertTrue(fatigueService.shouldFilterAlert(duplicate, "tenant1"));
        }

        @Test
        @DisplayName("覆盖模式分析报告")
        void overrideAnalysisReport() {
            CdssAlert alert1 = buildAlert("a1", "R_001", "P001", "ORDER_PLACED", CdssRiskLevel.HIGH);
            CdssAlert alert2 = buildAlert("a2", "R_002", "P002", "EMR_SAVED", CdssRiskLevel.MEDIUM);
            fatigueService.recordAlert(alert1);
            fatigueService.recordAlert(alert2);
            fatigueService.recordOverride(alert1, "OVERRIDE", "张医生");
            fatigueService.recordOverride(alert2, "ACKNOWLEDGE", "李医生");

            Map<String, Object> report = fatigueService.getOverrideAnalysis("tenant1");

            assertEquals(2, report.get("total_alerts"));
            assertEquals(1, report.get("total_overrides"));
            assertEquals(1, report.get("total_acknowledges"));
            assertEquals(0, report.get("total_escalations"));
            assertNotNull(report.get("override_by_rule"));
            assertNotNull(report.get("override_by_trigger"));
            assertNotNull(report.get("override_by_operator"));
        }

        @Test
        @DisplayName("配置 toView 输出完整字段")
        void configToViewOutput() {
            OrganizationContext orgContext = buildOrgContext();
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("trigger_point", "EMR_SAVED");
            request.put("risk_level", "MEDIUM");
            AlertFatigueConfig config = fatigueService.createConfig(request, orgContext);

            Map<String, Object> view = config.toView();

            assertEquals("tenant1", view.get("tenant_id"));
            assertNotNull(view.get("config_id"));
            assertEquals("EMR_SAVED", view.get("trigger_point"));
            assertEquals("MEDIUM", view.get("risk_level"));
            assertNotNull(view.get("deduplication_enabled"));
            assertNotNull(view.get("suppression_enabled"));
            assertNotNull(view.get("quiet_period_enabled"));
            assertNotNull(view.get("smart_filter_enabled"));
            assertEquals("ACTIVE", view.get("status"));
        }
    }

    // ──────────────────────── CdssRiskLevel 枚举 ────────────────────────

    @Nested
    @DisplayName("CdssRiskLevel 风险分级枚举")
    class RiskLevelEnumTests {

        @Test
        @DisplayName("CRITICAL 级别为阻断级别")
        void criticalIsBlocking() {
            assertTrue(CdssRiskLevel.CRITICAL.isBlocking());
        }

        @Test
        @DisplayName("非 CRITICAL 级别均不阻断")
        void nonCriticalNotBlocking() {
            assertFalse(CdssRiskLevel.INFO.isBlocking());
            assertFalse(CdssRiskLevel.LOW.isBlocking());
            assertFalse(CdssRiskLevel.MEDIUM.isBlocking());
            assertFalse(CdssRiskLevel.HIGH.isBlocking());
        }

        @Test
        @DisplayName("fromSeverity 正确映射各严重程度")
        void fromSeverityMapping() {
            assertEquals(CdssRiskLevel.CRITICAL, CdssRiskLevel.fromSeverity("CRITICAL"));
            assertEquals(CdssRiskLevel.HIGH, CdssRiskLevel.fromSeverity("HIGH"));
            assertEquals(CdssRiskLevel.MEDIUM, CdssRiskLevel.fromSeverity("MEDIUM"));
            assertEquals(CdssRiskLevel.MEDIUM, CdssRiskLevel.fromSeverity("WARNING"));
            assertEquals(CdssRiskLevel.LOW, CdssRiskLevel.fromSeverity("LOW"));
            assertEquals(CdssRiskLevel.INFO, CdssRiskLevel.fromSeverity("INFO"));
        }

        @Test
        @DisplayName("fromSeverity 大小写不敏感")
        void fromSeverityCaseInsensitive() {
            assertEquals(CdssRiskLevel.CRITICAL, CdssRiskLevel.fromSeverity("critical"));
            assertEquals(CdssRiskLevel.HIGH, CdssRiskLevel.fromSeverity("High"));
        }

        @Test
        @DisplayName("fromSeverity null 默认为 INFO")
        void fromSeverityNullDefaultsToInfo() {
            assertEquals(CdssRiskLevel.INFO, CdssRiskLevel.fromSeverity(null));
        }

        @Test
        @DisplayName("fromSeverity 未知值默认为 INFO")
        void fromSeverityUnknownDefaultsToInfo() {
            assertEquals(CdssRiskLevel.INFO, CdssRiskLevel.fromSeverity("UNKNOWN"));
        }

        @Test
        @DisplayName("各风险等级 code 和 label 正确")
        void riskLevelCodeAndLabel() {
            assertEquals("INFO", CdssRiskLevel.INFO.getCode());
            assertEquals("信息提示", CdssRiskLevel.INFO.getLabel());
            assertEquals("LOW", CdssRiskLevel.LOW.getCode());
            assertEquals("低风险提醒", CdssRiskLevel.LOW.getLabel());
            assertEquals("MEDIUM", CdssRiskLevel.MEDIUM.getCode());
            assertEquals("中风险提醒", CdssRiskLevel.MEDIUM.getLabel());
            assertEquals("HIGH", CdssRiskLevel.HIGH.getCode());
            assertEquals("高风险强提醒", CdssRiskLevel.HIGH.getLabel());
            assertEquals("CRITICAL", CdssRiskLevel.CRITICAL.getCode());
            assertEquals("危急阻断", CdssRiskLevel.CRITICAL.getLabel());
        }
    }
}
