package com.medkernel.workflow;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一待办和审批工作流Controller
 * 提供待办任务的创建、查询、审批、驳回、转办、取消、加签等接口
 */
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {
    private final WorkflowTodoService workflowTodoService;
    private final OrganizationContextService organizationContextService;

    public WorkflowController(WorkflowTodoService workflowTodoService,
                              OrganizationContextService organizationContextService) {
        this.workflowTodoService = workflowTodoService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 查询待办任务列表
     */
    @GetMapping("/todos")
    public ApiResult<List<Map<String, Object>>> listTodos(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String priority,
            HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("status", status);
        filters.put("businessType", businessType);
        filters.put("assignedTo", assignedTo);
        filters.put("priority", priority);
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(workflowTodoService.listTodoTasks(filters));
    }

    /**
     * 获取待办任务详情
     */
    @GetMapping("/todos/{taskCode}")
    public ApiResult<Map<String, Object>> getTodoDetail(@PathVariable String taskCode,
                                                        HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("taskCode", taskCode);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(workflowTodoService.getTodoTaskDetail(taskCode));
    }

    /**
     * 获取待办统计
     */
    @GetMapping("/todos/summary")
    public ApiResult<Map<String, Object>> getTodoSummary(
            @RequestParam(required = false) String assignedTo,
            HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("assignedTo", assignedTo);
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(workflowTodoService.getTodoSummary(filters));
    }

    /**
     * 创建待办任务
     */
    @PostMapping("/todos")
    public ApiResult<Map<String, Object>> createTodo(@RequestBody Map<String, Object> body,
                                                     HttpServletRequest request) {
        organizationContextService.applyExplicitFilters(body, request);
        return ApiResult.success(workflowTodoService.createTodoTask(body));
    }

    /**
     * 审批通过
     */
    @PostMapping("/todos/{taskCode}/approve")
    public ApiResult<Map<String, Object>> approveTask(@PathVariable String taskCode,
                                                      @RequestBody Map<String, Object> body,
                                                      HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, body);
        return ApiResult.success(workflowTodoService.approveTask(taskCode, body));
    }

    /**
     * 驳回
     */
    @PostMapping("/todos/{taskCode}/reject")
    public ApiResult<Map<String, Object>> rejectTask(@PathVariable String taskCode,
                                                     @RequestBody Map<String, Object> body,
                                                     HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, body);
        return ApiResult.success(workflowTodoService.rejectTask(taskCode, body));
    }

    /**
     * 转办
     */
    @PostMapping("/todos/{taskCode}/delegate")
    public ApiResult<Map<String, Object>> delegateTask(@PathVariable String taskCode,
                                                       @RequestBody Map<String, Object> body,
                                                       HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, body);
        return ApiResult.success(workflowTodoService.delegateTask(taskCode, body));
    }

    /**
     * 取消
     */
    @PostMapping("/todos/{taskCode}/cancel")
    public ApiResult<Map<String, Object>> cancelTask(@PathVariable String taskCode,
                                                     @RequestBody Map<String, Object> body,
                                                     HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, body);
        return ApiResult.success(workflowTodoService.cancelTask(taskCode, body));
    }

    /**
     * 加签（增加审批人）
     */
    @PostMapping("/todos/{taskCode}/add-sign")
    public ApiResult<Map<String, Object>> addSignTask(@PathVariable String taskCode,
                                                      @RequestBody Map<String, Object> body,
                                                      HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, body);
        return ApiResult.success(workflowTodoService.addSignTask(taskCode, body));
    }
}
