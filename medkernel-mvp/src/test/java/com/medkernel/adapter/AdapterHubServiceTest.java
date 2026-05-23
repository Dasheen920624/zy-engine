package com.medkernel.adapter;

import com.medkernel.dto.AdapterDefinitionImportRequest;
import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("适配器中心服务测试")
class AdapterHubServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private AdapterExecutionLogService executionLogService;

    private AdapterDefinitionSeeder definitionSeeder;
    private List<AdapterMockDataProvider> mockDataProviders;
    private AdapterHubService adapterHubService;

    @BeforeEach
    void setUp() {
        definitionSeeder = new AdapterDefinitionSeeder();
        mockDataProviders = new ArrayList<AdapterMockDataProvider>();
        adapterHubService = new AdapterHubService(
                persistenceService, executionLogService, mockDataProviders, definitionSeeder);
    }

    // ========== 适配器注册（importDefinitions） ==========

    @Test
    @DisplayName("导入适配器定义 - 成功注册单个定义")
    void importDefinitions_shouldRegisterSingleDefinition() {
        AdapterDefinitionImportRequest request = buildImportRequest(
                buildItem("TEST_ADAPTER", "测试适配器", "REST", "SYS",
                        "QUERY_TEST", "查询测试", "测试描述",
                        java.util.Arrays.asList("field1", "field2"), null));

        List<Map<String, Object>> result = adapterHubService.importDefinitions(request, "default", "ZYHOSPITAL");

        assertEquals(1, result.size());
        assertEquals("TEST_ADAPTER", result.get(0).get("adapter_code"));
        assertEquals("QUERY_TEST", result.get(0).get("query_code"));
        assertEquals("REST", result.get(0).get("adapter_type"));
    }

    @Test
    @DisplayName("导入适配器定义 - 成功注册多个定义")
    void importDefinitions_shouldRegisterMultipleDefinitions() {
        AdapterDefinitionImportRequest request = buildImportRequest(
                buildItem("ADAPTER_A", "适配器A", "REST", "SYS_A",
                        "QUERY_A", "查询A", "描述A", null, null),
                buildItem("ADAPTER_B", "适配器B", "SQL", "SYS_B",
                        "QUERY_B", "查询B", "描述B", null, null));

        List<Map<String, Object>> result = adapterHubService.importDefinitions(request, "default", "ZYHOSPITAL");

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("导入适配器定义 - 空列表抛异常")
    void importDefinitions_shouldThrowWhenEmpty() {
        AdapterDefinitionImportRequest request = new AdapterDefinitionImportRequest();
        request.setDefinitions(Collections.<AdapterDefinitionImportRequest.AdapterDefinitionItem>emptyList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adapterHubService.importDefinitions(request, "default", "ZYHOSPITAL"));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("导入适配器定义 - 缺少adapter_code抛异常")
    void importDefinitions_shouldThrowWhenAdapterCodeMissing() {
        AdapterDefinitionImportRequest.AdapterDefinitionItem item =
                new AdapterDefinitionImportRequest.AdapterDefinitionItem();
        item.setQuery_code("QUERY_X");

        AdapterDefinitionImportRequest request = buildImportRequest(item);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adapterHubService.importDefinitions(request, "default", "ZYHOSPITAL"));
        assertTrue(ex.getMessage().contains("adapter_code is required"));
    }

    @Test
    @DisplayName("导入适配器定义 - 缺少query_code抛异常")
    void importDefinitions_shouldThrowWhenQueryCodeMissing() {
        AdapterDefinitionImportRequest.AdapterDefinitionItem item =
                new AdapterDefinitionImportRequest.AdapterDefinitionItem();
        item.setAdapter_code("ADAPTER_X");

        AdapterDefinitionImportRequest request = buildImportRequest(item);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adapterHubService.importDefinitions(request, "default", "ZYHOSPITAL"));
        assertTrue(ex.getMessage().contains("query_code is required"));
    }

    @Test
    @DisplayName("导入适配器定义 - 带sample_rows成功注册")
    void importDefinitions_shouldRegisterWithSampleRows() {
        List<Map<String, Object>> sampleRows = new ArrayList<Map<String, Object>>();
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("field1", "value1");
        sampleRows.add(row);

        AdapterDefinitionImportRequest request = buildImportRequest(
                buildItem("ROW_ADAPTER", "行数据适配器", "REST", "SYS",
                        "QUERY_ROWS", "查询行数据", "描述",
                        java.util.Arrays.asList("field1"), sampleRows));

        List<Map<String, Object>> result = adapterHubService.importDefinitions(request, "default", "ZYHOSPITAL");

        assertEquals(1, result.size());
        assertTrue((Boolean) result.get(0).get("has_sample_rows"));
    }

    // ========== 适配器查询 ==========

    @Test
    @DisplayName("查询适配器 - 查询已注册定义返回SUCCESS")
    void query_shouldReturnSuccessForRegisteredDefinition() {
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(),
                any(), any(), any(), any());

        // 先导入
        AdapterDefinitionImportRequest request = buildImportRequest(
                buildItem("Q_ADAPTER", "查询适配器", "REST", "SYS",
                        "QUERY_Q", "查询Q", "描述", null, null));
        adapterHubService.importDefinitions(request, "default", "ZYHOSPITAL");

        // 再查询
        Map<String, Object> queryRequest = new LinkedHashMap<String, Object>();
        queryRequest.put("adapter_code", "Q_ADAPTER");
        queryRequest.put("query_code", "QUERY_Q");
        Map<String, Object> result = adapterHubService.query(queryRequest, "default", "ZYHOSPITAL");

        assertEquals("SUCCESS", result.get("status"));
        assertEquals("Q_ADAPTER", result.get("adapter_code"));
        assertEquals("QUERY_Q", result.get("query_code"));
        assertTrue((Boolean) result.get("mock"));
    }

    @Test
    @DisplayName("查询适配器 - 查询未注册定义返回UNSUPPORTED")
    void query_shouldReturnUnsupportedForUnknownDefinition() {
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(),
                any(), any(), any(), any());

        Map<String, Object> queryRequest = new LinkedHashMap<String, Object>();
        queryRequest.put("adapter_code", "UNKNOWN_ADAPTER");
        queryRequest.put("query_code", "UNKNOWN_QUERY");
        Map<String, Object> result = adapterHubService.query(queryRequest, "default", "ZYHOSPITAL");

        assertEquals("UNSUPPORTED", result.get("status"));
        assertNotNull(result.get("supported_queries"));
    }

    @Test
    @DisplayName("查询适配器 - 缺少adapter_code抛异常")
    void query_shouldThrowWhenAdapterCodeMissing() {
        Map<String, Object> queryRequest = new LinkedHashMap<String, Object>();
        queryRequest.put("query_code", "QUERY_X");

        assertThrows(IllegalArgumentException.class,
                () -> adapterHubService.query(queryRequest, "default", "ZYHOSPITAL"));
    }

    @Test
    @DisplayName("查询适配器 - 缺少query_code抛异常")
    void query_shouldThrowWhenQueryCodeMissing() {
        Map<String, Object> queryRequest = new LinkedHashMap<String, Object>();
        queryRequest.put("adapter_code", "ADAPTER_X");

        assertThrows(IllegalArgumentException.class,
                () -> adapterHubService.query(queryRequest, "default", "ZYHOSPITAL"));
    }

    @Test
    @DisplayName("查询适配器 - 内置种子定义可查询")
    void query_shouldReturnSuccessForBuiltInDefinition() {
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(),
                any(), any(), any(), any());

        Map<String, Object> queryRequest = new LinkedHashMap<String, Object>();
        queryRequest.put("adapter_code", "ECG_ADAPTER");
        queryRequest.put("query_code", "QUERY_ECG_REPORT");
        Map<String, Object> result = adapterHubService.query(queryRequest, "default", "ZYHOSPITAL");

        assertEquals("SUCCESS", result.get("status"));
        assertEquals("ECG_ADAPTER", result.get("adapter_code"));
    }

    // ========== 适配器列表 ==========

    @Test
    @DisplayName("列出适配器定义 - 返回指定租户和机构的定义")
    void listDefinitions_shouldReturnDefinitionsForTenantAndHospital() {
        AdapterDefinitionImportRequest request = buildImportRequest(
                buildItem("LIST_A", "列表适配器A", "REST", "SYS",
                        "QUERY_LA", "查询LA", "描述", null, null),
                buildItem("LIST_B", "列表适配器B", "SQL", "SYS",
                        "QUERY_LB", "查询LB", "描述", null, null));
        adapterHubService.importDefinitions(request, "default", "ZYHOSPITAL");

        List<Map<String, Object>> result = adapterHubService.listDefinitions("default", "ZYHOSPITAL");

        // 至少包含导入的2个 + 内置种子定义
        assertTrue(result.size() >= 2);
    }

    @Test
    @DisplayName("列出适配器定义 - 不同租户隔离")
    void listDefinitions_shouldIsolateByTenant() {
        AdapterDefinitionImportRequest request = buildImportRequest(
                buildItem("ISO_ADAPTER", "隔离适配器", "REST", "SYS",
                        "QUERY_ISO", "查询隔离", "描述", null, null));
        adapterHubService.importDefinitions(request, "tenant_other", "HOSP_OTHER");

        List<Map<String, Object>> result = adapterHubService.listDefinitions("tenant_other", "HOSP_OTHER");

        assertEquals(1, result.size());
        assertEquals("ISO_ADAPTER", result.get(0).get("adapter_code"));
    }

    // ========== 适配器绑定到组织 ==========

    @Test
    @DisplayName("适配器绑定组织 - 同一适配器可绑定到不同组织")
    void adapterBinding_shouldRegisterSameAdapterForDifferentOrg() {
        AdapterDefinitionImportRequest request1 = buildImportRequest(
                buildItem("ORG_ADAPTER", "组织适配器", "REST", "SYS",
                        "QUERY_ORG", "查询组织", "描述", null, null));
        adapterHubService.importDefinitions(request1, "default", "ZYHOSPITAL");

        AdapterDefinitionImportRequest request2 = buildImportRequest(
                buildItem("ORG_ADAPTER", "组织适配器", "REST", "SYS",
                        "QUERY_ORG", "查询组织", "描述", null, null));
        adapterHubService.importDefinitions(request2, "tenant2", "HOSP2");

        List<Map<String, Object>> list1 = adapterHubService.listDefinitions("default", "ZYHOSPITAL");
        List<Map<String, Object>> list2 = adapterHubService.listDefinitions("tenant2", "HOSP2");

        boolean foundInOrg1 = false;
        for (Map<String, Object> def : list1) {
            if ("ORG_ADAPTER".equals(def.get("adapter_code"))) {
                foundInOrg1 = true;
                break;
            }
        }
        boolean foundInOrg2 = false;
        for (Map<String, Object> def : list2) {
            if ("ORG_ADAPTER".equals(def.get("adapter_code"))) {
                foundInOrg2 = true;
                break;
            }
        }
        assertTrue(foundInOrg1, "适配器应在组织1中找到");
        assertTrue(foundInOrg2, "适配器应在组织2中找到");
    }

    // ========== 触发点管理 ==========

    @Test
    @DisplayName("获取适配器定义 - 成功获取已注册定义")
    void getDefinition_shouldReturnRegisteredDefinition() {
        AdapterDefinitionImportRequest request = buildImportRequest(
                buildItem("GET_ADAPTER", "获取适配器", "REST", "SYS",
                        "QUERY_GET", "查询获取", "描述",
                        java.util.Arrays.asList("f1"), null));
        adapterHubService.importDefinitions(request, "default", "ZYHOSPITAL");

        Map<String, Object> result = adapterHubService.getDefinition(
                "GET_ADAPTER", "QUERY_GET", "default", "ZYHOSPITAL");

        assertEquals("GET_ADAPTER", result.get("adapter_code"));
        assertEquals("QUERY_GET", result.get("query_code"));
    }

    @Test
    @DisplayName("获取适配器定义 - 未找到抛异常")
    void getDefinition_shouldThrowWhenNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> adapterHubService.getDefinition("NOT_EXIST", "NOT_EXIST", "default", "ZYHOSPITAL"));
        assertTrue(ex.getMessage().contains("not found"));
    }

    // ========== Mock数据提供者 ==========

    @Test
    @DisplayName("查询适配器 - Mock数据提供者返回数据")
    void query_shouldUseMockDataProvider() {
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(),
                any(), any(), any(), any());

        AdapterMockDataProvider provider = mock(AdapterMockDataProvider.class);
        List<Map<String, Object>> mockRows = new ArrayList<Map<String, Object>>();
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("result_field", "mock_value");
        mockRows.add(row);
        org.mockito.Mockito.when(provider.supports("MOCK_ADAPTER", "QUERY_MOCK")).thenReturn(true);
        org.mockito.Mockito.when(provider.provideRows("MOCK_ADAPTER", "QUERY_MOCK", any())).thenReturn(mockRows);

        mockDataProviders.add(provider);

        AdapterDefinitionImportRequest request = buildImportRequest(
                buildItem("MOCK_ADAPTER", "Mock适配器", "REST", "SYS",
                        "QUERY_MOCK", "查询Mock", "描述", null, null));
        adapterHubService.importDefinitions(request, "default", "ZYHOSPITAL");

        Map<String, Object> queryRequest = new LinkedHashMap<String, Object>();
        queryRequest.put("adapter_code", "MOCK_ADAPTER");
        queryRequest.put("query_code", "QUERY_MOCK");
        Map<String, Object> result = adapterHubService.query(queryRequest, "default", "ZYHOSPITAL");

        assertEquals("SUCCESS", result.get("status"));
        assertEquals(1, result.get("row_count"));
    }

    // ========== 审计日志 ==========

    @Test
    @DisplayName("查询适配器 - 审计日志写入失败不影响查询")
    void query_shouldNotFailWhenAuditLogPersistenceFails() {
        doThrow(new RuntimeException("DB down")).when(persistenceService)
                .saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        Map<String, Object> queryRequest = new LinkedHashMap<String, Object>();
        queryRequest.put("adapter_code", "ECG_ADAPTER");
        queryRequest.put("query_code", "QUERY_ECG_REPORT");

        // 不应抛异常
        Map<String, Object> result = adapterHubService.query(queryRequest, "default", "ZYHOSPITAL");
        assertEquals("SUCCESS", result.get("status"));
    }

    // ========== 辅助方法 ==========

    private AdapterDefinitionImportRequest buildImportRequest(AdapterDefinitionImportRequest.AdapterDefinitionItem... items) {
        AdapterDefinitionImportRequest request = new AdapterDefinitionImportRequest();
        request.setDefinitions(java.util.Arrays.asList(items));
        return request;
    }

    private AdapterDefinitionImportRequest.AdapterDefinitionItem buildItem(
            String adapterCode, String adapterName, String adapterType, String sourceSystem,
            String queryCode, String queryName, String description,
            List<String> schema, List<Map<String, Object>> sampleRows) {
        AdapterDefinitionImportRequest.AdapterDefinitionItem item =
                new AdapterDefinitionImportRequest.AdapterDefinitionItem();
        item.setAdapter_code(adapterCode);
        item.setAdapter_name(adapterName);
        item.setAdapter_type(adapterType);
        item.setSource_system(sourceSystem);
        item.setQuery_code(queryCode);
        item.setQuery_name(queryName);
        item.setDescription(description);
        item.setSchema(schema);
        item.setSample_rows(sampleRows);
        return item;
    }
}
