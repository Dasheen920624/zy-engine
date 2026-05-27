package com.medkernel.engine.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.observability.DiagnoseResponse;
import com.medkernel.shared.observability.DiagnoseResponseAssembler;
import com.medkernel.shared.observability.StateTransitionRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EvaluationEngineServiceTest {

    private EvaluationIndicatorRepository indicators;
    private EvaluationRunRepository runs;
    private EvaluationResultRepository results;
    private QualityFindingRepository findings;
    private RectificationTaskRepository tasks;
    private RectificationReviewRepository reviews;
    private EvaluationIdempotencyKeyRepository idempotencyKeys;
    private AuditEventPublisher auditPublisher;
    private StateTransitionRecorder transitions;
    private DiagnoseResponseAssembler diagnoseAssembler;
    private EvaluationEngineService service;

    @BeforeEach
    void setUp() {
        indicators = mock(EvaluationIndicatorRepository.class);
        runs = mock(EvaluationRunRepository.class);
        results = mock(EvaluationResultRepository.class);
        findings = mock(QualityFindingRepository.class);
        tasks = mock(RectificationTaskRepository.class);
        reviews = mock(RectificationReviewRepository.class);
        idempotencyKeys = mock(EvaluationIdempotencyKeyRepository.class);
        auditPublisher = mock(AuditEventPublisher.class);
        transitions = mock(StateTransitionRecorder.class);
        diagnoseAssembler = mock(DiagnoseResponseAssembler.class);
        service = new EvaluationEngineService(
            indicators, runs, results, findings, tasks, reviews, idempotencyKeys,
            auditPublisher, transitions, diagnoseAssembler);

        when(indicators.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(runs.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(results.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(findings.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tasks.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reviews.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequestContext.restore(new RequestContext.Snapshot(
            "trace-eval", OrgScope.tenant("tenant-A"), "qa-1"));
    }

    @AfterEach
    void clear() {
        RequestContext.clear();
    }

    @Test
    void indicatorFlowsFromDraftToActiveAndReplacesOldActiveVersion() {
        EvaluationIndicator draft = service.createIndicator(indicatorRequest(2));
        assertThat(draft.status()).isEqualTo(EvaluationIndicatorStatus.DRAFT);
        assertThat(draft.tenantId()).isEqualTo("tenant-A");

        when(indicators.findByIndicatorIdAndTenantId(draft.indicatorId(), "tenant-A"))
            .thenReturn(Optional.of(draft));
        EvaluationIndicator pending = service.submitIndicator(draft.indicatorId());
        assertThat(pending.status()).isEqualTo(EvaluationIndicatorStatus.PENDING_REVIEW);

        when(indicators.findByIndicatorIdAndTenantId(draft.indicatorId(), "tenant-A"))
            .thenReturn(Optional.of(pending));
        EvaluationIndicator published = service.publishIndicator(draft.indicatorId());
        assertThat(published.status()).isEqualTo(EvaluationIndicatorStatus.PUBLISHED);

        EvaluationIndicator oldActive = indicator("ei-old", 1, EvaluationIndicatorStatus.ACTIVE);
        when(indicators.findByIndicatorIdAndTenantId(draft.indicatorId(), "tenant-A"))
            .thenReturn(Optional.of(published));
        when(indicators.findByTenantIdAndIndicatorCodeAndStatus(
            "tenant-A", "IND.VTE.PROPHYLAXIS", EvaluationIndicatorStatus.ACTIVE))
            .thenReturn(List.of(oldActive));
        EvaluationIndicator active = service.activateIndicator(draft.indicatorId());

        assertThat(active.status()).isEqualTo(EvaluationIndicatorStatus.ACTIVE);
        ArgumentCaptor<EvaluationIndicator> saved = ArgumentCaptor.forClass(EvaluationIndicator.class);
        verify(indicators, org.mockito.Mockito.atLeast(5)).save(saved.capture());
        assertThat(saved.getAllValues())
            .anySatisfy(indicator -> {
                assertThat(indicator.indicatorId()).isEqualTo("ei-old");
                assertThat(indicator.status()).isEqualTo(EvaluationIndicatorStatus.OFFLINE);
            });
        verify(auditPublisher).publish(AuditAction.PUBLISH, "evaluation_indicator",
            draft.indicatorId(), "发布评估指标 IND.VTE.PROPHYLAXIS");
    }

    @Test
    void indicatorRejectsInvalidStateTransition() {
        EvaluationIndicator active = indicator("ei-active", 1, EvaluationIndicatorStatus.ACTIVE);
        when(indicators.findByIndicatorIdAndTenantId("ei-active", "tenant-A"))
            .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.submitIndicator("ei-active"))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_EVAL_003);
    }

    @Test
    void runRecordsResultAndCreatesTaskForHighRiskFinding() {
        when(indicators.findByIndicatorIdAndTenantId("ei-active", "tenant-A"))
            .thenReturn(Optional.of(indicator("ei-active", 1, EvaluationIndicatorStatus.ACTIVE)));

        EvaluationRunResponse response = service.run(runRequest(QualityFindingSeverity.P1, true));

        assertThat(response.status()).isEqualTo(EvaluationRunStatus.RECORDED);
        assertThat(response.resultCount()).isEqualTo(1);
        assertThat(response.findingCount()).isEqualTo(1);
        assertThat(response.taskCount()).isEqualTo(1);

        ArgumentCaptor<EvaluationResult> result = ArgumentCaptor.forClass(EvaluationResult.class);
        ArgumentCaptor<QualityFinding> finding = ArgumentCaptor.forClass(QualityFinding.class);
        ArgumentCaptor<RectificationTask> task = ArgumentCaptor.forClass(RectificationTask.class);
        verify(results).save(result.capture());
        verify(findings).save(finding.capture());
        verify(tasks).save(task.capture());
        assertThat(result.getValue().indicatorVersion()).isEqualTo(1);
        assertThat(finding.getValue().status()).isEqualTo(QualityFindingStatus.ASSIGNED);
        assertThat(task.getValue().status()).isEqualTo(RectificationTaskStatus.ASSIGNED);
        verify(auditPublisher).publish(AuditAction.EXECUTE, "evaluation_run",
            response.runId(), "接收评估运行 RUN.VTE");
    }

    @Test
    void runRejectsNonActiveIndicatorAndIncompleteHighRiskFinding() {
        when(indicators.findByIndicatorIdAndTenantId("ei-active", "tenant-A"))
            .thenReturn(Optional.of(indicator("ei-active", 1, EvaluationIndicatorStatus.DRAFT)));
        assertThatThrownBy(() -> service.run(runRequest(QualityFindingSeverity.P2, false)))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_EVAL_004);

        when(indicators.findByIndicatorIdAndTenantId("ei-active", "tenant-A"))
            .thenReturn(Optional.of(indicator("ei-active", 1, EvaluationIndicatorStatus.ACTIVE)));
        assertThatThrownBy(() -> service.run(runRequest(QualityFindingSeverity.P0, false)))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_EVAL_006);
    }

    @Test
    void lowerRiskFindingWithoutAssignmentRemainsNew() {
        when(indicators.findByIndicatorIdAndTenantId("ei-active", "tenant-A"))
            .thenReturn(Optional.of(indicator("ei-active", 1, EvaluationIndicatorStatus.ACTIVE)));

        service.run(runRequest(QualityFindingSeverity.P3, false));

        ArgumentCaptor<QualityFinding> finding = ArgumentCaptor.forClass(QualityFinding.class);
        verify(findings).save(finding.capture());
        assertThat(finding.getValue().status()).isEqualTo(QualityFindingStatus.NEW);
        verify(tasks, never()).save(any());
    }

    @Test
    void runRejectsFactWithoutContextOrExplicitManualSource() {
        when(indicators.findByIndicatorIdAndTenantId("ei-active", "tenant-A"))
            .thenReturn(Optional.of(indicator("ei-active", 1, EvaluationIndicatorStatus.ACTIVE)));
        EvaluationResultRequest result = new EvaluationResultRequest(
            "ei-active", EvaluationSubjectType.MEDICAL_RECORD, "record-1", BigDecimal.ONE,
            EvaluationResultLevel.PASS, false, "抽检通过", null, null, List.of());
        EvaluationRunRequest unlinked = new EvaluationRunRequest(
            "RUN.UNLINKED", EvaluationRunType.BATCH_IMPORT, null, null, null, null,
            "DISCHARGE", "1.0.0", "sha256:run", Instant.now(), List.of(result));

        assertThatThrownBy(() -> service.run(unlinked))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_EVAL_001);
    }

    @Test
    void rectificationSubmissionAndApprovalCloseFinding() {
        QualityFinding assigned = finding("qf-1", QualityFindingSeverity.P1, QualityFindingStatus.ASSIGNED);
        RectificationTask task = task("task-1", RectificationTaskStatus.ASSIGNED);
        when(findings.findByFindingIdAndTenantId("qf-1", "tenant-A")).thenReturn(Optional.of(assigned));
        when(tasks.findByFindingIdAndTenantId("qf-1", "tenant-A")).thenReturn(Optional.of(task));

        RectificationResponse submitted = service.submitRectification(
            "qf-1", new RectificationSubmitRequest("补录风险评估记录", "proof-1"));
        assertThat(submitted.findingStatus()).isEqualTo(QualityFindingStatus.REMEDIATING);
        assertThat(submitted.taskStatus()).isEqualTo(RectificationTaskStatus.SUBMITTED);

        QualityFinding remediating = finding("qf-1", QualityFindingSeverity.P1, QualityFindingStatus.REMEDIATING);
        RectificationTask submittedTask = task("task-1", RectificationTaskStatus.SUBMITTED);
        when(findings.findByFindingIdAndTenantId("qf-1", "tenant-A")).thenReturn(Optional.of(remediating));
        when(tasks.findByFindingIdAndTenantId("qf-1", "tenant-A")).thenReturn(Optional.of(submittedTask));

        RectificationReviewResponse approved = service.reviewRectification(
            "qf-1", new RectificationReviewRequest(
                RectificationReviewDecision.APPROVED, "证据充分，允许闭环", "review-proof-1"));
        assertThat(approved.findingStatus()).isEqualTo(QualityFindingStatus.CLOSED);
        assertThat(approved.taskStatus()).isEqualTo(RectificationTaskStatus.CLOSED);
        verify(reviews).save(any(RectificationReview.class));
        verify(auditPublisher).publish(eq(AuditAction.REVIEW), eq("quality_finding"), eq("qf-1"), any());
    }

    @Test
    void p0FindingCannotBeWaivedByOrdinaryReview() {
        when(findings.findByFindingIdAndTenantId("qf-p0", "tenant-A"))
            .thenReturn(Optional.of(finding("qf-p0", QualityFindingSeverity.P0, QualityFindingStatus.REMEDIATING)));
        when(tasks.findByFindingIdAndTenantId("qf-p0", "tenant-A"))
            .thenReturn(Optional.of(task("task-p0", RectificationTaskStatus.SUBMITTED)));

        assertThatThrownBy(() -> service.reviewRectification("qf-p0", new RectificationReviewRequest(
                RectificationReviewDecision.WAIVED, "申请豁免", null)))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_EVAL_007);
    }

    @Test
    void diagnoseAssemblesRunRelatedFacts() {
        EvaluationRun run = evaluationRun("run-1");
        EvaluationResult result = result("result-1", "run-1");
        QualityFinding finding = finding("qf-1", QualityFindingSeverity.P1, QualityFindingStatus.ASSIGNED);
        RectificationTask task = task("task-1", RectificationTaskStatus.ASSIGNED);
        DiagnoseResponse expected = new DiagnoseResponse(
            "evaluation_run", "run-1", "tenant-A", "RECORDED",
            run, List.of(), List.of(),
            Map.of("results", List.of("result-1"), "findings", List.of("qf-1"), "tasks", List.of("task-1")),
            null, "trace-eval", null);
        when(runs.findByRunIdAndTenantId("run-1", "tenant-A")).thenReturn(Optional.of(run));
        when(results.findByRunIdAndTenantIdOrderByCreatedAtAsc("run-1", "tenant-A")).thenReturn(List.of(result));
        when(findings.findByResultIdAndTenantIdOrderByCreatedAtAsc("result-1", "tenant-A"))
            .thenReturn(List.of(finding));
        when(tasks.findByFindingIdAndTenantId("qf-1", "tenant-A")).thenReturn(Optional.of(task));
        when(diagnoseAssembler.assemble(eq("evaluation_run"), eq("run-1"), eq("tenant-A"),
            eq("RECORDED"), eq(run), eq(List.of()), any(), any(), eq("trace-eval")))
            .thenReturn(expected);

        assertThat(service.diagnose("run-1")).isSameAs(expected);
    }

    private EvaluationIndicatorCreateRequest indicatorRequest(int version) {
        return new EvaluationIndicatorCreateRequest(
            "IND.VTE.PROPHYLAXIS", version, "静脉血栓预防完成率", EvaluationSubjectType.MEDICAL_RECORD,
            "符合住院风险分层病例", "完成预防评估病例", "出血高风险除外", "达标率 >= 95%",
            "DISCHARGE+24H", "全院住院科室", "dept-1", "guideline-1", "1.0.0");
    }

    private EvaluationRunRequest runRequest(QualityFindingSeverity severity, boolean assigned) {
        QualityFindingRequest finding = new QualityFindingRequest(
            "FIND.VTE.001", "未完成静脉血栓风险评估", "出院前未记录风险评估", severity,
            "缺少风险评估记录",
            assigned ? "dept-1" : null,
            assigned ? Instant.now().plusSeconds(86400) : null,
            assigned ? "head-1" : null);
        EvaluationResultRequest result = new EvaluationResultRequest(
            "ei-active", EvaluationSubjectType.MEDICAL_RECORD, "record-1", new BigDecimal("70.5000"),
            EvaluationResultLevel.NON_COMPLIANT, true, "指标未达标", "evidence-1", "dept-1",
            List.of(finding));
        return new EvaluationRunRequest(
            "RUN.VTE", EvaluationRunType.UPSTREAM_RESULT, "event-1", "snapshot-1",
            "patient-1", "enc-1", "DISCHARGE", "1.0.0", "sha256:run", Instant.now(), List.of(result));
    }

    private EvaluationIndicator indicator(String indicatorId, int version, EvaluationIndicatorStatus status) {
        Instant now = Instant.now();
        return new EvaluationIndicator(
            null, indicatorId, "tenant-A", "IND.VTE.PROPHYLAXIS", version, "静脉血栓预防完成率",
            EvaluationSubjectType.MEDICAL_RECORD, "分母", "分子", null, null,
            "DISCHARGE+24H", "全院", "dept-1", "guideline-1", "1.0.0", status,
            now, "qa-1", status == EvaluationIndicatorStatus.ACTIVE ? now : null,
            now, "qa-1", now, "qa-1", "trace-eval");
    }

    private EvaluationRun evaluationRun(String runId) {
        Instant now = Instant.now();
        return new EvaluationRun(
            null, runId, "tenant-A", "RUN.VTE", EvaluationRunType.UPSTREAM_RESULT,
            "event-1", "snapshot-1", "patient-1", "enc-1", "DISCHARGE", "1.0.0",
            "sha256:run", EvaluationRunStatus.RECORDED, null, now,
            now, "qa-1", now, "qa-1", "trace-eval");
    }

    private EvaluationResult result(String resultId, String runId) {
        Instant now = Instant.now();
        return new EvaluationResult(
            null, resultId, "tenant-A", runId, "ei-active", "IND.VTE.PROPHYLAXIS", 1,
            EvaluationSubjectType.MEDICAL_RECORD, "record-1", BigDecimal.ONE,
            EvaluationResultLevel.NON_COMPLIANT, true, "未达标", "evidence-1", "dept-1",
            now, "qa-1", now, "qa-1", "trace-eval");
    }

    private QualityFinding finding(String findingId, QualityFindingSeverity severity, QualityFindingStatus status) {
        Instant now = Instant.now();
        return new QualityFinding(
            null, findingId, "tenant-A", "run-1", "result-1", "ei-active", "FIND.VTE.001",
            "未完成静脉血栓风险评估", "出院前未记录风险评估", severity, status,
            "缺少风险评估记录", "dept-1", now.plusSeconds(86400),
            now, "qa-1", now, "qa-1", "trace-eval");
    }

    private RectificationTask task(String taskId, RectificationTaskStatus status) {
        Instant now = Instant.now();
        return new RectificationTask(
            null, taskId, "tenant-A", "qf-1", "dept-1", "head-1", status,
            now.plusSeconds(86400), null, null, null, null, null,
            now, "qa-1", now, "qa-1", "trace-eval");
    }
}
