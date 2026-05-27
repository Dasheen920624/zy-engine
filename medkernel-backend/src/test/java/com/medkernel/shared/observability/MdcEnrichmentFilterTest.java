package com.medkernel.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MdcEnrichmentFilterTest {

    @Autowired
    MockMvc mvc;

    @Test
    void clearsMdcAfterRequest() throws Exception {
        // 请求之前 MDC 应为空（或者无 traceId）
        assertThat(MDC.get("traceId")).isNull();

        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());

        // 请求结束后 MDC 应被清理
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("requestPath")).isNull();
    }
}
