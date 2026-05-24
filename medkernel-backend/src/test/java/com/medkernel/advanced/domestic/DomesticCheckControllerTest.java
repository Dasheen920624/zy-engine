package com.medkernel.advanced.domestic;

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
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DomesticCheckControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void snapshotReturnsAllFiveLayers() throws Exception {
        mvc.perform(get("/api/v1/advanced/domestic/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.os").exists())
            .andExpect(jsonPath("$.jdk").exists())
            .andExpect(jsonPath("$.middleware").exists())
            .andExpect(jsonPath("$.crypto").exists())
            .andExpect(jsonPath("$.score").exists());
    }
}
