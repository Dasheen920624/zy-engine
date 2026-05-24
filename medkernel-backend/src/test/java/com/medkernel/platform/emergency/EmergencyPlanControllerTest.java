package com.medkernel.platform.emergency;

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
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmergencyPlanControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void plansReturnFour() throws Exception {
        mvc.perform(get("/api/v1/platform/emergency/plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void activateReturnsActions() throws Exception {
        mvc.perform(post("/api/v1/platform/emergency/P-COVID/activate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("activated"))
            .andExpect(jsonPath("$.actionsTriggered.length()").value(5));
    }
}
