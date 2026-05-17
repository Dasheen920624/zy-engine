package com.zyengine.organization;

import java.util.LinkedHashMap;
import java.util.Map;

public class OrgOverrideEntry {
    private String tenantId;
    private String scopeLevel;
    private String scopeCode;
    private String assetType;
    private String overrideKey;
    private Object overrideValue;
    private String sourceLevel;
    private String description;
    private String createdBy;
    private String createdTime;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getScopeLevel() {
        return scopeLevel;
    }

    public void setScopeLevel(String scopeLevel) {
        this.scopeLevel = scopeLevel;
    }

    public String getScopeCode() {
        return scopeCode;
    }

    public void setScopeCode(String scopeCode) {
        this.scopeCode = scopeCode;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getOverrideKey() {
        return overrideKey;
    }

    public void setOverrideKey(String overrideKey) {
        this.overrideKey = overrideKey;
    }

    public Object getOverrideValue() {
        return overrideValue;
    }

    public void setOverrideValue(Object overrideValue) {
        this.overrideValue = overrideValue;
    }

    public String getSourceLevel() {
        return sourceLevel;
    }

    public void setSourceLevel(String sourceLevel) {
        this.sourceLevel = sourceLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("tenant_id", tenantId);
        view.put("scope_level", scopeLevel);
        view.put("scope_code", scopeCode);
        view.put("asset_type", assetType);
        view.put("override_key", overrideKey);
        view.put("override_value", overrideValue);
        view.put("source_level", sourceLevel);
        view.put("description", description);
        view.put("created_by", createdBy);
        view.put("created_time", createdTime);
        return view;
    }
}