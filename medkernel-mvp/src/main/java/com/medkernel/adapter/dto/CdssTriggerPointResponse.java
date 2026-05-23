package com.medkernel.adapter.dto;

import com.medkernel.adapter.CdssTriggerPointEntity;

import java.time.LocalDateTime;

/**
 * CDSS 触发点响应 DTO。
 */
public class CdssTriggerPointResponse {
    private Long id;
    private Long tenantId;
    private String triggerCode;
    private String triggerName;
    private String triggerType;
    private String businessScenario;
    private String accessStrategy;
    private String adapterCode;
    private String endpointUrl;
    private String ruleCodes;
    private String pathwayCodes;
    private int priority;
    private String riskLevel;
    private int timeoutMs;
    private String enabled;
    private String description;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public static CdssTriggerPointResponse fromEntity(CdssTriggerPointEntity entity) {
        CdssTriggerPointResponse dto = new CdssTriggerPointResponse();
        dto.setId(entity.getId());
        dto.setTenantId(entity.getTenantId());
        dto.setTriggerCode(entity.getTriggerCode());
        dto.setTriggerName(entity.getTriggerName());
        dto.setTriggerType(entity.getTriggerType());
        dto.setBusinessScenario(entity.getBusinessScenario());
        dto.setAccessStrategy(entity.getAccessStrategy());
        dto.setAdapterCode(entity.getAdapterCode());
        dto.setEndpointUrl(entity.getEndpointUrl());
        dto.setRuleCodes(entity.getRuleCodes());
        dto.setPathwayCodes(entity.getPathwayCodes());
        dto.setPriority(entity.getPriority());
        dto.setRiskLevel(entity.getRiskLevel());
        dto.setTimeoutMs(entity.getTimeoutMs());
        dto.setEnabled(entity.getEnabled());
        dto.setDescription(entity.getDescription());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedTime(entity.getUpdatedTime());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
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
    public String getRuleCodes() { return ruleCodes; }
    public void setRuleCodes(String ruleCodes) { this.ruleCodes = ruleCodes; }
    public String getPathwayCodes() { return pathwayCodes; }
    public void setPathwayCodes(String pathwayCodes) { this.pathwayCodes = pathwayCodes; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
