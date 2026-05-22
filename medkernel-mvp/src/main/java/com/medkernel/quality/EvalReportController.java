package com.medkernel.quality;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.quality.dto.GenerateReportRequest;
import com.medkernel.quality.dto.SubmitReviewRequest;
import com.medkernel.quality.dto.CreateRectificationRequest;
import com.medkernel.quality.dto.UpdateRectificationStatusRequest;
import com.medkernel.quality.dto.ReEvaluateRequest;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估报告、复核和整改闭环 API。
 * 提供报告生成/导出、人工复核、整改任务管理、再评估和归档操作。
 */
@Tag(name = "Eval Report")
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
    @Operation(summary = "Generate report")
    @PostMapping("/generate")
    public ApiResult<Map<String, Object>> generateReport(
            @RequestBody @Valid GenerateReportRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, body);
        try {
            EvalReportService.EvalReport report = evalReportService.generateReport(request.getEvalId(), orgContext);
            return ApiResult.success(report.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 导出评估报告（含完整指标得分和事实详情）。
     */
    @Operation(summary = "Export report")
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
    @Operation(summary = "Get report")
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
    @Operation(summary = "List reports")
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
    @Operation(summary = "Archive report")
    @PostMapping("/{reportId}/archive")
    public ApiResult<Map<String, Object>> archiveReport(
            @PathVariable String reportId,
            @RequestBody @Valid Map<String, Object> request,
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
    @Operation(summary = "Submit review")
    @PostMapping("/{reportId}/review")
    public ApiResult<Map<String, Object>> submitReview(
            @PathVariable String reportId,
            @RequestBody @Valid SubmitReviewRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, body);
        try {
            EvalReportService.EvalReview review = evalReportService.submitReview(reportId, body, orgContext);
            return ApiResult.success(review.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 列出报告的复核记录。
     */
    @Operation(summary = "List reviews")
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
    @Operation(summary = "Create rectification")
    @PostMapping("/{reportId}/rectification")
    public ApiResult<Map<String, Object>> createRectification(
            @PathVariable String reportId,
            @RequestBody @Valid CreateRectificationRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, body);
        try {
            EvalReportService.EvalRectification rect = evalReportService.createRectification(reportId, body, orgContext);
            return ApiResult.success(rect.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 自动生成整改任务（基于异常和缺失事实）。
     */
    @Operation(summary = "Auto create rectifications")
    @PostMapping("/{reportId}/rectification/auto")
    public ApiResult<List<Map<String, Object>>> autoCreateRectifications(
            @PathVariable String reportId,
            @RequestBody @Valid Map<String, Object> request,
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
    @Operation(summary = "Update rectification status")
    @PostMapping("/rectification/{rectId}/status")
    public ApiResult<Map<String, Object>> updateRectificationStatus(
            @PathVariable String rectId,
            @RequestBody @Valid UpdateRectificationStatusRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, body);
        try {
            EvalReportService.EvalRectification rect = evalReportService.updateRectificationStatus(rectId, body, orgContext);
            return ApiResult.success(rect.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 列出整改任务。
     */
    @Operation(summary = "List rectifications")
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
    @Operation(summary = "Re evaluate")
    @PostMapping("/re-evaluate")
    public ApiResult<Map<String, Object>> reEvaluate(
            @RequestBody @Valid ReEvaluateRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, body);
        try {
            EvalResult result = evalReportService.reEvaluate(request.getEvalId(), request.getInputData(), orgContext);
            return ApiResult.success(result.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    // ==================== DTO → Map 转换 ====================

    private Map<String, Object> toMap(GenerateReportRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("eval_id", request.getEvalId());
        return map;
    }

    private Map<String, Object> toMap(SubmitReviewRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("review_status", request.getReviewStatus());
        map.put("reviewed_by", request.getReviewedBy());
        map.put("review_note", request.getReviewNote());
        return map;
    }

    private Map<String, Object> toMap(CreateRectificationRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("rectification_type", request.getRectificationType());
        map.put("description", request.getDescription());
        map.put("assignee", request.getAssignee());
        map.put("due_date", request.getDueDate());
        map.put("related_indicator_codes", request.getRelatedIndicatorCodes());
        return map;
    }

    private Map<String, Object> toMap(UpdateRectificationStatusRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("status", request.getStatus());
        map.put("updated_by", request.getUpdatedBy());
        map.put("note", request.getNote());
        return map;
    }

    private Map<String, Object> toMap(ReEvaluateRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("eval_id", request.getEvalId());
        map.put("input_data", request.getInputData());
        return map;
    }
}
