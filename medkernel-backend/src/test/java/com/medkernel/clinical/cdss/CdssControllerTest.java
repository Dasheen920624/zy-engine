package com.medkernel.clinical.cdss;

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
class CdssControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void alertsReturnsSeed() throws Exception {
        mvc.perform(get("/api/v1/clinical/cdss/alerts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].source").exists());
    }

    @Test
    void adoptDecisionIncrementsMetric() throws Exception {
        mvc.perform(post("/api/v1/clinical/cdss/alerts/1/adopt"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decision").value("adopt"))
            .andExpect(jsonPath("$.recordedAt").exists());
    }
}
