package com.zyengine.rule;

import com.zyengine.common.TraceContext;
import com.zyengine.dto.RuleResult;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RuleService {
    private static final int EXEC_LOG_RING_CAPACITY = 500;

    private final EnginePersistenceService persistenceService;
    private final RuleDslEvaluator dslEvaluator = new RuleDslEvaluator();
    private final Map<String, RuleDefinition> ruleStore = new ConcurrentHashMap<String, RuleDefinition>();
    private final Deque<RuleExecLogEntry> execLogs = new ConcurrentLinkedDeque<RuleExecLogEntry>();
    private final AtomicLong execLogSequence = new AtomicLong();

    public RuleService(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public List<RuleDefinition> importRules(Object request) {
        List<Map<String, Object>> rules = normalizeRules(request);
        List<RuleDefinition> imported = new ArrayList<RuleDefinition>();
        for (Map<String, Object> rule : rules) {
            RuleDefinition definition = toDefinition(rule, "DRAFT");
            ruleStore.put(key(definition.getRuleCode(), definition.getVersionNo()), definition);
            persistenceService.saveRuleDefinition(definition, null);
            imported.add(definition);
        }
        return imported;
    }

    public RuleDefinition publish(String ruleCode, Map<String, Object> request) {
        String versionNo = string(request.get("version_no"), "1.0.0");
        RuleDefinition definition = ruleStore.get(key(ruleCode, versionNo));
        if (definition == null) {
            throw new IllegalArgumentException("rule not found: " + ruleCode + "@" + versionNo);
        }
        String approvedBy = string(request.get("approved_by"), null);
        markPublished(definition, approvedBy);
        persistenceService.saveRuleDefinition(definition, approvedBy);
        auditRuleChange("PUBLISH", "RULE", definition.getRuleCode(), approvedBy,
                packageReviewForAudit(definition.getPackageCode(), definition.getPackageVersion(), 1));
        return definition;
    }

    public Map<String, Object> reviewPackage(String packageCode, String packageVersion) {
        List<RuleDefinition> rules = rulesInPackage(packageCode, packageVersion);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("rule package not found: " + packageCode);
        }
        return buildPackageReview(packageCode, packageVersion, rules);
    }

    public Map<String, Object> publishPackage(String packageCode, Map<String, Object> request) {
        String packageVersion = string(request == null ? null : request.get("package_version"), null);
        String approvedBy = string(request == null ? null : request.get("approved_by"), null);
        List<RuleDefinition> rules = rulesInPackage(packageCode, packageVersion);
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
        List<RuleDefinition> list = new ArrayList<RuleDefinition>(ruleStore.values());
        Collections.sort(list, new Comparator<RuleDefinition>() {
            @Override
            public int compare(RuleDefinition left, RuleDefinition right) {
                return left.getRuleCode().compareTo(right.getRuleCode());
            }
        });
        return list;
    }

    private List<RuleDefinition> rulesInPackage(String packageCode, String packageVersion) {
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
        if (versionNo != null && !versionNo.trim().isEmpty()) {
            return ruleStore.get(key(ruleCode, versionNo));
        }
        RuleDefinition latest = null;
        for (RuleDefinition definition : ruleStore.values()) {
            if (ruleCode.equals(definition.getRuleCode())) {
                latest = definition;
            }
        }
        return latest;
    }

    public List<RuleResult> evaluate(Map<String, Object> request) {
        Map<String, Object> patientContext = getPatientContext(request);
        List<RuleDefinition> published = publishedRules();
        if (published.isEmpty()) {
            // 未导入规则时保留内置AMI规则，保证旧演示和健康验证不被配置化改造打断。
            return evaluateBuiltInRules(patientContext);
        }

        List<RuleResult> results = new ArrayList<RuleResult>();
        for (RuleDefinition definition : published) {
            results.add(executeDefinition(definition, patientContext));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public RuleResult simulate(Map<String, Object> request) {
        Map<String, Object> patientContext = getPatientContext(request);
        Object inlineRule = request.get("rule");
        if (inlineRule instanceof Map) {
            RuleDefinition definition = toDefinition((Map<String, Object>) inlineRule, "SIMULATION");
            return executeDefinition(definition, patientContext);
        }

        String ruleCode = string(request.get("rule_code"), null);
        String versionNo = string(request.get("version_no"), null);
        RuleDefinition definition = ruleCode == null ? firstPublishedRule() : getRule(ruleCode, versionNo);
        if (definition != null) {
            return executeDefinition(definition, patientContext);
        }
        long start = System.currentTimeMillis();
        RuleResult result = evaluateStemiCandidate(patientContext);
        recordExecLog(result, "BUILT_IN", patientContext, System.currentTimeMillis() - start, "SUCCESS", null, null);
        return result;
    }

    public List<RuleExecLogEntry> listExecLogs(Map<String, String> filters) {
        String ruleCode = filterValue(filters, "ruleCode");
        String traceId = filterValue(filters, "traceId");
        String patientId = filterValue(filters, "patientId");
        String encounterId = filterValue(filters, "encounterId");
        String resultStatus = filterValue(filters, "resultStatus");
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

    private List<RuleResult> evaluateBuiltInRules(Map<String, Object> patientContext) {
        List<RuleResult> results = new ArrayList<RuleResult>();
        long stemiStart = System.currentTimeMillis();
        RuleResult stemi = evaluateStemiCandidate(patientContext);
        recordExecLog(stemi, "BUILT_IN", patientContext, System.currentTimeMillis() - stemiStart, "SUCCESS", null, null);
        results.add(stemi);

        long ecgStart = System.currentTimeMillis();
        RuleResult ecg = evaluateEcgTimely(patientContext);
        recordExecLog(ecg, "BUILT_IN", patientContext, System.currentTimeMillis() - ecgStart, "SUCCESS", null, null);
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
                    elapsed, "SUCCESS", null, null);
            recordExecLog(result, definition.getVersionNo(), patientContext, elapsed, "SUCCESS", null, null);
            return result;
        } catch (RuntimeException ex) {
            result.setHit(false);
            result.setSeverity("ERROR");
            result.setMessage("规则执行异常：" + ex.getMessage());
            long elapsed = System.currentTimeMillis() - start;
            persistenceService.saveRuleExecLog(result, definition.getVersionNo(), patientContext,
                    elapsed, "ERROR", "RULE_EXEC_ERROR", ex.getMessage());
            recordExecLog(result, definition.getVersionNo(), patientContext, elapsed, "ERROR", "RULE_EXEC_ERROR", ex.getMessage());
            throw ex;
        }
    }

    private void recordExecLog(RuleResult result, String ruleVersion, Map<String, Object> patientContext,
                               long elapsedMs, String resultStatus, String errorCode, String errorMessage) {
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

    private Map<String, Object> packageReviewForAudit(String packageCode, String packageVersion, int ruleCount) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("package_code", packageCode);
        detail.put("package_version", packageVersion);
        detail.put("rule_count", ruleCount);
        return detail;
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
        Collections.sort(list, new Comparator<RuleDefinition>() {
            @Override
            public int compare(RuleDefinition left, RuleDefinition right) {
                Integer leftPriority = integer(left.getRuleJson().get("priority"), 0);
                Integer rightPriority = integer(right.getRuleJson().get("priority"), 0);
                return rightPriority.compareTo(leftPriority);
            }
        });
        return list;
    }

    private RuleDefinition firstPublishedRule() {
        List<RuleDefinition> published = publishedRules();
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

    private String key(String ruleCode, String versionNo) {
        return ruleCode + "::" + versionNo;
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
}
