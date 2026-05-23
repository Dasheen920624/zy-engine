package com.medkernel.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GA-CORE-07 / W1-G4：Virtual Threads 端到端 smoke。
 *
 * <p>HTTP 请求经 Tomcat 进入 Controller 时，{@code Thread.currentThread().isVirtual()}
 * 应该返回 true（Spring Boot 3.2+ 默认行为）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RuntimeProbeControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void runtimeReportsJdk21() throws Exception {
        mvc.perform(get("/api/v1/system/runtime"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.javaVersion", org.hamcrest.Matchers.startsWith("21")))
           .andExpect(jsonPath("$.isVirtualThread").value(true));
    }
}
