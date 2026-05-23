package com.medkernel.terminology;

import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminologyServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    private TerminologyService terminologyService;

    @BeforeEach
    void setUp() {
        // 构造函数内部调用 seedMappings()，需让 persistenceService.enabled() 返回 false
        // 以避免持久化层交互；saveUnmappedQueueEntry 也需容错（normalize 未命中时调用）
        lenient().when(persistenceService.enabled()).thenReturn(false);
        terminologyService = new TerminologyService(persistenceService);
    }

    // =========================================================================
    // normalize()
    // =========================================================================

    @Test
    @DisplayName("normalize - 命中已审核映射时返回匹配结果")
    void normalize_matchedMapping() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "I21.0");
        request.put("source_name", "急性前壁ST段抬高型心肌梗死");
        request.put("concept_type", "DIAGNOSIS");

        Map<String, Object> result = terminologyService.normalize(request);

        assertEquals(true, result.get("matched"));
        assertEquals("AMI_STEMI", result.get("standard_code"));
        assertEquals("急性ST段抬高型心肌梗死", result.get("standard_name"));
        assertEquals("READY", result.get("governance_status"));
        assertEquals("APPROVED", result.get("mapping_status"));
    }

    @Test
    @DisplayName("normalize - 未命中映射时进入治理队列")
    void normalize_unmapped() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "UNKNOWN_CODE");
        request.put("source_name", "未知诊断");
        request.put("concept_type", "DIAGNOSIS");

        Map<String, Object> result = terminologyService.normalize(request);

        assertEquals(false, result.get("matched"));
        assertNull(result.get("standard_code"));
        assertEquals("PENDING_MAPPING", result.get("governance_status"));
        assertEquals("UNMAPPED", result.get("mapping_status"));
        assertNotNull(result.get("queue_id"));
    }

    @Test
    @DisplayName("normalize - 缺少必填字段 source_system 时抛出异常")
    void normalize_missingSourceSystem() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_code", "I21.0");
        request.put("concept_type", "DIAGNOSIS");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.normalize(request));
        assertTrue(ex.getMessage().contains("source_system"));
    }

    @Test
    @DisplayName("normalize - 缺少必填字段 source_code 时抛出异常")
    void normalize_missingSourceCode() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("concept_type", "DIAGNOSIS");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.normalize(request));
        assertTrue(ex.getMessage().contains("source_code"));
    }

    @Test
    @DisplayName("normalize - 缺少必填字段 concept_type 时抛出异常")
    void normalize_missingConceptType() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "I21.0");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.normalize(request));
        assertTrue(ex.getMessage().contains("concept_type"));
    }

    @Test
    @DisplayName("normalize - 命中映射时调用审计日志")
    void normalize_auditCalledOnMatch() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "I21.0");
        request.put("concept_type", "DIAGNOSIS");

        terminologyService.normalize(request);

        verify(persistenceService).saveAuditLog(
                eq("TERMINOLOGY"), eq("NORMALIZE"), eq("CONCEPT"),
                anyString(), any(), any(), any(), any(Map.class));
    }

    // =========================================================================
    // normalizeCode()
    // =========================================================================

    @Test
    @DisplayName("normalizeCode - 命中种子映射 HIS/I21.0/DIAGNOSIS")
    void normalizeCode_hitSeedMapping() {
        Map<String, Object> result = terminologyService.normalizeCode("HIS", "I21.0", "急性前壁ST段抬高型心肌梗死", "DIAGNOSIS");

        assertEquals(true, result.get("matched"));
        assertEquals("AMI_STEMI", result.get("standard_code"));
        assertEquals("急性ST段抬高型心肌梗死", result.get("standard_name"));
        assertEquals("APPROVED", result.get("mapping_status"));
        assertEquals(0.98, result.get("confidence"));
        assertEquals("BUILT_IN_SAMPLE", result.get("mapping_source"));
    }

    @Test
    @DisplayName("normalizeCode - 命中种子映射 EMR/CHEST_PAIN/SYMPTOM")
    void normalizeCode_hitSymptomMapping() {
        Map<String, Object> result = terminologyService.normalizeCode("EMR", "CHEST_PAIN", "胸痛", "SYMPTOM");

        assertEquals(true, result.get("matched"));
        assertEquals("CHEST_PAIN", result.get("standard_code"));
    }

    @Test
    @DisplayName("normalizeCode - 命中种子映射 LIS/TNI/LAB_ITEM")
    void normalizeCode_hitLabItemMapping() {
        Map<String, Object> result = terminologyService.normalizeCode("LIS", "TNI", "肌钙蛋白I", "LAB_ITEM");

        assertEquals(true, result.get("matched"));
        assertEquals("TROPONIN_I", result.get("standard_code"));
    }

    @Test
    @DisplayName("normalizeCode - 未命中映射返回 UNMAPPED")
    void normalizeCode_unmapped() {
        Map<String, Object> result = terminologyService.normalizeCode("HIS", "XYZ999", "未知", "DIAGNOSIS");

        assertEquals(false, result.get("matched"));
        assertNull(result.get("standard_code"));
        assertEquals("UNMAPPED", result.get("mapping_status"));
        assertEquals("PENDING_MAPPING", result.get("governance_status"));
        assertEquals("GOVERNANCE_QUEUE", result.get("mapping_source"));
    }

    @Test
    @DisplayName("normalizeCode - 大小写不敏感匹配（自动转大写）")
    void normalizeCode_caseInsensitive() {
        Map<String, Object> result = terminologyService.normalizeCode("his", "i21.0", "测试", "diagnosis");

        assertEquals(true, result.get("matched"));
        assertEquals("AMI_STEMI", result.get("standard_code"));
    }

    @Test
    @DisplayName("normalizeCode - 前后空格自动裁剪")
    void normalizeCode_trimWhitespace() {
        Map<String, Object> result = terminologyService.normalizeCode("  HIS  ", "  I21.0  ", "测试", "  DIAGNOSIS  ");

        assertEquals(true, result.get("matched"));
        assertEquals("AMI_STEMI", result.get("standard_code"));
    }

    // =========================================================================
    // importMappings()
    // =========================================================================

    @Test
    @DisplayName("importMappings - 批量导入映射列表")
    void importMappings_listInput() {
        List<Map<String, Object>> mappings = new ArrayList<>();
        Map<String, Object> m1 = new HashMap<>();
        m1.put("source_system", "CUSTOM");
        m1.put("source_code", "C001");
        m1.put("source_name", "自定义码1");
        m1.put("concept_type", "DIAGNOSIS");
        m1.put("standard_code", "STD_C001");
        m1.put("standard_name", "标准码1");
        mappings.add(m1);

        Map<String, Object> m2 = new HashMap<>();
        m2.put("source_system", "CUSTOM");
        m2.put("source_code", "C002");
        m2.put("source_name", "自定义码2");
        m2.put("concept_type", "DIAGNOSIS");
        m2.put("standard_code", "STD_C002");
        m2.put("standard_name", "标准码2");
        mappings.add(m2);

        List<Map<String, Object>> imported = terminologyService.importMappings(mappings);

        assertEquals(2, imported.size());
        assertEquals("STD_C001", imported.get(0).get("standard_code"));
        assertEquals("STD_C002", imported.get(1).get("standard_code"));
    }

    @Test
    @DisplayName("importMappings - 包装在 mappings 字段中的嵌套格式")
    void importMappings_nestedFormat() {
        Map<String, Object> wrapper = new HashMap<>();
        List<Map<String, Object>> mappingList = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("source_system", "SYS");
        m.put("source_code", "S001");
        m.put("concept_type", "FINDING");
        m.put("standard_code", "STD_S001");
        mappingList.add(m);
        wrapper.put("mappings", mappingList);

        List<Map<String, Object>> imported = terminologyService.importMappings(wrapper);

        assertEquals(1, imported.size());
        assertEquals("STD_S001", imported.get(0).get("standard_code"));
    }

    @Test
    @DisplayName("importMappings - 空列表抛出异常")
    void importMappings_emptyList() {
        List<Map<String, Object>> empty = new ArrayList<>();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.importMappings(empty));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("importMappings - 缺少必填字段时整体回退")
    void importMappings_missingFieldCausesRollback() {
        List<Map<String, Object>> mappings = new ArrayList<>();
        Map<String, Object> valid = new HashMap<>();
        valid.put("source_system", "SYS");
        valid.put("source_code", "S001");
        valid.put("concept_type", "DIAGNOSIS");
        valid.put("standard_code", "STD_S001");
        mappings.add(valid);

        Map<String, Object> invalid = new HashMap<>();
        invalid.put("source_system", "SYS");
        // 缺少 source_code
        invalid.put("concept_type", "DIAGNOSIS");
        invalid.put("standard_code", "STD_S002");
        mappings.add(invalid);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.importMappings(mappings));
        assertTrue(ex.getMessage().contains("source_code"));
    }

    @Test
    @DisplayName("importMappings - 导入后可通过 getMapping 查询")
    void importMappings_queryableAfterImport() {
        List<Map<String, Object>> mappings = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("source_system", "NEW_SYS");
        m.put("source_code", "NC001");
        m.put("concept_type", "DIAGNOSIS");
        m.put("standard_code", "STD_NC001");
        mappings.add(m);

        terminologyService.importMappings(mappings);

        Map<String, Object> result = terminologyService.getMapping("NEW_SYS", "NC001", "DIAGNOSIS");
        assertEquals("STD_NC001", result.get("standard_code"));
    }

    @Test
    @DisplayName("importMappings - 默认值填充（mapping_status 默认 APPROVED，mapping_source 默认 IMPORTED）")
    void importMappings_defaultValues() {
        List<Map<String, Object>> mappings = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("source_system", "SYS");
        m.put("source_code", "S001");
        m.put("concept_type", "DIAGNOSIS");
        m.put("standard_code", "STD_S001");
        mappings.add(m);

        List<Map<String, Object>> imported = terminologyService.importMappings(mappings);

        assertEquals("APPROVED", imported.get(0).get("mapping_status"));
        assertEquals("IMPORTED", imported.get(0).get("mapping_source"));
        assertEquals(1.00, imported.get(0).get("confidence"));
    }

    @Test
    @DisplayName("importMappings - 覆盖已有映射（同 key 覆盖）")
    void importMappings_overrideExisting() {
        List<Map<String, Object>> mappings = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("source_system", "HIS");
        m.put("source_code", "I21.0");
        m.put("concept_type", "DIAGNOSIS");
        m.put("standard_code", "NEW_STANDARD");
        m.put("standard_name", "新标准名");
        mappings.add(m);

        terminologyService.importMappings(mappings);

        Map<String, Object> result = terminologyService.getMapping("HIS", "I21.0", "DIAGNOSIS");
        assertEquals("NEW_STANDARD", result.get("standard_code"));
    }

    // =========================================================================
    // listMappings()
    // =========================================================================

    @Test
    @DisplayName("listMappings - 返回种子映射列表")
    void listMappings_returnsSeedMappings() {
        List<Map<String, Object>> list = terminologyService.listMappings();

        assertFalse(list.isEmpty());
        // 种子映射有 8 条
        assertTrue(list.size() >= 8);
    }

    @Test
    @DisplayName("listMappings - 结果按 source_system、concept_type、source_code 排序")
    void listMappings_sortedOrder() {
        List<Map<String, Object>> list = terminologyService.listMappings();

        for (int i = 1; i < list.size(); i++) {
            String prevSystem = (String) list.get(i - 1).get("source_system");
            String currSystem = (String) list.get(i).get("source_system");
            assertTrue(prevSystem.compareTo(currSystem) <= 0,
                    "source_system 应升序排列");
        }
    }

    @Test
    @DisplayName("listMappings - 导入新映射后列表增长")
    void listMappings_growsAfterImport() {
        int before = terminologyService.listMappings().size();

        List<Map<String, Object>> mappings = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("source_system", "NEW");
        m.put("source_code", "X001");
        m.put("concept_type", "DIAGNOSIS");
        m.put("standard_code", "STD_X001");
        mappings.add(m);
        terminologyService.importMappings(mappings);

        int after = terminologyService.listMappings().size();
        assertEquals(before + 1, after);
    }

    // =========================================================================
    // getMapping()
    // =========================================================================

    @Test
    @DisplayName("getMapping - 查询存在的映射")
    void getMapping_found() {
        Map<String, Object> result = terminologyService.getMapping("HIS", "I21.0", "DIAGNOSIS");

        assertNotNull(result);
        assertEquals("AMI_STEMI", result.get("standard_code"));
        assertEquals("HIS", result.get("source_system"));
        assertEquals("I21.0", result.get("source_code"));
        assertEquals("DIAGNOSIS", result.get("concept_type"));
    }

    @Test
    @DisplayName("getMapping - 查询不存在的映射抛出异常")
    void getMapping_notFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.getMapping("XXX", "YYY", "ZZZ"));
        assertTrue(ex.getMessage().contains("mapping not found"));
    }

    @Test
    @DisplayName("getMapping - 大小写不敏感查询")
    void getMapping_caseInsensitive() {
        Map<String, Object> result = terminologyService.getMapping("his", "i21.0", "diagnosis");

        assertNotNull(result);
        assertEquals("AMI_STEMI", result.get("standard_code"));
    }

    @Test
    @DisplayName("getMapping - LIS/CTNI 映射到同一标准码 TROPONIN_I")
    void getMapping_differentSourceCodesSameStandard() {
        Map<String, Object> tni = terminologyService.getMapping("LIS", "TNI", "LAB_ITEM");
        Map<String, Object> ctni = terminologyService.getMapping("LIS", "CTNI", "LAB_ITEM");

        assertEquals(tni.get("standard_code"), ctni.get("standard_code"));
        assertEquals("TROPONIN_I", tni.get("standard_code"));
    }

    // =========================================================================
    // listPendingMappings()
    // =========================================================================

    @Test
    @DisplayName("listPendingMappings - 无待处理记录时返回空列表")
    void listPendingMappings_empty() {
        Map<String, String> filters = new HashMap<>();
        List<Map<String, Object>> result = terminologyService.listPendingMappings(filters);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listPendingMappings - normalize 未命中后可查到待处理记录")
    void listPendingMappings_afterUnmappedNormalize() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "UNMAPPED_001");
        request.put("concept_type", "DIAGNOSIS");
        terminologyService.normalize(request);

        Map<String, String> filters = new HashMap<>();
        List<Map<String, Object>> result = terminologyService.listPendingMappings(filters);

        assertFalse(result.isEmpty());
        Map<String, Object> entry = result.get(0);
        assertEquals("PENDING_MAPPING", entry.get("governance_status"));
        assertEquals("HIS", entry.get("source_system"));
        assertEquals("UNMAPPED_001", entry.get("source_code"));
    }

    @Test
    @DisplayName("listPendingMappings - 按 governanceStatus 过滤")
    void listPendingMappings_filterByStatus() {
        // 先产生一条 PENDING_MAPPING 记录
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "UNMAPPED_002");
        request.put("concept_type", "DIAGNOSIS");
        terminologyService.normalize(request);

        Map<String, String> filters = new HashMap<>();
        filters.put("governanceStatus", "PENDING_MAPPING");
        List<Map<String, Object>> result = terminologyService.listPendingMappings(filters);

        assertFalse(result.isEmpty());
        for (Map<String, Object> entry : result) {
            assertEquals("PENDING_MAPPING", entry.get("governance_status"));
        }
    }

    @Test
    @DisplayName("listPendingMappings - 按 sourceSystem 过滤")
    void listPendingMappings_filterBySourceSystem() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "UNMAPPED_003");
        request.put("concept_type", "DIAGNOSIS");
        terminologyService.normalize(request);

        Map<String, String> filters = new HashMap<>();
        filters.put("sourceSystem", "HIS");
        List<Map<String, Object>> result = terminologyService.listPendingMappings(filters);

        assertFalse(result.isEmpty());
        for (Map<String, Object> entry : result) {
            assertEquals("HIS", entry.get("source_system"));
        }
    }

    @Test
    @DisplayName("listPendingMappings - 按 conceptType 过滤")
    void listPendingMappings_filterByConceptType() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "UNMAPPED_004");
        request.put("concept_type", "DIAGNOSIS");
        terminologyService.normalize(request);

        Map<String, String> filters = new HashMap<>();
        filters.put("conceptType", "DIAGNOSIS");
        List<Map<String, Object>> result = terminologyService.listPendingMappings(filters);

        assertFalse(result.isEmpty());
        for (Map<String, Object> entry : result) {
            assertEquals("DIAGNOSIS", entry.get("concept_type"));
        }
    }

    @Test
    @DisplayName("listPendingMappings - limit 参数限制返回数量")
    void listPendingMappings_limit() {
        // 产生两条待处理记录
        for (int i = 10; i < 12; i++) {
            Map<String, Object> request = new HashMap<>();
            request.put("source_system", "HIS");
            request.put("source_code", "UNMAPPED_LIMIT_" + i);
            request.put("concept_type", "DIAGNOSIS");
            terminologyService.normalize(request);
        }

        Map<String, String> filters = new HashMap<>();
        filters.put("limit", "1");
        List<Map<String, Object>> result = terminologyService.listPendingMappings(filters);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("listPendingMappings - 持久化层启用时委托给 persistenceService")
    void listPendingMappings_delegatesToPersistence() {
        when(persistenceService.enabled()).thenReturn(true);
        List<Map<String, Object>> dbResult = new ArrayList<>();
        when(persistenceService.listUnmappedQueue(eq("default"), any(), any(), any(), anyInt()))
                .thenReturn(dbResult);

        Map<String, String> filters = new HashMap<>();
        List<Map<String, Object>> result = terminologyService.listPendingMappings(filters);

        assertTrue(result.isEmpty());
        verify(persistenceService).listUnmappedQueue(eq("default"), isNull(), isNull(), isNull(), eq(100));
    }

    // =========================================================================
    // approvePendingMapping()
    // =========================================================================

    @Test
    @DisplayName("approvePendingMapping - 审批待处理映射成功")
    void approvePendingMapping_success() {
        // 先产生一条 PENDING_MAPPING 记录
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "APPROVE_001");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = terminologyService.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        Map<String, Object> approveRequest = new HashMap<>();
        approveRequest.put("standard_code", "APPROVED_STD_001");
        approveRequest.put("standard_name", "审批标准名");
        approveRequest.put("reviewed_by", "admin");

        Map<String, Object> result = terminologyService.approvePendingMapping(queueId, approveRequest);

        assertEquals("APPROVED", result.get("governance_status"));
        assertEquals("APPROVED_STD_001", result.get("standard_code"));
        assertEquals("审批标准名", result.get("standard_name"));
        assertEquals("admin", result.get("reviewed_by"));
        assertNotNull(result.get("reviewed_time"));
    }

    @Test
    @DisplayName("approvePendingMapping - 审批后映射写入缓存，可被 normalize 命中")
    void approvePendingMapping_mappingBecomesQueryable() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "APPROVE_002");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = terminologyService.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        Map<String, Object> approveRequest = new HashMap<>();
        approveRequest.put("standard_code", "APPROVED_STD_002");
        approveRequest.put("reviewed_by", "admin");
        terminologyService.approvePendingMapping(queueId, approveRequest);

        // 再次 normalize 应命中
        Map<String, Object> secondResult = terminologyService.normalizeCode("HIS", "APPROVE_002", null, "DIAGNOSIS");
        assertEquals(true, secondResult.get("matched"));
        assertEquals("APPROVED_STD_002", secondResult.get("standard_code"));
    }

    @Test
    @DisplayName("approvePendingMapping - 不存在的 queueId 抛出异常")
    void approvePendingMapping_notFound() {
        Map<String, Object> approveRequest = new HashMap<>();
        approveRequest.put("standard_code", "STD");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.approvePendingMapping("NON_EXISTENT_ID", approveRequest));
        assertTrue(ex.getMessage().contains("queue entry not found"));
    }

    @Test
    @DisplayName("approvePendingMapping - 非 PENDING_MAPPING 状态不可审批")
    void approvePendingMapping_notPendingStatus() {
        // 先产生并审批一条记录
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "APPROVE_003");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = terminologyService.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        Map<String, Object> approveRequest = new HashMap<>();
        approveRequest.put("standard_code", "APPROVED_STD_003");
        terminologyService.approvePendingMapping(queueId, approveRequest);

        // 再次审批同一 queueId 应失败（状态已变为 APPROVED）
        Map<String, Object> secondApprove = new HashMap<>();
        secondApprove.put("standard_code", "ANOTHER_STD");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.approvePendingMapping(queueId, secondApprove));
        assertTrue(ex.getMessage().contains("not in PENDING_MAPPING status"));
    }

    @Test
    @DisplayName("approvePendingMapping - 缺少 standard_code 时抛出异常")
    void approvePendingMapping_missingStandardCode() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "APPROVE_004");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = terminologyService.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        Map<String, Object> approveRequest = new HashMap<>();
        // 不提供 standard_code，且队列中也无 proposed_standard_code

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.approvePendingMapping(queueId, approveRequest));
        assertTrue(ex.getMessage().contains("standard_code is required"));
    }

    @Test
    @DisplayName("approvePendingMapping - 持久化层启用时调用 updateUnmappedQueueStatus")
    void approvePendingMapping_persistenceCalled() {
        when(persistenceService.enabled()).thenReturn(true);
        doNothing().when(persistenceService).updateUnmappedQueueStatus(anyString(), anyString(), anyString(), anyString(), any());

        // 重新创建 service 让 enabled() 返回 true
        TerminologyService svc = new TerminologyService(persistenceService);

        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "APPROVE_PERSIST");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = svc.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        Map<String, Object> approveRequest = new HashMap<>();
        approveRequest.put("standard_code", "PERSIST_STD");
        approveRequest.put("reviewed_by", "admin");

        svc.approvePendingMapping(queueId, approveRequest);

        verify(persistenceService).updateUnmappedQueueStatus(
                eq(queueId), eq("default"), eq("APPROVED"), eq("admin"), any());
    }

    @Test
    @DisplayName("approvePendingMapping - reviewed_by 默认为 SYSTEM")
    void approvePendingMapping_defaultReviewer() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "APPROVE_005");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = terminologyService.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        Map<String, Object> approveRequest = new HashMap<>();
        approveRequest.put("standard_code", "STD_005");
        // 不提供 reviewed_by

        Map<String, Object> result = terminologyService.approvePendingMapping(queueId, approveRequest);

        assertEquals("SYSTEM", result.get("reviewed_by"));
    }

    // =========================================================================
    // rejectPendingMapping()
    // =========================================================================

    @Test
    @DisplayName("rejectPendingMapping - 驳回待处理映射成功")
    void rejectPendingMapping_success() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "REJECT_001");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = terminologyService.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        Map<String, Object> rejectRequest = new HashMap<>();
        rejectRequest.put("reviewed_by", "admin");
        rejectRequest.put("review_comment", "不符合标准");

        Map<String, Object> result = terminologyService.rejectPendingMapping(queueId, rejectRequest);

        assertEquals("REJECTED", result.get("governance_status"));
        assertEquals("admin", result.get("reviewed_by"));
        assertEquals("不符合标准", result.get("review_comment"));
        assertNotNull(result.get("reviewed_time"));
    }

    @Test
    @DisplayName("rejectPendingMapping - 驳回后映射不可被 normalize 命中")
    void rejectPendingMapping_mappingNotCreated() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "REJECT_002");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = terminologyService.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        Map<String, Object> rejectRequest = new HashMap<>();
        rejectRequest.put("reviewed_by", "admin");
        terminologyService.rejectPendingMapping(queueId, rejectRequest);

        // 驳回后 getMapping 应抛出异常
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.getMapping("HIS", "REJECT_002", "DIAGNOSIS"));
        assertTrue(ex.getMessage().contains("mapping not found"));
    }

    @Test
    @DisplayName("rejectPendingMapping - 不存在的 queueId 抛出异常")
    void rejectPendingMapping_notFound() {
        Map<String, Object> rejectRequest = new HashMap<>();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.rejectPendingMapping("NON_EXISTENT_ID", rejectRequest));
        assertTrue(ex.getMessage().contains("queue entry not found"));
    }

    @Test
    @DisplayName("rejectPendingMapping - 非 PENDING_MAPPING 状态不可驳回")
    void rejectPendingMapping_notPendingStatus() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "REJECT_003");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = terminologyService.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        // 先审批
        Map<String, Object> approveRequest = new HashMap<>();
        approveRequest.put("standard_code", "STD_REJECT_003");
        terminologyService.approvePendingMapping(queueId, approveRequest);

        // 再驳回同一 queueId 应失败
        Map<String, Object> rejectRequest = new HashMap<>();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> terminologyService.rejectPendingMapping(queueId, rejectRequest));
        assertTrue(ex.getMessage().contains("not in PENDING_MAPPING status"));
    }

    @Test
    @DisplayName("rejectPendingMapping - 持久化层启用时调用 updateUnmappedQueueStatus 和 deleteUnmappedQueueEntry")
    void rejectPendingMapping_persistenceCalled() {
        when(persistenceService.enabled()).thenReturn(true);
        doNothing().when(persistenceService).updateUnmappedQueueStatus(anyString(), anyString(), anyString(), anyString(), any());
        doNothing().when(persistenceService).deleteUnmappedQueueEntry(anyString(), anyString());

        TerminologyService svc = new TerminologyService(persistenceService);

        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "REJECT_PERSIST");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = svc.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        Map<String, Object> rejectRequest = new HashMap<>();
        rejectRequest.put("reviewed_by", "admin");
        svc.rejectPendingMapping(queueId, rejectRequest);

        verify(persistenceService).updateUnmappedQueueStatus(
                eq(queueId), eq("default"), eq("REJECTED"), eq("admin"), any());
        verify(persistenceService).deleteUnmappedQueueEntry(eq(queueId), eq("default"));
    }

    @Test
    @DisplayName("rejectPendingMapping - reviewed_by 默认为 SYSTEM")
    void rejectPendingMapping_defaultReviewer() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "REJECT_004");
        request.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalizeResult = terminologyService.normalize(request);
        String queueId = (String) normalizeResult.get("queue_id");

        Map<String, Object> rejectRequest = new HashMap<>();
        // 不提供 reviewed_by

        Map<String, Object> result = terminologyService.rejectPendingMapping(queueId, rejectRequest);

        assertEquals("SYSTEM", result.get("reviewed_by"));
    }

    // =========================================================================
    // 治理队列重复出现计数
    // =========================================================================

    @Test
    @DisplayName("normalize - 同一未映射编码多次出现时 occurrence_count 递增")
    void normalize_occurrenceCountIncrement() {
        Map<String, Object> request = new HashMap<>();
        request.put("source_system", "HIS");
        request.put("source_code", "REPEAT_001");
        request.put("concept_type", "DIAGNOSIS");

        terminologyService.normalize(request);
        terminologyService.normalize(request);

        Map<String, String> filters = new HashMap<>();
        filters.put("sourceSystem", "HIS");
        List<Map<String, Object>> pending = terminologyService.listPendingMappings(filters);

        Map<String, Object> entry = pending.stream()
                .filter(e -> "REPEAT_001".equals(e.get("source_code")))
                .findFirst()
                .orElse(null);

        assertNotNull(entry);
        assertEquals(2, entry.get("occurrence_count"));
    }
}
