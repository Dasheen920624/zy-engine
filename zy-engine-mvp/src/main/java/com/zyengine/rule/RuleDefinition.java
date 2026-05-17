package com.zyengine.rule;

import java.util.LinkedHashMap;
import java.util.Map;

public class RuleDefinition {
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private String versionNo;
    private String packageCode;
    private String packageVersion;
    private String status;
    private String severity;
    private boolean enabled;
    private String publishedBy;
    private String publishedTime;
    private String tenantId;
    private String groupCode;
    private String hospitalCode;
    private String campusCode;
    private String siteCode;
    private String departmentCode;
    private String legacyOrgCode;
    private String scopeLevel;
    private String scopeCode;
    private String orgSource;
    private String referenceDocumentCode;
    private String referenceCitationId;
    private String referenceBindingType;
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

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
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

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public String getPublishedTime() {
        return publishedTime;
    }

    public void setPublishedTime(String publishedTime) {
        this.publishedTime = publishedTime;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
    }

    public String getCampusCode() {
        return campusCode;
    }

    public void setCampusCode(String campusCode) {
        this.campusCode = campusCode;
    }

    public String getSiteCode() {
        return siteCode;
    }

    public void setSiteCode(String siteCode) {
        this.siteCode = siteCode;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getLegacyOrgCode() {
        return legacyOrgCode;
    }

    public void setLegacyOrgCode(String legacyOrgCode) {
        this.legacyOrgCode = legacyOrgCode;
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

    public String getOrgSource() {
        return orgSource;
    }

    public void setOrgSource(String orgSource) {
        this.orgSource = orgSource;
    }

    public String getReferenceDocumentCode() {
        return referenceDocumentCode;
    }

    public void setReferenceDocumentCode(String referenceDocumentCode) {
        this.referenceDocumentCode = referenceDocumentCode;
    }

    public String getReferenceCitationId() {
        return referenceCitationId;
    }

    public void setReferenceCitationId(String referenceCitationId) {
        this.referenceCitationId = referenceCitationId;
    }

    public String getReferenceBindingType() {
        return referenceBindingType;
    }

    public void setReferenceBindingType(String referenceBindingType) {
        this.referenceBindingType = referenceBindingType;
    }

    public Map<String, Object> getRuleJson() {
        return ruleJson;
    }

    public void setRuleJson(Map<String, Object> ruleJson) {
        this.ruleJson = ruleJson;
    }
}
