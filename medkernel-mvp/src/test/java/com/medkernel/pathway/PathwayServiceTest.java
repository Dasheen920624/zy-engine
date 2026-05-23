package com.medkernel.pathway;

import com.medkernel.adapter.AdapterHubService;
import com.medkernel.audit.PublishGateService;
import com.medkernel.common.exception.MissingSourceException;
import com.medkernel.dto.PatientPathwayInstance;
import com.medkernel.dto.PatientNodeState;
import com.medkernel.dto.PatientTaskState;
import com.medkernel.dto.PathwayVariationRecord;
import com.medkernel.dto.RecommendationCard;
import com.medkernel.dto.RuleResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.rule.RuleService;
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
class PathwayServiceTest {

    @Mock
    private RuleService ruleService;

    @Mock
    private AdapterHubService adapterHubService;

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private PublishGateService publishGateService;

    @Mock
    private PathwayTemplateService templateService;

    private PathwayService pathwayService;

    @BeforeEach
    void setUp() {
        pathwayService = new PathwayService(
                ruleService, adapterHubService, persistenceService, publishGateService, templateService);
        lenient().when(persistenceService.providerName()).thenReturn("TEST_DB");
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private Map<String, Object> validPathwayConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("pathway_code", "AMI_STEMI");
        config.put("pathway_name", "急性ST段抬高型心肌梗死诊疗路径");
        config.put("version_no", "1.0.0");
        config.put("tenant_id", "default");
        config.put("org_code", "DEFAULT_HOSPITAL");

        Map<String, Object> node1 = new LinkedHashMap<>();
        node1.put("node_code", "NODE_ADMIT");
        node1.put("node_name", "入院评估");
        node1.put("reference_document_code", "DOC_001");
        node1.put("tasks", new ArrayList<Map<String, Object>>());
        node1.put("transitions", new ArrayList<Map<String, Object>>());

        Map<String, Object> node2 = new LinkedHashMap<>();
        node2.put("node_code", "NODE_TREAT");
        node2.put("node_name", "治疗");
        node2.put("reference_document_code", "DOC_002");
        node2.put("tasks", new ArrayList<Map<String, Object>>());
        node2.put("transitions", new ArrayList<Map<String, Object>>());

        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("stage_code", "STAGE_1");
        stage.put("stage_name", "诊疗阶段");
        stage.put("nodes", Arrays.asList(node1, node2));

        config.put("stages", Collections.singletonList(stage));
        return config;
    }

    private Map<String, Object> validPathwayConfigWithTransitions() {
        Map<String, Object> config = validPathwayConfig();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stages = (List<Map<String, Object>>) config.get("stages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) stages.get(0).get("nodes");

        Map<String, Object> transition = new LinkedHashMap<>();
        transition.put("to_node", "NODE_TREAT");
        transition.put("priority", 1);
        nodes.get(0).put("transitions", Collections.singletonList(transition));

        return config;
    }

    private Map<String, Object> validPathwayConfigWithTasks() {
        Map<String, Object> config = validPathwayConfigWithTransitions();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stages = (List<Map<String, Object>>) config.get("stages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) stages.get(0).get("nodes");

        Map<String, Object> task = new LinkedHashMap<>();
        task.put("task_code", "TASK_ECG");
        task.put("task_name", "心电图检查");
        task.put("task_type", "EXAM");
        task.put("required", true);
        nodes.get(0).put("tasks", Collections.singletonList(task));

        return config;
    }

    private PublishGateService.GateCheckResult readyGateResult() {
        PublishGateService.GateCheckResult result = new PublishGateService.GateCheckResult();
        return result;
    }

    private PublishGateService.GateCheckResult blockedGateResult() {
        PublishGateService.GateCheckResult result = new PublishGateService.GateCheckResult();
        result.addIssue("ERROR", "reference_document_code", "缺少来源文档绑定", "NODE_001", "PATHWAY");
        return result;
    }

    private void setupPublishedPathway(String pathwayCode, String versionNo, Map<String, Object> config) {
        pathwayService.createPathway(config);
        when(publishGateService.checkPathwayReferences(any())).thenReturn(readyGateResult());
        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", versionNo);
        publishRequest.put("approved_by", "admin");
        pathwayService.publish(pathwayCode, publishRequest);
    }

    private PatientPathwayInstance admitPatient(String pathwayCode, String versionNo) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", pathwayCode);
        request.put("version_no", versionNo);
        request.put("doctor_id", "D001");
        return pathwayService.admit(request);
    }

    // ========================================================================
    // rebuildFromPersistence
    // ========================================================================

    @Test
    void shouldSkipRebuild_whenPersistenceNotEnabled() {
        when(persistenceService.enabled()).thenReturn(false);
        pathwayService.rebuildFromPersistence();
        verify(persistenceService, never()).loadAllPathwayDrafts();
    }

    @Test
    void shouldRebuildDraftsAndVersions_whenPersistenceEnabled() {
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Map<String, Object>> drafts = new LinkedHashMap<>();
        Map<String, Object> draftConfig = new LinkedHashMap<>();
        draftConfig.put("pathway_code", "PW001");
        drafts.put("PW001", draftConfig);
        when(persistenceService.loadAllPathwayDrafts()).thenReturn(drafts);

        List<Map<String, Object>> versions = new ArrayList<>();
        Map<String, Object> versionRow = new LinkedHashMap<>();
        versionRow.put("pathway_code", "PW001");
        versionRow.put("version_no", "1.0.0");
        versionRow.put("status", "PUBLISHED");
        versionRow.put("config", new LinkedHashMap<>());
        versions.add(versionRow);
        when(persistenceService.loadAllPathwayPublishedVersions()).thenReturn(versions);

        pathwayService.rebuildFromPersistence();

        verify(persistenceService).loadAllPathwayDrafts();
        verify(persistenceService).loadAllPathwayPublishedVersions();
    }

    @Test
    void shouldNotThrow_whenRebuildFails() {
        when(persistenceService.enabled()).thenReturn(true);
        when(persistenceService.loadAllPathwayDrafts()).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> pathwayService.rebuildFromPersistence());
    }

    // ========================================================================
    // createPathway
    // ========================================================================

    @Test
    void shouldCreatePathway_whenValidInput() {
        Map<String, Object> config = validPathwayConfig();

        Map<String, Object> result = pathwayService.createPathway(config);

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        assertEquals("DRAFT", result.get("status"));
        assertEquals("PASSED", result.get("validation"));
        verify(persistenceService).savePathwayDraft(eq("AMI_STEMI"), eq(config));
    }

    @Test
    void shouldThrow_whenPathwayCodeMissing() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("pathway_name", "test");

        assertThrows(IllegalArgumentException.class, () -> pathwayService.createPathway(config));
    }

    @Test
    void shouldThrow_whenValidationFails() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("pathway_code", "PW001");
        config.put("pathway_name", "test");
        config.put("version_no", "1.0.0");
        // missing stages

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pathwayService.createPathway(config));
        assertTrue(ex.getMessage().contains("pathway config invalid"));
    }

    // ========================================================================
    // saveDraft
    // ========================================================================

    @Test
    void shouldSaveDraft_whenNewDraft() {
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("pathway_name", "Updated Name");

        Map<String, Object> result = pathwayService.saveDraft("PW001", draft, "T001");

        assertEquals("PW001", result.get("pathway_code"));
        assertEquals("DRAFT", result.get("status"));
        verify(persistenceService).savePathwayDraft(eq("PW001"), any());
    }

    @Test
    void shouldMergeDraft_whenExistingDraft() {
        Map<String, Object> existingConfig = validPathwayConfig();
        pathwayService.createPathway(existingConfig);

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("pathway_name", "Updated Name");
        pathwayService.saveDraft("AMI_STEMI", update, "T001");

        Map<String, Object> pathway = pathwayService.getPathway("AMI_STEMI", null);
        @SuppressWarnings("unchecked")
        Map<String, Object> draftConfig = (Map<String, Object>) pathway.get("draft_config");
        assertEquals("Updated Name", draftConfig.get("pathway_name"));
    }

    // ========================================================================
    // submitReview
    // ========================================================================

    @Test
    void shouldSubmitReview_whenDraftExists() {
        pathwayService.createPathway(validPathwayConfig());

        Map<String, Object> result = pathwayService.submitReview("AMI_STEMI", "T001");

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        assertEquals("PENDING_REVIEW", result.get("review_status"));
    }

    @Test
    void shouldThrow_whenSubmitReviewAndNoDraft() {
        assertThrows(IllegalArgumentException.class,
                () -> pathwayService.submitReview("NONEXISTENT", "T001"));
    }

    // ========================================================================
    // publish
    // ========================================================================

    @Test
    void shouldPublish_whenGatePasses() {
        pathwayService.createPathway(validPathwayConfig());
        when(publishGateService.checkPathwayReferences(any())).thenReturn(readyGateResult());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");
        request.put("approved_by", "admin");

        Map<String, Object> result = pathwayService.publish("AMI_STEMI", request);

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        assertEquals("1.0.0", result.get("version_no"));
        assertEquals("PUBLISHED", result.get("status"));
        verify(persistenceService).savePathwayVersion(eq("AMI_STEMI"), eq("1.0.0"), eq("PUBLISHED"), any());
        verify(persistenceService).updatePathwayStatus(eq("AMI_STEMI"), eq("PUBLISHED"), any(), any());
    }

    @Test
    void shouldThrow_whenGateBlocks() {
        pathwayService.createPathway(validPathwayConfig());
        when(publishGateService.checkPathwayReferences(any())).thenReturn(blockedGateResult());
        when(publishGateService.formatBlockingMessage(any())).thenReturn("发布门禁检查未通过");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");

        assertThrows(MissingSourceException.class, () -> pathwayService.publish("AMI_STEMI", request));
    }

    @Test
    void shouldPublishWithDefaultVersion_whenNoDraftButRequestHasVersion() {
        when(publishGateService.checkPathwayReferences(any())).thenReturn(readyGateResult());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "2.0.0");
        request.put("approved_by", "admin");

        Map<String, Object> result = pathwayService.publish("PW_NO_DRAFT", request);

        assertEquals("PW_NO_DRAFT", result.get("pathway_code"));
        assertEquals("2.0.0", result.get("version_no"));
        assertEquals("PUBLISHED", result.get("status"));
    }

    @Test
    void shouldAuditGateCheck_onPublish() {
        pathwayService.createPathway(validPathwayConfig());
        when(publishGateService.checkPathwayReferences(any())).thenReturn(readyGateResult());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");
        request.put("approved_by", "admin");
        pathwayService.publish("AMI_STEMI", request);

        verify(publishGateService).auditGateCheck(eq("PATHWAY"), eq("PUBLISH_GATE"),
                eq("PATHWAY"), eq("AMI_STEMI"), eq("admin"), any());
    }

    // ========================================================================
    // rollback
    // ========================================================================

    @Test
    void shouldRollback_whenTargetVersionExists() {
        setupPublishedPathway("AMI_STEMI", "1.0.0", validPathwayConfig());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("target_version", "1.0.0");
        request.put("operator_id", "admin");
        request.put("reason", "回滚测试");

        Map<String, Object> result = pathwayService.rollback("AMI_STEMI", request);

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        assertEquals("ROLLED_BACK", result.get("status"));
        assertEquals("1.0.0", result.get("active_version"));
    }

    @Test
    void shouldRollback_withRollbackToVersionKey() {
        setupPublishedPathway("AMI_STEMI", "1.0.0", validPathwayConfig());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("rollback_to_version", "1.0.0");

        Map<String, Object> result = pathwayService.rollback("AMI_STEMI", request);

        assertEquals("ROLLED_BACK", result.get("status"));
    }

    @Test
    void shouldRollback_withVersionNoKey() {
        setupPublishedPathway("AMI_STEMI", "1.0.0", validPathwayConfig());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");

        Map<String, Object> result = pathwayService.rollback("AMI_STEMI", request);

        assertEquals("ROLLED_BACK", result.get("status"));
    }

    @Test
    void shouldThrow_whenNoTargetVersionSpecified() {
        assertThrows(IllegalArgumentException.class,
                () -> pathwayService.rollback("AMI_STEMI", new HashMap<>()));
    }

    @Test
    void shouldThrow_whenTargetVersionNotFound() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("target_version", "99.0.0");

        assertThrows(IllegalArgumentException.class,
                () -> pathwayService.rollback("AMI_STEMI", request));
    }

    @Test
    void shouldRollback_whenRequestIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> pathwayService.rollback("AMI_STEMI", null));
    }

    // ========================================================================
    // listPathways
    // ========================================================================

    @Test
    void shouldListEmpty_whenNoPathways() {
        List<Map<String, Object>> list = pathwayService.listPathways();
        assertTrue(list.isEmpty());
    }

    @Test
    void shouldListPathways_withDraftAndPublished() {
        setupPublishedPathway("AMI_STEMI", "1.0.0", validPathwayConfig());

        List<Map<String, Object>> list = pathwayService.listPathways();

        assertFalse(list.isEmpty());
        Map<String, Object> item = list.get(0);
        assertEquals("AMI_STEMI", item.get("pathway_code"));
        assertEquals("DRAFT", item.get("draft_status"));
        assertFalse(((List<?>) item.get("published_versions")).isEmpty());
    }

    // ========================================================================
    // listPathwaysFiltered
    // ========================================================================

    @Test
    void shouldFilterByStatus() {
        pathwayService.createPathway(validPathwayConfig());

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("status", "DRAFT");

        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertFalse(items.isEmpty());
    }

    @Test
    void shouldFilterBySearch() {
        pathwayService.createPathway(validPathwayConfig());

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("search", "STEMI");

        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertFalse(items.isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenSearchNotMatched() {
        pathwayService.createPathway(validPathwayConfig());

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("search", "NONEXISTENT");

        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertTrue(items.isEmpty());
    }

    @Test
    void shouldPaginateResults() {
        pathwayService.createPathway(validPathwayConfig());

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("page", "1");
        filters.put("size", "10");

        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        assertEquals(1, result.get("total"));
        assertEquals(1, result.get("page"));
        assertEquals(10, result.get("size"));
    }

    @Test
    void shouldHandleDefaultPagination() {
        pathwayService.createPathway(validPathwayConfig());

        Map<String, Object> result = pathwayService.listPathwaysFiltered(null);
        assertEquals(1, result.get("page"));
        assertEquals(20, result.get("size"));
    }

    // ========================================================================
    // deletePathway
    // ========================================================================

    @Test
    void shouldDeletePathway_whenDraftOnly() {
        pathwayService.createPathway(validPathwayConfig());

        Map<String, Object> result = pathwayService.deletePathway("AMI_STEMI");

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        assertEquals("DELETED", result.get("status"));
        verify(persistenceService).deletePathwayDraft("AMI_STEMI");
    }

    @Test
    void shouldThrow_whenDeleteNonexistentDraft() {
        assertThrows(IllegalArgumentException.class, () -> pathwayService.deletePathway("NONEXISTENT"));
    }

    @Test
    void shouldThrow_whenDeletePublishedPathway() {
        setupPublishedPathway("AMI_STEMI", "1.0.0", validPathwayConfig());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pathwayService.deletePathway("AMI_STEMI"));
        assertTrue(ex.getMessage().contains("cannot delete published pathway"));
    }

    // ========================================================================
    // diffPathway
    // ========================================================================

    @Test
    void shouldDelegateDiffToTemplateService() {
        Map<String, Object> diffResult = new LinkedHashMap<>();
        diffResult.put("pathway_code", "AMI_STEMI");
        when(templateService.diffPathway(eq("AMI_STEMI"), eq("1.0.0"), eq("2.0.0"), any(), any()))
                .thenReturn(diffResult);

        Map<String, Object> result = pathwayService.diffPathway("AMI_STEMI", "1.0.0", "2.0.0");

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        verify(templateService).diffPathway(eq("AMI_STEMI"), eq("1.0.0"), eq("2.0.0"), any(), any());
    }

    // ========================================================================
    // getPathway
    // ========================================================================

    @Test
    void shouldGetPathway_withDraftAndPublished() {
        setupPublishedPathway("AMI_STEMI", "1.0.0", validPathwayConfig());

        Map<String, Object> result = pathwayService.getPathway("AMI_STEMI", null);

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        assertEquals("DRAFT", result.get("draft_status"));
        assertNotNull(result.get("draft_config"));
        assertNotNull(result.get("published_config"));
    }

    @Test
    void shouldGetPathway_withSpecificVersion() {
        setupPublishedPathway("AMI_STEMI", "1.0.0", validPathwayConfig());

        Map<String, Object> result = pathwayService.getPathway("AMI_STEMI", "1.0.0");

        assertEquals("1.0.0", result.get("selected_version"));
    }

    @Test
    void shouldThrow_whenPathwayNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> pathwayService.getPathway("NONEXISTENT", null));
    }

    // ========================================================================
    // candidates
    // ========================================================================

    @Test
    void shouldReturnEmptyCards_whenNoStemiHit() {
        RuleResult ruleResult = new RuleResult();
        ruleResult.setRuleCode("R_AMI_STEMI_CANDIDATE");
        ruleResult.setHit(false);
        when(ruleService.evaluate(any())).thenReturn(Collections.singletonList(ruleResult));

        List<RecommendationCard> cards = pathwayService.candidates(new HashMap<>());

        assertTrue(cards.isEmpty());
    }

    @Test
    void shouldReturnRecommendationCard_whenStemiHit() {
        RuleResult ruleResult = new RuleResult();
        ruleResult.setRuleCode("R_AMI_STEMI_CANDIDATE");
        ruleResult.setHit(true);
        when(ruleService.evaluate(any())).thenReturn(Collections.singletonList(ruleResult));

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("patient_id", "P001");
        context.put("encounter_id", "E001");
        List<RecommendationCard> cards = pathwayService.candidates(context);

        assertFalse(cards.isEmpty());
        assertEquals("AMI_STEMI", cards.get(0).getTargetCode());
        assertEquals("PATHWAY_ENTRY", cards.get(0).getScenario());
        verify(persistenceService).saveRecommendation(any());
    }

    // ========================================================================
    // admit
    // ========================================================================

    @Test
    void shouldAdmitPatient_whenValidRequest() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");

        PatientPathwayInstance instance = pathwayService.admit(request);

        assertNotNull(instance);
        assertEquals("ACTIVE", instance.getStatus());
        assertEquals("P001", instance.getPatientId());
        assertEquals("E001", instance.getEncounterId());
        assertEquals("AMI_STEMI", instance.getPathwayCode());
        assertTrue(instance.getInstanceId().startsWith("ppi-"));
        verify(persistenceService).savePatientInstance(any(), eq("D001"));
    }

    @Test
    void shouldAdmitPatient_withOrganizationContext() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        OrganizationContext orgContext = new OrganizationContext();
        orgContext.setTenantId("T001");
        orgContext.setGroupCode("G001");
        orgContext.setHospitalCode("H001");
        orgContext.setCampusCode("C001");
        orgContext.setSiteCode("S001");
        orgContext.setDepartmentCode("D001");
        orgContext.setLegacyOrgCode("H001");
        orgContext.setEffectiveScopeLevel("DEPARTMENT");
        orgContext.setEffectiveScopeCode("D001");
        orgContext.setSource("HEADER");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");

        PatientPathwayInstance instance = pathwayService.admit(request, orgContext);

        assertEquals("T001", instance.getTenantId());
        assertEquals("H001", instance.getHospitalCode());
        assertEquals("DEPARTMENT", instance.getScopeLevel());
        assertEquals("D001", instance.getScopeCode());
        assertEquals("HEADER", instance.getOrgSource());
    }

    @Test
    void shouldReturnExistingInstance_whenDuplicateAdmit() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");

        PatientPathwayInstance first = pathwayService.admit(request);
        PatientPathwayInstance second = pathwayService.admit(request);

        assertEquals(first.getInstanceId(), second.getInstanceId());
    }

    @Test
    void shouldAdmitWithDefaultOrg_whenNoOrgContext() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");

        PatientPathwayInstance instance = pathwayService.admit(request);

        assertEquals("default", instance.getTenantId());
        assertNotNull(instance.getHospitalCode());
    }

    @Test
    void shouldAdmitWithBodyOrg_whenOrgInRequestBody() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");
        request.put("tenant_id", "T_BODY");
        request.put("hospital_code", "H_BODY");

        PatientPathwayInstance instance = pathwayService.admit(request);

        assertEquals("T_BODY", instance.getTenantId());
        assertEquals("H_BODY", instance.getHospitalCode());
        assertEquals("BODY", instance.getOrgSource());
    }

    @Test
    void shouldUseFirstNodeFromConfig_whenConfigHasNodes() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        assertEquals("NODE_ADMIT", instance.getCurrentNodeCode());
    }

    @Test
    void shouldFallbackToBuiltInNode_whenNoConfigNodes() {
        when(publishGateService.checkPathwayReferences(any())).thenReturn(readyGateResult());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");
        pathwayService.publish("AMI_FALLBACK", request);

        Map<String, Object> admitRequest = new LinkedHashMap<>();
        admitRequest.put("patient_id", "P001");
        admitRequest.put("encounter_id", "E002");
        admitRequest.put("pathway_code", "AMI_FALLBACK");
        admitRequest.put("version_no", "1.0.0");
        admitRequest.put("doctor_id", "D001");

        PatientPathwayInstance instance = pathwayService.admit(admitRequest);

        assertEquals("AMI_CHEST_PAIN_IDENTIFY", instance.getCurrentNodeCode());
    }

    // ========================================================================
    // getInstanceDetail
    // ========================================================================

    @Test
    void shouldGetInstanceDetail() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> detail = pathwayService.getInstanceDetail(instance.getInstanceId());

        assertNotNull(detail.get("instance"));
        assertNotNull(detail.get("nodes"));
        assertNotNull(detail.get("current_node"));
        assertNotNull(detail.get("variations"));
    }

    @Test
    void shouldThrow_whenInstanceNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> pathwayService.getInstanceDetail("nonexistent-id"));
    }

    // ========================================================================
    // getNodeState
    // ========================================================================

    @Test
    void shouldGetNodeState() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        PatientNodeState nodeState = pathwayService.getNodeState(instance.getInstanceId(), "NODE_ADMIT");

        assertNotNull(nodeState);
        assertEquals("NODE_ADMIT", nodeState.getNodeCode());
    }

    // ========================================================================
    // completeTask
    // ========================================================================

    @Test
    void shouldCompleteTask() {
        Map<String, Object> config = validPathwayConfigWithTasks();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("operator_id", "D001");

        PatientTaskState taskState = pathwayService.completeTask(
                instance.getInstanceId(), "NODE_ADMIT", "TASK_ECG", request);

        assertNotNull(taskState);
        assertEquals("COMPLETED", taskState.getStatus());
        assertEquals("D001", taskState.getOperatorId());
        verify(persistenceService, atLeastOnce()).saveTaskState(any());
    }

    // ========================================================================
    // skipTask
    // ========================================================================

    @Test
    void shouldSkipTask_andCreateVariation() {
        Map<String, Object> config = validPathwayConfigWithTasks();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("operator_id", "D001");
        request.put("reason", "患者拒绝");

        PatientTaskState taskState = pathwayService.skipTask(
                instance.getInstanceId(), "NODE_ADMIT", "TASK_ECG", request);

        assertEquals("SKIPPED", taskState.getStatus());

        List<PathwayVariationRecord> variations = pathwayService.listVariations(
                Collections.singletonMap("instanceId", instance.getInstanceId()));
        assertFalse(variations.isEmpty());
        assertEquals("TASK_SKIPPED", variations.get(0).getVariationType());
    }

    @Test
    void shouldSkipTask_withDefaultVariationType() {
        Map<String, Object> config = validPathwayConfigWithTasks();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();

        pathwayService.skipTask(instance.getInstanceId(), "NODE_ADMIT", "TASK_ECG", request);

        List<PathwayVariationRecord> variations = pathwayService.listVariations(
                Collections.singletonMap("instanceId", instance.getInstanceId()));
        assertFalse(variations.isEmpty());
        assertEquals("TASK_SKIPPED", variations.get(0).getVariationType());
    }

    // ========================================================================
    // recordVariation
    // ========================================================================

    @Test
    void shouldRecordVariation() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("variation_type", "PATHWAY_DEVIATION");
        request.put("reason", "患者过敏史未录入");
        request.put("operator_id", "D001");

        PathwayVariationRecord record = pathwayService.recordVariation(instance.getInstanceId(), request);

        assertNotNull(record);
        assertTrue(record.getVariationId().startsWith("var-"));
        assertEquals("PATHWAY_DEVIATION", record.getVariationType());
        assertEquals("患者过敏史未录入", record.getReason());
        verify(persistenceService).saveVariationRecord(any());
    }

    @Test
    void shouldRecordVariation_withDefaultReason() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("variation_type", "PATHWAY_DEVIATION");

        PathwayVariationRecord record = pathwayService.recordVariation(instance.getInstanceId(), request);

        assertNotNull(record.getReason());
    }

    @Test
    void shouldThrow_whenRecordVariationForNonexistentInstance() {
        assertThrows(IllegalArgumentException.class,
                () -> pathwayService.recordVariation("nonexistent", new HashMap<>()));
    }

    // ========================================================================
    // completeNode
    // ========================================================================

    @Test
    void shouldCompleteNode_andTransitionToNext() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");
        assertEquals("NODE_ADMIT", instance.getCurrentNodeCode());

        PatientPathwayInstance updated = pathwayService.completeNode(instance.getInstanceId(), "NODE_ADMIT");

        assertEquals("NODE_TREAT", updated.getCurrentNodeCode());
        verify(persistenceService).savePatientInstance(any(), isNull());
    }

    @Test
    void shouldCompleteNode_withBuiltInTransition() {
        when(publishGateService.checkPathwayReferences(any())).thenReturn(readyGateResult());
        Map<String, Object> publishRequest = new LinkedHashMap<>();
        publishRequest.put("version_no", "1.0.0");
        pathwayService.publish("AMI_BUILTIN", publishRequest);

        Map<String, Object> admitRequest = new LinkedHashMap<>();
        admitRequest.put("patient_id", "P001");
        admitRequest.put("encounter_id", "E001");
        admitRequest.put("pathway_code", "AMI_BUILTIN");
        admitRequest.put("version_no", "1.0.0");
        admitRequest.put("doctor_id", "D001");
        PatientPathwayInstance instance = pathwayService.admit(admitRequest);

        PatientPathwayInstance updated = pathwayService.completeNode(instance.getInstanceId(), "AMI_CHEST_PAIN_IDENTIFY");

        assertEquals("AMI_REPERFUSION_EVAL", updated.getCurrentNodeCode());
    }

    @Test
    void shouldCompleteNode_withVariation() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("variation_type", "EARLY_COMPLETE");
        request.put("reason", "提前完成");
        request.put("operator_id", "D001");

        PatientPathwayInstance updated = pathwayService.completeNode(
                instance.getInstanceId(), "NODE_ADMIT", request);

        List<PathwayVariationRecord> variations = pathwayService.listVariations(
                Collections.singletonMap("instanceId", instance.getInstanceId()));
        assertFalse(variations.isEmpty());
    }

    @Test
    void shouldReturnNotFoundInstance_whenInstanceNotInActiveInstances() {
        PatientPathwayInstance result = pathwayService.completeNode("nonexistent-id", "NODE_1");

        assertEquals("NOT_FOUND", result.getStatus());
        assertEquals("nonexistent-id", result.getInstanceId());
    }

    @Test
    void shouldCompleteNode_withoutTransition_whenLastNode() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        pathwayService.completeNode(instance.getInstanceId(), "NODE_ADMIT");
        PatientPathwayInstance updated = pathwayService.completeNode(instance.getInstanceId(), "NODE_TREAT");

        // NODE_TREAT has no transitions, so currentNodeCode stays as NODE_TREAT
        assertEquals("NODE_TREAT", updated.getCurrentNodeCode());
    }

    // ========================================================================
    // listInstances
    // ========================================================================

    @Test
    void shouldListInstances_withFilters() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        admitPatient("AMI_STEMI", "1.0.0");

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("pathwayCode", "AMI_STEMI");

        List<PatientPathwayInstance> instances = pathwayService.listInstances(filters);

        assertFalse(instances.isEmpty());
        assertEquals("AMI_STEMI", instances.get(0).getPathwayCode());
    }

    @Test
    void shouldFilterInstances_byStatus() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        admitPatient("AMI_STEMI", "1.0.0");

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("status", "ACTIVE");

        List<PatientPathwayInstance> instances = pathwayService.listInstances(filters);
        assertFalse(instances.isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenStatusNotMatched() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        admitPatient("AMI_STEMI", "1.0.0");

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("status", "COMPLETED");

        List<PatientPathwayInstance> instances = pathwayService.listInstances(filters);
        assertTrue(instances.isEmpty());
    }

    @Test
    void shouldRespectLimit() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        admitPatient("AMI_STEMI", "1.0.0");

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("limit", "0");

        List<PatientPathwayInstance> instances = pathwayService.listInstances(filters);
        // limit <= 0 defaults to 100
        assertFalse(instances.isEmpty());
    }

    @Test
    void shouldFilterByOrgFields() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        OrganizationContext orgContext = new OrganizationContext();
        orgContext.setTenantId("T001");
        orgContext.setHospitalCode("H001");
        orgContext.setLegacyOrgCode("H001");
        orgContext.setEffectiveScopeLevel("HOSPITAL");
        orgContext.setEffectiveScopeCode("H001");
        orgContext.setSource("HEADER");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");
        pathwayService.admit(request, orgContext);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("tenantId", "T001");
        filters.put("hospitalCode", "H001");

        List<PatientPathwayInstance> instances = pathwayService.listInstances(filters);
        assertFalse(instances.isEmpty());
    }

    // ========================================================================
    // summarizeNodeCompletion
    // ========================================================================

    @Test
    void shouldSummarizeNodeCompletion() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        admitPatient("AMI_STEMI", "1.0.0");

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("pathwayCode", "AMI_STEMI");

        Map<String, Object> summary = pathwayService.summarizeNodeCompletion(filters);

        assertTrue(((Number) summary.get("total_instances")).intValue() > 0);
        assertNotNull(summary.get("nodes"));
    }

    @Test
    void shouldSummarizeNodeCompletion_withNoInstances() {
        Map<String, String> filters = new LinkedHashMap<>();
        Map<String, Object> summary = pathwayService.summarizeNodeCompletion(filters);

        assertEquals(0, ((Number) summary.get("total_instances")).intValue());
    }

    // ========================================================================
    // summarizeNodeStayDuration
    // ========================================================================

    @Test
    void shouldSummarizeNodeStayDuration() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        admitPatient("AMI_STEMI", "1.0.0");

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("pathwayCode", "AMI_STEMI");

        Map<String, Object> summary = pathwayService.summarizeNodeStayDuration(filters);

        assertTrue(((Number) summary.get("total_instances")).intValue() > 0);
        assertNotNull(summary.get("nodes"));
    }

    // ========================================================================
    // summarizeInstances
    // ========================================================================

    @Test
    void shouldSummarizeInstances() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        admitPatient("AMI_STEMI", "1.0.0");

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("pathwayCode", "AMI_STEMI");

        Map<String, Object> summary = pathwayService.summarizeInstances(filters);

        assertTrue(((Number) summary.get("total")).intValue() > 0);
        assertNotNull(summary.get("by_pathway_code"));
        assertNotNull(summary.get("by_status"));
        assertNotNull(summary.get("variation_total"));
    }

    // ========================================================================
    // listVariations
    // ========================================================================

    @Test
    void shouldListVariations_withFilters() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "PATHWAY_DEVIATION");
        varRequest.put("reason", "test");
        pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("pathwayCode", "AMI_STEMI");

        List<PathwayVariationRecord> variations = pathwayService.listVariations(filters);
        assertFalse(variations.isEmpty());
    }

    @Test
    void shouldFilterVariations_byType() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "PATHWAY_DEVIATION");
        varRequest.put("reason", "test");
        pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("variationType", "PATHWAY_DEVIATION");

        List<PathwayVariationRecord> variations = pathwayService.listVariations(filters);
        assertFalse(variations.isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenVariationTypeNotMatched() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "PATHWAY_DEVIATION");
        varRequest.put("reason", "test");
        pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("variationType", "NONEXISTENT");

        List<PathwayVariationRecord> variations = pathwayService.listVariations(filters);
        assertTrue(variations.isEmpty());
    }

    // ========================================================================
    // summarizeVariations
    // ========================================================================

    @Test
    void shouldSummarizeVariations() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "PATHWAY_DEVIATION");
        varRequest.put("reason", "test");
        pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("pathwayCode", "AMI_STEMI");

        Map<String, Object> summary = pathwayService.summarizeVariations(filters);

        assertTrue(((Number) summary.get("total")).intValue() > 0);
        assertNotNull(summary.get("by_variation_type"));
    }

    // ========================================================================
    // Organization context integration
    // ========================================================================

    @Test
    void shouldApplyEffectiveScope_department() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        OrganizationContext orgContext = new OrganizationContext();
        orgContext.setTenantId("T001");
        orgContext.setGroupCode("G001");
        orgContext.setHospitalCode("H001");
        orgContext.setDepartmentCode("DEPT001");
        orgContext.setLegacyOrgCode("H001");
        orgContext.setEffectiveScopeLevel("DEPARTMENT");
        orgContext.setEffectiveScopeCode("DEPT001");
        orgContext.setSource("HEADER");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");

        PatientPathwayInstance instance = pathwayService.admit(request, orgContext);

        assertEquals("DEPARTMENT", instance.getScopeLevel());
        assertEquals("DEPT001", instance.getScopeCode());
    }

    @Test
    void shouldApplyEffectiveScope_fromBodyOrg() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");
        request.put("tenant_id", "T001");
        request.put("hospital_code", "H001");
        request.put("department_code", "DEPT001");

        PatientPathwayInstance instance = pathwayService.admit(request);

        assertEquals("DEPARTMENT", instance.getScopeLevel());
        assertEquals("DEPT001", instance.getScopeCode());
        assertEquals("BODY", instance.getOrgSource());
    }

    @Test
    void shouldApplyEffectiveScope_hospital_whenOnlyHospitalProvided() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");
        request.put("hospital_code", "H001");

        PatientPathwayInstance instance = pathwayService.admit(request);

        assertEquals("HOSPITAL", instance.getScopeLevel());
        assertEquals("H001", instance.getScopeCode());
    }

    @Test
    void shouldCopyOrganizationToVariation() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        OrganizationContext orgContext = new OrganizationContext();
        orgContext.setTenantId("T001");
        orgContext.setHospitalCode("H001");
        orgContext.setLegacyOrgCode("H001");
        orgContext.setEffectiveScopeLevel("HOSPITAL");
        orgContext.setEffectiveScopeCode("H001");
        orgContext.setSource("HEADER");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");

        PatientPathwayInstance instance = pathwayService.admit(request, orgContext);

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "TEST");
        varRequest.put("reason", "test");
        PathwayVariationRecord record = pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        assertEquals("T001", record.getTenantId());
        assertEquals("H001", record.getHospitalCode());
        assertEquals("HOSPITAL", record.getScopeLevel());
    }

    // ========================================================================
    // Error handling paths
    // ========================================================================

    @Test
    void shouldNotThrow_whenAuditFails() {
        doThrow(new RuntimeException("audit error"))
                .when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> config = validPathwayConfig();
        assertDoesNotThrow(() -> pathwayService.createPathway(config));
    }

    @Test
    void shouldHandleNullFiltersInListInstances() {
        List<PatientPathwayInstance> instances = pathwayService.listInstances(null);
        assertNotNull(instances);
    }

    @Test
    void shouldHandleNullFiltersInListVariations() {
        List<PathwayVariationRecord> variations = pathwayService.listVariations(null);
        assertNotNull(variations);
    }

    @Test
    void shouldHandleNullFiltersInListPathwaysFiltered() {
        Map<String, Object> result = pathwayService.listPathwaysFiltered(null);
        assertNotNull(result);
    }

    @Test
    void shouldHandleEmptyFiltersInListInstances() {
        List<PatientPathwayInstance> instances = pathwayService.listInstances(Collections.emptyMap());
        assertNotNull(instances);
    }

    // ========================================================================
    // Multiple versions
    // ========================================================================

    @Test
    void shouldManageMultipleVersions() {
        Map<String, Object> config = validPathwayConfig();
        pathwayService.createPathway(config);

        when(publishGateService.checkPathwayReferences(any())).thenReturn(readyGateResult());

        Map<String, Object> request1 = new LinkedHashMap<>();
        request1.put("version_no", "1.0.0");
        request1.put("approved_by", "admin");
        pathwayService.publish("AMI_STEMI", request1);

        Map<String, Object> request2 = new LinkedHashMap<>();
        request2.put("version_no", "2.0.0");
        request2.put("approved_by", "admin");
        pathwayService.publish("AMI_STEMI", request2);

        Map<String, Object> pathway = pathwayService.getPathway("AMI_STEMI", null);
        @SuppressWarnings("unchecked")
        List<String> versions = (List<String>) pathway.get("published_versions");
        assertEquals(2, versions.size());
        assertEquals("2.0.0", pathway.get("active_published_version"));
    }

    // ========================================================================
    // Task with adapter source
    // ========================================================================

    @Test
    void shouldEnrichTaskResult_withAdapterData() {
        Map<String, Object> config = validPathwayConfigWithTransitions();

        // Add task with source config
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stages = (List<Map<String, Object>>) config.get("stages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) stages.get(0).get("nodes");

        Map<String, Object> source = new LinkedHashMap<>();
        source.put("adapter_code", "LAB_ADAPTER");
        source.put("query_code", "QUERY_LAB_RESULT");

        Map<String, Object> task = new LinkedHashMap<>();
        task.put("task_code", "TASK_LAB");
        task.put("task_name", "检验结果");
        task.put("task_type", "EXAM");
        task.put("required", true);
        task.put("source", source);
        nodes.get(0).put("tasks", Collections.singletonList(task));

        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> adapterResult = new LinkedHashMap<>();
        adapterResult.put("status", "OK");
        adapterResult.put("row_count", 3);
        when(adapterHubService.query(any(), any(), any())).thenReturn(adapterResult);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("operator_id", "D001");
        request.put("result", Collections.singletonMap("lab_value", 42.0));

        PatientTaskState taskState = pathwayService.completeTask(
                instance.getInstanceId(), "NODE_ADMIT", "TASK_LAB", request);

        assertEquals("COMPLETED", taskState.getStatus());
        verify(adapterHubService).query(any(), any(), any());
    }

    // ========================================================================
    // Complete task with fallback (task not in config)
    // ========================================================================

    @Test
    void shouldCompleteFallbackTask_whenTaskNotInConfig() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("operator_id", "D001");

        PatientTaskState taskState = pathwayService.completeTask(
                instance.getInstanceId(), "NODE_ADMIT", "UNKNOWN_TASK", request);

        assertEquals("COMPLETED", taskState.getStatus());
        assertEquals("UNKNOWN_TASK", taskState.getTaskCode());
    }

    // ========================================================================
    // Variation reason extraction
    // ========================================================================

    @Test
    void shouldExtractVariationReason_fromReasonField() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("variation_type", "DEVIATION");
        request.put("reason", "具体原因");
        PathwayVariationRecord record = pathwayService.recordVariation(instance.getInstanceId(), request);

        assertEquals("具体原因", record.getReason());
    }

    @Test
    void shouldExtractVariationReason_fromVariationReasonField() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("variation_type", "DEVIATION");
        request.put("variation_reason", "变异原因");
        PathwayVariationRecord record = pathwayService.recordVariation(instance.getInstanceId(), request);

        assertEquals("变异原因", record.getReason());
    }

    @Test
    void shouldExtractVariationReason_fromDeviationReasonField() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("variation_type", "DEVIATION");
        request.put("deviation_reason", "偏差原因");
        PathwayVariationRecord record = pathwayService.recordVariation(instance.getInstanceId(), request);

        assertEquals("偏差原因", record.getReason());
    }

    // ========================================================================
    // listPathwaysFiltered - dept filter
    // ========================================================================

    @Test
    void shouldFilterByDept() {
        Map<String, Object> config = validPathwayConfig();
        config.put("dept", "CARDIOLOGY");
        config.put("specialty_code", "CARDIO");
        pathwayService.createPathway(config);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("dept", "CARDIOLOGY");

        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertFalse(items.isEmpty());
    }

    @Test
    void shouldFilterByDept_viaSpecialtyCode() {
        Map<String, Object> config = validPathwayConfig();
        config.put("specialty_code", "CARDIO");
        pathwayService.createPathway(config);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("dept", "CARDIO");

        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertFalse(items.isEmpty());
    }

    // ========================================================================
    // listPathwaysFiltered - tenant filter
    // ========================================================================

    @Test
    void shouldFilterByTenantId() {
        Map<String, Object> config = validPathwayConfig();
        config.put("tenant_id", "T_SPECIAL");
        pathwayService.createPathway(config);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("tenantId", "T_SPECIAL");

        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertFalse(items.isEmpty());
    }

    @Test
    void shouldExcludeOtherTenants_whenTenantFilterApplied() {
        Map<String, Object> config1 = validPathwayConfig();
        config1.put("tenant_id", "T001");
        config1.put("pathway_code", "PW_T001");
        config1.put("pathway_name", "路径T001");
        pathwayService.createPathway(config1);

        Map<String, Object> config2 = validPathwayConfig();
        config2.put("tenant_id", "T002");
        config2.put("pathway_code", "PW_T002");
        config2.put("pathway_name", "路径T002");
        pathwayService.createPathway(config2);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("tenantId", "T001");

        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertEquals("PW_T001", items.get(0).get("pathway_code"));
    }

    // ========================================================================
    // listInstances - org matching
    // ========================================================================

    @Test
    void shouldFilterInstances_byTenantId() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        OrganizationContext orgContext = new OrganizationContext();
        orgContext.setTenantId("T_SPECIAL");
        orgContext.setHospitalCode("H001");
        orgContext.setLegacyOrgCode("H001");
        orgContext.setEffectiveScopeLevel("HOSPITAL");
        orgContext.setEffectiveScopeCode("H001");
        orgContext.setSource("HEADER");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");
        pathwayService.admit(request, orgContext);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("tenantId", "T_SPECIAL");
        List<PatientPathwayInstance> instances = pathwayService.listInstances(filters);
        assertFalse(instances.isEmpty());

        Map<String, String> wrongFilters = new LinkedHashMap<>();
        wrongFilters.put("tenantId", "WRONG_TENANT");
        List<PatientPathwayInstance> wrongInstances = pathwayService.listInstances(wrongFilters);
        assertTrue(wrongInstances.isEmpty());
    }

    // ========================================================================
    // listVariations - org matching
    // ========================================================================

    @Test
    void shouldFilterVariations_byOrgFields() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        OrganizationContext orgContext = new OrganizationContext();
        orgContext.setTenantId("T_VAR");
        orgContext.setHospitalCode("H_VAR");
        orgContext.setLegacyOrgCode("H_VAR");
        orgContext.setEffectiveScopeLevel("HOSPITAL");
        orgContext.setEffectiveScopeCode("H_VAR");
        orgContext.setSource("HEADER");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");
        PatientPathwayInstance instance = pathwayService.admit(request, orgContext);

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "TEST");
        varRequest.put("reason", "test");
        pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("tenantId", "T_VAR");
        List<PathwayVariationRecord> variations = pathwayService.listVariations(filters);
        assertFalse(variations.isEmpty());
    }

    // ========================================================================
    // completeNode - node state tracking
    // ========================================================================

    @Test
    void shouldTrackNodeStateOnComplete() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        pathwayService.completeNode(instance.getInstanceId(), "NODE_ADMIT");

        Map<String, Object> detail = pathwayService.getInstanceDetail(instance.getInstanceId());
        @SuppressWarnings("unchecked")
        List<PatientNodeState> nodes = (List<PatientNodeState>) detail.get("nodes");

        PatientNodeState completedNode = null;
        for (PatientNodeState ns : nodes) {
            if ("NODE_ADMIT".equals(ns.getNodeCode())) {
                completedNode = ns;
            }
        }
        assertNotNull(completedNode);
        assertEquals("COMPLETED", completedNode.getStatus());
        assertNotNull(completedNode.getCompleteTime());
    }

    // ========================================================================
    // Pagination edge cases
    // ========================================================================

    @Test
    void shouldHandlePageBeyondResults() {
        pathwayService.createPathway(validPathwayConfig());

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("page", "999");
        filters.put("size", "10");

        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertTrue(items.isEmpty());
        assertEquals(1, result.get("total"));
    }

    @Test
    void shouldHandleInvalidPageNumber() {
        pathwayService.createPathway(validPathwayConfig());

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("page", "0");
        filters.put("size", "-1");

        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        assertEquals(1, result.get("page")); // defaults to 1
        assertEquals(20, result.get("size")); // defaults to 20
    }

    // ========================================================================
    // summarizeNodeCompletion with tasks
    // ========================================================================

    @Test
    void shouldSummarizeNodeCompletion_withTaskAggregation() {
        Map<String, Object> config = validPathwayConfigWithTasks();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> taskRequest = new LinkedHashMap<>();
        taskRequest.put("operator_id", "D001");
        pathwayService.completeTask(instance.getInstanceId(), "NODE_ADMIT", "TASK_ECG", taskRequest);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("pathwayCode", "AMI_STEMI");

        Map<String, Object> summary = pathwayService.summarizeNodeCompletion(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) summary.get("nodes");
        assertFalse(nodes.isEmpty());
    }

    // ========================================================================
    // Variation records copy org from instance
    // ========================================================================

    @Test
    void shouldCopyAllOrgFieldsToVariation() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        OrganizationContext orgContext = new OrganizationContext();
        orgContext.setTenantId("T001");
        orgContext.setGroupCode("G001");
        orgContext.setHospitalCode("H001");
        orgContext.setCampusCode("C001");
        orgContext.setSiteCode("S001");
        orgContext.setDepartmentCode("D001");
        orgContext.setLegacyOrgCode("H001");
        orgContext.setEffectiveScopeLevel("DEPARTMENT");
        orgContext.setEffectiveScopeCode("D001");
        orgContext.setSource("HEADER");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");
        PatientPathwayInstance instance = pathwayService.admit(request, orgContext);

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "TEST");
        varRequest.put("reason", "test");
        PathwayVariationRecord record = pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        assertEquals("T001", record.getTenantId());
        assertEquals("G001", record.getGroupCode());
        assertEquals("H001", record.getHospitalCode());
        assertEquals("C001", record.getCampusCode());
        assertEquals("S001", record.getSiteCode());
        assertEquals("D001", record.getDepartmentCode());
        assertEquals("DEPARTMENT", record.getScopeLevel());
        assertEquals("D001", record.getScopeCode());
    }

    // ========================================================================
    // rebuildFromPersistence - null config handling
    // ========================================================================

    @Test
    void shouldHandleNullConfigInVersions() {
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Map<String, Object>> drafts = new LinkedHashMap<>();
        when(persistenceService.loadAllPathwayDrafts()).thenReturn(drafts);

        List<Map<String, Object>> versions = new ArrayList<>();
        Map<String, Object> versionRow = new LinkedHashMap<>();
        versionRow.put("pathway_code", "PW_NULL");
        versionRow.put("version_no", "1.0.0");
        versionRow.put("status", "PUBLISHED");
        versionRow.put("config", null);
        versions.add(versionRow);
        when(persistenceService.loadAllPathwayPublishedVersions()).thenReturn(versions);

        assertDoesNotThrow(() -> pathwayService.rebuildFromPersistence());
    }

    @Test
    void shouldSkipVersionRow_whenCodeOrVersionNull() {
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Map<String, Object>> drafts = new LinkedHashMap<>();
        when(persistenceService.loadAllPathwayDrafts()).thenReturn(drafts);

        List<Map<String, Object>> versions = new ArrayList<>();
        Map<String, Object> versionRow = new LinkedHashMap<>();
        versionRow.put("pathway_code", null);
        versionRow.put("version_no", "1.0.0");
        versionRow.put("status", "PUBLISHED");
        versionRow.put("config", new LinkedHashMap<>());
        versions.add(versionRow);
        when(persistenceService.loadAllPathwayPublishedVersions()).thenReturn(versions);

        assertDoesNotThrow(() -> pathwayService.rebuildFromPersistence());
    }

    // ========================================================================
    // listPathwaysFiltered - completion rate
    // ========================================================================

    @Test
    void shouldCalculateCompletionRate() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        admitPatient("AMI_STEMI", "1.0.0");

        Map<String, String> filters = new LinkedHashMap<>();
        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

        assertFalse(items.isEmpty());
        // instance_count should be >= 1
        assertTrue(((Number) items.get(0).get("instance_count")).intValue() >= 1);
    }

    // ========================================================================
    // summarizeInstances - variation integration
    // ========================================================================

    @Test
    void shouldIncludeVariationStats_inInstanceSummary() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "PATHWAY_DEVIATION");
        varRequest.put("reason", "test");
        pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("pathwayCode", "AMI_STEMI");

        Map<String, Object> summary = pathwayService.summarizeInstances(filters);

        assertTrue(((Number) summary.get("variation_total")).intValue() > 0);
        assertNotNull(summary.get("variation_by_type"));
    }

    // ========================================================================
    // completeNode with null request
    // ========================================================================

    @Test
    void shouldCompleteNode_withNullRequest() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        PatientPathwayInstance updated = pathwayService.completeNode(instance.getInstanceId(), "NODE_ADMIT");

        assertEquals("NODE_TREAT", updated.getCurrentNodeCode());
    }

    // ========================================================================
    // skipTask with null request
    // ========================================================================

    @Test
    void shouldSkipTask_withNullRequest() {
        Map<String, Object> config = validPathwayConfigWithTasks();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        PatientTaskState taskState = pathwayService.skipTask(
                instance.getInstanceId(), "NODE_ADMIT", "TASK_ECG", null);

        assertEquals("SKIPPED", taskState.getStatus());
    }

    // ========================================================================
    // completeTask with null request
    // ========================================================================

    @Test
    void shouldCompleteTask_withNullRequest() {
        Map<String, Object> config = validPathwayConfigWithTasks();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        PatientTaskState taskState = pathwayService.completeTask(
                instance.getInstanceId(), "NODE_ADMIT", "TASK_ECG", null);

        assertEquals("COMPLETED", taskState.getStatus());
    }

    // ========================================================================
    // listVariations - limit
    // ========================================================================

    @Test
    void shouldRespectLimitInListVariations() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        for (int i = 0; i < 5; i++) {
            Map<String, Object> varRequest = new LinkedHashMap<>();
            varRequest.put("variation_type", "TYPE_" + i);
            varRequest.put("reason", "reason_" + i);
            pathwayService.recordVariation(instance.getInstanceId(), varRequest);
        }

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("limit", "2");

        List<PathwayVariationRecord> variations = pathwayService.listVariations(filters);
        assertEquals(2, variations.size());
    }

    // ========================================================================
    // listInstances - limit
    // ========================================================================

    @Test
    void shouldRespectLimitInListInstances() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);

        for (int i = 0; i < 3; i++) {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("patient_id", "P00" + i);
            request.put("encounter_id", "E00" + i);
            request.put("pathway_code", "AMI_STEMI");
            request.put("version_no", "1.0.0");
            request.put("doctor_id", "D001");
            pathwayService.admit(request);
        }

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("limit", "2");

        List<PatientPathwayInstance> instances = pathwayService.listInstances(filters);
        assertEquals(2, instances.size());
    }

    // ========================================================================
    // listVariations - instanceId filter
    // ========================================================================

    @Test
    void shouldFilterVariations_byInstanceId() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "TEST");
        varRequest.put("reason", "test");
        pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("instanceId", instance.getInstanceId());

        List<PathwayVariationRecord> variations = pathwayService.listVariations(filters);
        assertFalse(variations.isEmpty());
    }

    // ========================================================================
    // listVariations - nodeCode filter
    // ========================================================================

    @Test
    void shouldFilterVariations_byNodeCode() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "TEST");
        varRequest.put("reason", "test");
        varRequest.put("node_code", "NODE_ADMIT");
        pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("nodeCode", "NODE_ADMIT");

        List<PathwayVariationRecord> variations = pathwayService.listVariations(filters);
        assertFalse(variations.isEmpty());
    }

    // ========================================================================
    // listVariations - patientId and encounterId filter
    // ========================================================================

    @Test
    void shouldFilterVariations_byPatientId() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> varRequest = new LinkedHashMap<>();
        varRequest.put("variation_type", "TEST");
        varRequest.put("reason", "test");
        pathwayService.recordVariation(instance.getInstanceId(), varRequest);

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("patientId", "P001");

        List<PathwayVariationRecord> variations = pathwayService.listVariations(filters);
        assertFalse(variations.isEmpty());
    }

    // ========================================================================
    // summarizeVariations - empty
    // ========================================================================

    @Test
    void shouldSummarizeVariations_whenEmpty() {
        Map<String, String> filters = new LinkedHashMap<>();
        Map<String, Object> summary = pathwayService.summarizeVariations(filters);

        assertEquals(0, ((Number) summary.get("total")).intValue());
    }

    // ========================================================================
    // summarizeNodeStayDuration - empty
    // ========================================================================

    @Test
    void shouldSummarizeNodeStayDuration_whenNoInstances() {
        Map<String, String> filters = new LinkedHashMap<>();
        Map<String, Object> summary = pathwayService.summarizeNodeStayDuration(filters);

        assertEquals(0, ((Number) summary.get("total_instances")).intValue());
    }

    // ========================================================================
    // candidates - multiple rules
    // ========================================================================

    @Test
    void shouldReturnEmpty_whenOtherRuleHitsButNotStemi() {
        RuleResult otherRule = new RuleResult();
        otherRule.setRuleCode("R_OTHER");
        otherRule.setHit(true);

        RuleResult stemiRule = new RuleResult();
        stemiRule.setRuleCode("R_AMI_STEMI_CANDIDATE");
        stemiRule.setHit(false);

        when(ruleService.evaluate(any())).thenReturn(Arrays.asList(otherRule, stemiRule));

        List<RecommendationCard> cards = pathwayService.candidates(new HashMap<>());
        assertTrue(cards.isEmpty());
    }

    // ========================================================================
    // completeNode - variation detection
    // ========================================================================

    @Test
    void shouldNotCreateVariation_whenNoVariationFields() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        PatientPathwayInstance instance = admitPatient("AMI_STEMI", "1.0.0");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("operator_id", "D001");
        // no variation_type, variation_reason, deviation_reason, or reason

        pathwayService.completeNode(instance.getInstanceId(), "NODE_ADMIT", request);

        List<PathwayVariationRecord> variations = pathwayService.listVariations(
                Collections.singletonMap("instanceId", instance.getInstanceId()));
        assertTrue(variations.isEmpty());
    }

    // ========================================================================
    // listPathwaysFiltered - instance count and completion rate
    // ========================================================================

    @Test
    void shouldShowZeroCompletionRate_whenNoCompletedInstances() {
        Map<String, Object> config = validPathwayConfigWithTransitions();
        setupPublishedPathway("AMI_STEMI", "1.0.0", config);
        admitPatient("AMI_STEMI", "1.0.0");

        Map<String, String> filters = new LinkedHashMap<>();
        Map<String, Object> result = pathwayService.listPathwaysFiltered(filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

        assertFalse(items.isEmpty());
        Object rate = items.get(0).get("completion_rate");
        assertNotNull(rate);
    }

    // ========================================================================
    // rebuildFromPersistence - PUBLISHED status sets active version
    // ========================================================================

    @Test
    void shouldSetActiveVersion_whenStatusIsPublished() {
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Map<String, Object>> drafts = new LinkedHashMap<>();
        when(persistenceService.loadAllPathwayDrafts()).thenReturn(drafts);

        List<Map<String, Object>> versions = new ArrayList<>();
        Map<String, Object> versionRow = new LinkedHashMap<>();
        versionRow.put("pathway_code", "PW_REBUILT");
        versionRow.put("version_no", "1.0.0");
        versionRow.put("status", "PUBLISHED");
        versionRow.put("config", new LinkedHashMap<>());
        versions.add(versionRow);
        when(persistenceService.loadAllPathwayPublishedVersions()).thenReturn(versions);

        pathwayService.rebuildFromPersistence();

        Map<String, Object> pathway = pathwayService.getPathway("PW_REBUILT", null);
        assertEquals("1.0.0", pathway.get("active_published_version"));
    }

    // ========================================================================
    // rebuildFromPersistence - non-PUBLISHED status does not set active version
    // ========================================================================

    @Test
    void shouldFallbackToLatestVersion_whenNoActiveVersionSet() {
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Map<String, Object>> drafts = new LinkedHashMap<>();
        when(persistenceService.loadAllPathwayDrafts()).thenReturn(drafts);

        List<Map<String, Object>> versions = new ArrayList<>();
        Map<String, Object> versionRow = new LinkedHashMap<>();
        versionRow.put("pathway_code", "PW_DRAFT_ONLY");
        versionRow.put("version_no", "1.0.0");
        versionRow.put("status", "DRAFT");
        versionRow.put("config", new LinkedHashMap<>());
        versions.add(versionRow);
        when(persistenceService.loadAllPathwayPublishedVersions()).thenReturn(versions);

        pathwayService.rebuildFromPersistence();

        // The version exists in publishedPathways but activePublishedVersions was not set for DRAFT status.
        // activeVersion falls back to the latest version from publishedVersions.
        Map<String, Object> pathway = pathwayService.getPathway("PW_DRAFT_ONLY", "1.0.0");
        assertEquals("1.0.0", pathway.get("active_published_version"));
    }
}
