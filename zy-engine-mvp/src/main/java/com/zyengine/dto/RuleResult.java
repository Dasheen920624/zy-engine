package com.zyengine.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RuleResult {
    private String ruleCode;
    private boolean hit;
    private String severity;
    private String message;
    private List<String> actions = new ArrayList<String>();
    private List<Map<String, Object>> evidence = new ArrayList<Map<String, Object>>();

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public boolean isHit() {
        return hit;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> actions) {
        this.actions = actions;
    }

    public List<Map<String, Object>> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<Map<String, Object>> evidence) {
        this.evidence = evidence;
    }
}

