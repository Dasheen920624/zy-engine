package com.medkernel.organization;

import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.persistence.OrganizationPersistenceService;
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
 * OrganizationDirectoryService 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class OrganizationDirectoryServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private OrganizationPersistenceService orgPersistenceService;

    private OrganizationDirectoryService service;

    @BeforeEach
    void setUp() {
        when(orgPersistenceService.enabled()).thenReturn(false);
        service = new OrganizationDirectoryService(persistenceService, orgPersistenceService);
        service.loadFromDatabase();
    }

    // ===== importUnits() 测试 =====

    @Test
    void importUnits_withListInput_importsSuccessfully() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "GROUP");
        unit1.put("code", "GROUP01");
        unit1.put("name", "测试集团");

        List<Map<String, Object>> request = new ArrayList<>();
        request.add(unit1);

        Map<String, Object> result = service.importUnits(request);

        assertEquals(1, result.get("imported_count"));
        assertEquals("default", result.get("tenant_id"));
        verify(persistenceService).saveAuditLog(eq("ORGANIZATION"), eq("IMPORT"), eq("ORGANIZATION_BATCH"),
                eq("default"), isNull(), isNull(), isNull(), any(Map.class));
    }

    @Test
    void importUnits_withMapInputAndNestedUnits() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "GROUP");
        unit1.put("code", "GROUP01");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("tenant_id", "t1");
        request.put("operator_id", "admin");
        request.put("units", Collections.singletonList(unit1));

        Map<String, Object> result = service.importUnits(request);

        assertEquals(1, result.get("imported_count"));
        assertEquals("t1", result.get("tenant_id"));
    }

    @Test
    void importUnits_withMapInputNoUnitsKey_treatsBodyAsSingleUnit() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("level", "GROUP");
        request.put("code", "GROUP01");
        request.put("tenant_id", "t1");

        Map<String, Object> result = service.importUnits(request);

        assertEquals(1, result.get("imported_count"));
    }

    @Test
    void importUnits_emptyList_throwsException() {
        List<Map<String, Object>> request = new ArrayList<>();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importUnits(request));
        assertEquals("units is required", ex.getMessage());
    }

    @Test
    void importUnits_unsupportedLevel_throwsException() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "PLATFORM");
        unit1.put("code", "P001");

        List<Map<String, Object>> request = Collections.singletonList(unit1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importUnits(request));
        assertTrue(ex.getMessage().contains("unsupported organization level"));
    }

    @Test
    void importUnits_missingLevel_throwsException() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("code", "CODE01");

        List<Map<String, Object>> request = Collections.singletonList(unit1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importUnits(request));
        assertTrue(ex.getMessage().contains("level is required"));
    }

    @Test
    void importUnits_missingCode_throwsException() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "GROUP");

        List<Map<String, Object>> request = Collections.singletonList(unit1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importUnits(request));
        assertTrue(ex.getMessage().contains("code is required"));
    }

    @Test
    void importUnits_hospitalWithWrongParentLevel_throwsException() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "HOSPITAL");
        unit1.put("code", "HOSP01");
        unit1.put("parent_level", "CAMPUS");
        unit1.put("parent_code", "CAMP01");

        List<Map<String, Object>> request = Collections.singletonList(unit1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importUnits(request));
        assertTrue(ex.getMessage().contains("parent_level must be GROUP"));
    }

    @Test
    void importUnits_groupMustNotHaveParentLevel() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "GROUP");
        unit1.put("code", "GROUP01");
        unit1.put("parent_level", "HOSPITAL");
        unit1.put("parent_code", "HOSP01");

        List<Map<String, Object>> request = Collections.singletonList(unit1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.importUnits(request));
        assertTrue(ex.getMessage().contains("must not have parent_level"));
    }

    @Test
    void importUnits_groupWithoutParent_noWarning() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "GROUP");
        unit1.put("code", "GROUP01");

        List<Map<String, Object>> request = Collections.singletonList(unit1);

        Map<String, Object> result = service.importUnits(request);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertTrue(warnings.isEmpty());
    }

    @Test
    void importUnits_departmentWithoutParent_warnsMissingParent() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "DEPARTMENT");
        unit1.put("code", "DEPT01");
        // no parent_code -> will be treated as root node -> warning

        List<Map<String, Object>> request = Collections.singletonList(unit1);

        Map<String, Object> result = service.importUnits(request);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertFalse(warnings.isEmpty());
        assertEquals("WARN", warnings.get(0).get("severity"));
    }

    @Test
    void importUnits_departmentWithNonExistentParent_warnsParentNotImported() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "DEPARTMENT");
        unit1.put("code", "DEPT01");
        unit1.put("parent_level", "SITE");
        unit1.put("parent_code", "SITE01");

        List<Map<String, Object>> request = Collections.singletonList(unit1);

        Map<String, Object> result = service.importUnits(request);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertFalse(warnings.isEmpty());
        assertTrue(warnings.get(0).get("message").toString().contains("上级组织尚未导入"));
    }

    @Test
    void importUnits_updateExistingUnit_preservesCreatedTime() {
        // First import
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "GROUP");
        unit1.put("code", "GROUP01");
        service.importUnits(Collections.singletonList(unit1));

        // Second import of same unit
        Map<String, Object> unit2 = new LinkedHashMap<>();
        unit2.put("level", "GROUP");
        unit2.put("code", "GROUP01");
        unit2.put("name", "Updated Name");
        Map<String, Object> result = service.importUnits(Collections.singletonList(unit2));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> units = (List<Map<String, Object>>) result.get("units");
        assertNotNull(units.get(0).get("created_time"));
        assertNotNull(units.get(0).get("updated_time"));
        assertEquals("Updated Name", units.get(0).get("name"));
    }

    @Test
    void importUnits_camelCaseKeys() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "HOSPITAL");
        unit1.put("code", "HOSP01");
        unit1.put("orgName", "测试医院");
        unit1.put("parentLevel", "GROUP");
        unit1.put("parentCode", "GROUP01");

        // First import GROUP
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("level", "GROUP");
        group.put("code", "GROUP01");
        service.importUnits(Collections.singletonList(group));

        Map<String, Object> result = service.importUnits(Collections.singletonList(unit1));

        assertEquals(1, result.get("imported_count"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> units = (List<Map<String, Object>>) result.get("units");
        assertEquals("测试医院", units.get(0).get("name"));
    }

    @Test
    void importUnits_persistFails_doesNotThrow() {
        doThrow(new RuntimeException("DB error")).when(orgPersistenceService).saveOrganizationUnit(any());

        // Re-create service with enabled persistence
        when(orgPersistenceService.enabled()).thenReturn(true);
        service = new OrganizationDirectoryService(persistenceService, orgPersistenceService);

        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "GROUP");
        unit1.put("code", "GROUP01");

        assertDoesNotThrow(() -> service.importUnits(Collections.singletonList(unit1)));
    }

    @Test
    void importUnits_auditFails_doesNotThrow() {
        doThrow(new RuntimeException("Audit error")).when(persistenceService)
                .saveAuditLog(anyString(), anyString(), anyString(), anyString(),
                        isNull(), isNull(), isNull(), any(Map.class));

        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "GROUP");
        unit1.put("code", "GROUP01");

        assertDoesNotThrow(() -> service.importUnits(Collections.singletonList(unit1)));
    }

    // ===== listUnits() 测试 =====

    @Test
    void listUnits_emptyStore_returnsEmptyList() {
        List<Map<String, Object>> result = service.listUnits(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void listUnits_afterImport_returnsImportedUnits() {
        importGroup("GROUP01");

        List<Map<String, Object>> result = service.listUnits(null);

        assertEquals(1, result.size());
        assertEquals("GROUP01", result.get(0).get("code"));
    }

    @Test
    void listUnits_filterByLevel() {
        importGroup("GROUP01");
        importHospital("HOSP01", "GROUP01");

        Map<String, String> filters = new HashMap<>();
        filters.put("level", "HOSPITAL");

        List<Map<String, Object>> result = service.listUnits(filters);

        assertEquals(1, result.size());
        assertEquals("HOSP01", result.get(0).get("code"));
    }

    @Test
    void listUnits_filterByStatus() {
        Map<String, Object> unit1 = new LinkedHashMap<>();
        unit1.put("level", "GROUP");
        unit1.put("code", "GROUP01");
        unit1.put("status", "INACTIVE");
        service.importUnits(Collections.singletonList(unit1));

        Map<String, String> filters = new HashMap<>();
        filters.put("status", "ACTIVE");

        List<Map<String, Object>> result = service.listUnits(filters);
        assertTrue(result.isEmpty());
    }

    @Test
    void listUnits_filterByTenantId() {
        importGroup("GROUP01");

        Map<String, String> filters = new HashMap<>();
        filters.put("tenantId", "nonexistent");

        List<Map<String, Object>> result = service.listUnits(filters);
        assertTrue(result.isEmpty());
    }

    @Test
    void listUnits_withLimit() {
        for (int i = 0; i < 5; i++) {
            importGroup("GROUP0" + i);
        }

        Map<String, String> filters = new HashMap<>();
        filters.put("limit", "3");

        List<Map<String, Object>> result = service.listUnits(filters);
        assertEquals(3, result.size());
    }

    @Test
    void listUnits_zeroLimit_defaultsTo200() {
        importGroup("GROUP01");

        Map<String, String> filters = new HashMap<>();
        filters.put("limit", "0");

        List<Map<String, Object>> result = service.listUnits(filters);
        assertEquals(1, result.size());
    }

    @Test
    void listUnits_invalidLimit_defaultsTo200() {
        importGroup("GROUP01");

        Map<String, String> filters = new HashMap<>();
        filters.put("limit", "abc");

        List<Map<String, Object>> result = service.listUnits(filters);
        assertEquals(1, result.size());
    }

    @Test
    void listUnits_filterByParentLevelAndParentCode() {
        importGroup("GROUP01");
        importHospital("HOSP01", "GROUP01");

        Map<String, String> filters = new HashMap<>();
        filters.put("parentLevel", "GROUP");
        filters.put("parentCode", "GROUP01");

        List<Map<String, Object>> result = service.listUnits(filters);
        assertEquals(1, result.size());
        assertEquals("HOSP01", result.get(0).get("code"));
    }

    // ===== getUnit() 测试 =====

    @Test
    void getUnit_existingUnit_returnsUnitWithChildren() {
        importGroup("GROUP01");
        importHospital("HOSP01", "GROUP01");

        Map<String, Object> result = service.getUnit("GROUP", "GROUP01", null);

        assertEquals("GROUP01", result.get("code"));
        assertEquals("集团", result.get("level_name"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) result.get("children");
        assertEquals(1, children.size());
        assertEquals("HOSP01", children.get(0).get("code"));
    }

    @Test
    void getUnit_nonExistentUnit_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getUnit("GROUP", "NONEXISTENT", null));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void getUnit_withTenantId() {
        importGroup("GROUP01");

        Map<String, Object> result = service.getUnit("GROUP", "GROUP01", "default");
        assertEquals("GROUP01", result.get("code"));
    }

    // ===== scopeExists() 测试 =====

    @Test
    void scopeExists_platformDefault_returnsTrue() {
        assertTrue(service.scopeExists("default", "PLATFORM", "DEFAULT"));
    }

    @Test
    void scopeExists_platformNonDefault_returnsFalse() {
        assertFalse(service.scopeExists("default", "PLATFORM", "OTHER"));
    }

    @Test
    void scopeExists_existingUnit_returnsTrue() {
        importGroup("GROUP01");

        assertTrue(service.scopeExists("default", "GROUP", "GROUP01"));
    }

    @Test
    void scopeExists_nonExistentUnit_returnsFalse() {
        assertFalse(service.scopeExists("default", "GROUP", "NONEXISTENT"));
    }

    @Test
    void scopeExists_nullCode_returnsFalse() {
        assertFalse(service.scopeExists("default", "GROUP", null));
    }

    // ===== scopeReference() 测试 =====

    @Test
    void scopeReference_platformLevel() {
        Map<String, Object> ref = service.scopeReference("default", "PLATFORM", "DEFAULT");

        assertEquals("PLATFORM", ref.get("scope_level"));
        assertEquals("DEFAULT", ref.get("scope_code"));
        assertTrue((Boolean) ref.get("exists"));
        assertTrue((Boolean) ref.get("baseline"));
    }

    @Test
    void scopeReference_existingUnit() {
        importGroup("GROUP01");

        Map<String, Object> ref = service.scopeReference("default", "GROUP", "GROUP01");

        assertEquals("GROUP", ref.get("scope_level"));
        assertEquals("GROUP01", ref.get("scope_code"));
        assertTrue((Boolean) ref.get("exists"));
        assertFalse((Boolean) ref.get("baseline"));
        assertEquals("集团", ref.get("scope_name"));
        assertNotNull(ref.get("organization_name"));
    }

    @Test
    void scopeReference_nonExistentUnit() {
        Map<String, Object> ref = service.scopeReference("default", "GROUP", "NONEXISTENT");

        assertFalse((Boolean) ref.get("exists"));
        assertNull(ref.get("organization_name"));
    }

    // ===== tree() 测试 =====

    @Test
    void tree_emptyStore_returnsEmptyTree() {
        Map<String, String> filters = new HashMap<>();
        Map<String, Object> result = service.tree(filters);

        assertEquals("default", result.get("tenant_id"));
        assertEquals(0, result.get("root_count"));
    }

    @Test
    void tree_withRootUnits() {
        importGroup("GROUP01");

        Map<String, String> filters = new HashMap<>();
        Map<String, Object> result = service.tree(filters);

        assertEquals(1, result.get("root_count"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tree = (List<Map<String, Object>>) result.get("tree");
        assertEquals("GROUP01", tree.get(0).get("code"));
    }

    @Test
    void tree_withRootLevelAndRootCode() {
        importGroup("GROUP01");
        importHospital("HOSP01", "GROUP01");

        Map<String, String> filters = new HashMap<>();
        filters.put("rootLevel", "GROUP");
        filters.put("rootCode", "GROUP01");

        Map<String, Object> result = service.tree(filters);

        assertEquals(1, result.get("root_count"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tree = (List<Map<String, Object>>) result.get("tree");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) tree.get(0).get("children");
        assertEquals(1, children.size());
        assertEquals("HOSP01", children.get(0).get("code"));
    }

    @Test
    void tree_rootLevelWithoutRootCode_throwsException() {
        Map<String, String> filters = new HashMap<>();
        filters.put("rootLevel", "GROUP");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.tree(filters));
        assertTrue(ex.getMessage().contains("rootLevel and rootCode must be provided together"));
    }

    @Test
    void tree_rootCodeWithoutRootLevel_throwsException() {
        Map<String, String> filters = new HashMap<>();
        filters.put("rootCode", "GROUP01");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.tree(filters));
        assertTrue(ex.getMessage().contains("rootLevel and rootCode must be provided together"));
    }

    @Test
    void tree_nonExistentRoot_throwsException() {
        Map<String, String> filters = new HashMap<>();
        filters.put("rootLevel", "GROUP");
        filters.put("rootCode", "NONEXISTENT");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.tree(filters));
        assertTrue(ex.getMessage().contains("root not found"));
    }

    @Test
    void tree_hierarchyWithMultipleLevels() {
        importGroup("GROUP01");
        importHospital("HOSP01", "GROUP01");

        Map<String, Object> campus = new LinkedHashMap<>();
        campus.put("level", "CAMPUS");
        campus.put("code", "CAMP01");
        campus.put("parent_level", "HOSPITAL");
        campus.put("parent_code", "HOSP01");
        service.importUnits(Collections.singletonList(campus));

        Map<String, String> filters = new HashMap<>();
        filters.put("rootLevel", "GROUP");
        filters.put("rootCode", "GROUP01");

        Map<String, Object> result = service.tree(filters);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tree = (List<Map<String, Object>>) result.get("tree");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hospChildren = (List<Map<String, Object>>) tree.get(0).get("children");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> campusChildren = (List<Map<String, Object>>) hospChildren.get(0).get("children");
        assertEquals(1, campusChildren.size());
        assertEquals("CAMP01", campusChildren.get(0).get("code"));
    }

    // ===== loadFromDatabase() 测试 =====

    @Test
    void loadFromDatabase_disabledPersistence_keepsEmptyStore() {
        when(orgPersistenceService.enabled()).thenReturn(false);

        OrganizationDirectoryService svc = new OrganizationDirectoryService(persistenceService, orgPersistenceService);
        svc.loadFromDatabase();

        List<Map<String, Object>> result = svc.listUnits(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadFromDatabase_enabledPersistence_loadsUnits() {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setTenantId("default");
        unit.setLevel("GROUP");
        unit.setCode("LOADED_GROUP");
        unit.setName("从数据库加载");
        unit.setStatus("ACTIVE");
        unit.setDisplayOrder(0);

        when(orgPersistenceService.enabled()).thenReturn(true);
        when(orgPersistenceService.loadAllOrganizationUnits()).thenReturn(Collections.singletonList(unit));

        OrganizationDirectoryService svc = new OrganizationDirectoryService(persistenceService, orgPersistenceService);
        svc.loadFromDatabase();

        List<Map<String, Object>> result = svc.listUnits(null);
        assertEquals(1, result.size());
        assertEquals("LOADED_GROUP", result.get(0).get("code"));
    }

    @Test
    void loadFromDatabase_enabledPersistenceLoadFails_keepsEmptyStore() {
        when(orgPersistenceService.enabled()).thenReturn(true);
        when(orgPersistenceService.loadAllOrganizationUnits()).thenThrow(new RuntimeException("DB down"));

        OrganizationDirectoryService svc = new OrganizationDirectoryService(persistenceService, orgPersistenceService);
        svc.loadFromDatabase();

        List<Map<String, Object>> result = svc.listUnits(null);
        assertTrue(result.isEmpty());
    }

    // ===== 辅助方法 =====

    private void importGroup(String code) {
        Map<String, Object> unit = new LinkedHashMap<>();
        unit.put("level", "GROUP");
        unit.put("code", code);
        service.importUnits(Collections.singletonList(unit));
    }

    private void importHospital(String code, String parentCode) {
        Map<String, Object> unit = new LinkedHashMap<>();
        unit.put("level", "HOSPITAL");
        unit.put("code", code);
        unit.put("parent_level", "GROUP");
        unit.put("parent_code", parentCode);
        service.importUnits(Collections.singletonList(unit));
    }
}
