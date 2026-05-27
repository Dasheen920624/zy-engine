package com.medkernel.engine.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.observability.DiagnoseResponseAssembler;
import com.medkernel.shared.observability.StateTransitionRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJdbcTest
@Import(EvaluationEngineService.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:evaluation-flow-${random.uuid};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration/h2"
})
class EvaluationEngineIntegrationTest {

    @Autowired EvaluationEngineService service;
    @Autowired EvaluationIndicatorRepository indicators;
    @Autowired EvaluationRunRepository runs;
    @Autowired EvaluationResultRepository results;
    @Autowired QualityFindingRepository findings;
    @Autowired RectificationTaskRepository tasks;
    @Autowired RectificationReviewRepository reviews;
    @Autowired EvaluationIdempotencyKeyRepository idempotencyKeys;

    @MockBean AuditEventPublisher auditPublisher;
    @MockBean StateTransitionRecorder transitions;
    @MockBean DiagnoseResponseAssembler diagnoseAssembler;

    @BeforeEach
    void setUp() {
        RequestContext.restore(new RequestContext.Snapshot(
            "trace-real-flow", OrgScope.tenant("tenant-A"), "qa-1"));
    }

    @AfterEach
    void wipe() {
        RequestContext.clear();
        idempotencyKeys.deleteAll();
        reviews.deleteAll();
        tasks.deleteAll();
        findings.deleteAll();
        results.deleteAll();
        runs.deleteAll();
        indicators.deleteAll();
    }

    @Test
    void persistsIdempotentIndicatorRunRectificationAndReviewWorkflow() {
        EvaluationIndicator indicator = service.createIndicator(new EvaluationIndicatorCreateRequest(
            "IND.VTE.PROPHYLAXIS", 1, "静脉血栓预防完成率", EvaluationSubjectType.MEDICAL_RECORD,
            "符合住院风险分层病例", "完成预防评估病例", null, null,
            "DISCHARGE+24H", "全院住院科室", "dept-1", "guideline-1", "1.0.0"));
        service.submitIndicator(indicator.indicatorId());
        service.publishIndicator(indicator.indicatorId());
        service.activateIndicator(indicator.indicatorId());

        EvaluationRunResponse run = service.run(new EvaluationRunRequest(
            "RUN.VTE", EvaluationRunType.UPSTREAM_RESULT, "event-1", "snapshot-1",
            "patient-1", "enc-1", "DISCHARGE", "1.0.0", "sha256:run", Instant.now(),
            List.of(new EvaluationResultRequest(
                indicator.indicatorId(), EvaluationSubjectType.MEDICAL_RECORD, "record-1",
                new BigDecimal("70.5000"), EvaluationResultLevel.NON_COMPLIANT, true,
                "指标未达标", "evidence-1", "dept-1",
                List.of(new QualityFindingRequest(
                    "FIND.VTE.001", "未完成静脉血栓风险评估", "出院前未记录风险评估",
                    QualityFindingSeverity.P1, "缺少风险评估记录", "dept-1",
                    Instant.now().plusSeconds(86400), "head-1"))))));

        QualityFinding finding = service.listFindings(
            new QualityFindingFilter(QualityFindingSeverity.P1, null, "dept-1"),
            PageRequest.defaults()).items().getFirst();
        assertThat(run.taskCount()).isEqualTo(1);
        assertThat(service.listIndicators(null, PageRequest.defaults()).items())
            .extracting(EvaluationIndicator::status)
            .containsExactly(EvaluationIndicatorStatus.ACTIVE);
        assertThat(service.listResults(null, PageRequest.defaults()).items())
            .extracting(EvaluationResult::resultLevel)
            .containsExactly(EvaluationResultLevel.NON_COMPLIANT);

        RectificationSubmitRequest rectification =
            new RectificationSubmitRequest("补录风险评估记录", "proof-1");
        RectificationResponse submitted = service.submitRectification(
            finding.findingId(), rectification, "idem-rectification-1");
        RectificationResponse submittedReplay = service.submitRectification(
            finding.findingId(), rectification, "idem-rectification-1");
        assertThat(submittedReplay).isEqualTo(submitted);
        assertThatThrownBy(() -> service.submitRectification(
                finding.findingId(), new RectificationSubmitRequest("更换整改内容", "proof-2"),
                "idem-rectification-1"))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_EVAL_008);

        RectificationReviewRequest review =
            new RectificationReviewRequest(
                RectificationReviewDecision.APPROVED, "证据充分，允许闭环", "review-proof-1");
        RectificationReviewResponse approved = service.reviewRectification(
            finding.findingId(), review, "idem-review-1");
        RectificationReviewResponse approvedReplay = service.reviewRectification(
            finding.findingId(), review, "idem-review-1");
        assertThat(approvedReplay).isEqualTo(approved);
        assertThatThrownBy(() -> service.reviewRectification(
                finding.findingId(), new RectificationReviewRequest(
                    RectificationReviewDecision.WAIVED, "更换复核结论", null), "idem-review-1"))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_EVAL_008);

        QualityFindingDetailResponse detail = service.findingDetail(finding.findingId());
        assertThat(detail.finding().status()).isEqualTo(QualityFindingStatus.CLOSED);
        assertThat(detail.rectificationTask().status()).isEqualTo(RectificationTaskStatus.CLOSED);
        assertThat(detail.reviews())
            .extracting(RectificationReview::decision)
            .containsExactly(RectificationReviewDecision.APPROVED);
    }
}
