package com.medkernel.engine.followup;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.medkernel.shared.context.RequestContext;

/**
 * 随访引擎 Controller 安全矩阵测试。
 *
 * <p>验证未认证用户和不同角色用户对随访 API 的访问控制：
 * <ul>
 *   <li>未认证 → 401 Unauthorized</li>
 *   <li>有 followup.write 权限的角色（医务处）→ 200 OK</li>
 *   <li>有 followup.read 权限的角色（医生）→ GET 200 OK，POST 403 Forbidden</li>
 * </ul>
 *
 * <p>权限模型：{@code @perm.has('followup.write')} 基于 RoleCode → DefaultPermissionPolicy 角色权限映射推导。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class FollowupEngineControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FollowupEngineService service;

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    private static final String GENERATE_BODY = """
        {
          "patientId": "P1001",
          "encounterId": "E2001",
          "pathwayId": "PATH01",
          "diseaseCode": "I21.900",
          "riskLevel": "HIGH",
          "taskTypes": ["QUESTIONNAIRE", "OUTPATIENT"]
        }
        """;

    @Test
    void testGenerateWithoutAuth_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/engine/followup/plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(GENERATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_MEDICAL_AFFAIRS"})
    void testGenerateWithWriteRole_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/engine/followup/plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(GENERATE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_DOCTOR"})
    void testGetDetailWithReadRole_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/engine/followup/plans/P1000"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_DOCTOR"})
    void testGenerateWithReadOnlyRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/engine/followup/plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(GENERATE_BODY))
                .andExpect(status().isForbidden());
    }
}
