package com.medkernel.quality;

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

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 评估报告、复核和整改闭环 API。
 * 提供报告生成/导出、人工复核、整改任务管理、再评估和归档操作。
 */
@RestController
@RequestMapping("/api/quality/eval/report")
public class EvalReportController {
    private final EvalReportService evalReportService;
    private final OrganizationContextService organizationContextService;

    public EvalReportController(EvalReportService evalReportService,
                                 OrganizationContextService organizationContextService) {
        this.evalReportService = evalReportService;
        this.organizationContextService = organizationContextService;
    }

    // ==================== 评估报告 API ====================

    /**
     * 基于评估结果生成报告。
     */
    @PostMapping("/generate")
    public ApiResult<Map<String, Object>> generateReport(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        String evalId = (String) request.get("eval_id");
        if (evalId == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "eval_id is required");
        }
        try {
            EvalReportService.EvalReport report = evalReportService.generateReport(evalId, orgContext);
            return ApiResult.success(report.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 导出评估报告（含完整指标得分和事实详情）。
     */
    @GetMapping("/{reportId}/export")
    public ApiResult<Map<String, Object>> exportReport(
            @PathVariable String reportId,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            Map<String, Object> export = evalReportService.exportReport(reportId, orgContext);
            return ApiResult.success(export);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    /**
     * 获取评估报告详情。
     */
    @GetMapping("/{reportId}")
    public ApiResult<Map<String, Object>> getReport(
            @PathVariable String reportId,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        EvalReportService.EvalReport report = evalReportService.getReport(reportId, orgContext);
        if (report == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Report not found: " + reportId);
        }
        return ApiResult.success(report.toView());
    }

    /**
     * 列出评估报告。
     */
    @GetMapping("/list")
    public ApiResult<List<Map<String, Object>>> listReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String subject_type,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        List<EvalReportService.EvalReport> reports = evalReportService.listReports(status, subject_type, orgContext);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (EvalReportService.EvalReport report : reports) {
            views.add(report.toView());
        }
        return ApiResult.success(views);
    }

    /**
     * 归档评估报告。
     */
    @PostMapping("/{reportId}/archive")
    public ApiResult<Map<String, Object>> archiveReport(
            @PathVariable String reportId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalReportService.EvalReport report = evalReportService.archiveReport(reportId, orgContext);
            return ApiResult.success(report.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    // ==================== 人工复核 API ====================

    /**
     * 提交复核意见。
     */
    @PostMapping("/{reportId}/review")
    public ApiResult<Map<String, Object>> submitReview(
            @PathVariable String reportId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalReportService.EvalReview review = evalReportService.submitReview(reportId, request, orgContext);
            return ApiResult.success(review.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 列出报告的复核记录。
     */
    @GetMapping("/{reportId}/reviews")
    public ApiResult<List<Map<String, Object>>> listReviews(
            @PathVariable String reportId,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        List<EvalReportService.EvalReview> reviews = evalReportService.listReviews(reportId, orgContext);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (EvalReportService.EvalReview review : reviews) {
            views.add(review.toView());
        }
        return ApiResult.success(views);
    }

    // ==================== 整改任务 API ====================

    /**
     * 创建整改任务。
     */
    @PostMapping("/{reportId}/rectification")
    public ApiResult<Map<String, Object>> createRectification(
            @PathVariable String reportId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalReportService.EvalRectification rect = evalReportService.createRectification(reportId, request, orgContext);
            return ApiResult.success(rect.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 自动生成整改任务（基于异常和缺失事实）。
     */
    @PostMapping("/{reportId}/rectification/auto")
    public ApiResult<List<Map<String, Object>>> autoCreateRectifications(
            @PathVariable String reportId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            List<EvalReportService.EvalRectification> rects = evalReportService.autoCreateRectifications(reportId, orgContext);
            List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
            for (EvalReportService.EvalRectification rect : rects) {
                views.add(rect.toView());
            }
            return ApiResult.success(views);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 更新整改任务状态。
     */
    @PostMapping("/rectification/{rectId}/status")
    public ApiResult<Map<String, Object>> updateRectificationStatus(
            @PathVariable String rectId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalReportService.EvalRectification rect = evalReportService.updateRectificationStatus(rectId, request, orgContext);
            return ApiResult.success(rect.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 列出整改任务。
     */
    @GetMapping("/rectification/list")
    public ApiResult<List<Map<String, Object>>> listRectifications(
            @RequestParam(required = false) String report_id,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        List<EvalReportService.EvalRectification> rects = evalReportService.listRectifications(report_id, status, orgContext);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (EvalReportService.EvalRectification rect : rects) {
            views.add(rect.toView());
        }
        return ApiResult.success(views);
    }

    // ==================== 再评估 API ====================

    /**
     * 基于原评估结果执行再评估。
     */
    @PostMapping("/re-evaluate")
    public ApiResult<Map<String, Object>> reEvaluate(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        String evalId = (String) request.get("eval_id");
        @SuppressWarnings("unchecked")
        Map<String, Object> inputData = (Map<String, Object>) request.get("input_data");
        if (evalId == null || inputData == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "eval_id and input_data are required");
        }
        try {
            EvalResult result = evalReportService.reEvaluate(evalId, inputData, orgContext);
            return ApiResult.success(result.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }
}
