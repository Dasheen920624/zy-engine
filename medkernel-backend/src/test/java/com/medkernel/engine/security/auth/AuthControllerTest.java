package com.medkernel.engine.security.auth;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.engine.security.PlatformCredential;
import com.medkernel.engine.security.PlatformCredentialRepository;
import com.medkernel.engine.security.RoleCode;
import com.medkernel.engine.security.UserRoleAssignment;
import com.medkernel.engine.security.UserRoleAssignmentRepository;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    PlatformCredentialRepository credentialRepository;

    @Autowired
    UserRoleAssignmentRepository roleAssignmentRepository;

    private static final String TENANT = "t-1";
    private static final String USERNAME = "doctor-test";
    private static final String USER_ID = "doctor-1";
    private static final String RAW_PASSWORD = "Mk@2026pw";

    @BeforeEach
    void setUp() {
        // 清理旧数据（保证幂等）
        credentialRepository.findByTenantIdAndUsername(TENANT, USERNAME)
            .ifPresent(c -> credentialRepository.delete(c));

        Instant now = Instant.now();
        // 插入 ACTIVE 凭证
        credentialRepository.save(new PlatformCredential(
            null, "cred-doctor-test", TENANT, USER_ID, USERNAME,
            passwordEncoder.encode(RAW_PASSWORD), "ACTIVE", "N", null,
            now, "test", now, "test", "test-trace"
        ));

        // 插入角色分配（仅当不存在时）
        boolean hasRole = roleAssignmentRepository.findActiveByTenantIdAndUserId(TENANT, USER_ID)
            .stream().anyMatch(a -> RoleCode.DOCTOR.code().equals(a.roleCode()));
        if (!hasRole) {
            roleAssignmentRepository.save(new UserRoleAssignment(
                null, TENANT, USER_ID, RoleCode.DOCTOR.code(), "TENANT", TENANT,
                "Y", now, "test", now, "test"
            ));
        }
    }

    @AfterEach
    void cleanUp() {
        credentialRepository.findByTenantIdAndUsername(TENANT, USERNAME)
            .ifPresent(c -> credentialRepository.delete(c));
    }

    @Test
    void login_success_setsHttpOnlyCookie() throws Exception {
        var body = objectMapper.writeValueAsString(
            new LoginRequest(USERNAME, RAW_PASSWORD, TENANT));

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(header().exists("Set-Cookie"))
            .andExpect(result -> {
                String setCookie = result.getResponse().getHeader("Set-Cookie");
                assert setCookie != null && setCookie.contains("mk_access");
                assert setCookie.contains("HttpOnly");
            })
            .andExpect(jsonPath("$.data.userId").value(USER_ID))
            .andExpect(jsonPath("$.data.tenantId").value(TENANT))
            .andExpect(jsonPath("$.data.mustChangePwd").value(false));
    }

    @Test
    void login_wrongPassword_rejectedWithoutLeakingExistence() throws Exception {
        // 错误密码 → 401
        var wrongPwd = objectMapper.writeValueAsString(
            new LoginRequest(USERNAME, "WrongPassword123", TENANT));
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongPwd))
            .andExpect(status().isUnauthorized());

        // 不存在用户 → 也 401（防枚举：状态码一致）
        var noUser = objectMapper.writeValueAsString(
            new LoginRequest("nobody-xyz", "anything", TENANT));
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(noUser))
            .andExpect(status().isUnauthorized());
    }
}
