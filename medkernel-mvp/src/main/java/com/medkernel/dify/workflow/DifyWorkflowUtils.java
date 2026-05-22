package com.medkernel.dify.workflow;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DifyWorkflowUtils {

    private DifyWorkflowUtils() {
    }

    static String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    static String requireField(Map<String, Object> entry, String field) {
        String value = string(entry.get(field), null);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    static String patientId(Map<String, Object> inputs) {
        String direct = string(inputs.get("patient_id"), null);
        if (direct != null) {
            return direct;
        }
        Object patient = inputs.get("patient");
        if (patient instanceof Map) {
            return string(((Map<String, Object>) patient).get("patient_id"), null);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    static String encounterId(Map<String, Object> inputs) {
        String direct = string(inputs.get("encounter_id"), null);
        if (direct != null) {
            return direct;
        }
        Object encounter = inputs.get("encounter");
        if (encounter instanceof Map) {
            return string(((Map<String, Object>) encounter).get("encounter_id"), null);
        }
        return null;
    }

    static long longValue(Object value, long defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    static String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }

    static boolean isInteger(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    static boolean hasValue(Object value) {
        return value != null && (!(value instanceof String) || !((String) value).trim().isEmpty());
    }

    @SuppressWarnings("unchecked")
    static Object readPath(Map<String, Object> source, String path) {
        if (source == null || path == null) {
            return null;
        }
        String normalized = path.trim();
        if (normalized.startsWith("$.")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("request.")) {
            normalized = normalized.substring("request.".length());
        }
        if (normalized.startsWith("inputs.")) {
            normalized = normalized.substring("inputs.".length());
        }
        if (normalized.isEmpty()) {
            return null;
        }

        Object current = source;
        for (String token : normalized.split("\\.")) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(token);
            } else if (current instanceof List && isInteger(token)) {
                List<?> list = (List<?>) current;
                int index = Integer.parseInt(token);
                current = index >= 0 && index < list.size() ? list.get(index) : null;
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    static String filterValue(Map<String, String> filters, String key) {
        if (filters == null) {
            return null;
        }
        String value = filters.get(key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    static int filterInt(Map<String, String> filters, String key, int defaultValue) {
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

    static String key(String workflowCode, String workflowVersion) {
        return workflowCode + "::" + (workflowVersion == null ? "1.0.0" : workflowVersion);
    }
}
