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

/**
 * 评估质控 REST 入口（GA-ENG-API-08 {@code /api/v1/engine/evaluations}）。
 *
 * <p>承担指标配置、运行事实、评估结果、质控问题、整改、复核与诊断的 HTTP 合同；
 * 权限由 {@code evaluation.read}/{@code evaluation.write}/{@code evaluation.publish}/
 * {@code evaluation.execute}/{@code evaluation.remediate}/{@code evaluation.review} 拆分控制，
 * 租户隔离由类级 {@link DataScope}{@code (requireTenant=true)} 兜底。
 */
@RestController
@RequestMapping("/api/v1/engine/evaluations")
@DataScope(requireTenant = true)
public class EvaluationEngineController {

    private final EvaluationEngineService service;

    /**
     * 注入评估质控应用服务，控制器仅负责 HTTP 参数、权限入口与响应包装。
     */
    public EvaluationEngineController(EvaluationEngineService service) {
        this.service = service;
    }

    /**
     * 创建评估指标草稿版本。
     *
     * <p>权限：{@code evaluation.write}；请求必须包含指标编码、版本号、分子分母、时间窗、组织范围和来源引用。
     */
    @PostMapping("/indicators")
    @PreAuthorize("@perm.has('evaluation.write')")
    public ResponseEntity<ApiResult<EvaluationIndicator>> createIndicator(
            @RequestBody @Valid EvaluationIndicatorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(service.createIndicator(request)));
    }

    /**
     * 按状态、评估对象类型和指标编码分页查询指标版本。
     *
     * <p>权限：{@code evaluation.read}；过滤参数均可选，{@code null} 表示不过滤。
     */
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

    /**
     * 查看单个评估指标版本详情。
     *
     * <p>权限：{@code evaluation.read}；指标不存在时抛出 {@code ENG-EVAL-002}。
     */
    @GetMapping("/indicators/{indicatorId}")
    @PreAuthorize("@perm.has('evaluation.read')")
    public ApiResult<EvaluationIndicator> indicatorDetail(@PathVariable String indicatorId) {
        return ApiResult.ok(service.indicatorDetail(indicatorId));
    }

    /**
     * 将指标从草稿提交到待审核状态。
     *
     * <p>权限：{@code evaluation.write}；当前状态必须为 {@code DRAFT}。
     */
    @PostMapping("/indicators/{indicatorId}/submit")
    @PreAuthorize("@perm.has('evaluation.write')")
    public ApiResult<EvaluationIndicator> submitIndicator(@PathVariable String indicatorId) {
        return ApiResult.ok(service.submitIndicator(indicatorId));
    }

    /**
     * 发布已通过审核的评估指标。
     *
     * <p>权限：{@code evaluation.publish}；当前状态必须为 {@code PENDING_REVIEW}。
     */
    @PostMapping("/indicators/{indicatorId}/publish")
    @PreAuthorize("@perm.has('evaluation.publish')")
    public ApiResult<EvaluationIndicator> publishIndicator(@PathVariable String indicatorId) {
        return ApiResult.ok(service.publishIndicator(indicatorId));
    }

    /**
     * 激活已发布指标，并下线同编码旧的 {@code ACTIVE} 版本。
     *
     * <p>权限：{@code evaluation.publish}；用于控制新评估运行只能绑定当前生效口径。
     */
    @PostMapping("/indicators/{indicatorId}/activate")
    @PreAuthorize("@perm.has('evaluation.publish')")
    public ApiResult<EvaluationIndicator> activateIndicator(@PathVariable String indicatorId) {
        return ApiResult.ok(service.activateIndicator(indicatorId));
    }

    /**
     * 依据上下文快照自动执行质量控制扫描并持久化结果与整改。
     *
     * <p>权限：{@code evaluation.execute}；该方法会自动调取临床快照资源、校验入组、排除及达标状态。
     */
    @PostMapping("/evaluate-snapshot")
    @PreAuthorize("@perm.has('evaluation.execute')")
    public ResponseEntity<ApiResult<EvaluationRunResponse>> evaluateSnapshot(
            @RequestBody @Valid EvaluationEvaluateSnapshotRequest request) {
        return ResponseEntity.ok(ApiResult.ok(service.evaluateSnapshot(request)));
    }

    /**
     * 接收一次评估运行事实及其结果、质控问题和可选整改任务。
     *
     * <p>权限：{@code evaluation.execute}；接口不做自动指标计算，仅校验并持久化受控事实。
     */
    @PostMapping("/run")
    @PreAuthorize("@perm.has('evaluation.execute')")
    public ResponseEntity<ApiResult<EvaluationRunResponse>> run(
            @RequestBody @Valid EvaluationRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(service.run(request)));
    }

    /**
     * 按指标编码、结果等级和责任科室分页查询评估结果。
     *
     * <p>权限：{@code evaluation.read}；返回结果绑定指标版本和证据摘要，便于追溯。
     */
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

    /**
     * 按严重度、状态和责任科室分页查询质控问题。
     *
     * <p>权限：{@code evaluation.read}；支持质控待办、整改跟踪和风险分层检索。
     */
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

    /**
     * 查看质控问题详情、关联整改任务和复核记录。
     *
     * <p>权限：{@code evaluation.read}；问题不存在时抛出 {@code ENG-EVAL-005}。
     */
    @GetMapping("/findings/{findingId}")
    @PreAuthorize("@perm.has('evaluation.read')")
    public ApiResult<QualityFindingDetailResponse> findingDetail(@PathVariable String findingId) {
        return ApiResult.ok(service.findingDetail(findingId));
    }

    /**
     * 提交质控整改说明和证据引用。
     *
     * <p>权限：{@code evaluation.remediate}；可携带 {@code Idempotency-Key} 防止网络重试重复提交。
     */
    @PostMapping("/findings/{findingId}/rectification")
    @PreAuthorize("@perm.has('evaluation.remediate')")
    public ApiResult<RectificationResponse> submitRectification(
            @PathVariable String findingId,
            @RequestBody @Valid RectificationSubmitRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResult.ok(service.submitRectification(findingId, request, idempotencyKey));
    }

    /**
     * 提交整改复核结论，关闭、退回或豁免整改任务。
     *
     * <p>权限：{@code evaluation.review}；可携带 {@code Idempotency-Key}，且 {@code P0} 问题不得普通豁免。
     */
    @PostMapping("/findings/{findingId}/review")
    @PreAuthorize("@perm.has('evaluation.review')")
    public ApiResult<RectificationReviewResponse> reviewRectification(
            @PathVariable String findingId,
            @RequestBody @Valid RectificationReviewRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResult.ok(service.reviewRectification(findingId, request, idempotencyKey));
    }

    /**
     * 查看评估运行的可解释诊断响应。
     *
     * <p>权限：{@code evaluation.read}；诊断响应包含运行状态、结果 ID、问题 ID、整改任务 ID 与 traceId。
     */
    @GetMapping("/runs/{runId}/diagnose")
    @PreAuthorize("@perm.has('evaluation.read')")
    public ApiResult<DiagnoseResponse> diagnose(@PathVariable String runId) {
        return ApiResult.ok(service.diagnose(runId));
    }
}
