package com.medkernel.graph;

import com.medkernel.common.exception.MissingSourceException;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GraphService 单元测试")
class GraphServiceTest {

    @Mock
    private GraphProperties properties;

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private GraphQueryService graphQueryService;

    @Mock
    private GraphVersionService graphVersionService;

    private GraphService graphService;

    @BeforeEach
    void setUp() {
        graphService = new GraphService(properties, persistenceService, graphQueryService, graphVersionService);
    }

    // =========================================================================
    // importGraphVersions
    // =========================================================================

    @Test
    @DisplayName("导入图谱版本 - 委托给 GraphVersionService 并返回结果")
    void importGraphVersions_shouldDelegateToVersionService() {
        List<Map<String, Object>> expected = new ArrayList<Map<String, Object>>();
        Map<String, Object> versionEntry = new LinkedHashMap<String, Object>();
        versionEntry.put("graph_version", "V1");
        expected.add(versionEntry);

        when(graphVersionService.importGraphVersions(any(), any(Map.class), eq("default")))
                .thenReturn(expected);

        Object request = Collections.singletonMap("versions", Collections.singletonList(versionEntry));
        List<Map<String, Object>> result = graphService.importGraphVersions(request);

        assertEquals(1, result.size());
        assertEquals("V1", result.get(0).get("graph_version"));
    }

    @Test
    @DisplayName("导入图谱版本 - 带组织上下文时传递租户ID")
    void importGraphVersions_shouldPassTenantId() {
        List<Map<String, Object>> expected = new ArrayList<Map<String, Object>>();
        when(graphVersionService.importGraphVersions(any(), any(Map.class), eq("tenant_42")))
                .thenReturn(expected);

        OrganizationContext orgContext = new OrganizationContext();
        orgContext.setTenantId("tenant_42");

        Object request = Collections.emptyMap();
        graphService.importGraphVersions(request, orgContext);

        verify(graphVersionService).importGraphVersions(any(), any(Map.class), eq("tenant_42"));
    }

    @Test
    @DisplayName("导入图谱版本 - 空列表时抛出异常")
    void importGraphVersions_shouldThrowWhenEmpty() {
        when(graphVersionService.importGraphVersions(any(), any(Map.class), anyString()))
                .thenThrow(new IllegalArgumentException("graph versions list is empty"));

        assertThrows(IllegalArgumentException.class,
                () -> graphService.importGraphVersions(Collections.emptyMap()));
    }

    // =========================================================================
    // listGraphVersions
    // =========================================================================

    @Test
    @DisplayName("列出图谱版本 - 无数据时返回空列表")
    void listGraphVersions_shouldReturnEmptyWhenNoData() {
        when(graphVersionService.listGraphVersions(any(Map.class), eq("default")))
                .thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = graphService.listGraphVersions();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("列出图谱版本 - 返回已导入的版本列表")
    void listGraphVersions_shouldReturnImportedVersions() {
        List<Map<String, Object>> versions = new ArrayList<Map<String, Object>>();
        Map<String, Object> v1 = new LinkedHashMap<String, Object>();
        v1.put("graph_version", "V1");
        Map<String, Object> v2 = new LinkedHashMap<String, Object>();
        v2.put("graph_version", "V2");
        versions.add(v1);
        versions.add(v2);

        when(graphVersionService.listGraphVersions(any(Map.class), eq("default")))
                .thenReturn(versions);

        List<Map<String, Object>> result = graphService.listGraphVersions();
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("列出图谱版本 - 带过滤器和组织上下文")
    void listGraphVersions_shouldPassFiltersAndTenant() {
        when(graphVersionService.listGraphVersions(any(Map.class), eq("tenant_A")))
                .thenReturn(Collections.emptyList());

        OrganizationContext orgContext = new OrganizationContext();
        orgContext.setTenantId("tenant_A");
        Map<String, String> filters = new HashMap<String, String>();
        filters.put("status", "ACTIVE");

        List<Map<String, Object>> result = graphService.listGraphVersions(filters, orgContext);
        verify(graphVersionService).listGraphVersions(any(Map.class), eq("tenant_A"));
        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // getGraphVersion
    // =========================================================================

    @Test
    @DisplayName("获取图谱版本 - 返回指定版本详情")
    void getGraphVersion_shouldReturnVersion() {
        Map<String, Object> versionEntry = new LinkedHashMap<String, Object>();
        versionEntry.put("graph_version", "V1");
        versionEntry.put("status", "DRAFT");

        when(graphVersionService.getGraphVersion(eq("V1"), any(Map.class)))
                .thenReturn(versionEntry);

        Map<String, Object> result = graphService.getGraphVersion("V1");
        assertEquals("V1", result.get("graph_version"));
        assertEquals("DRAFT", result.get("status"));
    }

    @Test
    @DisplayName("获取图谱版本 - 版本不存在时抛出异常")
    void getGraphVersion_shouldThrowWhenNotFound() {
        when(graphVersionService.getGraphVersion(eq("NONEXISTENT"), any(Map.class)))
                .thenThrow(new IllegalArgumentException("graph version not found: NONEXISTENT"));

        assertThrows(IllegalArgumentException.class,
                () -> graphService.getGraphVersion("NONEXISTENT"));
    }

    @Test
    @DisplayName("获取图谱版本 - 带组织上下文时优先使用租户键查找")
    void getGraphVersion_shouldUseTenantKeyFirst() {
        Map<String, Object> versionEntry = new LinkedHashMap<String, Object>();
        versionEntry.put("graph_version", "V1");
        versionEntry.put("tenant_id", "tenant_X");

        // 先通过 importGraphVersions 将数据写入内部 map
        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        imported.add(versionEntry);
        when(graphVersionService.importGraphVersions(any(), any(Map.class), eq("tenant_X")))
                .thenAnswer(invocation -> {
                    Map<String, Map<String, Object>> map = invocation.getArgument(1);
                    map.put("tenant_X::V1", versionEntry);
                    return imported;
                });

        graphService.importGraphVersions(Collections.emptyMap(), orgContext("tenant_X"));

        Map<String, Object> result = graphService.getGraphVersion("V1", orgContext("tenant_X"));
        assertNotNull(result);
        assertEquals("V1", result.get("graph_version"));
    }

    // =========================================================================
    // activateGraphVersion
    // =========================================================================

    @Test
    @DisplayName("激活图谱版本 - 成功激活")
    void activateGraphVersion_shouldActivate() {
        Map<String, Object> activated = new LinkedHashMap<String, Object>();
        activated.put("graph_version", "V1");
        activated.put("status", "ACTIVE");

        when(graphVersionService.activateGraphVersion(eq("V1"), any(Map.class), any(Map.class), any(), eq("default")))
                .thenReturn(activated);

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("published_by", "admin");
        Map<String, Object> result = graphService.activateGraphVersion("V1", request);

        assertEquals("ACTIVE", result.get("status"));
    }

    @Test
    @DisplayName("激活图谱版本 - 版本不存在时抛出异常")
    void activateGraphVersion_shouldThrowWhenNotFound() {
        when(graphVersionService.activateGraphVersion(eq("MISSING"), any(Map.class), any(Map.class), any(), eq("default")))
                .thenThrow(new IllegalArgumentException("graph version not found: MISSING"));

        Map<String, Object> request = new HashMap<String, Object>();
        assertThrows(IllegalArgumentException.class,
                () -> graphService.activateGraphVersion("MISSING", request));
    }

    @Test
    @DisplayName("激活图谱版本 - 发布门禁未通过时抛出 MissingSourceException")
    void activateGraphVersion_shouldThrowWhenGateCheckFails() {
        when(graphVersionService.activateGraphVersion(eq("V1"), any(Map.class), any(Map.class), any(), eq("default")))
                .thenThrow(new MissingSourceException("发布门禁检查未通过"));

        Map<String, Object> request = new HashMap<String, Object>();
        assertThrows(MissingSourceException.class,
                () -> graphService.activateGraphVersion("V1", request));
    }

    @Test
    @DisplayName("激活图谱版本 - 带组织上下文时传递租户ID")
    void activateGraphVersion_shouldPassTenantId() {
        Map<String, Object> activated = new LinkedHashMap<String, Object>();
        activated.put("graph_version", "V1");
        activated.put("status", "ACTIVE");

        when(graphVersionService.activateGraphVersion(eq("V1"), any(Map.class), any(Map.class), any(), eq("tenant_99")))
                .thenReturn(activated);

        Map<String, Object> request = new HashMap<String, Object>();
        Map<String, Object> result = graphService.activateGraphVersion("V1", request, orgContext("tenant_99"));
        verify(graphVersionService).activateGraphVersion(eq("V1"), any(Map.class), any(Map.class), any(), eq("tenant_99"));
        assertEquals("ACTIVE", result.get("status"));
    }

    // =========================================================================
    // rollbackVersion
    // =========================================================================

    @Test
    @DisplayName("回滚图谱版本 - 成功回滚")
    void rollbackVersion_shouldRollback() {
        Map<String, Object> rollbackResult = new LinkedHashMap<String, Object>();
        rollbackResult.put("graph_version", "V1");
        rollbackResult.put("status", "ACTIVE");
        rollbackResult.put("previous_active_version", "V2");

        when(graphVersionService.rollbackVersion(eq("V1"), any(Map.class), any(Map.class), any(), eq("default")))
                .thenReturn(rollbackResult);

        Map<String, Object> request = new HashMap<String, Object>();
        Map<String, Object> result = graphService.rollbackVersion("V1", request);

        assertEquals("ACTIVE", result.get("status"));
        assertEquals("V2", result.get("previous_active_version"));
    }

    @Test
    @DisplayName("回滚图谱版本 - 版本不存在时抛出异常")
    void rollbackVersion_shouldThrowWhenNotFound() {
        when(graphVersionService.rollbackVersion(eq("MISSING"), any(Map.class), any(Map.class), any(), eq("default")))
                .thenThrow(new IllegalArgumentException("graph version not found: MISSING"));

        Map<String, Object> request = new HashMap<String, Object>();
        assertThrows(IllegalArgumentException.class,
                () -> graphService.rollbackVersion("MISSING", request));
    }

    @Test
    @DisplayName("回滚图谱版本 - 带组织上下文时传递租户ID")
    void rollbackVersion_shouldPassTenantId() {
        Map<String, Object> rollbackResult = new LinkedHashMap<String, Object>();
        rollbackResult.put("graph_version", "V1");
        rollbackResult.put("status", "ACTIVE");

        when(graphVersionService.rollbackVersion(eq("V1"), any(Map.class), any(Map.class), any(), eq("tenant_X")))
                .thenReturn(rollbackResult);

        Map<String, Object> request = new HashMap<String, Object>();
        Map<String, Object> result = graphService.rollbackVersion("V1", request, orgContext("tenant_X"));
        verify(graphVersionService).rollbackVersion(eq("V1"), any(Map.class), any(Map.class), any(), eq("tenant_X"));
        assertEquals("ACTIVE", result.get("status"));
    }

    // =========================================================================
    // importGraphEvidences
    // =========================================================================

    @Test
    @DisplayName("导入图谱证据 - 成功导入单条证据")
    void importGraphEvidences_shouldImportSingleEvidence() {
        when(properties.getDefaultVersion()).thenReturn("AMI_GRAPH_2026_01");

        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("evidence_id", "EV_001");
        evidence.put("target_code", "AMI_STEMI");
        evidence.put("target_type", "DISEASE");
        evidence.put("evidence_type", "GUIDELINE");
        evidence.put("title", "AMI诊疗指南");

        Object request = Collections.singletonMap("evidences", Collections.singletonList(evidence));
        List<Map<String, Object>> result = graphService.importGraphEvidences(request);

        assertEquals(1, result.size());
        assertEquals("EV_001", result.get(0).get("evidence_id"));
        assertEquals("AMI_STEMI", result.get(0).get("target_code"));
        assertEquals("DISEASE", result.get(0).get("target_type"));
        assertEquals("GUIDELINE", result.get(0).get("evidence_type"));
        assertNotNull(result.get(0).get("created_time"));
    }

    @Test
    @DisplayName("导入图谱证据 - 成功导入多条证据")
    void importGraphEvidences_shouldImportMultipleEvidences() {
        when(properties.getDefaultVersion()).thenReturn("AMI_GRAPH_2026_01");

        List<Map<String, Object>> evidences = new ArrayList<Map<String, Object>>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> ev = new LinkedHashMap<String, Object>();
            ev.put("evidence_id", "EV_00" + i);
            ev.put("target_code", "DISEASE_" + i);
            evidences.add(ev);
        }

        Object request = Collections.singletonMap("evidences", evidences);
        List<Map<String, Object>> result = graphService.importGraphEvidences(request);

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("导入图谱证据 - 空列表时抛出异常")
    void importGraphEvidences_shouldThrowWhenEmpty() {
        Object request = Collections.singletonMap("evidences", Collections.emptyList());
        assertThrows(IllegalArgumentException.class,
                () -> graphService.importGraphEvidences(request));
    }

    @Test
    @DisplayName("导入图谱证据 - 缺少必填字段时抛出异常")
    void importGraphEvidences_shouldThrowWhenMissingRequiredField() {
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("target_code", "AMI_STEMI");
        // 缺少 evidence_id

        Object request = Collections.singletonMap("evidences", Collections.singletonList(evidence));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> graphService.importGraphEvidences(request));
        assertTrue(ex.getMessage().contains("evidence_id is required"));
    }

    @Test
    @DisplayName("导入图谱证据 - 带组织上下文时设置租户ID")
    void importGraphEvidences_shouldSetTenantId() {
        when(properties.getDefaultVersion()).thenReturn("AMI_GRAPH_2026_01");

        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("evidence_id", "EV_TENANT");
        evidence.put("target_code", "D1");

        Object request = Collections.singletonMap("evidences", Collections.singletonList(evidence));
        List<Map<String, Object>> result = graphService.importGraphEvidences(request, orgContext("tenant_A"));

        assertEquals(1, result.size());
        assertEquals("tenant_A", result.get(0).get("tenant_id"));
    }

    @Test
    @DisplayName("导入图谱证据 - 使用默认版本号")
    void importGraphEvidences_shouldUseDefaultVersion() {
        when(properties.getDefaultVersion()).thenReturn("DEFAULT_V2");

        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("evidence_id", "EV_VER");
        evidence.put("target_code", "D1");

        Object request = Collections.singletonMap("evidences", Collections.singletonList(evidence));
        List<Map<String, Object>> result = graphService.importGraphEvidences(request);

        assertEquals("DEFAULT_V2", result.get(0).get("graph_version"));
    }

    @Test
    @DisplayName("导入图谱证据 - 指定版本号时使用指定值")
    void importGraphEvidences_shouldUseSpecifiedVersion() {
        when(properties.getDefaultVersion()).thenReturn("DEFAULT_V2");

        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("evidence_id", "EV_VER");
        evidence.put("target_code", "D1");
        evidence.put("graph_version", "CUSTOM_V3");

        Object request = Collections.singletonMap("evidences", Collections.singletonList(evidence));
        List<Map<String, Object>> result = graphService.importGraphEvidences(request);

        assertEquals("CUSTOM_V3", result.get(0).get("graph_version"));
    }

    @Test
    @DisplayName("导入图谱证据 - confidence 默认为 1.0")
    void importGraphEvidences_shouldDefaultConfidenceToOne() {
        when(properties.getDefaultVersion()).thenReturn("V1");

        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("evidence_id", "EV_CONF");
        evidence.put("target_code", "D1");

        Object request = Collections.singletonMap("evidences", Collections.singletonList(evidence));
        List<Map<String, Object>> result = graphService.importGraphEvidences(request);

        assertEquals(1.0, result.get(0).get("confidence"));
    }

    @Test
    @DisplayName("导入图谱证据 - confidence 指定数值时使用指定值")
    void importGraphEvidences_shouldUseSpecifiedConfidence() {
        when(properties.getDefaultVersion()).thenReturn("V1");

        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("evidence_id", "EV_CONF");
        evidence.put("target_code", "D1");
        evidence.put("confidence", 0.85);

        Object request = Collections.singletonMap("evidences", Collections.singletonList(evidence));
        List<Map<String, Object>> result = graphService.importGraphEvidences(request);

        assertEquals(0.85, result.get(0).get("confidence"));
    }

    // =========================================================================
    // listGraphEvidences
    // =========================================================================

    @Test
    @DisplayName("列出图谱证据 - 无数据时返回空列表")
    void listGraphEvidences_shouldReturnEmptyWhenNoData() {
        List<Map<String, Object>> result = graphService.listGraphEvidences(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("列出图谱证据 - 返回已导入的证据")
    void listGraphEvidences_shouldReturnImportedEvidences() {
        when(properties.getDefaultVersion()).thenReturn("V1");

        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("evidence_id", "EV_LIST_1");
        evidence.put("target_code", "D1");
        graphService.importGraphEvidences(
                Collections.singletonMap("evidences", Collections.singletonList(evidence)));

        List<Map<String, Object>> result = graphService.listGraphEvidences(null);
        assertEquals(1, result.size());
        assertEquals("EV_LIST_1", result.get(0).get("evidence_id"));
    }

    @Test
    @DisplayName("列出图谱证据 - 按 graphVersion 过滤")
    void listGraphEvidences_shouldFilterByGraphVersion() {
        when(properties.getDefaultVersion()).thenReturn("V1");

        Map<String, Object> ev1 = new LinkedHashMap<String, Object>();
        ev1.put("evidence_id", "EV_F1");
        ev1.put("target_code", "D1");
        graphService.importGraphEvidences(
                Collections.singletonMap("evidences", Collections.singletonList(ev1)));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("graphVersion", "V1");
        List<Map<String, Object>> result = graphService.listGraphEvidences(filters);
        assertEquals(1, result.size());

        filters.put("graphVersion", "NONEXISTENT");
        result = graphService.listGraphEvidences(filters);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("列出图谱证据 - 按 targetCode 过滤")
    void listGraphEvidences_shouldFilterByTargetCode() {
        when(properties.getDefaultVersion()).thenReturn("V1");

        Map<String, Object> ev1 = new LinkedHashMap<String, Object>();
        ev1.put("evidence_id", "EV_TC1");
        ev1.put("target_code", "DISEASE_A");
        Map<String, Object> ev2 = new LinkedHashMap<String, Object>();
        ev2.put("evidence_id", "EV_TC2");
        ev2.put("target_code", "DISEASE_B");

        List<Map<String, Object>> evidences = new ArrayList<Map<String, Object>>();
        evidences.add(ev1);
        evidences.add(ev2);
        graphService.importGraphEvidences(
                Collections.singletonMap("evidences", evidences));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("targetCode", "DISEASE_A");
        List<Map<String, Object>> result = graphService.listGraphEvidences(filters);
        assertEquals(1, result.size());
        assertEquals("EV_TC1", result.get(0).get("evidence_id"));
    }

    @Test
    @DisplayName("列出图谱证据 - 按 targetType 过滤")
    void listGraphEvidences_shouldFilterByTargetType() {
        when(properties.getDefaultVersion()).thenReturn("V1");

        Map<String, Object> ev1 = new LinkedHashMap<String, Object>();
        ev1.put("evidence_id", "EV_TT1");
        ev1.put("target_code", "D1");
        ev1.put("target_type", "DISEASE");
        Map<String, Object> ev2 = new LinkedHashMap<String, Object>();
        ev2.put("evidence_id", "EV_TT2");
        ev2.put("target_code", "D2");
        ev2.put("target_type", "PATHWAY");

        List<Map<String, Object>> evidences = new ArrayList<Map<String, Object>>();
        evidences.add(ev1);
        evidences.add(ev2);
        graphService.importGraphEvidences(
                Collections.singletonMap("evidences", evidences));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("targetType", "DISEASE");
        List<Map<String, Object>> result = graphService.listGraphEvidences(filters);
        assertEquals(1, result.size());
        assertEquals("EV_TT1", result.get(0).get("evidence_id"));
    }

    @Test
    @DisplayName("列出图谱证据 - 按 evidenceType 过滤")
    void listGraphEvidences_shouldFilterByEvidenceType() {
        when(properties.getDefaultVersion()).thenReturn("V1");

        Map<String, Object> ev1 = new LinkedHashMap<String, Object>();
        ev1.put("evidence_id", "EV_ET1");
        ev1.put("target_code", "D1");
        ev1.put("evidence_type", "GUIDELINE");
        Map<String, Object> ev2 = new LinkedHashMap<String, Object>();
        ev2.put("evidence_id", "EV_ET2");
        ev2.put("target_code", "D2");
        ev2.put("evidence_type", "LITERATURE");

        List<Map<String, Object>> evidences = new ArrayList<Map<String, Object>>();
        evidences.add(ev1);
        evidences.add(ev2);
        graphService.importGraphEvidences(
                Collections.singletonMap("evidences", evidences));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("evidenceType", "GUIDELINE");
        List<Map<String, Object>> result = graphService.listGraphEvidences(filters);
        assertEquals(1, result.size());
        assertEquals("EV_ET1", result.get(0).get("evidence_id"));
    }

    @Test
    @DisplayName("列出图谱证据 - limit 参数限制返回数量")
    void listGraphEvidences_shouldRespectLimit() {
        when(properties.getDefaultVersion()).thenReturn("V1");

        List<Map<String, Object>> evidences = new ArrayList<Map<String, Object>>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> ev = new LinkedHashMap<String, Object>();
            ev.put("evidence_id", "EV_LIM_" + i);
            ev.put("target_code", "D" + i);
            evidences.add(ev);
        }
        graphService.importGraphEvidences(
                Collections.singletonMap("evidences", evidences));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("limit", "2");
        List<Map<String, Object>> result = graphService.listGraphEvidences(filters);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("列出图谱证据 - 租户隔离")
    void listGraphEvidences_shouldIsolateByTenant() {
        when(properties.getDefaultVersion()).thenReturn("V1");

        Map<String, Object> ev1 = new LinkedHashMap<String, Object>();
        ev1.put("evidence_id", "EV_T_A");
        ev1.put("target_code", "D1");
        graphService.importGraphEvidences(
                Collections.singletonMap("evidences", Collections.singletonList(ev1)),
                orgContext("tenant_A"));

        Map<String, Object> ev2 = new LinkedHashMap<String, Object>();
        ev2.put("evidence_id", "EV_T_B");
        ev2.put("target_code", "D2");
        graphService.importGraphEvidences(
                Collections.singletonMap("evidences", Collections.singletonList(ev2)),
                orgContext("tenant_B"));

        List<Map<String, Object>> resultA = graphService.listGraphEvidences(null, orgContext("tenant_A"));
        assertEquals(1, resultA.size());
        assertEquals("EV_T_A", resultA.get(0).get("evidence_id"));

        List<Map<String, Object>> resultB = graphService.listGraphEvidences(null, orgContext("tenant_B"));
        assertEquals(1, resultB.size());
        assertEquals("EV_T_B", resultB.get(0).get("evidence_id"));
    }

    // =========================================================================
    // diseaseCandidates
    // =========================================================================

    @Test
    @DisplayName("疾病候选召回 - 成功返回候选列表")
    void diseaseCandidates_shouldReturnCandidates() {
        List<GraphCandidate> candidates = new ArrayList<GraphCandidate>();
        GraphCandidate candidate = new GraphCandidate();
        candidate.setDiseaseCode("AMI_STEMI");
        candidate.setDiseaseName("急性ST段抬高型心肌梗死");
        candidate.setRawGraphScore(92.0);
        candidate.setGraphVersion("V1");
        candidate.setGraphSource("FALLBACK_HEURISTIC");
        candidates.add(candidate);

        GraphCandidateQueryResult queryResult = new GraphCandidateQueryResult(
                candidates, "V1", "FALLBACK_HEURISTIC", false, null);

        when(graphQueryService.diseaseCandidates(any(Map.class), any(Map.class), any(List.class), eq("default")))
                .thenReturn(queryResult);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("symptom_codes", Collections.singletonList("CHEST_PAIN"));

        List<GraphCandidate> result = graphService.diseaseCandidates(request);

        assertEquals(1, result.size());
        assertEquals("AMI_STEMI", result.get(0).getDiseaseCode());
        assertEquals(92.0, result.get(0).getRawGraphScore());
    }

    @Test
    @DisplayName("疾病候选召回 - 无匹配时返回空列表")
    void diseaseCandidates_shouldReturnEmptyWhenNoMatch() {
        GraphCandidateQueryResult queryResult = new GraphCandidateQueryResult(
                Collections.<GraphCandidate>emptyList(), "V1", "FALLBACK_HEURISTIC", false, null);

        when(graphQueryService.diseaseCandidates(any(Map.class), any(Map.class), any(List.class), eq("default")))
                .thenReturn(queryResult);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("symptom_codes", Collections.singletonList("UNKNOWN_SYMPTOM"));

        List<GraphCandidate> result = graphService.diseaseCandidates(request);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("疾病候选召回 - 带组织上下文时传递租户ID")
    void diseaseCandidates_shouldPassTenantId() {
        GraphCandidateQueryResult queryResult = new GraphCandidateQueryResult(
                Collections.<GraphCandidate>emptyList(), "V1", "FALLBACK_HEURISTIC", false, null);

        when(graphQueryService.diseaseCandidates(any(Map.class), any(Map.class), any(List.class), eq("tenant_42")))
                .thenReturn(queryResult);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> request = new HashMap<String, Object>();
        graphService.diseaseCandidates(request, orgContext("tenant_42"));

        verify(graphQueryService).diseaseCandidates(any(Map.class), any(Map.class), any(List.class), eq("tenant_42"));
    }

    @Test
    @DisplayName("疾病候选召回 - 写入审计日志")
    void diseaseCandidates_shouldWriteAuditLog() {
        List<GraphCandidate> candidates = new ArrayList<GraphCandidate>();
        GraphCandidate candidate = new GraphCandidate();
        candidate.setDiseaseCode("AMI_STEMI");
        candidates.add(candidate);

        GraphCandidateQueryResult queryResult = new GraphCandidateQueryResult(
                candidates, "V1", "FALLBACK_HEURISTIC", false, null);

        when(graphQueryService.diseaseCandidates(any(Map.class), any(Map.class), any(List.class), eq("default")))
                .thenReturn(queryResult);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        graphService.diseaseCandidates(new HashMap<String, Object>());

        verify(persistenceService).saveAuditLog(
                eq("GRAPH"), eq("DISEASE_CANDIDATES"), eq("Disease"), any(),
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("疾病候选召回 - 降级时标记 degraded")
    void diseaseCandidates_shouldMarkDegraded() {
        List<GraphCandidate> candidates = new ArrayList<GraphCandidate>();
        GraphCandidate candidate = new GraphCandidate();
        candidate.setDiseaseCode("AMI_STEMI");
        candidate.setDegraded(true);
        candidate.setDegradedReason("Neo4j配置不完整");
        candidates.add(candidate);

        GraphCandidateQueryResult queryResult = new GraphCandidateQueryResult(
                candidates, "V1", "FALLBACK_HEURISTIC", true, "Neo4j配置不完整");

        when(graphQueryService.diseaseCandidates(any(Map.class), any(Map.class), any(List.class), eq("default")))
                .thenReturn(queryResult);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        List<GraphCandidate> result = graphService.diseaseCandidates(new HashMap<String, Object>());
        assertTrue(result.get(0).isDegraded());
        assertEquals("Neo4j配置不完整", result.get(0).getDegradedReason());
    }

    // =========================================================================
    // evidence
    // =========================================================================

    @Test
    @DisplayName("证据查询 - 成功返回证据列表")
    void evidence_shouldReturnEvidenceList() {
        List<Map<String, Object>> evidenceList = new ArrayList<Map<String, Object>>();
        Map<String, Object> ev = new LinkedHashMap<String, Object>();
        ev.put("evidence_id", "EV_001");
        ev.put("title", "AMI诊疗指南");
        ev.put("target_code", "AMI_STEMI");
        evidenceList.add(ev);

        GraphEvidenceQueryResult queryResult = new GraphEvidenceQueryResult(
                evidenceList, "AMI_STEMI", "V1", "FALLBACK_HEURISTIC", false, null);

        when(graphQueryService.evidence(any(Map.class), any(Map.class), eq("default")))
                .thenReturn(queryResult);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("target_code", "AMI_STEMI");

        List<Map<String, Object>> result = graphService.evidence(request);

        assertEquals(1, result.size());
        assertEquals("EV_001", result.get(0).get("evidence_id"));
    }

    @Test
    @DisplayName("证据查询 - 无匹配时返回空列表")
    void evidence_shouldReturnEmptyWhenNoMatch() {
        GraphEvidenceQueryResult queryResult = new GraphEvidenceQueryResult(
                Collections.<Map<String, Object>>emptyList(), "UNKNOWN", "V1", "FALLBACK_HEURISTIC", false, null);

        when(graphQueryService.evidence(any(Map.class), any(Map.class), eq("default")))
                .thenReturn(queryResult);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("target_code", "UNKNOWN");

        List<Map<String, Object>> result = graphService.evidence(request);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("证据查询 - 带组织上下文时传递租户ID")
    void evidence_shouldPassTenantId() {
        GraphEvidenceQueryResult queryResult = new GraphEvidenceQueryResult(
                Collections.<Map<String, Object>>emptyList(), "D1", "V1", "FALLBACK_HEURISTIC", false, null);

        when(graphQueryService.evidence(any(Map.class), any(Map.class), eq("tenant_X")))
                .thenReturn(queryResult);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("target_code", "D1");
        graphService.evidence(request, orgContext("tenant_X"));

        verify(graphQueryService).evidence(any(Map.class), any(Map.class), eq("tenant_X"));
    }

    @Test
    @DisplayName("证据查询 - 写入审计日志")
    void evidence_shouldWriteAuditLog() {
        List<Map<String, Object>> evidenceList = new ArrayList<Map<String, Object>>();
        Map<String, Object> ev = new LinkedHashMap<String, Object>();
        ev.put("evidence_id", "EV_AUDIT");
        evidenceList.add(ev);

        GraphEvidenceQueryResult queryResult = new GraphEvidenceQueryResult(
                evidenceList, "D1", "V1", "FALLBACK_HEURISTIC", false, null);

        when(graphQueryService.evidence(any(Map.class), any(Map.class), eq("default")))
                .thenReturn(queryResult);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("target_code", "D1");
        graphService.evidence(request);

        verify(persistenceService).saveAuditLog(
                eq("GRAPH"), eq("EVIDENCE_QUERY"), eq("Evidence"), any(),
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("证据查询 - 降级时包含降级信息")
    void evidence_shouldIncludeDegradedInfo() {
        List<Map<String, Object>> evidenceList = new ArrayList<Map<String, Object>>();
        Map<String, Object> ev = new LinkedHashMap<String, Object>();
        ev.put("evidence_id", "EV_DEG");
        ev.put("degraded", true);
        ev.put("degraded_reason", "Neo4j查询不可用");
        evidenceList.add(ev);

        GraphEvidenceQueryResult queryResult = new GraphEvidenceQueryResult(
                evidenceList, "D1", "V1", "FALLBACK_HEURISTIC", true, "Neo4j查询不可用");

        when(graphQueryService.evidence(any(Map.class), any(Map.class), eq("default")))
                .thenReturn(queryResult);
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("target_code", "D1");
        List<Map<String, Object>> result = graphService.evidence(request);

        assertEquals(1, result.size());
        assertEquals(true, result.get(0).get("degraded"));
        assertEquals("Neo4j查询不可用", result.get(0).get("degraded_reason"));
    }

    // =========================================================================
    // 辅助方法
    // =========================================================================

    private OrganizationContext orgContext(String tenantId) {
        OrganizationContext ctx = new OrganizationContext();
        ctx.setTenantId(tenantId);
        return ctx;
    }
}
