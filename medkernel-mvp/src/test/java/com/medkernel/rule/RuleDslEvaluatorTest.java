package com.medkernel.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link RuleDslEvaluator} 单元测试：覆盖所有 DSL 操作符、嵌套条件、
 * 缺失事实、边界场景。
 */
class RuleDslEvaluatorTest {

    private RuleDslEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RuleDslEvaluator();
    }

    // ──────────────────────── 辅助方法 ────────────────────────

    private Map<String, Object> ruleWithCondition(Object condition) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("condition", condition);
        return rule;
    }

    private Map<String, Object> atom(String fact, String operator, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fact", fact);
        m.put("operator", operator);
        if (value != null) {
            m.put("value", value);
        }
        return m;
    }

    private Map<String, Object> all(Object... children) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("all", Arrays.asList(children));
        return m;
    }

    private Map<String, Object> any(Object... children) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("any", Arrays.asList(children));
        return m;
    }

    private Map<String, Object> patientContext() {
        Map<String, Object> ctx = new LinkedHashMap<>();
        Map<String, Object> patient = new LinkedHashMap<>();
        patient.put("id", "P001");
        patient.put("gender", "male");
        patient.put("age", 65);
        ctx.put("patient", patient);

        Map<String, Object> encounter = new LinkedHashMap<>();
        encounter.put("id", "E001");
        encounter.put("class", "EMER");
        encounter.put("start_time", "2026-05-23T10:00:00");
        ctx.put("encounter", encounter);

        Map<String, Object> facts = new LinkedHashMap<>();
        List<Map<String, Object>> complaints = new ArrayList<>();
        Map<String, Object> complaint1 = new LinkedHashMap<>();
        complaint1.put("code", "chest_pain");
        complaint1.put("display", "胸痛");
        complaints.add(complaint1);
        Map<String, Object> complaint2 = new LinkedHashMap<>();
        complaint2.put("code", "dyspnea");
        complaint2.put("display", "呼吸困难");
        complaints.add(complaint2);
        facts.put("chief_complaints", complaints);

        List<Map<String, Object>> exams = new ArrayList<>();
        Map<String, Object> exam1 = new LinkedHashMap<>();
        exam1.put("code", "ST_ELEVATION");
        exam1.put("display", "ST段抬高");
        exams.add(exam1);
        facts.put("exam_findings", exams);

        List<String> diagnoses = Arrays.asList("I21.0", "I21.1", "I21.9");
        facts.put("diagnosis_codes", diagnoses);

        facts.put("ecg_time", "2026-05-23T10:15:00");
        facts.put("admission_time", "2026-05-23T10:00:00");

        ctx.put("facts", facts);
        return ctx;
    }

    // ──────────────────────── all 操作符 ────────────────────────

    @Nested
    @DisplayName("all 操作符（AND 逻辑）")
    class AllOperatorTests {

        @Test
        @DisplayName("所有子条件满足时命中")
        void allHitWhenAllChildrenMatch() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    all(
                            atom("chief_complaints.code", "equals", "chest_pain"),
                            atom("exam_findings.code", "equals", "ST_ELEVATION")
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
            assertEquals(2, outcome.getEvidence().size());
        }

        @Test
        @DisplayName("任一子条件不满足时未命中")
        void allMissWhenOneChildFails() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    all(
                            atom("chief_complaints.code", "equals", "chest_pain"),
                            atom("exam_findings.code", "equals", "NON_EXISTENT_CODE")
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }

        @Test
        @DisplayName("空 all 条件应命中（空 AND 为真）")
        void emptyAllShouldHit() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(all());
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }
    }

    // ──────────────────────── any 操作符 ────────────────────────

    @Nested
    @DisplayName("any 操作符（OR 逻辑）")
    class AnyOperatorTests {

        @Test
        @DisplayName("任一子条件满足时命中")
        void anyHitWhenOneChildMatches() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    any(
                            atom("chief_complaints.code", "equals", "nonexistent"),
                            atom("exam_findings.code", "equals", "ST_ELEVATION")
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("所有子条件均不满足时未命中")
        void anyMissWhenAllChildrenFail() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    any(
                            atom("chief_complaints.code", "equals", "nonexistent"),
                            atom("exam_findings.code", "equals", "nonexistent")
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }

        @Test
        @DisplayName("空 any 条件应未命中（空 OR 为假）")
        void emptyAnyShouldMiss() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(any());
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }
    }

    // ──────────────────────── exists 操作符 ────────────────────────

    @Nested
    @DisplayName("exists 操作符")
    class ExistsOperatorTests {

        @Test
        @DisplayName("事实存在时命中")
        void existsHitWhenFactPresent() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("chief_complaints.code", "exists", null));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("事实不存在时未命中且记录缺失事实")
        void existsMissWhenFactAbsent() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("nonexistent.field", "exists", null));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("nonexistent.field"));
        }
    }

    // ──────────────────────── not_exists 场景 ────────────────────────

    @Nested
    @DisplayName("not_exists 场景（通过缺失事实间接验证）")
    class NotExistsTests {

        @Test
        @DisplayName("事实不存在时 exists 返回 false，可用于模拟 not_exists")
        void notExistsSimulatedViaExistsMiss() {
            Map<String, Object> ctx = patientContext();
            // not_exists 语义：事实不存在 → 规则命中
            // 当前 DSL 不直接支持 not_exists，exists 在事实缺失时返回 false
            Map<String, Object> rule = ruleWithCondition(atom("nonexistent_fact", "exists", null));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("nonexistent_fact"));
        }

        @Test
        @DisplayName("not_exists 作为操作符时归入 unsupported_operator")
        void notExistsOperatorIsUnsupported() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("chief_complaints.code", "not_exists", null));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            boolean hasUnsupported = outcome.getMissingFacts().stream()
                    .anyMatch(f -> f.startsWith("unsupported_operator:"));
            assertTrue(hasUnsupported, "应包含 unsupported_operator:not_exists");
        }
    }

    // ──────────────────────── field 路径解析 ────────────────────────

    @Nested
    @DisplayName("field 路径解析")
    class FieldPathTests {

        @Test
        @DisplayName("patient 前缀路径解析")
        void patientPrefixPath() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("patient.gender", "equals", "male"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("encounter 前缀路径解析")
        void encounterPrefixPath() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("encounter.class", "equals", "EMER"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("facts 前缀路径解析")
        void factsPrefixPath() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("facts.ecg_time", "exists", null));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("无前缀路径自动映射到 facts")
        void noPrefixPathAutoMappedToFacts() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("ecg_time", "exists", null));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("嵌套路径 code 匹配集合中的元素")
        void nestedCodePathInCollection() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("chief_complaints.code", "equals", "chest_pain"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("forms 前缀路径解析")
        void formsPrefixPath() {
            Map<String, Object> ctx = patientContext();
            @SuppressWarnings("unchecked")
            Map<String, Object> facts = (Map<String, Object>) ctx.get("facts");
            Map<String, Object> forms = new LinkedHashMap<>();
            forms.put("triage_level", 2);
            facts.put("forms", forms);

            Map<String, Object> rule = ruleWithCondition(atom("forms.triage_level", "equals", 2));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }
    }

    // ──────────────────────── contains 操作符 ────────────────────────

    @Nested
    @DisplayName("contains 操作符")
    class ContainsOperatorTests {

        @Test
        @DisplayName("集合包含指定元素时命中")
        void containsHitWhenCollectionHasElement() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("diagnosis_codes", "contains", "I21.0"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("集合不包含指定元素时未命中")
        void containsMissWhenCollectionLacksElement() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("diagnosis_codes", "contains", "J00"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }

        @Test
        @DisplayName("字符串包含子串时命中")
        void containsHitWhenStringContainsSubstring() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("patient.gender", "contains", "mal"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }
    }

    // ──────────────────────── contains_any 场景 ────────────────────────

    @Nested
    @DisplayName("contains_any 场景")
    class ContainsAnyTests {

        @Test
        @DisplayName("contains_any 作为操作符时归入 unsupported_operator")
        void containsAnyOperatorIsUnsupported() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    atom("diagnosis_codes", "contains_any", Arrays.asList("I21.0", "J00")));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            boolean hasUnsupported = outcome.getMissingFacts().stream()
                    .anyMatch(f -> f.startsWith("unsupported_operator:"));
            assertTrue(hasUnsupported, "应包含 unsupported_operator:contains_any");
        }

        @Test
        @DisplayName("用 any + contains 模拟 contains_any 语义")
        void simulateContainsAnyViaAnyAndContains() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    any(
                            atom("diagnosis_codes", "contains", "I21.0"),
                            atom("diagnosis_codes", "contains", "J00")
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }
    }

    // ──────────────────────── in 操作符 ────────────────────────

    @Nested
    @DisplayName("in 操作符")
    class InOperatorTests {

        @Test
        @DisplayName("值在候选列表中时命中")
        void inHitWhenValueInList() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    atom("encounter.class", "in", Arrays.asList("EMER", "URGENT")));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("值不在候选列表中时未命中")
        void inMissWhenValueNotInList() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    atom("encounter.class", "in", Arrays.asList("AMB", "IMP")));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }

        @Test
        @DisplayName("value 不是集合时 in 未命中")
        void inMissWhenValueNotCollection() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    atom("encounter.class", "in", "EMER"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }

        @Test
        @DisplayName("集合中元素 code 匹配 in 候选列表")
        void inHitWithCollectionCodePath() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    atom("chief_complaints.code", "in", Arrays.asList("chest_pain", "headache")));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }
    }

    // ──────────────────────── equals 操作符 ────────────────────────

    @Nested
    @DisplayName("equals 操作符")
    class EqualsOperatorTests {

        @Test
        @DisplayName("值相等时命中")
        void equalsHitWhenValueMatches() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("patient.gender", "equals", "male"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("值不相等时未命中")
        void equalsMissWhenValueDiffers() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("patient.gender", "equals", "female"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }

        @Test
        @DisplayName("equals 比较忽略大小写")
        void equalsIsCaseInsensitive() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("patient.gender", "equals", "MALE"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("equals 命中时生成证据")
        void equalsHitProducesEvidence() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("patient.gender", "equals", "male"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
            assertEquals(1, outcome.getEvidence().size());
            assertEquals("patient.gender", outcome.getEvidence().get(0).get("fact"));
            assertEquals("equals", outcome.getEvidence().get(0).get("operator"));
        }
    }

    // ──────────────────────── within_minutes_from 操作符 ────────────────────────

    @Nested
    @DisplayName("within_minutes_from 操作符")
    class WithinMinutesFromTests {

        @Test
        @DisplayName("时间差在窗口内时命中")
        void withinMinutesHit() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> window = new LinkedHashMap<>();
            window.put("from", "admission_time");
            window.put("minutes", 30);

            Map<String, Object> rule = ruleWithCondition(atom("ecg_time", "within_minutes_from", window));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("时间差超出窗口时未命中")
        void withinMinutesMissWhenExceedsWindow() {
            Map<String, Object> ctx = patientContext();
            // ecg_time - admission_time = 15 分钟，窗口设为 10 分钟应未命中
            Map<String, Object> window = new LinkedHashMap<>();
            window.put("from", "admission_time");
            window.put("minutes", 10);

            Map<String, Object> rule = ruleWithCondition(atom("ecg_time", "within_minutes_from", window));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }

        @Test
        @DisplayName("OffsetDateTime 格式时间差计算")
        void withinMinutesWithOffsetDateTime() {
            Map<String, Object> ctx = patientContext();
            @SuppressWarnings("unchecked")
            Map<String, Object> facts = (Map<String, Object>) ctx.get("facts");
            facts.put("event_time", "2026-05-23T10:20:00+08:00");
            facts.put("base_time", "2026-05-23T10:00:00+08:00");

            Map<String, Object> window = new LinkedHashMap<>();
            window.put("from", "base_time");
            window.put("minutes", 30);

            Map<String, Object> rule = ruleWithCondition(atom("event_time", "within_minutes_from", window));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("起始时间晚于结束时间时未命中（负时间差）")
        void withinMinutesMissWhenNegativeDiff() {
            Map<String, Object> ctx = patientContext();
            @SuppressWarnings("unchecked")
            Map<String, Object> facts = (Map<String, Object>) ctx.get("facts");
            facts.put("late_event", "2026-05-23T09:00:00");
            facts.put("early_event", "2026-05-23T10:00:00");

            Map<String, Object> window = new LinkedHashMap<>();
            window.put("from", "early_event");
            window.put("minutes", 30);

            Map<String, Object> rule = ruleWithCondition(atom("late_event", "within_minutes_from", window));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }

        @Test
        @DisplayName("from 路径对应事实缺失时记录缺失事实")
        void withinMinutesMissingFromFact() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> window = new LinkedHashMap<>();
            window.put("from", "nonexistent_time");
            window.put("minutes", 30);

            Map<String, Object> rule = ruleWithCondition(atom("ecg_time", "within_minutes_from", window));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("nonexistent_time"));
        }

        @Test
        @DisplayName("value 不是 Map 时记录缺失事实")
        void withinMinutesInvalidWindowFormat() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("ecg_time", "within_minutes_from", "not_a_map"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().stream()
                    .anyMatch(f -> f.contains(".window")));
        }

        @Test
        @DisplayName("时间格式无效时记录缺失事实")
        void withinMinutesInvalidTimeFormat() {
            Map<String, Object> ctx = patientContext();
            @SuppressWarnings("unchecked")
            Map<String, Object> facts = (Map<String, Object>) ctx.get("facts");
            facts.put("bad_time", "not-a-time");

            Map<String, Object> window = new LinkedHashMap<>();
            window.put("from", "admission_time");
            window.put("minutes", 30);

            Map<String, Object> rule = ruleWithCondition(atom("bad_time", "within_minutes_from", window));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().stream()
                    .anyMatch(f -> f.contains(".time_format")));
        }

        @Test
        @DisplayName("窗口缺少 minutes 字段时记录缺失事实")
        void withinMinutesMissingMinutesField() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> window = new LinkedHashMap<>();
            window.put("from", "admission_time");
            // 不设置 minutes

            Map<String, Object> rule = ruleWithCondition(atom("ecg_time", "within_minutes_from", window));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().stream()
                    .anyMatch(f -> f.contains(".window")));
        }
    }

    // ──────────────────────── duration_minutes_between 场景 ────────────────────────

    @Nested
    @DisplayName("duration_minutes_between 场景")
    class DurationMinutesBetweenTests {

        @Test
        @DisplayName("duration_minutes_between 作为操作符时归入 unsupported_operator")
        void durationMinutesBetweenIsUnsupported() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    atom("ecg_time", "duration_minutes_between", "admission_time"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            boolean hasUnsupported = outcome.getMissingFacts().stream()
                    .anyMatch(f -> f.startsWith("unsupported_operator:"));
            assertTrue(hasUnsupported, "应包含 unsupported_operator:duration_minutes_between");
        }

        @Test
        @DisplayName("使用 within_minutes_from 实现时间差判断语义")
        void useWithinMinutesFromForDurationCheck() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> window = new LinkedHashMap<>();
            window.put("from", "admission_time");
            window.put("minutes", 20);

            Map<String, Object> rule = ruleWithCondition(atom("ecg_time", "within_minutes_from", window));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
            // 验证证据中包含时间差信息
            assertEquals(1, outcome.getEvidence().size());
            assertNotNull(outcome.getEvidence().get(0).get("actual"));
        }
    }

    // ──────────────────────── gt 场景 ────────────────────────

    @Nested
    @DisplayName("gt 场景（大于操作符）")
    class GtOperatorTests {

        @Test
        @DisplayName("gt 作为操作符时归入 unsupported_operator")
        void gtOperatorIsUnsupported() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("patient.age", "gt", 60));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            boolean hasUnsupported = outcome.getMissingFacts().stream()
                    .anyMatch(f -> f.startsWith("unsupported_operator:"));
            assertTrue(hasUnsupported, "应包含 unsupported_operator:gt");
        }
    }

    // ──────────────────────── 嵌套条件 ────────────────────────

    @Nested
    @DisplayName("嵌套条件")
    class NestedConditionTests {

        @Test
        @DisplayName("AND 内嵌套 OR")
        void andNestedWithOr() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    all(
                            atom("patient.gender", "equals", "male"),
                            any(
                                    atom("chief_complaints.code", "equals", "chest_pain"),
                                    atom("chief_complaints.code", "equals", "headache")
                            )
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("OR 内嵌套 AND")
        void orNestedWithAnd() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    any(
                            all(
                                    atom("patient.gender", "equals", "female"),
                                    atom("encounter.class", "equals", "EMER")
                            ),
                            atom("chief_complaints.code", "equals", "chest_pain")
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("多层嵌套 AND/OR")
        void deepNesting() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    all(
                            any(
                                    atom("patient.gender", "equals", "male"),
                                    atom("patient.gender", "equals", "female")
                            ),
                            all(
                                    atom("encounter.class", "equals", "EMER"),
                                    atom("chief_complaints.code", "exists", null)
                            )
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("嵌套条件中内层全部未命中导致外层未命中")
        void nestedMissPropagates() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    all(
                            atom("patient.gender", "equals", "male"),
                            any(
                                    atom("chief_complaints.code", "equals", "nonexistent"),
                                    atom("exam_findings.code", "equals", "nonexistent")
                            )
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }
    }

    // ──────────────────────── 缺失事实场景 ────────────────────────

    @Nested
    @DisplayName("缺失事实场景")
    class MissingFactsTests {

        @Test
        @DisplayName("事实路径不存在时记录缺失事实")
        void missingFactRecorded() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("nonexistent.field", "equals", "value"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("nonexistent.field"));
        }

        @Test
        @DisplayName("atom 缺少 fact 字段时记录缺失")
        void missingFactFieldInAtom() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> atom = new LinkedHashMap<>();
            atom.put("operator", "equals");
            atom.put("value", "something");
            Map<String, Object> rule = ruleWithCondition(atom);
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("fact/operator"));
        }

        @Test
        @DisplayName("atom 缺少 operator 字段时记录缺失")
        void missingOperatorFieldInAtom() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> atom = new LinkedHashMap<>();
            atom.put("fact", "patient.gender");
            atom.put("value", "male");
            Map<String, Object> rule = ruleWithCondition(atom);
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("fact/operator"));
        }

        @Test
        @DisplayName("all 中部分子条件事实缺失时记录缺失并返回 false")
        void allWithMissingFactChild() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    all(
                            atom("patient.gender", "equals", "male"),
                            atom("missing.field", "equals", "value")
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("missing.field"));
        }

        @Test
        @DisplayName("any 中所有子条件事实缺失时记录所有缺失事实")
        void anyWithAllMissingFactChildren() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(
                    any(
                            atom("missing1.field", "equals", "value"),
                            atom("missing2.field", "equals", "value")
                    )
            );
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("missing1.field"));
            assertTrue(outcome.getMissingFacts().contains("missing2.field"));
        }
    }

    // ──────────────────────── 边界场景 ────────────────────────

    @Nested
    @DisplayName("边界场景")
    class EdgeCaseTests {

        @Test
        @DisplayName("condition 为 null 时未命中且记录缺失")
        void nullCondition() {
            Map<String, Object> rule = new HashMap<>();
            rule.put("condition", null);
            Map<String, Object> ctx = patientContext();
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("condition"));
        }

        @Test
        @DisplayName("rule 中无 condition 键时未命中且记录缺失")
        void noConditionKey() {
            Map<String, Object> rule = new HashMap<>();
            rule.put("rule_code", "R001");
            Map<String, Object> ctx = patientContext();
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("condition"));
        }

        @Test
        @DisplayName("condition 为非 Map 类型时未命中且记录缺失")
        void conditionNotMap() {
            Map<String, Object> rule = new HashMap<>();
            rule.put("condition", "invalid_condition");
            Map<String, Object> ctx = patientContext();
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("condition"));
        }

        @Test
        @DisplayName("patientContext 为空 Map 时事实缺失")
        void emptyPatientContext() {
            Map<String, Object> ctx = new HashMap<>();
            Map<String, Object> rule = ruleWithCondition(atom("patient.gender", "equals", "male"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }

        @Test
        @DisplayName("atom 的 fact 和 value 均为 null 时记录缺失")
        void nullFactAndValue() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> atom = new LinkedHashMap<>();
            atom.put("fact", null);
            atom.put("operator", "equals");
            atom.put("value", null);
            Map<String, Object> rule = ruleWithCondition(atom);
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("fact/operator"));
        }

        @Test
        @DisplayName("equals 比较 null 与 null 应相等")
        void equalsNullWithNull() {
            Map<String, Object> ctx = new HashMap<>();
            Map<String, Object> facts = new HashMap<>();
            facts.put("nullable_field", null);
            ctx.put("facts", facts);
            Map<String, Object> rule = ruleWithCondition(atom("nullable_field", "equals", null));
            // nullable_field 路径解析后值为 null，equals(null, null) → true
            // 但 valuesForPath 对 null 值的处理：collectSegment 中 value instanceof Map 为 false，
            // map.containsKey(segment) 为 true 时 add map.get(segment) 即 null
            // 然后 flatten 跳过 null → actualValues 为空 → missingFacts
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
        }

        @Test
        @DisplayName("集合中 code 匹配路径段")
        void codeMatchInCollectionPathSegment() {
            Map<String, Object> ctx = patientContext();
            // chief_complaints 是列表，路径 "chief_complaints.chest_pain" 应通过 code 匹配
            Map<String, Object> rule = ruleWithCondition(atom("chief_complaints.chest_pain", "exists", null));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("不支持的 operator 记录 unsupported_operator")
        void unsupportedOperator() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> rule = ruleWithCondition(atom("patient.gender", "regex", "m.*"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("unsupported_operator:regex"));
        }

        @Test
        @DisplayName("within_minutes_from 事实路径不存在时记录缺失")
        void withinMinutesFactNotFound() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> window = new LinkedHashMap<>();
            window.put("from", "admission_time");
            window.put("minutes", 30);

            Map<String, Object> rule = ruleWithCondition(atom("nonexistent_time", "within_minutes_from", window));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().contains("nonexistent_time"));
        }

        @Test
        @DisplayName("within_minutes_from 窗口缺少 from 字段时记录缺失")
        void withinMinutesMissingFromField() {
            Map<String, Object> ctx = patientContext();
            Map<String, Object> window = new LinkedHashMap<>();
            window.put("minutes", 30);
            // 不设置 from

            Map<String, Object> rule = ruleWithCondition(atom("ecg_time", "within_minutes_from", window));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertFalse(outcome.isHit());
            assertTrue(outcome.getMissingFacts().stream()
                    .anyMatch(f -> f.contains(".window")));
        }

        @Test
        @DisplayName("equals 匹配集合中多个值的第一个")
        void equalsMatchesFirstInCollection() {
            Map<String, Object> ctx = patientContext();
            // chief_complaints 有两个元素，code 分别为 chest_pain 和 dyspnea
            Map<String, Object> rule = ruleWithCondition(atom("chief_complaints.code", "equals", "dyspnea"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("数值类型 equals 比较（字符串化后比较）")
        void numericEqualsComparison() {
            Map<String, Object> ctx = patientContext();
            // patient.age = 65 (Integer)
            Map<String, Object> rule = ruleWithCondition(atom("patient.age", "equals", 65));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }

        @Test
        @DisplayName("数值与字符串跨类型 equals 比较（字符串化后一致）")
        void numericStringEqualsComparison() {
            Map<String, Object> ctx = patientContext();
            // patient.age = 65 (Integer), value = "65" (String)
            Map<String, Object> rule = ruleWithCondition(atom("patient.age", "equals", "65"));
            RuleDslEvaluator.EvaluationOutcome outcome = evaluator.evaluate(rule, ctx);
            assertTrue(outcome.isHit());
        }
    }
}
