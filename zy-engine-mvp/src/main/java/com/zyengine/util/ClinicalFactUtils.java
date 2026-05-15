package com.zyengine.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ClinicalFactUtils {
    private ClinicalFactUtils() {
    }

    @SuppressWarnings("unchecked")
    public static String patientId(Map<String, Object> context) {
        Map<String, Object> patient = (Map<String, Object>) context.get("patient");
        return patient == null ? null : stringValue(patient.get("patient_id"));
    }

    @SuppressWarnings("unchecked")
    public static String encounterId(Map<String, Object> context) {
        Map<String, Object> encounter = (Map<String, Object>) context.get("encounter");
        return encounter == null ? null : stringValue(encounter.get("encounter_id"));
    }

    @SuppressWarnings("unchecked")
    public static boolean hasChestPain(Map<String, Object> context) {
        Map<String, Object> facts = (Map<String, Object>) context.get("facts");
        if (facts == null) {
            return false;
        }
        Object complaints = facts.get("chief_complaints");
        if (!(complaints instanceof List)) {
            return false;
        }
        for (Object item : (List<?>) complaints) {
            if (item instanceof Map) {
                Map<String, Object> complaint = (Map<String, Object>) item;
                if ("CHEST_PAIN".equalsIgnoreCase(stringValue(complaint.get("code")))) {
                    return true;
                }
                if (containsCollectionValue(complaint.get("codes"), "CHEST_PAIN")) {
                    return true;
                }
                if (containsAnyIgnoreCase(stringValue(complaint.get("text")),
                        "chest pain", "chest tightness", "precordial pain")) {
                    return true;
                }
                String text = stringValue(complaint.get("text"));
                if (text != null && (text.contains("胸痛") || text.contains("胸闷") || text.contains("心前区痛"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static boolean hasStElevation(Map<String, Object> context) {
        Map<String, Object> facts = (Map<String, Object>) context.get("facts");
        if (facts == null) {
            return false;
        }
        Object exams = facts.get("exams");
        if (!(exams instanceof List)) {
            return false;
        }
        for (Object exam : (List<?>) exams) {
            if (exam instanceof Map) {
                Object codes = ((Map<String, Object>) exam).get("finding_codes");
                if (containsCollectionValue(codes, "ST_ELEVATION_CONTIGUOUS_LEADS")) {
                    return true;
                }
                String report = stringValue(((Map<String, Object>) exam).get("report_text"));
                if (containsAnyIgnoreCase(report, "st elevation", "st-segment elevation")) {
                    return true;
                }
                if (report != null && (report.contains("ST段抬高") || report.contains("ST 段抬高"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static boolean hasHistory(Map<String, Object> context, String code) {
        Map<String, Object> facts = (Map<String, Object>) context.get("facts");
        if (facts == null) {
            return false;
        }
        Object histories = facts.get("histories");
        if (!(histories instanceof List)) {
            return false;
        }
        for (Object history : (List<?>) histories) {
            if (history instanceof Map && code.equals(stringValue(((Map<String, Object>) history).get("code")))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyIgnoreCase(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        String normalized = text.toLowerCase();
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCollectionValue(Object values, String expected) {
        if (values instanceof Collection) {
            for (Object value : (Collection<?>) values) {
                if (expected.equals(stringValue(value))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
