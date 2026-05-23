package com.medkernel.organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * OrganizationContextService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class OrganizationContextServiceTest {

    private OrganizationContextService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationContextService();
    }

    // ===== resolve() 测试 =====

    @Test
    void resolve_withNoHeadersOrParams_returnsDefaults() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("default", ctx.getTenantId());
        assertEquals("DEFAULT_HOSPITAL", ctx.getHospitalCode());
        assertEquals("DEFAULT_HOSPITAL", ctx.getLegacyOrgCode());
        assertNull(ctx.getGroupCode());
        assertNull(ctx.getCampusCode());
        assertNull(ctx.getSiteCode());
        assertNull(ctx.getDepartmentCode());
        assertEquals("HOSPITAL", ctx.getEffectiveScopeLevel());
        assertEquals("DEFAULT_HOSPITAL", ctx.getEffectiveScopeCode());
        assertEquals("DEFAULT", ctx.getSource());
        assertTrue(ctx.getWarnings().isEmpty());
    }

    @Test
    void resolve_withHeaderTenantId_usesHeaderValue() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn("tenant-001");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("tenant-001", ctx.getTenantId());
    }

    @Test
    void resolve_withQueryParamSnakeCase_usesQueryParam() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("tenant_id")).thenReturn("query-tenant");
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("query-tenant", ctx.getTenantId());
        assertEquals("QUERY", ctx.getSource());
    }

    @Test
    void resolve_withQueryParamCamelCase_usesQueryParam() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("tenantId")).thenReturn("camel-tenant");
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("camel-tenant", ctx.getTenantId());
    }

    @Test
    void resolve_queryParamTakesPrecedenceOverHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("tenant_id")).thenReturn("query-value");
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn("header-value");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("query-value", ctx.getTenantId());
        assertEquals("QUERY", ctx.getSource());
    }

    @Test
    void resolve_withAllHeaders_setsAllFields() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn("t1");
        when(request.getHeader("X-Group-Code")).thenReturn("g1");
        when(request.getHeader("X-Hospital-Code")).thenReturn("h1");
        when(request.getHeader("X-Campus-Code")).thenReturn("c1");
        when(request.getHeader("X-Site-Code")).thenReturn("s1");
        when(request.getHeader("X-Department-Code")).thenReturn("d1");
        when(request.getHeader("X-Org-Code")).thenReturn("o1");

        OrganizationContext ctx = service.resolve(request);

        assertEquals("t1", ctx.getTenantId());
        assertEquals("g1", ctx.getGroupCode());
        assertEquals("h1", ctx.getHospitalCode());
        assertEquals("c1", ctx.getCampusCode());
        assertEquals("s1", ctx.getSiteCode());
        assertEquals("d1", ctx.getDepartmentCode());
        assertEquals("o1", ctx.getLegacyOrgCode());
        assertEquals("DEPARTMENT", ctx.getEffectiveScopeLevel());
        assertEquals("d1", ctx.getEffectiveScopeCode());
        assertEquals("HEADER", ctx.getSource());
    }

    @Test
    void resolve_legacyOrgCodeFillsHospitalCode_whenHospitalCodeMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Org-Code")).thenReturn("LEGACY001");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("LEGACY001", ctx.getHospitalCode());
        assertEquals("LEGACY001", ctx.getLegacyOrgCode());
    }

    @Test
    void resolve_legacyOrgCodeDoesNotOverrideHospitalCode() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Hospital-Code")).thenReturn("HOSP001");
        when(request.getHeader("X-Org-Code")).thenReturn("LEGACY001");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("HOSP001", ctx.getHospitalCode());
        assertEquals("LEGACY001", ctx.getLegacyOrgCode());
    }

    @Test
    void resolve_effectiveScopeDepartment() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Department-Code")).thenReturn("dept01");
        when(request.getHeader("X-Hospital-Code")).thenReturn("hosp01");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("DEPARTMENT", ctx.getEffectiveScopeLevel());
        assertEquals("dept01", ctx.getEffectiveScopeCode());
    }

    @Test
    void resolve_effectiveScopeSite() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Site-Code")).thenReturn("site01");
        when(request.getHeader("X-Hospital-Code")).thenReturn("hosp01");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("SITE", ctx.getEffectiveScopeLevel());
        assertEquals("site01", ctx.getEffectiveScopeCode());
    }

    @Test
    void resolve_effectiveScopeCampus() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Campus-Code")).thenReturn("camp01");
        when(request.getHeader("X-Hospital-Code")).thenReturn("hosp01");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("CAMPUS", ctx.getEffectiveScopeLevel());
        assertEquals("camp01", ctx.getEffectiveScopeCode());
    }

    @Test
    void resolve_effectiveScopeGroup() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Group-Code")).thenReturn("group01");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("GROUP", ctx.getEffectiveScopeLevel());
        assertEquals("group01", ctx.getEffectiveScopeCode());
    }

    @Test
    void resolve_effectiveScopePlatform() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        // 默认 hospitalCode = DEFAULT_HOSPITAL, 所以 scope 是 HOSPITAL
        assertEquals("HOSPITAL", ctx.getEffectiveScopeLevel());
    }

    @Test
    void resolve_warnings_departmentWithoutParentContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Department-Code")).thenReturn("dept01");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertFalse(ctx.getWarnings().isEmpty());
        assertTrue(ctx.getWarnings().stream()
                .anyMatch(w -> w.contains("department_code") && w.contains("site/campus/hospital")));
    }

    @Test
    void resolve_warnings_siteWithoutCampusOrHospital() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Site-Code")).thenReturn("site01");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertTrue(ctx.getWarnings().stream()
                .anyMatch(w -> w.contains("site_code") && w.contains("campus/hospital")));
    }

    @Test
    void resolve_warnings_campusWithoutHospital() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Campus-Code")).thenReturn("camp01");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertTrue(ctx.getWarnings().stream()
                .anyMatch(w -> w.contains("campus_code") && w.contains("hospital")));
    }

    @Test
    void resolve_noWarnings_whenFullHierarchyProvided() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Hospital-Code")).thenReturn("hosp01");
        when(request.getHeader("X-Campus-Code")).thenReturn("camp01");
        when(request.getHeader("X-Site-Code")).thenReturn("site01");
        when(request.getHeader("X-Department-Code")).thenReturn("dept01");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertTrue(ctx.getWarnings().isEmpty());
    }

    @Test
    void resolve_blankHeaderTreatedAsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn("  ");
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("default", ctx.getTenantId());
    }

    @Test
    void resolve_blankQueryParamTreatedAsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("tenant_id")).thenReturn("  ");
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolve(request);

        assertEquals("default", ctx.getTenantId());
    }

    // ===== resolveWithBody() 测试 =====

    @Test
    void resolveWithBody_nullBody_returnsResolvedContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolveWithBody(request, null);

        assertEquals("default", ctx.getTenantId());
        assertEquals("DEFAULT", ctx.getSource());
    }

    @Test
    void resolveWithBody_emptyBody_returnsResolvedContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx = service.resolveWithBody(request, Collections.emptyMap());

        assertEquals("DEFAULT", ctx.getSource());
    }

    @Test
    void resolveWithBody_bodyOverridesHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, Object> body = new HashMap<>();
        body.put("tenant_id", "body-tenant");
        body.put("hospital_code", "body-hosp");

        OrganizationContext ctx = service.resolveWithBody(request, body);

        assertEquals("body-tenant", ctx.getTenantId());
        assertEquals("body-hosp", ctx.getHospitalCode());
        assertEquals("BODY", ctx.getSource());
    }

    @Test
    void resolveWithBody_bodyCamelCaseKeys() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, Object> body = new HashMap<>();
        body.put("tenantId", "camel-tenant");
        body.put("groupCode", "camel-group");

        OrganizationContext ctx = service.resolveWithBody(request, body);

        assertEquals("camel-tenant", ctx.getTenantId());
        assertEquals("camel-group", ctx.getGroupCode());
    }

    @Test
    void resolveWithBody_bodyLegacyOrgCodeFillsHospitalCode() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, Object> body = new HashMap<>();
        body.put("org_code", "LEGACY_ORG");

        OrganizationContext ctx = service.resolveWithBody(request, body);

        assertEquals("LEGACY_ORG", ctx.getHospitalCode());
        assertEquals("LEGACY_ORG", ctx.getLegacyOrgCode());
    }

    @Test
    void resolveWithBody_bodyLegacyOrgCodeDoesNotOverrideExistingHospital() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, Object> body = new HashMap<>();
        body.put("hospital_code", "HOSP001");
        body.put("org_code", "LEGACY001");

        OrganizationContext ctx = service.resolveWithBody(request, body);

        assertEquals("HOSP001", ctx.getHospitalCode());
        assertEquals("LEGACY001", ctx.getLegacyOrgCode());
    }

    @Test
    void resolveWithBody_bodySetsAllFields() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, Object> body = new HashMap<>();
        body.put("tenant_id", "t1");
        body.put("group_code", "g1");
        body.put("hospital_code", "h1");
        body.put("campus_code", "c1");
        body.put("site_code", "s1");
        body.put("department_code", "d1");

        OrganizationContext ctx = service.resolveWithBody(request, body);

        assertEquals("t1", ctx.getTenantId());
        assertEquals("g1", ctx.getGroupCode());
        assertEquals("h1", ctx.getHospitalCode());
        assertEquals("c1", ctx.getCampusCode());
        assertEquals("s1", ctx.getSiteCode());
        assertEquals("d1", ctx.getDepartmentCode());
        assertEquals("DEPARTMENT", ctx.getEffectiveScopeLevel());
        assertEquals("BODY", ctx.getSource());
    }

    // ===== resolveTenantId() 测试 =====

    @Test
    void resolveTenantId_returnsTenantIdString() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn("tenant-123");
        when(request.getHeader(anyString())).thenReturn(null);

        String tenantId = service.resolveTenantId(request);

        assertEquals("tenant-123", tenantId);
    }

    // ===== getTenantId() (Long) 测试 =====

    @Test
    void getTenantId_validLong_returnsLongValue() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn("42");
        when(request.getHeader(anyString())).thenReturn(null);

        Long tenantId = service.getTenantId(request);

        assertEquals(42L, tenantId);
    }

    @Test
    void getTenantId_invalidLong_returnsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn("not-a-number");
        when(request.getHeader(anyString())).thenReturn(null);

        Long tenantId = service.getTenantId(request);

        assertNull(tenantId);
    }

    @Test
    void getTenantId_emptyString_returnsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn("  ");
        when(request.getHeader(anyString())).thenReturn(null);

        Long tenantId = service.getTenantId(request);

        // blank header -> default "default" -> not a number -> null
        assertNull(tenantId);
    }

    // ===== getContext() 测试 =====

    @Test
    void getContext_returnsSameAsResolve() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        OrganizationContext ctx1 = service.resolve(request);
        OrganizationContext ctx2 = service.getContext(request);

        assertEquals(ctx1.getTenantId(), ctx2.getTenantId());
        assertEquals(ctx1.getHospitalCode(), ctx2.getHospitalCode());
    }

    // ===== applyExplicitFilters() 测试 =====

    @Test
    void applyExplicitFilters_nullFilters_doesNothing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        assertDoesNotThrow(() -> service.applyExplicitFilters(null, request));
    }

    @Test
    void applyExplicitFilters_nullRequest_doesNothing() {
        Map<String, String> filters = new HashMap<>();
        assertDoesNotThrow(() -> service.applyExplicitFilters(filters, null));
    }

    @Test
    void applyExplicitFilters_withHeaders_populatesFilters() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Tenant-Id")).thenReturn("t1");
        when(request.getHeader("X-Hospital-Code")).thenReturn("h1");
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, String> filters = new HashMap<>();
        service.applyExplicitFilters(filters, request);

        assertEquals("t1", filters.get("tenantId"));
        assertEquals("h1", filters.get("hospitalCode"));
    }

    @Test
    void applyExplicitFilters_hospitalCodeFallsBackToLegacyOrgCode() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Org-Code")).thenReturn("legacy01");
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, String> filters = new HashMap<>();
        service.applyExplicitFilters(filters, request);

        assertEquals("legacy01", filters.get("hospitalCode"));
        assertEquals("legacy01", filters.get("legacyOrgCode"));
    }

    @Test
    void applyExplicitFilters_hospitalCodePreferredOverLegacyOrgCode() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader("X-Hospital-Code")).thenReturn("hosp01");
        when(request.getHeader("X-Org-Code")).thenReturn("legacy01");
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, String> filters = new HashMap<>();
        service.applyExplicitFilters(filters, request);

        assertEquals("hosp01", filters.get("hospitalCode"));
        assertEquals("legacy01", filters.get("legacyOrgCode"));
    }

    @Test
    void applyExplicitFilters_withQueryParams_populatesFilters() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("tenant_id")).thenReturn("qt1");
        when(request.getParameter("scope_level")).thenReturn("HOSPITAL");
        when(request.getParameter("scope_code")).thenReturn("hosp01");
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, String> filters = new HashMap<>();
        service.applyExplicitFilters(filters, request);

        assertEquals("qt1", filters.get("tenantId"));
        assertEquals("HOSPITAL", filters.get("scopeLevel"));
        assertEquals("hosp01", filters.get("scopeCode"));
    }

    @Test
    void applyExplicitFilters_nullValuesNotAdded() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, String> filters = new HashMap<>();
        service.applyExplicitFilters(filters, request);

        assertFalse(filters.containsKey("tenantId"));
        assertFalse(filters.containsKey("hospitalCode"));
    }

    // ===== orgContextView() 测试 =====

    @Test
    void orgContextView_containsExpectedKeys() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, Object> view = service.orgContextView(request);

        assertTrue(view.containsKey("tenant_id"));
        assertTrue(view.containsKey("model"));
        assertTrue(view.containsKey("supported_scope_levels"));
        assertTrue(view.containsKey("scope_level_names"));
        assertTrue(view.containsKey("header_contract"));
        assertTrue(view.containsKey("db_only_supported"));
        assertEquals(true, view.get("db_only_supported"));
    }

    @Test
    void orgContextView_supportedScopeLevels() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, Object> view = service.orgContextView(request);

        @SuppressWarnings("unchecked")
        List<String> levels = (List<String>) view.get("supported_scope_levels");
        assertEquals(Arrays.asList("PLATFORM", "GROUP", "HOSPITAL", "CAMPUS", "SITE", "DEPARTMENT"), levels);
    }

    @Test
    void orgContextView_headerContract() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, Object> view = service.orgContextView(request);

        @SuppressWarnings("unchecked")
        Map<String, Object> headers = (Map<String, Object>) view.get("header_contract");
        assertEquals("X-Tenant-Id", headers.get("tenant_id"));
        assertEquals("X-Hospital-Code", headers.get("hospital_code"));
        assertEquals("X-Org-Code", headers.get("legacy_org_code"));
    }

    @Test
    void orgContextView_modelContainsLevels() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);

        Map<String, Object> view = service.orgContextView(request);

        @SuppressWarnings("unchecked")
        Map<String, Object> model = (Map<String, Object>) view.get("model");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> levels = (List<Map<String, Object>>) model.get("levels");
        assertEquals(6, levels.size());
        assertEquals("PLATFORM", levels.get(0).get("level"));
        assertEquals("DEPARTMENT", levels.get(5).get("level"));
    }

    // ===== 常量验证 =====

    @Test
    void headerConstants_haveExpectedValues() {
        assertEquals("X-Tenant-Id", OrganizationContextService.HEADER_TENANT_ID);
        assertEquals("X-Group-Code", OrganizationContextService.HEADER_GROUP_CODE);
        assertEquals("X-Hospital-Code", OrganizationContextService.HEADER_HOSPITAL_CODE);
        assertEquals("X-Campus-Code", OrganizationContextService.HEADER_CAMPUS_CODE);
        assertEquals("X-Site-Code", OrganizationContextService.HEADER_SITE_CODE);
        assertEquals("X-Department-Code", OrganizationContextService.HEADER_DEPARTMENT_CODE);
        assertEquals("X-Org-Code", OrganizationContextService.HEADER_ORG_CODE);
    }
}
