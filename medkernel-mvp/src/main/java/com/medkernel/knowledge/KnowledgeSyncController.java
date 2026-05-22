package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 医疗知识同步 API：同步触发、差异预览、审核、重试、取消。
 *
 * <p>API 说明：
 * <ul>
 *   <li>POST /api/knowledge/sync - 手动触发同步</li>
 *   <li>POST /api/knowledge/sync/auto - 触发自动同步</li>
 *   <li>GET /api/knowledge/sync/{logId} - 查询同步详情</li>
 *   <li>GET /api/knowledge/sync - 查询同步列表</li>
 *   <li>POST /api/knowledge/sync/{logId}/preview - 差异预览</li>
 *   <li>POST /api/knowledge/sync/{logId}/review - 审核差异</li>
 *   <li>POST /api/knowledge/sync/{logId}/approve - 审核通过后执行</li>
 *   <li>POST /api/knowledge/sync/{logId}/retry - 重试失败同步</li>
 *   <li>POST /api/knowledge/sync/{logId}/cancel - 取消同步</li>
 *   <li>GET /api/knowledge/sync/summary - 同步统计</li>
 * </ul>
 */
@Tag(name = "Knowledge Sync")
@RestController
@RequestMapping("/api/knowledge/sync")
public class KnowledgeSyncController {

    private final KnowledgeSyncService syncService;
    private final OrganizationContextService organizationContextService;

    public KnowledgeSyncController(KnowledgeSyncService syncService,
                                    OrganizationContextService organizationContextService) {
        this.syncService = syncService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 手动触发同步。
     *
     * <p>请求体：
     * <pre>
     * {
     *   "source_code": "KS-0001",
     *   "subscription_id": "SUB-0001",  // 可选
     *   "sync_mode": "FULL"             // FULL / INCREMENTAL / DRY_RUN
     * }
     * </pre>
     */
    @Operation(summary = "Trigger sync")
    @PostMapping
    public ApiResult<KnowledgeSyncLog> triggerSync(@RequestBody Map<String, Object> body,
                                                    HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = resolveTenantId(orgCtx);

        String sourceCode = (String) body.get("source_code");
        String subscriptionId = (String) body.get("subscription_id");
        String syncMode = (String) body.get("sync_mode");

        if (sourceCode == null || sourceCode.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "来源编码不能为空");
        }
        if (syncMode == null || syncMode.isEmpty()) {
            syncMode = KnowledgeSyncLog.SYNC_MODE_FULL;
        }
        if (!KnowledgeSyncLog.SYNC_MODE_FULL.equals(syncMode)
                && !KnowledgeSyncLog.SYNC_MODE_INCREMENTAL.equals(syncMode)
                && !KnowledgeSyncLog.SYNC_MODE_DRY_RUN.equals(syncMode)) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR,
                    "同步模式必须为 FULL、INCREMENTAL 或 DRY_RUN");
        }

        String triggeredBy = com.medkernel.common.TraceContext.getUsername() != null ? com.medkernel.common.TraceContext.getUsername() : "system";
        KnowledgeSyncLog log = syncService.triggerManualSync(
                tenantId, sourceCode, subscriptionId, syncMode, triggeredBy);
        return ApiResult.success(log);
    }

    /**
     * 触发自动同步（定时任务调用）。
     */
    @Operation(summary = "Trigger auto sync")
    @PostMapping("/auto")
    public ApiResult<List<KnowledgeSyncLog>> triggerAutoSync(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = resolveTenantId(orgCtx);
        String triggeredBy = "SCHEDULER";
        List<KnowledgeSyncLog> logs = syncService.triggerAutoSync(tenantId, triggeredBy);
        return ApiResult.success(logs);
    }

    /**
     * 查询同步详情。
     */
    @Operation(summary = "Get sync log")
    @GetMapping("/{logId}")
    public ApiResult<KnowledgeSyncLog> getSyncLog(@PathVariable Long logId) {
        KnowledgeSyncLog log = syncService.getSyncLog(logId);
        if (log == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "同步日志不存在");
        }
        return ApiResult.success(log);
    }

    /**
     * 查询同步列表。
     *
     * @param sourceCode   来源编码过滤（可选）
     * @param status       状态过滤（可选）
     * @param reviewStatus 审核状态过滤（可选）
     * @param limit        返回条数上限（默认50）
     */
    @Operation(summary = "List sync logs")
    @GetMapping
    public ApiResult<List<KnowledgeSyncLog>> listSyncLogs(
            @RequestParam(required = false) String sourceCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false, defaultValue = "50") int limit,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = resolveTenantId(orgCtx);
        List<KnowledgeSyncLog> logs = syncService.listSyncLogs(
                tenantId, sourceCode, status, reviewStatus, limit);
        return ApiResult.success(logs);
    }

    /**
     * 差异预览。
     * 对 PENDING 或 DIFF_READY 状态的同步执行差异分析。
     */
    @Operation(summary = "Preview diff")
    @PostMapping("/{logId}/preview")
    public ApiResult<KnowledgeSyncLog> previewDiff(@PathVariable Long logId) {
        try {
            KnowledgeSyncLog log = syncService.previewDiff(logId);
            return ApiResult.success(log);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    /**
     * 审核差异。
     *
     * <p>请求体：
     * <pre>
     * {
     *   "reviewStatus": "APPROVED",   // APPROVED / REJECTED
     *   "reviewedBy": "张医生",
     *   "reviewComment": "内容准确，可通过"
     * }
     * </pre>
     */
    @Operation(summary = "Review sync")
    @PostMapping("/{logId}/review")
    public ApiResult<String> reviewSync(@PathVariable Long logId,
                                         @RequestBody Map<String, String> body) {
        String reviewStatus = body.get("reviewStatus");
        String reviewedBy = body.get("reviewedBy") != null ? body.get("reviewedBy") : "system";
        String reviewComment = body.get("reviewComment");

        if (reviewStatus == null || reviewStatus.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "审核状态不能为空");
        }

        try {
            syncService.reviewSync(logId, reviewStatus, reviewedBy, reviewComment);
            return ApiResult.success("审核成功");
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    /**
     * 审核通过后执行实际同步。
     */
    @Operation(summary = "Approve and execute")
    @PostMapping("/{logId}/approve")
    public ApiResult<KnowledgeSyncLog> approveAndExecute(@PathVariable Long logId) {
        try {
            KnowledgeSyncLog log = syncService.executeApprovedSync(logId);
            return ApiResult.success(log);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    /**
     * 重试失败的同步。
     */
    @Operation(summary = "Retry sync")
    @PostMapping("/{logId}/retry")
    public ApiResult<KnowledgeSyncLog> retrySync(@PathVariable Long logId) {
        try {
            KnowledgeSyncLog log = syncService.retrySync(logId);
            return ApiResult.success(log);
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    /**
     * 取消同步。
     */
    @Operation(summary = "Cancel sync")
    @PostMapping("/{logId}/cancel")
    public ApiResult<String> cancelSync(@PathVariable Long logId,
                                         HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        String cancelledBy = com.medkernel.common.TraceContext.getUsername() != null ? com.medkernel.common.TraceContext.getUsername() : "system";

        try {
            syncService.cancelSync(logId, cancelledBy);
            return ApiResult.success("同步已取消");
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.CONFLICT, ex.getMessage());
        }
    }

    /**
     * 同步统计汇总。
     */
    @Operation(summary = "Summarize sync")
    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> summarizeSync(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = resolveTenantId(orgCtx);
        return ApiResult.success(syncService.summarizeSync(tenantId));
    }

    // ---- 辅助方法 ----

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }
}
