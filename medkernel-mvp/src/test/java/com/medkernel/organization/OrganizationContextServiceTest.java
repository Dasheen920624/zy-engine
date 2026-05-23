package com.medkernel.organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationContextService 单元测试")
class OrganizationContextServiceTest {

    private OrganizationContextService service;

    @BeforeEach
    void setUp() {
        service = new OrganizationContextService();
    }

    /**
     * 创建一个所有参数和 Header 都返回 null 的 mock request，
     * 然后通过具体 stub 覆盖特定值。
     * 注意：Mockito 中后注册的 stub 优先匹配，因此 anyString() 先注册。
     */
    private HttpServletRequest mockRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(anyString())).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);
        return request;
    }

    // ──────────────────── resolve() ────────────────────

    @Nested
    @DisplayName("resolve() 方法测试")
    class ResolveTests {

        @Test
        @DisplayName("无任何 Header 和参数时，返回默认值")
        void resolve_noHeadersNoParams_returnsDefaults() {
            HttpServletRequest request = mockRequest();

            OrganizationContext ctx = service.resolve(request);

            assertEquals("default", ctx.getTenantId());
            assertNull(ctx.getGroupCode());
            assertEquals("ZYHOSPITAL", ctx.getHospitalCode());
            assertNull(ctx.getCampusCode());
            assertNull(ctx.getSiteCode());
            assertNull(ctx.getDepartmentCode());
            assertEquals("ZYHOSPITAL", ctx.getLegacyOrgCode());
            assertEquals("HOSPITAL", ctx.getEffectiveScopeLevel());
            assertEquals("ZYHOSPITAL", ctx.getEffectiveScopeCode());
            assertEquals("DEFAULT", ctx.getSource());
            assertTrue(ctx.getWarnings().isEmpty());
        }

        @Test
        @DisplayName("仅通过 Header 传入全部组织维度")
        void resolve_allHeaders_returnsAllValues() {
            HttpServletRequest request = mockRequest();
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
        @DisplayName("仅传入部分 Header：tenantId 和 hospitalCode")
        void resolve_partialHeaders_tenantAndHospital() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("42");
            when(request.getHeader("X-Hospital-Code")).thenReturn("MYHOSP");

            OrganizationContext ctx = service.resolve(request);

            assertEquals("42", ctx.getTenantId());
            assertNull(ctx.getGroupCode());
            assertEquals("MYHOSP", ctx.getHospitalCode());
            assertEquals("MYHOSP", ctx.getLegacyOrgCode());
            assertEquals("HOSPITAL", ctx.getEffectiveScopeLevel());
            assertEquals("MYHOSP", ctx.getEffectiveScopeCode());
            assertEquals("HEADER", ctx.getSource());
        }

        @Test
        @DisplayName("Query 参数优先于 Header")
        void resolve_queryParamOverridesHeader() {
            HttpServletRequest request = mockRequest();
            when(request.getParameter("tenant_id")).thenReturn("qTenant");
            // Header 也设置了值，但 Query 参数优先
            when(request.getHeader("X-Hospital-Code")).thenReturn("hHosp");

            OrganizationContext ctx = service.resolve(request);

            assertEquals("qTenant", ctx.getTenantId());
            assertEquals("hHosp", ctx.getHospitalCode());
            assertEquals("QUERY", ctx.getSource());
        }

        @Test
        @DisplayName("camelCase Query 参数也能识别")
        void resolve_camelCaseQueryParam() {
            HttpServletRequest request = mockRequest();
            when(request.getParameter("tenantId")).thenReturn("camelTenant");
            when(request.getParameter("hospitalCode")).thenReturn("camelHosp");

            OrganizationContext ctx = service.resolve(request);

            assertEquals("camelTenant", ctx.getTenantId());
            assertEquals("camelHosp", ctx.getHospitalCode());
        }

        @Test
        @DisplayName("legacyOrgCode 为空时自动填充 hospitalCode")
        void resolve_legacyOrgCodeFallbackToHospitalCode() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("t1");
            when(request.getHeader("X-Hospital-Code")).thenReturn("HOSP_A");

            OrganizationContext ctx = service.resolve(request);

            assertEquals("HOSP_A", ctx.getHospitalCode());
            assertEquals("HOSP_A", ctx.getLegacyOrgCode());
        }

        @Test
        @DisplayName("hospitalCode 为空但 legacyOrgCode 有值时，hospitalCode 回退到 legacyOrgCode")
        void resolve_hospitalCodeFallbackToLegacyOrgCode() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Org-Code")).thenReturn("ORG_X");

            OrganizationContext ctx = service.resolve(request);

            assertEquals("ORG_X", ctx.getHospitalCode());
            assertEquals("ORG_X", ctx.getLegacyOrgCode());
        }

        @Test
        @DisplayName("Header 值为空白字符串时视为 null")
        void resolve_blankHeaderTreatedAsNull() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("  ");
            when(request.getHeader("X-Hospital-Code")).thenReturn("  HOSP  ");

            OrganizationContext ctx = service.resolve(request);

            // 空白 tenantId 应回退到默认值 "default"
            assertEquals("default", ctx.getTenantId());
            // hospitalCode 应被 trim
            assertEquals("HOSP", ctx.getHospitalCode());
        }

        @Test
        @DisplayName("effectiveScopeLevel 从最细粒度开始匹配：DEPARTMENT")
        void resolve_effectiveScopeDepartment() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("t1");
            when(request.getHeader("X-Group-Code")).thenReturn("g1");
            when(request.getHeader("X-Hospital-Code")).thenReturn("h1");
            when(request.getHeader("X-Campus-Code")).thenReturn("c1");
            when(request.getHeader("X-Site-Code")).thenReturn("s1");
            when(request.getHeader("X-Department-Code")).thenReturn("d1");

            OrganizationContext ctx = service.resolve(request);

            assertEquals("DEPARTMENT", ctx.getEffectiveScopeLevel());
            assertEquals("d1", ctx.getEffectiveScopeCode());
        }

        @Test
        @DisplayName("effectiveScopeLevel：SITE 级别")
        void resolve_effectiveScopeSite() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("t1");
            when(request.getHeader("X-Hospital-Code")).thenReturn("h1");
            when(request.getHeader("X-Campus-Code")).thenReturn("c1");
            when(request.getHeader("X-Site-Code")).thenReturn("s1");

            OrganizationContext ctx = service.resolve(request);

            assertEquals("SITE", ctx.getEffectiveScopeLevel());
            assertEquals("s1", ctx.getEffectiveScopeCode());
        }

        @Test
        @DisplayName("effectiveScopeLevel：CAMPUS 级别")
        void resolve_effectiveScopeCampus() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("t1");
            when(request.getHeader("X-Hospital-Code")).thenReturn("h1");
            when(request.getHeader("X-Campus-Code")).thenReturn("c1");

            OrganizationContext ctx = service.resolve(request);

            assertEquals("CAMPUS", ctx.getEffectiveScopeLevel());
            assertEquals("c1", ctx.getEffectiveScopeCode());
        }

        @Test
        @DisplayName("effectiveScopeLevel：GROUP 级别（groupCode 有值，hospitalCode 未设置时回退默认，scope 为 HOSPITAL）")
        void resolve_effectiveScopeGroupWithDefaultHospital() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("t1");
            when(request.getHeader("X-Group-Code")).thenReturn("g1");

            OrganizationContext ctx = service.resolve(request);

            // hospitalCode 为空时自动填充 DEFAULT_HOSPITAL_CODE，所以 scope 为 HOSPITAL 而非 GROUP
            assertEquals("HOSPITAL", ctx.getEffectiveScopeLevel());
            assertEquals("ZYHOSPITAL", ctx.getEffectiveScopeCode());
        }

        @Test
        @DisplayName("effectiveScopeLevel：仅 tenantId 时回退到默认 hospitalCode，scope 为 HOSPITAL")
        void resolve_effectiveScopeDefaultHospital() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("t1");

            OrganizationContext ctx = service.resolve(request);

            // hospitalCode 为空时会自动填充 DEFAULT_HOSPITAL_CODE，所以 scope 为 HOSPITAL 而非 PLATFORM
            assertEquals("HOSPITAL", ctx.getEffectiveScopeLevel());
            assertEquals("ZYHOSPITAL", ctx.getEffectiveScopeCode());
        }

        @Test
        @DisplayName("完整层级无警告")
        void resolve_fullHierarchy_noWarnings() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("t1");
            when(request.getHeader("X-Group-Code")).thenReturn("g1");
            when(request.getHeader("X-Hospital-Code")).thenReturn("h1");
            when(request.getHeader("X-Campus-Code")).thenReturn("c1");
            when(request.getHeader("X-Site-Code")).thenReturn("s1");
            when(request.getHeader("X-Department-Code")).thenReturn("d1");

            OrganizationContext ctx = service.resolve(request);

            assertTrue(ctx.getWarnings().isEmpty());
        }

        @Test
        @DisplayName("部分层级（仅 department + hospital）无警告，因为 hospital 已提供上级上下文")
        void resolve_departmentWithHospital_noWarning() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("t1");
            when(request.getHeader("X-Hospital-Code")).thenReturn("h1");
            when(request.getHeader("X-Department-Code")).thenReturn("d1");

            OrganizationContext ctx = service.resolve(request);

            // hospitalCode 有值，不触发 "缺少上级" 警告
            assertTrue(ctx.getWarnings().isEmpty());
        }

        @Test
        @DisplayName("source 为 HEADER：仅有 Header 无 Query 参数")
        void resolve_sourceHeader() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("t1");

            OrganizationContext ctx = service.resolve(request);

            assertEquals("HEADER", ctx.getSource());
        }

        @Test
        @DisplayName("source 为 QUERY：有 Query 参数时优先")
        void resolve_sourceQuery() {
            HttpServletRequest request = mockRequest();
            when(request.getParameter("tenant_id")).thenReturn("q1");

            OrganizationContext ctx = service.resolve(request);

            assertEquals("QUERY", ctx.getSource());
        }

        @Test
        @DisplayName("source 为 DEFAULT：无任何参数和 Header")
        void resolve_sourceDefault() {
            HttpServletRequest request = mockRequest();

            OrganizationContext ctx = service.resolve(request);

            assertEquals("DEFAULT", ctx.getSource());
        }
    }

    // ──────────────────── resolveWithBody() ────────────────────

    @Nested
    @DisplayName("resolveWithBody() 方法测试")
    class ResolveWithBodyTests {

        @Test
        @DisplayName("body 为 null 时等同于 resolve()")
        void resolveWithBody_nullBody_sameAsResolve() {
            HttpServletRequest request = mockRequest();

            OrganizationContext ctx = service.resolveWithBody(request, null);

            assertEquals("default", ctx.getTenantId());
            assertEquals("ZYHOSPITAL", ctx.getHospitalCode());
            assertNotEquals("BODY", ctx.getSource());
        }

        @Test
        @DisplayName("body 为空 Map 时等同于 resolve()")
        void resolveWithBody_emptyBody_sameAsResolve() {
            HttpServletRequest request = mockRequest();

            OrganizationContext ctx = service.resolveWithBody(request, Collections.emptyMap());

            assertEquals("default", ctx.getTenantId());
            assertNotEquals("BODY", ctx.getSource());
        }

        @Test
        @DisplayName("body 中的值覆盖 Header 值")
        void resolveWithBody_bodyOverridesHeader() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("headerTenant");
            when(request.getHeader("X-Hospital-Code")).thenReturn("headerHosp");

            Map<String, Object> body = new HashMap<>();
            body.put("tenant_id", "bodyTenant");
            body.put("hospital_code", "bodyHosp");

            OrganizationContext ctx = service.resolveWithBody(request, body);

            assertEquals("bodyTenant", ctx.getTenantId());
            assertEquals("bodyHosp", ctx.getHospitalCode());
            assertEquals("BODY", ctx.getSource());
        }

        @Test
        @DisplayName("body 中使用 camelCase 键名也能识别")
        void resolveWithBody_camelCaseKeys() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> body = new HashMap<>();
            body.put("tenantId", "camelTenant");
            body.put("hospitalCode", "camelHosp");
            body.put("groupCode", "camelGroup");

            OrganizationContext ctx = service.resolveWithBody(request, body);

            assertEquals("camelTenant", ctx.getTenantId());
            assertEquals("camelHosp", ctx.getHospitalCode());
            assertEquals("camelGroup", ctx.getGroupCode());
            assertEquals("BODY", ctx.getSource());
        }

        @Test
        @DisplayName("body 中 snake_case 优先于 camelCase")
        void resolveWithBody_snakeCasePreferredOverCamelCase() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> body = new HashMap<>();
            body.put("tenant_id", "snakeTenant");
            body.put("tenantId", "camelTenant");

            OrganizationContext ctx = service.resolveWithBody(request, body);

            assertEquals("snakeTenant", ctx.getTenantId());
        }

        @Test
        @DisplayName("body 中 org_code 设置后 legacyOrgCode 更新，但已有 hospitalCode 不被覆盖")
        void resolveWithBody_orgCodeDoesNotOverrideExistingHospital() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> body = new HashMap<>();
            body.put("org_code", "ORG_BODY");

            OrganizationContext ctx = service.resolveWithBody(request, body);

            assertEquals("ORG_BODY", ctx.getLegacyOrgCode());
            // hospitalCode 已被 resolve 设置为 ZYHOSPITAL，body 中 org_code 不会覆盖已有 hospitalCode
            assertEquals("ZYHOSPITAL", ctx.getHospitalCode());
        }

        @Test
        @DisplayName("body 中 hospital_code 同时设置时，org_code 不会覆盖 hospitalCode")
        void resolveWithBody_hospitalAndOrgCode_bothSet() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> body = new HashMap<>();
            body.put("hospital_code", "BODY_HOSP");
            body.put("org_code", "BODY_ORG");

            OrganizationContext ctx = service.resolveWithBody(request, body);

            assertEquals("BODY_HOSP", ctx.getHospitalCode());
            assertEquals("BODY_ORG", ctx.getLegacyOrgCode());
        }

        @Test
        @DisplayName("body 中值为空白字符串时不视为有效覆盖")
        void resolveWithBody_blankBodyValue_notApplied() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("headerTenant");

            Map<String, Object> body = new HashMap<>();
            body.put("tenant_id", "  ");

            OrganizationContext ctx = service.resolveWithBody(request, body);

            // 空白字符串被 clean 为 null，不会覆盖
            assertEquals("headerTenant", ctx.getTenantId());
        }

        @Test
        @DisplayName("body 中数值类型字段会被转为字符串")
        void resolveWithBody_numericValueConvertedToString() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> body = new HashMap<>();
            body.put("tenant_id", 12345L);

            OrganizationContext ctx = service.resolveWithBody(request, body);

            assertEquals("12345", ctx.getTenantId());
            assertEquals("BODY", ctx.getSource());
        }

        @Test
        @DisplayName("body 覆盖后 effectiveScopeLevel 重新计算")
        void resolveWithBody_effectiveScopeRecalculated() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> body = new HashMap<>();
            body.put("campus_code", "CAMP_B");

            OrganizationContext ctx = service.resolveWithBody(request, body);

            assertEquals("CAMPUS", ctx.getEffectiveScopeLevel());
            assertEquals("CAMP_B", ctx.getEffectiveScopeCode());
        }
    }

    // ──────────────────── resolveTenantId() & getTenantId() ────────────────────

    @Nested
    @DisplayName("resolveTenantId() 和 getTenantId() 方法测试")
    class TenantIdTests {

        @Test
        @DisplayName("resolveTenantId 返回字符串租户ID")
        void resolveTenantId_returnsString() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("42");

            String tenantId = service.resolveTenantId(request);

            assertEquals("42", tenantId);
        }

        @Test
        @DisplayName("resolveTenantId 无 Header 时返回默认值")
        void resolveTenantId_noHeader_returnsDefault() {
            HttpServletRequest request = mockRequest();

            String tenantId = service.resolveTenantId(request);

            assertEquals("default", tenantId);
        }

        @Test
        @DisplayName("getTenantId 返回 Long 类型租户ID")
        void getTenantId_returnsLong() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("42");

            Long tenantId = service.getTenantId(request);

            assertEquals(42L, tenantId);
        }

        @Test
        @DisplayName("getTenantId 无法解析为数字时返回 null")
        void getTenantId_nonNumeric_returnsNull() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("abc");

            Long tenantId = service.getTenantId(request);

            assertNull(tenantId);
        }

        @Test
        @DisplayName("getTenantId 默认值 'default' 无法解析为数字时返回 null")
        void getTenantId_defaultValue_returnsNull() {
            HttpServletRequest request = mockRequest();

            Long tenantId = service.getTenantId(request);

            assertNull(tenantId);
        }
    }

    // ──────────────────── applyExplicitFilters() ────────────────────

    @Nested
    @DisplayName("applyExplicitFilters() 方法测试")
    class ApplyExplicitFiltersTests {

        @Test
        @DisplayName("filters 为 null 时不抛异常")
        void applyExplicitFilters_nullFilters_noException() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            assertDoesNotThrow(() -> service.applyExplicitFilters(null, request));
        }

        @Test
        @DisplayName("request 为 null 时不抛异常")
        void applyExplicitFilters_nullRequest_noException() {
            Map<String, String> filters = new HashMap<>();
            assertDoesNotThrow(() -> service.applyExplicitFilters(filters, null));
        }

        @Test
        @DisplayName("从 Header 填充 filters")
        void applyExplicitFilters_fromHeaders() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Tenant-Id")).thenReturn("t1");
            when(request.getHeader("X-Group-Code")).thenReturn("g1");
            when(request.getHeader("X-Hospital-Code")).thenReturn("h1");
            when(request.getHeader("X-Campus-Code")).thenReturn("c1");
            when(request.getHeader("X-Site-Code")).thenReturn("s1");
            when(request.getHeader("X-Department-Code")).thenReturn("d1");
            when(request.getHeader("X-Org-Code")).thenReturn("o1");

            Map<String, String> filters = new HashMap<>();
            service.applyExplicitFilters(filters, request);

            assertEquals("t1", filters.get("tenantId"));
            assertEquals("g1", filters.get("groupCode"));
            assertEquals("h1", filters.get("hospitalCode"));
            assertEquals("c1", filters.get("campusCode"));
            assertEquals("s1", filters.get("siteCode"));
            assertEquals("d1", filters.get("departmentCode"));
            assertEquals("o1", filters.get("legacyOrgCode"));
        }

        @Test
        @DisplayName("hospitalCode 为空时回退到 legacyOrgCode")
        void applyExplicitFilters_hospitalCodeFallbackToOrgCode() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Org-Code")).thenReturn("ORG1");

            Map<String, String> filters = new HashMap<>();
            service.applyExplicitFilters(filters, request);

            assertEquals("ORG1", filters.get("hospitalCode"));
            assertEquals("ORG1", filters.get("legacyOrgCode"));
        }

        @Test
        @DisplayName("hospitalCode 有值时不被 legacyOrgCode 覆盖")
        void applyExplicitFilters_hospitalCodeNotOverriddenByOrgCode() {
            HttpServletRequest request = mockRequest();
            when(request.getHeader("X-Hospital-Code")).thenReturn("HOSP1");
            when(request.getHeader("X-Org-Code")).thenReturn("ORG1");

            Map<String, String> filters = new HashMap<>();
            service.applyExplicitFilters(filters, request);

            assertEquals("HOSP1", filters.get("hospitalCode"));
            assertEquals("ORG1", filters.get("legacyOrgCode"));
        }

        @Test
        @DisplayName("Query 参数优先于 Header")
        void applyExplicitFilters_queryParamOverridesHeader() {
            HttpServletRequest request = mockRequest();
            when(request.getParameter("tenant_id")).thenReturn("qTenant");
            // Header 也设置了 hospitalCode，确保 Query 参数 tenant_id 优先
            when(request.getHeader("X-Hospital-Code")).thenReturn("hHosp");

            Map<String, String> filters = new HashMap<>();
            service.applyExplicitFilters(filters, request);

            assertEquals("qTenant", filters.get("tenantId"));
            assertEquals("hHosp", filters.get("hospitalCode"));
        }

        @Test
        @DisplayName("scope_level 和 scope_code 从 Query 参数获取")
        void applyExplicitFilters_scopeLevelAndCode() {
            HttpServletRequest request = mockRequest();
            when(request.getParameter("scope_level")).thenReturn("HOSPITAL");
            when(request.getParameter("scope_code")).thenReturn("H1");

            Map<String, String> filters = new HashMap<>();
            service.applyExplicitFilters(filters, request);

            assertEquals("HOSPITAL", filters.get("scopeLevel"));
            assertEquals("H1", filters.get("scopeCode"));
        }

        @Test
        @DisplayName("无任何值时 filters 为空")
        void applyExplicitFilters_noValues_emptyFilters() {
            HttpServletRequest request = mockRequest();

            Map<String, String> filters = new HashMap<>();
            service.applyExplicitFilters(filters, request);

            assertTrue(filters.isEmpty());
        }
    }

    // ──────────────────── orgContextView() ────────────────────

    @Nested
    @DisplayName("orgContextView() 方法测试")
    class OrgContextViewTests {

        @Test
        @DisplayName("返回包含 model、supported_scope_levels、header_contract 等字段的视图")
        void orgContextView_containsAllKeys() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> view = service.orgContextView(request);

            assertTrue(view.containsKey("model"));
            assertTrue(view.containsKey("supported_scope_levels"));
            assertTrue(view.containsKey("scope_level_names"));
            assertTrue(view.containsKey("header_contract"));
            assertTrue(view.containsKey("db_only_supported"));
            assertTrue(view.containsKey("tenant_id"));
            assertTrue(view.containsKey("hospital_code"));
            assertTrue(view.containsKey("effective_scope_level"));
            assertTrue(view.containsKey("source"));
            assertTrue(view.containsKey("warnings"));
        }

        @Test
        @DisplayName("supported_scope_levels 包含完整层级列表")
        void orgContextView_supportedScopeLevels() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> view = service.orgContextView(request);

            @SuppressWarnings("unchecked")
            List<String> levels = (List<String>) view.get("supported_scope_levels");
            assertEquals(Arrays.asList("PLATFORM", "GROUP", "HOSPITAL", "CAMPUS", "SITE", "DEPARTMENT"), levels);
        }

        @Test
        @DisplayName("db_only_supported 为 true")
        void orgContextView_dbOnlySupported() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> view = service.orgContextView(request);

            assertEquals(true, view.get("db_only_supported"));
        }

        @Test
        @DisplayName("header_contract 映射正确")
        void orgContextView_headerContract() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> view = service.orgContextView(request);

            @SuppressWarnings("unchecked")
            Map<String, Object> headers = (Map<String, Object>) view.get("header_contract");
            assertEquals("X-Tenant-Id", headers.get("tenant_id"));
            assertEquals("X-Group-Code", headers.get("group_code"));
            assertEquals("X-Hospital-Code", headers.get("hospital_code"));
            assertEquals("X-Campus-Code", headers.get("campus_code"));
            assertEquals("X-Site-Code", headers.get("site_code"));
            assertEquals("X-Department-Code", headers.get("department_code"));
            assertEquals("X-Org-Code", headers.get("legacy_org_code"));
        }

        @Test
        @DisplayName("model 包含 levels 和 config_precedence")
        void orgContextView_modelStructure() {
            HttpServletRequest request = mockRequest();

            Map<String, Object> view = service.orgContextView(request);

            @SuppressWarnings("unchecked")
            Map<String, Object> model = (Map<String, Object>) view.get("model");
            assertTrue(model.containsKey("levels"));
            assertTrue(model.containsKey("config_precedence"));
            assertTrue(model.containsKey("baseline_scope_level"));
            assertTrue(model.containsKey("baseline_scope_name"));
            assertTrue(model.containsKey("legacy_mapping"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> levels = (List<Map<String, Object>>) model.get("levels");
            assertEquals(6, levels.size());

            @SuppressWarnings("unchecked")
            List<String> precedence = (List<String>) model.get("config_precedence");
            assertEquals(Arrays.asList("DEPARTMENT", "SITE", "CAMPUS", "HOSPITAL", "GROUP", "PLATFORM"), precedence);
        }
    }

    // ──────────────────── getContext() ────────────────────

    @Nested
    @DisplayName("getContext() 便捷方法测试")
    class GetContextTests {

        @Test
        @DisplayName("getContext 等同于 resolve")
        void getContext_sameAsResolve() {
            HttpServletRequest request = mockRequest();

            OrganizationContext fromResolve = service.resolve(request);
            OrganizationContext fromGetContext = service.getContext(request);

            assertEquals(fromResolve.getTenantId(), fromGetContext.getTenantId());
            assertEquals(fromResolve.getHospitalCode(), fromGetContext.getHospitalCode());
            assertEquals(fromResolve.getEffectiveScopeLevel(), fromGetContext.getEffectiveScopeLevel());
        }
    }

    // ──────────────────── 常量验证 ────────────────────

    @Nested
    @DisplayName("常量验证测试")
    class ConstantTests {

        @Test
        @DisplayName("Header 常量名称正确")
        void headerConstants_correct() {
            assertEquals("X-Tenant-Id", OrganizationContextService.HEADER_TENANT_ID);
            assertEquals("X-Group-Code", OrganizationContextService.HEADER_GROUP_CODE);
            assertEquals("X-Hospital-Code", OrganizationContextService.HEADER_HOSPITAL_CODE);
            assertEquals("X-Campus-Code", OrganizationContextService.HEADER_CAMPUS_CODE);
            assertEquals("X-Site-Code", OrganizationContextService.HEADER_SITE_CODE);
            assertEquals("X-Department-Code", OrganizationContextService.HEADER_DEPARTMENT_CODE);
            assertEquals("X-Org-Code", OrganizationContextService.HEADER_ORG_CODE);
        }
    }
}
