package com.medkernel.shared.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RuntimeOperationsControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void operationsSnapshotExposesRuntimeContractWithoutSecrets() throws Exception {
        mvc.perform(get("/api/v1/system/operations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.serviceName").value("medkernel"))
            .andExpect(jsonPath("$.data.deploymentMode").value("ci"))
            .andExpect(jsonPath("$.data.databaseDialect").value("h2"))
            .andExpect(jsonPath("$.data.migrationLocation").value("classpath:db/migration/h2"))
            .andExpect(jsonPath("$.data.activeProfiles", hasItem("test")))
            .andExpect(jsonPath("$.data.healthStatus").value("UP"))
            .andExpect(jsonPath("$.data.featureFlags[*].key", hasItems(
                "graph-projection",
                "dify-workflow",
                "audit-persistence",
                "external-provider",
                "domestic-crypto"
            )))
            .andExpect(jsonPath("$.data.dependencies[*].key", hasItems(
                "database",
                "prometheus",
                "backup-restore",
                "graph-projection",
                "dify-workflow"
            )))
            .andExpect(jsonPath("$.data.backup.checksumPolicy").value("SHA-256 摘要随备份文件生成，恢复前自动校验"))
            .andExpect(jsonPath("$.data.domesticProfile.databaseVendors", hasItems("达梦", "人大金仓")))
            .andExpect(content().string(not(containsString("password"))))
            .andExpect(content().string(not(containsString("secret"))));
    }
}
