package com.medkernel.quality.insurance;

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
class DrgRulesetControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void rulesetsLists() throws Exception {
        mvc.perform(get("/api/v1/quality/insurance/drg/rulesets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void syncReturnsDiff() throws Exception {
        mvc.perform(post("/api/v1/quality/insurance/drg/sync"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.diff.added").value(3));
    }
}
