package com.medkernel.adapter.dto;

import java.util.Map;

/**
 * 触发点匹配响应 DTO。
 */
public class TriggerMatchResponse {
    private String triggerCode;
    private String triggerName;
    private String triggerType;
    private String businessScenario;
    private String accessStrategy;
    private String adapterCode;
    private String endpointUrl;
    private int priority;
    private String riskLevel;
    private String ruleCodes;
    private String pathwayCodes;

    public static TriggerMatchResponse fromMap(Map<String, Object> map) {
        TriggerMatchResponse dto = new TriggerMatchResponse();
        dto.setTriggerCode(string(map.get("triggerCode")));
        dto.setTriggerName(string(map.get("triggerName")));
        dto.setTriggerType(string(map.get("triggerType")));
        dto.setBusinessScenario(string(map.get("businessScenario")));
        dto.setAccessStrategy(string(map.get("accessStrategy")));
        dto.setAdapterCode(string(map.get("adapterCode")));
        dto.setEndpointUrl(string(map.get("endpointUrl")));
        dto.setPriority(intValue(map.get("priority")));
        dto.setRiskLevel(string(map.get("riskLevel")));
        dto.setRuleCodes(string(map.get("ruleCodes")));
        dto.setPathwayCodes(string(map.get("pathwayCodes")));
        return dto;
    }

    private static String string(Object value) {
        return value != null ? value.toString() : null;
    }

    private static int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    public String getTriggerCode() { return triggerCode; }
    public void setTriggerCode(String triggerCode) { this.triggerCode = triggerCode; }
    public String getTriggerName() { return triggerName; }
    public void setTriggerName(String triggerName) { this.triggerName = triggerName; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getBusinessScenario() { return businessScenario; }
    public void setBusinessScenario(String businessScenario) { this.businessScenario = businessScenario; }
    public String getAccessStrategy() { return accessStrategy; }
    public void setAccessStrategy(String accessStrategy) { this.accessStrategy = accessStrategy; }
    public String getAdapterCode() { return adapterCode; }
    public void setAdapterCode(String adapterCode) { this.adapterCode = adapterCode; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRuleCodes() { return ruleCodes; }
    public void setRuleCodes(String ruleCodes) { this.ruleCodes = ruleCodes; }
    public String getPathwayCodes() { return pathwayCodes; }
    public void setPathwayCodes(String pathwayCodes) { this.pathwayCodes = pathwayCodes; }
}
