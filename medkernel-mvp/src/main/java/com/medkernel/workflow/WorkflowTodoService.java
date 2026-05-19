package com.medkernel.workflow;

import com.medkernel.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 统一待办服务：管理待办任务的创建、查询、完成和取消。
 * 支持审核、发布、回滚、整改、知识包、合规、同步异常等业务类型。
 */
@Service
public class WorkflowTodoService {
    private final EnginePersistenceService persistenceService;

    public WorkflowTodoService(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    /**
     * 创建待办任务
     */
    public Map<String, Object> createTodoTask(Map<String, Object> params) {
        String taskCode = "WF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String tenantId = getString(params, "tenantId", "default");
        String businessType = getString(params, "businessType", "REVIEW");
        String businessCode = getString(params, "businessCode", "");
        String businessVersion = getString(params, "businessVersion", null);
        String title = getString(params, "title", "待办任务");
        String description = getString(params, "description", null);
        String priority = getString(params, "priority", "NORMAL");
        String assignedType = getString(params, "assignedType", "USER");
        String assignedTo = getString(params, "assignedTo", null);
        String createdBy = getString(params, "createdBy", "system");
        String dueTime = getString(params, "dueTime", null);

        Map<String, Object> task = new LinkedHashMap<String, Object>();
        task.put("taskCode", taskCode);
        task.put("tenantId", tenantId);
        task.put("businessType", businessType);
        task.put("businessCode", businessCode);
        task.put("businessVersion", businessVersion);
        task.put("title", title);
        task.put("description", description);
        task.put("priority", priority);
        task.put("status", "PENDING");
        task.put("assignedType", assignedType);
        task.put("assignedTo", assignedTo);
        task.put("createdBy", createdBy);
        task.put("dueTime", dueTime);
        task.put("createdTime", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        // 组织上下文
        task.put("tenantCode", getString(params, "tenantCode", null));
        task.put("groupCode", getString(params, "groupCode", null));
        task.put("hospitalCode", getString(params, "hospitalCode", null));
        task.put("campusCode", getString(params, "campusCode", null));
        task.put("siteCode", getString(params, "siteCode", null));
        task.put("departmentCode", getString(params, "departmentCode", null));
        task.put("scopeLevel", getString(params, "scopeLevel", null));
        task.put("scopeCode", getString(params, "scopeCode", null));

        return task;
    }

    /**
     * 查询待办任务列表
     */
    public List<Map<String, Object>> listTodoTasks(Map<String, String> filters) {
        List<Map<String, Object>> tasks = new ArrayList<Map<String, Object>>();

        // 从内存中构建示例数据（后续接通持久化）
        String status = filters.get("status");
        String businessType = filters.get("businessType");
        String assignedTo = filters.get("assignedTo");

        // 示例：配置包审核待办
        if (status == null || "PENDING".equals(status)) {
            if (businessType == null || "REVIEW".equals(businessType)) {
                Map<String, Object> task1 = new LinkedHashMap<String, Object>();
                task1.put("taskCode", "WF-20260519-001");
                task1.put("businessType", "REVIEW");
                task1.put("businessCode", "CFG-AMI-001");
                task1.put("businessVersion", "1.0.0");
                task1.put("title", "AMI推荐规则包审核");
                task1.put("description", "配置包CFG-AMI-001 v1.0.0待审核发布");
                task1.put("priority", "HIGH");
                task1.put("status", "PENDING");
                task1.put("assignedType", "ROLE");
                task1.put("assignedTo", "MEDICAL_EXPERT");
                task1.put("createdBy", "admin");
                task1.put("createdTime", "2026-05-19T20:00:00+08:00");
                task1.put("dueTime", "2026-05-20T18:00:00+08:00");
                tasks.add(task1);
            }

            if (businessType == null || "PUBLISH".equals(businessType)) {
                Map<String, Object> task2 = new LinkedHashMap<String, Object>();
                task2.put("taskCode", "WF-20260519-002");
                task2.put("businessType", "PUBLISH");
                task2.put("businessCode", "PATH-CHD-001");
                task2.put("businessVersion", "2.1.0");
                task2.put("title", "儿童哮喘路径发布审批");
                task2.put("description", "路径PATH-CHD-001 v2.1.0待发布审批");
                task2.put("priority", "NORMAL");
                task2.put("status", "PENDING");
                task2.put("assignedType", "USER");
                task2.put("assignedTo", "zheng07");
                task2.put("createdBy", "zhao01");
                task2.put("createdTime", "2026-05-19T19:30:00+08:00");
                task2.put("dueTime", "2026-05-21T18:00:00+08:00");
                tasks.add(task2);
            }

            if (businessType == null || "SYNC".equals(businessType)) {
                Map<String, Object> task3 = new LinkedHashMap<String, Object>();
                task3.put("taskCode", "WF-20260519-003");
                task3.put("businessType", "SYNC");
                task3.put("businessCode", "SYNC-HIS-20260519");
                task3.put("title", "HIS用户同步异常处理");
                task3.put("description", "HIS用户同步任务失败，需要人工处理");
                task3.put("priority", "URGENT");
                task3.put("status", "PENDING");
                task3.put("assignedType", "ROLE");
                task3.put("assignedTo", "IT_ADMIN");
                task3.put("createdBy", "system");
                task3.put("createdTime", "2026-05-19T21:00:00+08:00");
                task3.put("dueTime", "2026-05-19T23:00:00+08:00");
                tasks.add(task3);
            }
        }

        return tasks;
    }

    /**
     * 获取待办任务详情
     */
    public Map<String, Object> getTodoTaskDetail(String taskCode) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("taskCode", taskCode);
        detail.put("businessType", "REVIEW");
        detail.put("businessCode", "CFG-AMI-001");
        detail.put("businessVersion", "1.0.0");
        detail.put("title", "AMI推荐规则包审核");
        detail.put("description", "配置包CFG-AMI-001 v1.0.0待审核发布");
        detail.put("priority", "HIGH");
        detail.put("status", "PENDING");
        detail.put("assignedType", "ROLE");
        detail.put("assignedTo", "MEDICAL_EXPERT");
        detail.put("createdBy", "admin");
        detail.put("createdTime", "2026-05-19T20:00:00+08:00");
        detail.put("dueTime", "2026-05-20T18:00:00+08:00");

        // 审批历史
        List<Map<String, Object>> actions = new ArrayList<Map<String, Object>>();
        Map<String, Object> action1 = new LinkedHashMap<String, Object>();
        action1.put("actionType", "SUBMIT");
        action1.put("actionResult", "SUCCESS");
        action1.put("operatorId", "admin");
        action1.put("operatorName", "系统管理员");
        action1.put("comment", "提交审核");
        action1.put("createdTime", "2026-05-19T20:00:00+08:00");
        actions.add(action1);
        detail.put("actions", actions);

        return detail;
    }

    /**
     * 完成待办任务（通过）
     */
    public Map<String, Object> approveTask(String taskCode, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("taskCode", taskCode);
        result.put("status", "APPROVED");
        result.put("completedBy", getString(params, "operatorId", "system"));
        result.put("completedTime", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        result.put("completedComment", getString(params, "comment", "审核通过"));
        return result;
    }

    /**
     * 驳回待办任务
     */
    public Map<String, Object> rejectTask(String taskCode, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("taskCode", taskCode);
        result.put("status", "REJECTED");
        result.put("completedBy", getString(params, "operatorId", "system"));
        result.put("completedTime", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        result.put("completedComment", getString(params, "comment", "审核驳回"));
        return result;
    }

    /**
     * 转办待办任务
     */
    public Map<String, Object> delegateTask(String taskCode, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("taskCode", taskCode);
        result.put("status", "PENDING");
        result.put("assignedTo", getString(params, "delegateTo", ""));
        result.put("assignedType", "USER");
        result.put("delegatedBy", getString(params, "operatorId", "system"));
        result.put("delegatedTime", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        result.put("delegateComment", getString(params, "comment", "任务转办"));
        return result;
    }

    /**
     * 取消待办任务
     */
    public Map<String, Object> cancelTask(String taskCode, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("taskCode", taskCode);
        result.put("status", "CANCELLED");
        result.put("cancelledBy", getString(params, "operatorId", "system"));
        result.put("cancelledTime", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        result.put("cancelReason", getString(params, "reason", "任务取消"));
        return result;
    }

    /**
     * 加签（增加审批人）
     */
    public Map<String, Object> addSignTask(String taskCode, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("taskCode", taskCode);
        result.put("status", "PENDING");
        result.put("addedSigner", getString(params, "addSignTo", ""));
        result.put("addedBy", getString(params, "operatorId", "system"));
        result.put("addedTime", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        result.put("addSignComment", getString(params, "comment", "增加审批人"));
        return result;
    }

    /**
     * 获取待办统计
     */
    public Map<String, Object> getTodoSummary(Map<String, String> filters) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("totalPending", 3);
        summary.put("urgentCount", 1);
        summary.put("highCount", 1);
        summary.put("normalCount", 1);
        summary.put("lowCount", 0);
        summary.put("overdueCount", 0);

        Map<String, Object> byType = new LinkedHashMap<String, Object>();
        byType.put("REVIEW", 1);
        byType.put("PUBLISH", 1);
        byType.put("SYNC", 1);
        byType.put("ROLLBACK", 0);
        byType.put("RECTIFY", 0);
        byType.put("KNOWLEDGE", 0);
        byType.put("COMPLIANCE", 0);
        summary.put("byBusinessType", byType);

        return summary;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }
}
