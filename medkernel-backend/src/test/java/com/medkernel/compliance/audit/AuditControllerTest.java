package com.medkernel.compliance.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.medkernel.shared.audit.persistence.AuditQueryService;
import com.medkernel.shared.context.RequestContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller 安全 + DataScope 矩阵：
 *
 * <ul>
 *   <li>{@code GET /events}：{@code audit.read} 权限拥有者通过 PreAuthorize，缺租户被 DataScope 拒</li>
 *   <li>{@code GET /events}：无 {@code audit.read} 的角色（医生）直接 403</li>
 *   <li>{@code POST /snapshot}：{@code audit.export} 拥有者通过 PreAuthorize，缺租户被 DataScope 拒</li>
 *   <li>{@code POST /snapshot}：无 {@code audit.export} 的角色（医生）直接 403</li>
 * </ul>
 *
 * <p>{@link AuditQueryService} 被 @MockBean 替换，避免数据库依赖；这里只验证 Controller 层契约。
 * 端到端的链路 / 落库正确性已由 {@code AuditChainWriterTest} 覆盖。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuditControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AuditQueryService queryService;

    @AfterEach
    void clearAll() {
        RequestContext.clear();
    }

    @Test
    @WithMockUser(authorities = "ROLE_AUDIT_COMPLIANCE")
    void auditComplianceHasReadButDataScopeRejectsMissingTenant() throws Exception {
        mvc.perform(get("/api/v1/compliance/audit/events"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENG-BASE-001"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_DOCTOR")
    void doctorIsForbiddenFromReadingAudit() throws Exception {
        mvc.perform(get("/api/v1/compliance/audit/events"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_GUEST")
    void guestIsForbiddenFromReadingAudit() throws Exception {
        mvc.perform(get("/api/v1/compliance/audit/events"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_AUDIT_COMPLIANCE")
    void auditComplianceCanReachSnapshotButDataScopeFails() throws Exception {
        mvc.perform(post("/api/v1/compliance/audit/snapshot?reason=test"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("ENG-BASE-001"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_DOCTOR")
    void doctorCannotExportSnapshot() throws Exception {
        mvc.perform(post("/api/v1/compliance/audit/snapshot?reason=test"))
            .andExpect(status().isForbidden());
    }
}
