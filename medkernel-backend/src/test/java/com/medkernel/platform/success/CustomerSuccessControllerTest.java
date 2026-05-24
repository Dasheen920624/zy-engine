package com.medkernel.platform.success;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CustomerSuccessControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void healthScoreReturnsAllDimensions() throws Exception {
        mvc.perform(get("/api/v1/platform/customer-success/health-score"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.score").exists())
            .andExpect(jsonPath("$.dimensions").exists())
            .andExpect(jsonPath("$.renewProbability").exists());
    }

    @Test
    void benchmarkReturnsRank() throws Exception {
        mvc.perform(get("/api/v1/platform/customer-success/benchmark"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rank").value(2));
    }
}
