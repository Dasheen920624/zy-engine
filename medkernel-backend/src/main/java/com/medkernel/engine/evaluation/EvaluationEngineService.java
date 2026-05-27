package com.medkernel.engine.evaluation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.observability.DiagnoseResponse;
import com.medkernel.shared.observability.DiagnoseResponseAssembler;
import com.medkernel.shared.observability.StateTransitionRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationEngineService {

    private static final String INDICATOR_ENTITY = "evaluation_indicator";
    private static final String RUN_ENTITY = "evaluation_run";
    private static final String FINDING_ENTITY = "quality_finding";
    private static final String TASK_ENTITY = "rectification_task";

    private final EvaluationIndicatorRepository indicators;
    private final EvaluationRunRepository runs;
    private final EvaluationResultRepository results;
    private final QualityFindingRepository findings;
    private final RectificationTaskRepository tasks;
    private final RectificationReviewRepository reviews;
    private final AuditEventPublisher auditPublisher;
    private final StateTransitionRecorder transitions;
    private final DiagnoseResponseAssembler diagnoseAssembler;

    public EvaluationEngineService(
            EvaluationIndicatorRepository indicators,
            EvaluationRunRepository runs,
            EvaluationResultRepository results,
            QualityFindingRepository findings,
            RectificationTaskRepository tasks,
            RectificationReviewRepository reviews,
            AuditEventPublisher auditPublisher,
            StateTransitionRecorder transitions,
            DiagnoseResponseAssembler diagnoseAssembler) {
        this.indicators = indicators;
        this.runs = runs;
        this.results = results;
        this.findings = findings;
        this.tasks = tasks;
        this.reviews = reviews;
        this.auditPublisher = auditPublisher;
        this.transitions = transitions;
        this.diagnoseAssembler = diagnoseAssembler;
    }

    @Transactional
    public EvaluationIndicator createIndicator(EvaluationIndicatorCreateRequest request) {
        validateIndicator(request);
        Instant now = Instant.now();
        String indicatorId = "ei-" + UUID.randomUUID();
        EvaluationIndicator indicator = indicators.save(new EvaluationIndicator(
            null, indicatorId, tenantId(), request.indicatorCode(), request.versionNo(), request.name(),
            request.subjectType(), request.denominatorDefinition(), request.numeratorDefinition(),
            request.exclusionDefinition(), request.scoringDefinition(), request.timeWindow(),
            request.organizationScope(), request.responsibleDepartmentId(), request.sourceRef(),
            request.packageVersion(), EvaluationIndicatorStatus.DRAFT, null, null, null,
            now, actor(), now, actor(), traceId()));
        transitions.record(INDICATOR_ENTITY, indicatorId, null, EvaluationIndicatorStatus.DRAFT.name(),
            "创建评估指标草稿", null);
        auditPublisher.publish(AuditAction.CREATE, INDICATOR_ENTITY, indicatorId,
            "创建评估指标 " + request.indicatorCode());
        return indicator;
    }

    @Transactional(readOnly = true)
    public PageResponse<EvaluationIndicator> listIndicators(
            EvaluationIndicatorFilter filter, PageRequest pageRequest) {
        PageRequest req = pageRequest == null ? PageRequest.defaults() : pageRequest;
        EvaluationIndicatorFilter f = filter == null
            ? new EvaluationIndicatorFilter(null, null, null)
            : filter;
        String status = f.status() == null ? null : f.status().name();
        String subjectType = f.subjectType() == null ? null : f.subjectType().name();
        long total = indicators.countByFilter(tenantId(), status, subjectType, f.indicatorCode());
        List<EvaluationIndicator> rows = indicators.pageByFilter(
            tenantId(), status, subjectType, f.indicatorCode(), req.offset(), req.safeSize());
        return PageResponse.of(rows, req, total);
    }

    @Transactional(readOnly = true)
    public EvaluationIndicator indicatorDetail(String indicatorId) {
        return findIndicator(indicatorId);
    }

    @Transactional
    public EvaluationIndicator submitIndicator(String indicatorId) {
        EvaluationIndicator indicator = findIndicator(indicatorId);
        requireStatus(indicator, EvaluationIndicatorStatus.DRAFT);
        EvaluationIndicator saved = saveIndicatorStatus(
            indicator, EvaluationIndicatorStatus.PENDING_REVIEW, null, null);
        transitions.record(INDICATOR_ENTITY, indicatorId, indicator.status().name(), saved.status().name(),
            "提交评估指标审核", null);
        auditPublisher.publish(AuditAction.REVIEW, INDICATOR_ENTITY, indicatorId,
            "提交评估指标审核 " + indicator.indicatorCode());
        return saved;
    }

    @Transactional
    public EvaluationIndicator publishIndicator(String indicatorId) {
        EvaluationIndicator indicator = findIndicator(indicatorId);
        requireStatus(indicator, EvaluationIndicatorStatus.PENDING_REVIEW);
        Instant now = Instant.now();
        EvaluationIndicator saved = saveIndicatorStatus(
            indicator, EvaluationIndicatorStatus.PUBLISHED, now, null);
        transitions.record(INDICATOR_ENTITY, indicatorId, indicator.status().name(), saved.status().name(),
            "发布评估指标", null);
        auditPublisher.publish(AuditAction.PUBLISH, INDICATOR_ENTITY, indicatorId,
            "发布评估指标 " + indicator.indicatorCode());
        return saved;
    }

    @Transactional
    public EvaluationIndicator activateIndicator(String indicatorId) {
        EvaluationIndicator indicator = findIndicator(indicatorId);
        requireStatus(indicator, EvaluationIndicatorStatus.PUBLISHED);
        Instant now = Instant.now();
        for (EvaluationIndicator old : indicators.findByTenantIdAndIndicatorCodeAndStatus(
                tenantId(), indicator.indicatorCode(), EvaluationIndicatorStatus.ACTIVE)) {
            EvaluationIndicator offline = saveIndicatorStatus(old, EvaluationIndicatorStatus.OFFLINE, null, null);
            transitions.record(INDICATOR_ENTITY, old.indicatorId(), old.status().name(), offline.status().name(),
                "新版指标激活后下线旧版", null);
        }
        EvaluationIndicator active = saveIndicatorStatus(
            indicator, EvaluationIndicatorStatus.ACTIVE, indicator.publishedAt(), now);
        transitions.record(INDICATOR_ENTITY, indicatorId, indicator.status().name(), active.status().name(),
            "激活评估指标", null);
        auditPublisher.publish(AuditAction.UPDATE, INDICATOR_ENTITY, indicatorId,
            "激活评估指标 " + indicator.indicatorCode());
        return active;
    }

    @Transactional
    public EvaluationRunResponse run(EvaluationRunRequest request) {
        validateRun(request);
        String tenantId = tenantId();
        Map<String, EvaluationIndicator> activeIndicators = new LinkedHashMap<>();
        for (EvaluationResultRequest resultRequest : request.results()) {
            EvaluationIndicator indicator = indicators
                .findByIndicatorIdAndTenantId(resultRequest.indicatorId(), tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENG_EVAL_004));
            if (indicator.status() != EvaluationIndicatorStatus.ACTIVE) {
                throw new ApiException(ErrorCode.ENG_EVAL_004);
            }
            activeIndicators.put(resultRequest.indicatorId(), indicator);
            validateResult(resultRequest);
        }

        Instant now = Instant.now();
        String actor = actor();
        String traceId = traceId();
        String runId = "er-" + UUID.randomUUID();
        EvaluationRun savedRun = runs.save(new EvaluationRun(
            null, runId, tenantId, request.runCode(), request.runType(), request.sourceEventId(),
            request.contextSnapshotId(), request.patientId(), request.encounterId(), request.scenarioCode(),
            request.packageVersion(), request.inputDigest(), EvaluationRunStatus.RECORDED, null,
            request.occurredAt() == null ? now : request.occurredAt(),
            now, actor, now, actor, traceId));

        int findingCount = 0;
        int taskCount = 0;
        for (EvaluationResultRequest resultRequest : request.results()) {
            EvaluationIndicator indicator = activeIndicators.get(resultRequest.indicatorId());
            String resultId = "ers-" + UUID.randomUUID();
            results.save(new EvaluationResult(
                null, resultId, tenantId, runId, indicator.indicatorId(), indicator.indicatorCode(),
                indicator.versionNo(), resultRequest.subjectType(), resultRequest.subjectRefId(),
                resultRequest.scoreValue(), resultRequest.resultLevel(), resultRequest.hitFlag(),
                resultRequest.evidenceSummary(), resultRequest.sourceRef(), resultRequest.responsibleDepartmentId(),
                now, actor, now, actor, traceId));
            for (QualityFindingRequest findingRequest : safeFindings(resultRequest.findings())) {
                boolean assigned = shouldAssign(findingRequest);
                String findingId = "qf-" + UUID.randomUUID();
                QualityFinding finding = findings.save(new QualityFinding(
                    null, findingId, tenantId, runId, resultId, indicator.indicatorId(),
                    findingRequest.findingCode(), findingRequest.title(), findingRequest.description(),
                    findingRequest.severity(), assigned ? QualityFindingStatus.ASSIGNED : QualityFindingStatus.NEW,
                    findingRequest.evidenceSummary(), findingRequest.responsibleDepartmentId(),
                    findingRequest.dueAt(), now, actor, now, actor, traceId));
                findingCount++;
                if (assigned) {
                    String taskId = "rct-" + UUID.randomUUID();
                    tasks.save(new RectificationTask(
                        null, taskId, tenantId, findingId, findingRequest.responsibleDepartmentId(),
                        findingRequest.assigneeUserId(), RectificationTaskStatus.ASSIGNED, findingRequest.dueAt(),
                        null, null, null, null, null, now, actor, now, actor, traceId));
                    taskCount++;
                    transitions.record(TASK_ENTITY, taskId, null, RectificationTaskStatus.ASSIGNED.name(),
                        "创建质控整改任务", null);
                }
                transitions.record(FINDING_ENTITY, finding.findingId(), null, finding.status().name(),
                    "记录质控问题", null);
            }
        }
        transitions.record(RUN_ENTITY, runId, null, savedRun.status().name(), "接收评估运行", null);
        auditPublisher.publish(AuditAction.EXECUTE, RUN_ENTITY, runId, "接收评估运行 " + request.runCode());
        return new EvaluationRunResponse(
            runId, savedRun.status(), request.results().size(), findingCount, taskCount, traceId);
    }

    @Transactional(readOnly = true)
    public PageResponse<EvaluationResult> listResults(EvaluationResultFilter filter, PageRequest pageRequest) {
        PageRequest req = pageRequest == null ? PageRequest.defaults() : pageRequest;
        EvaluationResultFilter f = filter == null ? new EvaluationResultFilter(null, null, null) : filter;
        String level = f.resultLevel() == null ? null : f.resultLevel().name();
        long total = results.countByFilter(tenantId(), f.indicatorCode(), level, f.responsibleDepartmentId());
        List<EvaluationResult> rows = results.pageByFilter(
            tenantId(), f.indicatorCode(), level, f.responsibleDepartmentId(), req.offset(), req.safeSize());
        return PageResponse.of(rows, req, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<QualityFinding> listFindings(QualityFindingFilter filter, PageRequest pageRequest) {
        PageRequest req = pageRequest == null ? PageRequest.defaults() : pageRequest;
        QualityFindingFilter f = filter == null ? new QualityFindingFilter(null, null, null) : filter;
        String severity = f.severity() == null ? null : f.severity().name();
        String status = f.status() == null ? null : f.status().name();
        long total = findings.countByFilter(tenantId(), severity, status, f.responsibleDepartmentId());
        List<QualityFinding> rows = findings.pageByFilter(
            tenantId(), severity, status, f.responsibleDepartmentId(), req.offset(), req.safeSize());
        return PageResponse.of(rows, req, total);
    }

    @Transactional(readOnly = true)
    public QualityFindingDetailResponse findingDetail(String findingId) {
        QualityFinding finding = findFinding(findingId);
        return new QualityFindingDetailResponse(
            finding,
            tasks.findByFindingIdAndTenantId(findingId, tenantId()).orElse(null),
            reviews.findByFindingIdAndTenantIdOrderByReviewedAtAsc(findingId, tenantId()));
    }

    @Transactional
    public RectificationResponse submitRectification(String findingId, RectificationSubmitRequest request) {
        if (request == null || !hasText(request.rectificationSummary()) || !hasText(request.evidenceRef())) {
            throw new ApiException(ErrorCode.ENG_EVAL_001);
        }
        QualityFinding finding = findFinding(findingId);
        RectificationTask task = findTask(findingId);
        if (task.status() != RectificationTaskStatus.ASSIGNED
                && task.status() != RectificationTaskStatus.RETURNED) {
            throw new ApiException(ErrorCode.ENG_EVAL_007);
        }
        Instant now = Instant.now();
        String actor = actor();
        RectificationTask submitted = tasks.save(new RectificationTask(
            task.id(), task.taskId(), task.tenantId(), task.findingId(), task.responsibleDepartmentId(),
            task.assigneeUserId(), RectificationTaskStatus.SUBMITTED, task.dueAt(),
            request.rectificationSummary(), request.evidenceRef(), now, actor, null,
            task.createdAt(), task.createdBy(), now, actor, task.traceId()));
        QualityFinding remediating = saveFindingStatus(finding, QualityFindingStatus.REMEDIATING, now, actor);
        transitions.record(TASK_ENTITY, task.taskId(), task.status().name(), submitted.status().name(),
            "提交质控整改", null);
        transitions.record(FINDING_ENTITY, findingId, finding.status().name(), remediating.status().name(),
            "责任科室提交整改", null);
        auditPublisher.publish(AuditAction.UPDATE, FINDING_ENTITY, findingId, "提交质控整改 " + task.taskId());
        return new RectificationResponse(task.taskId(), remediating.status(), submitted.status(), traceId());
    }

    @Transactional
    public RectificationReviewResponse reviewRectification(String findingId, RectificationReviewRequest request) {
        if (request == null || request.decision() == null) {
            throw new ApiException(ErrorCode.ENG_EVAL_001);
        }
        QualityFinding finding = findFinding(findingId);
        RectificationTask task = findTask(findingId);
        if (task.status() != RectificationTaskStatus.SUBMITTED
                || finding.status() != QualityFindingStatus.REMEDIATING) {
            throw new ApiException(ErrorCode.ENG_EVAL_007);
        }
        if (request.decision() == RectificationReviewDecision.WAIVED
                && finding.severity() == QualityFindingSeverity.P0) {
            throw new ApiException(ErrorCode.ENG_EVAL_007, "P0 质控问题不得通过普通复核豁免");
        }
        if ((request.decision() == RectificationReviewDecision.APPROVED
                && !hasText(request.comment()) && !hasText(request.evidenceRef()))
                || (request.decision() == RectificationReviewDecision.WAIVED && !hasText(request.comment()))) {
            throw new ApiException(ErrorCode.ENG_EVAL_007);
        }
        Instant now = Instant.now();
        String actor = actor();
        String reviewId = "rr-" + UUID.randomUUID();
        reviews.save(new RectificationReview(
            null, reviewId, tenantId(), findingId, task.taskId(), request.decision(), request.comment(),
            request.evidenceRef(), actor, now, now, actor, now, actor, traceId()));
        QualityFindingStatus findingStatus = switch (request.decision()) {
            case APPROVED -> QualityFindingStatus.CLOSED;
            case RETURNED -> QualityFindingStatus.REMEDIATING;
            case WAIVED -> QualityFindingStatus.WAIVED;
        };
        RectificationTaskStatus taskStatus = switch (request.decision()) {
            case APPROVED -> RectificationTaskStatus.CLOSED;
            case RETURNED -> RectificationTaskStatus.RETURNED;
            case WAIVED -> RectificationTaskStatus.WAIVED;
        };
        QualityFinding reviewedFinding = saveFindingStatus(finding, findingStatus, now, actor);
        RectificationTask reviewedTask = tasks.save(new RectificationTask(
            task.id(), task.taskId(), task.tenantId(), task.findingId(), task.responsibleDepartmentId(),
            task.assigneeUserId(), taskStatus, task.dueAt(), task.rectificationSummary(), task.evidenceRef(),
            task.submittedAt(), task.submittedBy(),
            taskStatus == RectificationTaskStatus.CLOSED ? now : task.closedAt(),
            task.createdAt(), task.createdBy(), now, actor, task.traceId()));
        transitions.record(FINDING_ENTITY, findingId, finding.status().name(), reviewedFinding.status().name(),
            "复核质控整改 " + request.decision(), null);
        transitions.record(TASK_ENTITY, task.taskId(), task.status().name(), reviewedTask.status().name(),
            "复核整改任务 " + request.decision(), null);
        auditPublisher.publish(AuditAction.REVIEW, FINDING_ENTITY, findingId,
            "复核质控整改 " + request.decision());
        return new RectificationReviewResponse(reviewId, reviewedFinding.status(), reviewedTask.status(), traceId());
    }

    @Transactional(readOnly = true)
    public DiagnoseResponse diagnose(String runId) {
        EvaluationRun run = runs.findByRunIdAndTenantId(runId, tenantId())
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_EVAL_001, "评估运行不存在"));
        List<EvaluationResult> runResults = results.findByRunIdAndTenantIdOrderByCreatedAtAsc(runId, tenantId());
        List<QualityFinding> runFindings = new ArrayList<>();
        List<RectificationTask> runTasks = new ArrayList<>();
        for (EvaluationResult result : runResults) {
            List<QualityFinding> resultFindings =
                findings.findByResultIdAndTenantIdOrderByCreatedAtAsc(result.resultId(), tenantId());
            runFindings.addAll(resultFindings);
            for (QualityFinding finding : resultFindings) {
                tasks.findByFindingIdAndTenantId(finding.findingId(), tenantId()).ifPresent(runTasks::add);
            }
        }
        Map<String, List<String>> related = Map.of(
            "results", runResults.stream().map(EvaluationResult::resultId).toList(),
            "findings", runFindings.stream().map(QualityFinding::findingId).toList(),
            "tasks", runTasks.stream().map(RectificationTask::taskId).toList());
        return diagnoseAssembler.assemble(
            RUN_ENTITY, runId, tenantId(), run.status().name(), run, List.of(), related, null,
            run.traceId() == null ? traceId() : run.traceId());
    }

    private void validateIndicator(EvaluationIndicatorCreateRequest request) {
        if (request == null || request.versionNo() < 1 || !hasText(request.indicatorCode())
                || !hasText(request.name()) || request.subjectType() == null
                || !hasText(request.denominatorDefinition()) || !hasText(request.numeratorDefinition())
                || !hasText(request.timeWindow()) || !hasText(request.organizationScope())
                || !hasText(request.responsibleDepartmentId()) || !hasText(request.sourceRef())) {
            throw new ApiException(ErrorCode.ENG_EVAL_001);
        }
    }

    private void validateRun(EvaluationRunRequest request) {
        if (request == null || !hasText(request.runCode()) || request.runType() == null
                || !hasText(request.scenarioCode()) || !hasText(request.inputDigest())
                || request.results() == null || request.results().isEmpty()) {
            throw new ApiException(ErrorCode.ENG_EVAL_001);
        }
        boolean hasContextReference = hasText(request.sourceEventId()) || hasText(request.contextSnapshotId());
        boolean hasManualSource = request.runType() == EvaluationRunType.MANUAL_SAMPLE
            && request.results().stream().allMatch(result -> result != null && hasText(result.sourceRef()));
        if (!hasContextReference && !hasManualSource) {
            throw new ApiException(ErrorCode.ENG_EVAL_001, "评估运行缺少可追溯的上下文或人工抽检来源");
        }
    }

    private void validateResult(EvaluationResultRequest result) {
        if (result == null || !hasText(result.indicatorId()) || result.subjectType() == null
                || !hasText(result.subjectRefId()) || result.resultLevel() == null
                || !hasText(result.evidenceSummary())) {
            throw new ApiException(ErrorCode.ENG_EVAL_001);
        }
        for (QualityFindingRequest finding : safeFindings(result.findings())) {
            if (finding == null || !hasText(finding.findingCode()) || !hasText(finding.title())
                    || !hasText(finding.description()) || finding.severity() == null
                    || !hasText(finding.evidenceSummary())) {
                throw new ApiException(ErrorCode.ENG_EVAL_001);
            }
            if (isHighRisk(finding.severity())
                    && (!hasText(finding.responsibleDepartmentId()) || finding.dueAt() == null)) {
                throw new ApiException(ErrorCode.ENG_EVAL_006);
            }
            if (!isHighRisk(finding.severity())
                    && hasPartialAssignment(finding)
                    && (!hasText(finding.responsibleDepartmentId()) || finding.dueAt() == null)) {
                throw new ApiException(ErrorCode.ENG_EVAL_001);
            }
        }
    }

    private boolean shouldAssign(QualityFindingRequest request) {
        return isHighRisk(request.severity())
            || (hasText(request.responsibleDepartmentId()) && request.dueAt() != null);
    }

    private boolean hasPartialAssignment(QualityFindingRequest request) {
        return hasText(request.responsibleDepartmentId())
            || request.dueAt() != null
            || hasText(request.assigneeUserId());
    }

    private boolean isHighRisk(QualityFindingSeverity severity) {
        return severity == QualityFindingSeverity.P0 || severity == QualityFindingSeverity.P1;
    }

    private List<QualityFindingRequest> safeFindings(List<QualityFindingRequest> value) {
        return value == null ? List.of() : value;
    }

    private EvaluationIndicator findIndicator(String indicatorId) {
        return indicators.findByIndicatorIdAndTenantId(indicatorId, tenantId())
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_EVAL_002));
    }

    private QualityFinding findFinding(String findingId) {
        return findings.findByFindingIdAndTenantId(findingId, tenantId())
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_EVAL_005));
    }

    private RectificationTask findTask(String findingId) {
        return tasks.findByFindingIdAndTenantId(findingId, tenantId())
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_EVAL_005));
    }

    private void requireStatus(EvaluationIndicator indicator, EvaluationIndicatorStatus required) {
        if (indicator.status() != required) {
            throw new ApiException(ErrorCode.ENG_EVAL_003);
        }
    }

    private EvaluationIndicator saveIndicatorStatus(
            EvaluationIndicator indicator, EvaluationIndicatorStatus status, Instant publishedAt, Instant activatedAt) {
        Instant now = Instant.now();
        return indicators.save(new EvaluationIndicator(
            indicator.id(), indicator.indicatorId(), indicator.tenantId(), indicator.indicatorCode(),
            indicator.versionNo(), indicator.name(), indicator.subjectType(), indicator.denominatorDefinition(),
            indicator.numeratorDefinition(), indicator.exclusionDefinition(), indicator.scoringDefinition(),
            indicator.timeWindow(), indicator.organizationScope(), indicator.responsibleDepartmentId(),
            indicator.sourceRef(), indicator.packageVersion(), status,
            publishedAt == null ? indicator.publishedAt() : publishedAt,
            publishedAt == null ? indicator.publishedBy() : actor(),
            activatedAt == null ? indicator.activatedAt() : activatedAt,
            indicator.createdAt(), indicator.createdBy(), now, actor(), indicator.traceId()));
    }

    private QualityFinding saveFindingStatus(
            QualityFinding finding, QualityFindingStatus status, Instant now, String actor) {
        return findings.save(new QualityFinding(
            finding.id(), finding.findingId(), finding.tenantId(), finding.runId(), finding.resultId(),
            finding.indicatorId(), finding.findingCode(), finding.title(), finding.description(),
            finding.severity(), status, finding.evidenceSummary(), finding.responsibleDepartmentId(),
            finding.dueAt(), finding.createdAt(), finding.createdBy(), now, actor, finding.traceId()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String tenantId() {
        String tenantId = RequestContext.currentOrgScope().tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw ApiException.tenantMissing();
        }
        return tenantId;
    }

    private String actor() {
        return RequestContext.currentUserId().orElse("system");
    }

    private String traceId() {
        String traceId = RequestContext.currentTraceId();
        return traceId == null ? RequestContext.snapshot().traceId() : traceId;
    }
}
