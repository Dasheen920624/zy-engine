package com.medkernel.shared.web;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GA-CORE-07 / W1-G4：Virtual Threads 端到端 smoke。
 *
 * <p>必须用 RANDOM_PORT 启动真实 Tomcat（而非 MockMvc）才能验证
 * {@code spring.threads.virtual.enabled=true} 在 Tomcat connector 上生效。
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RuntimeProbeControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    @SuppressWarnings("unchecked")
    void runtimeReportsJdk21AndVirtualThread() {
        Map<String, Object> body = rest.getForObject("/api/v1/system/runtime", Map.class);

        assertThat(body).isNotNull();
        assertThat((String) body.get("javaVersion")).startsWith("21");
        assertThat((Boolean) body.get("isVirtualThread"))
            .as("Spring Boot 3.3 + JDK 21 默认应使 HTTP 请求跑在 Virtual Thread 上")
            .isTrue();
    }
}
