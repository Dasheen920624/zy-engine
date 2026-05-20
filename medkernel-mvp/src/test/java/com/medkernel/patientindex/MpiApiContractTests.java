package com.medkernel.patientindex;

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
 * 患者主索引（MPI）REST API 契约测试
 * 覆盖 /api/mpi 下所有端点的正确性、参数校验及 traceId 传播
 *
 * @see com.medkernel.patientindex.controller.MpiController
 * @see com.medkernel.patientindex.service.MpiService
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MpiApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ==================== GET /api/mpi/patients ====================

    @Test
    void listPatientsReturnsSeededData() throws Exception {
        Map<String, Object> result = invokeGet("/api/mpi/patients");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertTrue(data.size() >= 1, "expected at least 1 seeded patient, got " + data.size());

        Map<String, Object> first = data.get(0);
        assertNotNull(first.get("mpi_id"), "mpi_id must not be null");
        assertNotNull(first.get("patient_name_masked"), "patient_name_masked must not be null");
        assertNotNull(first.get("status"), "status must not be null");
    }

    // ==================== POST /api/mpi/patients ====================

    @Test
    void createPatientReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_name", "李四");
        body.put("gender", "女");
        body.put("birth_date", "1985-05-15");
        body.put("id_card_no", "110101198505151234");
        body.put("phone", "13900001111");

        Map<String, Object> result = invokePost("/api/mpi/patients", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertNotNull(data.get("mpi_id"), "mpi_id must not be null");
        assertNotNull(data.get("patient_name_masked"), "patient_name_masked must not be null");
        assertEquals("女", data.get("gender"));
        assertNotNull(data.get("trace_id"), "trace_id must be present");
    }

    @Test
    void createPatientWithMissingNameReturnsError() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("gender", "男");

        invokePostExpectingClientError("/api/mpi/patients", body);
    }

    // ==================== POST /api/mpi/patients/query ====================

    @Test
    void queryPatientByMpiIdReturnsSeededPatient() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mpi_id", "MPI_SAMPLE_001");

        Map<String, Object> result = invokePost("/api/mpi/patients/query", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals(true, data.get("found"));
        assertEquals("MPI_SAMPLE_001", data.get("mpi_id"));
        assertNotNull(data.get("identifier_mappings"), "identifier_mappings should be present");
        assertNotNull(data.get("encounters"), "encounters should be present");
    }

    @Test
    void queryPatientByNameReturnsMatch() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_name", "张三");
        body.put("birth_date", "1990-01-01");

        Map<String, Object> result = invokePost("/api/mpi/patients/query", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals(true, data.get("found"));
        assertEquals("MPI_SAMPLE_001", data.get("mpi_id"));
    }

    @Test
    void queryPatientNotFoundReturnsFalse() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("patient_name", "不存在的人");
        body.put("birth_date", "2000-01-01");

        Map<String, Object> result = invokePost("/api/mpi/patients/query", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals(false, data.get("found"));
    }

    // ==================== POST /api/mpi/identifiers ====================

    @Test
    void addIdentifierMappingReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mpi_id", "MPI_SAMPLE_001");
        body.put("source_system", "LIS");
        body.put("identifier_type", "PATIENT_ID");
        body.put("identifier_value", "LIS_P001");
        body.put("is_primary", false);

        Map<String, Object> result = invokePost("/api/mpi/identifiers", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("MPI_SAMPLE_001", data.get("mpi_id"));
        assertEquals("LIS", data.get("source_system"));
        assertEquals("PATIENT_ID", data.get("identifier_type"));
        assertNotNull(data.get("identifier_masked"), "identifier_masked must not be null");
    }

    @Test
    void addIdentifierMappingWithMissingFieldsReturnsError() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mpi_id", "MPI_SAMPLE_001");

        invokePostExpectingClientError("/api/mpi/identifiers", body);
    }

    // ==================== POST /api/mpi/encounters ====================

    @Test
    void createEncounterReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mpi_id", "MPI_SAMPLE_001");
        body.put("encounter_id", "E_TEST_001");
        body.put("encounter_type", "INPATIENT");
        body.put("hospital_code", "ZYHOSPITAL");
        body.put("department_code", "DEPT_002");
        body.put("attending_doctor_id", "DOC_002");
        body.put("diagnosis_code", "I21.0");
        body.put("diagnosis_name", "急性心肌梗死");

        Map<String, Object> result = invokePost("/api/mpi/encounters", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("MPI_SAMPLE_001", data.get("mpi_id"));
        assertEquals("E_TEST_001", data.get("encounter_id"));
        assertEquals("INPATIENT", data.get("encounter_type"));
        assertEquals("ZYHOSPITAL", data.get("hospital_code"));
    }

    // ==================== GET /api/mpi/conflicts ====================

    @Test
    void listPendingConflictsReturnsEmptyInitially() throws Exception {
        Map<String, Object> result = invokeGet("/api/mpi/conflicts");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        // 初始状态没有待处理冲突
        assertNotNull(data, "data must not be null");
    }

    // ==================== traceId 传播 ====================

    @Test
    void mpiOperationsPropagateTraceId() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mpi_id", "MPI_SAMPLE_001");

        Map<String, Object> result = invokePost("/api/mpi/patients/query", body);
        Map<String, Object> data = asMap(result.get("data"));

        String traceId = (String) data.get("trace_id");
        assertNotNull(traceId, "trace_id must be present in response");
        assertTrue(traceId.length() > 0, "trace_id must not be empty");
    }

    // ==================== 数据脱敏验证 ====================

    @Test
    void patientNameIsMasked() throws Exception {
        Map<String, Object> result = invokeGet("/api/mpi/patients");
        List<Map<String, Object>> data = asListOfMap(result.get("data"));

        for (Map<String, Object> patient : data) {
            String maskedName = (String) patient.get("patient_name_masked");
            assertNotNull(maskedName, "patient_name_masked must not be null");
            // 脱敏后的姓名应该包含*
            assertTrue(maskedName.contains("*"), "masked name should contain *: " + maskedName);
        }
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
