package com.medkernel.engine.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;

@Component
public class RuleDslEvaluator {

    private final ObjectMapper json;

    public RuleDslEvaluator(ObjectMapper json) {
        this.json = json;
    }

    public RuleDslEvaluation evaluate(JsonNode dsl, JsonNode context) {
        if (dsl == null || !dsl.isObject()) {
            throw invalid("规则 DSL 必须是 JSON 对象");
        }
        JsonNode when = dsl.get("when");
        if (when == null || !when.isObject()) {
            throw invalid("规则 DSL 缺少 when 条件");
        }
        boolean hit = evaluateConditionNode(when, context == null ? json.createObjectNode() : context);
        if (!hit) {
            return new RuleDslEvaluation(false, null, List.of(), dsl.path("explain"));
        }

        List<RuleActionResult> actions = parseActions(dsl.path("then"));
        RuleRiskLevel highest = actions.stream()
            .map(RuleActionResult::severity)
            .reduce(null, RuleRiskLevel::max);
        return new RuleDslEvaluation(true, highest, actions, dsl.path("explain"));
    }

    private boolean evaluateConditionNode(JsonNode node, JsonNode context) {
        JsonNode all = node.get("all");
        if (all != null) {
            if (!all.isArray()) {
                throw invalid("when.all 必须是数组");
            }
            for (JsonNode child : all) {
                if (!evaluateConditionNode(child, context)) {
                    return false;
                }
            }
            return true;
        }

        JsonNode any = node.get("any");
        if (any != null) {
            if (!any.isArray()) {
                throw invalid("when.any 必须是数组");
            }
            for (JsonNode child : any) {
                if (evaluateConditionNode(child, context)) {
                    return true;
                }
            }
            return false;
        }

        return evaluateLeaf(node, context);
    }

    private boolean evaluateLeaf(JsonNode node, JsonNode context) {
        String fact = requiredText(node, "fact");
        String operator = requiredText(node, "operator").toLowerCase(Locale.ROOT);
        JsonNode actual = findPath(context, fact);
        JsonNode expected = node.get("value");

        return switch (operator) {
            case "exists" -> exists(actual);
            case "equals" -> exists(actual) && valuesEqual(actual, expected);
            case "not_equals" -> !exists(actual) || !valuesEqual(actual, expected);
            case "contains" -> contains(actual, expected);
            case "gt" -> compare(actual, expected) > 0;
            case "gte" -> compare(actual, expected) >= 0;
            case "lt" -> compare(actual, expected) < 0;
            case "lte" -> compare(actual, expected) <= 0;
            case "in" -> in(actual, expected);
            case "not_in" -> !in(actual, expected);
            default -> throw invalid("不支持的规则算子: " + operator);
        };
    }

    private List<RuleActionResult> parseActions(JsonNode then) {
        if (then == null || then.isMissingNode()) {
            return List.of();
        }
        if (!then.isArray()) {
            throw invalid("then 必须是数组");
        }
        List<RuleActionResult> actions = new ArrayList<>();
        for (JsonNode action : then) {
            String actionCode = requiredText(action, "actionCode");
            RuleRiskLevel severity = parseSeverity(action.path("severity").asText(null));
            boolean requires = action.path("requiresPhysicianConfirmation").asBoolean(false)
                || requiresConfirmation(actionCode, severity);
            actions.add(new RuleActionResult(
                actionCode, severity, action.path("message").asText(null), requires));
        }
        return actions;
    }

    private JsonNode findPath(JsonNode source, String path) {
        JsonNode current = source;
        for (String segment : path.split("\\.")) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return json.missingNode();
            }
            current = current.path(segment);
        }
        return current == null ? json.missingNode() : current;
    }

    private boolean exists(JsonNode actual) {
        if (actual == null || actual.isMissingNode() || actual.isNull()) {
            return false;
        }
        if (actual.isTextual()) {
            return !actual.asText().isBlank();
        }
        if (actual.isArray() || actual.isObject()) {
            return actual.size() > 0;
        }
        return true;
    }

    private boolean valuesEqual(JsonNode actual, JsonNode expected) {
        if (expected == null || expected.isMissingNode()) {
            return !exists(actual);
        }
        if (actual.isNumber() && expected.isNumber()) {
            return actual.decimalValue().compareTo(expected.decimalValue()) == 0;
        }
        if (actual.isBoolean() || expected.isBoolean()) {
            return actual.asBoolean() == expected.asBoolean();
        }
        if (actual.isTextual() || expected.isTextual()) {
            return actual.asText().equals(expected.asText());
        }
        return actual.equals(expected);
    }

    private boolean contains(JsonNode actual, JsonNode expected) {
        if (!exists(actual) || expected == null || expected.isMissingNode()) {
            return false;
        }
        if (actual.isArray()) {
            Iterator<JsonNode> values = actual.elements();
            while (values.hasNext()) {
                if (valuesEqual(values.next(), expected)) {
                    return true;
                }
            }
            return false;
        }
        return actual.isTextual() && actual.asText().contains(expected.asText());
    }

    private boolean in(JsonNode actual, JsonNode expected) {
        if (!exists(actual) || expected == null || !expected.isArray()) {
            return false;
        }
        for (JsonNode item : expected) {
            if (valuesEqual(actual, item)) {
                return true;
            }
        }
        return false;
    }

    private int compare(JsonNode actual, JsonNode expected) {
        if (!exists(actual) || expected == null || !actual.isNumber() || !expected.isNumber()) {
            return -1;
        }
        BigDecimal left = actual.decimalValue();
        BigDecimal right = expected.decimalValue();
        return left.compareTo(right);
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw invalid("规则 DSL 缺少字段: " + field);
        }
        return value;
    }

    private RuleRiskLevel parseSeverity(String value) {
        if (value == null || value.isBlank()) {
            return RuleRiskLevel.LOW;
        }
        try {
            return RuleRiskLevel.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalid("规则风险级别无效: " + value);
        }
    }

    private boolean requiresConfirmation(String actionCode, RuleRiskLevel severity) {
        return severity == RuleRiskLevel.HIGH
            || severity == RuleRiskLevel.CRITICAL
            || "BLOCK".equals(actionCode)
            || "STRONG_REMINDER".equals(actionCode)
            || "RECOMMEND_NEXT".equals(actionCode);
    }

    private ApiException invalid(String message) {
        return new ApiException(ErrorCode.ENG_RULE_001, message);
    }
}
