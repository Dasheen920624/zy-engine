package com.medkernel.compliance.masking;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaskingControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void profilesReturnsFour() throws Exception {
        mvc.perform(get("/api/v1/compliance/masking/profiles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.DEV").exists())
            .andExpect(jsonPath("$.EXPORT").exists());
    }

    @Test
    void applyDevProfileSemiMasksName() throws Exception {
        Map<String, String> raw = Map.of("name", "张三丰", "phone", "13812345678");
        mvc.perform(post("/api/v1/compliance/masking/apply?profile=DEV")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(raw)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("张**"))
            .andExpect(jsonPath("$.phone").value("138****5678"));
    }
}
