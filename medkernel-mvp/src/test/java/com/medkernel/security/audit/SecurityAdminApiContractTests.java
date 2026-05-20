package com.medkernel.security.audit;

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
 * 安全管理（审计防篡改、密钥轮换）REST API 契约测试
 * 覆盖 /api/security/admin 下所有端点的正确性、参数校验
 *
 * @see com.medkernel.security.audit.SecurityAdminController
 * @see com.medkernel.security.audit.AuditChainService
 * @see com.medkernel.security.audit.KeyManagementService
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAdminApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ==================== GET /api/security/admin/baseline ====================

    @Test
    void getSecurityBaselineReturnsSuccess() throws Exception {
        Map<String, Object> result = invokeGet("/api/security/admin/baseline");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertNotNull(data.get("encryption"), "encryption status should be present");
        assertNotNull(data.get("audit_chain"), "audit_chain status should be present");
        assertNotNull(data.get("recommendations"), "recommendations should be present");
    }

    // ==================== GET /api/security/admin/keys ====================

    @Test
    void listKeysReturnsSeededData() throws Exception {
        Map<String, Object> result = invokeGet("/api/security/admin/keys");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertTrue(data.size() >= 1, "expected at least 1 seeded key, got " + data.size());

        Map<String, Object> first = data.get(0);
        assertNotNull(first.get("id"), "id must not be null");
        assertNotNull(first.get("key_id"), "key_id must not be null");
        assertNotNull(first.get("key_version"), "key_version must not be null");
        assertNotNull(first.get("algorithm"), "algorithm must not be null");
        assertNotNull(first.get("status"), "status must not be null");
        // 密钥材料不应暴露
        assertEquals(null, first.get("key_material"), "key_material should not be exposed");
    }

    // ==================== GET /api/security/admin/keys/active ====================

    @Test
    void getActiveKeyReturnsKey() throws Exception {
        Map<String, Object> result = invokeGet("/api/security/admin/keys/active");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertNotNull(data.get("key_id"), "key_id must not be null");
        assertEquals("ACTIVE", data.get("status"), "status should be ACTIVE");
        assertEquals("AES-256-GCM", data.get("algorithm"), "algorithm should be AES-256-GCM");
    }

    // ==================== POST /api/security/admin/keys/rotate ====================

    @Test
    void rotateKeyCreatesNewVersion() throws Exception {
        // 获取当前版本
        Map<String, Object> beforeResult = invokeGet("/api/security/admin/keys/active");
        Map<String, Object> beforeData = asMap(beforeResult.get("data"));
        int currentVersion = ((Number) beforeData.get("key_version")).intValue();

        // 执行轮换
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "测试密钥轮换");
        Map<String, Object> result = invokePost("/api/security/admin/keys/rotate", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals(currentVersion + 1, ((Number) data.get("key_version")).intValue(),
                "new version should be incremented");
        assertEquals("ACTIVE", data.get("status"), "new key should be ACTIVE");
    }

    // ==================== POST /api/security/admin/encrypt ====================

    @Test
    void encryptReturnsEncryptedText() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plaintext", "Hello, World!");
        Map<String, Object> result = invokePost("/api/security/admin/encrypt", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("Hello, World!", data.get("plaintext"), "plaintext should be preserved");
        String encrypted = (String) data.get("encrypted");
        assertNotNull(encrypted, "encrypted text should not be null");
        assertTrue(encrypted.startsWith("ENC("), "encrypted text should start with ENC(");
        assertTrue(encrypted.endsWith(")"), "encrypted text should end with )");
    }

    @Test
    void encryptWithEmptyPlaintextReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plaintext", "");
        Map<String, Object> result = invokePost("/api/security/admin/encrypt", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for empty plaintext");
    }

    // ==================== POST /api/security/admin/decrypt ====================

    @Test
    void decryptRoundTripSucceeds() throws Exception {
        // 先加密
        Map<String, Object> encryptBody = new LinkedHashMap<>();
        encryptBody.put("plaintext", "测试解密");
        Map<String, Object> encryptResult = invokePost("/api/security/admin/encrypt", encryptBody);
        Map<String, Object> encryptData = asMap(encryptResult.get("data"));
        String encrypted = (String) encryptData.get("encrypted");

        // 再解密
        Map<String, Object> decryptBody = new LinkedHashMap<>();
        decryptBody.put("encrypted", encrypted);
        Map<String, Object> decryptResult = invokePost("/api/security/admin/decrypt", decryptBody);
        assertEquals(Boolean.TRUE, decryptResult.get("success"), "success should be true");

        Map<String, Object> decryptData = asMap(decryptResult.get("data"));
        assertEquals("测试解密", decryptData.get("decrypted"), "decrypted text should match original");
    }

    @Test
    void decryptWithInvalidTextReturnsFailure() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("encrypted", "invalid-encrypted-text");
        Map<String, Object> result = invokePost("/api/security/admin/decrypt", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for invalid encrypted text");
    }

    // ==================== POST /api/security/admin/audit-chain/verify ====================

    @Test
    void verifyAuditChainReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("table_name", "engine_audit_log");
        Map<String, Object> result = invokePost("/api/security/admin/audit-chain/verify", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertNotNull(data.get("checkpoint_id"), "checkpoint_id should be present");
        assertNotNull(data.get("chain_status"), "chain_status should be present");
        assertNotNull(data.get("total_records"), "total_records should be present");
        assertNotNull(data.get("valid_records"), "valid_records should be present");
    }

    @Test
    void verifyAuditChainWithInvalidTableReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("table_name", "invalid_table");
        Map<String, Object> result = invokePost("/api/security/admin/audit-chain/verify", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for invalid table");
    }

    @Test
    void verifyAuditChainWithEmptyTableReturnsValidation() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("table_name", "");
        Map<String, Object> result = invokePost("/api/security/admin/audit-chain/verify", body);
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false for empty table name");
    }

    // ==================== GET /api/security/admin/audit-chain/status ====================

    @Test
    void getAuditChainStatusReturnsSuccess() throws Exception {
        Map<String, Object> result = invokeGet("/api/security/admin/audit-chain/status");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertNotNull(data.get("engine_audit_log"), "engine_audit_log status should be present");
        assertNotNull(data.get("sec_auth_audit_log"), "sec_auth_audit_log status should be present");
        assertNotNull(data.get("sec_sso_audit_log"), "sec_sso_audit_log status should be present");
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
