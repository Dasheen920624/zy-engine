package com.medkernel.ops.controller;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.ops.entity.OpsSyncTask;
import com.medkernel.ops.service.OpsSyncTaskService;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 异步任务控制器：提供异步任务管理的 REST API
 *
 * <p>API 端点：
 * <ul>
 *   <li>GET  /api/ops/tasks - 获取任务列表</li>
 *   <li>GET  /api/ops/tasks/{id} - 获取任务详情</li>
 *   <li>POST /api/ops/tasks - 创建任务</li>
 *   <li>POST /api/ops/tasks/{id}/cancel - 取消任务</li>
 *   <li>POST /api/ops/tasks/{id}/retry - 重试任务</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ops/tasks")
public class OpsSyncTaskController {

    private final OpsSyncTaskService taskService;
    private final OrganizationContextService organizationContextService;

    public OpsSyncTaskController(OpsSyncTaskService taskService,
                                  OrganizationContextService organizationContextService) {
        this.taskService = taskService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 获取任务列表
     *
     * @param status   可选，按状态筛选
     * @param taskType 可选，按任务类型筛选
     * @param request  HTTP请求
     * @return 任务列表
     */
    @GetMapping
    public ApiResult<List<OpsSyncTask>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskType,
            HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);

        List<OpsSyncTask> tasks;
        if (status != null && !status.isEmpty()) {
            tasks = taskService.listTasksByStatus(tenantId, status);
        } else if (taskType != null && !taskType.isEmpty()) {
            tasks = taskService.listTasksByType(tenantId, taskType);
        } else {
            tasks = taskService.listTasks(tenantId);
        }

        return ApiResult.success(tasks);
    }

    /**
     * 获取任务详情
     *
     * @param id      任务ID
     * @param request HTTP请求
     * @return 任务详情
     */
    @GetMapping("/{id}")
    public ApiResult<OpsSyncTask> get(@PathVariable Long id, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        OpsSyncTask task = taskService.getTask(tenantId, id);

        if (task == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Task not found: " + id);
        }

        return ApiResult.success(task);
    }

    /**
     * 创建任务
     *
     * @param request   HTTP请求
     * @param taskParam 任务参数
     * @return 创建的任务
     */
    @PostMapping
    public ApiResult<OpsSyncTask> create(HttpServletRequest request,
                                          @RequestBody CreateTaskParam taskParam) {
        Long tenantId = organizationContextService.getTenantId(request);

        // 参数校验
        if (taskParam.getTaskCode() == null || taskParam.getTaskCode().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Task code is required");
        }
        if (taskParam.getTaskType() == null || taskParam.getTaskType().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "Task type is required");
        }

        // 检查任务编码是否已存在
        OpsSyncTask existing = taskService.getTaskByCode(tenantId, taskParam.getTaskCode());
        if (existing != null) {
            return ApiResult.failure(ErrorCode.CONFLICT, "Task code already exists: " + taskParam.getTaskCode());
        }

        OpsSyncTask task = taskService.createTask(
                tenantId,
                taskParam.getTaskCode(),
                taskParam.getTaskType(),
                taskParam.getMaxRetries(),
                taskParam.getScheduledTime()
        );

        return ApiResult.success(task);
    }

    /**
     * 取消任务
     *
     * @param id      任务ID
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);

        try {
            taskService.cancelTask(tenantId, id);
            return ApiResult.success(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    /**
     * 重试任务
     *
     * @param id      任务ID
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/{id}/retry")
    public ApiResult<Void> retry(@PathVariable Long id, HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);

        try {
            taskService.retryTask(tenantId, id);
            return ApiResult.success(null);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    // ==================== 请求参数类 ====================

    /**
     * 创建任务请求参数
     */
    public static class CreateTaskParam {
        /** 任务编码 */
        private String taskCode;

        /** 任务类型 */
        private String taskType;

        /** 最大重试次数，默认3 */
        private Integer maxRetries;

        /** 计划执行时间（null表示立即执行） */
        private java.time.LocalDateTime scheduledTime;

        public String getTaskCode() {
            return taskCode;
        }

        public void setTaskCode(String taskCode) {
            this.taskCode = taskCode;
        }

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public java.time.LocalDateTime getScheduledTime() {
            return scheduledTime;
        }

        public void setScheduledTime(java.time.LocalDateTime scheduledTime) {
            this.scheduledTime = scheduledTime;
        }
    }
}
