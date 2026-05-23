package com.medkernel.quality;

import com.medkernel.organization.OrganizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvalReportServiceTest {

    @Mock
    private EvalScoringService evalScoringService;

    private EvalReportService service;

    private OrganizationContext orgContext;

    @BeforeEach
    void setUp() {
        service = new EvalReportService(evalScoringService);
        orgContext = new OrganizationContext();
        orgContext.setTenantId("tenant1");
        orgContext.setHospitalCode("HOSP001");
        orgContext.setGroupCode("GROUP1");
        orgContext.setSource("HEADER");
    }

    // ==================== generateReport ====================

    @Test
    void shouldThrowException_whenEvalResultNotFound() {
        when(evalScoringService.getResult(eq("NONEXISTENT"), any(OrganizationContext.class))).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> service.generateReport("NONEXISTENT", orgContext));
    }

    @Test
    void shouldGenerateReport_fromEvalResult() {
        EvalResult evalResult = createEvalResult("EVAL-001", "SET-001", "MODEL", "model-1", "GPT-4",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-001"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-001", orgContext);

        assertNotNull(report.getReportId());
        assertEquals("EVAL-001", report.getEvalId());
        assertEquals("tenant1", report.getTenantId());
        assertEquals("SET-001", report.getSetCode());
        assertEquals("MODEL", report.getSubjectType());
        assertEquals("model-1", report.getSubjectId());
        assertEquals("GPT-4", report.getSubjectName());
        assertEquals(85.0, report.getTotalScore(), 0.01);
        assertEquals(100.0, report.getMaxPossibleScore(), 0.01);
        assertEquals("INFO", report.getRiskLevel());
        assertEquals("DRAFT", report.getStatus());
        assertNotNull(report.getRecommendations());
    }

    @Test
    void shouldGenerateRecommendations_forCriticalRisk() {
        EvalResult evalResult = createEvalResult("EVAL-CRIT", "SET-001", "MODEL", "model-1", "Test",
                10.0, 100.0, "CRITICAL");
        when(evalScoringService.getResult(eq("EVAL-CRIT"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-CRIT", orgContext);
        assertTrue(report.getRecommendations().stream()
                .anyMatch(r -> r.contains("严重风险")));
    }

    @Test
    void shouldGenerateRecommendations_forHighRisk() {
        EvalResult evalResult = createEvalResult("EVAL-HIGH", "SET-001", "MODEL", "model-1", "Test",
                40.0, 100.0, "HIGH");
        when(evalScoringService.getResult(eq("EVAL-HIGH"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-HIGH", orgContext);
        assertTrue(report.getRecommendations().stream()
                .anyMatch(r -> r.contains("高风险")));
    }

    @Test
    void shouldGenerateRecommendations_forMediumRisk() {
        EvalResult evalResult = createEvalResult("EVAL-MED", "SET-001", "MODEL", "model-1", "Test",
                70.0, 100.0, "MEDIUM");
        when(evalScoringService.getResult(eq("EVAL-MED"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-MED", orgContext);
        assertTrue(report.getRecommendations().stream()
                .anyMatch(r -> r.contains("中等风险")));
    }

    @Test
    void shouldGenerateRecommendations_forAbnormalFacts() {
        EvalResult evalResult = createEvalResult("EVAL-ABN", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        List<EvalResult.EvalFact> abnormalFacts = new ArrayList<EvalResult.EvalFact>();
        EvalResult.EvalFact fact = new EvalResult.EvalFact();
        fact.setFactType("ABNORMAL");
        fact.setIndicatorCode("IND-001");
        fact.setIndicatorName("Accuracy");
        fact.setDescription("Below threshold");
        fact.setSeverity("HIGH");
        abnormalFacts.add(fact);
        evalResult.setAbnormalFacts(abnormalFacts);

        when(evalScoringService.getResult(eq("EVAL-ABN"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-ABN", orgContext);
        assertTrue(report.getRecommendations().stream()
                .anyMatch(r -> r.contains("1") && r.contains("异常")));
    }

    @Test
    void shouldGenerateRecommendations_forMissingFacts() {
        EvalResult evalResult = createEvalResult("EVAL-MISS", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        List<EvalResult.EvalFact> missingFacts = new ArrayList<EvalResult.EvalFact>();
        EvalResult.EvalFact fact = new EvalResult.EvalFact();
        fact.setFactType("MISSING");
        fact.setIndicatorCode("IND-002");
        fact.setIndicatorName("Reliability");
        fact.setDescription("Data missing");
        fact.setSeverity("HIGH");
        missingFacts.add(fact);
        evalResult.setMissingFacts(missingFacts);

        when(evalScoringService.getResult(eq("EVAL-MISS"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-MISS", orgContext);
        assertTrue(report.getRecommendations().stream()
                .anyMatch(r -> r.contains("1") && r.contains("缺失")));
    }

    // ==================== exportReport ====================

    @Test
    void shouldExportReport() {
        EvalResult evalResult = createEvalResult("EVAL-EXP", "SET-001", "MODEL", "model-1", "GPT-4",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-EXP"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-EXP", orgContext);
        Map<String, Object> exported = service.exportReport(report.getReportId(), orgContext);

        assertNotNull(exported);
        assertEquals(report.getReportId(), exported.get("report_id"));
        assertEquals("DRAFT", exported.get("status"));
    }

    @Test
    void shouldThrowException_whenExportNonexistentReport() {
        assertThrows(IllegalArgumentException.class,
                () -> service.exportReport("NONEXISTENT", orgContext));
    }

    // ==================== getReport ====================

    @Test
    void shouldReturnNull_whenReportNotFound() {
        EvalReportService.EvalReport report = service.getReport("NONEXISTENT", orgContext);
        assertNull(report);
    }

    @Test
    void shouldReturnNull_whenTenantMismatch() {
        EvalResult evalResult = createEvalResult("EVAL-TM", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-TM"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-TM", orgContext);

        OrganizationContext otherTenant = new OrganizationContext();
        otherTenant.setTenantId("tenant2");

        EvalReportService.EvalReport retrieved = service.getReport(report.getReportId(), otherTenant);
        assertNull(retrieved);
    }

    // ==================== listReports ====================

    @Test
    void shouldListReports_withFilters() {
        EvalResult evalResult = createEvalResult("EVAL-LIST", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-LIST"), any(OrganizationContext.class))).thenReturn(evalResult);

        service.generateReport("EVAL-LIST", orgContext);

        List<EvalReportService.EvalReport> reports = service.listReports("DRAFT", "MODEL", orgContext);
        assertEquals(1, reports.size());
    }

    @Test
    void shouldListReports_withNoMatch() {
        EvalResult evalResult = createEvalResult("EVAL-LIST2", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-LIST2"), any(OrganizationContext.class))).thenReturn(evalResult);

        service.generateReport("EVAL-LIST2", orgContext);

        List<EvalReportService.EvalReport> reports = service.listReports("ARCHIVED", null, orgContext);
        assertTrue(reports.isEmpty());
    }

    // ==================== archiveReport ====================

    @Test
    void shouldThrowException_whenArchiveNonexistentReport() {
        assertThrows(IllegalArgumentException.class,
                () -> service.archiveReport("NONEXISTENT", orgContext));
    }

    @Test
    void shouldThrowException_whenArchiveNonReviewedReport() {
        EvalResult evalResult = createEvalResult("EVAL-ARCH", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-ARCH"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-ARCH", orgContext);
        // Report is in DRAFT status, not REVIEWED

        assertThrows(IllegalArgumentException.class,
                () -> service.archiveReport(report.getReportId(), orgContext));
    }

    @Test
    void shouldArchiveReport_whenReviewed() {
        EvalResult evalResult = createEvalResult("EVAL-ARCH2", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-ARCH2"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-ARCH2", orgContext);

        // Submit approved review to change status to REVIEWED
        Map<String, Object> reviewReq = new HashMap<String, Object>();
        reviewReq.put("reviewer_id", "rev1");
        reviewReq.put("reviewer_name", "Reviewer");
        reviewReq.put("review_result", "APPROVED");
        reviewReq.put("review_comment", "Good");
        service.submitReview(report.getReportId(), reviewReq, orgContext);

        EvalReportService.EvalReport archived = service.archiveReport(report.getReportId(), orgContext);
        assertEquals("ARCHIVED", archived.getStatus());
        assertNotNull(archived.getArchivedAt());
    }

    // ==================== submitReview ====================

    @Test
    void shouldThrowException_whenReviewNonexistentReport() {
        Map<String, Object> reviewReq = new HashMap<String, Object>();
        reviewReq.put("review_result", "APPROVED");

        assertThrows(IllegalArgumentException.class,
                () -> service.submitReview("NONEXISTENT", reviewReq, orgContext));
    }

    @Test
    void shouldSubmitApprovedReview() {
        EvalResult evalResult = createEvalResult("EVAL-REV1", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-REV1"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-REV1", orgContext);

        Map<String, Object> reviewReq = new HashMap<String, Object>();
        reviewReq.put("reviewer_id", "rev1");
        reviewReq.put("reviewer_name", "Dr. Reviewer");
        reviewReq.put("review_result", "APPROVED");
        reviewReq.put("review_comment", "Looks good");

        EvalReportService.EvalReview review = service.submitReview(report.getReportId(), reviewReq, orgContext);
        assertNotNull(review.getReviewId());
        assertEquals("APPROVED", review.getReviewResult());
        assertEquals("Dr. Reviewer", review.getReviewerName());
        assertEquals("REVIEWED", report.getStatus());
    }

    @Test
    void shouldSubmitRejectedReview() {
        EvalResult evalResult = createEvalResult("EVAL-REV2", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-REV2"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-REV2", orgContext);

        Map<String, Object> reviewReq = new HashMap<String, Object>();
        reviewReq.put("review_result", "REJECTED");
        reviewReq.put("review_comment", "Need improvement");

        service.submitReview(report.getReportId(), reviewReq, orgContext);
        assertEquals("REVIEW_REJECTED", report.getStatus());
    }

    @Test
    void shouldSubmitConditionallyApprovedReview() {
        EvalResult evalResult = createEvalResult("EVAL-REV3", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-REV3"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-REV3", orgContext);

        Map<String, Object> reviewReq = new HashMap<String, Object>();
        reviewReq.put("review_result", "CONDITIONALLY_APPROVED");
        reviewReq.put("review_comment", "Approved with conditions");

        service.submitReview(report.getReportId(), reviewReq, orgContext);
        assertEquals("CONDITIONALLY_APPROVED", report.getStatus());
    }

    @Test
    void shouldThrowException_whenInvalidReviewResult() {
        EvalResult evalResult = createEvalResult("EVAL-REV4", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-REV4"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-REV4", orgContext);

        Map<String, Object> reviewReq = new HashMap<String, Object>();
        reviewReq.put("review_result", "INVALID");

        assertThrows(IllegalArgumentException.class,
                () -> service.submitReview(report.getReportId(), reviewReq, orgContext));
    }

    // ==================== listReviews ====================

    @Test
    void shouldListReviews_forReport() {
        EvalResult evalResult = createEvalResult("EVAL-LREV", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-LREV"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-LREV", orgContext);

        Map<String, Object> reviewReq = new HashMap<String, Object>();
        reviewReq.put("review_result", "APPROVED");
        reviewReq.put("reviewer_name", "Reviewer 1");
        service.submitReview(report.getReportId(), reviewReq, orgContext);

        List<EvalReportService.EvalReview> reviews = service.listReviews(report.getReportId(), orgContext);
        assertEquals(1, reviews.size());
    }

    // ==================== createRectification ====================

    @Test
    void shouldCreateRectification() {
        EvalResult evalResult = createEvalResult("EVAL-RECT", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-RECT"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-RECT", orgContext);

        Map<String, Object> rectReq = new HashMap<String, Object>();
        rectReq.put("title", "Fix accuracy issue");
        rectReq.put("description", "Accuracy below threshold");
        rectReq.put("assignee_id", "user1");
        rectReq.put("assignee_name", "Engineer Zhang");
        rectReq.put("priority", "HIGH");
        rectReq.put("due_date", "2026-06-30");

        EvalReportService.EvalRectification rect = service.createRectification(report.getReportId(), rectReq, orgContext);
        assertNotNull(rect.getRectId());
        assertEquals("Fix accuracy issue", rect.getTitle());
        assertEquals("HIGH", rect.getPriority());
        assertEquals("PENDING", rect.getStatus());
        assertEquals(report.getReportId(), rect.getReportId());
    }

    @Test
    void shouldThrowException_whenCreateRectificationForNonexistentReport() {
        Map<String, Object> rectReq = new HashMap<String, Object>();
        rectReq.put("title", "Test");

        assertThrows(IllegalArgumentException.class,
                () -> service.createRectification("NONEXISTENT", rectReq, orgContext));
    }

    // ==================== autoCreateRectifications ====================

    @Test
    void shouldAutoCreateRectifications_fromAbnormalFacts() {
        EvalResult evalResult = createEvalResult("EVAL-AUTO", "SET-001", "MODEL", "model-1", "Test",
                50.0, 100.0, "HIGH");
        List<EvalResult.EvalFact> abnormalFacts = new ArrayList<EvalResult.EvalFact>();
        EvalResult.EvalFact fact = new EvalResult.EvalFact();
        fact.setFactType("ABNORMAL");
        fact.setIndicatorCode("IND-001");
        fact.setIndicatorName("Accuracy");
        fact.setDescription("Below threshold");
        fact.setSeverity("HIGH");
        abnormalFacts.add(fact);
        evalResult.setAbnormalFacts(abnormalFacts);

        when(evalScoringService.getResult(eq("EVAL-AUTO"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-AUTO", orgContext);

        List<EvalReportService.EvalRectification> rects = service.autoCreateRectifications(report.getReportId(), orgContext);
        assertEquals(1, rects.size());
        assertTrue(rects.get(0).getTitle().contains("Accuracy"));
    }

    @Test
    void shouldAutoCreateRectifications_fromMissingFacts() {
        EvalResult evalResult = createEvalResult("EVAL-AUTO2", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        List<EvalResult.EvalFact> missingFacts = new ArrayList<EvalResult.EvalFact>();
        EvalResult.EvalFact fact = new EvalResult.EvalFact();
        fact.setFactType("MISSING");
        fact.setIndicatorCode("IND-002");
        fact.setIndicatorName("Reliability");
        fact.setDescription("Data missing");
        fact.setSeverity("HIGH");
        missingFacts.add(fact);
        evalResult.setMissingFacts(missingFacts);

        when(evalScoringService.getResult(eq("EVAL-AUTO2"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-AUTO2", orgContext);

        List<EvalReportService.EvalRectification> rects = service.autoCreateRectifications(report.getReportId(), orgContext);
        assertEquals(1, rects.size());
        assertTrue(rects.get(0).getTitle().contains("Reliability"));
    }

    @Test
    void shouldThrowException_whenAutoCreateForNonexistentReport() {
        assertThrows(IllegalArgumentException.class,
                () -> service.autoCreateRectifications("NONEXISTENT", orgContext));
    }

    // ==================== updateRectificationStatus ====================

    @Test
    void shouldUpdateStatus_fromPendingToInProgress() {
        EvalResult evalResult = createEvalResult("EVAL-UST", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-UST"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-UST", orgContext);

        Map<String, Object> rectReq = new HashMap<String, Object>();
        rectReq.put("title", "Fix issue");
        EvalReportService.EvalRectification rect = service.createRectification(report.getReportId(), rectReq, orgContext);

        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("status", "IN_PROGRESS");
        updateReq.put("updated_by", "user1");

        EvalReportService.EvalRectification updated = service.updateRectificationStatus(rect.getRectId(), updateReq, orgContext);
        assertEquals("IN_PROGRESS", updated.getStatus());
    }

    @Test
    void shouldUpdateStatus_fromInProgressToCompleted() {
        EvalResult evalResult = createEvalResult("EVAL-UST2", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-UST2"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-UST2", orgContext);

        Map<String, Object> rectReq = new HashMap<String, Object>();
        rectReq.put("title", "Fix issue");
        EvalReportService.EvalRectification rect = service.createRectification(report.getReportId(), rectReq, orgContext);

        // First: PENDING -> IN_PROGRESS
        Map<String, Object> startReq = new HashMap<String, Object>();
        startReq.put("status", "IN_PROGRESS");
        service.updateRectificationStatus(rect.getRectId(), startReq, orgContext);

        // Then: IN_PROGRESS -> COMPLETED
        Map<String, Object> completeReq = new HashMap<String, Object>();
        completeReq.put("status", "COMPLETED");
        completeReq.put("updated_by", "user1");

        EvalReportService.EvalRectification completed = service.updateRectificationStatus(rect.getRectId(), completeReq, orgContext);
        assertEquals("COMPLETED", completed.getStatus());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    void shouldThrowException_whenInvalidStatusTransition() {
        EvalResult evalResult = createEvalResult("EVAL-UST3", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-UST3"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-UST3", orgContext);

        Map<String, Object> rectReq = new HashMap<String, Object>();
        rectReq.put("title", "Fix issue");
        EvalReportService.EvalRectification rect = service.createRectification(report.getReportId(), rectReq, orgContext);

        // Cannot go from PENDING directly to COMPLETED
        Map<String, Object> completeReq = new HashMap<String, Object>();
        completeReq.put("status", "COMPLETED");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateRectificationStatus(rect.getRectId(), completeReq, orgContext));
    }

    @Test
    void shouldThrowException_whenUpdateCompletedTask() {
        EvalResult evalResult = createEvalResult("EVAL-UST4", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-UST4"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-UST4", orgContext);

        Map<String, Object> rectReq = new HashMap<String, Object>();
        rectReq.put("title", "Fix issue");
        EvalReportService.EvalRectification rect = service.createRectification(report.getReportId(), rectReq, orgContext);

        // PENDING -> IN_PROGRESS -> COMPLETED
        Map<String, Object> startReq = new HashMap<String, Object>();
        startReq.put("status", "IN_PROGRESS");
        service.updateRectificationStatus(rect.getRectId(), startReq, orgContext);

        Map<String, Object> completeReq = new HashMap<String, Object>();
        completeReq.put("status", "COMPLETED");
        service.updateRectificationStatus(rect.getRectId(), completeReq, orgContext);

        // Now try to update completed task
        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("status", "IN_PROGRESS");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateRectificationStatus(rect.getRectId(), updateReq, orgContext));
    }

    @Test
    void shouldThrowException_whenRectificationNotFound() {
        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("status", "IN_PROGRESS");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateRectificationStatus("NONEXISTENT", updateReq, orgContext));
    }

    @Test
    void shouldThrowException_whenStatusIsNull() {
        EvalResult evalResult = createEvalResult("EVAL-UST5", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-UST5"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-UST5", orgContext);

        Map<String, Object> rectReq = new HashMap<String, Object>();
        rectReq.put("title", "Fix issue");
        EvalReportService.EvalRectification rect = service.createRectification(report.getReportId(), rectReq, orgContext);

        Map<String, Object> updateReq = new HashMap<String, Object>();
        // No status

        assertThrows(IllegalArgumentException.class,
                () -> service.updateRectificationStatus(rect.getRectId(), updateReq, orgContext));
    }

    // ==================== getRectification ====================

    @Test
    void shouldReturnNull_whenRectificationNotFound() {
        EvalReportService.EvalRectification rect = service.getRectification("NONEXISTENT", orgContext);
        assertNull(rect);
    }

    // ==================== listRectifications ====================

    @Test
    void shouldListRectifications() {
        EvalResult evalResult = createEvalResult("EVAL-LRECT", "SET-001", "MODEL", "model-1", "Test",
                85.0, 100.0, "INFO");
        when(evalScoringService.getResult(eq("EVAL-LRECT"), any(OrganizationContext.class))).thenReturn(evalResult);

        EvalReportService.EvalReport report = service.generateReport("EVAL-LRECT", orgContext);

        Map<String, Object> rectReq = new HashMap<String, Object>();
        rectReq.put("title", "Fix issue 1");
        service.createRectification(report.getReportId(), rectReq, orgContext);

        List<EvalReportService.EvalRectification> rects = service.listRectifications(report.getReportId(), null, orgContext);
        assertEquals(1, rects.size());
    }

    // ==================== reEvaluate ====================

    @Test
    void shouldThrowException_whenOriginalResultNotFound() {
        when(evalScoringService.getResult(eq("NONEXISTENT"), any(OrganizationContext.class))).thenReturn(null);

        Map<String, Object> inputData = new HashMap<String, Object>();
        assertThrows(IllegalArgumentException.class,
                () -> service.reEvaluate("NONEXISTENT", inputData, orgContext));
    }

    @Test
    void shouldReEvaluate_withNewInputData() {
        EvalResult original = createEvalResult("EVAL-REEV", "SET-001", "MODEL", "model-1", "Test",
                50.0, 100.0, "HIGH");
        when(evalScoringService.getResult(eq("EVAL-REEV"), any(OrganizationContext.class))).thenReturn(original);

        EvalResult newResult = createEvalResult("EVAL-REEV-NEW", "SET-001", "MODEL", "model-1", "Test",
                90.0, 100.0, "INFO");
        when(evalScoringService.evaluate(eq("SET-001"), eq("model-1"), eq("Test"),
                anyMap(), any(OrganizationContext.class))).thenReturn(newResult);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 90);

        EvalResult result = service.reEvaluate("EVAL-REEV", inputData, orgContext);
        assertNotNull(result);
        verify(evalScoringService).evaluate(eq("SET-001"), eq("model-1"), eq("Test"),
                anyMap(), any(OrganizationContext.class));
    }

    // ==================== Helper ====================

    private EvalResult createEvalResult(String evalId, String setCode, String subjectType,
                                         String subjectId, String subjectName,
                                         double totalScore, double maxScore, String riskLevel) {
        EvalResult result = new EvalResult();
        result.setEvalId(evalId);
        result.setTenantId("tenant1");
        result.setSetCode(setCode);
        result.setSubjectType(subjectType);
        result.setSubjectId(subjectId);
        result.setSubjectName(subjectName);
        result.setTotalScore(totalScore);
        result.setMaxPossibleScore(maxScore);
        result.setRiskLevel(riskLevel);
        result.setIndicatorScores(new ArrayList<EvalResult.IndicatorScore>());
        result.setAbnormalFacts(new ArrayList<EvalResult.EvalFact>());
        result.setMissingFacts(new ArrayList<EvalResult.EvalFact>());
        return result;
    }
}
