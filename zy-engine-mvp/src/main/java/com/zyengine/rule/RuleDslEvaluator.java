package com.zyengine.rule;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuleDslEvaluator {
    public EvaluationOutcome evaluate(Map<String, Object> rule, Map<String, Object> patientContext) {
        EvaluationOutcome outcome = new EvaluationOutcome();
        Object condition = rule.get("condition");
        outcome.hit = evaluateNode(condition, patientContext, outcome);
        return outcome;
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateNode(Object node, Map<String, Object> patientContext, EvaluationOutcome outcome) {
        if (!(node instanceof Map)) {
            outcome.missingFacts.add("condition");
            return false;
        }
        Map<String, Object> condition = (Map<String, Object>) node;
        Object all = condition.get("all");
        if (all instanceof Collection) {
            for (Object child : (Collection<?>) all) {
                if (!evaluateNode(child, patientContext, outcome)) {
                    return false;
                }
            }
            return true;
        }
        Object any = condition.get("any");
        if (any instanceof Collection) {
            boolean matched = false;
            for (Object child : (Collection<?>) any) {
                matched = evaluateNode(child, patientContext, outcome) || matched;
            }
            return matched;
        }
        return evaluateAtom(condition, patientContext, outcome);
    }

    private boolean evaluateAtom(Map<String, Object> atom, Map<String, Object> patientContext, EvaluationOutcome outcome) {
        String fact = string(atom.get("fact"));
        String operator = string(atom.get("operator"));
        Object expected = atom.get("value");
        if (isBlank(fact) || isBlank(operator)) {
            outcome.missingFacts.add("fact/operator");
            return false;
        }

        List<Object> actualValues = valuesForPath(patientContext, fact);
        if ("within_minutes_from".equals(operator)) {
            return withinMinutesFrom(actualValues, expected, patientContext, fact, outcome);
        }
        if (actualValues.isEmpty()) {
            outcome.missingFacts.add(fact);
            return false;
        }
        if ("exists".equals(operator)) {
            return true;
        }
        if ("equals".equals(operator)) {
            for (Object actual : actualValues) {
                if (same(actual, expected)) {
                    addEvidence(outcome, fact, operator, actual);
                    return true;
                }
            }
            return false;
        }
        if ("in".equals(operator)) {
            Collection<?> expectedValues = expected instanceof Collection ? (Collection<?>) expected : null;
            if (expectedValues == null) {
                return false;
            }
            for (Object actual : actualValues) {
                for (Object item : expectedValues) {
                    if (same(actual, item)) {
                        addEvidence(outcome, fact, operator, actual);
                        return true;
                    }
                }
            }
            return false;
        }
        if ("contains".equals(operator)) {
            for (Object actual : actualValues) {
                if (actual instanceof Collection) {
                    for (Object item : (Collection<?>) actual) {
                        if (same(item, expected)) {
                            addEvidence(outcome, fact, operator, item);
                            return true;
                        }
                    }
                } else if (actual != null && expected != null && String.valueOf(actual).contains(String.valueOf(expected))) {
                    addEvidence(outcome, fact, operator, actual);
                    return true;
                } else if (same(actual, expected)) {
                    addEvidence(outcome, fact, operator, actual);
                    return true;
                }
            }
            return false;
        }
        outcome.missingFacts.add("unsupported_operator:" + operator);
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean withinMinutesFrom(List<Object> actualValues, Object expected, Map<String, Object> patientContext,
                                      String fact, EvaluationOutcome outcome) {
        if (actualValues.isEmpty()) {
            outcome.missingFacts.add(fact);
            return false;
        }
        if (!(expected instanceof Map)) {
            outcome.missingFacts.add(fact + ".window");
            return false;
        }
        Map<String, Object> window = (Map<String, Object>) expected;
        String fromPath = string(window.get("from"));
        Integer minutes = integer(window.get("minutes"));
        if (isBlank(fromPath) || minutes == null) {
            outcome.missingFacts.add(fact + ".window");
            return false;
        }
        List<Object> fromValues = valuesForPath(patientContext, fromPath);
        if (fromValues.isEmpty()) {
            outcome.missingFacts.add(fromPath);
            return false;
        }
        Temporal start = parseTime(string(fromValues.get(0)));
        Temporal end = parseTime(string(actualValues.get(0)));
        if (start == null || end == null) {
            outcome.missingFacts.add(fact + ".time_format");
            return false;
        }
        long diff = minutesBetween(start, end);
        boolean matched = diff >= 0 && diff <= minutes;
        if (matched) {
            addEvidence(outcome, fact, "within_minutes_from", diff + " minutes");
        }
        return matched;
    }

    @SuppressWarnings("unchecked")
    private List<Object> valuesForPath(Map<String, Object> patientContext, String path) {
        Object root = patientContext;
        String normalizedPath = path;
        if (path.startsWith("patient.")) {
            root = patientContext.get("patient");
            normalizedPath = path.substring("patient.".length());
        } else if (path.startsWith("encounter.")) {
            root = patientContext.get("encounter");
            normalizedPath = path.substring("encounter.".length());
        } else if (path.startsWith("facts.")) {
            root = patientContext.get("facts");
            normalizedPath = path.substring("facts.".length());
        } else if (path.startsWith("forms.")) {
            Map<String, Object> facts = (Map<String, Object>) patientContext.get("facts");
            root = facts == null ? null : facts.get("forms");
            normalizedPath = path.substring("forms.".length());
        } else if (!(path.startsWith("encounter") || path.startsWith("patient"))) {
            Map<String, Object> facts = (Map<String, Object>) patientContext.get("facts");
            if (facts != null && facts.containsKey(firstSegment(path))) {
                root = facts;
            }
        }
        List<Object> current = new ArrayList<Object>();
        current.add(root);
        String[] segments = normalizedPath.split("\\.");
        for (String segment : segments) {
            List<Object> next = new ArrayList<Object>();
            for (Object value : current) {
                collectSegment(value, segment, next);
            }
            current = flatten(next);
            if (current.isEmpty()) {
                return current;
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private void collectSegment(Object value, String segment, List<Object> next) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                collectSegment(item, segment, next);
            }
            return;
        }
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.containsKey(segment)) {
                next.add(map.get(segment));
                return;
            }
            Object code = map.get("code");
            if (code != null && segment.equals(String.valueOf(code))) {
                next.add(map);
            }
        }
    }

    private List<Object> flatten(List<Object> values) {
        List<Object> flattened = new ArrayList<Object>();
        for (Object value : values) {
            if (value instanceof Collection) {
                flattened.addAll((Collection<?>) value);
            } else if (value != null) {
                flattened.add(value);
            }
        }
        return flattened;
    }

    private void addEvidence(EvaluationOutcome outcome, String fact, String operator, Object actual) {
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("type", "rule_condition");
        evidence.put("fact", fact);
        evidence.put("operator", operator);
        evidence.put("actual", actual);
        outcome.evidence.add(evidence);
    }

    private boolean same(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return actual == expected;
        }
        return String.valueOf(actual).equalsIgnoreCase(String.valueOf(expected));
    }

    private Temporal parseTime(String text) {
        if (isBlank(text)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(text);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private long minutesBetween(Temporal start, Temporal end) {
        if (start instanceof OffsetDateTime && end instanceof OffsetDateTime) {
            return Duration.between((OffsetDateTime) start, (OffsetDateTime) end).toMinutes();
        }
        LocalDateTime startLocal = start instanceof OffsetDateTime
                ? ((OffsetDateTime) start).toLocalDateTime()
                : (LocalDateTime) start;
        LocalDateTime endLocal = end instanceof OffsetDateTime
                ? ((OffsetDateTime) end).toLocalDateTime()
                : (LocalDateTime) end;
        return Duration.between(startLocal, endLocal).toMinutes();
    }

    private String firstSegment(String path) {
        int index = path.indexOf('.');
        return index < 0 ? path : path.substring(0, index);
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer integer(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? null : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static class EvaluationOutcome {
        private boolean hit;
        private List<Map<String, Object>> evidence = new ArrayList<Map<String, Object>>();
        private List<String> missingFacts = new ArrayList<String>();

        public boolean isHit() {
            return hit;
        }

        public List<Map<String, Object>> getEvidence() {
            return evidence;
        }

        public List<String> getMissingFacts() {
            return missingFacts;
        }
    }
}
