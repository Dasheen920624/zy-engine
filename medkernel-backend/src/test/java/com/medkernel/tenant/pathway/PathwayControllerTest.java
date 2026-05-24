package com.medkernel.tenant.pathway;

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
class PathwayControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void listReturnsFourTemplates() throws Exception {
        mvc.perform(get("/api/v1/tenant/pathways"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].name").value("胸痛 AMI 急诊路径"));
    }

    @Test
    void publishReturnsCanary() throws Exception {
        mvc.perform(post("/api/v1/tenant/pathways/p1/publish"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stage").value("canary"))
            .andExpect(jsonPath("$.rollout").value("10%"));
    }
}
