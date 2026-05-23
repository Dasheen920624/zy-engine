package com.medkernel.quality;

import com.medkernel.organization.OrganizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvalScoringService 单元测试")
class EvalScoringServiceTest {

    @Mock
    private EvalService evalService;

    private EvalScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new EvalScoringService(evalService);
    }

    // ==================== 辅助方法 ====================

    private OrganizationContext buildDefaultOrgContext() {
        OrganizationContext ctx = new OrganizationContext();
        ctx.setTenantId("tenant-001");
        ctx.setHospitalCode("ZYHOSPITAL");
        ctx.setEffectiveScopeLevel("HOSPITAL");
        ctx.setEffectiveScopeCode("ZYHOSPITAL");
        ctx.setSource("DEFAULT");
        return ctx;
    }

    private EvalIndicatorSet buildPublishedSet(String setCode, String subjectType) {
        EvalIndicatorSet set = new EvalIndicatorSet();
        set.setTenantId("tenant-001");
        set.setSetCode(setCode);
        set.setSetName("测试指标集");
        set.setSubjectType(subjectType);
        set.setStatus("PUBLISHED");
        set.setVersion("1.0.0");
        return set;
    }

    private EvalIndicator buildIndicator(String code, String name, String type,
                                          double weight, double maxValue, String unit,
                                          String thresholdExpression, String riskLevelMapping) {
        EvalIndicator ind = new EvalIndicator();
        ind.setIndicatorCode(code);
        ind.setIndicatorName(name);
        ind.setIndicatorType(type);
        ind.setWeight(weight);
        ind.setMaxValue(maxValue);
        ind.setUnit(unit);
        ind.setThresholdExpression(thresholdExpression);
        ind.setRiskLevelMapping(riskLevelMapping);
        return ind;
    }

    // ==================== evaluate 评估评分 ====================

    @Nested
    @DisplayName("evaluate 评估评分")
    class EvaluateTests {

        @Test
        @DisplayName("指标集不存在时抛出异常")
        void evaluateSetNotFound() {
            when(evalService.getSet(eq("NOT_EXIST"), any())).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> scoringService.evaluate("NOT_EXIST", "S001", "测试对象",
                            new HashMap<>(), buildDefaultOrgContext()));
            assertTrue(ex.getMessage().contains("Indicator set not found"));
        }

        @Test
        @DisplayName("指标集未发布时抛出异常")
        void evaluateSetNotPublished() {
            EvalIndicatorSet set = buildPublishedSet("SET-001", "EMR");
            set.setStatus("DRAFT");
            when(evalService.getSet(eq("SET-001"), any())).thenReturn(set);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> scoringService.evaluate("SET-001", "S001", "测试对象",
                            new HashMap<>(), buildDefaultOrgContext()));
            assertTrue(ex.getMessage().contains("PUBLISHED"));
        }

        @Test
        @DisplayName("指标集无指标时抛出异常")
        void evaluateNoIndicators() {
            EvalIndicatorSet set = buildPublishedSet("SET-001", "EMR");
            when(evalService.getSet(eq("SET-001"), any())).thenReturn(set);
            when(evalService.listIndicatorsBySet("SET-001")).thenReturn(Collections.emptyList());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> scoringService.evaluate("SET-001", "S001", "测试对象",
                            new HashMap<>(), buildDefaultOrgContext()));
            assertTrue(ex.getMessage().contains("No indicators"));
        }

        @Test
        @DisplayName("正常评估 - 所有指标达标")
        void evaluateAllIndicatorsPass() {
            EvalIndicatorSet set = buildPublishedSet("SET-001", "EMR");
            when(evalService.getSet(eq("SET-001"), any())).thenReturn(set);

            List<EvalIndicator> indicators = new ArrayList<>();
            indicators.add(buildIndicator("IND-001", "诊断准确率", "RATE", 0.4, 100, "%",
                    "score >= 60", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\",\"LOW\":\"score<90\"}"));
            indicators.add(buildIndicator("IND-002", "治疗及时率", "RATE", 0.6, 100, "%",
                    "score >= 70", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\",\"LOW\":\"score<90\"}"));
            when(evalService.listIndicatorsBySet("SET-001")).thenReturn(indicators);

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("IND-001", 85);
            inputData.put("IND-002", 92);

            EvalResult result = scoringService.evaluate("SET-001", "S001", "测试对象",
                    inputData, buildDefaultOrgContext());

            assertNotNull(result);
            assertNotNull(result.getEvalId());
            assertEquals("tenant-001", result.getTenantId());
            assertEquals("SET-001", result.getSetCode());
            assertEquals("EMR", result.getSubjectType());
            assertEquals("S001", result.getSubjectId());
            assertEquals("测试对象", result.getSubjectName());

            // 加权总分: 85*0.4 + 92*0.6 = 34 + 55.2 = 89.2
            assertEquals(89.2, result.getTotalScore(), 0.01);
            // 最大加权总分: 100*0.4 + 100*0.6 = 100
            assertEquals(100.0, result.getMaxPossibleScore(), 0.01);

            // 两个指标都达标
            assertEquals(2, result.getIndicatorScores().size());
            assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
            assertTrue(result.getIndicatorScores().get(1).isThresholdMet());

            // 无异常事实
            assertTrue(result.getAbnormalFacts().isEmpty());
            assertTrue(result.getMissingFacts().isEmpty());

            // 整体风险等级: 百分比 89.2% → LOW
            assertEquals("LOW", result.getRiskLevel());
        }

        @Test
        @DisplayName("正常评估 - 部分指标未达标产生异常事实")
        void evaluateSomeIndicatorsFail() {
            EvalIndicatorSet set = buildPublishedSet("SET-001", "EMR");
            when(evalService.getSet(eq("SET-001"), any())).thenReturn(set);

            List<EvalIndicator> indicators = new ArrayList<>();
            indicators.add(buildIndicator("IND-001", "诊断准确率", "RATE", 0.5, 100, "%",
                    "score >= 60", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\",\"LOW\":\"score<90\"}"));
            indicators.add(buildIndicator("IND-002", "治疗及时率", "RATE", 0.5, 100, "%",
                    "score >= 70", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\",\"LOW\":\"score<90\"}"));
            when(evalService.listIndicatorsBySet("SET-001")).thenReturn(indicators);

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("IND-001", 50);  // 未达阈值 60
            inputData.put("IND-002", 95);

            EvalResult result = scoringService.evaluate("SET-001", "S001", "测试对象",
                    inputData, buildDefaultOrgContext());

            // IND-001 未达标
            assertFalse(result.getIndicatorScores().get(0).isThresholdMet());
            assertTrue(result.getIndicatorScores().get(1).isThresholdMet());

            // 1 条异常事实
            assertEquals(1, result.getAbnormalFacts().size());
            assertEquals("ABNORMAL", result.getAbnormalFacts().get(0).getFactType());
            assertEquals("IND-001", result.getAbnormalFacts().get(0).getIndicatorCode());

            // IND-001 得分 50 → HIGH (score<60)
            assertEquals("HIGH", result.getIndicatorScores().get(0).getRiskLevel());

            // 有 HIGH 异常事实 → 整体 HIGH
            assertEquals("HIGH", result.getRiskLevel());
        }

        @Test
        @DisplayName("输入数据缺失时产生缺失事实")
        void evaluateMissingInputData() {
            EvalIndicatorSet set = buildPublishedSet("SET-001", "EMR");
            when(evalService.getSet(eq("SET-001"), any())).thenReturn(set);

            List<EvalIndicator> indicators = new ArrayList<>();
            indicators.add(buildIndicator("IND-001", "诊断准确率", "RATE", 0.5, 100, "%",
                    "score >= 60", null));
            indicators.add(buildIndicator("IND-002", "治疗及时率", "RATE", 0.5, 100, "%",
                    "score >= 70", null));
            when(evalService.listIndicatorsBySet("SET-001")).thenReturn(indicators);

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("IND-001", 80);
            // IND-002 缺失

            EvalResult result = scoringService.evaluate("SET-001", "S001", "测试对象",
                    inputData, buildDefaultOrgContext());

            // IND-002 缺失 → rawScore=0, weightedScore=0, thresholdMet=false, riskLevel=HIGH
            EvalResult.IndicatorScore missingScore = result.getIndicatorScores().get(1);
            assertEquals(0, missingScore.getRawScore(), 0.01);
            assertEquals(0, missingScore.getWeightedScore(), 0.01);
            assertFalse(missingScore.isThresholdMet());
            assertEquals("HIGH", missingScore.getRiskLevel());
            assertEquals("输入数据缺失", missingScore.getExplanation());

            // 1 条缺失事实
            assertEquals(1, result.getMissingFacts().size());
            assertEquals("MISSING", result.getMissingFacts().get(0).getFactType());
            assertEquals("IND-002", result.getMissingFacts().get(0).getIndicatorCode());
            assertEquals("HIGH", result.getMissingFacts().get(0).getSeverity());

            // 有 HIGH 异常事实（缺失指标 thresholdMet=false 也会产生异常事实）
            // 但缺失不产生异常事实，只产生缺失事实。异常事实来自未达标的指标
            // IND-001 得分 80，阈值 >=60，达标，无异常事实
            // IND-002 缺失，thresholdMet=false，但代码中缺失走的是 else 分支，不产生异常事实
            // 实际上缺失指标不产生异常事实，只有未达标才产生
            // 但缺失指标的 thresholdMet=false，且缺失指标不会进入异常事实列表
            // 整体风险等级: 百分比 = (80*0.5)/(100*0.5+100*0.5) * 100 = 40/100 * 100 = 40 → HIGH
            assertEquals("HIGH", result.getRiskLevel());
        }
    }

    // ==================== 阈值评估 ====================

    @Nested
    @DisplayName("阈值表达式评估")
    class ThresholdEvaluationTests {

        private EvalIndicatorSet publishedSet;
        private OrganizationContext orgCtx;

        @BeforeEach
        void initSet() {
            publishedSet = buildPublishedSet("SET-TH", "EMR");
            orgCtx = buildDefaultOrgContext();
        }

        @Test
        @DisplayName("阈值表达式 score >= N - 达标")
        void thresholdGreaterThanOrEqualPass() {
            when(evalService.getSet(eq("SET-TH"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-TH1", "准确率", "RATE", 1.0, 100, "%",
                            "score >= 60", null));
            when(evalService.listIndicatorsBySet("SET-TH")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-TH1", 60);
            EvalResult result = scoringService.evaluate("SET-TH", "S001", "测试", input, orgCtx);

            assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
        }

        @Test
        @DisplayName("阈值表达式 score >= N - 未达标")
        void thresholdGreaterThanOrEqualFail() {
            when(evalService.getSet(eq("SET-TH"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-TH1", "准确率", "RATE", 1.0, 100, "%",
                            "score >= 60", null));
            when(evalService.listIndicatorsBySet("SET-TH")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-TH1", 59);
            EvalResult result = scoringService.evaluate("SET-TH", "S001", "测试", input, orgCtx);

            assertFalse(result.getIndicatorScores().get(0).isThresholdMet());
        }

        @Test
        @DisplayName("阈值表达式 score <= N - 达标")
        void thresholdLessThanOrEqualPass() {
            when(evalService.getSet(eq("SET-TH"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-TH2", "误诊率", "RATE", 1.0, 100, "%",
                            "score <= 5", null));
            when(evalService.listIndicatorsBySet("SET-TH")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-TH2", 5);
            EvalResult result = scoringService.evaluate("SET-TH", "S001", "测试", input, orgCtx);

            assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
        }

        @Test
        @DisplayName("阈值表达式 score <= N - 未达标")
        void thresholdLessThanOrEqualFail() {
            when(evalService.getSet(eq("SET-TH"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-TH2", "误诊率", "RATE", 1.0, 100, "%",
                            "score <= 5", null));
            when(evalService.listIndicatorsBySet("SET-TH")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-TH2", 6);
            EvalResult result = scoringService.evaluate("SET-TH", "S001", "测试", input, orgCtx);

            assertFalse(result.getIndicatorScores().get(0).isThresholdMet());
        }

        @Test
        @DisplayName("阈值表达式 score > N - 达标")
        void thresholdGreaterThanPass() {
            when(evalService.getSet(eq("SET-TH"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-TH3", "覆盖率", "RATE", 1.0, 100, "%",
                            "score > 50", null));
            when(evalService.listIndicatorsBySet("SET-TH")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-TH3", 51);
            EvalResult result = scoringService.evaluate("SET-TH", "S001", "测试", input, orgCtx);

            assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
        }

        @Test
        @DisplayName("阈值表达式 score > N - 未达标（边界值）")
        void thresholdGreaterThanFailBoundary() {
            when(evalService.getSet(eq("SET-TH"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-TH3", "覆盖率", "RATE", 1.0, 100, "%",
                            "score > 50", null));
            when(evalService.listIndicatorsBySet("SET-TH")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-TH3", 50);
            EvalResult result = scoringService.evaluate("SET-TH", "S001", "测试", input, orgCtx);

            assertFalse(result.getIndicatorScores().get(0).isThresholdMet());
        }

        @Test
        @DisplayName("阈值表达式 score < N - 达标")
        void thresholdLessThanPass() {
            when(evalService.getSet(eq("SET-TH"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-TH4", "缺陷率", "RATE", 1.0, 100, "%",
                            "score < 10", null));
            when(evalService.listIndicatorsBySet("SET-TH")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-TH4", 9);
            EvalResult result = scoringService.evaluate("SET-TH", "S001", "测试", input, orgCtx);

            assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
        }

        @Test
        @DisplayName("无阈值表达式时默认达标")
        void thresholdNullDefaultsToPass() {
            when(evalService.getSet(eq("SET-TH"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-TH5", "无阈值指标", "SCORE", 1.0, 100, "分",
                            null, null));
            when(evalService.listIndicatorsBySet("SET-TH")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-TH5", 30);
            EvalResult result = scoringService.evaluate("SET-TH", "S001", "测试", input, orgCtx);

            assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
        }

        @Test
        @DisplayName("空阈值表达式时默认达标")
        void thresholdEmptyDefaultsToPass() {
            when(evalService.getSet(eq("SET-TH"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-TH6", "空阈值指标", "SCORE", 1.0, 100, "分",
                            "", null));
            when(evalService.listIndicatorsBySet("SET-TH")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-TH6", 10);
            EvalResult result = scoringService.evaluate("SET-TH", "S001", "测试", input, orgCtx);

            assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
        }
    }

    // ==================== 风险等级映射 ====================

    @Nested
    @DisplayName("风险等级映射")
    class RiskLevelMappingTests {

        private EvalIndicatorSet publishedSet;
        private OrganizationContext orgCtx;

        @BeforeEach
        void initSet() {
            publishedSet = buildPublishedSet("SET-RL", "EMR");
            orgCtx = buildDefaultOrgContext();
        }

        @Test
        @DisplayName("得分低于60映射为HIGH风险")
        void riskLevelHigh() {
            when(evalService.getSet(eq("SET-RL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-RL1", "准确率", "RATE", 1.0, 100, "%",
                            "score >= 60", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\",\"LOW\":\"score<90\"}"));
            when(evalService.listIndicatorsBySet("SET-RL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-RL1", 50);
            EvalResult result = scoringService.evaluate("SET-RL", "S001", "测试", input, orgCtx);

            assertEquals("HIGH", result.getIndicatorScores().get(0).getRiskLevel());
        }

        @Test
        @DisplayName("得分60-79映射为MEDIUM风险")
        void riskLevelMedium() {
            when(evalService.getSet(eq("SET-RL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-RL1", "准确率", "RATE", 1.0, 100, "%",
                            "score >= 60", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\",\"LOW\":\"score<90\"}"));
            when(evalService.listIndicatorsBySet("SET-RL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-RL1", 70);
            EvalResult result = scoringService.evaluate("SET-RL", "S001", "测试", input, orgCtx);

            assertEquals("MEDIUM", result.getIndicatorScores().get(0).getRiskLevel());
        }

        @Test
        @DisplayName("得分80-89映射为LOW风险")
        void riskLevelLow() {
            when(evalService.getSet(eq("SET-RL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-RL1", "准确率", "RATE", 1.0, 100, "%",
                            "score >= 60", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\",\"LOW\":\"score<90\"}"));
            when(evalService.listIndicatorsBySet("SET-RL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-RL1", 85);
            EvalResult result = scoringService.evaluate("SET-RL", "S001", "测试", input, orgCtx);

            assertEquals("LOW", result.getIndicatorScores().get(0).getRiskLevel());
        }

        @Test
        @DisplayName("得分90及以上映射为INFO风险")
        void riskLevelInfo() {
            when(evalService.getSet(eq("SET-RL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-RL1", "准确率", "RATE", 1.0, 100, "%",
                            "score >= 60", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\",\"LOW\":\"score<90\"}"));
            when(evalService.listIndicatorsBySet("SET-RL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-RL1", 95);
            EvalResult result = scoringService.evaluate("SET-RL", "S001", "测试", input, orgCtx);

            assertEquals("INFO", result.getIndicatorScores().get(0).getRiskLevel());
        }

        @Test
        @DisplayName("无风险等级映射时默认为INFO")
        void riskLevelNullDefaultsToInfo() {
            when(evalService.getSet(eq("SET-RL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-RL2", "无映射指标", "SCORE", 1.0, 100, "分",
                            "score >= 60", null));
            when(evalService.listIndicatorsBySet("SET-RL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-RL2", 50);
            EvalResult result = scoringService.evaluate("SET-RL", "S001", "测试", input, orgCtx);

            assertEquals("INFO", result.getIndicatorScores().get(0).getRiskLevel());
        }

        @Test
        @DisplayName("风险等级映射包含CRITICAL级别")
        void riskLevelCritical() {
            when(evalService.getSet(eq("SET-RL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-RL3", "严重指标", "RATE", 1.0, 100, "%",
                            "score >= 30", "{\"CRITICAL\":\"score<30\",\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\"}"));
            when(evalService.listIndicatorsBySet("SET-RL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-RL3", 20);
            EvalResult result = scoringService.evaluate("SET-RL", "S001", "测试", input, orgCtx);

            assertEquals("CRITICAL", result.getIndicatorScores().get(0).getRiskLevel());
        }
    }

    // ==================== 整体风险等级 ====================

    @Nested
    @DisplayName("整体风险等级判定")
    class OverallRiskLevelTests {

        private EvalIndicatorSet publishedSet;
        private OrganizationContext orgCtx;

        @BeforeEach
        void initSet() {
            publishedSet = buildPublishedSet("SET-ORL", "EMR");
            orgCtx = buildDefaultOrgContext();
        }

        @Test
        @DisplayName("有CRITICAL异常事实时整体风险为CRITICAL")
        void overallRiskCriticalFromFacts() {
            when(evalService.getSet(eq("SET-ORL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-CR", "严重指标", "RATE", 1.0, 100, "%",
                            "score >= 30", "{\"CRITICAL\":\"score<30\",\"HIGH\":\"score<60\"}"));
            when(evalService.listIndicatorsBySet("SET-ORL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-CR", 20);
            EvalResult result = scoringService.evaluate("SET-ORL", "S001", "测试", input, orgCtx);

            assertEquals("CRITICAL", result.getRiskLevel());
        }

        @Test
        @DisplayName("有HIGH异常事实时整体风险为HIGH")
        void overallRiskHighFromFacts() {
            when(evalService.getSet(eq("SET-ORL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-HI", "高风险指标", "RATE", 1.0, 100, "%",
                            "score >= 60", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\"}"));
            when(evalService.listIndicatorsBySet("SET-ORL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-HI", 50);
            EvalResult result = scoringService.evaluate("SET-ORL", "S001", "测试", input, orgCtx);

            assertEquals("HIGH", result.getRiskLevel());
        }

        @Test
        @DisplayName("无异常事实且百分比低于60时整体风险为HIGH")
        void overallRiskHighFromPercentage() {
            when(evalService.getSet(eq("SET-ORL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-PCT1", "得分指标", "SCORE", 1.0, 100, "分",
                            null, null));
            when(evalService.listIndicatorsBySet("SET-ORL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-PCT1", 50);
            EvalResult result = scoringService.evaluate("SET-ORL", "S001", "测试", input, orgCtx);

            assertEquals("HIGH", result.getRiskLevel());
        }

        @Test
        @DisplayName("无异常事实且百分比60-74时整体风险为MEDIUM")
        void overallRiskMediumFromPercentage() {
            when(evalService.getSet(eq("SET-ORL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-PCT2", "得分指标", "SCORE", 1.0, 100, "分",
                            null, null));
            when(evalService.listIndicatorsBySet("SET-ORL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-PCT2", 68);
            EvalResult result = scoringService.evaluate("SET-ORL", "S001", "测试", input, orgCtx);

            assertEquals("MEDIUM", result.getRiskLevel());
        }

        @Test
        @DisplayName("无异常事实且百分比75-89时整体风险为LOW")
        void overallRiskLowFromPercentage() {
            when(evalService.getSet(eq("SET-ORL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-PCT3", "得分指标", "SCORE", 1.0, 100, "分",
                            null, null));
            when(evalService.listIndicatorsBySet("SET-ORL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-PCT3", 80);
            EvalResult result = scoringService.evaluate("SET-ORL", "S001", "测试", input, orgCtx);

            assertEquals("LOW", result.getRiskLevel());
        }

        @Test
        @DisplayName("无异常事实且百分比90及以上时整体风险为INFO")
        void overallRiskInfoFromPercentage() {
            when(evalService.getSet(eq("SET-ORL"), any())).thenReturn(publishedSet);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-PCT4", "得分指标", "SCORE", 1.0, 100, "分",
                            null, null));
            when(evalService.listIndicatorsBySet("SET-ORL")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-PCT4", 95);
            EvalResult result = scoringService.evaluate("SET-ORL", "S001", "测试", input, orgCtx);

            assertEquals("INFO", result.getRiskLevel());
        }
    }

    // ==================== 加权评分计算 ====================

    @Nested
    @DisplayName("加权评分计算")
    class WeightedScoreTests {

        @Test
        @DisplayName("多指标加权得分计算正确")
        void weightedScoreCalculation() {
            EvalIndicatorSet set = buildPublishedSet("SET-WS", "EMR");
            when(evalService.getSet(eq("SET-WS"), any())).thenReturn(set);

            List<EvalIndicator> indicators = new ArrayList<>();
            indicators.add(buildIndicator("IND-WS1", "指标A", "RATE", 0.3, 100, "%",
                    "score >= 60", null));
            indicators.add(buildIndicator("IND-WS2", "指标B", "SCORE", 0.7, 50, "分",
                    "score >= 30", null));
            when(evalService.listIndicatorsBySet("SET-WS")).thenReturn(indicators);

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("IND-WS1", 80);
            inputData.put("IND-WS2", 40);

            EvalResult result = scoringService.evaluate("SET-WS", "S001", "测试",
                    inputData, buildDefaultOrgContext());

            // IND-WS1: rawScore=80, weightedScore=80*0.3=24
            assertEquals(80, result.getIndicatorScores().get(0).getRawScore(), 0.01);
            assertEquals(24, result.getIndicatorScores().get(0).getWeightedScore(), 0.01);

            // IND-WS2: rawScore=40, weightedScore=40*0.7=28
            assertEquals(40, result.getIndicatorScores().get(1).getRawScore(), 0.01);
            assertEquals(28, result.getIndicatorScores().get(1).getWeightedScore(), 0.01);

            // totalScore = 24 + 28 = 52
            assertEquals(52, result.getTotalScore(), 0.01);

            // maxPossibleScore = 100*0.3 + 50*0.7 = 30 + 35 = 65
            assertEquals(65, result.getMaxPossibleScore(), 0.01);
        }

        @Test
        @DisplayName("输入值为字符串数字时正确解析")
        void inputAsStringNumber() {
            EvalIndicatorSet set = buildPublishedSet("SET-WS2", "EMR");
            when(evalService.getSet(eq("SET-WS2"), any())).thenReturn(set);

            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-STR", "字符串指标", "SCORE", 1.0, 100, "分",
                            "score >= 60", null));
            when(evalService.listIndicatorsBySet("SET-WS2")).thenReturn(indicators);

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("IND-STR", "75");

            EvalResult result = scoringService.evaluate("SET-WS2", "S001", "测试",
                    inputData, buildDefaultOrgContext());

            assertEquals(75, result.getIndicatorScores().get(0).getRawScore(), 0.01);
            assertTrue(result.getIndicatorScores().get(0).isThresholdMet());
        }
    }

    // ==================== 解释生成 ====================

    @Nested
    @DisplayName("指标解释生成")
    class ExplanationTests {

        @Test
        @DisplayName("达标指标解释包含达标信息")
        void explanationThresholdMet() {
            EvalIndicatorSet set = buildPublishedSet("SET-EXP", "EMR");
            when(evalService.getSet(eq("SET-EXP"), any())).thenReturn(set);

            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-EXP1", "诊断准确率", "RATE", 1.0, 100, "%",
                            "score >= 60", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\",\"LOW\":\"score<90\"}"));
            when(evalService.listIndicatorsBySet("SET-EXP")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-EXP1", 85);
            EvalResult result = scoringService.evaluate("SET-EXP", "S001", "测试",
                    input, buildDefaultOrgContext());

            String explanation = result.getIndicatorScores().get(0).getExplanation();
            assertTrue(explanation.contains("诊断准确率"));
            assertTrue(explanation.contains("85"));
            assertTrue(explanation.contains("达标"));
            assertTrue(explanation.contains("LOW"));
        }

        @Test
        @DisplayName("未达标指标解释包含未达标信息")
        void explanationThresholdNotMet() {
            EvalIndicatorSet set = buildPublishedSet("SET-EXP2", "EMR");
            when(evalService.getSet(eq("SET-EXP2"), any())).thenReturn(set);

            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-EXP2", "治疗及时率", "RATE", 1.0, 100, "%",
                            "score >= 80", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\"}"));
            when(evalService.listIndicatorsBySet("SET-EXP2")).thenReturn(indicators);

            Map<String, Object> input = Collections.singletonMap("IND-EXP2", 70);
            EvalResult result = scoringService.evaluate("SET-EXP2", "S001", "测试",
                    input, buildDefaultOrgContext());

            String explanation = result.getIndicatorScores().get(0).getExplanation();
            assertTrue(explanation.contains("未达标"));
            assertTrue(explanation.contains("MEDIUM"));
        }
    }

    // ==================== getResult 获取评估结果 ====================

    @Nested
    @DisplayName("getResult 获取评估结果")
    class GetResultTests {

        @Test
        @DisplayName("获取已存在的评估结果")
        void getResultExisting() {
            EvalIndicatorSet set = buildPublishedSet("SET-GR", "EMR");
            when(evalService.getSet(eq("SET-GR"), any())).thenReturn(set);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-GR", "指标", "SCORE", 1.0, 100, "分", null, null));
            when(evalService.listIndicatorsBySet("SET-GR")).thenReturn(indicators);

            OrganizationContext orgCtx = buildDefaultOrgContext();
            EvalResult evaluated = scoringService.evaluate("SET-GR", "S001", "测试",
                    Collections.singletonMap("IND-GR", 80), orgCtx);

            EvalResult fetched = scoringService.getResult(evaluated.getEvalId(), orgCtx);

            assertNotNull(fetched);
            assertEquals(evaluated.getEvalId(), fetched.getEvalId());
        }

        @Test
        @DisplayName("评估结果不存在时返回null")
        void getResultNotFound() {
            EvalResult fetched = scoringService.getResult("EVAL-9999", buildDefaultOrgContext());
            assertNull(fetched);
        }

        @Test
        @DisplayName("不同租户无法获取评估结果")
        void getResultDifferentTenant() {
            EvalIndicatorSet set = buildPublishedSet("SET-GR2", "EMR");
            when(evalService.getSet(eq("SET-GR2"), any())).thenReturn(set);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-GR2", "指标", "SCORE", 1.0, 100, "分", null, null));
            when(evalService.listIndicatorsBySet("SET-GR2")).thenReturn(indicators);

            OrganizationContext orgCtx1 = buildDefaultOrgContext();
            EvalResult evaluated = scoringService.evaluate("SET-GR2", "S001", "测试",
                    Collections.singletonMap("IND-GR2", 80), orgCtx1);

            OrganizationContext orgCtx2 = new OrganizationContext();
            orgCtx2.setTenantId("tenant-other");

            EvalResult fetched = scoringService.getResult(evaluated.getEvalId(), orgCtx2);
            assertNull(fetched);
        }
    }

    // ==================== listResults 列出评估结果 ====================

    @Nested
    @DisplayName("listResults 列出评估结果")
    class ListResultsTests {

        @Test
        @DisplayName("按指标集编码过滤结果")
        void listResultsFilterBySetCode() {
            EvalIndicatorSet set1 = buildPublishedSet("SET-LR1", "EMR");
            EvalIndicatorSet set2 = buildPublishedSet("SET-LR2", "DEPARTMENT");
            when(evalService.getSet(eq("SET-LR1"), any())).thenReturn(set1);
            when(evalService.getSet(eq("SET-LR2"), any())).thenReturn(set2);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-LR", "指标", "SCORE", 1.0, 100, "分", null, null));
            when(evalService.listIndicatorsBySet(any())).thenReturn(indicators);

            OrganizationContext orgCtx = buildDefaultOrgContext();
            scoringService.evaluate("SET-LR1", "S001", "测试1",
                    Collections.singletonMap("IND-LR", 80), orgCtx);
            scoringService.evaluate("SET-LR2", "S002", "测试2",
                    Collections.singletonMap("IND-LR", 90), orgCtx);

            List<EvalResult> results = scoringService.listResults("SET-LR1", null, orgCtx);
            assertEquals(1, results.size());
            assertEquals("SET-LR1", results.get(0).getSetCode());
        }

        @Test
        @DisplayName("按评估对象类型过滤结果")
        void listResultsFilterBySubjectType() {
            EvalIndicatorSet set1 = buildPublishedSet("SET-LR3", "EMR");
            EvalIndicatorSet set2 = buildPublishedSet("SET-LR4", "DEPARTMENT");
            when(evalService.getSet(eq("SET-LR3"), any())).thenReturn(set1);
            when(evalService.getSet(eq("SET-LR4"), any())).thenReturn(set2);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-LR2", "指标", "SCORE", 1.0, 100, "分", null, null));
            when(evalService.listIndicatorsBySet(any())).thenReturn(indicators);

            OrganizationContext orgCtx = buildDefaultOrgContext();
            scoringService.evaluate("SET-LR3", "S001", "测试1",
                    Collections.singletonMap("IND-LR2", 80), orgCtx);
            scoringService.evaluate("SET-LR4", "S002", "测试2",
                    Collections.singletonMap("IND-LR2", 90), orgCtx);

            List<EvalResult> results = scoringService.listResults(null, "EMR", orgCtx);
            assertEquals(1, results.size());
            assertEquals("EMR", results.get(0).getSubjectType());
        }

        @Test
        @DisplayName("不同租户结果隔离")
        void listResultsTenantIsolation() {
            EvalIndicatorSet set = buildPublishedSet("SET-LR5", "EMR");
            when(evalService.getSet(eq("SET-LR5"), any())).thenReturn(set);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-LR3", "指标", "SCORE", 1.0, 100, "分", null, null));
            when(evalService.listIndicatorsBySet("SET-LR5")).thenReturn(indicators);

            OrganizationContext orgCtx1 = buildDefaultOrgContext();
            scoringService.evaluate("SET-LR5", "S001", "测试",
                    Collections.singletonMap("IND-LR3", 80), orgCtx1);

            OrganizationContext orgCtx2 = new OrganizationContext();
            orgCtx2.setTenantId("tenant-other");

            List<EvalResult> results = scoringService.listResults(null, null, orgCtx2);
            assertTrue(results.isEmpty());
        }
    }

    // ==================== 评估结果 toView ====================

    @Nested
    @DisplayName("评估结果视图生成")
    class ResultViewTests {

        @Test
        @DisplayName("评估结果 toView 包含完整字段")
        void resultToViewComplete() {
            EvalIndicatorSet set = buildPublishedSet("SET-VW", "EMR");
            when(evalService.getSet(eq("SET-VW"), any())).thenReturn(set);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-VW", "准确率", "RATE", 1.0, 100, "%",
                            "score >= 60", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\"}"));
            when(evalService.listIndicatorsBySet("SET-VW")).thenReturn(indicators);

            Map<String, Object> inputData = Collections.singletonMap("IND-VW", 70);
            EvalResult result = scoringService.evaluate("SET-VW", "S001", "测试",
                    inputData, buildDefaultOrgContext());

            Map<String, Object> view = result.toView();

            assertNotNull(view.get("eval_id"));
            assertEquals("tenant-001", view.get("tenant_id"));
            assertEquals("SET-VW", view.get("set_code"));
            assertEquals("EMR", view.get("subject_type"));
            assertEquals("S001", view.get("subject_id"));
            assertNotNull(view.get("total_score"));
            assertNotNull(view.get("max_possible_score"));
            assertNotNull(view.get("score_percentage"));
            assertNotNull(view.get("risk_level"));
            assertNotNull(view.get("indicator_scores"));
            assertNotNull(view.get("evaluated_by"));
            assertNotNull(view.get("evaluated_at"));
            assertNotNull(view.get("org_context"));
        }

        @Test
        @DisplayName("评估结果 toView 百分比计算正确")
        void resultToViewPercentage() {
            EvalIndicatorSet set = buildPublishedSet("SET-VW2", "EMR");
            when(evalService.getSet(eq("SET-VW2"), any())).thenReturn(set);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-VW2", "得分指标", "SCORE", 1.0, 100, "分", null, null));
            when(evalService.listIndicatorsBySet("SET-VW2")).thenReturn(indicators);

            Map<String, Object> inputData = Collections.singletonMap("IND-VW2", 85);
            EvalResult result = scoringService.evaluate("SET-VW2", "S001", "测试",
                    inputData, buildDefaultOrgContext());

            Map<String, Object> view = result.toView();
            // 85/100 * 100 = 85.0%
            assertEquals(85.0, (Double) view.get("score_percentage"), 0.1);
        }
    }

    // ==================== 组织上下文 ====================

    @Nested
    @DisplayName("组织上下文处理")
    class OrgContextTests {

        @Test
        @DisplayName("评估结果包含组织上下文信息")
        void resultContainsOrgContext() {
            EvalIndicatorSet set = buildPublishedSet("SET-OC", "EMR");
            when(evalService.getSet(eq("SET-OC"), any())).thenReturn(set);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-OC", "指标", "SCORE", 1.0, 100, "分", null, null));
            when(evalService.listIndicatorsBySet("SET-OC")).thenReturn(indicators);

            OrganizationContext orgCtx = buildDefaultOrgContext();
            EvalResult result = scoringService.evaluate("SET-OC", "S001", "测试",
                    Collections.singletonMap("IND-OC", 80), orgCtx);

            Map<String, Object> orgContextView = result.getOrgContext();
            assertNotNull(orgContextView);
            assertEquals("ZYHOSPITAL", orgContextView.get("hospital_code"));
            assertEquals("HOSPITAL", orgContextView.get("scope_level"));
            assertEquals("ZYHOSPITAL", orgContextView.get("scope_code"));
            assertEquals("DEFAULT", orgContextView.get("org_source"));
        }
    }

    // ==================== EvalResult.IndicatorScore toView ====================

    @Nested
    @DisplayName("指标得分视图")
    class IndicatorScoreViewTests {

        @Test
        @DisplayName("IndicatorScore toView 包含完整字段")
        void indicatorScoreToView() {
            EvalIndicatorSet set = buildPublishedSet("SET-ISV", "EMR");
            when(evalService.getSet(eq("SET-ISV"), any())).thenReturn(set);
            List<EvalIndicator> indicators = Collections.singletonList(
                    buildIndicator("IND-ISV", "准确率", "RATE", 0.5, 100, "%",
                            "score >= 60", "{\"HIGH\":\"score<60\",\"MEDIUM\":\"score<80\"}"));
            when(evalService.listIndicatorsBySet("SET-ISV")).thenReturn(indicators);

            Map<String, Object> inputData = Collections.singletonMap("IND-ISV", 70);
            EvalResult result = scoringService.evaluate("SET-ISV", "S001", "测试",
                    inputData, buildDefaultOrgContext());

            EvalResult.IndicatorScore score = result.getIndicatorScores().get(0);
            Map<String, Object> view = score.toView();

            assertEquals("IND-ISV", view.get("indicator_code"));
            assertEquals("准确率", view.get("indicator_name"));
            assertEquals("RATE", view.get("indicator_type"));
            assertEquals(0.5, view.get("weight"));
            assertEquals(70.0, view.get("raw_score"));
            assertEquals(35.0, view.get("weighted_score"));
            assertEquals(100.0, view.get("max_value"));
            assertEquals("MEDIUM", view.get("risk_level"));
            assertEquals("%", view.get("unit"));
            assertEquals(true, view.get("threshold_met"));
            assertNotNull(view.get("explanation"));
        }
    }
}
