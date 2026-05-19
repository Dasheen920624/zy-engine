package com.medkernel.quality;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估指标集。
 * 一组相关指标的集合，含版本号，可绑定来源文档。
 * 对应数据库表：qc_eval_indicator_set
 */
public class EvalIndicatorSet {
    private String tenantId;
    private String setCode;
    private String setName;
    private String subjectType;
    private String description;
    private String version;
    private String status; // DRAFT, PUBLISHED, DEPRECATED
    private String documentCode;
    private String citationId;
    private String bindingType;
    private String groupCode;
    private String hospitalCode;
    private String campusCode;
    private String siteCode;
    private String departmentCode;
    private String scopeLevel;
    private String scopeCode;
    private String orgSource;
    private String createdBy;
    private String createdTime;
    private String updatedTime;
    private List<EvalIndicator> indicators;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }

    public String getSetName() { return setName; }
    public void setSetName(String setName) { this.setName = setName; }

    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDocumentCode() { return documentCode; }
    public void setDocumentCode(String documentCode) { this.documentCode = documentCode; }

    public String getCitationId() { return citationId; }
    public void setCitationId(String citationId) { this.citationId = citationId; }

    public String getBindingType() { return bindingType; }
    public void setBindingType(String bindingType) { this.bindingType = bindingType; }

    public String getGroupCode() { return groupCode; }
    public void setGroupCode(String groupCode) { this.groupCode = groupCode; }

    public String getHospitalCode() { return hospitalCode; }
    public void setHospitalCode(String hospitalCode) { this.hospitalCode = hospitalCode; }

    public String getCampusCode() { return campusCode; }
    public void setCampusCode(String campusCode) { this.campusCode = campusCode; }

    public String getSiteCode() { return siteCode; }
    public void setSiteCode(String siteCode) { this.siteCode = siteCode; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getScopeLevel() { return scopeLevel; }
    public void setScopeLevel(String scopeLevel) { this.scopeLevel = scopeLevel; }

    public String getScopeCode() { return scopeCode; }
    public void setScopeCode(String scopeCode) { this.scopeCode = scopeCode; }

    public String getOrgSource() { return orgSource; }
    public void setOrgSource(String orgSource) { this.orgSource = orgSource; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }

    public String getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(String updatedTime) { this.updatedTime = updatedTime; }

    public List<EvalIndicator> getIndicators() { return indicators; }
    public void setIndicators(List<EvalIndicator> indicators) { this.indicators = indicators; }

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("tenant_id", tenantId);
        view.put("set_code", setCode);
        view.put("set_name", setName);
        view.put("subject_type", subjectType);
        view.put("description", description);
        view.put("version", version);
        view.put("status", status);
        view.put("source", buildSourceView());
        view.put("org_context", buildOrgContextView());
        view.put("created_by", createdBy);
        view.put("created_time", createdTime);
        view.put("updated_time", updatedTime);
        if (indicators != null) {
            List<Map<String, Object>> indicatorViews = new ArrayList<Map<String, Object>>();
            for (EvalIndicator ind : indicators) {
                indicatorViews.add(ind.toView());
            }
            view.put("indicators", indicatorViews);
        }
        return view;
    }

    private Map<String, Object> buildSourceView() {
        Map<String, Object> source = new LinkedHashMap<String, Object>();
        source.put("document_code", documentCode);
        source.put("citation_id", citationId);
        source.put("binding_type", bindingType);
        return source;
    }

    private Map<String, Object> buildOrgContextView() {
        Map<String, Object> ctx = new LinkedHashMap<String, Object>();
        ctx.put("group_code", groupCode);
        ctx.put("hospital_code", hospitalCode);
        ctx.put("campus_code", campusCode);
        ctx.put("site_code", siteCode);
        ctx.put("department_code", departmentCode);
        ctx.put("scope_level", scopeLevel);
        ctx.put("scope_code", scopeCode);
        ctx.put("org_source", orgSource);
        return ctx;
    }
}
