package com.medkernel.organization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrganizationContext {
    private String tenantId;
    private String groupCode;
    private String hospitalCode;
    private String campusCode;
    private String siteCode;
    private String departmentCode;
    private String legacyOrgCode;
    private String effectiveScopeLevel;
    private String effectiveScopeCode;
    private String source;
    private List<String> warnings = new ArrayList<String>();

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("tenant_id", tenantId);
        view.put("group_code", groupCode);
        view.put("hospital_code", hospitalCode);
        view.put("campus_code", campusCode);
        view.put("site_code", siteCode);
        view.put("department_code", departmentCode);
        view.put("legacy_org_code", legacyOrgCode);
        view.put("effective_scope_level", effectiveScopeLevel);
        view.put("effective_scope_code", effectiveScopeCode);
        view.put("inheritance_order", inheritanceOrder());
        view.put("source", source);
        view.put("warnings", warnings);
        return view;
    }

    private List<Map<String, Object>> inheritanceOrder() {
        List<Map<String, Object>> order = new ArrayList<Map<String, Object>>();
        addScope(order, "DEPARTMENT", departmentCode);
        addScope(order, "SITE", siteCode);
        addScope(order, "CAMPUS", campusCode);
        addScope(order, "HOSPITAL", hospitalCode);
        addScope(order, "GROUP", groupCode);
        addScope(order, "PLATFORM", "DEFAULT");
        return order;
    }

    private void addScope(List<Map<String, Object>> order, String level, String code) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        Map<String, Object> scope = new LinkedHashMap<String, Object>();
        scope.put("scope_level", level);
        scope.put("scope_code", code);
        scope.put("scope_name", scopeName(level));
        order.add(scope);
    }

    private String scopeName(String level) {
        if ("DEPARTMENT".equals(level)) {
            return "科室";
        }
        if ("SITE".equals(level)) {
            return "卫生所/站点";
        }
        if ("CAMPUS".equals(level)) {
            return "院区";
        }
        if ("HOSPITAL".equals(level)) {
            return "医院";
        }
        if ("GROUP".equals(level)) {
            return "集团";
        }
        if ("PLATFORM".equals(level)) {
            return "系统内置默认（产品基线配置）";
        }
        return level;
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

    public String getEffectiveScopeLevel() {
        return effectiveScopeLevel;
    }

    public void setEffectiveScopeLevel(String effectiveScopeLevel) {
        this.effectiveScopeLevel = effectiveScopeLevel;
    }

    public String getEffectiveScopeCode() {
        return effectiveScopeCode;
    }

    public void setEffectiveScopeCode(String effectiveScopeCode) {
        this.effectiveScopeCode = effectiveScopeCode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings == null ? new ArrayList<String>() : warnings;
    }
}
