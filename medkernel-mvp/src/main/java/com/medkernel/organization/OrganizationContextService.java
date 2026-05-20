package com.medkernel.organization;

import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrganizationContextService {
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_GROUP_CODE = "X-Group-Code";
    public static final String HEADER_HOSPITAL_CODE = "X-Hospital-Code";
    public static final String HEADER_CAMPUS_CODE = "X-Campus-Code";
    public static final String HEADER_SITE_CODE = "X-Site-Code";
    public static final String HEADER_DEPARTMENT_CODE = "X-Department-Code";
    public static final String HEADER_ORG_CODE = "X-Org-Code";

    private static final String DEFAULT_TENANT_ID = "default";
    private static final String DEFAULT_HOSPITAL_CODE = "ZYHOSPITAL";

    public OrganizationContext resolve(HttpServletRequest request) {
        OrganizationContext context = new OrganizationContext();
        context.setTenantId(value(request, "tenant_id", "tenantId", HEADER_TENANT_ID, DEFAULT_TENANT_ID));
        context.setGroupCode(value(request, "group_code", "groupCode", HEADER_GROUP_CODE, null));
        context.setHospitalCode(value(request, "hospital_code", "hospitalCode", HEADER_HOSPITAL_CODE, null));
        context.setCampusCode(value(request, "campus_code", "campusCode", HEADER_CAMPUS_CODE, null));
        context.setSiteCode(value(request, "site_code", "siteCode", HEADER_SITE_CODE, null));
        context.setDepartmentCode(value(request, "department_code", "departmentCode", HEADER_DEPARTMENT_CODE, null));
        context.setLegacyOrgCode(value(request, "org_code", "orgCode", HEADER_ORG_CODE, null));

        if (context.getHospitalCode() == null && context.getLegacyOrgCode() != null) {
            context.setHospitalCode(context.getLegacyOrgCode());
        }
        if (context.getHospitalCode() == null) {
            context.setHospitalCode(DEFAULT_HOSPITAL_CODE);
        }
        if (context.getLegacyOrgCode() == null) {
            context.setLegacyOrgCode(context.getHospitalCode());
        }

        applyEffectiveScope(context);
        context.setSource(resolveSource(request));
        context.setWarnings(warnings(context));
        return context;
    }

    /**
     * 在 Header/Query 解析基础上叠加请求体的组织声明，便于 RULE-001/PKG-001 这类
     * 同时接受 Header 与 JSON Body 的接口让第三方在 Body 中显式声明组织维度时优先生效。
     */
    public OrganizationContext resolveWithBody(HttpServletRequest request, Map<String, Object> body) {
        OrganizationContext context = resolve(request);
        if (body == null || body.isEmpty()) {
            return context;
        }
        boolean applied = applyBody(context, body);
        if (applied) {
            applyEffectiveScope(context);
            context.setSource("BODY");
            context.setWarnings(warnings(context));
        }
        return context;
    }

    /**
     * 便捷方法：直接获取租户ID字符串。
     */
    public String resolveTenantId(HttpServletRequest request) {
        OrganizationContext ctx = resolve(request);
        return ctx.getTenantId();
    }

    /**
     * 便捷方法：获取租户ID（Long类型）。
     */
    public Long getTenantId(HttpServletRequest request) {
        String tenantId = resolveTenantId(request);
        if (tenantId == null || tenantId.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 便捷方法：等同于 resolve(request)，返回 OrganizationContext。
     */
    public OrganizationContext getContext(HttpServletRequest request) {
        return resolve(request);
    }

    public void applyExplicitFilters(Map<String, String> filters, HttpServletRequest request) {
        if (filters == null || request == null) {
            return;
        }
        putFilter(filters, "tenantId", explicitValue(request, "tenant_id", "tenantId", HEADER_TENANT_ID));
        putFilter(filters, "groupCode", explicitValue(request, "group_code", "groupCode", HEADER_GROUP_CODE));
        String hospitalCode = explicitValue(request, "hospital_code", "hospitalCode", HEADER_HOSPITAL_CODE);
        String legacyOrgCode = explicitValue(request, "org_code", "orgCode", HEADER_ORG_CODE);
        putFilter(filters, "hospitalCode", hospitalCode == null ? legacyOrgCode : hospitalCode);
        putFilter(filters, "legacyOrgCode", legacyOrgCode);
        putFilter(filters, "campusCode", explicitValue(request, "campus_code", "campusCode", HEADER_CAMPUS_CODE));
        putFilter(filters, "siteCode", explicitValue(request, "site_code", "siteCode", HEADER_SITE_CODE));
        putFilter(filters, "departmentCode", explicitValue(request, "department_code", "departmentCode", HEADER_DEPARTMENT_CODE));
        putFilter(filters, "scopeLevel", explicitParam(request, "scope_level", "scopeLevel"));
        putFilter(filters, "scopeCode", explicitParam(request, "scope_code", "scopeCode"));
    }

    private void putFilter(Map<String, String> filters, String key, String value) {
        if (value != null) {
            filters.put(key, value);
        }
    }

    private String explicitValue(HttpServletRequest request, String snakeParam, String camelParam, String headerName) {
        String value = explicitParam(request, snakeParam, camelParam);
        if (value == null) {
            value = clean(request.getHeader(headerName));
        }
        return value;
    }

    private String explicitParam(HttpServletRequest request, String snakeParam, String camelParam) {
        String value = clean(request.getParameter(snakeParam));
        if (value == null) {
            value = clean(request.getParameter(camelParam));
        }
        return value;
    }

    private boolean applyBody(OrganizationContext context, Map<String, Object> body) {
        boolean applied = false;
        String tenant = bodyField(body, "tenant_id", "tenantId");
        if (tenant != null) {
            context.setTenantId(tenant);
            applied = true;
        }
        String group = bodyField(body, "group_code", "groupCode");
        if (group != null) {
            context.setGroupCode(group);
            applied = true;
        }
        String hospital = bodyField(body, "hospital_code", "hospitalCode");
        if (hospital != null) {
            context.setHospitalCode(hospital);
            applied = true;
        }
        String campus = bodyField(body, "campus_code", "campusCode");
        if (campus != null) {
            context.setCampusCode(campus);
            applied = true;
        }
        String site = bodyField(body, "site_code", "siteCode");
        if (site != null) {
            context.setSiteCode(site);
            applied = true;
        }
        String department = bodyField(body, "department_code", "departmentCode");
        if (department != null) {
            context.setDepartmentCode(department);
            applied = true;
        }
        String legacyOrg = bodyField(body, "org_code", "orgCode");
        if (legacyOrg != null) {
            context.setLegacyOrgCode(legacyOrg);
            if (context.getHospitalCode() == null) {
                context.setHospitalCode(legacyOrg);
            }
            applied = true;
        }
        if (context.getLegacyOrgCode() == null && context.getHospitalCode() != null) {
            context.setLegacyOrgCode(context.getHospitalCode());
        }
        return applied;
    }

    private String bodyField(Map<String, Object> body, String snakeKey, String camelKey) {
        String value = clean(stringOf(body.get(snakeKey)));
        if (value == null) {
            value = clean(stringOf(body.get(camelKey)));
        }
        return value;
    }

    private String stringOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public Map<String, Object> orgContextView(HttpServletRequest request) {
        OrganizationContext context = resolve(request);
        Map<String, Object> data = context.toView();
        data.put("model", modelView());
        data.put("supported_scope_levels", Arrays.asList(
                "PLATFORM", "GROUP", "HOSPITAL", "CAMPUS", "SITE", "DEPARTMENT"));
        data.put("scope_level_names", scopeLevelNames());
        data.put("header_contract", headerContract());
        data.put("db_only_supported", true);
        return data;
    }

    private void applyEffectiveScope(OrganizationContext context) {
        if (present(context.getDepartmentCode())) {
            context.setEffectiveScopeLevel("DEPARTMENT");
            context.setEffectiveScopeCode(context.getDepartmentCode());
            return;
        }
        if (present(context.getSiteCode())) {
            context.setEffectiveScopeLevel("SITE");
            context.setEffectiveScopeCode(context.getSiteCode());
            return;
        }
        if (present(context.getCampusCode())) {
            context.setEffectiveScopeLevel("CAMPUS");
            context.setEffectiveScopeCode(context.getCampusCode());
            return;
        }
        if (present(context.getHospitalCode())) {
            context.setEffectiveScopeLevel("HOSPITAL");
            context.setEffectiveScopeCode(context.getHospitalCode());
            return;
        }
        if (present(context.getGroupCode())) {
            context.setEffectiveScopeLevel("GROUP");
            context.setEffectiveScopeCode(context.getGroupCode());
            return;
        }
        context.setEffectiveScopeLevel("PLATFORM");
        context.setEffectiveScopeCode("DEFAULT");
    }

    private List<String> warnings(OrganizationContext context) {
        List<String> warnings = new ArrayList<String>();
        if (present(context.getDepartmentCode()) && !present(context.getSiteCode())
                && !present(context.getCampusCode()) && !present(context.getHospitalCode())) {
            warnings.add("department_code 已提供，但缺少上级 site/campus/hospital 上下文。");
        }
        if (present(context.getSiteCode()) && !present(context.getCampusCode()) && !present(context.getHospitalCode())) {
            warnings.add("site_code 已提供，但缺少上级 campus/hospital 上下文。");
        }
        if (present(context.getCampusCode()) && !present(context.getHospitalCode())) {
            warnings.add("campus_code 已提供，但缺少上级 hospital 上下文。");
        }
        return warnings;
    }

    private Map<String, Object> modelView() {
        List<Map<String, Object>> levels = new ArrayList<Map<String, Object>>();
        levels.add(level("PLATFORM", "系统内置默认（产品基线配置）", null));
        levels.add(level("GROUP", "集团", null));
        levels.add(level("HOSPITAL", "医院", "GROUP"));
        levels.add(level("CAMPUS", "院区", "HOSPITAL"));
        levels.add(level("SITE", "卫生所/站点", "CAMPUS"));
        levels.add(level("DEPARTMENT", "科室", "SITE"));

        Map<String, Object> model = new LinkedHashMap<String, Object>();
        model.put("levels", levels);
        model.put("config_precedence", Arrays.asList(
                "DEPARTMENT", "SITE", "CAMPUS", "HOSPITAL", "GROUP", "PLATFORM"));
        model.put("baseline_scope_level", "PLATFORM");
        model.put("baseline_scope_name", "系统内置默认（产品基线配置）");
        model.put("legacy_mapping", "org_code 当前兼容映射为 hospital_code，后续逐步迁移到完整组织维度。");
        return model;
    }

    private Map<String, Object> level(String code, String name, String parent) {
        Map<String, Object> level = new LinkedHashMap<String, Object>();
        level.put("level", code);
        level.put("name", name);
        level.put("parent_level", parent);
        return level;
    }

    private Map<String, Object> scopeLevelNames() {
        Map<String, Object> names = new LinkedHashMap<String, Object>();
        names.put("PLATFORM", "系统内置默认（产品基线配置）");
        names.put("GROUP", "集团");
        names.put("HOSPITAL", "医院");
        names.put("CAMPUS", "院区");
        names.put("SITE", "卫生所/站点");
        names.put("DEPARTMENT", "科室");
        return names;
    }

    private Map<String, Object> headerContract() {
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("tenant_id", HEADER_TENANT_ID);
        headers.put("group_code", HEADER_GROUP_CODE);
        headers.put("hospital_code", HEADER_HOSPITAL_CODE);
        headers.put("campus_code", HEADER_CAMPUS_CODE);
        headers.put("site_code", HEADER_SITE_CODE);
        headers.put("department_code", HEADER_DEPARTMENT_CODE);
        headers.put("legacy_org_code", HEADER_ORG_CODE);
        return headers;
    }

    private String value(HttpServletRequest request, String snakeParam, String camelParam,
                         String headerName, String defaultValue) {
        String value = clean(request.getParameter(snakeParam));
        if (value == null) {
            value = clean(request.getParameter(camelParam));
        }
        if (value == null) {
            value = clean(request.getHeader(headerName));
        }
        return value == null ? defaultValue : value;
    }

    private String resolveSource(HttpServletRequest request) {
        if (hasAnyParam(request)) {
            return "QUERY";
        }
        if (hasAnyHeader(request)) {
            return "HEADER";
        }
        return "DEFAULT";
    }

    private boolean hasAnyParam(HttpServletRequest request) {
        return present(request.getParameter("tenant_id"))
                || present(request.getParameter("tenantId"))
                || present(request.getParameter("group_code"))
                || present(request.getParameter("groupCode"))
                || present(request.getParameter("hospital_code"))
                || present(request.getParameter("hospitalCode"))
                || present(request.getParameter("campus_code"))
                || present(request.getParameter("campusCode"))
                || present(request.getParameter("site_code"))
                || present(request.getParameter("siteCode"))
                || present(request.getParameter("department_code"))
                || present(request.getParameter("departmentCode"))
                || present(request.getParameter("org_code"))
                || present(request.getParameter("orgCode"));
    }

    private boolean hasAnyHeader(HttpServletRequest request) {
        return present(request.getHeader(HEADER_TENANT_ID))
                || present(request.getHeader(HEADER_GROUP_CODE))
                || present(request.getHeader(HEADER_HOSPITAL_CODE))
                || present(request.getHeader(HEADER_CAMPUS_CODE))
                || present(request.getHeader(HEADER_SITE_CODE))
                || present(request.getHeader(HEADER_DEPARTMENT_CODE))
                || present(request.getHeader(HEADER_ORG_CODE));
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private boolean present(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
