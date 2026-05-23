package com.medkernel.rule;

import com.medkernel.common.exception.MissingSourceException;
import com.medkernel.dto.RuleResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private RuleEvalResultRepository ruleEvalResultRepository;

    @Mock
    private RuleExecutionLogService ruleExecutionLogService;

    private RuleService ruleService;

    @BeforeEach
    void setUp() {
        ruleService = new RuleService(persistenceService, ruleEvalResultRepository, ruleExecutionLogService);
    }

    // ==================== importRules ====================

    @Test
    void shouldImportRules_whenGivenListOfMaps() {
        Map<String, Object> rule1 = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        Map<String, Object> rule2 = buildRuleMap("R002", "Rule 2", "EMR_QC");

        List<Map<String, Object>> request = Arrays.asList(rule1, rule2);
        List<RuleDefinition> imported = ruleService.importRules(request);

        assertEquals(2, imported.size());
        assertEquals("R001", imported.get(0).getRuleCode());
        assertEquals("R002", imported.get(1).getRuleCode());
        assertEquals("DRAFT", imported.get(0).getStatus());
        verify(persistenceService, times(2)).saveRuleDefinition(any(RuleDefinition.class), isNull());
    }

    @Test
    void shouldImportRules_whenGivenMapWithRulesKey() {
        Map<String, Object> rule1 = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("rules", Collections.singletonList(rule1));
        wrapper.put("package_code", "PKG001");
        wrapper.put("package_version", "1.0.0");

        List<RuleDefinition> imported = ruleService.importRules(wrapper);

        assertEquals(1, imported.size());
        assertEquals("PKG001", imported.get(0).getPackageCode());
        assertEquals("1.0.0", imported.get(0).getPackageVersion());
    }

    @Test
    void shouldImportRules_whenGivenSingleMap() {
        Map<String, Object> rule = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        List<RuleDefinition> imported = ruleService.importRules(rule);
        assertEquals(1, imported.size());
        assertEquals("R001", imported.get(0).getRuleCode());
    }

    @Test
    void shouldImportRules_withOrganizationContext() {
        Map<String, Object> rule = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");

        List<RuleDefinition> imported = ruleService.importRules(Collections.singletonList(rule), orgContext);

        assertEquals(1, imported.size());
        assertEquals("T001", imported.get(0).getTenantId());
        assertEquals("HOSP001", imported.get(0).getHospitalCode());
    }

    @Test
    void shouldThrowException_whenImportingRuleWithoutRuleCode() {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_name", "No Code");

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.importRules(Collections.singletonList(rule)));
    }

    @Test
    void shouldInheritPackageCode_whenWrapperHasPackageCodeButRulesDoNot() {
        Map<String, Object> rule = buildRuleMap("R001", "Rule 1", "GENERAL");
        // rule does not have package_code
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("rules", Collections.singletonList(rule));
        wrapper.put("package_code", "PKG_INHERITED");

        List<RuleDefinition> imported = ruleService.importRules(wrapper);
        assertEquals("PKG_INHERITED", imported.get(0).getPackageCode());
    }

    @Test
    void shouldNotOverridePackageCode_whenRuleAlreadyHasOne() {
        Map<String, Object> rule = buildRuleMap("R001", "Rule 1", "GENERAL");
        rule.put("package_code", "PKG_OWN");
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("rules", Collections.singletonList(rule));
        wrapper.put("package_code", "PKG_INHERITED");

        List<RuleDefinition> imported = ruleService.importRules(wrapper);
        assertEquals("PKG_OWN", imported.get(0).getPackageCode());
    }

    // ==================== publish ====================

    @Test
    void shouldPublishRule_whenRuleExistsAndHasReference() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");
        request.put("approved_by", "admin");

        RuleDefinition published = ruleService.publish("R001", request);

        assertEquals("PUBLISHED", published.getStatus());
        assertEquals("admin", published.getPublishedBy());
        assertNotNull(published.getPublishedTime());
        verify(persistenceService).saveRuleDefinition(any(RuleDefinition.class), eq("admin"));
    }

    @Test
    void shouldThrowException_whenPublishingNonExistentRule() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.publish("NON_EXISTENT", request));
    }

    @Test
    void shouldThrowMissingSourceException_whenPublishingRuleWithoutReference() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        // no reference_document_code
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");

        assertThrows(MissingSourceException.class,
                () -> ruleService.publish("R001", request));
    }

    @Test
    void shouldPublishRule_withOrganizationContext() {
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");
        Map<String, Object> ruleMap = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");

        ruleService.importRules(Collections.singletonList(ruleMap), orgContext);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");
        request.put("approved_by", "admin");

        RuleDefinition published = ruleService.publish("R001", request, orgContext);
        assertEquals("PUBLISHED", published.getStatus());
    }

    // ==================== reviewPackage ====================

    @Test
    void shouldReviewPackage_whenPackageExists() {
        importRulesInPackage("PKG001", "1.0.0", 3);

        Map<String, Object> review = ruleService.reviewPackage("PKG001", "1.0.0");

        assertEquals("PKG001", review.get("package_code"));
        assertEquals(3, review.get("total_rules"));
        assertTrue(review.containsKey("ready_to_publish"));
        assertTrue(review.containsKey("issues"));
        assertTrue(review.containsKey("by_type"));
        assertTrue(review.containsKey("by_status"));
    }

    @Test
    void shouldThrowException_whenReviewingNonExistentPackage() {
        assertThrows(IllegalArgumentException.class,
                () -> ruleService.reviewPackage("NON_EXISTENT", "1.0.0"));
    }

    @Test
    void shouldReviewPackage_withOrgContext() {
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");
        Map<String, Object> ruleMap = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        ruleMap.put("package_code", "PKG001");
        ruleMap.put("package_version", "1.0.0");
        ruleService.importRules(Collections.singletonList(ruleMap), orgContext);

        Map<String, Object> review = ruleService.reviewPackage("PKG001", "1.0.0", orgContext);
        assertEquals(1, review.get("total_rules"));
    }

    // ==================== publishPackage ====================

    @Test
    void shouldPublishPackage_whenAllRulesHaveReferences() {
        Map<String, Object> rule1 = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        rule1.put("package_code", "PKG001");
        rule1.put("package_version", "1.0.0");
        rule1.put("reference_document_code", "DOC001");

        Map<String, Object> rule2 = buildRuleMap("R002", "Rule 2", "EMR_QC");
        rule2.put("package_code", "PKG001");
        rule2.put("package_version", "1.0.0");
        rule2.put("reference_document_code", "DOC002");

        ruleService.importRules(Arrays.asList(rule1, rule2));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("package_version", "1.0.0");
        request.put("approved_by", "admin");

        Map<String, Object> result = ruleService.publishPackage("PKG001", request);

        assertEquals(2, result.get("published_count"));
        assertEquals("admin", result.get("published_by"));
        verify(persistenceService, atLeast(2)).saveRuleDefinition(any(RuleDefinition.class), eq("admin"));
    }

    @Test
    void shouldThrowMissingSourceException_whenPublishingPackageWithoutReferences() {
        Map<String, Object> rule1 = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        rule1.put("package_code", "PKG001");
        rule1.put("package_version", "1.0.0");
        // no reference_document_code

        ruleService.importRules(Collections.singletonList(rule1));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("package_version", "1.0.0");

        assertThrows(MissingSourceException.class,
                () -> ruleService.publishPackage("PKG001", request));
    }

    @Test
    void shouldThrowIllegalArgumentException_whenPublishingPackageWithDslIssues() {
        Map<String, Object> rule1 = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        rule1.put("package_code", "PKG001");
        rule1.put("package_version", "1.0.0");
        rule1.put("reference_document_code", "DOC001");
        // condition with empty all[] - DSL issue
        rule1.put("condition", Map.of("all", Collections.emptyList()));

        ruleService.importRules(Collections.singletonList(rule1));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("package_version", "1.0.0");

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.publishPackage("PKG001", request));
    }

    @Test
    void shouldThrowException_whenPublishingNonExistentPackage() {
        Map<String, Object> request = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class,
                () -> ruleService.publishPackage("NON_EXISTENT", request));
    }

    @Test
    void shouldThrowException_whenPackageCodeIsNull() {
        Map<String, Object> request = new LinkedHashMap<>();
        assertThrows(IllegalArgumentException.class,
                () -> ruleService.publishPackage(null, request));
    }

    // ==================== listRules ====================

    @Test
    void shouldListAllRules_whenNoFilters() {
        ruleService.importRules(Arrays.asList(
                buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY"),
                buildRuleMap("R002", "Rule 2", "EMR_QC")));

        List<RuleDefinition> rules = ruleService.listRules();

        assertEquals(2, rules.size());
    }

    @Test
    void shouldListRulesSortedByRuleCode() {
        ruleService.importRules(Arrays.asList(
                buildRuleMap("R002", "Rule 2", "EMR_QC"),
                buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY")));

        List<RuleDefinition> rules = ruleService.listRules();

        assertEquals("R001", rules.get(0).getRuleCode());
        assertEquals("R002", rules.get(1).getRuleCode());
    }

    @Test
    void shouldFilterRulesByTenantId() {
        OrganizationContext org1 = buildOrgContext("T001", "HOSP001");
        OrganizationContext org2 = buildOrgContext("T002", "HOSP002");

        ruleService.importRules(Collections.singletonList(buildRuleMap("R001", "Rule 1", "GENERAL")), org1);
        ruleService.importRules(Collections.singletonList(buildRuleMap("R002", "Rule 2", "GENERAL")), org2);

        Map<String, String> filters = new HashMap<>();
        filters.put("tenantId", "T001");

        List<RuleDefinition> rules = ruleService.listRules(filters);
        assertEquals(1, rules.size());
        assertEquals("R001", rules.get(0).getRuleCode());
    }

    @Test
    void shouldFilterRulesByHospitalCode() {
        OrganizationContext org1 = buildOrgContext("T001", "HOSP001");
        OrganizationContext org2 = buildOrgContext("T001", "HOSP002");

        ruleService.importRules(Collections.singletonList(buildRuleMap("R001", "Rule 1", "GENERAL")), org1);
        ruleService.importRules(Collections.singletonList(buildRuleMap("R002", "Rule 2", "GENERAL")), org2);

        Map<String, String> filters = new HashMap<>();
        filters.put("hospitalCode", "HOSP001");

        List<RuleDefinition> rules = ruleService.listRules(filters);
        assertEquals(1, rules.size());
    }

    @Test
    void shouldReturnEmptyList_whenNoRulesImported() {
        List<RuleDefinition> rules = ruleService.listRules();
        assertTrue(rules.isEmpty());
    }

    // ==================== getRule ====================

    @Test
    void shouldGetRule_byCodeAndVersion() {
        ruleService.importRules(Collections.singletonList(buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY")));

        RuleDefinition rule = ruleService.getRule("R001", "1.0.0");

        assertNotNull(rule);
        assertEquals("R001", rule.getRuleCode());
    }

    @Test
    void shouldGetRule_latestWhenVersionIsNull() {
        ruleService.importRules(Collections.singletonList(buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY")));

        RuleDefinition rule = ruleService.getRule("R001", null);

        assertNotNull(rule);
        assertEquals("R001", rule.getRuleCode());
    }

    @Test
    void shouldReturnNull_whenRuleNotFound() {
        RuleDefinition rule = ruleService.getRule("NON_EXISTENT", "1.0.0");
        assertNull(rule);
    }

    @Test
    void shouldGetRule_withOrganizationContext() {
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");
        ruleService.importRules(Collections.singletonList(buildRuleMap("R001", "Rule 1", "GENERAL")), orgContext);

        RuleDefinition rule = ruleService.getRule("R001", "1.0.0", orgContext);
        assertNotNull(rule);
        assertEquals("T001", rule.getTenantId());
    }

    @Test
    void shouldFallbackToLegacyDefault_whenOrgContextRuleNotFound() {
        // Import without org context (becomes legacy default)
        ruleService.importRules(Collections.singletonList(buildRuleMap("R001", "Rule 1", "GENERAL")));

        OrganizationContext orgContext = buildOrgContext("T_OTHER", "HOSP_OTHER");
        RuleDefinition rule = ruleService.getRule("R001", null, orgContext);

        assertNotNull(rule);
        assertEquals("R001", rule.getRuleCode());
    }

    // ==================== evaluate ====================

    @Test
    void shouldEvaluateBuiltInRules_whenNoPublishedRules() {
        Map<String, Object> patientContext = buildStemiPatientContext();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(request);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        // Built-in STEMI rule should be present
        assertTrue(results.stream().anyMatch(r -> "R_AMI_STEMI_CANDIDATE".equals(r.getRuleCode())));
    }

    @Test
    void shouldEvaluatePublishedRules_whenAvailable() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Test Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");
        request.put("approved_by", "admin");
        ruleService.publish("R001", request);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        Map<String, Object> patient = new LinkedHashMap<>();
        patient.put("age", "65");
        patientContext.put("patient", patient);

        Map<String, Object> evalRequest = new LinkedHashMap<>();
        evalRequest.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(evalRequest);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("R001", results.get(0).getRuleCode());
    }

    @Test
    void shouldEvaluateWithOrgContext() {
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");
        Map<String, Object> ruleMap = buildRuleMap("R001", "Test Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap), orgContext);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");
        request.put("approved_by", "admin");
        ruleService.publish("R001", request, orgContext);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        Map<String, Object> patient = new LinkedHashMap<>();
        patient.put("age", "65");
        patientContext.put("patient", patient);

        Map<String, Object> evalRequest = new LinkedHashMap<>();
        evalRequest.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(evalRequest, orgContext);
        assertNotNull(results);
    }

    // ==================== evaluateForScenario ====================

    @Test
    void shouldEvaluateForScenario_whenValidRequest() {
        // Import and publish a rule with scenario_codes
        Map<String, Object> ruleMap = buildRuleMap("R001", "Pathway Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        Map<String, Object> patient = new LinkedHashMap<>();
        patient.put("age", "65");
        patientContext.put("patient", patient);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", patientContext);

        Map<String, Object> result = ruleService.evaluateForScenario(request);

        assertNotNull(result.get("result_id"));
        assertNotNull(result.get("trace_id"));
        assertEquals("PATHWAY_ENTRY", result.get("scenario_code"));
        assertTrue(result.containsKey("results"));
    }

    @Test
    void shouldThrowException_whenScenarioCodeMissing() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", Map.of("patient", Map.of("age", "65")));

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.evaluateForScenario(request));
    }

    @Test
    void shouldThrowException_whenUnsupportedScenarioCode() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "UNSUPPORTED_SCENARIO");
        request.put("patient_context", Map.of("patient", Map.of("age", "65")));

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.evaluateForScenario(request));
    }

    @Test
    void shouldThrowException_whenPatientContextMissing() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.evaluateForScenario(request));
    }

    @Test
    void shouldThrowException_whenPatientContextEmpty() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", Collections.emptyMap());

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.evaluateForScenario(request));
    }

    @Test
    void shouldThrowException_whenRequestIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> ruleService.evaluateForScenario(null));
    }

    @Test
    void shouldEvaluateForScenario_withOrgContext() {
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");
        Map<String, Object> ruleMap = buildRuleMap("R001", "Pathway Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap), orgContext);

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest, orgContext);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        Map<String, Object> patient = new LinkedHashMap<>();
        patient.put("age", "65");
        patientContext.put("patient", patient);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", patientContext);

        Map<String, Object> result = ruleService.evaluateForScenario(request, orgContext);
        assertNotNull(result.get("result_id"));
        assertEquals("T001", result.get("tenant_id"));
    }

    @Test
    void shouldReturnWarning_whenNoRulesMatchScenario() {
        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", patientContext);

        Map<String, Object> result = ruleService.evaluateForScenario(request);

        List<?> warnings = (List<?>) result.get("warnings");
        assertNotNull(warnings);
        assertFalse(warnings.isEmpty());
        Map<?, ?> warning = (Map<?, ?>) warnings.get(0);
        assertEquals("NO_RULES_MATCHED", warning.get("code"));
    }

    @Test
    void shouldUseDefaultOrg_whenNoOrgContext() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Pathway Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", patientContext);

        Map<String, Object> result = ruleService.evaluateForScenario(request);
        assertEquals("default", result.get("tenant_id"));
    }

    // ==================== batchEvaluateForScenario ====================

    @Test
    void shouldBatchEvaluateForScenario_whenValidRequest() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Pathway Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext1 = new LinkedHashMap<>();
        patientContext1.put("patient", Map.of("age", "65"));
        Map<String, Object> patientContext2 = new LinkedHashMap<>();
        patientContext2.put("patient", Map.of("age", "70"));

        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("patient_context", patientContext1);
        item1.put("case_id", "CASE001");
        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("patient_context", patientContext2);
        item2.put("case_id", "CASE002");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("items", Arrays.asList(item1, item2));

        Map<String, Object> result = ruleService.batchEvaluateForScenario(request);

        assertNotNull(result.get("batch_id"));
        assertEquals(2, result.get("total_items"));
        List<?> evaluations = (List<?>) result.get("evaluations");
        assertEquals(2, evaluations.size());
    }

    @Test
    void shouldThrowException_whenItemsIsEmpty() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("items", Collections.emptyList());

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.batchEvaluateForScenario(request));
    }

    @Test
    void shouldThrowException_whenItemsIsNotCollection() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("items", "not_a_list");

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.batchEvaluateForScenario(request));
    }

    @Test
    void shouldThrowException_whenItemMissingPatientContext() {
        Map<String, Object> item = new LinkedHashMap<>();
        // no patient_context

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("items", Collections.singletonList(item));

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.batchEvaluateForScenario(request));
    }

    @Test
    void shouldThrowException_whenItemIsNotMap() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("items", Collections.singletonList("not_a_map"));

        assertThrows(IllegalArgumentException.class,
                () -> ruleService.batchEvaluateForScenario(request));
    }

    // ==================== getEvaluation ====================

    @Test
    void shouldGetEvaluation_byResultId() {
        // First create an evaluation via evaluateForScenario
        Map<String, Object> ruleMap = buildRuleMap("R001", "Pathway Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", patientContext);

        Map<String, Object> evalResult = ruleService.evaluateForScenario(request);
        String resultId = String.valueOf(evalResult.get("result_id"));

        Map<String, Object> retrieved = ruleService.getEvaluation(resultId);
        assertEquals(resultId, retrieved.get("result_id"));
    }

    @Test
    void shouldThrowException_whenResultIdIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> ruleService.getEvaluation(null));
    }

    @Test
    void shouldThrowException_whenResultIdIsEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> ruleService.getEvaluation(""));
    }

    @Test
    void shouldThrowException_whenEvaluationNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> ruleService.getEvaluation("non_existent_id"));
    }

    // ==================== listEvaluations ====================

    @Test
    void shouldListEvaluations_whenEvaluationsExist() {
        createSampleEvaluation();

        List<Map<String, Object>> evaluations = ruleService.listEvaluations(null);
        assertFalse(evaluations.isEmpty());
    }

    @Test
    void shouldFilterEvaluations_byScenarioCode() {
        createSampleEvaluation();

        Map<String, String> filters = new HashMap<>();
        filters.put("scenarioCode", "PATHWAY_ENTRY");

        List<Map<String, Object>> evaluations = ruleService.listEvaluations(filters);
        assertFalse(evaluations.isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenFilterDoesNotMatch() {
        createSampleEvaluation();

        Map<String, String> filters = new HashMap<>();
        filters.put("scenarioCode", "NON_EXISTENT");

        List<Map<String, Object>> evaluations = ruleService.listEvaluations(filters);
        assertTrue(evaluations.isEmpty());
    }

    @Test
    void shouldFilterEvaluations_byTenantId() {
        createSampleEvaluation();

        Map<String, String> filters = new HashMap<>();
        filters.put("tenantId", "default");

        List<Map<String, Object>> evaluations = ruleService.listEvaluations(filters);
        assertFalse(evaluations.isEmpty());
    }

    @Test
    void shouldRespectLimitAndOffset() {
        createSampleEvaluation();

        Map<String, String> filters = new HashMap<>();
        filters.put("limit", "1");
        filters.put("offset", "0");

        List<Map<String, Object>> evaluations = ruleService.listEvaluations(filters);
        assertTrue(evaluations.size() <= 1);
    }

    @Test
    void shouldReturnEmptyList_whenNoEvaluations() {
        List<Map<String, Object>> evaluations = ruleService.listEvaluations(null);
        assertTrue(evaluations.isEmpty());
    }

    // ==================== simulate ====================

    @Test
    void shouldSimulate_withInlineRule() {
        Map<String, Object> inlineRule = new LinkedHashMap<>();
        inlineRule.put("rule_code", "R_SIM");
        inlineRule.put("rule_name", "Sim Rule");
        inlineRule.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));

        Map<String, Object> patientContext = new LinkedHashMap<>();
        Map<String, Object> patient = new LinkedHashMap<>();
        patient.put("age", "65");
        patientContext.put("patient", patient);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);
        request.put("rule", inlineRule);

        RuleResult result = ruleService.simulate(request);

        assertNotNull(result);
        assertEquals("R_SIM", result.getRuleCode());
    }

    @Test
    void shouldSimulate_withExistingRuleCode() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Test Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);
        request.put("rule_code", "R001");

        RuleResult result = ruleService.simulate(request);
        assertNotNull(result);
        assertEquals("R001", result.getRuleCode());
    }

    @Test
    void shouldSimulate_withBuiltInRules_whenNoRuleCodeAndNoPublishedRules() {
        Map<String, Object> patientContext = buildStemiPatientContext();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        RuleResult result = ruleService.simulate(request);
        assertNotNull(result);
        assertEquals("R_AMI_STEMI_CANDIDATE", result.getRuleCode());
        assertTrue(result.isHit());
    }

    @Test
    void shouldSimulate_withOrgContext() {
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");
        Map<String, Object> ruleMap = buildRuleMap("R001", "Test Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap), orgContext);

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest, orgContext);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);
        request.put("rule_code", "R001");

        RuleResult result = ruleService.simulate(request, orgContext);
        assertNotNull(result);
    }

    // ==================== listExecLogs / summarizeExecLogs / getExecLog ====================

    @Test
    void shouldDelegateListExecLogs() {
        Map<String, String> filters = new HashMap<>();
        when(ruleExecutionLogService.listExecLogs(filters)).thenReturn(Collections.emptyList());

        List<RuleExecLogEntry> logs = ruleService.listExecLogs(filters);
        assertTrue(logs.isEmpty());
        verify(ruleExecutionLogService).listExecLogs(filters);
    }

    @Test
    void shouldDelegateSummarizeExecLogs() {
        Map<String, String> filters = new HashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", 0);
        when(ruleExecutionLogService.summarizeExecLogs(filters)).thenReturn(summary);

        Map<String, Object> result = ruleService.summarizeExecLogs(filters);
        assertEquals(0, result.get("total"));
        verify(ruleExecutionLogService).summarizeExecLogs(filters);
    }

    @Test
    void shouldDelegateGetExecLog() {
        RuleExecLogEntry entry = new RuleExecLogEntry();
        entry.setLogId("rxl-1");
        when(ruleExecutionLogService.getExecLog("rxl-1")).thenReturn(entry);

        RuleExecLogEntry result = ruleService.getExecLog("rxl-1");
        assertEquals("rxl-1", result.getLogId());
        verify(ruleExecutionLogService).getExecLog("rxl-1");
    }

    // ==================== Organization context integration ====================

    @Test
    void shouldApplyDepartmentScope_whenRuleHasDepartmentCode() {
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");
        Map<String, Object> ruleMap = buildRuleMap("R001", "Dept Rule", "GENERAL");
        ruleMap.put("department_code", "DEPT001");

        List<RuleDefinition> imported = ruleService.importRules(Collections.singletonList(ruleMap), orgContext);

        assertEquals("DEPARTMENT", imported.get(0).getScopeLevel());
        assertEquals("DEPT001", imported.get(0).getScopeCode());
    }

    @Test
    void shouldApplyHospitalScope_whenNoFinerGrainedCode() {
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");

        Map<String, Object> ruleMap = buildRuleMap("R001", "Hosp Rule", "GENERAL");
        List<RuleDefinition> imported = ruleService.importRules(Collections.singletonList(ruleMap), orgContext);

        assertEquals("HOSPITAL", imported.get(0).getScopeLevel());
        assertEquals("HOSP001", imported.get(0).getScopeCode());
    }

    @Test
    void shouldApplyDefaultOrg_whenNoOrgContext() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Default Rule", "GENERAL");

        List<RuleDefinition> imported = ruleService.importRules(Collections.singletonList(ruleMap));

        assertEquals("default", imported.get(0).getTenantId());
        assertEquals("DEFAULT_HOSPITAL", imported.get(0).getHospitalCode());
        assertEquals("HOSPITAL", imported.get(0).getScopeLevel());
    }

    @Test
    void shouldUseRuleJsonOrg_whenNoOrgContext() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Body Rule", "GENERAL");
        ruleMap.put("tenant_id", "T_FROM_BODY");
        ruleMap.put("hospital_code", "HOSP_FROM_BODY");

        List<RuleDefinition> imported = ruleService.importRules(Collections.singletonList(ruleMap));

        assertEquals("T_FROM_BODY", imported.get(0).getTenantId());
        assertEquals("HOSP_FROM_BODY", imported.get(0).getHospitalCode());
        assertEquals("BODY", imported.get(0).getOrgSource());
    }

    @Test
    void shouldUseLegacyOrgCode_whenNoHospitalCode() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Legacy Rule", "GENERAL");
        ruleMap.put("org_code", "LEGACY_ORG");

        List<RuleDefinition> imported = ruleService.importRules(Collections.singletonList(ruleMap));

        assertEquals("LEGACY_ORG", imported.get(0).getHospitalCode());
        assertEquals("LEGACY_ORG", imported.get(0).getLegacyOrgCode());
    }

    @Test
    void shouldApplyCampusScope_whenCampusCodePresent() {
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");
        Map<String, Object> ruleMap = buildRuleMap("R001", "Campus Rule", "GENERAL");
        ruleMap.put("campus_code", "CAMPUS001");

        List<RuleDefinition> imported = ruleService.importRules(Collections.singletonList(ruleMap), orgContext);

        assertEquals("CAMPUS", imported.get(0).getScopeLevel());
        assertEquals("CAMPUS001", imported.get(0).getScopeCode());
    }

    @Test
    void shouldApplySiteScope_whenSiteCodePresent() {
        OrganizationContext orgContext = buildOrgContext("T001", "HOSP001");
        Map<String, Object> ruleMap = buildRuleMap("R001", "Site Rule", "GENERAL");
        ruleMap.put("site_code", "SITE001");

        List<RuleDefinition> imported = ruleService.importRules(Collections.singletonList(ruleMap), orgContext);

        assertEquals("SITE", imported.get(0).getScopeLevel());
        assertEquals("SITE001", imported.get(0).getScopeCode());
    }

    // ==================== DSL evaluation scenarios ====================

    @Test
    void shouldHitRule_whenConditionMatches() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Age Rule", "GENERAL");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(request);
        assertEquals(1, results.size());
        assertTrue(results.get(0).isHit());
    }

    @Test
    void shouldNotHitRule_whenConditionDoesNotMatch() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Age Rule", "GENERAL");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "30"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(request);
        assertEquals(1, results.size());
        assertFalse(results.get(0).isHit());
    }

    @Test
    void shouldHandleExistsOperator() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Exists Rule", "GENERAL");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.diagnosis", "operator", "exists"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("diagnosis", "AMI"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(request);
        assertTrue(results.get(0).isHit());
    }

    @Test
    void shouldHandleInOperator() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "In Rule", "GENERAL");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.blood_type", "operator", "in",
                        "value", List.of("A", "B", "O")))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("blood_type", "A"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(request);
        assertTrue(results.get(0).isHit());
    }

    @Test
    void shouldHandleAnyOperator() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Any Rule", "GENERAL");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("condition", Map.of("any", List.of(
                Map.of("fact", "patient.allergy_a", "operator", "equals", "value", "true"),
                Map.of("fact", "patient.allergy_b", "operator", "equals", "value", "true"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("allergy_b", "true"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(request);
        assertTrue(results.get(0).isHit());
    }

    // ==================== Built-in STEMI rule ====================

    @Test
    void shouldHitStemiRule_whenChestPainAndStElevation() {
        Map<String, Object> patientContext = buildStemiPatientContext();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(request);

        RuleResult stemiResult = results.stream()
                .filter(r -> "R_AMI_STEMI_CANDIDATE".equals(r.getRuleCode()))
                .findFirst().orElse(null);

        assertNotNull(stemiResult);
        assertTrue(stemiResult.isHit());
        assertEquals("HIGH", stemiResult.getSeverity());
        assertFalse(stemiResult.getActions().isEmpty());
    }

    @Test
    void shouldNotHitStemiRule_whenOnlyChestPain() {
        Map<String, Object> patientContext = new LinkedHashMap<>();
        Map<String, Object> facts = new LinkedHashMap<>();
        List<Map<String, Object>> complaints = new ArrayList<>();
        Map<String, Object> complaint = new LinkedHashMap<>();
        complaint.put("code", "CHEST_PAIN");
        complaints.add(complaint);
        facts.put("chief_complaints", complaints);
        patientContext.put("facts", facts);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(request);

        RuleResult stemiResult = results.stream()
                .filter(r -> "R_AMI_STEMI_CANDIDATE".equals(r.getRuleCode()))
                .findFirst().orElse(null);

        assertNotNull(stemiResult);
        assertFalse(stemiResult.isHit());
    }

    // ==================== Scenario candidate matching ====================

    @Test
    void shouldMatchScenarioByRuleType_whenNoScenarioCodesDeclared() {
        // PATHWAY_ENTRY rule type maps to PATHWAY_ENTRY scenario by default
        Map<String, Object> ruleMap = buildRuleMap("R001", "Pathway Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        // no scenario_codes declared - should use default mapping
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", patientContext);

        Map<String, Object> result = ruleService.evaluateForScenario(request);
        List<?> results = (List<?>) result.get("results");
        assertEquals(1, results.size());
    }

    @Test
    void shouldFilterByPackageCode_inScenarioEvaluation() {
        Map<String, Object> rule1 = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        rule1.put("reference_document_code", "DOC001");
        rule1.put("package_code", "PKG001");
        rule1.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        rule1.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));

        Map<String, Object> rule2 = buildRuleMap("R002", "Rule 2", "PATHWAY_ENTRY");
        rule2.put("reference_document_code", "DOC002");
        rule2.put("package_code", "PKG002");
        rule2.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        rule2.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "70"))));

        ruleService.importRules(Arrays.asList(rule1, rule2));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);
        ruleService.publish("R002", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("rule_package_code", "PKG001");
        request.put("patient_context", patientContext);

        Map<String, Object> result = ruleService.evaluateForScenario(request);
        List<?> results = (List<?>) result.get("results");
        assertEquals(1, results.size());
    }

    @Test
    void shouldFilterByRuleCodes_inScenarioEvaluation() {
        Map<String, Object> rule1 = buildRuleMap("R001", "Rule 1", "PATHWAY_ENTRY");
        rule1.put("reference_document_code", "DOC001");
        rule1.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        rule1.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));

        Map<String, Object> rule2 = buildRuleMap("R002", "Rule 2", "PATHWAY_ENTRY");
        rule2.put("reference_document_code", "DOC002");
        rule2.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        rule2.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "70"))));

        ruleService.importRules(Arrays.asList(rule1, rule2));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);
        ruleService.publish("R002", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("rule_codes", List.of("R001"));
        request.put("patient_context", patientContext);

        Map<String, Object> result = ruleService.evaluateForScenario(request);
        List<?> results = (List<?>) result.get("results");
        assertEquals(1, results.size());
    }

    // ==================== Rule priority sorting ====================

    @Test
    void shouldSortPublishedRulesByPriority() {
        Map<String, Object> ruleLow = buildRuleMap("R_LOW", "Low Priority", "GENERAL");
        ruleLow.put("reference_document_code", "DOC001");
        ruleLow.put("priority", 1);
        ruleLow.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));

        Map<String, Object> ruleHigh = buildRuleMap("R_HIGH", "High Priority", "GENERAL");
        ruleHigh.put("reference_document_code", "DOC002");
        ruleHigh.put("priority", 10);
        ruleHigh.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));

        ruleService.importRules(Arrays.asList(ruleLow, ruleHigh));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R_LOW", publishRequest);
        ruleService.publish("R_HIGH", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(request);
        assertEquals("R_HIGH", results.get(0).getRuleCode());
        assertEquals("R_LOW", results.get(1).getRuleCode());
    }

    // ==================== Rule severity ====================

    @Test
    void shouldReturnCriticalSeverity_whenSafetyBlockType() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Safety Rule", "SAFETY_BLOCK");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("severity", null);
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        List<RuleResult> results = ruleService.evaluate(request);
        assertEquals("CRITICAL", results.get(0).getSeverity());
    }

    // ==================== Disabled rules ====================

    @Test
    void shouldNotEvaluateDisabledRules() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Disabled Rule", "GENERAL");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("enabled", false);
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_context", patientContext);

        // Disabled rule should not be evaluated, falls back to built-in
        List<RuleResult> results = ruleService.evaluate(request);
        assertTrue(results.stream().noneMatch(r -> "R001".equals(r.getRuleCode())));
    }

    // ==================== Evaluation ring buffer ====================

    @Test
    void shouldPersistEvaluationToResultRepository() {
        when(ruleEvalResultRepository.enabled()).thenReturn(true);

        Map<String, Object> ruleMap = buildRuleMap("R001", "Pathway Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", patientContext);

        ruleService.evaluateForScenario(request);

        verify(ruleEvalResultRepository).save(any(RuleEvalResultEntity.class));
    }

    @Test
    void shouldNotPersist_whenResultRepositoryNotEnabled() {
        when(ruleEvalResultRepository.enabled()).thenReturn(false);

        Map<String, Object> ruleMap = buildRuleMap("R001", "Pathway Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", patientContext);

        ruleService.evaluateForScenario(request);

        verify(ruleEvalResultRepository, never()).save(any(RuleEvalResultEntity.class));
    }

    // ==================== Review package details ====================

    @Test
    void shouldReportReadyToPublish_whenNoIssues() {
        Map<String, Object> rule = buildRuleMap("R001", "Good Rule", "PATHWAY_ENTRY");
        rule.put("package_code", "PKG001");
        rule.put("package_version", "1.0.0");
        rule.put("reference_document_code", "DOC001");
        rule.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(rule));

        Map<String, Object> review = ruleService.reviewPackage("PKG001", "1.0.0");

        assertTrue((Boolean) review.get("ready_to_publish"));
        List<?> issues = (List<?>) review.get("issues");
        assertTrue(issues.isEmpty());
    }

    @Test
    void shouldReportNotReady_whenMissingReferenceDocument() {
        Map<String, Object> rule = buildRuleMap("R001", "No Ref Rule", "PATHWAY_ENTRY");
        rule.put("package_code", "PKG001");
        rule.put("package_version", "1.0.0");
        // no reference_document_code
        ruleService.importRules(Collections.singletonList(rule));

        Map<String, Object> review = ruleService.reviewPackage("PKG001", "1.0.0");

        assertFalse((Boolean) review.get("ready_to_publish"));
        List<?> issues = (List<?>) review.get("issues");
        assertFalse(issues.isEmpty());
    }

    @Test
    void shouldReportNotReady_whenDslConditionInvalid() {
        Map<String, Object> rule = buildRuleMap("R001", "Bad DSL Rule", "PATHWAY_ENTRY");
        rule.put("package_code", "PKG001");
        rule.put("package_version", "1.0.0");
        rule.put("reference_document_code", "DOC001");
        rule.put("condition", "not_a_map");

        ruleService.importRules(Collections.singletonList(rule));

        Map<String, Object> review = ruleService.reviewPackage("PKG001", "1.0.0");

        assertFalse((Boolean) review.get("ready_to_publish"));
    }

    @Test
    void shouldReportEnabledAndDisabledCounts() {
        Map<String, Object> rule1 = buildRuleMap("R001", "Enabled Rule", "GENERAL");
        rule1.put("package_code", "PKG001");
        rule1.put("package_version", "1.0.0");
        rule1.put("enabled", true);

        Map<String, Object> rule2 = buildRuleMap("R002", "Disabled Rule", "GENERAL");
        rule2.put("package_code", "PKG001");
        rule2.put("package_version", "1.0.0");
        rule2.put("enabled", false);

        ruleService.importRules(Arrays.asList(rule1, rule2));

        Map<String, Object> review = ruleService.reviewPackage("PKG001", "1.0.0");

        assertEquals(1, review.get("enabled_rules"));
        assertEquals(1, review.get("disabled_rules"));
    }

    @Test
    void shouldReportMixedVersion() {
        Map<String, Object> rule1 = buildRuleMap("R001", "Rule 1", "GENERAL");
        rule1.put("package_code", "PKG001");
        rule1.put("package_version", "1.0.0");

        Map<String, Object> rule2 = buildRuleMap("R002", "Rule 2", "GENERAL");
        rule2.put("package_code", "PKG001");
        rule2.put("package_version", "2.0.0");

        ruleService.importRules(Arrays.asList(rule1, rule2));

        Map<String, Object> review = ruleService.reviewPackage("PKG001", null);

        assertEquals("MIXED", review.get("package_version"));
    }

    // ==================== Supported scenario codes ====================

    @Test
    void shouldSupportAllStandardScenarioCodes() {
        String[] scenarios = {"PATHWAY_ENTRY", "EMR_QC", "INSURANCE_QC", "ORDER_SAFETY",
                "DRUG_INDICATION", "EXAM_RATIONALITY"};

        for (String scenario : scenarios) {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("scenario_code", scenario);
            request.put("patient_context", Map.of("patient", Map.of("age", "65")));

            // Should not throw unsupported scenario_code exception
            assertDoesNotThrow(() -> ruleService.evaluateForScenario(request));
        }
    }

    // ==================== Evaluation result fields ====================

    @Test
    void shouldIncludeAllRequiredFields_inScenarioEvaluationResult() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Pathway Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", patientContext);

        Map<String, Object> result = ruleService.evaluateForScenario(request);

        assertTrue(result.containsKey("result_id"));
        assertTrue(result.containsKey("trace_id"));
        assertTrue(result.containsKey("scenario_code"));
        assertTrue(result.containsKey("evaluated_count"));
        assertTrue(result.containsKey("hit_count"));
        assertTrue(result.containsKey("elapsed_ms"));
        assertTrue(result.containsKey("results"));
        assertTrue(result.containsKey("warnings"));
        assertTrue(result.containsKey("created_time"));
    }

    // ==================== Helper methods ====================

    private Map<String, Object> buildRuleMap(String ruleCode, String ruleName, String ruleType) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", ruleCode);
        rule.put("rule_name", ruleName);
        rule.put("rule_type", ruleType);
        rule.put("version_no", "1.0.0");
        rule.put("severity", "HIGH");
        rule.put("enabled", true);
        rule.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        return rule;
    }

    private OrganizationContext buildOrgContext(String tenantId, String hospitalCode) {
        OrganizationContext ctx = new OrganizationContext();
        ctx.setTenantId(tenantId);
        ctx.setHospitalCode(hospitalCode);
        ctx.setEffectiveScopeLevel("HOSPITAL");
        ctx.setEffectiveScopeCode(hospitalCode);
        ctx.setSource("HEADER");
        return ctx;
    }

    private Map<String, Object> buildStemiPatientContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        Map<String, Object> facts = new LinkedHashMap<>();

        List<Map<String, Object>> complaints = new ArrayList<>();
        Map<String, Object> complaint = new LinkedHashMap<>();
        complaint.put("code", "CHEST_PAIN");
        complaints.add(complaint);
        facts.put("chief_complaints", complaints);

        List<Map<String, Object>> exams = new ArrayList<>();
        Map<String, Object> exam = new LinkedHashMap<>();
        exam.put("finding_codes", List.of("ST_ELEVATION_CONTIGUOUS_LEADS"));
        exams.add(exam);
        facts.put("exams", exams);

        context.put("facts", facts);
        return context;
    }

    private void importRulesInPackage(String packageCode, String packageVersion, int count) {
        for (int i = 0; i < count; i++) {
            Map<String, Object> rule = buildRuleMap("R00" + (i + 1), "Rule " + (i + 1), "GENERAL");
            rule.put("package_code", packageCode);
            rule.put("package_version", packageVersion);
            ruleService.importRules(Collections.singletonList(rule));
        }
    }

    private void createSampleEvaluation() {
        Map<String, Object> ruleMap = buildRuleMap("R001", "Pathway Rule", "PATHWAY_ENTRY");
        ruleMap.put("reference_document_code", "DOC001");
        ruleMap.put("scenario_codes", List.of("PATHWAY_ENTRY"));
        ruleMap.put("condition", Map.of("all", List.of(
                Map.of("fact", "patient.age", "operator", "equals", "value", "65"))));
        ruleService.importRules(Collections.singletonList(ruleMap));

        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        publishRequest.put("approved_by", "admin");
        ruleService.publish("R001", publishRequest);

        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("patient", Map.of("age", "65"));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scenario_code", "PATHWAY_ENTRY");
        request.put("patient_context", patientContext);

        ruleService.evaluateForScenario(request);
    }
}
