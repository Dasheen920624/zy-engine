package com.medkernel.quality;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.quality.dto.CreateEvalSetRequest;
import com.medkernel.quality.dto.UpdateEvalSetRequest;
import com.medkernel.quality.dto.CreateIndicatorRequest;
import com.medkernel.quality.dto.UpdateIndicatorRequest;
import com.medkernel.quality.dto.EvaluateRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
 * 智能评估指标模型 API。
 * 提供指标集和指标的 CRUD、发布、废弃等操作。
 */
@Tag(name = "Eval")
@RestController
@RequestMapping("/api/quality/eval")
public class EvalController {
    private final EvalService evalService;
    private final EvalScoringService evalScoringService;
    private final OrganizationContextService organizationContextService;

    public EvalController(EvalService evalService,
                          EvalScoringService evalScoringService,
                          OrganizationContextService organizationContextService) {
        this.evalService = evalService;
        this.evalScoringService = evalScoringService;
        this.organizationContextService = organizationContextService;
    }

    // ==================== 指标集 API ====================

    @Operation(summary = "Create set")
    @PostMapping("/sets")
    public ApiResult<Map<String, Object>> createSet(
            @RequestBody @Valid CreateEvalSetRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, body);
        try {
            EvalIndicatorSet set = evalService.createSet(body, orgContext);
            return ApiResult.success(set.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Update set")
    @PutMapping("/sets/{setCode}")
    public ApiResult<Map<String, Object>> updateSet(
            @PathVariable String setCode,
            @RequestBody @Valid UpdateEvalSetRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, body);
        try {
            EvalIndicatorSet set = evalService.updateSet(setCode, body, orgContext);
            return ApiResult.success(set.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Publish set")
    @PostMapping("/sets/{setCode}/publish")
    public ApiResult<Map<String, Object>> publishSet(
            @PathVariable String setCode,
            @RequestBody @Valid Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalIndicatorSet set = evalService.publishSet(setCode, orgContext);
            return ApiResult.success(set.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Deprecate set")
    @PostMapping("/sets/{setCode}/deprecate")
    public ApiResult<Map<String, Object>> deprecateSet(
            @PathVariable String setCode,
            @RequestBody @Valid Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            EvalIndicatorSet set = evalService.deprecateSet(setCode, orgContext);
            return ApiResult.success(set.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "List sets")
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

    @Operation(summary = "Get set")
    @GetMapping("/sets/{setCode}")
    public ApiResult<Map<String, Object>> getSet(
            @PathVariable String setCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        EvalIndicatorSet set = evalService.getSet(setCode, orgContext);
        if (set == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Indicator set not found: " + setCode);
        }
        return ApiResult.success(set.toView());
    }

    // ==================== 指标 API ====================

    @Operation(summary = "Create indicator")
    @PostMapping("/sets/{setCode}/indicators")
    public ApiResult<Map<String, Object>> createIndicator(
            @PathVariable String setCode,
            @RequestBody @Valid CreateIndicatorRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, body);
        try {
            EvalIndicator indicator = evalService.createIndicator(setCode, body, orgContext);
            return ApiResult.success(indicator.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Update indicator")
    @PutMapping("/indicators/{indicatorCode}")
    public ApiResult<Map<String, Object>> updateIndicator(
            @PathVariable String indicatorCode,
            @RequestBody @Valid UpdateIndicatorRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, body);
        try {
            EvalIndicator indicator = evalService.updateIndicator(indicatorCode, body, orgContext);
            return ApiResult.success(indicator.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Delete indicator")
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

    @Operation(summary = "List indicators")
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

    @Operation(summary = "Get indicator")
    @GetMapping("/indicators/{indicatorCode}")
    public ApiResult<Map<String, Object>> getIndicator(
            @PathVariable String indicatorCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        EvalIndicator indicator = evalService.getIndicator(indicatorCode, orgContext);
        if (indicator == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Indicator not found: " + indicatorCode);
        }
        return ApiResult.success(indicator.toView());
    }

    // ==================== 评分引擎 API ====================

    @Operation(summary = "Evaluate")
    @PostMapping("/evaluate")
    public ApiResult<Map<String, Object>> evaluate(
            @RequestBody @Valid EvaluateRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, body);
        try {
            EvalResult result = evalScoringService.evaluate(request.getSetCode(), request.getSubjectId(), request.getSubjectName(), request.getInputData(), orgContext);
            return ApiResult.success(result.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "List results")
    @GetMapping("/results")
    public ApiResult<List<Map<String, Object>>> listResults(
            @RequestParam(required = false) String set_code,
            @RequestParam(required = false) String subject_type,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        List<EvalResult> results = evalScoringService.listResults(set_code, subject_type, orgContext);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (EvalResult result : results) {
            views.add(result.toView());
        }
        return ApiResult.success(views);
    }

    @Operation(summary = "Get result")
    @GetMapping("/results/{evalId}")
    public ApiResult<Map<String, Object>> getResult(
            @PathVariable String evalId,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        EvalResult result = evalScoringService.getResult(evalId, orgContext);
        if (result == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Evaluation result not found: " + evalId);
        }
        return ApiResult.success(result.toView());
    }

    // ==================== DTO → Map 转换 ====================

    private Map<String, Object> toMap(CreateEvalSetRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("set_code", request.getSetCode());
        map.put("set_name", request.getSetName());
        map.put("subject_type", request.getSubjectType());
        map.put("description", request.getDescription());
        map.put("version", request.getVersion());
        return map;
    }

    private Map<String, Object> toMap(UpdateEvalSetRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("set_name", request.getSetName());
        map.put("description", request.getDescription());
        map.put("version", request.getVersion());
        return map;
    }

    private Map<String, Object> toMap(CreateIndicatorRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("indicator_code", request.getIndicatorCode());
        map.put("indicator_name", request.getIndicatorName());
        map.put("weight", request.getWeight());
        map.put("threshold", request.getThreshold());
        map.put("risk_mapping", request.getRiskMapping());
        map.put("calculation_expression", request.getCalculationExpression());
        map.put("description", request.getDescription());
        map.put("source_document_code", request.getSourceDocumentCode());
        return map;
    }

    private Map<String, Object> toMap(UpdateIndicatorRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("indicator_name", request.getIndicatorName());
        map.put("weight", request.getWeight());
        map.put("threshold", request.getThreshold());
        map.put("risk_mapping", request.getRiskMapping());
        map.put("calculation_expression", request.getCalculationExpression());
        map.put("description", request.getDescription());
        map.put("source_document_code", request.getSourceDocumentCode());
        return map;
    }

    private Map<String, Object> toMap(EvaluateRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("set_code", request.getSetCode());
        map.put("subject_id", request.getSubjectId());
        map.put("subject_name", request.getSubjectName());
        map.put("input_data", request.getInputData());
        return map;
    }
}
