package com.medkernel.patient;

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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

    @Test
    void registerPatientIdentityAcceptsAndReturnsSnakeCase() throws Exception {
        String suffix = uniqueSuffix();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenant_id", "TENANT_SNAKE");
        body.put("platform_patient_id", "P-SNAKE-" + suffix);
        body.put("identity_type", "HIS_PATIENT_ID");
        body.put("external_id", "HIS-P-" + suffix);
        body.put("source_system", "HIS");

        Map<String, Object> response = postOk("/api/v1/mpi/patient-identities", body);

        assertNotNull(response.get("id"));
        assertEquals(body.get("tenant_id"), response.get("tenant_id"));
        assertEquals(body.get("platform_patient_id"), response.get("platform_patient_id"));
        assertEquals(body.get("identity_type"), response.get("identity_type"));
        assertEquals(body.get("external_id"), response.get("external_id"));
        assertEquals(body.get("source_system"), response.get("source_system"));
        assertFalse(response.containsKey("platformPatientId"));
    }

    @Test
    void batchRegisterPatientIdentitiesUsesSnakeCasePayloadAndResult() throws Exception {
        String suffix = uniqueSuffix();
        Map<String, Object> id1 = new LinkedHashMap<>();
        id1.put("identity_type", "HIS_PATIENT_ID");
        id1.put("external_id", "HIS-B-" + suffix);
        id1.put("source_system", "HIS");
        Map<String, Object> id2 = new LinkedHashMap<>();
        id2.put("identity_type", "EMR_PATIENT_ID");
        id2.put("external_id", "EMR-B-" + suffix);
        id2.put("source_system", "EMR");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenant_id", "TENANT_SNAKE");
        body.put("platform_patient_id", "P-BATCH-" + suffix);
        body.put("identities", Arrays.asList(id1, id2));

        Map<String, Object> response = postOk("/api/v1/mpi/patient-identities/batch", body);

        assertEquals(2, ((Number) response.get("registered_count")).intValue());
        assertEquals(body.get("platform_patient_id"), response.get("platform_patient_id"));
        assertFalse(response.containsKey("registeredCount"));
    }

    @Test
    void findPatientByExternalIdAcceptsSnakeCaseQueryParams() throws Exception {
        String suffix = uniqueSuffix();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenant_id", "TENANT_SNAKE");
        body.put("platform_patient_id", "P-LOOKUP-" + suffix);
        body.put("identity_type", "HIS_PATIENT_ID");
        body.put("external_id", "HIS-L-" + suffix);
        body.put("source_system", "HIS");
        postOk("/api/v1/mpi/patient-identities", body);

        Map<String, Object> response = getOk("/api/v1/mpi/patient-identities/external"
                + "?tenant_id=TENANT_SNAKE"
                + "&identity_type=HIS_PATIENT_ID"
                + "&source_system=HIS"
                + "&external_id=HIS-L-" + suffix);

        assertEquals(body.get("platform_patient_id"), response.get("platform_patient_id"));
        assertFalse(response.containsKey("platformPatientId"));
    }

    @Test
    void registerVisitIdentityAcceptsAndReturnsSnakeCase() throws Exception {
        String suffix = uniqueSuffix();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenant_id", "TENANT_SNAKE");
        body.put("platform_visit_id", "V-SNAKE-" + suffix);
        body.put("platform_patient_id", "P-SNAKE-" + suffix);
        body.put("visit_type", "OUTPATIENT");
        body.put("identity_type", "HIS_VISIT_ID");
        body.put("external_id", "HIS-V-" + suffix);
        body.put("source_system", "HIS");
        body.put("visit_date", "2026-05-22");
        body.put("department_code", "CARD");

        Map<String, Object> response = postOk("/api/v1/mpi/visit-identities", body);

        assertNotNull(response.get("id"));
        assertEquals(body.get("platform_visit_id"), response.get("platform_visit_id"));
        assertEquals(body.get("platform_patient_id"), response.get("platform_patient_id"));
        assertEquals(body.get("visit_type"), response.get("visit_type"));
        assertEquals(body.get("department_code"), response.get("department_code"));
        assertFalse(response.containsKey("platformVisitId"));
    }

    @Test
    void verifyAndMergePatientIdentitiesAcceptSnakeCasePayloads() throws Exception {
        String suffix = uniqueSuffix();
        Long sourceId = createPatientIdentity("P-MERGE-SRC-" + suffix, "SRC-" + suffix);
        Long targetId = createPatientIdentity("P-MERGE-DST-" + suffix, "DST-" + suffix);

        Map<String, Object> verifyBody = new LinkedHashMap<>();
        verifyBody.put("verified_by", "platform-admin");
        postOkNoBody("/api/v1/mpi/patient-identities/" + sourceId + "/verify", verifyBody);

        Map<String, Object> mergeBody = new LinkedHashMap<>();
        mergeBody.put("source_id", sourceId);
        mergeBody.put("target_id", targetId);
        mergeBody.put("merged_by", "platform-admin");
        postOkNoBody("/api/v1/mpi/patient-identities/merge", mergeBody);
    }

    private Long createPatientIdentity(String platformPatientId, String externalId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenant_id", "TENANT_SNAKE");
        body.put("platform_patient_id", platformPatientId);
        body.put("identity_type", "HIS_PATIENT_ID");
        body.put("external_id", externalId);
        body.put("source_system", "HIS");
        Map<String, Object> response = postOk("/api/v1/mpi/patient-identities", body);
        return ((Number) response.get("id")).longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postOk(String url, Map<String, Object> body) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token())
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Map.class);
    }

    private void postOkNoBody(String url, Map<String, Object> body) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token())
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOk(String url) throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + token())
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        assertEquals(200, mvcResult.getResponse().getStatus(), mvcResult.getResponse().getContentAsString());
        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Map.class);
    }

    private String token() {
        return jwtTokenProvider.createToken(1L, 1001L, "admin", "管理员");
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime());
    }
}
