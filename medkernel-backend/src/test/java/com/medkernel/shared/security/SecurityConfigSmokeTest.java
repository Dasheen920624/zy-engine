package com.medkernel.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GA-CORE-02 / W1-G3 闸门 smoke：
 * <ul>
 *   <li>SecurityFilterChain bean 已注册
 *   <li>/api/v1/system/ping 白名单可匿名访问 → 200
 *   <li>不存在的鉴权端点匿名访问 → 401（OAuth2 Resource Server 生效证据）
 * </ul>
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles("test")
class SecurityConfigSmokeTest {

    @Autowired
    WebApplicationContext context;

    @Test
    void publicSystemPingIsAllowed() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/api/v1/system/ping"))
           .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointReturns401WithoutToken() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        mvc.perform(get("/api/v1/protected-placeholder"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void filterChainBeanIsRegistered() {
        assertThat(context.getBean(org.springframework.security.web.SecurityFilterChain.class))
            .as("SecurityFilterChain bean registered")
            .isNotNull();
    }
}
