package com.medkernel.organization;

import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrgOverrideService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class OrgOverrideServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private OrganizationDirectoryService organizationDirectoryService;

    private OrgOverrideService service;

    @BeforeEach
    void setUp() {
        service = new OrgOverrideService(persistenceService, organizationDirectoryService);
    }

    // ===== importEntries() 测试 =====

    @Test
    void importEntries_withListInput_importsSuccessfully() {
        Map<String, Object> entry1 = new LinkedHashMap<>();
        entry1.put("scope_level", "HOSPITAL");
        entry1.put("scope_code", "HOSP01");
        entry1.put("override_key", "theme.color");
        entry1.put("override_value", "#ff0000");

        List<Map<String, Object>> request = new ArrayList<>();
        request.add(entry1);

        List<Map<String, Object>> result = service.importEntries(request);

        assertEquals(1, result.size());
        assertEquals("HOSPITAL", result.get(0).get("scope_level"));
        assertEquals("HOSP01", result.get(0).get("scope_code"));
        assertEquals("theme.color", result.get(0).get("override_key"));
        assertEquals("#ff0000", result.get(0).get("override_value"));
        verify(persistenceService).saveAuditLog(eq("ORG_OVERRIDE"), eq("IMPORT"), eq("ORG_OVERRIDE_BATCH"),
                eq("default"), isNull(), isNull(), isNull(), any(Map.class));
    }

    @Test
    void importEntries_withMapInputAndNestedEntries() {
        Map<String, Object> entry1 = new LinkedHashMap<>();
        entry1.put("scope_level", "HOSPITAL");
        entry1.put("scope_code", "HOSP01");
        entry1.put("override_key", "theme.color");
        entry1.put("override_value", "#00ff00");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("tenant_id", "t1");
        request.put("operator_id", "admin");
        request.put("entries", Collections.singletonList(entry1));

        List<Map<String, Object>> result = service.importEntries(request);

        assertEquals(1, result.size());
        assertEquals("t1", result.get(0).get("tenant_id"));
    }

    @Test
    void importEntries_withMapInputNoEntriesKey_treatsBodyAsSingleEntry() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("scope_level", "PLATFORM");
        request.put("scope_code", "DEFAULT");
        request.put("override_key", "global.setting");
        request.put("override_value", "value1");
        request.put("tenant_id", "t1");

        List<Map<String, Object>> result = service.importEntries(request);

        assertEquals(1, result.size());
    }

    @Test
    void importEntries_emptyList_throwsException() {
        List<Map<String, Object>> request = new ArrayList<>();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importEntries(request));
        assertEquals("entries is required", ex.getMessage());
    }

    @Test
    void importEntries_unsupportedScopeLevel_throwsException() {
        Map<String, Object> entry1 = new LinkedHashMap<>();
        entry1.put("scope_level", "INVALID");
        entry1.put("scope_code", "CODE01");
        entry1.put("override_key", "key1");

        List<Map<String, Object>> request = Collections.singletonList(entry1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importEntries(request));
        assertTrue(ex.getMessage().contains("unsupported scope_level"));
    }

    @Test
    void importEntries_missingScopeLevel_throwsException() {
        Map<String, Object> entry1 = new LinkedHashMap<>();
        entry1.put("scope_code", "CODE01");
        entry1.put("override_key", "key1");

        List<Map<String, Object>> request = Collections.singletonList(entry1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importEntries(request));
        assertTrue(ex.getMessage().contains("scope_level is required"));
    }

    @Test
    void importEntries_missingScopeCode_throwsException() {
        Map<String, Object> entry1 = new LinkedHashMap<>();
        entry1.put("scope_level", "HOSPITAL");
        entry1.put("override_key", "key1");

        List<Map<String, Object>> request = Collections.singletonList(entry1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importEntries(request));
        assertTrue(ex.getMessage().contains("scope_code is required"));
    }

    @Test
    void importEntries_missingOverrideKey_throwsException() {
        Map<String, Object> entry1 = new LinkedHashMap<>();
        entry1.put("scope_level", "HOSPITAL");
        entry1.put("scope_code", "HOSP01");

        List<Map<String, Object>> request = Collections.singletonList(entry1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importEntries(request));
        assertTrue(ex.getMessage().contains("override_key is required"));
    }

    @Test
    void importEntries_camelCaseKeys() {
        Map<String, Object> entry1 = new LinkedHashMap<>();
        entry1.put("scopeLevel", "HOSPITAL");
        entry1.put("scopeCode", "HOSP01");
        entry1.put("assetType", "THEME");
        entry1.put("overrideKey", "theme.color");
        entry1.put("overrideValue", "#0000ff");

        List<Map<String, Object>> request = Collections.singletonList(entry1);

        List<Map<String, Object>> result = service.importEntries(request);

        assertEquals(1, result.size());
        assertEquals("THEME", result.get(0).get("asset_type"));
        assertEquals("#0000ff", result.get(0).get("override_value"));
    }

    @Test
    void importEntries_multipleEntries() {
        Map<String, Object> entry1 = new LinkedHashMap<>();
        entry1.put("scope_level", "HOSPITAL");
        entry1.put("scope_code", "HOSP01");
        entry1.put("override_key", "key1");
        entry1.put("override_value", "val1");

        Map<String, Object> entry2 = new LinkedHashMap<>();
        entry2.put("scope_level", "CAMPUS");
        entry2.put("scope_code", "CAMP01");
        entry2.put("override_key", "key2");
        entry2.put("override_value", "val2");

        List<Map<String, Object>> request = Arrays.asList(entry1, entry2);

        List<Map<String, Object>> result = service.importEntries(request);

        assertEquals(2, result.size());
    }

    @Test
    void importEntries_mapInputWithNonMapEntriesItem_ignored() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("entries", "not-a-list");

        // When "entries" is not a Collection, body itself is treated as single entry
        // but it lacks scope_level -> should throw
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importEntries(request));
        assertTrue(ex.getMessage().contains("scope_level is required"));
    }

    @Test
    void importEntries_auditFails_doesNotThrow() {
        doThrow(new RuntimeException("Audit error")).when(persistenceService)
                .saveAuditLog(anyString(), anyString(), anyString(), anyString(),
                        isNull(), isNull(), isNull(), any(Map.class));

        Map<String, Object> entry1 = new LinkedHashMap<>();
        entry1.put("scope_level", "HOSPITAL");
        entry1.put("scope_code", "HOSP01");
        entry1.put("override_key", "key1");

        assertDoesNotThrow(() -> service.importEntries(Collections.singletonList(entry1)));
    }

    // ===== listEntries() 测试 =====

    @Test
    void listEntries_emptyStore_returnsEmptyList() {
        List<Map<String, Object>> result = service.listEntries(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void listEntries_afterImport_returnsEntries() {
        importEntry("HOSPITAL", "HOSP01", null, "key1", "val1");

        List<Map<String, Object>> result = service.listEntries(null);

        assertEquals(1, result.size());
    }

    @Test
    void listEntries_filterByScopeLevel() {
        importEntry("HOSPITAL", "HOSP01", null, "key1", "val1");
        importEntry("CAMPUS", "CAMP01", null, "key2", "val2");

        Map<String, String> filters = new HashMap<>();
        filters.put("scopeLevel", "HOSPITAL");

        List<Map<String, Object>> result = service.listEntries(filters);

        assertEquals(1, result.size());
        assertEquals("HOSP01", result.get(0).get("scope_code"));
    }

    @Test
    void listEntries_filterByAssetType() {
        importEntry("HOSPITAL", "HOSP01", "THEME", "key1", "val1");
        importEntry("HOSPITAL", "HOSP01", "CONFIG", "key2", "val2");

        Map<String, String> filters = new HashMap<>();
        filters.put("assetType", "THEME");

        List<Map<String, Object>> result = service.listEntries(filters);

        assertEquals(1, result.size());
        assertEquals("THEME", result.get(0).get("asset_type"));
    }

    @Test
    void listEntries_filterByOverrideKey() {
        importEntry("HOSPITAL", "HOSP01", null, "key1", "val1");
        importEntry("HOSPITAL", "HOSP01", null, "key2", "val2");

        Map<String, String> filters = new HashMap<>();
        filters.put("overrideKey", "key1");

        List<Map<String, Object>> result = service.listEntries(filters);

        assertEquals(1, result.size());
        assertEquals("key1", result.get(0).get("override_key"));
    }

    @Test
    void listEntries_filterByTenantId() {
        importEntry("HOSPITAL", "HOSP01", null, "key1", "val1");

        Map<String, String> filters = new HashMap<>();
        filters.put("tenantId", "nonexistent");

        List<Map<String, Object>> result = service.listEntries(filters);
        assertTrue(result.isEmpty());
    }

    @Test
    void listEntries_withLimit() {
        for (int i = 0; i < 5; i++) {
            importEntry("HOSPITAL", "HOSP0" + i, null, "key" + i, "val" + i);
        }

        Map<String, String> filters = new HashMap<>();
        filters.put("limit", "3");

        List<Map<String, Object>> result = service.listEntries(filters);
        assertTrue(result.size() <= 3);
    }

    @Test
    void listEntries_zeroLimit_defaultsTo200() {
        importEntry("HOSPITAL", "HOSP01", null, "key1", "val1");

        Map<String, String> filters = new HashMap<>();
        filters.put("limit", "0");

        List<Map<String, Object>> result = service.listEntries(filters);
        assertEquals(1, result.size());
    }

    @Test
    void listEntries_invalidLimit_defaultsTo200() {
        importEntry("HOSPITAL", "HOSP01", null, "key1", "val1");

        Map<String, String> filters = new HashMap<>();
        filters.put("limit", "abc");

        List<Map<String, Object>> result = service.listEntries(filters);
        assertEquals(1, result.size());
    }

    // ===== computeOverride() 测试 =====

    @Test
    void computeOverride_emptyStore_returnsEmptyEffective() {
        OrganizationContext context = createContext("default", null, "HOSP01", null, null, null);

        Map<String, Object> result = service.computeOverride(context, "THEME");

        assertEquals("default", result.get("tenant_id"));
        assertEquals("THEME", result.get("asset_type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> effective = (Map<String, Object>) result.get("effective");
        assertTrue(effective.isEmpty());
    }

    @Test
    void computeOverride_withMatchingEntries_returnsEffective() {
        importEntry("HOSPITAL", "HOSP01", "THEME", "color", "red");

        OrganizationContext context = createContext("default", null, "HOSP01", null, null, null);

        Map<String, Object> result = service.computeOverride(context, "THEME");

        @SuppressWarnings("unchecked")
        Map<String, Object> effective = (Map<String, Object>) result.get("effective");
        assertEquals("red", effective.get("color"));
    }

    @Test
    void computeOverride_inheritanceFineGrainedOverridesCoarse() {
        // PLATFORM level override
        importEntry("PLATFORM", "DEFAULT", null, "color", "blue");
        // HOSPITAL level override (should win)
        importEntry("HOSPITAL", "HOSP01", null, "color", "red");

        OrganizationContext context = createContext("default", null, "HOSP01", null, null, null);

        Map<String, Object> result = service.computeOverride(context, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> effective = (Map<String, Object>) result.get("effective");
        assertEquals("red", effective.get("color"));
    }

    @Test
    void computeOverride_departmentOverridesHospital() {
        importEntry("HOSPITAL", "HOSP01", null, "color", "blue");
        importEntry("DEPARTMENT", "DEPT01", null, "color", "green");

        OrganizationContext context = createContext("default", null, "HOSP01", null, null, "DEPT01");

        Map<String, Object> result = service.computeOverride(context, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> effective = (Map<String, Object>) result.get("effective");
        assertEquals("green", effective.get("color"));
    }

    @Test
    void computeOverride_differentKeysMerged() {
        importEntry("HOSPITAL", "HOSP01", null, "color", "red");
        importEntry("CAMPUS", "CAMP01", null, "font", "Arial");

        OrganizationContext context = createContext("default", null, "HOSP01", "CAMP01", null, null);

        Map<String, Object> result = service.computeOverride(context, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> effective = (Map<String, Object>) result.get("effective");
        assertEquals("red", effective.get("color"));
        assertEquals("Arial", effective.get("font"));
    }

    @Test
    void computeOverride_inheritanceChainBuilt() {
        OrganizationContext context = createContext("default", "GROUP01", "HOSP01", "CAMP01", "SITE01", "DEPT01");

        Map<String, Object> result = service.computeOverride(context, null);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> chain = (List<Map<String, String>>) result.get("inheritance_chain");
        assertEquals(6, chain.size()); // DEPARTMENT, SITE, CAMPUS, HOSPITAL, GROUP, PLATFORM
        assertEquals("DEPARTMENT", chain.get(0).get("scope_level"));
        assertEquals("PLATFORM", chain.get(5).get("scope_level"));
    }

    @Test
    void computeOverride_nullTenantId_defaultsToDefault() {
        OrganizationContext context = new OrganizationContext();
        context.setTenantId(null);
        context.setHospitalCode("HOSP01");

        importEntry("default", "HOSPITAL", "HOSP01", null, "key1", "val1");

        Map<String, Object> result = service.computeOverride(context, null);

        assertEquals("default", result.get("tenant_id"));
    }

    @Test
    void computeOverride_resolvedSourcesContainsInfo() {
        importEntry("HOSPITAL", "HOSP01", null, "color", "red");

        OrganizationContext context = createContext("default", null, "HOSP01", null, null, null);

        Map<String, Object> result = service.computeOverride(context, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sources = (List<Map<String, Object>>) result.get("resolved_sources");
        assertEquals(1, sources.size());
        assertEquals("color", sources.get(0).get("override_key"));
        assertEquals("red", sources.get(0).get("resolved_value"));
        assertNotNull(sources.get(0).get("resolved_from"));
        assertEquals("HOSPITAL", sources.get(0).get("resolved_level"));
    }

    // ===== resolveOverride() 测试 =====

    @Test
    void resolveOverride_existingKey_returnsValue() {
        importEntry("HOSPITAL", "HOSP01", null, "color", "red");

        OrganizationContext context = createContext("default", null, "HOSP01", null, null, null);

        Map<String, Object> result = service.resolveOverride(context, null, "color");

        assertEquals("color", result.get("override_key"));
        assertEquals("red", result.get("resolved_value"));
        assertNotNull(result.get("resolved_from"));
        assertEquals("HOSPITAL", result.get("resolved_level"));
    }

    @Test
    void resolveOverride_nonExistentKey_returnsNullValue() {
        OrganizationContext context = createContext("default", null, "HOSP01", null, null, null);

        Map<String, Object> result = service.resolveOverride(context, null, "nonexistent");

        assertEquals("nonexistent", result.get("override_key"));
        assertNull(result.get("resolved_value"));
        assertNull(result.get("resolved_from"));
    }

    @Test
    void resolveOverride_nullOverrideKey_throwsException() {
        OrganizationContext context = createContext("default", null, "HOSP01", null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resolveOverride(context, null, null));
        assertEquals("overrideKey is required", ex.getMessage());
    }

    @Test
    void resolveOverride_emptyOverrideKey_throwsException() {
        OrganizationContext context = createContext("default", null, "HOSP01", null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.resolveOverride(context, null, "  "));
        assertEquals("overrideKey is required", ex.getMessage());
    }

    @Test
    void resolveOverride_withAssetType() {
        importEntry("HOSPITAL", "HOSP01", "THEME", "color", "red");
        importEntry("HOSPITAL", "HOSP01", "CONFIG", "color", "blue");

        OrganizationContext context = createContext("default", null, "HOSP01", null, null, null);

        Map<String, Object> result = service.resolveOverride(context, "THEME", "color");

        assertEquals("red", result.get("resolved_value"));
    }

    // ===== hasOverride() 测试 =====

    @Test
    void hasOverride_existingEntry_returnsTrue() {
        importEntry("HOSPITAL", "HOSP01", null, "color", "red");

        assertTrue(service.hasOverride("default", "HOSPITAL", "HOSP01", null, "color"));
    }

    @Test
    void hasOverride_nonExistentEntry_returnsFalse() {
        assertFalse(service.hasOverride("default", "HOSPITAL", "HOSP01", null, "color"));
    }

    @Test
    void hasOverride_withAssetType_matching_returnsTrue() {
        importEntry("HOSPITAL", "HOSP01", "THEME", "color", "red");

        assertTrue(service.hasOverride("default", "HOSPITAL", "HOSP01", "THEME", "color"));
    }

    @Test
    void hasOverride_withAssetType_notMatching_returnsFalse() {
        importEntry("HOSPITAL", "HOSP01", "THEME", "color", "red");

        assertFalse(service.hasOverride("default", "HOSPITAL", "HOSP01", "CONFIG", "color"));
    }

    @Test
    void hasOverride_nullAssetType_matchesNullAssetTypeEntry() {
        importEntry("HOSPITAL", "HOSP01", null, "color", "red");

        assertTrue(service.hasOverride("default", "HOSPITAL", "HOSP01", null, "color"));
    }

    @Test
    void hasOverride_entryWithNullAssetType_matchesAnyAssetType() {
        importEntry("HOSPITAL", "HOSP01", null, "color", "red");

        assertTrue(service.hasOverride("default", "HOSPITAL", "HOSP01", "THEME", "color"));
    }

    // ===== entryCount() 测试 =====

    @Test
    void entryCount_emptyStore_returnsZero() {
        assertEquals(0, service.entryCount());
    }

    @Test
    void entryCount_afterImport_returnsCount() {
        importEntry("HOSPITAL", "HOSP01", null, "key1", "val1");
        importEntry("CAMPUS", "CAMP01", null, "key2", "val2");

        assertEquals(2, service.entryCount());
    }

    // ===== 综合场景测试 =====

    @Test
    void fullScenario_importComputeResolve() {
        // 1. 导入覆盖配置
        importEntry("PLATFORM", "DEFAULT", null, "theme", "light");
        importEntry("HOSPITAL", "HOSP01", null, "theme", "dark");
        importEntry("DEPARTMENT", "DEPT01", null, "theme", "custom");

        // 2. 计算覆盖
        OrganizationContext context = createContext("default", null, "HOSP01", null, null, "DEPT01");
        Map<String, Object> computed = service.computeOverride(context, null);

        @SuppressWarnings("unchecked")
        Map<String, Object> effective = (Map<String, Object>) computed.get("effective");
        assertEquals("custom", effective.get("theme"));

        // 3. 解析单个覆盖
        Map<String, Object> resolved = service.resolveOverride(context, null, "theme");
        assertEquals("custom", resolved.get("resolved_value"));
        assertEquals("DEPARTMENT", resolved.get("resolved_level"));

        // 4. 没有 department 上下文时，应回退到 hospital
        OrganizationContext hospitalOnly = createContext("default", null, "HOSP01", null, null, null);
        Map<String, Object> resolved2 = service.resolveOverride(hospitalOnly, null, "theme");
        assertEquals("dark", resolved2.get("resolved_value"));
        assertEquals("HOSPITAL", resolved2.get("resolved_level"));
    }

    @Test
    void importEntries_withDescription() {
        Map<String, Object> entry1 = new LinkedHashMap<>();
        entry1.put("scope_level", "HOSPITAL");
        entry1.put("scope_code", "HOSP01");
        entry1.put("override_key", "key1");
        entry1.put("override_value", "val1");
        entry1.put("description", "测试描述");

        List<Map<String, Object>> request = Collections.singletonList(entry1);

        List<Map<String, Object>> result = service.importEntries(request);

        assertEquals("测试描述", result.get(0).get("description"));
    }

    // ===== 辅助方法 =====

    private void importEntry(String scopeLevel, String scopeCode, String assetType,
                             String overrideKey, Object overrideValue) {
        importEntry("default", scopeLevel, scopeCode, assetType, overrideKey, overrideValue);
    }

    private void importEntry(String tenantId, String scopeLevel, String scopeCode,
                             String assetType, String overrideKey, Object overrideValue) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("scope_level", scopeLevel);
        entry.put("scope_code", scopeCode);
        entry.put("override_key", overrideKey);
        entry.put("override_value", overrideValue);
        if (assetType != null) {
            entry.put("asset_type", assetType);
        }
        if (!"default".equals(tenantId)) {
            entry.put("tenant_id", tenantId);
        }
        service.importEntries(Collections.singletonList(entry));
    }

    private OrganizationContext createContext(String tenantId, String groupCode, String hospitalCode,
                                              String campusCode, String siteCode, String departmentCode) {
        OrganizationContext context = new OrganizationContext();
        context.setTenantId(tenantId);
        context.setGroupCode(groupCode);
        context.setHospitalCode(hospitalCode);
        context.setCampusCode(campusCode);
        context.setSiteCode(siteCode);
        context.setDepartmentCode(departmentCode);
        return context;
    }
}
