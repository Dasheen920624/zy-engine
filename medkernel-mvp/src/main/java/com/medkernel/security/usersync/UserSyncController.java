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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户同步REST接口
 * 支持 HIS/EMR/OA/统一身份平台用户同步管理
 */
@RestController
@RequestMapping("/api/user-sync")
public class UserSyncController {

    private final UserSyncService userSyncService;
    private final OrganizationContextService organizationContextService;

    public UserSyncController(UserSyncService userSyncService,
                              OrganizationContextService organizationContextService) {
        this.userSyncService = userSyncService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 列出所有同步源
     */
    @GetMapping("/sources")
    public ApiResult<List<SyncSource>> listSources(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(userSyncService.listSources(toLong(orgCtx.getTenantId())));
    }

    /**
     * 获取同步源详情
     */
    @GetMapping("/sources/{sourceId}")
    public ApiResult<SyncSource> getSource(@PathVariable Long sourceId,
                                           HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        SyncSource source = userSyncService.getSource(toLong(orgCtx.getTenantId()), sourceId);
        if (source == null) {
            return ApiResult.notFound("Sync source not found");
        }
        return ApiResult.success(source);
    }

    /**
     * 创建同步源
     */
    @PostMapping("/sources")
    public ApiResult<SyncSource> createSource(@RequestBody SyncSource source,
                                              HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        source.setTenantId(toLong(orgCtx.getTenantId()));
        return ApiResult.success(userSyncService.createSource(source));
    }

    /**
     * 更新同步源
     */
    @PostMapping("/sources/{sourceId}")
    public ApiResult<SyncSource> updateSource(@PathVariable Long sourceId,
                                              @RequestBody SyncSource source,
                                              HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        source.setId(sourceId);
        source.setTenantId(toLong(orgCtx.getTenantId()));
        return ApiResult.success(userSyncService.updateSource(source));
    }

    /**
     * 列出所有同步任务
     */
    @GetMapping("/tasks")
    public ApiResult<List<SyncTask>> listTasks(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(userSyncService.listTasks(toLong(orgCtx.getTenantId())));
    }

    /**
     * 获取同步任务详情
     */
    @GetMapping("/tasks/{taskId}")
    public ApiResult<SyncTask> getTask(@PathVariable Long taskId,
                                       HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        SyncTask task = userSyncService.getTask(toLong(orgCtx.getTenantId()), taskId);
        if (task == null) {
            return ApiResult.notFound("Sync task not found");
        }
        return ApiResult.success(task);
    }

    /**
     * 手动触发同步任务
     */
    @PostMapping("/sources/{sourceId}/sync")
    public ApiResult<SyncTask> triggerSync(@PathVariable Long sourceId,
                                           @RequestBody Map<String, Object> request,
                                           HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        String taskType = (String) request.getOrDefault("taskType", "MANUAL");

        // 获取外部用户数据（模拟）
        List<UserSyncService.ExternalUser> externalUsers = getExternalUsers(request);

        SyncTask task = userSyncService.executeSync(
                toLong(orgCtx.getTenantId()), sourceId, taskType, externalUsers);
        return ApiResult.success(task);
    }

    /**
     * 获取同步任务日志
     */
    @GetMapping("/tasks/{taskId}/logs")
    public ApiResult<List<SyncLog>> listTaskLogs(@PathVariable Long taskId,
                                                  HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(userSyncService.listLogs(toLong(orgCtx.getTenantId()), taskId));
    }

    /**
     * 从请求中解析外部用户数据（模拟）
     */
    @SuppressWarnings("unchecked")
    private List<UserSyncService.ExternalUser> getExternalUsers(Map<String, Object> request) {
        List<Map<String, Object>> usersData = (List<Map<String, Object>>) request.get("users");
        if (usersData == null) {
            return Collections.emptyList();
        }

        return usersData.stream()
                .map(userData -> new UserSyncService.ExternalUser(
                        (String) userData.get("externalId"),
                        (String) userData.get("username"),
                        (String) userData.get("displayName"),
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
}
