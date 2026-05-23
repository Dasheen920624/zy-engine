package com.medkernel.pathway;

import com.medkernel.adapter.AdapterHubService;
import com.medkernel.audit.PublishGateService;
import com.medkernel.common.exception.MissingSourceException;
import com.medkernel.dto.PatientPathwayInstance;
import com.medkernel.dto.PathwayVariationRecord;
import com.medkernel.dto.RecommendationCard;
import com.medkernel.dto.RuleResult;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.rule.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PathwayService 单元测试")
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

    @InjectMocks
    private PathwayService pathwayService;

    private Map<String, Object> validConfig;

    @BeforeEach
    void setUp() {
        // 模拟持久化层未启用，使 rebuildFromPersistence 直接返回，不加载任何数据
        when(persistenceService.enabled()).thenReturn(false);
        when(persistenceService.providerName()).thenReturn("IN_MEMORY");

        // 手动调用 @PostConstruct 方法
        pathwayService.rebuildFromPersistence();

        // 构建一份合法的路径配置
        validConfig = new LinkedHashMap<>();
        validConfig.put("pathway_code", "AMI_STEMI");
        validConfig.put("pathway_name", "急性ST段抬高型心肌梗死诊疗路径");
        validConfig.put("version", "1.0.0");
        validConfig.put("stages", buildStages());
    }

    // =========================================================================
    // createPathway
    // =========================================================================

    @Test
    @DisplayName("createPathway - 创建新路径草稿成功")
    void createPathway_success() {
        Map<String, Object> result = pathwayService.createPathway(validConfig);

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        assertEquals("DRAFT", result.get("status"));
        assertEquals("PASSED", result.get("validation"));

        verify(persistenceService).savePathwayDraft("AMI_STEMI", validConfig);
        verify(persistenceService).saveAuditLog(eq("PATHWAY"), eq("CREATE_DRAFT"),
                eq("PATHWAY"), eq("AMI_STEMI"), isNull(), isNull(), isNull(), anyMap());
    }

    @Test
    @DisplayName("createPathway - 缺少 pathway_code 时抛出异常")
    void createPathway_missingPathwayCode_throwsException() {
        Map<String, Object> config = new LinkedHashMap<>(validConfig);
        config.remove("pathway_code");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pathwayService.createPathway(config));
        assertTrue(ex.getMessage().contains("pathway_code is required"));
    }

    @Test
    @DisplayName("createPathway - 配置校验失败时抛出异常")
    void createPathway_invalidConfig_throwsException() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("pathway_code", "BAD_PATHWAY");
        // 缺少 pathway_name / version / stages

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pathwayService.createPathway(config));
        assertTrue(ex.getMessage().contains("pathway config invalid"));
    }

    // =========================================================================
    // saveDraft
    // =========================================================================

    @Test
    @DisplayName("saveDraft - 保存草稿到已有路径成功")
    void saveDraft_existingPathway_success() {
        // 先创建一条路径
        pathwayService.createPathway(validConfig);

        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("pathway_name", "更新后的路径名称");

        Map<String, Object> result = pathwayService.saveDraft("AMI_STEMI", draft, "tenant-001");

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        assertEquals("DRAFT", result.get("status"));
        verify(persistenceService, atLeastOnce()).savePathwayDraft(eq("AMI_STEMI"), anyMap());
    }

    @Test
    @DisplayName("saveDraft - 保存草稿到新路径成功")
    void saveDraft_newPathway_success() {
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("pathway_name", "新路径");

        Map<String, Object> result = pathwayService.saveDraft("NEW_PW", draft, "tenant-001");

        assertEquals("NEW_PW", result.get("pathway_code"));
        assertEquals("DRAFT", result.get("status"));
        verify(persistenceService).savePathwayDraft(eq("NEW_PW"), anyMap());
    }

    // =========================================================================
    // publish
    // =========================================================================

    @Test
    @DisplayName("publish - 发布路径成功")
    void publish_success() {
        // 先创建草稿
        pathwayService.createPathway(validConfig);

        // 模拟发布门禁通过
        PublishGateService.GateCheckResult gateResult = mock(PublishGateService.GateCheckResult.class);
        when(gateResult.isReadyToPublish()).thenReturn(true);
        when(gateResult.toMapList()).thenReturn(Collections.emptyList());
        when(publishGateService.checkPathwayReferences(anyList())).thenReturn(gateResult);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");
        request.put("approved_by", "doctor-001");

        Map<String, Object> result = pathwayService.publish("AMI_STEMI", request);

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        assertEquals("1.0.0", result.get("version_no"));
        assertEquals("PUBLISHED", result.get("status"));

        verify(persistenceService).savePathwayVersion(eq("AMI_STEMI"), eq("1.0.0"),
                eq("PUBLISHED"), anyMap());
        verify(persistenceService).updatePathwayStatus(eq("AMI_STEMI"), eq("PUBLISHED"),
                any(), any());
    }

    @Test
    @DisplayName("publish - 发布门禁未通过时抛出 MissingSourceException")
    void publish_gateBlocked_throwsMissingSourceException() {
        pathwayService.createPathway(validConfig);

        PublishGateService.GateCheckResult gateResult = mock(PublishGateService.GateCheckResult.class);
        when(gateResult.isReadyToPublish()).thenReturn(false);
        when(gateResult.toMapList()).thenReturn(Collections.emptyList());
        when(publishGateService.checkPathwayReferences(anyList())).thenReturn(gateResult);
        when(publishGateService.formatBlockingMessage(gateResult)).thenReturn("发布门禁检查未通过");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version_no", "1.0.0");

        assertThrows(MissingSourceException.class,
                () -> pathwayService.publish("AMI_STEMI", request));
    }

    // =========================================================================
    // listPathways
    // =========================================================================

    @Test
    @DisplayName("listPathways - 空列表返回空结果")
    void listPathways_empty() {
        List<Map<String, Object>> list = pathwayService.listPathways();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("listPathways - 返回已创建的路径列表")
    void listPathways_returnsCreatedPathways() {
        pathwayService.createPathway(validConfig);

        List<Map<String, Object>> list = pathwayService.listPathways();

        assertEquals(1, list.size());
        assertEquals("AMI_STEMI", list.get(0).get("pathway_code"));
        assertEquals("DRAFT", list.get(0).get("draft_status"));
    }

    @Test
    @DisplayName("listPathways - 同时包含草稿和已发布版本")
    void listPathways_draftAndPublished() {
        pathwayService.createPathway(validConfig);

        PublishGateService.GateCheckResult gateResult = mock(PublishGateService.GateCheckResult.class);
        when(gateResult.isReadyToPublish()).thenReturn(true);
        when(gateResult.toMapList()).thenReturn(Collections.emptyList());
        when(publishGateService.checkPathwayReferences(anyList())).thenReturn(gateResult);

        pathwayService.publish("AMI_STEMI", null);

        List<Map<String, Object>> list = pathwayService.listPathways();

        assertEquals(1, list.size());
        Map<String, Object> item = list.get(0);
        assertEquals("AMI_STEMI", item.get("pathway_code"));
        assertEquals("DRAFT", item.get("draft_status"));
        List<?> versions = (List<?>) item.get("published_versions");
        assertFalse(versions.isEmpty());
    }

    // =========================================================================
    // getPathway
    // =========================================================================

    @Test
    @DisplayName("getPathway - 获取已存在的路径详情")
    void getPathway_existingPathway() {
        pathwayService.createPathway(validConfig);

        Map<String, Object> result = pathwayService.getPathway("AMI_STEMI", null);

        assertEquals("AMI_STEMI", result.get("pathway_code"));
        assertEquals("DRAFT", result.get("draft_status"));
        assertNotNull(result.get("draft_config"));
    }

    @Test
    @DisplayName("getPathway - 路径不存在时抛出异常")
    void getPathway_notFound_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pathwayService.getPathway("NON_EXISTENT", null));
        assertTrue(ex.getMessage().contains("pathway not found"));
    }

    // =========================================================================
    // candidates
    // =========================================================================

    @Test
    @DisplayName("candidates - 规则命中 STEMI 候选时返回推荐卡")
    void candidates_stemiHit_returnsRecommendationCard() {
        RuleResult stemiResult = new RuleResult();
        stemiResult.setRuleCode("R_AMI_STEMI_CANDIDATE");
        stemiResult.setHit(true);

        when(ruleService.evaluate(anyMap())).thenReturn(Collections.singletonList(stemiResult));

        Map<String, Object> patientContext = new HashMap<>();
        patientContext.put("patient_id", "P001");
        patientContext.put("encounter_id", "E001");

        List<RecommendationCard> cards = pathwayService.candidates(patientContext);

        assertFalse(cards.isEmpty());
        assertEquals("AMI_STEMI", cards.get(0).getTargetCode());
        assertEquals("PATHWAY_ENTRY", cards.get(0).getScenario());
        verify(persistenceService).saveRecommendation(cards.get(0));
    }

    @Test
    @DisplayName("candidates - 规则未命中时返回空列表")
    void candidates_noHit_returnsEmptyList() {
        RuleResult missResult = new RuleResult();
        missResult.setRuleCode("R_AMI_STEMI_CANDIDATE");
        missResult.setHit(false);

        when(ruleService.evaluate(anyMap())).thenReturn(Collections.singletonList(missResult));

        Map<String, Object> patientContext = new HashMap<>();
        patientContext.put("patient_id", "P001");
        patientContext.put("encounter_id", "E001");

        List<RecommendationCard> cards = pathwayService.candidates(patientContext);

        assertTrue(cards.isEmpty());
        verify(persistenceService, never()).saveRecommendation(any());
    }

    // =========================================================================
    // admit
    // =========================================================================

    @Test
    @DisplayName("admit - 患者入径成功")
    void admit_success() {
        // 先创建并发布路径
        pathwayService.createPathway(validConfig);

        PublishGateService.GateCheckResult gateResult = mock(PublishGateService.GateCheckResult.class);
        when(gateResult.isReadyToPublish()).thenReturn(true);
        when(gateResult.toMapList()).thenReturn(Collections.emptyList());
        when(publishGateService.checkPathwayReferences(anyList())).thenReturn(gateResult);

        pathwayService.publish("AMI_STEMI", null);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");
        request.put("doctor_id", "D001");

        PatientPathwayInstance instance = pathwayService.admit(request);

        assertNotNull(instance);
        assertNotNull(instance.getInstanceId());
        assertTrue(instance.getInstanceId().startsWith("ppi-"));
        assertEquals("P001", instance.getPatientId());
        assertEquals("E001", instance.getEncounterId());
        assertEquals("AMI_STEMI", instance.getPathwayCode());
        assertEquals("1.0.0", instance.getVersionNo());
        assertEquals("ACTIVE", instance.getStatus());
        assertNotNull(instance.getCurrentNodeCode());

        verify(persistenceService).savePatientInstance(instance, "D001");
    }

    @Test
    @DisplayName("admit - 重复入径返回已有实例")
    void admit_duplicate_returnsExistingInstance() {
        pathwayService.createPathway(validConfig);

        PublishGateService.GateCheckResult gateResult = mock(PublishGateService.GateCheckResult.class);
        when(gateResult.isReadyToPublish()).thenReturn(true);
        when(gateResult.toMapList()).thenReturn(Collections.emptyList());
        when(publishGateService.checkPathwayReferences(anyList())).thenReturn(gateResult);

        pathwayService.publish("AMI_STEMI", null);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("patient_id", "P001");
        request.put("encounter_id", "E001");
        request.put("pathway_code", "AMI_STEMI");
        request.put("version_no", "1.0.0");

        PatientPathwayInstance first = pathwayService.admit(request);
        PatientPathwayInstance second = pathwayService.admit(request);

        assertEquals(first.getInstanceId(), second.getInstanceId());
    }

    // =========================================================================
    // completeNode
    // =========================================================================

    @Test
    @DisplayName("completeNode - 完成节点后推进到下一节点")
    void completeNode_advancesToNextNode() {
        // 创建并发布路径
        pathwayService.createPathway(validConfig);

        PublishGateService.GateCheckResult gateResult = mock(PublishGateService.GateCheckResult.class);
        when(gateResult.isReadyToPublish()).thenReturn(true);
        when(gateResult.toMapList()).thenReturn(Collections.emptyList());
        when(publishGateService.checkPathwayReferences(anyList())).thenReturn(gateResult);

        pathwayService.publish("AMI_STEMI", null);

        // 入径
        Map<String, Object> admitRequest = new LinkedHashMap<>();
        admitRequest.put("patient_id", "P001");
        admitRequest.put("encounter_id", "E001");
        admitRequest.put("pathway_code", "AMI_STEMI");
        admitRequest.put("version_no", "1.0.0");

        PatientPathwayInstance instance = pathwayService.admit(admitRequest);
        String instanceId = instance.getInstanceId();
        String currentNode = instance.getCurrentNodeCode();

        // 完成当前节点
        PatientPathwayInstance updated = pathwayService.completeNode(instanceId, currentNode);

        assertNotNull(updated);
        // 节点推进：当前节点码应该已变化（配置中有 transitions 或内置兜底逻辑）
        assertNotEquals(currentNode, updated.getCurrentNodeCode());
    }

    @Test
    @DisplayName("completeNode - 实例不存在时返回 NOT_FOUND 状态")
    void completeNode_instanceNotFound_returnsNotFound() {
        PatientPathwayInstance result = pathwayService.completeNode("non-existent-id", "NODE_1");

        assertNotNull(result);
        assertEquals("non-existent-id", result.getInstanceId());
        assertEquals("NOT_FOUND", result.getStatus());
    }

    @Test
    @DisplayName("completeNode - 完成节点时携带变异信息会记录变异")
    void completeNode_withVariation_recordsVariation() {
        pathwayService.createPathway(validConfig);

        PublishGateService.GateCheckResult gateResult = mock(PublishGateService.GateCheckResult.class);
        when(gateResult.isReadyToPublish()).thenReturn(true);
        when(gateResult.toMapList()).thenReturn(Collections.emptyList());
        when(publishGateService.checkPathwayReferences(anyList())).thenReturn(gateResult);

        pathwayService.publish("AMI_STEMI", null);

        Map<String, Object> admitRequest = new LinkedHashMap<>();
        admitRequest.put("patient_id", "P001");
        admitRequest.put("encounter_id", "E001");
        admitRequest.put("pathway_code", "AMI_STEMI");
        admitRequest.put("version_no", "1.0.0");

        PatientPathwayInstance instance = pathwayService.admit(admitRequest);
        String instanceId = instance.getInstanceId();
        String currentNode = instance.getCurrentNodeCode();

        Map<String, Object> completeRequest = new LinkedHashMap<>();
        completeRequest.put("variation_type", "PATHWAY_DEVIATION");
        completeRequest.put("reason", "患者过敏，调整用药方案");

        PatientPathwayInstance updated = pathwayService.completeNode(instanceId, currentNode, completeRequest);

        assertNotNull(updated);
        verify(persistenceService).saveVariationRecord(any(PathwayVariationRecord.class));
    }

    // =========================================================================
    // recordVariation
    // =========================================================================

    @Test
    @DisplayName("recordVariation - 记录变异成功")
    void recordVariation_success() {
        pathwayService.createPathway(validConfig);

        PublishGateService.GateCheckResult gateResult = mock(PublishGateService.GateCheckResult.class);
        when(gateResult.isReadyToPublish()).thenReturn(true);
        when(gateResult.toMapList()).thenReturn(Collections.emptyList());
        when(publishGateService.checkPathwayReferences(anyList())).thenReturn(gateResult);

        pathwayService.publish("AMI_STEMI", null);

        Map<String, Object> admitRequest = new LinkedHashMap<>();
        admitRequest.put("patient_id", "P001");
        admitRequest.put("encounter_id", "E001");
        admitRequest.put("pathway_code", "AMI_STEMI");
        admitRequest.put("version_no", "1.0.0");

        PatientPathwayInstance instance = pathwayService.admit(admitRequest);

        Map<String, Object> variationRequest = new LinkedHashMap<>();
        variationRequest.put("node_code", instance.getCurrentNodeCode());
        variationRequest.put("variation_type", "TASK_SKIPPED");
        variationRequest.put("reason", "患者拒绝执行该检查");
        variationRequest.put("operator_id", "D001");

        PathwayVariationRecord record = pathwayService.recordVariation(instance.getInstanceId(), variationRequest);

        assertNotNull(record);
        assertNotNull(record.getVariationId());
        assertTrue(record.getVariationId().startsWith("var-"));
        assertEquals(instance.getInstanceId(), record.getInstanceId());
        assertEquals("TASK_SKIPPED", record.getVariationType());
        assertEquals("患者拒绝执行该检查", record.getReason());
        assertEquals("D001", record.getOperatorId());

        verify(persistenceService).saveVariationRecord(record);
    }

    @Test
    @DisplayName("recordVariation - 实例不存在时抛出异常")
    void recordVariation_instanceNotFound_throwsException() {
        Map<String, Object> variationRequest = new LinkedHashMap<>();
        variationRequest.put("variation_type", "PATHWAY_DEVIATION");
        variationRequest.put("reason", "测试");

        assertThrows(IllegalArgumentException.class,
                () -> pathwayService.recordVariation("non-existent-id", variationRequest));
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    /**
     * 构建一份合法的 stages 配置，包含两个节点和 transitions。
     */
    private List<Map<String, Object>> buildStages() {
        List<Map<String, Object>> stages = new ArrayList<>();

        Map<String, Object> stage1 = new LinkedHashMap<>();
        stage1.put("stage_code", "STAGE_1");
        stage1.put("stage_name", "第一阶段");

        List<Map<String, Object>> nodes1 = new ArrayList<>();

        Map<String, Object> node1 = new LinkedHashMap<>();
        node1.put("node_code", "AMI_CHEST_PAIN_IDENTIFY");
        node1.put("node_name", "胸痛识别");
        node1.put("reference_document_code", "DOC_001");
        List<Map<String, Object>> transitions1 = new ArrayList<>();
        Map<String, Object> trans1 = new LinkedHashMap<>();
        trans1.put("to_node", "AMI_REPERFUSION_EVAL");
        trans1.put("priority", 1);
        transitions1.add(trans1);
        node1.put("transitions", transitions1);
        List<Map<String, Object>> tasks1 = new ArrayList<>();
        Map<String, Object> task1 = new LinkedHashMap<>();
        task1.put("task_code", "ECG_EXAM");
        task1.put("task_name", "心电图检查");
        task1.put("task_type", "EXAM");
        task1.put("required", true);
        tasks1.add(task1);
        node1.put("tasks", tasks1);
        nodes1.add(node1);

        Map<String, Object> node2 = new LinkedHashMap<>();
        node2.put("node_code", "AMI_REPERFUSION_EVAL");
        node2.put("node_name", "再灌注评估");
        node2.put("reference_document_code", "DOC_002");
        List<Map<String, Object>> transitions2 = new ArrayList<>();
        Map<String, Object> trans2 = new LinkedHashMap<>();
        trans2.put("to_node", "AMI_INPATIENT_TREATMENT");
        trans2.put("priority", 1);
        transitions2.add(trans2);
        node2.put("transitions", transitions2);
        List<Map<String, Object>> tasks2 = new ArrayList<>();
        Map<String, Object> task2 = new LinkedHashMap<>();
        task2.put("task_code", "THROMBOLYSIS_EVAL");
        task2.put("task_name", "溶栓评估");
        task2.put("task_type", "EVAL");
        task2.put("required", true);
        tasks2.add(task2);
        node2.put("tasks", tasks2);
        nodes1.add(node2);

        Map<String, Object> node3 = new LinkedHashMap<>();
        node3.put("node_code", "AMI_INPATIENT_TREATMENT");
        node3.put("node_name", "住院治疗");
        node3.put("reference_document_code", "DOC_003");
        node3.put("transitions", Collections.emptyList());
        node3.put("tasks", Collections.emptyList());
        nodes1.add(node3);

        stage1.put("nodes", nodes1);
        stages.add(stage1);

        return stages;
    }
}
