package com.medkernel.rule;

import com.medkernel.common.exception.MissingSourceException;
import com.medkernel.dto.RuleResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.rule.RuleDslEvaluator.EvaluationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RuleService 单元测试")
class RuleServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private RuleEvalResultRepository ruleEvalResultRepository;

    @Mock
    private RuleExecutionLogService ruleExecutionLogService;

    private RuleService ruleService;

    @BeforeEach
    void setUp() {
        ruleService = new RuleService(persistenceService, ruleEvalResultRepository, ruleExecutionLogService);
    }

    // ──────────────────────── 辅助方法 ────────────────────────

    private Map<String, Object> buildRuleMap(String ruleCode, String ruleName, String ruleType,
                                              String severity, boolean enabled, String referenceDocumentCode) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", ruleCode);
        rule.put("rule_name", ruleName);
        rule.put("rule_type", ruleType);
        rule.put("severity", severity);
        rule.put("enabled", enabled);
        rule.put("version_no", "1.0.0");
        if (referenceDocumentCode != null) {
            rule.put("reference_document_code", referenceDocumentCode);
        }
        // 默认 condition
        Map<String, Object> condition = new LinkedHashMap<>();
        condition.put("fact", "patient.gender");
        condition.put("operator", "equals");
        condition.put("value", "male");
        rule.put("condition", condition);
        return rule;
    }

    private Map<String, Object> buildPatientContext() {
        Map<String, Object> ctx = new LinkedHashMap<>();
        Map<String, Object> patient = new LinkedHashMap<>();
        patient.put("patient_id", "P001");
        patient.put("gender", "male");
        patient.put("age", 65);
        ctx.put("patient", patient);

        Map<String, Object> encounter = new LinkedHashMap<>();
        encounter.put("encounter_id", "E001");
        encounter.put("class", "EMER");
        ctx.put("encounter", encounter);

        Map<String, Object> facts = new LinkedHashMap<>();
        List<Map<String, Object>> complaints = new ArrayList<>();
        Map<String, Object> complaint1 = new LinkedHashMap<>();
        complaint1.put("code", "CHEST_PAIN");
        complaint1.put("text", "胸痛");
        complaints.add(complaint1);
        facts.put("chief_complaints", complaints);

        List<Map<String, Object>> exams = new ArrayList<>();
        Map<String, Object> exam1 = new LinkedHashMap<>();
        exam1.put("finding_codes", Arrays.asList("ST_ELEVATION_CONTIGUOUS_LEADS"));
        exams.add(exam1);
        facts.put("exams", exams);

        facts.put("diagnosis_codes", Arrays.asList("I21.0"));
        ctx.put("facts", facts);
        return ctx;
    }

    private OrganizationContext buildDefaultOrgContext() {
        OrganizationContext ctx = new OrganizationContext();
        ctx.setTenantId("default");
        ctx.setHospitalCode("ZYHOSPITAL");
        ctx.setEffectiveScopeLevel("HOSPITAL");
        ctx.setEffectiveScopeCode("ZYHOSPITAL");
        ctx.setSource("DEFAULT");
        return ctx;
    }

    // ──────────────────────── importRules ────────────────────────

    @Nested
    @DisplayName("importRules 导入规则")
    class ImportRulesTests {

        @Test
        @DisplayName("成功导入规则定义列表")
        void importRulesSuccess() {
            Map<String, Object> rule1 = buildRuleMap("R_001", "测试规则1", "GENERAL", "HIGH", true, "DOC001");
            Map<String, Object> rule2 = buildRuleMap("R_002", "测试规则2", "PATHWAY_ENTRY", "CRITICAL", true, "DOC002");
            List<Map<String, Object>> request = Arrays.asList(rule1, rule2);

            List<RuleDefinition> imported = ruleService.importRules(request);

            assertEquals(2, imported.size());
            assertEquals("R_001", imported.get(0).getRuleCode());
            assertEquals("R_002", imported.get(1).getRuleCode());
            assertEquals("DRAFT", imported.get(0).getStatus());
            assertEquals("DRAFT", imported.get(1).getStatus());
            verify(persistenceService).saveRuleDefinition(eq(imported.get(0)), isNull());
            verify(persistenceService).saveRuleDefinition(eq(imported.get(1)), isNull());
        }

        @Test
        @DisplayName("导入规则使用 Map 包装格式（含 package_code）")
        void importRulesWithPackageCode() {
            Map<String, Object> rule1 = buildRuleMap("R_001", "测试规则1", "GENERAL", "HIGH", true, null);
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("package_code", "PKG_001");
            request.put("package_version", "1.0.0");
            request.put("rules", Collections.singletonList(rule1));

            List<RuleDefinition> imported = ruleService.importRules(request);

            assertEquals(1, imported.size());
            assertEquals("PKG_001", imported.get(0).getPackageCode());
            assertEquals("1.0.0", imported.get(0).getPackageVersion());
        }

        @Test
        @DisplayName("导入规则缺少 rule_code 时抛出异常")
        void importRulesMissingRuleCode() {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("rule_name", "无编码规则");
            List<Map<String, Object>> request = Collections.singletonList(rule);

            assertThrows(IllegalArgumentException.class, () -> ruleService.importRules(request));
        }

        @Test
        @DisplayName("导入规则默认值填充正确")
        void importRulesDefaultValues() {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("rule_code", "R_DEFAULT");
            List<Map<String, Object>> request = Collections.singletonList(rule);

            List<RuleDefinition> imported = ruleService.importRules(request);

            assertEquals(1, imported.size());
            RuleDefinition def = imported.get(0);
            assertEquals("R_DEFAULT", def.getRuleName()); // rule_name 默认取 ruleCode
            assertEquals("GENERAL", def.getRuleType());   // rule_type 默认 GENERAL
            assertEquals("1.0.0", def.getVersionNo());    // version_no 默认 1.0.0
            assertEquals("HIGH", def.getSeverity());      // severity 默认 HIGH
            assertTrue(def.isEnabled());                   // enabled 默认 true
        }
    }

    // ──────────────────────── publish ────────────────────────

    @Nested
    @DisplayName("publish 发布规则")
    class PublishTests {

        @Test
        @DisplayName("成功发布已导入的规则")
        void publishSuccess() {
            Map<String, Object> ruleMap = buildRuleMap("R_PUB_001", "可发布规则", "GENERAL", "HIGH", true, "DOC001");
            ruleService.importRules(Collections.singletonList(ruleMap));

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("version_no", "1.0.0");
            request.put("approved_by", "admin");

            RuleDefinition published = ruleService.publish("R_PUB_001", request);

            assertEquals("PUBLISHED", published.getStatus());
            assertEquals("admin", published.getPublishedBy());
            assertNotNull(published.getPublishedTime());
            verify(persistenceService).saveRuleDefinition(eq(published), eq("admin"));
        }

        @Test
        @DisplayName("发布不存在的规则时抛出异常")
        void publishNonExistentRule() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("version_no", "1.0.0");

            assertThrows(IllegalArgumentException.class,
                    () -> ruleService.publish("R_NOT_EXIST", request));
        }

        @Test
        @DisplayName("发布缺少来源文档绑定的规则时抛出 MissingSourceException")
        void publishWithoutReferenceDocument() {
            Map<String, Object> ruleMap = buildRuleMap("R_NO_REF", "无来源规则", "GENERAL", "HIGH", true, null);
            ruleService.importRules(Collections.singletonList(ruleMap));

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("version_no", "1.0.0");

            assertThrows(MissingSourceException.class,
                    () -> ruleService.publish("R_NO_REF", request));
        }
    }

    // ──────────────────────── listRules ────────────────────────

    @Nested
    @DisplayName("listRules 列出规则")
    class ListRulesTests {

        @Test
        @DisplayName("列出所有已导入规则")
        void listAllRules() {
            Map<String, Object> rule1 = buildRuleMap("R_001", "规则1", "GENERAL", "HIGH", true, null);
            Map<String, Object> rule2 = buildRuleMap("R_002", "规则2", "GENERAL", "HIGH", true, null);
            ruleService.importRules(Arrays.asList(rule1, rule2));

            List<RuleDefinition> rules = ruleService.listRules();

            assertEquals(2, rules.size());
        }

        @Test
        @DisplayName("无规则时返回空列表")
        void listRulesEmpty() {
            List<RuleDefinition> rules = ruleService.listRules();

            assertTrue(rules.isEmpty());
        }

        @Test
        @DisplayName("规则按 ruleCode 排序")
        void listRulesSortedByCode() {
            Map<String, Object> ruleB = buildRuleMap("R_BBB", "规则B", "GENERAL", "HIGH", true, null);
            Map<String, Object> ruleA = buildRuleMap("R_AAA", "规则A", "GENERAL", "HIGH", true, null);
            ruleService.importRules(Arrays.asList(ruleB, ruleA));

            List<RuleDefinition> rules = ruleService.listRules();

            assertEquals("R_AAA", rules.get(0).getRuleCode());
            assertEquals("R_BBB", rules.get(1).getRuleCode());
        }
    }

    // ──────────────────────── getRule ────────────────────────

    @Nested
    @DisplayName("getRule 获取单条规则")
    class GetRuleTests {

        @Test
        @DisplayName("按 ruleCode 和 versionNo 获取规则")
        void getRuleByCodeAndVersion() {
            Map<String, Object> ruleMap = buildRuleMap("R_GET_001", "查询规则", "GENERAL", "HIGH", true, null);
            ruleService.importRules(Collections.singletonList(ruleMap));

            RuleDefinition found = ruleService.getRule("R_GET_001", "1.0.0");

            assertNotNull(found);
            assertEquals("R_GET_001", found.getRuleCode());
        }

        @Test
        @DisplayName("不存在的规则返回 null")
        void getRuleNotFound() {
            RuleDefinition found = ruleService.getRule("R_NOT_EXIST", "1.0.0");

            assertNull(found);
        }

        @Test
        @DisplayName("versionNo 为 null 时返回最新版本")
        void getRuleLatestVersion() {
            Map<String, Object> ruleMap = buildRuleMap("R_LATEST", "最新版本规则", "GENERAL", "HIGH", true, null);
            ruleService.importRules(Collections.singletonList(ruleMap));

            RuleDefinition found = ruleService.getRule("R_LATEST", null);

            assertNotNull(found);
            assertEquals("R_LATEST", found.getRuleCode());
        }
    }

    // ──────────────────────── evaluate ────────────────────────

    @Nested
    @DisplayName("evaluate 评估规则")
    class EvaluateTests {

        @Test
        @DisplayName("无已发布规则时回退到内置 AMI 规则")
        void evaluateWithBuiltInRules() {
            Map<String, Object> request = buildPatientContext();

            List<RuleResult> results = ruleService.evaluate(request);

            assertNotNull(results);
            assertFalse(results.isEmpty());
            // 内置规则至少包含 STEMI 候选和心电图质控
            assertTrue(results.stream().anyMatch(r -> "R_AMI_STEMI_CANDIDATE".equals(r.getRuleCode())));
            assertTrue(results.stream().anyMatch(r -> "R_AMI_ECG_TIMELY".equals(r.getRuleCode())));
        }

        @Test
        @DisplayName("内置 STEMI 规则在胸痛+ST抬高时命中")
        void evaluateBuiltInStemiHit() {
            Map<String, Object> request = buildPatientContext();

            List<RuleResult> results = ruleService.evaluate(request);

            RuleResult stemi = results.stream()
                    .filter(r -> "R_AMI_STEMI_CANDIDATE".equals(r.getRuleCode()))
                    .findFirst().orElse(null);
            assertNotNull(stemi);
            assertTrue(stemi.isHit());
            assertEquals("HIGH", stemi.getSeverity());
            assertFalse(stemi.getActions().isEmpty());
        }

        @Test
        @DisplayName("已发布配置化规则评估命中")
        void evaluatePublishedRuleHit() {
            // 导入并发布一条规则
            Map<String, Object> ruleMap = buildRuleMap("R_EVAL_001", "性别判断规则", "GENERAL", "HIGH", true, "DOC001");
            ruleService.importRules(Collections.singletonList(ruleMap));

            Map<String, Object> pubRequest = new LinkedHashMap<>();
            pubRequest.put("version_no", "1.0.0");
            pubRequest.put("approved_by", "admin");
            ruleService.publish("R_EVAL_001", pubRequest);

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("patient_context", buildPatientContext());

            List<RuleResult> results = ruleService.evaluate(request);

            assertNotNull(results);
            assertTrue(results.stream().anyMatch(r -> "R_EVAL_001".equals(r.getRuleCode()) && r.isHit()));
        }

        @Test
        @DisplayName("已发布配置化规则评估未命中")
        void evaluatePublishedRuleMiss() {
            Map<String, Object> ruleMap = new LinkedHashMap<>();
            ruleMap.put("rule_code", "R_EVAL_MISS");
            ruleMap.put("rule_name", "不命中规则");
            ruleMap.put("rule_type", "GENERAL");
            ruleMap.put("severity", "HIGH");
            ruleMap.put("enabled", true);
            ruleMap.put("reference_document_code", "DOC001");
            Map<String, Object> condition = new LinkedHashMap<>();
            condition.put("fact", "patient.gender");
            condition.put("operator", "equals");
            condition.put("value", "female");
            ruleMap.put("condition", condition);

            ruleService.importRules(Collections.singletonList(ruleMap));

            Map<String, Object> pubRequest = new LinkedHashMap<>();
            pubRequest.put("version_no", "1.0.0");
            pubRequest.put("approved_by", "admin");
            ruleService.publish("R_EVAL_MISS", pubRequest);

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("patient_context", buildPatientContext());

            List<RuleResult> results = ruleService.evaluate(request);

            RuleResult missResult = results.stream()
                    .filter(r -> "R_EVAL_MISS".equals(r.getRuleCode()))
                    .findFirst().orElse(null);
            assertNotNull(missResult);
            assertFalse(missResult.isHit());
        }

        @Test
        @DisplayName("DRAFT 规则不参与评估")
        void evaluateSkipsDraftRules() {
            Map<String, Object> ruleMap = buildRuleMap("R_DRAFT_ONLY", "草稿规则", "GENERAL", "HIGH", true, "DOC001");
            ruleService.importRules(Collections.singletonList(ruleMap));
            // 不发布，保持 DRAFT 状态

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("patient_context", buildPatientContext());

            List<RuleResult> results = ruleService.evaluate(request);

            // DRAFT 规则不参与评估，回退到内置规则
            assertTrue(results.stream().noneMatch(r -> "R_DRAFT_ONLY".equals(r.getRuleCode())));
        }

        @Test
        @DisplayName("disabled 规则不参与评估")
        void evaluateSkipsDisabledRules() {
            Map<String, Object> ruleMap = buildRuleMap("R_DISABLED", "禁用规则", "GENERAL", "HIGH", false, "DOC001");
            ruleService.importRules(Collections.singletonList(ruleMap));

            // 手动设置为 PUBLISHED + disabled
            RuleDefinition def = ruleService.getRule("R_DISABLED", "1.0.0");
            def.setStatus("PUBLISHED");

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("patient_context", buildPatientContext());

            List<RuleResult> results = ruleService.evaluate(request);

            assertTrue(results.stream().noneMatch(r -> "R_DISABLED".equals(r.getRuleCode())));
        }
    }

    // ──────────────────────── simulate ────────────────────────

    @Nested
    @DisplayName("simulate 模拟规则")
    class SimulateTests {

        @Test
        @DisplayName("模拟内联规则定义")
        void simulateInlineRule() {
            Map<String, Object> inlineRule = new LinkedHashMap<>();
            inlineRule.put("rule_code", "R_SIM_INLINE");
            inlineRule.put("rule_name", "内联模拟规则");
            inlineRule.put("rule_type", "GENERAL");
            inlineRule.put("severity", "HIGH");
            Map<String, Object> condition = new LinkedHashMap<>();
            condition.put("fact", "patient.gender");
            condition.put("operator", "equals");
            condition.put("value", "male");
            inlineRule.put("condition", condition);

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("patient_context", buildPatientContext());
            request.put("rule", inlineRule);

            RuleResult result = ruleService.simulate(request);

            assertNotNull(result);
            assertEquals("R_SIM_INLINE", result.getRuleCode());
            assertTrue(result.isHit());
        }

        @Test
        @DisplayName("模拟指定 ruleCode 的已发布规则")
        void simulatePublishedRuleByCode() {
            Map<String, Object> ruleMap = buildRuleMap("R_SIM_PUB", "模拟发布规则", "GENERAL", "HIGH", true, "DOC001");
            ruleService.importRules(Collections.singletonList(ruleMap));

            Map<String, Object> pubRequest = new LinkedHashMap<>();
            pubRequest.put("version_no", "1.0.0");
            pubRequest.put("approved_by", "admin");
            ruleService.publish("R_SIM_PUB", pubRequest);

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("patient_context", buildPatientContext());
            request.put("rule_code", "R_SIM_PUB");

            RuleResult result = ruleService.simulate(request);

            assertNotNull(result);
            assertEquals("R_SIM_PUB", result.getRuleCode());
            assertTrue(result.isHit());
        }

        @Test
        @DisplayName("无匹配规则时回退到内置 STEMI 规则")
        void simulateFallbackToBuiltIn() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("patient_context", buildPatientContext());

            RuleResult result = ruleService.simulate(request);

            assertNotNull(result);
            assertEquals("R_AMI_STEMI_CANDIDATE", result.getRuleCode());
        }

        @Test
        @DisplayName("模拟内联规则未命中")
        void simulateInlineRuleMiss() {
            Map<String, Object> inlineRule = new LinkedHashMap<>();
            inlineRule.put("rule_code", "R_SIM_MISS");
            inlineRule.put("rule_name", "模拟未命中规则");
            Map<String, Object> condition = new LinkedHashMap<>();
            condition.put("fact", "patient.gender");
            condition.put("operator", "equals");
            condition.put("value", "female");
            inlineRule.put("condition", condition);

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("patient_context", buildPatientContext());
            request.put("rule", inlineRule);

            RuleResult result = ruleService.simulate(request);

            assertNotNull(result);
            assertFalse(result.isHit());
        }
    }

    // ──────────────────────── listExecLogs ────────────────────────

    @Nested
    @DisplayName("listExecLogs 查询执行日志")
    class ListExecLogsTests {

        @Test
        @DisplayName("委托给 RuleExecutionLogService 查询")
        void listExecLogsDelegates() {
            List<RuleExecLogEntry> expectedLogs = new ArrayList<>();
            when(ruleExecutionLogService.listExecLogs(any())).thenReturn(expectedLogs);

            Map<String, String> filters = new HashMap<>();
            filters.put("ruleCode", "R_001");
            List<RuleExecLogEntry> logs = ruleService.listExecLogs(filters);

            assertEquals(expectedLogs, logs);
            verify(ruleExecutionLogService).listExecLogs(filters);
        }

        @Test
        @DisplayName("无过滤条件查询全部日志")
        void listExecLogsWithoutFilters() {
            List<RuleExecLogEntry> expectedLogs = new ArrayList<>();
            when(ruleExecutionLogService.listExecLogs(isNull())).thenReturn(expectedLogs);

            List<RuleExecLogEntry> logs = ruleService.listExecLogs(null);

            assertEquals(expectedLogs, logs);
            verify(ruleExecutionLogService).listExecLogs(null);
        }
    }

    // ──────────────────────── summarizeExecLogs ────────────────────────

    @Nested
    @DisplayName("summarizeExecLogs 汇总执行日志")
    class SummarizeExecLogsTests {

        @Test
        @DisplayName("委托给 RuleExecutionLogService 汇总")
        void summarizeExecLogsDelegates() {
            Map<String, Object> expectedSummary = new LinkedHashMap<>();
            expectedSummary.put("total", 10);
            expectedSummary.put("total_hits", 5);
            when(ruleExecutionLogService.summarizeExecLogs(any())).thenReturn(expectedSummary);

            Map<String, String> filters = new HashMap<>();
            filters.put("tenantId", "default");
            Map<String, Object> summary = ruleService.summarizeExecLogs(filters);

            assertEquals(expectedSummary, summary);
            verify(ruleExecutionLogService).summarizeExecLogs(filters);
        }

        @Test
        @DisplayName("无过滤条件汇总全部日志")
        void summarizeExecLogsWithoutFilters() {
            Map<String, Object> expectedSummary = new LinkedHashMap<>();
            expectedSummary.put("total", 0);
            when(ruleExecutionLogService.summarizeExecLogs(isNull())).thenReturn(expectedSummary);

            Map<String, Object> summary = ruleService.summarizeExecLogs(null);

            assertEquals(expectedSummary, summary);
            verify(ruleExecutionLogService).summarizeExecLogs(null);
        }
    }

    // ──────────────────────── getExecLog ────────────────────────

    @Nested
    @DisplayName("getExecLog 获取单条执行日志")
    class GetExecLogTests {

        @Test
        @DisplayName("委托给 RuleExecutionLogService 获取")
        void getExecLogDelegates() {
            RuleExecLogEntry expected = new RuleExecLogEntry();
            expected.setLogId("rxl-1");
            expected.setRuleCode("R_001");
            when(ruleExecutionLogService.getExecLog("rxl-1")).thenReturn(expected);

            RuleExecLogEntry log = ruleService.getExecLog("rxl-1");

            assertNotNull(log);
            assertEquals("rxl-1", log.getLogId());
            verify(ruleExecutionLogService).getExecLog("rxl-1");
        }
    }

    // ──────────────────────── reviewPackage ────────────────────────

    @Nested
    @DisplayName("reviewPackage 审查规则包")
    class ReviewPackageTests {

        @Test
        @DisplayName("审查包含来源文档绑定的规则包")
        void reviewPackageWithReference() {
            Map<String, Object> rule1 = buildRuleMap("R_PKG_001", "包规则1", "GENERAL", "HIGH", true, "DOC001");
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("package_code", "PKG_REVIEW");
            request.put("rules", Collections.singletonList(rule1));
            ruleService.importRules(request);

            Map<String, Object> review = ruleService.reviewPackage("PKG_REVIEW", null);

            assertNotNull(review);
            assertEquals("PKG_REVIEW", review.get("package_code"));
            assertEquals(1, review.get("total_rules"));
            assertTrue((Boolean) review.get("ready_to_publish"));
        }

        @Test
        @DisplayName("审查缺少来源文档绑定的规则包")
        void reviewPackageWithoutReference() {
            Map<String, Object> rule1 = buildRuleMap("R_PKG_NOREF", "无来源包规则", "GENERAL", "HIGH", true, null);
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("package_code", "PKG_NOREF");
            request.put("rules", Collections.singletonList(rule1));
            ruleService.importRules(request);

            Map<String, Object> review = ruleService.reviewPackage("PKG_NOREF", null);

            assertNotNull(review);
            assertFalse((Boolean) review.get("ready_to_publish"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> issues = (List<Map<String, Object>>) review.get("issues");
            assertFalse(issues.isEmpty());
        }

        @Test
        @DisplayName("审查不存在的规则包时抛出异常")
        void reviewNonExistentPackage() {
            assertThrows(IllegalArgumentException.class,
                    () -> ruleService.reviewPackage("PKG_NOT_EXIST", null));
        }
    }

    // ──────────────────────── publishPackage ────────────────────────

    @Nested
    @DisplayName("publishPackage 发布规则包")
    class PublishPackageTests {

        @Test
        @DisplayName("成功发布包含来源文档绑定的规则包")
        void publishPackageSuccess() {
            Map<String, Object> rule1 = buildRuleMap("R_PKG_PUB_001", "可发布包规则", "GENERAL", "HIGH", true, "DOC001");
            Map<String, Object> importRequest = new LinkedHashMap<>();
            importRequest.put("package_code", "PKG_PUB");
            importRequest.put("rules", Collections.singletonList(rule1));
            ruleService.importRules(importRequest);

            Map<String, Object> pubRequest = new LinkedHashMap<>();
            pubRequest.put("approved_by", "admin");

            Map<String, Object> result = ruleService.publishPackage("PKG_PUB", pubRequest);

            assertNotNull(result);
            assertEquals(1, result.get("published_count"));
            assertEquals("admin", result.get("published_by"));
        }

        @Test
        @DisplayName("发布缺少来源文档绑定的规则包时抛出 MissingSourceException")
        void publishPackageWithoutReference() {
            Map<String, Object> rule1 = buildRuleMap("R_PKG_NOREF2", "无来源包规则2", "GENERAL", "HIGH", true, null);
            Map<String, Object> importRequest = new LinkedHashMap<>();
            importRequest.put("package_code", "PKG_NOREF2");
            importRequest.put("rules", Collections.singletonList(rule1));
            ruleService.importRules(importRequest);

            Map<String, Object> pubRequest = new LinkedHashMap<>();
            pubRequest.put("approved_by", "admin");

            assertThrows(MissingSourceException.class,
                    () -> ruleService.publishPackage("PKG_NOREF2", pubRequest));
        }
    }

    // ──────────────────────── evaluateForScenario ────────────────────────

    @Nested
    @DisplayName("evaluateForScenario 场景化评估")
    class EvaluateForScenarioTests {

        @Test
        @DisplayName("缺少 scenario_code 时抛出异常")
        void evaluateForScenarioMissingCode() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("patient_context", buildPatientContext());

            assertThrows(IllegalArgumentException.class,
                    () -> ruleService.evaluateForScenario(request));
        }

        @Test
        @DisplayName("不支持的 scenario_code 时抛出异常")
        void evaluateForScenarioUnsupportedCode() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("scenario_code", "UNSUPPORTED_CODE");
            request.put("patient_context", buildPatientContext());

            assertThrows(IllegalArgumentException.class,
                    () -> ruleService.evaluateForScenario(request));
        }

        @Test
        @DisplayName("缺少 patient_context 时抛出异常")
        void evaluateForScenarioMissingPatientContext() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("scenario_code", "PATHWAY_ENTRY");

            assertThrows(IllegalArgumentException.class,
                    () -> ruleService.evaluateForScenario(request));
        }

        @Test
        @DisplayName("空 patient_context 时抛出异常")
        void evaluateForScenarioEmptyPatientContext() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("scenario_code", "PATHWAY_ENTRY");
            request.put("patient_context", new LinkedHashMap<>());

            assertThrows(IllegalArgumentException.class,
                    () -> ruleService.evaluateForScenario(request));
        }

        @Test
        @DisplayName("场景化评估返回标准化结果信封")
        void evaluateForScenarioReturnsEnvelope() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("scenario_code", "PATHWAY_ENTRY");
            request.put("patient_context", buildPatientContext());

            Map<String, Object> result = ruleService.evaluateForScenario(request);

            assertNotNull(result);
            assertNotNull(result.get("result_id"));
            assertNotNull(result.get("trace_id"));
            assertEquals("PATHWAY_ENTRY", result.get("scenario_code"));
            assertNotNull(result.get("results"));
        }
    }

    // ──────────────────────── getEvaluation ────────────────────────

    @Nested
    @DisplayName("getEvaluation 获取评估结果")
    class GetEvaluationTests {

        @Test
        @DisplayName("获取已记录的评估结果")
        void getEvaluationSuccess() {
            // 先执行一次场景化评估以产生记录
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("scenario_code", "PATHWAY_ENTRY");
            request.put("patient_context", buildPatientContext());

            Map<String, Object> evalResult = ruleService.evaluateForScenario(request);
            String resultId = String.valueOf(evalResult.get("result_id"));

            Map<String, Object> fetched = ruleService.getEvaluation(resultId);

            assertNotNull(fetched);
            assertEquals(resultId, fetched.get("result_id"));
        }

        @Test
        @DisplayName("获取不存在的评估结果时抛出异常")
        void getEvaluationNotFound() {
            assertThrows(IllegalArgumentException.class,
                    () -> ruleService.getEvaluation("nonexistent-id"));
        }

        @Test
        @DisplayName("resultId 为 null 时抛出异常")
        void getEvaluationNullId() {
            assertThrows(IllegalArgumentException.class,
                    () -> ruleService.getEvaluation(null));
        }
    }

    // ──────────────────────── listEvaluations ────────────────────────

    @Nested
    @DisplayName("listEvaluations 列出评估结果")
    class ListEvaluationsTests {

        @Test
        @DisplayName("无评估记录时返回空列表")
        void listEvaluationsEmpty() {
            List<Map<String, Object>> evaluations = ruleService.listEvaluations(null);

            assertTrue(evaluations.isEmpty());
        }

        @Test
        @DisplayName("执行评估后可列出评估记录")
        void listEvaluationsAfterEval() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("scenario_code", "EMR_QC");
            request.put("patient_context", buildPatientContext());
            ruleService.evaluateForScenario(request);

            List<Map<String, Object>> evaluations = ruleService.listEvaluations(null);

            assertFalse(evaluations.isEmpty());
        }
    }

    // ──────────────────────── batchEvaluateForScenario ────────────────────────

    @Nested
    @DisplayName("batchEvaluateForScenario 批量场景化评估")
    class BatchEvaluateForScenarioTests {

        @Test
        @DisplayName("批量评估多条患者上下文")
        void batchEvaluateSuccess() {
            Map<String, Object> item1 = new LinkedHashMap<>();
            item1.put("patient_context", buildPatientContext());
            item1.put("case_id", "CASE001");

            Map<String, Object> patientCtx2 = new LinkedHashMap<>();
            Map<String, Object> patient2 = new LinkedHashMap<>();
            patient2.put("patient_id", "P002");
            patient2.put("gender", "female");
            patientCtx2.put("patient", patient2);
            Map<String, Object> encounter2 = new LinkedHashMap<>();
            encounter2.put("encounter_id", "E002");
            patientCtx2.put("encounter", encounter2);
            Map<String, Object> facts2 = new LinkedHashMap<>();
            patientCtx2.put("facts", facts2);

            Map<String, Object> item2 = new LinkedHashMap<>();
            item2.put("patient_context", patientCtx2);
            item2.put("case_id", "CASE002");

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("scenario_code", "PATHWAY_ENTRY");
            request.put("items", Arrays.asList(item1, item2));

            Map<String, Object> result = ruleService.batchEvaluateForScenario(request);

            assertNotNull(result);
            assertNotNull(result.get("batch_id"));
            assertEquals(2, result.get("total_items"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> evaluations = (List<Map<String, Object>>) result.get("evaluations");
            assertEquals(2, evaluations.size());
        }

        @Test
        @DisplayName("items 为空时抛出异常")
        void batchEvaluateEmptyItems() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("scenario_code", "PATHWAY_ENTRY");
            request.put("items", Collections.emptyList());

            assertThrows(IllegalArgumentException.class,
                    () -> ruleService.batchEvaluateForScenario(request));
        }
    }
}
