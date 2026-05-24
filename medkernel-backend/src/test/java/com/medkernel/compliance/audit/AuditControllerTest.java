package com.medkernel.compliance.audit;

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
class AuditControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void eventsReturnSeed() throws Exception {
        mvc.perform(get("/api/v1/compliance/audit/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void snapshotProducesSm3Signature() throws Exception {
        mvc.perform(post("/api/v1/compliance/audit/snapshot?reason=test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.signature").value(org.hamcrest.Matchers.startsWith("✓ SM3")));
    }
}
