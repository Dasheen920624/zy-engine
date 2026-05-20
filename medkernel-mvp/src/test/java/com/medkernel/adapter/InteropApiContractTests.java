package com.medkernel.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 互联互通标准适配器 REST API 契约测试
 * 覆盖 /api/interop 下所有端点的正确性、参数校验及 traceId 传播
 *
 * @see InteropController
 * @see InteropAdapterService
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InteropApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ==================== GET /api/interop/adapters ====================

    @Test
    void listInteropAdaptersReturnsAllBuiltIn() throws Exception {
        Map<String, Object> result = invokeGet("/api/interop/adapters");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertTrue(data.size() >= 9, "expected at least 9 interop adapters, got " + data.size());

        // 验证每个 adapter 包含必要字段
        for (Map<String, Object> adapter : data) {
            assertNotNull(adapter.get("adapter_code"), "adapter_code must not be null");
            assertNotNull(adapter.get("adapter_name"), "adapter_name must not be null");
            assertNotNull(adapter.get("adapter_type"), "adapter_type must not be null");
            assertNotNull(adapter.get("query_code"), "query_code must not be null");
            assertNotNull(adapter.get("source"), "source must not be null");
        }
    }

    @Test
    void listInteropAdaptersContainsHl7Adapter() throws Exception {
        Map<String, Object> result = invokeGet("/api/interop/adapters");
        List<Map<String, Object>> data = asListOfMap(result.get("data"));

        boolean found = false;
        for (Map<String, Object> adapter : data) {
            if ("HIS_HL7_ADAPTER".equals(adapter.get("adapter_code"))
                    && "QUERY_PATIENT_ADT".equals(adapter.get("query_code"))) {
                found = true;
                assertEquals("HL7", adapter.get("adapter_type"));
                assertEquals("HIS", adapter.get("source_system"));
                assertEquals("MLLP", adapter.get("protocol"));
                assertNotNull(adapter.get("hl7_message_type"), "HL7 adapter must have hl7_message_type");
                break;
            }
        }
        assertTrue(found, "HIS_HL7_ADAPTER.QUERY_PATIENT_ADT should be present");
    }

    @Test
    void listInteropAdaptersContainsFhirAdapter() throws Exception {
        Map<String, Object> result = invokeGet("/api/interop/adapters");
        List<Map<String, Object>> data = asListOfMap(result.get("data"));

        boolean found = false;
        for (Map<String, Object> adapter : data) {
            if ("HIS_FHIR_ADAPTER".equals(adapter.get("adapter_code"))
                    && "QUERY_PATIENT_RESOURCE".equals(adapter.get("query_code"))) {
                found = true;
                assertEquals("FHIR", adapter.get("adapter_type"));
                assertNotNull(adapter.get("fhir_resource_type"), "FHIR adapter must have fhir_resource_type");
                assertEquals("Patient", adapter.get("fhir_resource_type"));
                break;
            }
        }
        assertTrue(found, "HIS_FHIR_ADAPTER.QUERY_PATIENT_RESOURCE should be present");
    }

    @Test
    void listInteropAdaptersContainsDicomAdapter() throws Exception {
        Map<String, Object> result = invokeGet("/api/interop/adapters");
        List<Map<String, Object>> data = asListOfMap(result.get("data"));

        boolean found = false;
        for (Map<String, Object> adapter : data) {
            if ("PACS_DICOM_ADAPTER".equals(adapter.get("adapter_code"))
                    && "QUERY_CT_IMAGE".equals(adapter.get("query_code"))) {
                found = true;
                assertEquals("DICOM", adapter.get("adapter_type"));
                assertNotNull(adapter.get("dicom_sop_class"), "DICOM adapter must have dicom_sop_class");
                break;
            }
        }
        assertTrue(found, "PACS_DICOM_ADAPTER.QUERY_CT_IMAGE should be present");
    }

    // ==================== GET /api/interop/cds-hooks ====================

    @Test
    void listCdsHooksServicesReturnsBuiltIn() throws Exception {
        Map<String, Object> result = invokeGet("/api/interop/cds-hooks");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertTrue(data.size() >= 1, "expected at least 1 CDS Hooks service, got " + data.size());

        Map<String, Object> first = data.get(0);
        assertNotNull(first.get("hook_id"), "hook_id must not be null");
        assertNotNull(first.get("hook_type"), "hook_type must not be null");
        assertNotNull(first.get("service_id"), "service_id must not be null");
        assertNotNull(first.get("service_title"), "service_title must not be null");
    }

    @Test
    void listCdsHooksContainsAmiRiskAssessment() throws Exception {
        Map<String, Object> result = invokeGet("/api/interop/cds-hooks");
        List<Map<String, Object>> data = asListOfMap(result.get("data"));

        boolean found = false;
        for (Map<String, Object> service : data) {
            if ("HOOK_CDS_001".equals(service.get("hook_id"))) {
                found = true;
                assertEquals("order-sign", service.get("hook_type"));
                assertEquals("ami-risk-assessment", service.get("service_id"));
                break;
            }
        }
        assertTrue(found, "HOOK_CDS_001 ami-risk-assessment should be present");
    }

    // ==================== GET /api/interop/smart-apps ====================

    @Test
    void listSmartAppsReturnsBuiltIn() throws Exception {
        Map<String, Object> result = invokeGet("/api/interop/smart-apps");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertTrue(data.size() >= 1, "expected at least 1 SMART app, got " + data.size());

        Map<String, Object> first = data.get(0);
        assertNotNull(first.get("app_id"), "app_id must not be null");
        assertNotNull(first.get("app_name"), "app_name must not be null");
        assertNotNull(first.get("app_type"), "app_type must not be null");
        assertNotNull(first.get("client_id"), "client_id must not be null");
    }

    @Test
    void listSmartAppsContainsEcgApp() throws Exception {
        Map<String, Object> result = invokeGet("/api/interop/smart-apps");
        List<Map<String, Object>> data = asListOfMap(result.get("data"));

        boolean found = false;
        for (Map<String, Object> app : data) {
            if ("SMART_APP_001".equals(app.get("app_id"))) {
                found = true;
                assertEquals("EHR_LAUNCH", app.get("app_type"));
                assertEquals("ecg-analysis-app", app.get("client_id"));
                break;
            }
        }
        assertTrue(found, "SMART_APP_001 ecg-analysis-app should be present");
    }

    // ==================== POST /api/interop/query ====================

    @Test
    void queryHl7AdapterReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("adapter_code", "HIS_HL7_ADAPTER");
        body.put("query_code", "QUERY_PATIENT_ADT");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("patient_id", "P_TEST_001");
        body.put("params", params);

        Map<String, Object> result = invokePost("/api/interop/query", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("SUCCESS", data.get("status"));
        assertEquals("HIS_HL7_ADAPTER", data.get("adapter_code"));
        assertEquals("QUERY_PATIENT_ADT", data.get("query_code"));
        assertNotNull(data.get("trace_id"), "trace_id must be present");
        assertEquals(true, data.get("mock"));
        assertEquals("HL7", data.get("adapter_type"));
        assertNotNull(data.get("rows"), "rows must not be null");
    }

    @Test
    void queryFhirAdapterReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("adapter_code", "HIS_FHIR_ADAPTER");
        body.put("query_code", "QUERY_PATIENT_RESOURCE");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("patient_id", "P_TEST_002");
        body.put("params", params);

        Map<String, Object> result = invokePost("/api/interop/query", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("SUCCESS", data.get("status"));
        assertEquals("FHIR", data.get("adapter_type"));
        assertEquals("Patient", data.get("fhir_resource_type"));
    }

    @Test
    void queryCdaAdapterReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("adapter_code", "EMR_CDA_ADAPTER");
        body.put("query_code", "QUERY_DISCHARGE_SUMMARY");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("patient_id", "P_TEST_003");
        body.put("params", params);

        Map<String, Object> result = invokePost("/api/interop/query", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("SUCCESS", data.get("status"));
        assertEquals("CDA", data.get("adapter_type"));
    }

    @Test
    void queryDicomAdapterReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("adapter_code", "PACS_DICOM_ADAPTER");
        body.put("query_code", "QUERY_CT_IMAGE");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("patient_id", "P_TEST_004");
        body.put("params", params);

        Map<String, Object> result = invokePost("/api/interop/query", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("SUCCESS", data.get("status"));
        assertEquals("DICOM", data.get("adapter_type"));
        assertEquals("CTImageStorage", data.get("dicom_sop_class"));
    }

    @Test
    void queryWithMissingAdapterCodeReturnsError() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query_code", "QUERY_PATIENT_ADT");

        invokePostExpectingClientError("/api/interop/query", body);
    }

    @Test
    void queryWithMissingQueryCodeReturnsError() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("adapter_code", "HIS_HL7_ADAPTER");

        invokePostExpectingClientError("/api/interop/query", body);
    }

    @Test
    void queryUnsupportedAdapterReturnsUnsupportedStatus() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("adapter_code", "NON_EXISTENT_ADAPTER");
        body.put("query_code", "NON_EXISTENT_QUERY");

        Map<String, Object> result = invokePost("/api/interop/query", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true even for unsupported");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("UNSUPPORTED", data.get("status"));
        assertEquals("NON_EXISTENT_ADAPTER", data.get("adapter_code"));
        assertNotNull(data.get("supported_queries"), "supported_queries should be listed");
    }

    // ==================== POST /api/interop/cds-hooks ====================

    @Test
    void queryCdsHooksReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("hook_id", "HOOK_CDS_001");
        body.put("hook_type", "order-sign");

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("patient_id", "P_TEST_005");
        context.put("user_id", "U_TEST_001");
        body.put("context", context);

        Map<String, Object> result = invokePost("/api/interop/cds-hooks", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("SUCCESS", data.get("status"));
        assertEquals("HOOK_CDS_001", data.get("hook_id"));
        assertEquals("order-sign", data.get("hook_type"));
        assertNotNull(data.get("cards"), "cards must not be null");
        assertTrue(((List<?>) data.get("cards")).size() > 0, "should return at least 1 card");
    }

    @Test
    void queryCdsHooksWithMissingHookIdReturnsError() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("hook_type", "order-sign");

        invokePostExpectingClientError("/api/interop/cds-hooks", body);
    }

    // ==================== POST /api/interop/smart-apps ====================

    @Test
    void querySmartAppReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app_id", "SMART_APP_001");

        Map<String, Object> launchContext = new LinkedHashMap<>();
        launchContext.put("patient_id", "P_TEST_006");
        body.put("launch_context", launchContext);

        Map<String, Object> result = invokePost("/api/interop/smart-apps", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("SUCCESS", data.get("status"));
        assertEquals("SMART_APP_001", data.get("app_id"));
        assertEquals("EHR_LAUNCH", data.get("app_type"));
        assertNotNull(data.get("launch_url"), "launch_url must not be null");
    }

    @Test
    void querySmartAppWithMissingAppIdReturnsError() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("launch_context", new LinkedHashMap<>());

        invokePostExpectingClientError("/api/interop/smart-apps", body);
    }

    // ==================== traceId 传播 ====================

    @Test
    void interopQueryPropagatesTraceId() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("adapter_code", "HIS_HL7_ADAPTER");
        body.put("query_code", "QUERY_PATIENT_ADT");
        body.put("params", new LinkedHashMap<>());

        Map<String, Object> result = invokePost("/api/interop/query", body);
        Map<String, Object> data = asMap(result.get("data"));

        String traceId = (String) data.get("trace_id");
        assertNotNull(traceId, "trace_id must be present in response");
        assertTrue(traceId.length() > 0, "trace_id must not be empty");
    }

    @Test
    void cdsHooksQueryPropagatesTraceId() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("hook_id", "HOOK_CDS_001");
        body.put("hook_type", "order-sign");
        body.put("context", new LinkedHashMap<>());

        Map<String, Object> result = invokePost("/api/interop/cds-hooks", body);
        Map<String, Object> data = asMap(result.get("data"));

        String traceId = (String) data.get("trace_id");
        assertNotNull(traceId, "trace_id must be present in CDS Hooks response");
        assertTrue(traceId.length() > 0, "trace_id must not be empty");
    }

    @Test
    void smartAppQueryPropagatesTraceId() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app_id", "SMART_APP_001");
        body.put("launch_context", new LinkedHashMap<>());

        Map<String, Object> result = invokePost("/api/interop/smart-apps", body);
        Map<String, Object> data = asMap(result.get("data"));

        String traceId = (String) data.get("trace_id");
        assertNotNull(traceId, "trace_id must be present in SMART App response");
        assertTrue(traceId.length() > 0, "trace_id must not be empty");
    }

    // ==================== 标准覆盖度 ====================

    @Test
    void adaptersCoverAllStandards() throws Exception {
        Map<String, Object> result = invokeGet("/api/interop/adapters");
        List<Map<String, Object>> data = asListOfMap(result.get("data"));

        java.util.Set<String> adapterTypes = new java.util.HashSet<>();
        for (Map<String, Object> adapter : data) {
            adapterTypes.add((String) adapter.get("adapter_type"));
        }

        assertTrue(adapterTypes.contains("HL7"), "must cover HL7 standard");
        assertTrue(adapterTypes.contains("FHIR"), "must cover FHIR standard");
        assertTrue(adapterTypes.contains("CDA"), "must cover CDA standard");
        assertTrue(adapterTypes.contains("DICOM"), "must cover DICOM standard");
        assertTrue(adapterTypes.contains("IHE"), "must cover IHE standard");
        assertTrue(adapterTypes.contains("REST"), "must cover REST standard");
    }

    @Test
    void adaptersCoverAllSourceSystems() throws Exception {
        Map<String, Object> result = invokeGet("/api/interop/adapters");
        List<Map<String, Object>> data = asListOfMap(result.get("data"));

        java.util.Set<String> sourceSystems = new java.util.HashSet<>();
        for (Map<String, Object> adapter : data) {
            sourceSystems.add((String) adapter.get("source_system"));
        }

        assertTrue(sourceSystems.contains("HIS"), "must cover HIS");
        assertTrue(sourceSystems.contains("EMR"), "must cover EMR");
        assertTrue(sourceSystems.contains("LIS"), "must cover LIS");
        assertTrue(sourceSystems.contains("PACS"), "must cover PACS");
        assertTrue(sourceSystems.contains("INSURANCE"), "must cover INSURANCE");
        assertTrue(sourceSystems.contains("OA"), "must cover OA");
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> invokeGet(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .header("Authorization", testBearerToken()))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus(), "GET " + path + " unexpected status");
        return parse(result);
    }

    private Map<String, Object> invokePost(String path, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", testBearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsBytes(body)))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus(), "POST " + path + " unexpected status");
        return parse(result);
    }

    private Map<String, Object> invokePostExpectingClientError(String path, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", testBearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsBytes(body)))
                .andReturn();
        assertTrue(result.getResponse().getStatus() >= 400,
                "POST " + path + " expected 4xx but got " + result.getResponse().getStatus());
        return parse(result);
    }

    private String testBearerToken() {
        return "Bearer " + jwtTokenProvider.createToken(1L, 1L, "junit", "JUnit Contract Tester");
    }

    private Map<String, Object> parse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readValue(body, LinkedHashMap.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object value) {
        return value instanceof List ? (List<Object>) value : java.util.Collections.<Object>emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object value) {
        return value instanceof List ? (List<Map<String, Object>>) value : java.util.Collections.<Map<String, Object>>emptyList();
    }
}
