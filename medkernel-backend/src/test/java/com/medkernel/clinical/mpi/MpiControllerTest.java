package com.medkernel.clinical.mpi;

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
class MpiControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void patientsReturnsSeedData() throws Exception {
        mvc.perform(get("/api/v1/clinical/mpi/patients"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].mpiId").value("MPI-000123456"))
            .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void patientsSearchFiltersById() throws Exception {
        mvc.perform(get("/api/v1/clinical/mpi/patients?q=123456"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void statsExposesFourCounters() throws Exception {
        mvc.perform(get("/api/v1/clinical/mpi/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").exists())
            .andExpect(jsonPath("$.conflicts").exists());
    }
}
