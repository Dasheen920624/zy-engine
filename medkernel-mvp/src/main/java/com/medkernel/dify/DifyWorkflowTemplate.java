package com.medkernel.dify;

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
    private Integer retryCount;
    private Map<String, Object> inputDefaults = new LinkedHashMap<String, Object>();
    private Map<String, String> inputMappings = new LinkedHashMap<String, String>();
    private List<String> requiredInputs = new ArrayList<String>();
    private Map<String, Object> degradedOutputs = new LinkedHashMap<String, Object>();
    private String referenceDocumentCode;
    private String referenceBindingType;

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

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Map<String, Object> getInputDefaults() {
        return inputDefaults;
    }

    public void setInputDefaults(Map<String, Object> inputDefaults) {
        this.inputDefaults = inputDefaults;
    }

    public Map<String, String> getInputMappings() {
        return inputMappings;
    }

    public void setInputMappings(Map<String, String> inputMappings) {
        this.inputMappings = inputMappings;
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

    public String getReferenceDocumentCode() {
        return referenceDocumentCode;
    }

    public void setReferenceDocumentCode(String referenceDocumentCode) {
        this.referenceDocumentCode = referenceDocumentCode;
    }

    public String getReferenceBindingType() {
        return referenceBindingType;
    }

    public void setReferenceBindingType(String referenceBindingType) {
        this.referenceBindingType = referenceBindingType;
    }
}
