package com.medkernel.security.usersync;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.security.usersync.entity.SyncLog;
import com.medkernel.security.usersync.entity.SyncSource;
import com.medkernel.security.usersync.entity.SyncTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户同步REST接口
 * 支持 HIS/EMR/OA/统一身份平台用户同步管理
 */
@RestController
@RequestMapping("/api/user-sync")
public class UserSyncApiController {

    private final UserSyncApiService userSyncApiService;
    private final OrganizationContextService organizationContextService;

    public UserSyncApiController(UserSyncApiService userSyncApiService,
                                 OrganizationContextService organizationContextService) {
        this.userSyncApiService = userSyncApiService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 列出所有同步源
     */
    @GetMapping("/sources")
    public ApiResult<List<Map<String, Object>>> listSources(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SyncSource source : userSyncApiService.listSources(toLong(orgCtx.getTenantId()))) {
            result.add(toSourceView(source));
        }
        return ApiResult.success(result);
    }

    /**
     * 获取同步源详情
     */
    @GetMapping("/sources/{sourceId}")
    public ApiResult<Map<String, Object>> getSource(@PathVariable Long sourceId,
                                                    HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        SyncSource source = userSyncApiService.getSource(toLong(orgCtx.getTenantId()), sourceId);
        if (source == null) {
            return ApiResult.notFound("Sync source not found");
        }
        return ApiResult.success(toSourceView(source));
    }

    /**
     * 创建同步源
     */
    @PostMapping("/sources")
    public ApiResult<Map<String, Object>> createSource(@RequestBody SyncSource source,
                                                       HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        source.setTenantId(toLong(orgCtx.getTenantId()));
        return ApiResult.success(toSourceView(userSyncApiService.createSource(source)));
    }

    /**
     * 更新同步源
     */
    @PostMapping("/sources/{sourceId}")
    public ApiResult<Map<String, Object>> updateSource(@PathVariable Long sourceId,
                                                       @RequestBody SyncSource source,
                                                       HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = toLong(orgCtx.getTenantId());
        SyncSource existing = userSyncApiService.getSource(tenantId, sourceId);
        if (existing == null) {
            return ApiResult.notFound("Sync source not found");
        }
        source.setId(sourceId);
        source.setTenantId(tenantId);
        if (source.getSourceCode() == null) source.setSourceCode(existing.getSourceCode());
        if (source.getSourceName() == null) source.setSourceName(existing.getSourceName());
        if (source.getSourceType() == null) source.setSourceType(existing.getSourceType());
        if (source.getConnectionConfig() == null) source.setConnectionConfig(existing.getConnectionConfig());
        if (source.getSyncMode() == null) source.setSyncMode(existing.getSyncMode());
        if (source.getCronExpression() == null) source.setCronExpression(existing.getCronExpression());
        if (source.getStatus() == null) source.setStatus(existing.getStatus());
        if (source.getDescription() == null) source.setDescription(existing.getDescription());
        return ApiResult.success(toSourceView(userSyncApiService.updateSource(source)));
    }

    /**
     * 列出所有同步任务
     */
    @GetMapping("/tasks")
    public ApiResult<List<Map<String, Object>>> listTasks(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SyncTask task : userSyncApiService.listTasks(toLong(orgCtx.getTenantId()))) {
            result.add(toTaskView(task));
        }
        return ApiResult.success(result);
    }

    /**
     * 获取同步任务详情
     */
    @GetMapping("/tasks/{taskId}")
    public ApiResult<Map<String, Object>> getTask(@PathVariable Long taskId,
                                                  HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        SyncTask task = userSyncApiService.getTask(toLong(orgCtx.getTenantId()), taskId);
        if (task == null) {
            return ApiResult.notFound("Sync task not found");
        }
        return ApiResult.success(toTaskView(task));
    }

    /**
     * 手动触发同步任务
     */
    @PostMapping("/sources/{sourceId}/sync")
    public ApiResult<Map<String, Object>> triggerSync(@PathVariable Long sourceId,
                                                      @RequestBody Map<String, Object> request,
                                                      HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        String taskType = (String) request.getOrDefault("task_type", "MANUAL");

        // 获取外部用户数据（模拟）
        List<UserSyncApiService.ExternalUser> externalUsers = getExternalUsers(request);

        SyncTask task = userSyncApiService.executeSync(
                toLong(orgCtx.getTenantId()), sourceId, taskType, externalUsers);
        return ApiResult.success(toTaskView(task));
    }

    /**
     * 获取同步任务日志
     */
    @GetMapping("/tasks/{taskId}/logs")
    public ApiResult<List<Map<String, Object>>> listTaskLogs(@PathVariable Long taskId,
                                                             HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SyncLog log : userSyncApiService.listLogs(toLong(orgCtx.getTenantId()), taskId)) {
            result.add(toLogView(log));
        }
        return ApiResult.success(result);
    }

    /**
     * 从请求中解析外部用户数据（模拟）
     */
    @SuppressWarnings("unchecked")
    private List<UserSyncApiService.ExternalUser> getExternalUsers(Map<String, Object> request) {
        List<Map<String, Object>> usersData = (List<Map<String, Object>>) request.get("users");
        if (usersData == null) {
            return Collections.emptyList();
        }

        return usersData.stream()
                .map(userData -> new UserSyncApiService.ExternalUser(
                        (String) userData.get("external_id"),
                        (String) userData.get("username"),
                        (String) userData.get("display_name"),
                        (String) userData.get("email"),
                        (String) userData.get("phone"),
                        (String) userData.get("department"),
                        (String) userData.get("position")))
                .collect(Collectors.toList());
    }

    private static Long toLong(String value) {
        if (value == null || value.isEmpty()) return null;
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return null; }
    }

    private Map<String, Object> toSourceView(SyncSource source) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", source.getId());
        view.put("tenant_id", source.getTenantId());
        view.put("source_code", source.getSourceCode());
        view.put("source_name", source.getSourceName());
        view.put("source_type", source.getSourceType());
        view.put("connection_config", source.getConnectionConfig());
        view.put("sync_mode", source.getSyncMode());
        view.put("cron_expression", source.getCronExpression());
        view.put("status", source.getStatus());
        view.put("description", source.getDescription());
        view.put("last_sync_time", source.getLastSyncTime());
        view.put("created_time", source.getCreatedTime());
        view.put("updated_time", source.getUpdatedTime());
        view.put("trace_id", com.medkernel.common.TraceContext.getTraceId());
        return view;
    }

    private Map<String, Object> toTaskView(SyncTask task) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", task.getId());
        view.put("tenant_id", task.getTenantId());
        view.put("source_id", task.getSourceId());
        view.put("task_type", task.getTaskType());
        view.put("status", task.getStatus());
        view.put("total_count", task.getTotalCount());
        view.put("success_count", task.getSuccessCount());
        view.put("failed_count", task.getFailedCount());
        view.put("skip_count", task.getSkipCount());
        view.put("start_time", task.getStartTime());
        view.put("end_time", task.getEndTime());
        view.put("error_message", task.getErrorMessage());
        view.put("triggered_by", task.getTriggeredBy());
        view.put("created_time", task.getCreatedTime());
        view.put("updated_time", task.getUpdatedTime());
        view.put("trace_id", com.medkernel.common.TraceContext.getTraceId());
        return view;
    }

    private Map<String, Object> toLogView(SyncLog log) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", log.getId());
        view.put("tenant_id", log.getTenantId());
        view.put("task_id", log.getTaskId());
        view.put("external_id", log.getExternalId());
        view.put("external_username", log.getExternalUsername());
        view.put("platform_user_id", log.getPlatformUserId());
        view.put("operation", log.getOperation());
        view.put("status", log.getStatus());
        view.put("error_message", log.getErrorMessage());
        view.put("created_time", log.getCreatedTime());
        return view;
    }
}
