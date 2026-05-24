package com.medkernel.platform.license;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class OfflineLicenseControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void issueProducesSm3Signature() throws Exception {
        mvc.perform(post("/api/v1/platform/license/offline/issue?hospitalId=h1&validUntil=2027-12-31&tier=enterprise"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sm3Signature").exists())
            .andExpect(jsonPath("$.licenseId").value(org.hamcrest.Matchers.startsWith("LIC-")));
    }

    @Test
    void statusReturnsUsageMetrics() throws Exception {
        mvc.perform(get("/api/v1/platform/license/offline/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tier").value("enterprise"))
            .andExpect(jsonPath("$.usage").exists());
    }
}
