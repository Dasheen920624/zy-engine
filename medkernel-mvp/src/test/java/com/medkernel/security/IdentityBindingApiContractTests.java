package com.medkernel.security;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 多身份源绑定、合并和解绑 REST API 契约测试
 * 覆盖 /api/security/identity 下所有端点的正确性、参数校验
 *
 * @see com.medkernel.security.IdentityBindingController
 * @see com.medkernel.security.IdentityBindingService
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityBindingApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ==================== POST /api/security/identity/bindings ====================

    @Test
    void bindIdentityReturnsSuccess() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("tenant_id", 1L);
        request.put("user_id", 1001L);
        request.put("provider_id", 4001L);
        request.put("external_subject", "TEST001");
        request.put("external_org_code", "HOSPITAL");
        request.put("external_display_name", "测试用户");
        request.put("created_by", "test");

        Map<String, Object> result = invokePost("/api/security/identity/bindings", request);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Long bindingId = ((Number) result.get("data")).longValue();
        assertNotNull(bindingId, "binding_id should not be null");
    }

    @Test
    void bindIdentityWithDuplicateExternalAccountReturnsError() throws Exception {
        // 第一次绑定
        Map<String, Object> request1 = new LinkedHashMap<>();
        request1.put("tenant_id", 1L);
        request1.put("user_id", 1001L);
        request1.put("provider_id", 4001L);
        request1.put("external_subject", "DUPLICATE001");
        request1.put("created_by", "test");
        invokePost("/api/security/identity/bindings", request1);

        // 第二次绑定相同外部账号到不同用户
        Map<String, Object> request2 = new LinkedHashMap<>();
        request2.put("tenant_id", 1L);
        request2.put("user_id", 1002L);
        request2.put("provider_id", 4001L);
        request2.put("external_subject", "DUPLICATE001");
        request2.put("created_by", "test");

        Map<String, Object> result = invokePost("/api/security/identity/bindings", request2);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false");
        assertEquals("DUPLICATE_EXTERNAL_ACCOUNT", result.get("error_code"), "error_code should be DUPLICATE_EXTERNAL_ACCOUNT");
    }

    // ==================== DELETE /api/security/identity/bindings/{bindingId} ====================

    @Test
    void unbindIdentityReturnsSuccess() throws Exception {
        // 先绑定
        Map<String, Object> bindRequest = new LinkedHashMap<>();
        bindRequest.put("tenant_id", 1L);
        bindRequest.put("user_id", 1001L);
        bindRequest.put("provider_id", 4001L);
        bindRequest.put("external_subject", "UNBIND001");
        bindRequest.put("created_by", "test");
        Map<String, Object> bindResult = invokePost("/api/security/identity/bindings", bindRequest);
        Long bindingId = ((Number) bindResult.get("data")).longValue();

        // 解绑
        MvcResult mvcResult = mockMvc.perform(delete("/api/security/identity/bindings/{bindingId}", bindingId)
                        .param("tenant_id", "1")
                        .param("unbind_reason", "测试解绑")
                        .param("unbound_by", "test")
                        .header("Authorization", "Bearer " + authToken())
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        Map<String, Object> result = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Map.class);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");
    }

    // ==================== POST /api/security/identity/merge ====================

    @Test
    void mergeUsersReturnsSuccess() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("tenant_id", 1L);
        request.put("source_user_id", 1002L);
        request.put("target_user_id", 1001L);
        request.put("merge_reason", "测试合并");
        request.put("merged_by", "test");

        Map<String, Object> result = invokePost("/api/security/identity/merge", request);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");
    }

    @Test
    void mergeUsersWithSameUserReturnsError() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("tenant_id", 1L);
        request.put("source_user_id", 1001L);
        request.put("target_user_id", 1001L);
        request.put("merge_reason", "测试合并");
        request.put("merged_by", "test");

        Map<String, Object> result = invokePost("/api/security/identity/merge", request);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false");
        assertEquals("VALIDATION_ERROR", result.get("error_code"), "error_code should be VALIDATION_ERROR");
    }

    // ==================== GET /api/security/identity/bindings/user/{userId} ====================

    @Test
    void getBindingsByUserIdReturnsSuccess() throws Exception {
        Map<String, Object> result = invokeGet("/api/security/identity/bindings/user/1001?tenant_id=1");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertNotNull(data, "data should not be null");
    }

    // ==================== GET /api/security/identity/bindings/{bindingId} ====================

    @Test
    void getBindingByIdReturnsSuccess() throws Exception {
        // 先绑定
        Map<String, Object> bindRequest = new LinkedHashMap<>();
        bindRequest.put("tenant_id", 1L);
        bindRequest.put("user_id", 1001L);
        bindRequest.put("provider_id", 4001L);
        bindRequest.put("external_subject", "GET_BINDING_001");
        bindRequest.put("created_by", "test");
        Map<String, Object> bindResult = invokePost("/api/security/identity/bindings", bindRequest);
        Long bindingId = ((Number) bindResult.get("data")).longValue();

        // 获取绑定详情
        Map<String, Object> result = invokeGet("/api/security/identity/bindings/" + bindingId + "?tenant_id=1");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertNotNull(data.get("id"), "id must not be null");
        assertNotNull(data.get("user_id"), "user_id must not be null");
        assertNotNull(data.get("provider_id"), "provider_id must not be null");
        assertNotNull(data.get("external_subject"), "external_subject must not be null");
        assertNotNull(data.get("binding_status"), "binding_status must not be null");
    }

    // ==================== GET /api/security/identity/merge/user/{userId} ====================

    @Test
    void getMergeRecordsReturnsSuccess() throws Exception {
        Map<String, Object> result = invokeGet("/api/security/identity/merge/user/1001?tenant_id=1");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertNotNull(data, "data should not be null");
    }

    // ==================== GET /api/security/identity/unbind/user/{userId} ====================

    @Test
    void getUnbindRecordsReturnsSuccess() throws Exception {
        Map<String, Object> result = invokeGet("/api/security/identity/unbind/user/1001?tenant_id=1");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertNotNull(data, "data should not be null");
    }

    // ==================== Helper methods ====================

    private Map<String, Object> invokeGet(String url) throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + authToken())
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Map.class);
    }

    private Map<String, Object> invokePost(String url, Map<String, Object> request) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + authToken())
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Map.class);
    }

    private String authToken() {
        return jwtTokenProvider.createToken(1001L, 1L, "admin", "系统管理员");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object obj) {
        return (List<Map<String, Object>>) obj;
    }
}
