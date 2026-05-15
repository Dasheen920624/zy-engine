package com.zyengine.rule;

import java.util.LinkedHashMap;
import java.util.Map;

public class RuleDefinition {
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private String versionNo;
    private String status;
    private String severity;
    private boolean enabled;
    private Map<String, Object> ruleJson = new LinkedHashMap<String, Object>();

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getRuleJson() {
        return ruleJson;
    }

    public void setRuleJson(Map<String, Object> ruleJson) {
        this.ruleJson = ruleJson;
    }
}
