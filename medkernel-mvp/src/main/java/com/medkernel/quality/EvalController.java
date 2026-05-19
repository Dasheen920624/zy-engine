package com.medkernel.quality;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.common.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能评估指标模型 API。
 * 提供指标集和指标的 CRUD、发布、废弃等操作。
 */
@RestController
@RequestMapping("/api/quality/eval")
public class EvalController {
    private final EvalService evalService;
    private final OrganizationContextService organizationContextService;

    public EvalController(EvalService evalService,
                          OrganizationContextService organizationContextService) {
        this.evalService = evalService;
        this.organizationContextService = organizationContextService;
    }

    // ==================== 指标集 API ====================

    @PostMapping("/sets")
    public ApiResult<Map<String, Object>> createSet(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalIndicatorSet set = evalService.createSet(request, orgContext);
            return ApiResult.success(set.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @PutMapping("/sets/{setCode}")
    public ApiResult<Map<String, Object>> updateSet(
            @PathVariable String setCode,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalIndicatorSet set = evalService.updateSet(setCode, request, orgContext);
            return ApiResult.success(set.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @PostMapping("/sets/{setCode}/publish")
    public ApiResult<Map<String, Object>> publishSet(
            @PathVariable String setCode,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalIndicatorSet set = evalService.publishSet(setCode, orgContext);
            return ApiResult.success(set.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @PostMapping("/sets/{setCode}/deprecate")
    public ApiResult<Map<String, Object>> deprecateSet(
            @PathVariable String setCode,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalIndicatorSet set = evalService.deprecateSet(setCode, orgContext);
            return ApiResult.success(set.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @GetMapping("/sets")
    public ApiResult<List<Map<String, Object>>> listSets(
            @RequestParam(required = false) String subject_type,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Map<String, String> filters = new LinkedHashMap<String, String>();
        if (subject_type != null) filters.put("subject_type", subject_type);
        if (status != null) filters.put("status", status);
        List<EvalIndicatorSet> sets = evalService.listSets(filters, orgContext);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (EvalIndicatorSet set : sets) {
            views.add(set.toView());
        }
        return ApiResult.success(views);
    }

    @GetMapping("/sets/{setCode}")
    public ApiResult<Map<String, Object>> getSet(
            @PathVariable String setCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        EvalIndicatorSet set = evalService.getSet(setCode, orgContext);
        if (set == null) {
            return ApiResult.failure(ErrorCode.NOT_FOUND, "Indicator set not found: " + setCode);
        }
        return ApiResult.success(set.toView());
    }

    // ==================== 指标 API ====================

    @PostMapping("/sets/{setCode}/indicators")
    public ApiResult<Map<String, Object>> createIndicator(
            @PathVariable String setCode,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalIndicator indicator = evalService.createIndicator(setCode, request, orgContext);
            return ApiResult.success(indicator.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @PutMapping("/indicators/{indicatorCode}")
    public ApiResult<Map<String, Object>> updateIndicator(
            @PathVariable String indicatorCode,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalIndicator indicator = evalService.updateIndicator(indicatorCode, request, orgContext);
            return ApiResult.success(indicator.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @DeleteMapping("/indicators/{indicatorCode}")
    public ApiResult<Map<String, Object>> deleteIndicator(
            @PathVariable String indicatorCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            EvalIndicator indicator = evalService.deleteIndicator(indicatorCode, orgContext);
            return ApiResult.success(indicator.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @GetMapping("/sets/{setCode}/indicators")
    public ApiResult<List<Map<String, Object>>> listIndicators(
            @PathVariable String setCode,
            HttpServletRequest httpRequest) {
        List<EvalIndicator> indicators = evalService.listIndicatorsBySet(setCode);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (EvalIndicator ind : indicators) {
            views.add(ind.toView());
        }
        return ApiResult.success(views);
    }

    @GetMapping("/indicators/{indicatorCode}")
    public ApiResult<Map<String, Object>> getIndicator(
            @PathVariable String indicatorCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        EvalIndicator indicator = evalService.getIndicator(indicatorCode, orgContext);
        if (indicator == null) {
            return ApiResult.failure(ErrorCode.NOT_FOUND, "Indicator not found: " + indicatorCode);
        }
        return ApiResult.success(indicator.toView());
    }
}
