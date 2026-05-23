package com.medkernel.workflow;

import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowTodoService 单元测试")
class WorkflowTodoServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    private WorkflowTodoService workflowTodoService;

    @BeforeEach
    void setUp() {
        workflowTodoService = new WorkflowTodoService(persistenceService);
    }

    // ──────────────────────── 辅助方法 ────────────────────────

    private Map<String, Object> buildCreateParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("tenantId", "tenant1");
        params.put("businessType", "REVIEW");
        params.put("businessCode", "CFG-AMI-001");
        params.put("businessVersion", "1.0.0");
        params.put("title", "AMI推荐规则包审核");
        params.put("description", "配置包CFG-AMI-001 v1.0.0待审核发布");
        params.put("priority", "HIGH");
        params.put("assignedType", "ROLE");
        params.put("assignedTo", "MEDICAL_EXPERT");
        params.put("createdBy", "admin");
        params.put("dueTime", "2026-05-20T18:00:00+08:00");
        return params;
    }

    // ──────────────────────── 待办创建 ────────────────────────

    @Nested
    @DisplayName("待办创建")
    class CreateTodoTests {

        @Test
        @DisplayName("创建待办任务成功")
        void createTodoTaskSuccess() {
            Map<String, Object> result = workflowTodoService.createTodoTask(buildCreateParams());

            assertNotNull(result);
            assertTrue(((String) result.get("taskCode")).startsWith("WF-"));
            assertEquals("tenant1", result.get("tenantId"));
            assertEquals("REVIEW", result.get("businessType"));
            assertEquals("CFG-AMI-001", result.get("businessCode"));
            assertEquals("1.0.0", result.get("businessVersion"));
            assertEquals("AMI推荐规则包审核", result.get("title"));
            assertEquals("HIGH", result.get("priority"));
            assertEquals("PENDING", result.get("status"));
            assertEquals("ROLE", result.get("assignedType"));
            assertEquals("MEDICAL_EXPERT", result.get("assignedTo"));
            assertEquals("admin", result.get("createdBy"));
            assertNotNull(result.get("createdTime"));
        }

        @Test
        @DisplayName("创建待办任务使用默认值")
        void createTodoTaskWithDefaults() {
            Map<String, Object> result = workflowTodoService.createTodoTask(new HashMap<>());

            assertEquals("default", result.get("tenantId"));
            assertEquals("REVIEW", result.get("businessType"));
            assertEquals("", result.get("businessCode"));
            assertNull(result.get("businessVersion"));
            assertEquals("待办任务", result.get("title"));
            assertNull(result.get("description"));
            assertEquals("NORMAL", result.get("priority"));
            assertEquals("USER", result.get("assignedType"));
            assertNull(result.get("assignedTo"));
            assertEquals("system", result.get("createdBy"));
            assertNull(result.get("dueTime"));
        }

        @Test
        @DisplayName("创建待办任务携带组织上下文")
        void createTodoTaskWithOrgContext() {
            Map<String, Object> params = buildCreateParams();
            params.put("tenantCode", "TC001");
            params.put("groupCode", "GRP001");
            params.put("hospitalCode", "HOS001");
            params.put("campusCode", "CMP001");
            params.put("siteCode", "SITE001");
            params.put("departmentCode", "DEPT001");
            params.put("scopeLevel", "HOSPITAL");
            params.put("scopeCode", "HOS001");

            Map<String, Object> result = workflowTodoService.createTodoTask(params);

            assertEquals("TC001", result.get("tenantCode"));
            assertEquals("GRP001", result.get("groupCode"));
            assertEquals("HOS001", result.get("hospitalCode"));
            assertEquals("CMP001", result.get("campusCode"));
            assertEquals("SITE001", result.get("siteCode"));
            assertEquals("DEPT001", result.get("departmentCode"));
            assertEquals("HOSPITAL", result.get("scopeLevel"));
            assertEquals("HOS001", result.get("scopeCode"));
        }

        @Test
        @DisplayName("创建不同业务类型的待办任务")
        void createTodoTaskDifferentBusinessTypes() {
            String[] types = {"REVIEW", "PUBLISH", "ROLLBACK", "RECTIFY", "KNOWLEDGE", "COMPLIANCE", "SYNC"};
            for (String type : types) {
                Map<String, Object> params = buildCreateParams();
                params.put("businessType", type);
                Map<String, Object> result = workflowTodoService.createTodoTask(params);
                assertEquals(type, result.get("businessType"));
            }
        }
    }

    // ──────────────────────── 待办查询 ────────────────────────

    @Nested
    @DisplayName("待办查询")
    class ListTodoTests {

        @Test
        @DisplayName("查询全部待办任务")
        void listAllTodoTasks() {
            Map<String, String> filters = new HashMap<>();

            List<Map<String, Object>> result = workflowTodoService.listTodoTasks(filters);

            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("按业务类型过滤待办任务")
        void listTodoTasksFilterByBusinessType() {
            Map<String, String> filters = new HashMap<>();
            filters.put("businessType", "REVIEW");

            List<Map<String, Object>> result = workflowTodoService.listTodoTasks(filters);

            assertEquals(1, result.size());
            assertEquals("REVIEW", result.get(0).get("businessType"));
        }

        @Test
        @DisplayName("按状态过滤待办任务")
        void listTodoTasksFilterByStatus() {
            Map<String, String> filters = new HashMap<>();
            filters.put("status", "PENDING");

            List<Map<String, Object>> result = workflowTodoService.listTodoTasks(filters);

            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("非PENDING状态返回空列表")
        void listTodoTasksNonPendingStatus() {
            Map<String, String> filters = new HashMap<>();
            filters.put("status", "APPROVED");

            List<Map<String, Object>> result = workflowTodoService.listTodoTasks(filters);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("获取待办任务详情")
        void getTodoTaskDetail() {
            Map<String, Object> detail = workflowTodoService.getTodoTaskDetail("WF-20260519-001");

            assertNotNull(detail);
            assertEquals("WF-20260519-001", detail.get("taskCode"));
            assertEquals("REVIEW", detail.get("businessType"));
            assertEquals("PENDING", detail.get("status"));
            assertNotNull(detail.get("actions"));
        }
    }

    // ──────────────────────── 审批通过 ────────────────────────

    @Nested
    @DisplayName("审批通过")
    class ApproveTaskTests {

        @Test
        @DisplayName("审批通过待办任务成功")
        void approveTaskSuccess() {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("operatorId", "zheng07");
            params.put("comment", "审核通过，规则包可发布");

            Map<String, Object> result = workflowTodoService.approveTask("WF-20260519-001", params);

            assertEquals("WF-20260519-001", result.get("taskCode"));
            assertEquals("APPROVED", result.get("status"));
            assertEquals("zheng07", result.get("completedBy"));
            assertEquals("审核通过，规则包可发布", result.get("completedComment"));
            assertNotNull(result.get("completedTime"));
        }

        @Test
        @DisplayName("审批通过使用默认操作人")
        void approveTaskDefaultOperator() {
            Map<String, Object> params = new HashMap<>();

            Map<String, Object> result = workflowTodoService.approveTask("WF-001", params);

            assertEquals("system", result.get("completedBy"));
            assertEquals("审核通过", result.get("completedComment"));
        }
    }

    // ──────────────────────── 审批驳回 ────────────────────────

    @Nested
    @DisplayName("审批驳回")
    class RejectTaskTests {

        @Test
        @DisplayName("驳回待办任务成功")
        void rejectTaskSuccess() {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("operatorId", "zheng07");
            params.put("comment", "规则包存在安全隐患，需修改后重新提交");

            Map<String, Object> result = workflowTodoService.rejectTask("WF-20260519-001", params);

            assertEquals("WF-20260519-001", result.get("taskCode"));
            assertEquals("REJECTED", result.get("status"));
            assertEquals("zheng07", result.get("completedBy"));
            assertEquals("规则包存在安全隐患，需修改后重新提交", result.get("completedComment"));
            assertNotNull(result.get("completedTime"));
        }

        @Test
        @DisplayName("驳回使用默认操作人和默认意见")
        void rejectTaskDefaultOperator() {
            Map<String, Object> params = new HashMap<>();

            Map<String, Object> result = workflowTodoService.rejectTask("WF-001", params);

            assertEquals("system", result.get("completedBy"));
            assertEquals("审核驳回", result.get("completedComment"));
        }
    }

    // ──────────────────────── 转办 ────────────────────────

    @Nested
    @DisplayName("转办任务")
    class DelegateTaskTests {

        @Test
        @DisplayName("转办待办任务成功")
        void delegateTaskSuccess() {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("delegateTo", "wang05");
            params.put("operatorId", "zheng07");
            params.put("comment", "转交给王专家审核");

            Map<String, Object> result = workflowTodoService.delegateTask("WF-20260519-001", params);

            assertEquals("WF-20260519-001", result.get("taskCode"));
            assertEquals("PENDING", result.get("status"));
            assertEquals("wang05", result.get("assignedTo"));
            assertEquals("USER", result.get("assignedType"));
            assertEquals("zheng07", result.get("delegatedBy"));
            assertNotNull(result.get("delegatedTime"));
            assertEquals("转交给王专家审核", result.get("delegateComment"));
        }

        @Test
        @DisplayName("转办使用默认值")
        void delegateTaskWithDefaults() {
            Map<String, Object> params = new HashMap<>();

            Map<String, Object> result = workflowTodoService.delegateTask("WF-001", params);

            assertEquals("", result.get("assignedTo"));
            assertEquals("system", result.get("delegatedBy"));
            assertEquals("任务转办", result.get("delegateComment"));
        }
    }

    // ──────────────────────── 取消任务 ────────────────────────

    @Nested
    @DisplayName("取消任务")
    class CancelTaskTests {

        @Test
        @DisplayName("取消待办任务成功")
        void cancelTaskSuccess() {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("operatorId", "admin");
            params.put("reason", "任务已过期，自动取消");

            Map<String, Object> result = workflowTodoService.cancelTask("WF-20260519-001", params);

            assertEquals("WF-20260519-001", result.get("taskCode"));
            assertEquals("CANCELLED", result.get("status"));
            assertEquals("admin", result.get("cancelledBy"));
            assertEquals("任务已过期，自动取消", result.get("cancelReason"));
            assertNotNull(result.get("cancelledTime"));
        }

        @Test
        @DisplayName("取消使用默认值")
        void cancelTaskWithDefaults() {
            Map<String, Object> params = new HashMap<>();

            Map<String, Object> result = workflowTodoService.cancelTask("WF-001", params);

            assertEquals("system", result.get("cancelledBy"));
            assertEquals("任务取消", result.get("cancelReason"));
        }
    }

    // ──────────────────────── 加签 ────────────────────────

    @Nested
    @DisplayName("加签（增加审批人）")
    class AddSignTaskTests {

        @Test
        @DisplayName("加签成功")
        void addSignTaskSuccess() {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("addSignTo", "liu03");
            params.put("operatorId", "zheng07");
            params.put("comment", "需要刘主任会签");

            Map<String, Object> result = workflowTodoService.addSignTask("WF-20260519-001", params);

            assertEquals("WF-20260519-001", result.get("taskCode"));
            assertEquals("PENDING", result.get("status"));
            assertEquals("liu03", result.get("addedSigner"));
            assertEquals("zheng07", result.get("addedBy"));
            assertNotNull(result.get("addedTime"));
            assertEquals("需要刘主任会签", result.get("addSignComment"));
        }

        @Test
        @DisplayName("加签使用默认值")
        void addSignTaskWithDefaults() {
            Map<String, Object> params = new HashMap<>();

            Map<String, Object> result = workflowTodoService.addSignTask("WF-001", params);

            assertEquals("", result.get("addedSigner"));
            assertEquals("system", result.get("addedBy"));
            assertEquals("增加审批人", result.get("addSignComment"));
        }
    }

    // ──────────────────────── 待办统计 ────────────────────────

    @Nested
    @DisplayName("待办统计")
    class TodoSummaryTests {

        @Test
        @DisplayName("获取待办统计")
        void getTodoSummary() {
            Map<String, String> filters = new HashMap<>();

            Map<String, Object> summary = workflowTodoService.getTodoSummary(filters);

            assertEquals(3, summary.get("totalPending"));
            assertEquals(1, summary.get("urgentCount"));
            assertEquals(1, summary.get("highCount"));
            assertEquals(1, summary.get("normalCount"));
            assertEquals(0, summary.get("lowCount"));
            assertEquals(0, summary.get("overdueCount"));
        }

        @Test
        @DisplayName("待办统计包含业务类型分布")
        void getTodoSummaryByBusinessType() {
            Map<String, String> filters = new HashMap<>();

            Map<String, Object> summary = workflowTodoService.getTodoSummary(filters);

            @SuppressWarnings("unchecked")
            Map<String, Object> byType = (Map<String, Object>) summary.get("byBusinessType");
            assertEquals(1, byType.get("REVIEW"));
            assertEquals(1, byType.get("PUBLISH"));
            assertEquals(1, byType.get("SYNC"));
            assertEquals(0, byType.get("ROLLBACK"));
            assertEquals(0, byType.get("RECTIFY"));
            assertEquals(0, byType.get("KNOWLEDGE"));
            assertEquals(0, byType.get("COMPLIANCE"));
        }
    }

    // ──────────────────────── 超时处理 ────────────────────────

    @Nested
    @DisplayName("超时处理场景")
    class TimeoutHandlingTests {

        @Test
        @DisplayName("创建带截止时间的待办任务")
        void createTodoTaskWithDueTime() {
            Map<String, Object> params = buildCreateParams();
            params.put("dueTime", "2026-05-20T18:00:00+08:00");

            Map<String, Object> result = workflowTodoService.createTodoTask(params);

            assertEquals("2026-05-20T18:00:00+08:00", result.get("dueTime"));
        }

        @Test
        @DisplayName("创建无截止时间的待办任务")
        void createTodoTaskWithoutDueTime() {
            Map<String, Object> params = buildCreateParams();
            params.remove("dueTime");

            Map<String, Object> result = workflowTodoService.createTodoTask(params);

            assertNull(result.get("dueTime"));
        }

        @Test
        @DisplayName("超时任务可被取消")
        void overdueTaskCanBeCancelled() {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("operatorId", "system");
            params.put("reason", "任务超时自动取消");

            Map<String, Object> result = workflowTodoService.cancelTask("WF-OVERDUE-001", params);

            assertEquals("CANCELLED", result.get("status"));
            assertEquals("任务超时自动取消", result.get("cancelReason"));
        }
    }
}
