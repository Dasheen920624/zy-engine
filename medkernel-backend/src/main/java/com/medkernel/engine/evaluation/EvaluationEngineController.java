package com.medkernel.engine.evaluation;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.datascope.DataScope;
import com.medkernel.shared.observability.DiagnoseResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/engine/evaluations")
@DataScope(requireTenant = true)
public class EvaluationEngineController {

    private final EvaluationEngineService service;

    public EvaluationEngineController(EvaluationEngineService service) {
        this.service = service;
    }

    @PostMapping("/indicators")
    @PreAuthorize("@perm.has('evaluation.write')")
    public ResponseEntity<ApiResult<EvaluationIndicator>> createIndicator(
            @RequestBody @Valid EvaluationIndicatorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(service.createIndicator(request)));
    }

    @GetMapping("/indicators")
    @PreAuthorize("@perm.has('evaluation.read')")
    public ApiResult<PageResponse<EvaluationIndicator>> indicators(
            @RequestParam(required = false) EvaluationIndicatorStatus status,
            @RequestParam(required = false) EvaluationSubjectType subjectType,
            @RequestParam(required = false) String indicatorCode,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResult.ok(service.listIndicators(
            new EvaluationIndicatorFilter(status, subjectType, indicatorCode),
            new PageRequest(page, size, sort)));
    }

    @GetMapping("/indicators/{indicatorId}")
    @PreAuthorize("@perm.has('evaluation.read')")
    public ApiResult<EvaluationIndicator> indicatorDetail(@PathVariable String indicatorId) {
        return ApiResult.ok(service.indicatorDetail(indicatorId));
    }

    @PostMapping("/indicators/{indicatorId}/submit")
    @PreAuthorize("@perm.has('evaluation.write')")
    public ApiResult<EvaluationIndicator> submitIndicator(@PathVariable String indicatorId) {
        return ApiResult.ok(service.submitIndicator(indicatorId));
    }

    @PostMapping("/indicators/{indicatorId}/publish")
    @PreAuthorize("@perm.has('evaluation.publish')")
    public ApiResult<EvaluationIndicator> publishIndicator(@PathVariable String indicatorId) {
        return ApiResult.ok(service.publishIndicator(indicatorId));
    }

    @PostMapping("/indicators/{indicatorId}/activate")
    @PreAuthorize("@perm.has('evaluation.publish')")
    public ApiResult<EvaluationIndicator> activateIndicator(@PathVariable String indicatorId) {
        return ApiResult.ok(service.activateIndicator(indicatorId));
    }

    @PostMapping("/run")
    @PreAuthorize("@perm.has('evaluation.execute')")
    public ResponseEntity<ApiResult<EvaluationRunResponse>> run(
            @RequestBody @Valid EvaluationRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(service.run(request)));
    }

    @GetMapping("/results")
    @PreAuthorize("@perm.has('evaluation.read')")
    public ApiResult<PageResponse<EvaluationResult>> results(
            @RequestParam(required = false) String indicatorCode,
            @RequestParam(required = false) EvaluationResultLevel resultLevel,
            @RequestParam(required = false) String responsibleDepartmentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResult.ok(service.listResults(
            new EvaluationResultFilter(indicatorCode, resultLevel, responsibleDepartmentId),
            new PageRequest(page, size, sort)));
    }

    @GetMapping("/findings")
    @PreAuthorize("@perm.has('evaluation.read')")
    public ApiResult<PageResponse<QualityFinding>> findings(
            @RequestParam(required = false) QualityFindingSeverity severity,
            @RequestParam(required = false) QualityFindingStatus status,
            @RequestParam(required = false) String responsibleDepartmentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResult.ok(service.listFindings(
            new QualityFindingFilter(severity, status, responsibleDepartmentId),
            new PageRequest(page, size, sort)));
    }

    @GetMapping("/findings/{findingId}")
    @PreAuthorize("@perm.has('evaluation.read')")
    public ApiResult<QualityFindingDetailResponse> findingDetail(@PathVariable String findingId) {
        return ApiResult.ok(service.findingDetail(findingId));
    }

    @PostMapping("/findings/{findingId}/rectification")
    @PreAuthorize("@perm.has('evaluation.remediate')")
    public ApiResult<RectificationResponse> submitRectification(
            @PathVariable String findingId,
            @RequestBody @Valid RectificationSubmitRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResult.ok(service.submitRectification(findingId, request, idempotencyKey));
    }

    @PostMapping("/findings/{findingId}/review")
    @PreAuthorize("@perm.has('evaluation.review')")
    public ApiResult<RectificationReviewResponse> reviewRectification(
            @PathVariable String findingId,
            @RequestBody @Valid RectificationReviewRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResult.ok(service.reviewRectification(findingId, request, idempotencyKey));
    }

    @GetMapping("/runs/{runId}/diagnose")
    @PreAuthorize("@perm.has('evaluation.read')")
    public ApiResult<DiagnoseResponse> diagnose(@PathVariable String runId) {
        return ApiResult.ok(service.diagnose(runId));
    }
}
