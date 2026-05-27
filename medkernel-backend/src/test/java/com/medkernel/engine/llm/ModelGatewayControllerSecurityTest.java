package com.medkernel.engine.llm;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.medkernel.shared.context.RequestContext;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ModelGatewayControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModelGatewayService service;

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    private static final String TASK_BODY = """
        {
          "capabilityCode": "knowledge.extract",
          "inputData": "提取高血压病历信息",
          "desensitizeStrategy": "DEFAULT",
          "expectedSchema": "required: [entity]",
          "timeoutSeconds": 60
        }
        """;

    @Test
    void testSubmitTaskWithoutAuth_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/model-capabilities/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TASK_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSubmitTaskWithWriteRole_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/model-capabilities/tasks")
                .with(jwt().jwt(token -> token
                    .subject("test-user")
                    .claim("tenant_id", "tenant-1")
                    .claim("roles", List.of("doctor")))
                    .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TASK_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void testSubmitTaskWithReadOnlyRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/model-capabilities/tasks")
                .with(jwt().jwt(token -> token
                    .subject("test-user")
                    .claim("tenant_id", "tenant-1")
                    .claim("roles", List.of("qa-manager")))
                    .authorities(new SimpleGrantedAuthority("ROLE_QA_MANAGER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TASK_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSubmitTaskWithWriteRoleButMissingTenant_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/model-capabilities/tasks")
                .with(jwt().jwt(token -> token
                    .subject("test-user")
                    .claim("roles", List.of("doctor")))
                    .authorities(new SimpleGrantedAuthority("ROLE_DOCTOR")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(TASK_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ENG-BASE-001"));
    }
}
