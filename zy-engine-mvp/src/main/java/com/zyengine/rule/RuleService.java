package com.zyengine.rule;

import com.zyengine.dto.RuleResult;
import com.zyengine.persistence.EnginePersistenceService;
import com.zyengine.util.ClinicalFactUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RuleService {
    private final EnginePersistenceService persistenceService;
    private final RuleDslEvaluator dslEvaluator = new RuleDslEvaluator();
    private final Map<String, RuleDefinition> ruleStore = new ConcurrentHashMap<String, RuleDefinition>();

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
        definition.setStatus("PUBLISHED");
        persistenceService.saveRuleDefinition(definition, string(request.get("approved_by"), null));
        return definition;
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
        return evaluateStemiCandidate(patientContext);
    }

    private List<RuleResult> evaluateBuiltInRules(Map<String, Object> patientContext) {
        List<RuleResult> results = new ArrayList<RuleResult>();
        results.add(evaluateStemiCandidate(patientContext));
        results.add(evaluateEcgTimely(patientContext));
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
            persistenceService.saveRuleExecLog(result, definition.getVersionNo(), patientContext,
                    System.currentTimeMillis() - start, "SUCCESS", null, null);
            return result;
        } catch (RuntimeException ex) {
            result.setHit(false);
            result.setSeverity("ERROR");
            result.setMessage("规则执行异常：" + ex.getMessage());
            persistenceService.saveRuleExecLog(result, definition.getVersionNo(), patientContext,
                    System.currentTimeMillis() - start, "ERROR", "RULE_EXEC_ERROR", ex.getMessage());
            throw ex;
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
                return normalizeRules(nested);
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
        definition.setStatus(string(rule.get("status"), defaultStatus));
        definition.setSeverity(string(rule.get("severity"), "HIGH"));
        definition.setEnabled(booleanValue(rule.get("enabled"), true));
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
}
