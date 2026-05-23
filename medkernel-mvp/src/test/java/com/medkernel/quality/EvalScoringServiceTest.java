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
class EvalScoringServiceTest {

    @Mock
    private EvalService evalService;

    private EvalScoringService service;

    private OrganizationContext orgContext;

    @BeforeEach
    void setUp() {
        service = new EvalScoringService(evalService);
        orgContext = new OrganizationContext();
        orgContext.setTenantId("tenant1");
        orgContext.setHospitalCode("HOSP001");
        orgContext.setGroupCode("GROUP1");
        orgContext.setCampusCode("CAMPUS1");
        orgContext.setSiteCode("SITE1");
        orgContext.setDepartmentCode("DEPT1");
        orgContext.setSource("HEADER");
    }

    // ==================== evaluate ====================

    @Test
    void shouldThrowException_whenIndicatorSetNotFound() {
        when(evalService.getSet(eq("NONEXISTENT"), any(OrganizationContext.class))).thenReturn(null);

        Map<String, Object> inputData = new HashMap<String, Object>();
        assertThrows(IllegalArgumentException.class,
                () -> service.evaluate("NONEXISTENT", "sub1", "Subject 1", inputData, orgContext));
    }

    @Test
    void shouldThrowException_whenIndicatorSetNotPublished() {
        EvalIndicatorSet set = new EvalIndicatorSet();
        set.setSetCode("SET-001");
        set.setStatus("DRAFT");
        set.setSubjectType("MODEL");

        when(evalService.getSet(eq("SET-001"), any(OrganizationContext.class))).thenReturn(set);

        Map<String, Object> inputData = new HashMap<String, Object>();
        assertThrows(IllegalArgumentException.class,
                () -> service.evaluate("SET-001", "sub1", "Subject 1", inputData, orgContext));
    }

    @Test
    void shouldThrowException_whenNoIndicators() {
        EvalIndicatorSet set = new EvalIndicatorSet();
        set.setSetCode("SET-002");
        set.setStatus("PUBLISHED");
        set.setSubjectType("MODEL");

        when(evalService.getSet(eq("SET-002"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-002")).thenReturn(new ArrayList<EvalIndicator>());

        Map<String, Object> inputData = new HashMap<String, Object>();
        assertThrows(IllegalArgumentException.class,
                () -> service.evaluate("SET-002", "sub1", "Subject 1", inputData, orgContext));
    }

    @Test
    void shouldEvaluate_withAllIndicatorsMet() {
        EvalIndicatorSet set = createPublishedSet("SET-100");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();

        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 0.5, 100.0, "score >= 60");
        indicators.add(ind1);
        EvalIndicator ind2 = createIndicator("IND-002", "Reliability", "SCORE", 0.5, 100.0, "score >= 70");
        indicators.add(ind2);

        when(evalService.getSet(eq("SET-100"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-100")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 85);
        inputData.put("IND-002", 90);

        EvalResult result = service.evaluate("SET-100", "model-1", "GPT-4", inputData, orgContext);

        assertNotNull(result.getEvalId());
        assertEquals("tenant1", result.getTenantId());
        assertEquals("SET-100", result.getSetCode());
        assertEquals("model-1", result.getSubjectId());
        assertEquals("GPT-4", result.getSubjectName());
        assertEquals(87.5, result.getTotalScore(), 0.01);
        assertEquals(100.0, result.getMaxPossibleScore(), 0.01);
        assertTrue(result.getIndicatorScores().size() == 2);
        assertTrue(result.getAbnormalFacts().isEmpty());
        assertTrue(result.getMissingFacts().isEmpty());
    }

    @Test
    void shouldEvaluate_withMissingInputData() {
        EvalIndicatorSet set = createPublishedSet("SET-200");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();

        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 1.0, 100.0, "score >= 60");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-200"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-200")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        // No data for IND-001

        EvalResult result = service.evaluate("SET-200", "model-1", "Test Model", inputData, orgContext);

        assertEquals(0, result.getTotalScore(), 0.01);
        assertTrue(result.getMissingFacts().size() == 1);
        assertEquals("MISSING", result.getMissingFacts().get(0).getFactType());
        assertEquals("IND-001", result.getMissingFacts().get(0).getIndicatorCode());
    }

    @Test
    void shouldEvaluate_withThresholdNotMet() {
        EvalIndicatorSet set = createPublishedSet("SET-300");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();

        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 1.0, 100.0, "score >= 80");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-300"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-300")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 50);

        EvalResult result = service.evaluate("SET-300", "model-1", "Test Model", inputData, orgContext);

        assertTrue(result.getAbnormalFacts().size() == 1);
        assertEquals("ABNORMAL", result.getAbnormalFacts().get(0).getFactType());
        assertFalse(result.getIndicatorScores().get(0).isThresholdMet());
    }

    @Test
    void shouldEvaluate_withLessThanOrEqual() {
        EvalIndicatorSet set = createPublishedSet("SET-400");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();

        EvalIndicator ind1 = createIndicator("IND-001", "Error Rate", "RATE", 1.0, 100.0, "score <= 5");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-400"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-400")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 3);

        EvalResult result = service.evaluate("SET-400", "model-1", "Test Model", inputData, orgContext);
        assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
    }

    @Test
    void shouldEvaluate_withGreaterThan() {
        EvalIndicatorSet set = createPublishedSet("SET-401");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();

        EvalIndicator ind1 = createIndicator("IND-001", "Coverage", "RATE", 1.0, 100.0, "score > 90");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-401"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-401")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 95);

        EvalResult result = service.evaluate("SET-401", "model-1", "Test Model", inputData, orgContext);
        assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
    }

    @Test
    void shouldEvaluate_withLessThan() {
        EvalIndicatorSet set = createPublishedSet("SET-402");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();

        EvalIndicator ind1 = createIndicator("IND-001", "Latency", "SCORE", 1.0, 100.0, "score < 10");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-402"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-402")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 5);

        EvalResult result = service.evaluate("SET-402", "model-1", "Test Model", inputData, orgContext);
        assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
    }

    @Test
    void shouldDetermineOverallRiskLevel_CRITICAL() {
        EvalIndicatorSet set = createPublishedSet("SET-CRIT");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();
        EvalIndicator ind1 = createIndicator("IND-001", "Safety", "SCORE", 1.0, 100.0, "score >= 90");
        ind1.setRiskLevelMapping("{\"CRITICAL\":\"score<10\",\"HIGH\":\"score<30\"}");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-CRIT"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-CRIT")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 5);

        EvalResult result = service.evaluate("SET-CRIT", "model-1", "Test", inputData, orgContext);
        assertEquals("CRITICAL", result.getRiskLevel());
    }

    @Test
    void shouldDetermineOverallRiskLevel_HIGH() {
        EvalIndicatorSet set = createPublishedSet("SET-HIGH");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();
        EvalIndicator ind1 = createIndicator("IND-001", "Safety", "SCORE", 1.0, 100.0, "score >= 90");
        ind1.setRiskLevelMapping("{\"HIGH\":\"score<30\"}");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-HIGH"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-HIGH")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 20);

        EvalResult result = service.evaluate("SET-HIGH", "model-1", "Test", inputData, orgContext);
        assertEquals("HIGH", result.getRiskLevel());
    }

    @Test
    void shouldDetermineOverallRiskLevel_INFO_whenAllGood() {
        EvalIndicatorSet set = createPublishedSet("SET-INFO");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();
        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 1.0, 100.0, "score >= 60");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-INFO"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-INFO")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 95);

        EvalResult result = service.evaluate("SET-INFO", "model-1", "Test", inputData, orgContext);
        assertEquals("INFO", result.getRiskLevel());
    }

    @Test
    void shouldHandleNoThresholdExpression() {
        EvalIndicatorSet set = createPublishedSet("SET-NOTHRESH");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();
        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 1.0, 100.0, null);
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-NOTHRESH"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-NOTHRESH")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 50);

        EvalResult result = service.evaluate("SET-NOTHRESH", "model-1", "Test", inputData, orgContext);
        assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
    }

    @Test
    void shouldHandleStringInputValue() {
        EvalIndicatorSet set = createPublishedSet("SET-STR");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();
        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 1.0, 100.0, "score >= 60");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-STR"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-STR")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", "85");

        EvalResult result = service.evaluate("SET-STR", "model-1", "Test", inputData, orgContext);
        assertEquals(85.0, result.getIndicatorScores().get(0).getRawScore(), 0.01);
    }

    // ==================== getResult ====================

    @Test
    void shouldReturnNull_whenResultNotFound() {
        EvalResult result = service.getResult("NONEXISTENT", orgContext);
        assertNull(result);
    }

    @Test
    void shouldReturnNull_whenTenantMismatch() {
        // First create a result
        EvalIndicatorSet set = createPublishedSet("SET-TM");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();
        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 1.0, 100.0, "score >= 60");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-TM"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-TM")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 85);

        EvalResult created = service.evaluate("SET-TM", "model-1", "Test", inputData, orgContext);

        // Try to get with different tenant
        OrganizationContext otherTenant = new OrganizationContext();
        otherTenant.setTenantId("tenant2");

        EvalResult result = service.getResult(created.getEvalId(), otherTenant);
        assertNull(result);
    }

    @Test
    void shouldReturnResult_whenExists() {
        EvalIndicatorSet set = createPublishedSet("SET-GET");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();
        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 1.0, 100.0, "score >= 60");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-GET"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-GET")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 85);

        EvalResult created = service.evaluate("SET-GET", "model-1", "Test", inputData, orgContext);
        EvalResult retrieved = service.getResult(created.getEvalId(), orgContext);

        assertNotNull(retrieved);
        assertEquals(created.getEvalId(), retrieved.getEvalId());
    }

    // ==================== listResults ====================

    @Test
    void shouldListResults_withFilters() {
        EvalIndicatorSet set = createPublishedSet("SET-LIST");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();
        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 1.0, 100.0, "score >= 60");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-LIST"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-LIST")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 85);

        service.evaluate("SET-LIST", "model-1", "Test", inputData, orgContext);

        List<EvalResult> results = service.listResults("SET-LIST", "MODEL", orgContext);
        assertEquals(1, results.size());
    }

    @Test
    void shouldListResults_withNoMatch() {
        EvalIndicatorSet set = createPublishedSet("SET-LIST2");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();
        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 1.0, 100.0, "score >= 60");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-LIST2"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-LIST2")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 85);

        service.evaluate("SET-LIST2", "model-1", "Test", inputData, orgContext);

        // Filter by different set code
        List<EvalResult> results = service.listResults("OTHER-SET", null, orgContext);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldFilterBySubjectType() {
        EvalIndicatorSet set = createPublishedSet("SET-LIST3");
        set.setSubjectType("MODEL");
        List<EvalIndicator> indicators = new ArrayList<EvalIndicator>();
        EvalIndicator ind1 = createIndicator("IND-001", "Accuracy", "SCORE", 1.0, 100.0, "score >= 60");
        indicators.add(ind1);

        when(evalService.getSet(eq("SET-LIST3"), any(OrganizationContext.class))).thenReturn(set);
        when(evalService.listIndicatorsBySet("SET-LIST3")).thenReturn(indicators);

        Map<String, Object> inputData = new HashMap<String, Object>();
        inputData.put("IND-001", 85);

        service.evaluate("SET-LIST3", "model-1", "Test", inputData, orgContext);

        // Match subject type
        List<EvalResult> matching = service.listResults(null, "MODEL", orgContext);
        assertEquals(1, matching.size());

        // Non-matching subject type
        List<EvalResult> nonMatching = service.listResults(null, "PROMPT", orgContext);
        assertTrue(nonMatching.isEmpty());
    }

    // ==================== Helper methods ====================

    private EvalIndicatorSet createPublishedSet(String setCode) {
        EvalIndicatorSet set = new EvalIndicatorSet();
        set.setSetCode(setCode);
        set.setStatus("PUBLISHED");
        set.setSubjectType("MODEL");
        set.setTenantId("tenant1");
        return set;
    }

    private EvalIndicator createIndicator(String code, String name, String type,
                                           double weight, double maxValue, String threshold) {
        EvalIndicator ind = new EvalIndicator();
        ind.setIndicatorCode(code);
        ind.setIndicatorName(name);
        ind.setIndicatorType(type);
        ind.setWeight(weight);
        ind.setMaxValue(maxValue);
        ind.setThresholdExpression(threshold);
        ind.setUnit("%");
        return ind;
    }
}
