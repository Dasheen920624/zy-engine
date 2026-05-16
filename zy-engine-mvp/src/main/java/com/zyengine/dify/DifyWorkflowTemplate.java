package com.zyengine.dify;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DifyWorkflowTemplate {
    private String workflowCode;
    private String workflowName;
    private String workflowVersion;
    private String description;
    private String difyAppCode;
    private Integer timeoutMs;
    private Map<String, Object> inputDefaults = new LinkedHashMap<String, Object>();
    private List<String> requiredInputs = new ArrayList<String>();
    private Map<String, Object> degradedOutputs = new LinkedHashMap<String, Object>();

    public String getWorkflowCode() {
        return workflowCode;
    }

    public void setWorkflowCode(String workflowCode) {
        this.workflowCode = workflowCode;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getWorkflowVersion() {
        return workflowVersion;
    }

    public void setWorkflowVersion(String workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDifyAppCode() {
        return difyAppCode;
    }

    public void setDifyAppCode(String difyAppCode) {
        this.difyAppCode = difyAppCode;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Map<String, Object> getInputDefaults() {
        return inputDefaults;
    }

    public void setInputDefaults(Map<String, Object> inputDefaults) {
        this.inputDefaults = inputDefaults;
    }

    public List<String> getRequiredInputs() {
        return requiredInputs;
    }

    public void setRequiredInputs(List<String> requiredInputs) {
        this.requiredInputs = requiredInputs;
    }

    public Map<String, Object> getDegradedOutputs() {
        return degradedOutputs;
    }

    public void setDegradedOutputs(Map<String, Object> degradedOutputs) {
        this.degradedOutputs = degradedOutputs;
    }
}
