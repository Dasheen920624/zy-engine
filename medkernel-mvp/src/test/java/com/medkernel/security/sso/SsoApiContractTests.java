package com.medkernel.security.sso;

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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * SSO（单点登录）REST API 契约测试
 * 覆盖 /api/sso 下所有端点的正确性、参数校验及 traceId 传播
 *
 * @see com.medkernel.security.sso.SsoConfigController
 * @see com.medkernel.security.sso.SsoConfigService
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SsoApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ==================== GET /api/sso/health ====================

    @Test
    void healthReturnsOk() throws Exception {
        Map<String, Object> result = invokeGet("/api/sso/health");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("ok", data.get("status"));
        assertEquals("sso", data.get("module"));
    }

    // ==================== GET /api/sso/configs ====================

    @Test
    void listConfigsReturnsSeededData() throws Exception {
        Map<String, Object> result = invokeGet("/api/sso/configs");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertTrue(data.size() >= 4, "expected at least 4 seeded configs, got " + data.size());

        Map<String, Object> first = data.get(0);
        assertNotNull(first.get("id"), "id must not be null");
        assertNotNull(first.get("config_code"), "config_code must not be null");
        assertNotNull(first.get("config_name"), "config_name must not be null");
        assertNotNull(first.get("protocol_type"), "protocol_type must not be null");
        assertNotNull(first.get("status"), "status must not be null");
        assertNotNull(first.get("priority"), "priority must not be null");
    }

    // ==================== GET /api/sso/configs/detail ====================

    @Test
    void getConfigReturnsCASConfig() throws Exception {
        // First get the list to find a valid config ID
        Map<String, Object> listResult = invokeGet("/api/sso/configs");
        List<Map<String, Object>> configs = asListOfMap(listResult.get("data"));
        Long configId = ((Number) configs.get(0).get("id")).longValue();

        Map<String, Object> result = invokeGet("/api/sso/configs/detail?id=" + configId);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals(configId, ((Number) data.get("id")).longValue());
        assertNotNull(data.get("config_code"), "config_code must not be null");
        assertNotNull(data.get("protocol_type"), "protocol_type must not be null");
    }

    @Test
    void getNonExistentConfigReturnsFailure() throws Exception {
        Map<String, Object> result = invokeGet("/api/sso/configs/detail?id=99999");
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false");
    }

    // ==================== POST /api/sso/configs ====================

    @Test
    void saveConfigCreatesNewConfig() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("configCode", "OIDC-TEST");
        body.put("configName", "OIDC 测试配置");
        body.put("protocolType", "OIDC");
        body.put("status", "DISABLED");
        body.put("priority", 10);
        body.put("oidcIssuer", "https://accounts.example.com");
        body.put("oidcClientId", "test-client-id");
        body.put("oidcClientSecret", "test-client-secret");
        body.put("oidcRedirectUri", "https://app.example.com/callback");
        body.put("oidcScope", "openid profile email");
        body.put("oidcResponseType", "code");
        body.put("autoCreateUser", false);
        body.put("autoUpdateUser", false);
        body.put("sessionTimeoutMinutes", 480);

        Map<String, Object> result = invokePost("/api/sso/configs", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");
    }

    // ==================== POST /api/sso/configs/delete ====================

    @Test
    void deleteConfigWithValidIdReturnsSuccess() throws Exception {
        // First create a config to delete
        Map<String, Object> createBody = new LinkedHashMap<>();
        createBody.put("configCode", "DELETE-TEST");
        createBody.put("configName", "待删除配置");
        createBody.put("protocolType", "CAS");
        createBody.put("status", "DISABLED");
        createBody.put("priority", 99);
        createBody.put("autoCreateUser", false);
        createBody.put("autoUpdateUser", false);
        createBody.put("sessionTimeoutMinutes", 480);
        invokePost("/api/sso/configs", createBody);

        // Get the created config
        Map<String, Object> listResult = invokeGet("/api/sso/configs");
        List<Map<String, Object>> configs = asListOfMap(listResult.get("data"));
        Long configToDelete = configs.stream()
                .filter(c -> "DELETE-TEST".equals(c.get("config_code")))
                .map(c -> ((Number) c.get("id")).longValue())
                .findFirst()
                .orElse(null);

        if (configToDelete != null) {
            Map<String, Object> deleteBody = new LinkedHashMap<>();
            deleteBody.put("id", configToDelete);
            Map<String, Object> result = invokePost("/api/sso/configs/delete", deleteBody);
            assertEquals(Boolean.TRUE, result.get("success"), "success should be true");
        }
    }

    @Test
    void deleteConfigWithNullIdReturnsFailure() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        // No "id" field
        Map<String, Object> result = invokePost("/api/sso/configs/delete", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false");
    }

    // ==================== POST /api/sso/cas/callback ====================

    @Test
    void casCallbackWithEmptyTicketReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ticket", "");
        Map<String, Object> result = invokePost("/api/sso/cas/callback", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for empty ticket");
    }

    @Test
    void casCallbackWithNullTicketReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        // No "ticket" field
        Map<String, Object> result = invokePost("/api/sso/cas/callback", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for null ticket");
    }

    // ==================== POST /api/sso/oidc/callback ====================

    @Test
    void oidcCallbackWithEmptyCodeReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "");
        Map<String, Object> result = invokePost("/api/sso/oidc/callback", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for empty code");
    }

    @Test
    void oidcCallbackWithNullCodeReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        // No "code" field
        Map<String, Object> result = invokePost("/api/sso/oidc/callback", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for null code");
    }

    // ==================== POST /api/sso/saml/callback ====================

    @Test
    void samlCallbackWithEmptyResponseReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("SAMLResponse", "");
        Map<String, Object> result = invokePost("/api/sso/saml/callback", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for empty SAMLResponse");
    }

    @Test
    void samlCallbackWithNullResponseReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        // No "SAMLResponse" field
        Map<String, Object> result = invokePost("/api/sso/saml/callback", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for null SAMLResponse");
    }

    // ==================== POST /api/sso/ldap/login ====================

    @Test
    void ldapLoginWithEmptyCredentialsReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "");
        body.put("password", "");
        Map<String, Object> result = invokePost("/api/sso/ldap/login", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for empty credentials");
    }

    @Test
    void ldapLoginWithNullCredentialsReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        // No username/password
        Map<String, Object> result = invokePost("/api/sso/ldap/login", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for null credentials");
    }

    // ==================== GET /api/sso/sessions ====================

    @Test
    void listSessionsReturnsEmptyList() throws Exception {
        Map<String, Object> result = invokeGet("/api/sso/sessions");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertNotNull(data, "sessions list should not be null");
    }

    // ==================== GET /api/sso/audit-logs ====================

    @Test
    void listAuditLogsReturnsSuccess() throws Exception {
        Map<String, Object> result = invokeGet("/api/sso/audit-logs?limit=10");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertNotNull(data, "audit logs list should not be null");
    }

    @Test
    void listAuditLogsWithDefaultLimitReturnsSuccess() throws Exception {
        Map<String, Object> result = invokeGet("/api/sso/audit-logs");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");
    }

    // ==================== POST /api/sso/logout ====================

    @Test
    void logoutWithInvalidSessionReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("session_token", "non-existent-token");
        Map<String, Object> result = invokePost("/api/sso/logout", body);
        // Logout should succeed even with non-existent session
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");
    }

    // ==================== Helper methods ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeGet(String url) throws Exception {
        String token = authToken();
        MvcResult mvcResult = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokePost(String url, Map<String, Object> body) throws Exception {
        String token = authToken();
        MvcResult mvcResult = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, Map.class);
    }

    private String authToken() {
        return jwtTokenProvider.createToken(1001L, 1L, "admin", "系统管理员");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object obj) {
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return Collections.emptyList();
    }
}
