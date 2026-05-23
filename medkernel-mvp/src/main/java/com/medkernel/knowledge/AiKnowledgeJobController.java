package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.knowledge.dto.CreateJobRequest;
import com.medkernel.knowledge.dto.LogModelCallRequest;
import com.medkernel.knowledge.dto.ModelCallSummaryResponse;
import com.medkernel.knowledge.dto.ReviewJobRequest;
import com.medkernel.knowledge.dto.UpdateJobStatusRequest;
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
import javax.validation.Valid;
import java.util.List;

/**
 * AI 知识生产任务 API：任务管理和模型调用记录。
 */
@Tag(name = "Ai Knowledge Job")
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
    @Operation(summary = "Create job")
    @PostMapping
    public ApiResult<AiKnowledgeJob> createJob(@Valid @RequestBody CreateJobRequest request,
                                                 HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        AiKnowledgeJob job = new AiKnowledgeJob();
        job.setTenantId(resolveTenantId(orgCtx));
        job.setJobType(request.getJobType());
        job.setJobName(request.getDescription());
        job.setSourceCode(request.getSourceCode());
        job.setInputSummary(request.getParameters());
        job.setCreatedBy(request.getCreatedBy());
        return ApiResult.success(jobService.createJob(job));
    }

    /**
     * 查询任务列表。
     */
    @Operation(summary = "List jobs")
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
    @Operation(summary = "Get job")
    @GetMapping("/{jobId}")
    public ApiResult<AiKnowledgeJob> getJob(@PathVariable Long jobId,
                                              HttpServletRequest httpRequest) {
        return ApiResult.success(jobService.getJob(jobId));
    }

    /**
     * 更新任务状态。
     */
    @Operation(summary = "Update job status")
    @PostMapping("/{jobId}/status")
    public ApiResult<String> updateJobStatus(@PathVariable Long jobId,
                                               @Valid @RequestBody UpdateJobStatusRequest request,
                                               HttpServletRequest httpRequest) {
        String status = request.getStatus();
        String errorCode = request.getErrorCode();
        String errorMessage = request.getErrorMessage();
        jobService.updateJobStatus(jobId, status, errorCode, errorMessage);
        return ApiResult.success("状态更新成功");
    }

    /**
     * 审核任务。
     */
    @Operation(summary = "Review job")
    @PostMapping("/{jobId}/review")
    public ApiResult<String> reviewJob(@PathVariable Long jobId,
                                         @Valid @RequestBody ReviewJobRequest request,
                                         HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        String reviewStatus = request.getReviewStatus();
        String reviewedBy = request.getReviewedBy() != null ? request.getReviewedBy() : "system";
        String reviewComment = request.getReviewComment();
        jobService.reviewJob(jobId, reviewStatus, reviewedBy, reviewComment);
        return ApiResult.success("审核成功");
    }

    /**
     * 记录模型调用日志。
     */
    @Operation(summary = "Log model call")
    @PostMapping("/model-calls")
    public ApiResult<AiModelCallLog> logModelCall(@Valid @RequestBody LogModelCallRequest request,
                                                    HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        AiModelCallLog callLog = new AiModelCallLog();
        callLog.setTenantId(resolveTenantId(orgCtx));
        callLog.setJobId(request.getJobId());
        callLog.setCallType(request.getCallType());
        callLog.setModelProvider(request.getModelProvider());
        callLog.setModelVersion(request.getModelVersion());
        callLog.setInputTokenCount(request.getInputTokens());
        callLog.setOutputTokenCount(request.getOutputTokens());
        callLog.setElapsedMs(request.getElapsedMs() != null ? request.getElapsedMs().intValue() : null);
        callLog.setCallStatus(request.getCallStatus());
        callLog.setErrorCode(request.getErrorCode());
        callLog.setErrorMessage(request.getErrorMessage());
        return ApiResult.success(jobService.logModelCall(callLog));
    }

    /**
     * 查询模型调用日志。
     */
    @Operation(summary = "List model call logs")
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
    @Operation(summary = "Summarize model calls")
    @GetMapping("/model-calls/summary")
    public ApiResult<ModelCallSummaryResponse> summarizeModelCalls(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(ModelCallSummaryResponse.fromMap(jobService.summarizeModelCalls(resolveTenantId(orgCtx))));
    }

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }
}
