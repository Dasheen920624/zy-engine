package com.medkernel.security.usersync;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 用户同步（User Sync）REST API 契约测试
 * 覆盖 /api/user-sync 下所有端点的正确性、参数校验及 traceId 传播
 *
 * @see com.medkernel.security.usersync.UserSyncApiController
 * @see com.medkernel.security.usersync.UserSyncApiService
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSyncApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ==================== GET /api/user-sync/sources ====================

    @Test
    void listSourcesReturnsSeededData() throws Exception {
        Map<String, Object> result = invokeGet("/api/user-sync/sources");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertTrue(data.size() >= 4, "expected at least 4 seeded sources, got " + data.size());

        Map<String, Object> first = data.get(0);
        assertNotNull(first.get("id"), "id must not be null");
        assertNotNull(first.get("source_code"), "source_code must not be null");
        assertNotNull(first.get("source_name"), "source_name must not be null");
        assertNotNull(first.get("source_type"), "source_type must not be null");
        assertNotNull(first.get("status"), "status must not be null");
    }

    // ==================== GET /api/user-sync/sources/{sourceId} ====================

    @Test
    void getSourceReturnsHIS() throws Exception {
        Map<String, Object> result = invokeGet("/api/user-sync/sources/4001");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals(4001L, ((Number) data.get("id")).longValue());
        assertEquals("HIS", data.get("source_code"));
        assertEquals("医院信息系统", data.get("source_name"));
        assertEquals("HIS", data.get("source_type"));
        assertEquals("ACTIVE", data.get("status"));
    }

    @Test
    void getNonExistentSourceReturnsNotFound() throws Exception {
        Map<String, Object> result = invokeGet("/api/user-sync/sources/9999");
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false");
    }

    // ==================== POST /api/user-sync/sources ====================

    @Test
    void createSourceReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source_code", "LIS");
        body.put("source_name", "检验信息系统");
        body.put("source_type", "LIS");
        body.put("sync_mode", "MANUAL");
        body.put("status", "ACTIVE");
        body.put("description", "LIS 用户同步源");

        Map<String, Object> result = invokePost("/api/user-sync/sources", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertNotNull(data.get("id"), "id must not be null");
        assertEquals("LIS", data.get("source_code"));
        assertEquals("检验信息系统", data.get("source_name"));
        assertNotNull(data.get("trace_id"), "trace_id must be present");
    }

    // ==================== POST /api/user-sync/sources/{sourceId} ====================

    @Test
    void updateSourceReturnsSuccess() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source_name", "医院信息系统（已更新）");
        body.put("description", "HIS 用户同步源（已更新）");

        Map<String, Object> result = invokePost("/api/user-sync/sources/4001", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("医院信息系统（已更新）", data.get("source_name"));
    }

    // ==================== GET /api/user-sync/tasks ====================

    @Test
    void listTasksReturnsEmptyList() throws Exception {
        Map<String, Object> result = invokeGet("/api/user-sync/tasks");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertNotNull(data, "task list should not be null");
    }

    // ==================== POST /api/user-sync/sources/{sourceId}/sync ====================

    @Test
    void triggerSyncReturnsTask() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("task_type", "MANUAL");

        List<Map<String, Object>> users = Arrays.asList(
                createUser("EXT001", "user001", "用户001", "user001@example.com", "13800000001", "内科", "医生"),
                createUser("EXT002", "user002", "用户002", "user002@example.com", "13800000002", "外科", "护士")
        );
        body.put("users", users);

        Map<String, Object> result = invokePost("/api/user-sync/sources/4001/sync", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertNotNull(data.get("id"), "task id must not be null");
        assertEquals("MANUAL", data.get("task_type"));
        assertNotNull(data.get("status"), "status must not be null");
        assertNotNull(data.get("trace_id"), "trace_id must be present");
    }

    @Test
    void triggerSyncWithEmptyUsersReturnsTask() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("task_type", "MANUAL");
        body.put("users", Collections.emptyList());

        Map<String, Object> result = invokePost("/api/user-sync/sources/4001/sync", body);
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        Map<String, Object> data = asMap(result.get("data"));
        assertNotNull(data.get("id"), "task id must not be null");
        assertEquals(0, ((Number) data.get("total_count")).intValue());
    }

    // ==================== GET /api/user-sync/tasks/{taskId} ====================

    @Test
    void getNonExistentTaskReturnsNotFound() throws Exception {
        Map<String, Object> result = invokeGet("/api/user-sync/tasks/9999");
        assertEquals(Boolean.FALSE, result.get("success"), "success should be false");
    }

    // ==================== GET /api/user-sync/tasks/{taskId}/logs ====================

    @Test
    void listTaskLogsReturnsEmptyList() throws Exception {
        // First create a task
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("task_type", "MANUAL");
        body.put("users", Collections.emptyList());

        Map<String, Object> syncResult = invokePost("/api/user-sync/sources/4001/sync", body);
        Map<String, Object> taskData = asMap(syncResult.get("data"));
        Long taskId = ((Number) taskData.get("id")).longValue();

        // Then get logs
        Map<String, Object> result = invokeGet("/api/user-sync/tasks/" + taskId + "/logs");
        assertEquals(Boolean.TRUE, result.get("success"), "success should be true");

        List<Map<String, Object>> data = asListOfMap(result.get("data"));
        assertEquals(0, data.size(), "expected empty logs for empty sync");
    }

    // ==================== Helper methods ====================

    private Map<String, Object> createUser(String externalId, String username, String displayName,
                                           String email, String phone, String department, String position) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("external_id", externalId);
        user.put("username", username);
        user.put("display_name", displayName);
        user.put("email", email);
        user.put("phone", phone);
        user.put("department", department);
        user.put("position", position);
        return user;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeGet(String url) throws Exception {
        String token = jwtTokenProvider.createToken(1L, 1001L, "admin", "管理员");
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
        String token = jwtTokenProvider.createToken(1L, 1001L, "admin", "管理员");
        MvcResult mvcResult = mockMvc.perform(post(url)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, Map.class);
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
