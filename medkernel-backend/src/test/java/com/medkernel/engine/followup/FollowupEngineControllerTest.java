package com.medkernel.engine.followup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.medkernel.shared.context.RequestContext;

/**
 * 随访引擎 Controller 功能性集成测试。
 *
 * <p>覆盖以下端点的正常路径与校验拒绝路径：
 * <ul>
 *   <li>{@code POST /api/v1/engine/followup/plans/generate} — 生成随访计划</li>
 *   <li>{@code GET /api/v1/engine/followup/plans/{planId}} — 查询计划详情</li>
 *   <li>{@code POST /api/v1/engine/followup/tasks/{taskId}/questionnaires} — 提交随访问卷</li>
 *   <li>{@code POST /api/v1/engine/followup/events/report-abnormal} — 上报异常回院事件</li>
 * </ul>
 *
 * <p>{@link FollowupEngineService} 被 {@code @MockBean} 替换，避免数据库依赖；
 * 这里只验证 Controller 层契约（请求路由、权限、参数校验、响应结构）。
 *
 * <p>权限模型说明：{@code @perm.has('followup.write')} 基于角色推导；
 * 测试中使用 {@code ROLE_MEDICAL_AFFAIRS}（医务处，拥有 followup.write）和
 * {@code ROLE_DOCTOR}（临床医生，仅拥有 followup.read）来驱动权限判断。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class FollowupEngineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FollowupEngineService service;

    @AfterEach
    void clearContext() {
        RequestContext.clear();
    }

    // ── 1. POST /plans/generate ────────────────────────────────────────

    private static final String GENERATE_BODY = """
        {
          "patientId": "P1001",
          "encounterId": "E2001",
          "pathwayId": "PATH01",
          "diseaseCode": "I21.900",
          "riskLevel": "HIGH",
          "taskTypes": ["QUESTIONNAIRE", "OUTPATIENT"]
        }
        """;

    @Test
    @WithMockUser(authorities = {"ROLE_MEDICAL_AFFAIRS"})
    void generatePlan_ReturnsOkWithPlanDetail() throws Exception {
        FollowupPlanDetailResponse mockResponse = new FollowupPlanDetailResponse(
            "PLAN-001", "tenant-1", "P1001", "E2001", "I21.900",
            FollowupPlanStatus.ACTIVE,
            List.of(
                new FollowupTaskDetailResponse("TASK-001", FollowupTaskType.QUESTIONNAIRE, null, FollowupTaskStatus.PENDING),
                new FollowupTaskDetailResponse("TASK-002", FollowupTaskType.OUTPATIENT, null, FollowupTaskStatus.PENDING)
            )
        );
        when(service.generatePlan(any(FollowupPlanGenerateRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/engine/followup/plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(GENERATE_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.planId").value("PLAN-001"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.tasks.length()").value(2))
            .andExpect(jsonPath("$.data.tasks[0].taskType").value("QUESTIONNAIRE"));

        verify(service).generatePlan(any(FollowupPlanGenerateRequest.class));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_MEDICAL_AFFAIRS"})
    void generatePlan_MissingPatientId_ReturnsBadRequest() throws Exception {
        String bodyMissingPatientId = """
            {
              "encounterId": "E2001",
              "taskTypes": ["QUESTIONNAIRE"]
            }
            """;

        mockMvc.perform(post("/api/v1/engine/followup/plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyMissingPatientId))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_MEDICAL_AFFAIRS"})
    void generatePlan_MissingTaskTypes_ReturnsBadRequest() throws Exception {
        String bodyMissingTaskTypes = """
            {
              "patientId": "P1001",
              "encounterId": "E2001"
            }
            """;

        mockMvc.perform(post("/api/v1/engine/followup/plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyMissingTaskTypes))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_DOCTOR"})
    void generatePlan_DoctorLacksWritePermission_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/engine/followup/plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(GENERATE_BODY))
            .andExpect(status().isForbidden());
    }

    // ── 2. GET /plans/{planId} ─────────────────────────────────────────

    @Test
    @WithMockUser(authorities = {"ROLE_DOCTOR"})
    void getPlanDetail_ReturnsOkWithPlanDetail() throws Exception {
        FollowupPlanDetailResponse mockResponse = new FollowupPlanDetailResponse(
            "PLAN-001", "tenant-1", "P1001", "E2001", "I21.900",
            FollowupPlanStatus.ACTIVE,
            List.of(
                new FollowupTaskDetailResponse("TASK-001", FollowupTaskType.QUESTIONNAIRE, null, FollowupTaskStatus.PENDING)
            )
        );
        when(service.getPlanDetail("PLAN-001")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/engine/followup/plans/PLAN-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.planId").value("PLAN-001"))
            .andExpect(jsonPath("$.data.patientId").value("P1001"))
            .andExpect(jsonPath("$.data.tasks.length()").value(1));

        verify(service).getPlanDetail("PLAN-001");
    }

    @Test
    void getPlanDetail_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/engine/followup/plans/PLAN-001"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GUEST_INVALID"})
    void getPlanDetail_WithUnrecognizedRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/engine/followup/plans/PLAN-001"))
            .andExpect(status().isForbidden());
    }

    // ── 3. POST /tasks/{taskId}/questionnaires ─────────────────────────

    private static final String QUESTIONNAIRE_BODY = """
        {
          "taskId": "TASK-001",
          "formData": "{\\"q1\\": \\"yes\\", \\"q2\\": \\"no\\"}",
          "executorId": "DOCTOR-001",
          "executorType": "PHYSICIAN"
        }
        """;

    @Test
    @WithMockUser(authorities = {"ROLE_MEDICAL_AFFAIRS"})
    void submitQuestionnaire_ReturnsOk() throws Exception {
        doNothing().when(service).submitQuestionnaire(eq("TASK-001"), any(FollowupQuestionnaireSubmitRequest.class));

        mockMvc.perform(post("/api/v1/engine/followup/tasks/TASK-001/questionnaires")
                .contentType(MediaType.APPLICATION_JSON)
                .content(QUESTIONNAIRE_BODY))
            .andExpect(status().isOk());

        verify(service).submitQuestionnaire(eq("TASK-001"), any(FollowupQuestionnaireSubmitRequest.class));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_MEDICAL_AFFAIRS"})
    void submitQuestionnaire_MissingFormData_ReturnsBadRequest() throws Exception {
        String bodyMissingFormData = """
            {
              "taskId": "TASK-001"
            }
            """;

        mockMvc.perform(post("/api/v1/engine/followup/tasks/TASK-001/questionnaires")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyMissingFormData))
            .andExpect(status().isBadRequest());
    }

    // ── 4. POST /events/report-abnormal ────────────────────────────────

    private static final String ABNORMAL_BODY = """
        {
          "planId": "PLAN-001",
          "eventType": "ABNORMAL_RETURN",
          "payload": "{\\"reason\\": \\"血压异常升高\\"}",
          "triggeredBy": "FOLLOWUP_NURSE_001"
        }
        """;

    @Test
    @WithMockUser(authorities = {"ROLE_MEDICAL_AFFAIRS"})
    void reportAbnormal_ReturnsOk() throws Exception {
        doNothing().when(service).reportAbnormal(any(FollowupAbnormalReportRequest.class));

        mockMvc.perform(post("/api/v1/engine/followup/events/report-abnormal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ABNORMAL_BODY))
            .andExpect(status().isOk());

        verify(service).reportAbnormal(any(FollowupAbnormalReportRequest.class));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_MEDICAL_AFFAIRS"})
    void reportAbnormal_MissingPlanId_ReturnsBadRequest() throws Exception {
        String bodyMissingPlanId = """
            {
              "eventType": "ABNORMAL_RETURN",
              "payload": "{\\"reason\\": \\"test\\"}"
            }
            """;

        mockMvc.perform(post("/api/v1/engine/followup/events/report-abnormal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyMissingPlanId))
            .andExpect(status().isBadRequest());
    }

    @Test
    void reportAbnormal_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/engine/followup/events/report-abnormal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ABNORMAL_BODY))
            .andExpect(status().isUnauthorized());
    }
}
