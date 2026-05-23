package com.medkernel.provenance;

import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceAssetBindingServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    private SourceAssetBindingService bindingService;

    @BeforeEach
    void setUp() {
        when(persistenceService.listSourceAssetBindings()).thenReturn(new ArrayList<SourceAssetBinding>());
        bindingService = new SourceAssetBindingService(persistenceService);
        bindingService.rebuildFromPersistence();
    }

    // =========================================================================
    // importBindings
    // =========================================================================

    @Test
    void importBindings_withMapRequest_shouldImportSuccessfully() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("tenant_id", "tenant1");
        request.put("operator_id", "admin");
        request.put("bindings", Arrays.asList(bind));

        Map<String, Object> result = bindingService.importBindings(request);

        assertEquals("tenant1", result.get("tenant_id"));
        assertEquals(1, result.get("imported_count"));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("bindings");
        assertEquals(1, imported.size());
        assertEquals("RULE", imported.get(0).get("asset_type"));
        verify(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));
    }

    @Test
    void importBindings_withListRequest_shouldImportSuccessfully() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        List<Map<String, Object>> request = Arrays.asList(bind);

        Map<String, Object> result = bindingService.importBindings(request);

        assertEquals("default", result.get("tenant_id"));
        assertEquals(1, result.get("imported_count"));
    }

    @Test
    void importBindings_withCamelCaseKeys_shouldImportSuccessfully() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = new LinkedHashMap<String, Object>();
        bind.put("assetType", "RULE");
        bind.put("assetCode", "RULE_CAMEL");
        bind.put("documentCode", "DOC_CAMEL");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("bindings", Arrays.asList(bind));

        Map<String, Object> result = bindingService.importBindings(request);
        assertEquals(1, result.get("imported_count"));
    }

    @Test
    void importBindings_emptyBindings_shouldThrow() {
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("bindings", new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> bindingService.importBindings(request));
    }

    @Test
    void importBindings_missingAssetType_shouldThrow() {
        Map<String, Object> bind = new LinkedHashMap<String, Object>();
        bind.put("asset_code", "RULE_001");
        bind.put("document_code", "DOC_001");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("bindings", Arrays.asList(bind));

        assertThrows(IllegalArgumentException.class, () -> bindingService.importBindings(request));
    }

    @Test
    void importBindings_missingAssetCode_shouldThrow() {
        Map<String, Object> bind = new LinkedHashMap<String, Object>();
        bind.put("asset_type", "RULE");
        bind.put("document_code", "DOC_001");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("bindings", Arrays.asList(bind));

        assertThrows(IllegalArgumentException.class, () -> bindingService.importBindings(request));
    }

    @Test
    void importBindings_missingDocumentCode_shouldThrow() {
        Map<String, Object> bind = new LinkedHashMap<String, Object>();
        bind.put("asset_type", "RULE");
        bind.put("asset_code", "RULE_001");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("bindings", Arrays.asList(bind));

        assertThrows(IllegalArgumentException.class, () -> bindingService.importBindings(request));
    }

    @Test
    void importBindings_unsupportedAssetType_shouldThrow() {
        Map<String, Object> bind = buildValidBindingPayload();
        bind.put("asset_type", "INVALID_TYPE");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("bindings", Arrays.asList(bind));

        assertThrows(IllegalArgumentException.class, () -> bindingService.importBindings(request));
    }

    @Test
    void importBindings_unsupportedBindingType_shouldThrow() {
        Map<String, Object> bind = buildValidBindingPayload();
        bind.put("binding_type", "INVALID_TYPE");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("bindings", Arrays.asList(bind));

        assertThrows(IllegalArgumentException.class, () -> bindingService.importBindings(request));
    }

    @Test
    void importBindings_allSupportedAssetTypes_shouldSucceed() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        String[] types = {"RULE", "PATHWAY", "CONFIG_PACKAGE", "ADAPTER", "GRAPH", "QC_METRIC"};
        for (String type : types) {
            Map<String, Object> bind = buildValidBindingPayload();
            bind.put("asset_type", type);
            bind.put("asset_code", "CODE_" + type);
            bind.put("binding_id", "BIND_" + type);

            Map<String, Object> result = bindingService.importBindings(Arrays.asList(bind));
            assertEquals(1, result.get("imported_count"));
        }
    }

    @Test
    void importBindings_allSupportedBindingTypes_shouldSucceed() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        String[] types = {"EVIDENCE", "REFERENCE", "DERIVATION", "COMPLIANCE"};
        for (String type : types) {
            Map<String, Object> bind = buildValidBindingPayload();
            bind.put("binding_type", type);
            bind.put("binding_id", "BIND_" + type);

            Map<String, Object> result = bindingService.importBindings(Arrays.asList(bind));
            assertEquals(1, result.get("imported_count"));
        }
    }

    @Test
    void importBindings_defaultBindingTypeIsReference() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        bind.remove("binding_type");

        Map<String, Object> result = bindingService.importBindings(Arrays.asList(bind));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("bindings");
        assertEquals("REFERENCE", imported.get(0).get("binding_type"));
    }

    @Test
    void importBindings_autoGeneratedBindingId_shouldGenerateId() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        bind.remove("binding_id");

        Map<String, Object> result = bindingService.importBindings(Arrays.asList(bind));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("bindings");
        assertNotNull(imported.get(0).get("binding_id"));
        assertTrue(((String) imported.get(0).get("binding_id")).startsWith("BIND_"));
    }

    @Test
    void importBindings_explicitBindingId_shouldUseProvidedId() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        bind.put("binding_id", "MY_BIND_001");

        Map<String, Object> result = bindingService.importBindings(Arrays.asList(bind));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("bindings");
        assertEquals("MY_BIND_001", imported.get(0).get("binding_id"));
    }

    @Test
    void importBindings_noCitationId_shouldGenerateInfoWarning() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        bind.remove("citation_id");

        Map<String, Object> result = bindingService.importBindings(Arrays.asList(bind));
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertFalse(warnings.isEmpty());
        assertEquals("INFO", warnings.get(0).get("severity"));
        assertEquals("citation_id", warnings.get(0).get("field"));
    }

    @Test
    void importBindings_withCitationId_shouldNotGenerateWarning() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        bind.put("citation_id", "CIT_001");

        Map<String, Object> result = bindingService.importBindings(Arrays.asList(bind));
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertTrue(warnings.isEmpty());
    }

    @Test
    void importBindings_shouldCallAudit() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("operator_id", "admin");
        request.put("bindings", Arrays.asList(bind));

        bindingService.importBindings(request);

        verify(persistenceService).saveAuditLog(
                eq("PROVENANCE"), eq("IMPORT_BINDING"), eq("SRC_ASSET_BINDING"),
                any(), any(), any(), eq("admin"), any(Map.class));
    }

    @Test
    void importBindings_auditFailure_shouldNotPropagate() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));
        doThrow(new RuntimeException("audit failed")).when(persistenceService)
                .saveAuditLog(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), any(Map.class));

        Map<String, Object> bind = buildValidBindingPayload();

        Map<String, Object> result = bindingService.importBindings(Arrays.asList(bind));
        assertNotNull(result);
    }

    @Test
    void importBindings_updateExisting_shouldPreserveCreatedTime() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        // First import
        Map<String, Object> bind1 = buildValidBindingPayload();
        Map<String, Object> result1 = bindingService.importBindings(Arrays.asList(bind1));
        List<Map<String, Object>> imported1 = (List<Map<String, Object>>) result1.get("bindings");
        String originalCreatedTime = (String) imported1.get(0).get("created_time");

        // Second import of same binding
        Map<String, Object> bind2 = buildValidBindingPayload();
        Map<String, Object> result2 = bindingService.importBindings(Arrays.asList(bind2));
        List<Map<String, Object>> imported2 = (List<Map<String, Object>>) result2.get("bindings");
        assertEquals(originalCreatedTime, imported2.get(0).get("created_time"));
    }

    @Test
    void importBindings_multipleBindings_shouldImportAll() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        Map<String, Object> bind2 = buildValidBindingPayload();
        bind2.put("binding_id", "BIND_002");
        bind2.put("asset_code", "RULE_002");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("bindings", Arrays.asList(bind1, bind2));

        Map<String, Object> result = bindingService.importBindings(request);
        assertEquals(2, result.get("imported_count"));
    }

    // =========================================================================
    // listBindings
    // =========================================================================

    @Test
    void listBindings_noFilters_shouldReturnAll() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        Map<String, Object> bind2 = buildValidBindingPayload();
        bind2.put("binding_id", "BIND_002");
        bind2.put("asset_code", "RULE_002");
        bindingService.importBindings(Arrays.asList(bind1, bind2));

        List<Map<String, Object>> result = bindingService.listBindings(new HashMap<String, String>());
        assertTrue(result.size() >= 2);
    }

    @Test
    void listBindings_filterByAssetType_shouldReturnMatching() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        bind1.put("asset_type", "RULE");
        Map<String, Object> bind2 = buildValidBindingPayload();
        bind2.put("binding_id", "BIND_002");
        bind2.put("asset_type", "PATHWAY");
        bindingService.importBindings(Arrays.asList(bind1, bind2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("assetType", "RULE");
        List<Map<String, Object>> result = bindingService.listBindings(filters);

        for (Map<String, Object> r : result) {
            assertEquals("RULE", r.get("asset_type"));
        }
    }

    @Test
    void listBindings_filterByAssetCode_shouldReturnMatching() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        Map<String, Object> bind2 = buildValidBindingPayload();
        bind2.put("binding_id", "BIND_002");
        bind2.put("asset_code", "RULE_002");
        bindingService.importBindings(Arrays.asList(bind1, bind2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("assetCode", "RULE_001");
        List<Map<String, Object>> result = bindingService.listBindings(filters);

        for (Map<String, Object> r : result) {
            assertEquals("RULE_001", r.get("asset_code"));
        }
    }

    @Test
    void listBindings_filterByDocumentCode_shouldReturnMatching() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        Map<String, Object> bind2 = buildValidBindingPayload();
        bind2.put("binding_id", "BIND_002");
        bind2.put("document_code", "DOC_002");
        bindingService.importBindings(Arrays.asList(bind1, bind2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("documentCode", "DOC_001");
        List<Map<String, Object>> result = bindingService.listBindings(filters);

        for (Map<String, Object> r : result) {
            assertEquals("DOC_001", r.get("document_code"));
        }
    }

    @Test
    void listBindings_filterByBindingType_shouldReturnMatching() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        bind1.put("binding_type", "EVIDENCE");
        Map<String, Object> bind2 = buildValidBindingPayload();
        bind2.put("binding_id", "BIND_002");
        bind2.put("binding_type", "REFERENCE");
        bindingService.importBindings(Arrays.asList(bind1, bind2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("bindingType", "EVIDENCE");
        List<Map<String, Object>> result = bindingService.listBindings(filters);

        for (Map<String, Object> r : result) {
            assertEquals("EVIDENCE", r.get("binding_type"));
        }
    }

    @Test
    void listBindings_withLimit_shouldRespectLimit() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        Map<String, Object> bind2 = buildValidBindingPayload();
        bind2.put("binding_id", "BIND_002");
        Map<String, Object> bind3 = buildValidBindingPayload();
        bind3.put("binding_id", "BIND_003");
        bindingService.importBindings(Arrays.asList(bind1, bind2, bind3));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("limit", "2");
        List<Map<String, Object>> result = bindingService.listBindings(filters);

        assertTrue(result.size() <= 2);
    }

    @Test
    void listBindings_nullFilters_shouldReturnAll() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        bindingService.importBindings(Arrays.asList(bind1));

        List<Map<String, Object>> result = bindingService.listBindings(null);
        assertNotNull(result);
    }

    // =========================================================================
    // getBinding
    // =========================================================================

    @Test
    void getBinding_existingBinding_shouldReturnBinding() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        bindingService.importBindings(Arrays.asList(bind));

        Map<String, Object> result = bindingService.getBinding("BIND_001", null);
        assertEquals("BIND_001", result.get("binding_id"));
    }

    @Test
    void getBinding_nonExistingBinding_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> bindingService.getBinding("NONEXISTENT", null));
    }

    @Test
    void getBinding_withTenantId_shouldResolveTenant() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("tenant_id", "tenant1");
        request.put("bindings", Arrays.asList(bind));
        bindingService.importBindings(request);

        Map<String, Object> result = bindingService.getBinding("BIND_001", "tenant1");
        assertEquals("BIND_001", result.get("binding_id"));
    }

    @Test
    void getBinding_nullTenantId_shouldUseDefault() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind = buildValidBindingPayload();
        bindingService.importBindings(Arrays.asList(bind));

        Map<String, Object> result = bindingService.getBinding("BIND_001", null);
        assertNotNull(result);
    }

    // =========================================================================
    // getBindingsByAsset
    // =========================================================================

    @Test
    void getBindingsByAsset_shouldReturnMatchingBindings() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        Map<String, Object> bind2 = buildValidBindingPayload();
        bind2.put("binding_id", "BIND_002");
        bind2.put("asset_type", "PATHWAY");
        bind2.put("asset_code", "PATHWAY_001");
        bindingService.importBindings(Arrays.asList(bind1, bind2));

        List<Map<String, Object>> result = bindingService.getBindingsByAsset("RULE", "RULE_001", null);
        for (Map<String, Object> r : result) {
            assertEquals("RULE", r.get("asset_type"));
            assertEquals("RULE_001", r.get("asset_code"));
        }
    }

    @Test
    void getBindingsByAsset_noMatching_shouldReturnEmpty() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        bindingService.importBindings(Arrays.asList(bind1));

        List<Map<String, Object>> result = bindingService.getBindingsByAsset("PATHWAY", "PATHWAY_MISSING", null);
        assertTrue(result.isEmpty());
    }

    @Test
    void getBindingsByAsset_nullAssetType_shouldMatchAllTypes() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        bindingService.importBindings(Arrays.asList(bind1));

        List<Map<String, Object>> result = bindingService.getBindingsByAsset(null, "RULE_001", null);
        assertFalse(result.isEmpty());
    }

    // =========================================================================
    // getBindingsByDocument
    // =========================================================================

    @Test
    void getBindingsByDocument_shouldReturnMatchingBindings() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        Map<String, Object> bind2 = buildValidBindingPayload();
        bind2.put("binding_id", "BIND_002");
        bind2.put("document_code", "DOC_002");
        bindingService.importBindings(Arrays.asList(bind1, bind2));

        List<Map<String, Object>> result = bindingService.getBindingsByDocument("DOC_001", null);
        for (Map<String, Object> r : result) {
            assertEquals("DOC_001", r.get("document_code"));
        }
    }

    @Test
    void getBindingsByDocument_noMatching_shouldReturnEmpty() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        Map<String, Object> bind1 = buildValidBindingPayload();
        bindingService.importBindings(Arrays.asList(bind1));

        List<Map<String, Object>> result = bindingService.getBindingsByDocument("DOC_MISSING", null);
        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // bindingCount
    // =========================================================================

    @Test
    void bindingCount_shouldReturnStoreSize() {
        doNothing().when(persistenceService).saveSourceAssetBinding(any(SourceAssetBinding.class));

        assertEquals(0, bindingService.bindingCount());

        Map<String, Object> bind1 = buildValidBindingPayload();
        bindingService.importBindings(Arrays.asList(bind1));

        assertEquals(1, bindingService.bindingCount());
    }

    // =========================================================================
    // rebuildFromPersistence
    // =========================================================================

    @Test
    void rebuildFromPersistence_shouldLoadPersistedBindings() {
        SourceAssetBinding persisted = new SourceAssetBinding();
        persisted.setTenantId("default");
        persisted.setBindingId("BIND_PERSISTED");
        persisted.setAssetType("RULE");
        persisted.setAssetCode("RULE_P");

        when(persistenceService.listSourceAssetBindings()).thenReturn(Arrays.asList(persisted));

        SourceAssetBindingService freshService = new SourceAssetBindingService(persistenceService);
        freshService.rebuildFromPersistence();

        assertEquals(1, freshService.bindingCount());
        Map<String, Object> result = freshService.getBinding("BIND_PERSISTED", null);
        assertEquals("BIND_PERSISTED", result.get("binding_id"));
    }

    @Test
    void rebuildFromPersistence_emptyStore_shouldNotFail() {
        when(persistenceService.listSourceAssetBindings()).thenReturn(new ArrayList<SourceAssetBinding>());

        SourceAssetBindingService freshService = new SourceAssetBindingService(persistenceService);
        freshService.rebuildFromPersistence();

        assertEquals(0, freshService.bindingCount());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private Map<String, Object> buildValidBindingPayload() {
        Map<String, Object> bind = new LinkedHashMap<String, Object>();
        bind.put("binding_id", "BIND_001");
        bind.put("asset_type", "RULE");
        bind.put("asset_code", "RULE_001");
        bind.put("document_code", "DOC_001");
        bind.put("binding_type", "EVIDENCE");
        bind.put("citation_id", "CIT_001");
        return bind;
    }
}
