package com.zyengine.rule;

import com.zyengine.common.TraceContext;
import com.zyengine.dto.RuleResult;
import com.zyengine.organization.OrganizationContext;
import com.zyengine.persistence.EnginePersistenceService;
import com.zyengine.util.ClinicalFactUtils;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RuleService {
    private static final int EXEC_LOG_RING_CAPACITY = 500;
    private static final int EVALUATION_RING_CAPACITY = 500;
    private static final String DEFAULT_TENANT_ID = "default";
    private static final String DEFAULT_HOSPITAL_CODE = "ZYHOSPITAL";
    private static final String DEFAULT_SCOPE_LEVEL = "HOSPITAL";

    // 第三方规则引擎对外开放的标准场景码，与产品化方案/前端规则校验工作台保持一致。
    private static final Set<String> SUPPORTED_RULE_ENGINE_SCENARIOS = new LinkedHashSet<String>(Arrays.asList(
            "PATHWAY_ENTRY", "EMR_QC", "INSURANCE_QC", "ORDER_SAFETY",
            "DRUG_INDICATION", "EXAM_RATIONALITY"));

    // 历史规则只声明 rule_type 时的默认场景路由，避免老规则需要立刻补 scenario_codes 才能被第三方调用。
    private static final Map<String, Set<String>> DEFAULT_RULE_TYPE_SCENARIOS = defaultRuleTypeScenarios();

    // 暴露给列表/详情接口的摘要字段，列表返回不包含 results/warnings 详情，避免大 payload。
    private static final List<String> EVALUATION_SUMMARY_FIELDS = Arrays.asList(
            "result_id", "batch_id", "case_id", "scenario_code",
            "rule_package_code", "rule_package_version", "source",
            "evaluated_count", "hit_count", "elapsed_ms",
            "trace_id", "operator_id", "tenant_id",
            "group_code", "hospital_code", "campus_code", "site_code", "department_code",
            "scope_level", "scope_code", "org_source",
            "patient_id", "encounter_id", "created_time");

    private final EnginePersistenceService persistenceService;
    private final RuleDslEvaluator dslEvaluator = new RuleDslEvaluator();
    private final Map<String, RuleDefinition> ruleStore = new ConcurrentHashMap<String, RuleDefinition>();
    private final Deque<RuleExecLogEntry> execLogs = new ConcurrentLinkedDeque<RuleExecLogEntry>();
    private final AtomicLong execLogSequence = new AtomicLong();
    // 第三方规则引擎评估结果环形缓冲；后续可接 Oracle/达梦持久化，但单批先保证 DB-only 也能回查。
    private final Deque<Map<String, Object>> evaluationStore = new ConcurrentLinkedDeque<Map<String, Object>>();
    private final AtomicLong evaluationSequence = new AtomicLong();
    private final AtomicLong batchSequence = new AtomicLong();

    public RuleService(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public List<RuleDefinition> importRules(Object request) {
        return importRules(request, null);
    }

    public List<RuleDefinition> importRules(Object request, OrganizationContext orgContext) {
        List<Map<String, Object>> rules = normalizeRules(request);
        List<RuleDefinition> imported = new ArrayList<RuleDefinition>();
        for (Map<String, Object> rule : rules) {
            RuleDefinition definition = toDefinition(rule, "DRAFT");
            applyOrganization(definition, orgContext);
            ruleStore.put(key(definition), definition);
            persistenceService.saveRuleDefinition(definition, null);
            imported.add(definition);
        }
        return imported;
    }

    public RuleDefinition publish(String ruleCode, Map<String, Object> request) {
        return publish(ruleCode, request, null);
    }

    public RuleDefinition publish(String ruleCode, Map<String, Object> request, OrganizationContext orgContext) {
        String versionNo = string(request.get("version_no"), "1.0.0");
        RuleDefinition definition = findRule(ruleCode, versionNo, orgContext);
        if (definition == null) {
            throw new IllegalArgumentException("rule not found: " + ruleCode + "@" + versionNo);
        }
        String approvedBy = string(request.get("approved_by"), null);
        markPublished(definition, approvedBy);
        persistenceService.saveRuleDefinition(definition, approvedBy);
        auditRuleChange("PUBLISH", "RULE", definition.getRuleCode(), approvedBy,
                packageReviewForAudit(definition.getPackageCode(), definition.getPackageVersion(), 1, definition));
        return definition;
    }

    public Map<String, Object> reviewPackage(String packageCode, String packageVersion) {
        return reviewPackage(packageCode, packageVersion, null);
    }

    public Map<String, Object> reviewPackage(String packageCode, String packageVersion, OrganizationContext orgContext) {
        List<RuleDefinition> rules = rulesInPackage(packageCode, packageVersion, orgContext);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("rule package not found: " + packageCode);
        }
        return buildPackageReview(packageCode, packageVersion, rules);
    }

    public Map<String, Object> publishPackage(String packageCode, Map<String, Object> request) {
        return publishPackage(packageCode, request, null);
    }

    public Map<String, Object> publishPackage(String packageCode, Map<String, Object> request,
                                              OrganizationContext orgContext) {
        String packageVersion = string(request == null ? null : request.get("package_version"), null);
        String approvedBy = string(request == null ? null : request.get("approved_by"), null);
        List<RuleDefinition> rules = rulesInPackage(packageCode, packageVersion, orgContext);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("rule package not found: " + packageCode);
        }

        Map<String, Object> review = buildPackageReview(packageCode, packageVersion, rules);
        if (!Boolean.TRUE.equals(review.get("ready_to_publish"))) {
            throw new IllegalArgumentException("rule package is not ready to publish: " + review.get("issues"));
        }

        int published = 0;
        for (RuleDefinition definition : rules) {
            markPublished(definition, approvedBy);
            persistenceService.saveRuleDefinition(definition, approvedBy);
            published++;
        }
        Map<String, Object> result = buildPackageReview(packageCode, packageVersion, rules);
        result.put("published_count", published);
        result.put("published_by", approvedBy);
        auditRuleChange("PUBLISH_PACKAGE", "RULE_PACKAGE", packageCode, approvedBy, result);
        return result;
    }

    public List<RuleDefinition> listRules() {
        return listRules(null);
    }

    public List<RuleDefinition> listRules(Map<String, String> filters) {
        List<RuleDefinition> list = new ArrayList<RuleDefinition>(ruleStore.values());
        list = filterRulesByOrg(list, filters);
        Collections.sort(list, new Comparator<RuleDefinition>() {
            @Override
            public int compare(RuleDefinition left, RuleDefinition right) {
                int byRule = left.getRuleCode().compareTo(right.getRuleCode());
                if (byRule != 0) {
                    return byRule;
                }
                return string(left.getScopeCode(), "").compareTo(string(right.getScopeCode(), ""));
            }
        });
        return list;
    }

    private List<RuleDefinition> rulesInPackage(String packageCode, String packageVersion) {
        return rulesInPackage(packageCode, packageVersion, null);
    }

    private List<RuleDefinition> rulesInPackage(String packageCode, String packageVersion,
                                                OrganizationContext orgContext) {
        String code = string(packageCode, null);
        if (code == null) {
            throw new IllegalArgumentException("packageCode is required");
        }
        List<RuleDefinition> rules = new ArrayList<RuleDefinition>();
        for (RuleDefinition definition : ruleStore.values()) {
            if (!code.equals(definition.getPackageCode())) {
                continue;
            }
            if (packageVersion != null && !packageVersion.equals(definition.getPackageVersion())) {
                continue;
            }
            if (!matchesDefinitionContext(definition, orgContext)) {
                continue;
            }
            rules.add(definition);
        }
        Collections.sort(rules, new Comparator<RuleDefinition>() {
            @Override
            public int compare(RuleDefinition left, RuleDefinition right) {
                Integer leftPriority = integer(left.getRuleJson().get("priority"), 0);
                Integer rightPriority = integer(right.getRuleJson().get("priority"), 0);
                int byPriority = rightPriority.compareTo(leftPriority);
                return byPriority != 0 ? byPriority : left.getRuleCode().compareTo(right.getRuleCode());
            }
        });
        return rules;
    }

    private Map<String, Object> buildPackageReview(String packageCode, String packageVersion, List<RuleDefinition> rules) {
        Map<String, Integer> byType = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byStatus = new LinkedHashMap<String, Integer>();
        List<Map<String, Object>> issues = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> ruleSummaries = new ArrayList<Map<String, Object>>();

        int enabledRules = 0;
        int draftRules = 0;
        int publishedRules = 0;
        int disabledRules = 0;
        String resolvedPackageVersion = packageVersion;
        for (RuleDefinition definition : rules) {
            if (resolvedPackageVersion == null) {
                resolvedPackageVersion = definition.getPackageVersion();
            } else if (definition.getPackageVersion() != null && !resolvedPackageVersion.equals(definition.getPackageVersion())) {
                resolvedPackageVersion = "MIXED";
            }
            if (definition.isEnabled()) {
                enabledRules++;
            } else {
                disabledRules++;
            }
            if ("DRAFT".equalsIgnoreCase(definition.getStatus())) {
                draftRules++;
            }
            if ("PUBLISHED".equalsIgnoreCase(definition.getStatus())) {
                publishedRules++;
            }
            increment(byType, definition.getRuleType());
            increment(byStatus, definition.getStatus());
            collectDslIssues(definition, issues);
            ruleSummaries.add(ruleSummary(definition));
        }

        Map<String, Object> review = new LinkedHashMap<String, Object>();
        review.put("package_code", packageCode);
        review.put("package_version", resolvedPackageVersion);
        if (!rules.isEmpty()) {
            putOrgFields(review, rules.get(0));
        }
        review.put("total_rules", rules.size());
        review.put("enabled_rules", enabledRules);
        review.put("draft_rules", draftRules);
        review.put("published_rules", publishedRules);
        review.put("disabled_rules", disabledRules);
        review.put("ready_to_publish", !rules.isEmpty() && issues.isEmpty());
        review.put("issues", issues);
        review.put("by_type", bucketsToList(byType, "rule_type"));
        review.put("by_status", bucketsToList(byStatus, "status"));
        review.put("rules", ruleSummaries);
        return review;
    }

    private Map<String, Object> ruleSummary(RuleDefinition definition) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("rule_code", definition.getRuleCode());
        summary.put("rule_name", definition.getRuleName());
        summary.put("version_no", definition.getVersionNo());
        summary.put("rule_type", definition.getRuleType());
        summary.put("status", definition.getStatus());
        summary.put("enabled", definition.isEnabled());
        summary.put("severity", definition.getSeverity());
        summary.put("priority", integer(definition.getRuleJson().get("priority"), 0));
        summary.put("tenant_id", definition.getTenantId());
        summary.put("hospital_code", definition.getHospitalCode());
        summary.put("scope_level", definition.getScopeLevel());
        summary.put("scope_code", definition.getScopeCode());
        return summary;
    }

    private void collectDslIssues(RuleDefinition definition, List<Map<String, Object>> issues) {
        collectDslIssues(definition.getRuleCode(), definition.getRuleJson().get("condition"), "condition", issues);
    }

    @SuppressWarnings("unchecked")
    private void collectDslIssues(String ruleCode, Object node, String path, List<Map<String, Object>> issues) {
        if (!(node instanceof Map)) {
            issues.add(issue(ruleCode, path, "condition must be an object"));
            return;
        }
        Map<String, Object> condition = (Map<String, Object>) node;
        Object all = condition.get("all");
        Object any = condition.get("any");
        if (all instanceof Collection || any instanceof Collection) {
            Collection<?> children = all instanceof Collection ? (Collection<?>) all : (Collection<?>) any;
            String childKey = all instanceof Collection ? "all" : "any";
            if (children.isEmpty()) {
                issues.add(issue(ruleCode, path + "." + childKey, "condition group must not be empty"));
            }
            int index = 0;
            for (Object child : children) {
                collectDslIssues(ruleCode, child, path + "." + childKey + "[" + index + "]", issues);
                index++;
            }
            return;
        }

        String fact = string(condition.get("fact"), null);
        String operator = string(condition.get("operator"), null);
        if (fact == null) {
            issues.add(issue(ruleCode, path + ".fact", "fact is required"));
        }
        if (operator == null) {
            issues.add(issue(ruleCode, path + ".operator", "operator is required"));
            return;
        }
        if (!supportedOperator(operator)) {
            issues.add(issue(ruleCode, path + ".operator", "unsupported operator: " + operator));
            return;
        }
        if ("in".equals(operator) && !(condition.get("value") instanceof Collection)) {
            issues.add(issue(ruleCode, path + ".value", "operator in requires collection value"));
        }
        if ("within_minutes_from".equals(operator)) {
            Object value = condition.get("value");
            if (!(value instanceof Map)) {
                issues.add(issue(ruleCode, path + ".value", "within_minutes_from requires window object"));
            } else {
                Map<String, Object> window = (Map<String, Object>) value;
                if (string(window.get("from"), null) == null || integer(window.get("minutes"), -1) < 0) {
                    issues.add(issue(ruleCode, path + ".value", "within_minutes_from requires from and minutes"));
                }
            }
        }
    }

    private Map<String, Object> issue(String ruleCode, String field, String message) {
        Map<String, Object> issue = new LinkedHashMap<String, Object>();
        issue.put("rule_code", ruleCode);
        issue.put("severity", "ERROR");
        issue.put("field", field);
        issue.put("message", message);
        return issue;
    }

    private boolean supportedOperator(String operator) {
        return "exists".equals(operator)
                || "equals".equals(operator)
                || "in".equals(operator)
                || "contains".equals(operator)
                || "within_minutes_from".equals(operator);
    }

    public RuleDefinition getRule(String ruleCode, String versionNo) {
        return getRule(ruleCode, versionNo, null);
    }

    public RuleDefinition getRule(String ruleCode, String versionNo, OrganizationContext orgContext) {
        return findRule(ruleCode, versionNo, orgContext);
    }

    private RuleDefinition findRule(String ruleCode, String versionNo, OrganizationContext orgContext) {
        if (versionNo != null && !versionNo.trim().isEmpty()) {
            RuleDefinition exact = ruleStore.get(key(orgContext, ruleCode, versionNo));
            if (exact != null) {
                return exact;
            }
            return ruleStore.get(legacyKey(ruleCode, versionNo));
        }
        RuleDefinition latest = null;
        for (RuleDefinition definition : ruleStore.values()) {
            if (ruleCode.equals(definition.getRuleCode()) && matchesDefinitionContext(definition, orgContext)) {
                latest = definition;
            }
        }
        if (latest == null && orgContext != null) {
            for (RuleDefinition definition : ruleStore.values()) {
                if (ruleCode.equals(definition.getRuleCode()) && isLegacyDefault(definition)) {
                    latest = definition;
                }
            }
        }
        return latest;
    }

    public List<RuleResult> evaluate(Map<String, Object> request) {
        return evaluate(request, null);
    }

    public List<RuleResult> evaluate(Map<String, Object> request, OrganizationContext orgContext) {
        Map<String, Object> patientContext = getPatientContext(request);
        List<RuleDefinition> published = publishedRules(orgContext);
        if (published.isEmpty()) {
            // 未导入规则时保留内置AMI规则，保证旧演示和健康验证不被配置化改造打断。
            return evaluateBuiltInRules(patientContext, orgContext);
        }

        List<RuleResult> results = new ArrayList<RuleResult>();
        for (RuleDefinition definition : published) {
            results.add(executeDefinition(definition, patientContext));
        }
        return results;
    }

    /**
     * RULE-001 第三方规则引擎 API：通过 scenario_code + rule_package_code 路由到已发布规则，
     * 输出标准化结果信封并写入审计。第二批补齐：分配 result_id 并落入评估环形缓冲供回查。
     */
    public Map<String, Object> evaluateForScenario(Map<String, Object> request) {
        return evaluateForScenario(request, null);
    }

    public Map<String, Object> evaluateForScenario(Map<String, Object> request, OrganizationContext orgContext) {
        ScenarioInvocation invocation = parseInvocation(request, orgContext);
        Map<String, Object> patientContext = requirePatientContext(request.get("patient_context"));
        ScenarioPipelineOutcome outcome = runScenarioPipeline(invocation, patientContext);

        Map<String, Object> evaluation = recordEvaluation("SINGLE", null, null,
                invocation, patientContext, outcome);

        auditEvaluation("EVALUATE_SCENARIO", invocation, patientContext, outcome,
                Collections.singletonList(String.valueOf(evaluation.get("result_id"))), null);
        return evaluation;
    }

    /**
     * RULE-001 第二批：批量同步评估。items 内每个 patient_context 共享同一个 scenario_code/package 过滤条件，
     * 每条独立写入 evaluation ring 并返回单独的 result_id，便于 UI 工作台批量复演与抽样回查。
     */
    public Map<String, Object> batchEvaluateForScenario(Map<String, Object> request) {
        return batchEvaluateForScenario(request, null);
    }

    public Map<String, Object> batchEvaluateForScenario(Map<String, Object> request, OrganizationContext orgContext) {
        ScenarioInvocation invocation = parseInvocation(request, orgContext);
        Object itemsRaw = request == null ? null : request.get("items");
        if (!(itemsRaw instanceof Collection) || ((Collection<?>) itemsRaw).isEmpty()) {
            throw new IllegalArgumentException("items must be a non-empty array");
        }

        String batchId = "rebatch-" + batchSequence.incrementAndGet();
        String createdTime = nowText();
        long batchStarted = System.currentTimeMillis();
        List<Map<String, Object>> evaluations = new ArrayList<Map<String, Object>>();
        List<String> resultIds = new ArrayList<String>();
        int totalEvaluated = 0;
        int totalHits = 0;
        int index = 0;

        for (Object item : (Collection<?>) itemsRaw) {
            if (!(item instanceof Map)) {
                throw new IllegalArgumentException("items[" + index + "] must be an object with patient_context");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> itemMap = (Map<String, Object>) item;
            Map<String, Object> patientContext;
            try {
                patientContext = requirePatientContext(itemMap.get("patient_context"));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("items[" + index + "]: " + ex.getMessage());
            }
            String caseId = string(itemMap.get("case_id"), null);

            ScenarioPipelineOutcome outcome = runScenarioPipeline(invocation, patientContext);
            Map<String, Object> evaluation = recordEvaluation("BATCH", batchId, caseId,
                    invocation, patientContext, outcome);
            evaluations.add(evaluation);
            resultIds.add(String.valueOf(evaluation.get("result_id")));
            totalEvaluated += outcome.candidatesCount;
            totalHits += outcome.hitCount;
            index++;
        }

        long elapsed = System.currentTimeMillis() - batchStarted;

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("trace_id", TraceContext.getTraceId());
        response.put("batch_id", batchId);
        response.put("scenario_code", invocation.scenarioCode);
        response.put("rule_package_code", invocation.packageCode);
        response.put("rule_package_version", invocation.packageVersion);
        response.put("total_items", evaluations.size());
        response.put("total_evaluated", totalEvaluated);
        response.put("total_hits", totalHits);
        response.put("elapsed_ms", elapsed);
        response.put("created_time", createdTime);
        response.put("evaluations", evaluations);

        auditEvaluation("BATCH_EVALUATE_SCENARIO", invocation, null,
                aggregatePipelineOutcome(totalEvaluated, totalHits, elapsed), resultIds, batchId);
        return response;
    }

    public Map<String, Object> getEvaluation(String resultId) {
        if (resultId == null || resultId.trim().isEmpty()) {
            throw new IllegalArgumentException("resultId is required");
        }
        for (Map<String, Object> evaluation : evaluationStore) {
            if (resultId.equals(evaluation.get("result_id"))) {
                return new LinkedHashMap<String, Object>(evaluation);
            }
        }
        throw new IllegalArgumentException("rule engine evaluation not found: " + resultId);
    }

    public List<Map<String, Object>> listEvaluations(Map<String, String> filters) {
        String scenarioCode = upper(filterValue(filters, "scenarioCode"));
        String packageCode = filterValue(filters, "packageCode");
        String batchId = filterValue(filters, "batchId");
        String source = upper(filterValue(filters, "source"));
        String patientId = filterValue(filters, "patientId");
        String encounterId = filterValue(filters, "encounterId");
        String tenantId = filterValue(filters, "tenantId");
        String groupCode = filterValue(filters, "groupCode");
        String hospitalCode = filterValue(filters, "hospitalCode");
        String campusCode = filterValue(filters, "campusCode");
        String siteCode = filterValue(filters, "siteCode");
        String departmentCode = filterValue(filters, "departmentCode");
        String scopeLevel = upper(filterValue(filters, "scopeLevel"));
        String scopeCode = filterValue(filters, "scopeCode");
        int limit = filterInt(filters, "limit", 100);
        int offset = filterInt(filters, "offset", 0);
        if (limit <= 0 || limit > EVALUATION_RING_CAPACITY) {
            limit = EVALUATION_RING_CAPACITY;
        }
        if (offset < 0) {
            offset = 0;
        }

        List<Map<String, Object>> matched = new ArrayList<Map<String, Object>>();
        int skipped = 0;
        java.util.Iterator<Map<String, Object>> iterator = evaluationStore.descendingIterator();
        while (iterator.hasNext() && matched.size() < limit) {
            Map<String, Object> evaluation = iterator.next();
            if (scenarioCode != null && !scenarioCode.equalsIgnoreCase(string(evaluation.get("scenario_code"), null))) {
                continue;
            }
            if (packageCode != null && !packageCode.equalsIgnoreCase(string(evaluation.get("rule_package_code"), null))) {
                continue;
            }
            if (batchId != null && !batchId.equals(evaluation.get("batch_id"))) {
                continue;
            }
            if (source != null && !source.equalsIgnoreCase(string(evaluation.get("source"), null))) {
                continue;
            }
            if (patientId != null && !patientId.equals(evaluation.get("patient_id"))) {
                continue;
            }
            if (encounterId != null && !encounterId.equals(evaluation.get("encounter_id"))) {
                continue;
            }
            if (tenantId != null && !tenantId.equals(evaluation.get("tenant_id"))) {
                continue;
            }
            if (groupCode != null && !groupCode.equals(evaluation.get("group_code"))) {
                continue;
            }
            if (hospitalCode != null && !hospitalCode.equals(evaluation.get("hospital_code"))) {
                continue;
            }
            if (campusCode != null && !campusCode.equals(evaluation.get("campus_code"))) {
                continue;
            }
            if (siteCode != null && !siteCode.equals(evaluation.get("site_code"))) {
                continue;
            }
            if (departmentCode != null && !departmentCode.equals(evaluation.get("department_code"))) {
                continue;
            }
            if (scopeLevel != null && !scopeLevel.equalsIgnoreCase(string(evaluation.get("scope_level"), null))) {
                continue;
            }
            if (scopeCode != null && !scopeCode.equals(evaluation.get("scope_code"))) {
                continue;
            }
            if (skipped < offset) {
                skipped++;
                continue;
            }
            matched.add(evaluationSummary(evaluation));
        }
        return matched;
    }

    private ScenarioInvocation parseInvocation(Map<String, Object> request, OrganizationContext orgContext) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        String scenarioCode = upper(string(request.get("scenario_code"), null));
        if (scenarioCode == null) {
            throw new IllegalArgumentException("scenario_code is required");
        }
        if (!SUPPORTED_RULE_ENGINE_SCENARIOS.contains(scenarioCode)) {
            throw new IllegalArgumentException("unsupported scenario_code: " + scenarioCode
                    + "; supported: " + SUPPORTED_RULE_ENGINE_SCENARIOS);
        }
        ScenarioInvocation invocation = new ScenarioInvocation();
        invocation.scenarioCode = scenarioCode;
        invocation.packageCode = string(request.get("rule_package_code"), null);
        invocation.packageVersion = string(request.get("rule_package_version"), null);
        invocation.ruleCodeFilter = stringList(request.get("rule_codes"));
        invocation.operatorId = string(request.get("operator_id"), null);
        // 优先采用 Controller 已经解析好的 OrganizationContext；老调用者只传 body 时按 body.tenant_id 兜底。
        if (orgContext != null) {
            invocation.tenantId = orgContext.getTenantId();
            invocation.groupCode = orgContext.getGroupCode();
            invocation.hospitalCode = orgContext.getHospitalCode();
            invocation.campusCode = orgContext.getCampusCode();
            invocation.siteCode = orgContext.getSiteCode();
            invocation.departmentCode = orgContext.getDepartmentCode();
            invocation.scopeLevel = orgContext.getEffectiveScopeLevel();
            invocation.scopeCode = orgContext.getEffectiveScopeCode();
            invocation.orgSource = orgContext.getSource();
        } else {
            invocation.tenantId = string(request.get("tenant_id"), DEFAULT_TENANT_ID);
            invocation.hospitalCode = string(request.get("hospital_code"), DEFAULT_HOSPITAL_CODE);
            invocation.scopeLevel = DEFAULT_SCOPE_LEVEL;
            invocation.scopeCode = invocation.hospitalCode;
            invocation.orgSource = "DEFAULT";
        }
        return invocation;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requirePatientContext(Object raw) {
        // 第三方接入要求显式声明 patient_context，以避免把 scenario_code 这种控制参数误当作上下文事实。
        if (!(raw instanceof Map)) {
            throw new IllegalArgumentException("patient_context is required");
        }
        Map<String, Object> context = (Map<String, Object>) raw;
        if (context.isEmpty()) {
            throw new IllegalArgumentException("patient_context is required");
        }
        return context;
    }

    private ScenarioPipelineOutcome runScenarioPipeline(ScenarioInvocation invocation,
                                                       Map<String, Object> patientContext) {
        long started = System.currentTimeMillis();
        List<RuleDefinition> candidates = scenarioCandidates(invocation);

        List<Map<String, Object>> resultEntries = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> warnings = new ArrayList<Map<String, Object>>();
        int hitCount = 0;
        for (RuleDefinition definition : candidates) {
            Map<String, Object> entry = executeForScenario(definition, invocation.scenarioCode, patientContext);
            if (Boolean.TRUE.equals(entry.get("hit"))) {
                hitCount++;
            }
            resultEntries.add(entry);
        }
        if (candidates.isEmpty()) {
            // 没有匹配规则时不抛异常，避免第三方系统被规则尚未配置的情况打断；返回 warning 即可。
            Map<String, Object> warning = new LinkedHashMap<String, Object>();
            warning.put("severity", "INFO");
            warning.put("code", "NO_RULES_MATCHED");
            warning.put("message", "未找到匹配的已发布规则，请确认 scenario_code/rule_package_code 配置。");
            warnings.add(warning);
        }

        ScenarioPipelineOutcome outcome = new ScenarioPipelineOutcome();
        outcome.candidatesCount = candidates.size();
        outcome.hitCount = hitCount;
        outcome.elapsedMs = System.currentTimeMillis() - started;
        outcome.resultEntries = resultEntries;
        outcome.warnings = warnings;
        return outcome;
    }

    private Map<String, Object> recordEvaluation(String source, String batchId, String caseId,
                                                 ScenarioInvocation invocation, Map<String, Object> patientContext,
                                                 ScenarioPipelineOutcome outcome) {
        String resultId = "reval-" + evaluationSequence.incrementAndGet();
        Map<String, Object> evaluation = new LinkedHashMap<String, Object>();
        evaluation.put("trace_id", TraceContext.getTraceId());
        evaluation.put("result_id", resultId);
        evaluation.put("batch_id", batchId);
        evaluation.put("case_id", caseId);
        evaluation.put("scenario_code", invocation.scenarioCode);
        evaluation.put("rule_package_code", invocation.packageCode);
        evaluation.put("rule_package_version", invocation.packageVersion);
        evaluation.put("source", source);
        evaluation.put("evaluated_count", outcome.candidatesCount);
        evaluation.put("hit_count", outcome.hitCount);
        evaluation.put("elapsed_ms", outcome.elapsedMs);
        evaluation.put("operator_id", invocation.operatorId);
        evaluation.put("tenant_id", invocation.tenantId);
        evaluation.put("group_code", invocation.groupCode);
        evaluation.put("hospital_code", invocation.hospitalCode);
        evaluation.put("campus_code", invocation.campusCode);
        evaluation.put("site_code", invocation.siteCode);
        evaluation.put("department_code", invocation.departmentCode);
        evaluation.put("scope_level", invocation.scopeLevel);
        evaluation.put("scope_code", invocation.scopeCode);
        evaluation.put("org_source", invocation.orgSource);
        evaluation.put("patient_id", ClinicalFactUtils.patientId(patientContext));
        evaluation.put("encounter_id", ClinicalFactUtils.encounterId(patientContext));
        evaluation.put("created_time", nowText());
        evaluation.put("results", outcome.resultEntries);
        evaluation.put("warnings", outcome.warnings);

        // 落入评估环形缓冲；存储的是详情副本，list/get 接口都基于它再分发。
        evaluationStore.addLast(new LinkedHashMap<String, Object>(evaluation));
        while (evaluationStore.size() > EVALUATION_RING_CAPACITY) {
            evaluationStore.pollFirst();
        }
        return evaluation;
    }

    private Map<String, Object> evaluationSummary(Map<String, Object> evaluation) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        for (String field : EVALUATION_SUMMARY_FIELDS) {
            summary.put(field, evaluation.get(field));
        }
        return summary;
    }

    private void auditEvaluation(String actionType, ScenarioInvocation invocation,
                                 Map<String, Object> patientContext, ScenarioPipelineOutcome outcome,
                                 List<String> resultIds, String batchId) {
        Map<String, Object> auditDetail = new LinkedHashMap<String, Object>();
        auditDetail.put("scenario_code", invocation.scenarioCode);
        auditDetail.put("rule_package_code", invocation.packageCode);
        auditDetail.put("rule_package_version", invocation.packageVersion);
        auditDetail.put("rule_codes_filter", invocation.ruleCodeFilter);
        auditDetail.put("evaluated_count", outcome.candidatesCount);
        auditDetail.put("hit_count", outcome.hitCount);
        auditDetail.put("elapsed_ms", outcome.elapsedMs);
        auditDetail.put("tenant_id", invocation.tenantId);
        auditDetail.put("group_code", invocation.groupCode);
        auditDetail.put("hospital_code", invocation.hospitalCode);
        auditDetail.put("campus_code", invocation.campusCode);
        auditDetail.put("site_code", invocation.siteCode);
        auditDetail.put("department_code", invocation.departmentCode);
        auditDetail.put("scope_level", invocation.scopeLevel);
        auditDetail.put("scope_code", invocation.scopeCode);
        auditDetail.put("org_source", invocation.orgSource);
        auditDetail.put("result_ids", resultIds);
        auditDetail.put("batch_id", batchId);
        try {
            String patientId = patientContext == null ? null : ClinicalFactUtils.patientId(patientContext);
            String encounterId = patientContext == null ? null : ClinicalFactUtils.encounterId(patientContext);
            persistenceService.saveAuditLog("RULE_ENGINE", actionType, "SCENARIO",
                    invocation.scenarioCode, patientId, encounterId, invocation.operatorId, auditDetail);
        } catch (RuntimeException ignored) {
            // 第三方调用主流程不能因审计落库失败中断，审计降级在内存环形日志中可继续观测。
        }
    }

    private ScenarioPipelineOutcome aggregatePipelineOutcome(int evaluatedCount, int hitCount, long elapsedMs) {
        ScenarioPipelineOutcome aggregate = new ScenarioPipelineOutcome();
        aggregate.candidatesCount = evaluatedCount;
        aggregate.hitCount = hitCount;
        aggregate.elapsedMs = elapsedMs;
        aggregate.resultEntries = Collections.emptyList();
        aggregate.warnings = Collections.emptyList();
        return aggregate;
    }

    private static class ScenarioInvocation {
        String scenarioCode;
        String packageCode;
        String packageVersion;
        List<String> ruleCodeFilter = Collections.emptyList();
        String operatorId;
        String tenantId;
        String groupCode;
        String hospitalCode;
        String campusCode;
        String siteCode;
        String departmentCode;
        String scopeLevel;
        String scopeCode;
        String orgSource;
    }

    private static class ScenarioPipelineOutcome {
        int candidatesCount;
        int hitCount;
        long elapsedMs;
        List<Map<String, Object>> resultEntries = Collections.emptyList();
        List<Map<String, Object>> warnings = Collections.emptyList();
    }

    private List<RuleDefinition> scenarioCandidates(ScenarioInvocation invocation) {
        List<RuleDefinition> allCandidates = new ArrayList<RuleDefinition>();
        List<RuleDefinition> exactCandidates = new ArrayList<RuleDefinition>();
        List<RuleDefinition> legacyCandidates = new ArrayList<RuleDefinition>();
        for (RuleDefinition definition : publishedRules()) {
            if (invocation.packageCode != null && !invocation.packageCode.equalsIgnoreCase(definition.getPackageCode())) {
                continue;
            }
            if (invocation.packageVersion != null && !invocation.packageVersion.equalsIgnoreCase(definition.getPackageVersion())) {
                continue;
            }
            if (!invocation.ruleCodeFilter.isEmpty() && !invocation.ruleCodeFilter.contains(definition.getRuleCode())) {
                continue;
            }
            if (!scenariosOf(definition).contains(invocation.scenarioCode)) {
                continue;
            }
            allCandidates.add(definition);
            if (matchesInvocationScope(definition, invocation)) {
                exactCandidates.add(definition);
            }
            if (isLegacyDefault(definition)) {
                legacyCandidates.add(definition);
            }
        }
        if (!exactCandidates.isEmpty()) {
            return exactCandidates;
        }
        if (!legacyCandidates.isEmpty()) {
            return legacyCandidates;
        }
        return hasOrgInvocation(invocation) ? Collections.<RuleDefinition>emptyList() : allCandidates;
    }

    private Set<String> scenariosOf(RuleDefinition definition) {
        Set<String> set = new LinkedHashSet<String>();
        Object declaredList = definition.getRuleJson().get("scenario_codes");
        if (declaredList instanceof Collection) {
            for (Object item : (Collection<?>) declaredList) {
                String value = upper(string(item, null));
                if (value != null) {
                    set.add(value);
                }
            }
        }
        String declaredSingle = upper(string(definition.getRuleJson().get("scenario_code"), null));
        if (declaredSingle != null) {
            set.add(declaredSingle);
        }
        if (set.isEmpty()) {
            Set<String> derived = DEFAULT_RULE_TYPE_SCENARIOS.get(upper(definition.getRuleType()));
            if (derived != null) {
                set.addAll(derived);
            }
        }
        return set;
    }

    private Map<String, Object> executeForScenario(RuleDefinition definition, String scenarioCode,
                                                    Map<String, Object> patientContext) {
        long start = System.currentTimeMillis();
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule_code", definition.getRuleCode());
        entry.put("rule_name", definition.getRuleName());
        entry.put("rule_type", definition.getRuleType());
        entry.put("scenario_code", scenarioCode);
        entry.put("package_code", definition.getPackageCode());
        entry.put("package_version", definition.getPackageVersion());
        entry.put("version_no", definition.getVersionNo());
        entry.put("tenant_id", definition.getTenantId());
        entry.put("hospital_code", definition.getHospitalCode());
        entry.put("scope_level", definition.getScopeLevel());
        entry.put("scope_code", definition.getScopeCode());

        RuleResult result = new RuleResult();
        result.setRuleCode(definition.getRuleCode());
        try {
            RuleDslEvaluator.EvaluationOutcome outcome = dslEvaluator.evaluate(definition.getRuleJson(), patientContext);
            result.setHit(outcome.isHit());
            result.setSeverity(outcome.isHit() ? severity(definition) : "INFO");
            result.setMessage(message(definition, outcome));
            result.setActions(actions(definition));
            result.setEvidence(new ArrayList<Map<String, Object>>(outcome.getEvidence()));
            long elapsed = System.currentTimeMillis() - start;
            persistenceService.saveRuleExecLog(result, definition.getVersionNo(), patientContext,
                    elapsed, "SUCCESS", null, null, orgFields(definition));
            recordExecLog(result, definition.getVersionNo(), patientContext, elapsed,
                    "SUCCESS", null, null, definition, null);

            entry.put("hit", result.isHit());
            entry.put("severity", result.getSeverity());
            entry.put("message", result.getMessage());
            entry.put("actions", result.getActions());
            entry.put("evidence", result.getEvidence());
            entry.put("missing_facts", new ArrayList<String>(outcome.getMissingFacts()));
            entry.put("elapsed_ms", elapsed);
            entry.put("result_status", "SUCCESS");
            return entry;
        } catch (RuntimeException ex) {
            long elapsed = System.currentTimeMillis() - start;
            result.setHit(false);
            result.setSeverity("ERROR");
            result.setMessage("规则执行异常：" + ex.getMessage());
            persistenceService.saveRuleExecLog(result, definition.getVersionNo(), patientContext,
                    elapsed, "ERROR", "RULE_EXEC_ERROR", ex.getMessage(), orgFields(definition));
            recordExecLog(result, definition.getVersionNo(), patientContext, elapsed, "ERROR",
                    "RULE_EXEC_ERROR", ex.getMessage(), definition, null);

            entry.put("hit", false);
            entry.put("severity", "ERROR");
            entry.put("message", result.getMessage());
            entry.put("actions", new ArrayList<String>());
            entry.put("evidence", new ArrayList<Map<String, Object>>());
            entry.put("missing_facts", new ArrayList<String>());
            entry.put("elapsed_ms", elapsed);
            entry.put("result_status", "ERROR");
            entry.put("error_code", "RULE_EXEC_ERROR");
            entry.put("error_message", ex.getMessage());
            return entry;
        }
    }

    @SuppressWarnings("unchecked")
    public RuleResult simulate(Map<String, Object> request) {
        return simulate(request, null);
    }

    @SuppressWarnings("unchecked")
    public RuleResult simulate(Map<String, Object> request, OrganizationContext orgContext) {
        Map<String, Object> patientContext = getPatientContext(request);
        Object inlineRule = request.get("rule");
        if (inlineRule instanceof Map) {
            RuleDefinition definition = toDefinition((Map<String, Object>) inlineRule, "SIMULATION");
            applyOrganization(definition, orgContext);
            return executeDefinition(definition, patientContext);
        }

        String ruleCode = string(request.get("rule_code"), null);
        String versionNo = string(request.get("version_no"), null);
        RuleDefinition definition = ruleCode == null ? firstPublishedRule(orgContext) : getRule(ruleCode, versionNo, orgContext);
        if (definition != null) {
            return executeDefinition(definition, patientContext);
        }
        long start = System.currentTimeMillis();
        RuleResult result = evaluateStemiCandidate(patientContext);
        recordExecLog(result, "BUILT_IN", patientContext, System.currentTimeMillis() - start,
                "SUCCESS", null, null, null, orgContext);
        return result;
    }

    public List<RuleExecLogEntry> listExecLogs(Map<String, String> filters) {
        String ruleCode = filterValue(filters, "ruleCode");
        String traceId = filterValue(filters, "traceId");
        String patientId = filterValue(filters, "patientId");
        String encounterId = filterValue(filters, "encounterId");
        String resultStatus = filterValue(filters, "resultStatus");
        String tenantId = filterValue(filters, "tenantId");
        String groupCode = filterValue(filters, "groupCode");
        String hospitalCode = filterValue(filters, "hospitalCode");
        String campusCode = filterValue(filters, "campusCode");
        String siteCode = filterValue(filters, "siteCode");
        String departmentCode = filterValue(filters, "departmentCode");
        String scopeLevel = upper(filterValue(filters, "scopeLevel"));
        String scopeCode = filterValue(filters, "scopeCode");
        Boolean hit = filterBoolean(filters, "hit");
        int limit = filterInt(filters, "limit", 100);
        if (limit <= 0 || limit > EXEC_LOG_RING_CAPACITY) {
            limit = EXEC_LOG_RING_CAPACITY;
        }

        List<RuleExecLogEntry> matched = new ArrayList<RuleExecLogEntry>();
        // ConcurrentLinkedDeque 的 descendingIterator 返回最新写入的元素，便于按时间倒序返回执行日志。
        java.util.Iterator<RuleExecLogEntry> iterator = execLogs.descendingIterator();
        while (iterator.hasNext() && matched.size() < limit) {
            RuleExecLogEntry entry = iterator.next();
            if (ruleCode != null && !ruleCode.equalsIgnoreCase(entry.getRuleCode())) {
                continue;
            }
            if (traceId != null && !traceId.equals(entry.getTraceId())) {
                continue;
            }
            if (patientId != null && !patientId.equals(entry.getPatientId())) {
                continue;
            }
            if (encounterId != null && !encounterId.equals(entry.getEncounterId())) {
                continue;
            }
            if (resultStatus != null && !resultStatus.equalsIgnoreCase(entry.getResultStatus())) {
                continue;
            }
            if (hit != null && hit.booleanValue() != entry.isHit()) {
                continue;
            }
            if (!matchesLogOrg(entry, tenantId, groupCode, hospitalCode, campusCode,
                    siteCode, departmentCode, scopeLevel, scopeCode)) {
                continue;
            }
            matched.add(entry);
        }
        return matched;
    }

    public Map<String, Object> summarizeExecLogs(Map<String, String> filters) {
        Map<String, String> effective = new LinkedHashMap<String, String>();
        if (filters != null) {
            effective.putAll(filters);
        }
        effective.put("limit", String.valueOf(Integer.MAX_VALUE));
        List<RuleExecLogEntry> entries = listExecLogs(effective);

        int total = entries.size();
        int totalHits = 0;
        long totalElapsed = 0;
        int errorCount = 0;
        Map<String, RuleAggregate> byRule = new LinkedHashMap<String, RuleAggregate>();
        Map<String, Integer> bySeverity = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byResultStatus = new LinkedHashMap<String, Integer>();

        for (RuleExecLogEntry entry : entries) {
            if (entry.isHit()) {
                totalHits++;
            }
            totalElapsed += entry.getElapsedMs();
            if ("ERROR".equalsIgnoreCase(entry.getResultStatus())) {
                errorCount++;
            }
            increment(bySeverity, entry.getSeverity());
            increment(byResultStatus, entry.getResultStatus());

            String ruleCode = entry.getRuleCode();
            if (ruleCode == null) {
                continue;
            }
            RuleAggregate agg = byRule.get(ruleCode);
            if (agg == null) {
                agg = new RuleAggregate(ruleCode);
                byRule.put(ruleCode, agg);
            }
            agg.total++;
            if (entry.isHit()) {
                agg.hits++;
            }
            if ("ERROR".equalsIgnoreCase(entry.getResultStatus())) {
                agg.errors++;
            }
            agg.totalElapsed += entry.getElapsedMs();
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total", total);
        summary.put("total_hits", totalHits);
        summary.put("hit_rate", total == 0 ? 0.0 : Math.round(totalHits * 10000.0 / total) / 100.0);
        summary.put("error_count", errorCount);
        summary.put("average_elapsed_ms", total == 0 ? 0.0 : Math.round(totalElapsed * 100.0 / total) / 100.0);
        summary.put("by_rule", aggregatesToList(byRule));
        summary.put("by_severity", bucketsToList(bySeverity, "severity"));
        summary.put("by_result_status", bucketsToList(byResultStatus, "result_status"));
        summary.put("by_hospital_code", aggregateExecLogs(entries, "hospital_code"));
        summary.put("by_scope", aggregateExecLogs(entries, "scope"));
        return summary;
    }

    private void increment(Map<String, Integer> counts, String key) {
        if (key == null) {
            return;
        }
        Integer value = counts.get(key);
        counts.put(key, value == null ? 1 : value + 1);
    }

    private List<Map<String, Object>> aggregatesToList(Map<String, RuleAggregate> aggregates) {
        List<RuleAggregate> list = new ArrayList<RuleAggregate>(aggregates.values());
        Collections.sort(list, new Comparator<RuleAggregate>() {
            @Override
            public int compare(RuleAggregate left, RuleAggregate right) {
                int byTotal = Integer.compare(right.total, left.total);
                return byTotal != 0 ? byTotal : left.ruleCode.compareTo(right.ruleCode);
            }
        });
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (RuleAggregate agg : list) {
            result.add(agg.toView());
        }
        return result;
    }

    private List<Map<String, Object>> bucketsToList(Map<String, Integer> counts, String key) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
                int byCount = right.getValue().compareTo(left.getValue());
                return byCount != 0 ? byCount : left.getKey().compareTo(right.getKey());
            }
        });
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : entries) {
            Map<String, Object> bucket = new LinkedHashMap<String, Object>();
            bucket.put(key, entry.getKey());
            bucket.put("count", entry.getValue());
            result.add(bucket);
        }
        return result;
    }

    private static class RuleAggregate {
        private final String ruleCode;
        private int total;
        private int hits;
        private int errors;
        private long totalElapsed;

        RuleAggregate(String ruleCode) {
            this.ruleCode = ruleCode;
        }

        Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("rule_code", ruleCode);
            view.put("total", total);
            view.put("hits", hits);
            view.put("errors", errors);
            view.put("hit_rate", total == 0 ? 0.0 : Math.round(hits * 10000.0 / total) / 100.0);
            view.put("average_elapsed_ms", total == 0 ? 0.0 : Math.round(totalElapsed * 100.0 / total) / 100.0);
            return view;
        }
    }

    public RuleExecLogEntry getExecLog(String logId) {
        if (logId == null) {
            throw new IllegalArgumentException("logId is required");
        }
        for (RuleExecLogEntry entry : execLogs) {
            if (logId.equals(entry.getLogId())) {
                return entry;
            }
        }
        throw new IllegalArgumentException("rule exec log not found: " + logId);
    }

    private List<RuleResult> evaluateBuiltInRules(Map<String, Object> patientContext, OrganizationContext orgContext) {
        List<RuleResult> results = new ArrayList<RuleResult>();
        long stemiStart = System.currentTimeMillis();
        RuleResult stemi = evaluateStemiCandidate(patientContext);
        recordExecLog(stemi, "BUILT_IN", patientContext, System.currentTimeMillis() - stemiStart,
                "SUCCESS", null, null, null, orgContext);
        results.add(stemi);

        long ecgStart = System.currentTimeMillis();
        RuleResult ecg = evaluateEcgTimely(patientContext);
        recordExecLog(ecg, "BUILT_IN", patientContext, System.currentTimeMillis() - ecgStart,
                "SUCCESS", null, null, null, orgContext);
        results.add(ecg);
        return results;
    }

    private RuleResult executeDefinition(RuleDefinition definition, Map<String, Object> patientContext) {
        long start = System.currentTimeMillis();
        RuleResult result = new RuleResult();
        result.setRuleCode(definition.getRuleCode());
        try {
            // 所有配置化规则统一通过DSL执行器处理，便于后续扩展更多操作符和审计信息。
            RuleDslEvaluator.EvaluationOutcome outcome = dslEvaluator.evaluate(definition.getRuleJson(), patientContext);
            result.setHit(outcome.isHit());
            result.setSeverity(outcome.isHit() ? severity(definition) : "INFO");
            result.setMessage(message(definition, outcome));
            result.setActions(actions(definition));
            result.setEvidence(outcome.getEvidence());
            if (!outcome.getMissingFacts().isEmpty()) {
                Map<String, Object> missing = new LinkedHashMap<String, Object>();
                missing.put("type", "missing_fact");
                missing.put("facts", outcome.getMissingFacts());
                result.getEvidence().add(missing);
            }
            long elapsed = System.currentTimeMillis() - start;
            persistenceService.saveRuleExecLog(result, definition.getVersionNo(), patientContext,
                    elapsed, "SUCCESS", null, null, orgFields(definition));
            recordExecLog(result, definition.getVersionNo(), patientContext, elapsed,
                    "SUCCESS", null, null, definition, null);
            return result;
        } catch (RuntimeException ex) {
            result.setHit(false);
            result.setSeverity("ERROR");
            result.setMessage("规则执行异常：" + ex.getMessage());
            long elapsed = System.currentTimeMillis() - start;
            persistenceService.saveRuleExecLog(result, definition.getVersionNo(), patientContext,
                    elapsed, "ERROR", "RULE_EXEC_ERROR", ex.getMessage(), orgFields(definition));
            recordExecLog(result, definition.getVersionNo(), patientContext, elapsed,
                    "ERROR", "RULE_EXEC_ERROR", ex.getMessage(), definition, null);
            throw ex;
        }
    }

    private void recordExecLog(RuleResult result, String ruleVersion, Map<String, Object> patientContext,
                               long elapsedMs, String resultStatus, String errorCode, String errorMessage,
                               RuleDefinition definition, OrganizationContext orgContext) {
        RuleExecLogEntry entry = new RuleExecLogEntry();
        entry.setLogId("rxl-" + execLogSequence.incrementAndGet());
        entry.setTraceId(TraceContext.getTraceId());
        entry.setRuleCode(result.getRuleCode());
        entry.setRuleVersion(ruleVersion);
        entry.setPatientId(ClinicalFactUtils.patientId(patientContext));
        entry.setEncounterId(ClinicalFactUtils.encounterId(patientContext));
        entry.setHit(result.isHit());
        entry.setSeverity(result.getSeverity());
        entry.setMessage(result.getMessage());
        entry.setElapsedMs(elapsedMs);
        entry.setResultStatus(resultStatus);
        entry.setErrorCode(errorCode);
        entry.setErrorMessage(errorMessage);
        entry.setActions(new ArrayList<String>(result.getActions()));
        entry.setEvidence(new ArrayList<Map<String, Object>>(result.getEvidence()));
        entry.setCreatedTime(nowText());
        applyOrganization(entry, definition, orgContext);

        // 内存环形缓冲优先保留最近 EXEC_LOG_RING_CAPACITY 条记录，长期归档仍依赖 RE_RULE_EXEC_LOG。
        execLogs.addLast(entry);
        while (execLogs.size() > EXEC_LOG_RING_CAPACITY) {
            execLogs.pollFirst();
        }
    }

    private void markPublished(RuleDefinition definition, String approvedBy) {
        definition.setStatus("PUBLISHED");
        definition.setPublishedBy(approvedBy);
        definition.setPublishedTime(nowText());
        definition.getRuleJson().put("status", "PUBLISHED");
        definition.getRuleJson().put("published_by", approvedBy);
        definition.getRuleJson().put("published_time", definition.getPublishedTime());
    }

    private Map<String, Object> packageReviewForAudit(String packageCode, String packageVersion, int ruleCount,
                                                      RuleDefinition definition) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("package_code", packageCode);
        detail.put("package_version", packageVersion);
        detail.put("rule_count", ruleCount);
        putOrgFields(detail, definition);
        return detail;
    }

    private void putOrgFields(Map<String, Object> target, RuleDefinition definition) {
        if (definition == null) {
            return;
        }
        target.put("tenant_id", definition.getTenantId());
        target.put("group_code", definition.getGroupCode());
        target.put("hospital_code", definition.getHospitalCode());
        target.put("campus_code", definition.getCampusCode());
        target.put("site_code", definition.getSiteCode());
        target.put("department_code", definition.getDepartmentCode());
        target.put("scope_level", definition.getScopeLevel());
        target.put("scope_code", definition.getScopeCode());
        target.put("org_source", definition.getOrgSource());
    }

    private Map<String, Object> orgFields(RuleDefinition definition) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        putOrgFields(fields, definition);
        return fields;
    }

    private void auditRuleChange(String actionType, String targetType, String targetCode,
                                 String operatorId, Map<String, Object> detail) {
        try {
            persistenceService.saveAuditLog("RULE", actionType, targetType, targetCode,
                    null, null, operatorId, detail);
        } catch (RuntimeException ignored) {
            // 规则发布主流程不因审计落库失败而中断。
        }
    }

    private RuleResult evaluateStemiCandidate(Map<String, Object> context) {
        boolean hit = ClinicalFactUtils.hasChestPain(context) && ClinicalFactUtils.hasStElevation(context);
        RuleResult result = new RuleResult();
        result.setRuleCode("R_AMI_STEMI_CANDIDATE");
        result.setHit(hit);
        result.setSeverity(hit ? "HIGH" : "INFO");
        result.setMessage(hit
                ? "疑似STEMI，请医生评估是否启动急性心肌梗死诊疗路径。"
                : "未命中STEMI候选入径规则。");
        if (hit) {
            result.setActions(Arrays.asList("CREATE_RECOMMENDATION", "PUSH_TO_DOCTOR"));
            result.getEvidence().add(evidence("chief_complaint", "胸痛相关主诉命中。"));
            result.getEvidence().add(evidence("exam_finding", "心电图ST段抬高检查发现命中。"));
        }
        return result;
    }

    private RuleResult evaluateEcgTimely(Map<String, Object> context) {
        RuleResult result = new RuleResult();
        result.setRuleCode("R_AMI_ECG_TIMELY");
        result.setHit(false);
        result.setSeverity("INFO");
        result.setMessage("样例患者心电图已完成，未触发心电图超时质控。");
        result.setActions(new ArrayList<String>());
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeRules(Object request) {
        List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();
        if (request instanceof List) {
            for (Object item : (List<?>) request) {
                if (item instanceof Map) {
                    rules.add((Map<String, Object>) item);
                }
            }
            return rules;
        }
        if (request instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) request;
            Object nested = map.get("rules");
            if (nested instanceof List) {
                List<Map<String, Object>> nestedRules = normalizeRules(nested);
                String packageCode = string(map.get("package_code"), string(map.get("packageCode"), null));
                String packageVersion = string(map.get("package_version"), string(map.get("packageVersion"), null));
                if (packageCode == null && packageVersion == null) {
                    return nestedRules;
                }
                List<Map<String, Object>> inherited = new ArrayList<Map<String, Object>>();
                for (Map<String, Object> rule : nestedRules) {
                    Map<String, Object> copy = new LinkedHashMap<String, Object>(rule);
                    if (packageCode != null && string(copy.get("package_code"), string(copy.get("packageCode"), null)) == null) {
                        copy.put("package_code", packageCode);
                    }
                    if (packageVersion != null
                            && string(copy.get("package_version"), string(copy.get("packageVersion"), null)) == null) {
                        copy.put("package_version", packageVersion);
                    }
                    inherited.add(copy);
                }
                return inherited;
            }
            rules.add(map);
        }
        return rules;
    }

    private RuleDefinition toDefinition(Map<String, Object> rule, String defaultStatus) {
        String ruleCode = string(rule.get("rule_code"), null);
        if (ruleCode == null) {
            throw new IllegalArgumentException("rule_code is required");
        }
        RuleDefinition definition = new RuleDefinition();
        definition.setRuleCode(ruleCode);
        definition.setRuleName(string(rule.get("rule_name"), ruleCode));
        definition.setRuleType(string(rule.get("rule_type"), "GENERAL"));
        definition.setVersionNo(string(rule.get("version_no"), "1.0.0"));
        definition.setPackageCode(string(rule.get("package_code"), string(rule.get("packageCode"), null)));
        definition.setPackageVersion(string(rule.get("package_version"), string(rule.get("packageVersion"), null)));
        definition.setStatus(string(rule.get("status"), defaultStatus));
        definition.setSeverity(string(rule.get("severity"), "HIGH"));
        definition.setEnabled(booleanValue(rule.get("enabled"), true));
        definition.setPublishedBy(string(rule.get("published_by"), string(rule.get("publishedBy"), null)));
        definition.setPublishedTime(string(rule.get("published_time"), string(rule.get("publishedTime"), null)));
        definition.setRuleJson(new LinkedHashMap<String, Object>(rule));
        return definition;
    }

    private List<RuleDefinition> publishedRules() {
        List<RuleDefinition> list = new ArrayList<RuleDefinition>();
        for (RuleDefinition definition : ruleStore.values()) {
            if (definition.isEnabled() && "PUBLISHED".equals(definition.getStatus())) {
                list.add(definition);
            }
        }
        sortRulesByPriority(list);
        return list;
    }

    private List<RuleDefinition> publishedRules(OrganizationContext orgContext) {
        List<RuleDefinition> list = new ArrayList<RuleDefinition>();
        for (RuleDefinition definition : ruleStore.values()) {
            if (definition.isEnabled() && "PUBLISHED".equals(definition.getStatus())
                    && matchesDefinitionContext(definition, orgContext)) {
                list.add(definition);
            }
        }
        if (list.isEmpty() && orgContext != null) {
            for (RuleDefinition definition : ruleStore.values()) {
                if (definition.isEnabled() && "PUBLISHED".equals(definition.getStatus())
                        && isLegacyDefault(definition)) {
                    list.add(definition);
                }
            }
        }
        sortRulesByPriority(list);
        return list;
    }

    private void sortRulesByPriority(List<RuleDefinition> list) {
        Collections.sort(list, new Comparator<RuleDefinition>() {
            @Override
            public int compare(RuleDefinition left, RuleDefinition right) {
                Integer leftPriority = integer(left.getRuleJson().get("priority"), 0);
                Integer rightPriority = integer(right.getRuleJson().get("priority"), 0);
                return rightPriority.compareTo(leftPriority);
            }
        });
    }

    private RuleDefinition firstPublishedRule() {
        return firstPublishedRule(null);
    }

    private RuleDefinition firstPublishedRule(OrganizationContext orgContext) {
        List<RuleDefinition> published = publishedRules(orgContext);
        return published.isEmpty() ? null : published.get(0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPatientContext(Map<String, Object> request) {
        Object value = request.get("patient_context");
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return request;
    }

    private String message(RuleDefinition definition, RuleDslEvaluator.EvaluationOutcome outcome) {
        if (outcome.isHit()) {
            return string(definition.getRuleJson().get("message_template"), definition.getRuleName() + "命中。");
        }
        if (!outcome.getMissingFacts().isEmpty()) {
            return "规则未命中，缺少数据：" + outcome.getMissingFacts();
        }
        return "规则未命中：" + definition.getRuleName();
    }

    @SuppressWarnings("unchecked")
    private List<String> actions(RuleDefinition definition) {
        List<String> list = new ArrayList<String>();
        Object actions = definition.getRuleJson().get("actions");
        if (actions instanceof Collection) {
            for (Object action : (Collection<?>) actions) {
                if (action instanceof Map) {
                    Object type = ((Map<String, Object>) action).get("type");
                    if (type != null) {
                        list.add(String.valueOf(type));
                    }
                } else if (action != null) {
                    list.add(String.valueOf(action));
                }
            }
        }
        return list;
    }

    private String severity(RuleDefinition definition) {
        String severity = definition.getSeverity();
        if (severity != null) {
            return severity;
        }
        String type = definition.getRuleType();
        if ("SAFETY_BLOCK".equals(type)) {
            return "CRITICAL";
        }
        if ("PATHWAY_ENTRY".equals(type)) {
            return "HIGH";
        }
        return "INFO";
    }

    private Map<String, Object> evidence(String type, String text) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        map.put("text", text);
        return map;
    }

    private List<RuleDefinition> filterRulesByOrg(List<RuleDefinition> rules, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return rules;
        }
        String tenantId = filterValue(filters, "tenantId");
        String groupCode = filterValue(filters, "groupCode");
        String hospitalCode = filterValue(filters, "hospitalCode");
        String campusCode = filterValue(filters, "campusCode");
        String siteCode = filterValue(filters, "siteCode");
        String departmentCode = filterValue(filters, "departmentCode");
        String scopeLevel = upper(filterValue(filters, "scopeLevel"));
        String scopeCode = filterValue(filters, "scopeCode");
        List<RuleDefinition> matched = new ArrayList<RuleDefinition>();
        for (RuleDefinition definition : rules) {
            if (matchesRuleOrg(definition, tenantId, groupCode, hospitalCode, campusCode,
                    siteCode, departmentCode, scopeLevel, scopeCode)) {
                matched.add(definition);
            }
        }
        return matched;
    }

    private void applyOrganization(RuleDefinition definition, OrganizationContext orgContext) {
        if (orgContext != null) {
            definition.setTenantId(orgContext.getTenantId());
            definition.setGroupCode(orgContext.getGroupCode());
            definition.setHospitalCode(orgContext.getHospitalCode());
            definition.setCampusCode(orgContext.getCampusCode());
            definition.setSiteCode(orgContext.getSiteCode());
            definition.setDepartmentCode(orgContext.getDepartmentCode());
            definition.setLegacyOrgCode(orgContext.getLegacyOrgCode());
            definition.setScopeLevel(orgContext.getEffectiveScopeLevel());
            definition.setScopeCode(orgContext.getEffectiveScopeCode());
            definition.setOrgSource(orgContext.getSource());
        } else {
            definition.setTenantId(ruleJsonString(definition, "tenant_id", "tenantId", DEFAULT_TENANT_ID));
            definition.setGroupCode(ruleJsonString(definition, "group_code", "groupCode", null));
            String hospitalCode = ruleJsonString(definition, "hospital_code", "hospitalCode", null);
            String legacyOrgCode = ruleJsonString(definition, "org_code", "orgCode", null);
            if (hospitalCode == null && legacyOrgCode != null) {
                hospitalCode = legacyOrgCode;
            }
            if (hospitalCode == null) {
                hospitalCode = DEFAULT_HOSPITAL_CODE;
            }
            if (legacyOrgCode == null) {
                legacyOrgCode = hospitalCode;
            }
            definition.setHospitalCode(hospitalCode);
            definition.setLegacyOrgCode(legacyOrgCode);
            definition.setCampusCode(ruleJsonString(definition, "campus_code", "campusCode", null));
            definition.setSiteCode(ruleJsonString(definition, "site_code", "siteCode", null));
            definition.setDepartmentCode(ruleJsonString(definition, "department_code", "departmentCode", null));
            applyEffectiveScope(definition);
            definition.setOrgSource(hasRuleJsonOrg(definition) ? "BODY" : "DEFAULT");
        }
        syncOrgToRuleJson(definition);
    }

    private void applyEffectiveScope(RuleDefinition definition) {
        if (present(definition.getDepartmentCode())) {
            definition.setScopeLevel("DEPARTMENT");
            definition.setScopeCode(definition.getDepartmentCode());
            return;
        }
        if (present(definition.getSiteCode())) {
            definition.setScopeLevel("SITE");
            definition.setScopeCode(definition.getSiteCode());
            return;
        }
        if (present(definition.getCampusCode())) {
            definition.setScopeLevel("CAMPUS");
            definition.setScopeCode(definition.getCampusCode());
            return;
        }
        if (present(definition.getHospitalCode())) {
            definition.setScopeLevel("HOSPITAL");
            definition.setScopeCode(definition.getHospitalCode());
            return;
        }
        if (present(definition.getGroupCode())) {
            definition.setScopeLevel("GROUP");
            definition.setScopeCode(definition.getGroupCode());
            return;
        }
        definition.setScopeLevel("PLATFORM");
        definition.setScopeCode("DEFAULT");
    }

    private void syncOrgToRuleJson(RuleDefinition definition) {
        definition.getRuleJson().put("tenant_id", definition.getTenantId());
        definition.getRuleJson().put("group_code", definition.getGroupCode());
        definition.getRuleJson().put("hospital_code", definition.getHospitalCode());
        definition.getRuleJson().put("campus_code", definition.getCampusCode());
        definition.getRuleJson().put("site_code", definition.getSiteCode());
        definition.getRuleJson().put("department_code", definition.getDepartmentCode());
        definition.getRuleJson().put("scope_level", definition.getScopeLevel());
        definition.getRuleJson().put("scope_code", definition.getScopeCode());
        definition.getRuleJson().put("org_source", definition.getOrgSource());
    }

    private void applyOrganization(RuleExecLogEntry entry, RuleDefinition definition,
                                   OrganizationContext orgContext) {
        if (definition != null) {
            entry.setTenantId(definition.getTenantId());
            entry.setGroupCode(definition.getGroupCode());
            entry.setHospitalCode(definition.getHospitalCode());
            entry.setCampusCode(definition.getCampusCode());
            entry.setSiteCode(definition.getSiteCode());
            entry.setDepartmentCode(definition.getDepartmentCode());
            entry.setScopeLevel(definition.getScopeLevel());
            entry.setScopeCode(definition.getScopeCode());
            entry.setOrgSource(definition.getOrgSource());
            return;
        }
        if (orgContext != null) {
            entry.setTenantId(orgContext.getTenantId());
            entry.setGroupCode(orgContext.getGroupCode());
            entry.setHospitalCode(orgContext.getHospitalCode());
            entry.setCampusCode(orgContext.getCampusCode());
            entry.setSiteCode(orgContext.getSiteCode());
            entry.setDepartmentCode(orgContext.getDepartmentCode());
            entry.setScopeLevel(orgContext.getEffectiveScopeLevel());
            entry.setScopeCode(orgContext.getEffectiveScopeCode());
            entry.setOrgSource(orgContext.getSource());
            return;
        }
        entry.setTenantId(DEFAULT_TENANT_ID);
        entry.setHospitalCode(DEFAULT_HOSPITAL_CODE);
        entry.setScopeLevel(DEFAULT_SCOPE_LEVEL);
        entry.setScopeCode(DEFAULT_HOSPITAL_CODE);
        entry.setOrgSource("DEFAULT");
    }

    private boolean matchesDefinitionContext(RuleDefinition definition, OrganizationContext orgContext) {
        if (orgContext == null) {
            return isLegacyDefault(definition);
        }
        return matches(orgContext.getTenantId(), definition.getTenantId(), false)
                && matches(orgContext.getEffectiveScopeLevel(), definition.getScopeLevel(), true)
                && matches(orgContext.getEffectiveScopeCode(), definition.getScopeCode(), false);
    }

    private boolean matchesInvocationScope(RuleDefinition definition, ScenarioInvocation invocation) {
        return matches(invocation.tenantId, definition.getTenantId(), false)
                && matches(invocation.scopeLevel, definition.getScopeLevel(), true)
                && matches(invocation.scopeCode, definition.getScopeCode(), false);
    }

    private boolean matchesRuleOrg(RuleDefinition definition, String tenantId, String groupCode,
                                   String hospitalCode, String campusCode, String siteCode,
                                   String departmentCode, String scopeLevel, String scopeCode) {
        return matches(tenantId, definition.getTenantId(), false)
                && matches(groupCode, definition.getGroupCode(), false)
                && matches(hospitalCode, definition.getHospitalCode(), false)
                && matches(campusCode, definition.getCampusCode(), false)
                && matches(siteCode, definition.getSiteCode(), false)
                && matches(departmentCode, definition.getDepartmentCode(), false)
                && matches(scopeLevel, definition.getScopeLevel(), true)
                && matches(scopeCode, definition.getScopeCode(), false);
    }

    private boolean matchesLogOrg(RuleExecLogEntry entry, String tenantId, String groupCode,
                                  String hospitalCode, String campusCode, String siteCode,
                                  String departmentCode, String scopeLevel, String scopeCode) {
        return matches(tenantId, entry.getTenantId(), false)
                && matches(groupCode, entry.getGroupCode(), false)
                && matches(hospitalCode, entry.getHospitalCode(), false)
                && matches(campusCode, entry.getCampusCode(), false)
                && matches(siteCode, entry.getSiteCode(), false)
                && matches(departmentCode, entry.getDepartmentCode(), false)
                && matches(scopeLevel, entry.getScopeLevel(), true)
                && matches(scopeCode, entry.getScopeCode(), false);
    }

    private boolean matches(String expected, String actual, boolean ignoreCase) {
        if (expected == null) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return ignoreCase ? expected.equalsIgnoreCase(actual) : expected.equals(actual);
    }

    private boolean isLegacyDefault(RuleDefinition definition) {
        return DEFAULT_TENANT_ID.equals(definition.getTenantId())
                && DEFAULT_SCOPE_LEVEL.equalsIgnoreCase(string(definition.getScopeLevel(), null))
                && DEFAULT_HOSPITAL_CODE.equals(definition.getScopeCode());
    }

    private boolean hasOrgInvocation(ScenarioInvocation invocation) {
        return invocation != null && (present(invocation.tenantId)
                || present(invocation.scopeLevel) || present(invocation.scopeCode));
    }

    private String key(RuleDefinition definition) {
        return string(definition.getTenantId(), DEFAULT_TENANT_ID) + "::"
                + string(definition.getScopeLevel(), DEFAULT_SCOPE_LEVEL) + "::"
                + string(definition.getScopeCode(), DEFAULT_HOSPITAL_CODE) + "::"
                + definition.getRuleCode() + "::" + definition.getVersionNo();
    }

    private String key(OrganizationContext orgContext, String ruleCode, String versionNo) {
        if (orgContext == null) {
            return legacyKey(ruleCode, versionNo);
        }
        return string(orgContext.getTenantId(), DEFAULT_TENANT_ID) + "::"
                + string(orgContext.getEffectiveScopeLevel(), DEFAULT_SCOPE_LEVEL) + "::"
                + string(orgContext.getEffectiveScopeCode(), DEFAULT_HOSPITAL_CODE) + "::"
                + ruleCode + "::" + versionNo;
    }

    private String legacyKey(String ruleCode, String versionNo) {
        return DEFAULT_TENANT_ID + "::" + DEFAULT_SCOPE_LEVEL + "::"
                + DEFAULT_HOSPITAL_CODE + "::" + ruleCode + "::" + versionNo;
    }

    private List<Map<String, Object>> aggregateExecLogs(List<RuleExecLogEntry> entries, String dimension) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (RuleExecLogEntry entry : entries) {
            String key = execLogDimension(entry, dimension);
            if (key == null) {
                continue;
            }
            Integer value = counts.get(key);
            counts.put(key, value == null ? 1 : value + 1);
        }
        return bucketsToList(counts, dimension);
    }

    private String execLogDimension(RuleExecLogEntry entry, String dimension) {
        if ("hospital_code".equals(dimension)) {
            return entry.getHospitalCode();
        }
        if ("scope".equals(dimension)) {
            return string(entry.getScopeLevel(), "UNKNOWN") + ":" + string(entry.getScopeCode(), "UNKNOWN");
        }
        return null;
    }

    private String ruleJsonString(RuleDefinition definition, String snakeKey, String camelKey, String defaultValue) {
        String value = string(definition.getRuleJson().get(snakeKey), null);
        if (value == null) {
            value = string(definition.getRuleJson().get(camelKey), null);
        }
        return value == null ? defaultValue : value;
    }

    private boolean hasRuleJsonOrg(RuleDefinition definition) {
        return ruleJsonString(definition, "tenant_id", "tenantId", null) != null
                || ruleJsonString(definition, "group_code", "groupCode", null) != null
                || ruleJsonString(definition, "hospital_code", "hospitalCode", null) != null
                || ruleJsonString(definition, "campus_code", "campusCode", null) != null
                || ruleJsonString(definition, "site_code", "siteCode", null) != null
                || ruleJsonString(definition, "department_code", "departmentCode", null) != null
                || ruleJsonString(definition, "org_code", "orgCode", null) != null;
    }

    private boolean present(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    private Boolean booleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private Integer integer(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String filterValue(Map<String, String> filters, String key) {
        if (filters == null) {
            return null;
        }
        String value = filters.get(key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private Boolean filterBoolean(Map<String, String> filters, String key) {
        String value = filterValue(filters, key);
        if (value == null) {
            return null;
        }
        return Boolean.valueOf(value);
    }

    private int filterInt(Map<String, String> filters, String key, int defaultValue) {
        String value = filterValue(filters, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private List<String> stringList(Object value) {
        List<String> list = new ArrayList<String>();
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                String text = string(item, null);
                if (text != null) {
                    list.add(text);
                }
            }
        }
        return list;
    }

    private static Map<String, Set<String>> defaultRuleTypeScenarios() {
        Map<String, Set<String>> map = new LinkedHashMap<String, Set<String>>();
        map.put("PATHWAY_ENTRY", new LinkedHashSet<String>(Arrays.asList("PATHWAY_ENTRY")));
        map.put("TIME_LIMIT_QC", new LinkedHashSet<String>(Arrays.asList("EMR_QC")));
        map.put("SAFETY_BLOCK", new LinkedHashSet<String>(Arrays.asList("ORDER_SAFETY")));
        map.put("EMR_QC", new LinkedHashSet<String>(Arrays.asList("EMR_QC")));
        map.put("INSURANCE_QC", new LinkedHashSet<String>(Arrays.asList("INSURANCE_QC")));
        map.put("ORDER_SAFETY", new LinkedHashSet<String>(Arrays.asList("ORDER_SAFETY")));
        map.put("DRUG_INDICATION", new LinkedHashSet<String>(Arrays.asList("DRUG_INDICATION")));
        map.put("EXAM_RATIONALITY", new LinkedHashSet<String>(Arrays.asList("EXAM_RATIONALITY")));
        return map;
    }
}
