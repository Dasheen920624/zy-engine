package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * AI 知识生产任务 API：任务管理和模型调用记录。
 */
@RestController
@RequestMapping("/api/knowledge/jobs")
public class AiKnowledgeJobController {

    private final AiKnowledgeJobService jobService;
    private final OrganizationContextService organizationContextService;

    public AiKnowledgeJobController(AiKnowledgeJobService jobService,
                                     OrganizationContextService organizationContextService) {
        this.jobService = jobService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 创建知识生产任务。
     */
    @PostMapping
    public ApiResult<AiKnowledgeJob> createJob(@RequestBody AiKnowledgeJob job,
                                                 HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = resolveWithBody(httpRequest, job);
        job.setTenantId(resolveTenantId(orgCtx));
        return ApiResult.success(jobService.createJob(job));
    }

    /**
     * 查询任务列表。
     */
    @GetMapping
    public ApiResult<List<AiKnowledgeJob>> listJobs(
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false, defaultValue = "50") int limit,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(jobService.listJobs(resolveTenantId(orgCtx), jobType, status, reviewStatus, limit));
    }

    /**
     * 查询单个任务。
     */
    @GetMapping("/{jobId}")
    public ApiResult<AiKnowledgeJob> getJob(@PathVariable Long jobId,
                                              HttpServletRequest httpRequest) {
        return ApiResult.success(jobService.getJob(jobId));
    }

    /**
     * 更新任务状态。
     */
    @PostMapping("/{jobId}/status")
    public ApiResult<String> updateJobStatus(@PathVariable Long jobId,
                                               @RequestBody Map<String, String> body,
                                               HttpServletRequest httpRequest) {
        String status = body.get("status");
        String errorCode = body.get("errorCode");
        String errorMessage = body.get("errorMessage");
        jobService.updateJobStatus(jobId, status, errorCode, errorMessage);
        return ApiResult.success("状态更新成功");
    }

    /**
     * 审核任务。
     */
    @PostMapping("/{jobId}/review")
    public ApiResult<String> reviewJob(@PathVariable Long jobId,
                                         @RequestBody Map<String, String> body,
                                         HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        String reviewStatus = body.get("reviewStatus");
        String reviewedBy = body.get("reviewedBy") != null ? body.get("reviewedBy") : "system";
        String reviewComment = body.get("reviewComment");
        jobService.reviewJob(jobId, reviewStatus, reviewedBy, reviewComment);
        return ApiResult.success("审核成功");
    }

    /**
     * 记录模型调用日志。
     */
    @PostMapping("/model-calls")
    public ApiResult<AiModelCallLog> logModelCall(@RequestBody AiModelCallLog callLog,
                                                    HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = resolveWithBody(httpRequest, callLog);
        callLog.setTenantId(resolveTenantId(orgCtx));
        return ApiResult.success(jobService.logModelCall(callLog));
    }

    /**
     * 查询模型调用日志。
     */
    @GetMapping("/model-calls")
    public ApiResult<List<AiModelCallLog>> listModelCallLogs(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String callType,
            @RequestParam(required = false) String callStatus,
            @RequestParam(required = false, defaultValue = "50") int limit,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(jobService.listModelCallLogs(resolveTenantId(orgCtx), jobId, callType, callStatus, limit));
    }

    /**
     * 模型调用统计汇总。
     */
    @GetMapping("/model-calls/summary")
    public ApiResult<Map<String, Object>> summarizeModelCalls(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(jobService.summarizeModelCalls(resolveTenantId(orgCtx)));
    }

    private OrganizationContext resolveWithBody(HttpServletRequest httpRequest, Object body) {
        if (body instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) body;
            return organizationContextService.resolveWithBody(httpRequest, map);
        }
        return organizationContextService.resolve(httpRequest);
    }

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }
}
