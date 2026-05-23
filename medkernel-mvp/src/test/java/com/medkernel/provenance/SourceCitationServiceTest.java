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
class SourceCitationServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    private SourceCitationService citationService;

    @BeforeEach
    void setUp() {
        when(persistenceService.listSourceCitations()).thenReturn(new ArrayList<SourceCitation>());
        citationService = new SourceCitationService(persistenceService);
        citationService.rebuildFromPersistence();
    }

    // =========================================================================
    // importCitations
    // =========================================================================

    @Test
    void importCitations_withMapRequest_shouldImportSuccessfully() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("tenant_id", "tenant1");
        request.put("operator_id", "admin");
        request.put("citations", Arrays.asList(cit));

        Map<String, Object> result = citationService.importCitations(request);

        assertEquals("tenant1", result.get("tenant_id"));
        assertEquals(1, result.get("imported_count"));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("citations");
        assertEquals(1, imported.size());
        assertEquals("DOC_001", imported.get(0).get("document_code"));
        verify(persistenceService).saveSourceCitation(any(SourceCitation.class));
    }

    @Test
    void importCitations_withListRequest_shouldImportSuccessfully() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        List<Map<String, Object>> request = Arrays.asList(cit);

        Map<String, Object> result = citationService.importCitations(request);

        assertEquals("default", result.get("tenant_id"));
        assertEquals(1, result.get("imported_count"));
    }

    @Test
    void importCitations_withCamelCaseKeys_shouldImportSuccessfully() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = new LinkedHashMap<String, Object>();
        cit.put("documentCode", "DOC_CAMEL");
        cit.put("citationType", "SECTION");
        cit.put("section", "3.1");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("citations", Arrays.asList(cit));

        Map<String, Object> result = citationService.importCitations(request);
        assertEquals(1, result.get("imported_count"));
    }

    @Test
    void importCitations_emptyCitations_shouldThrow() {
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("citations", new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> citationService.importCitations(request));
    }

    @Test
    void importCitations_missingDocumentCode_shouldThrow() {
        Map<String, Object> cit = new LinkedHashMap<String, Object>();
        cit.put("citation_type", "SECTION");
        cit.put("section", "3.1");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("citations", Arrays.asList(cit));

        assertThrows(IllegalArgumentException.class, () -> citationService.importCitations(request));
    }

    @Test
    void importCitations_unsupportedCitationType_shouldThrow() {
        Map<String, Object> cit = buildValidCitationPayload();
        cit.put("citation_type", "INVALID_TYPE");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("citations", Arrays.asList(cit));

        assertThrows(IllegalArgumentException.class, () -> citationService.importCitations(request));
    }

    @Test
    void importCitations_allSupportedCitationTypes_shouldSucceed() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        String[] types = {"PAGE", "SECTION", "CLAUSE", "TABLE", "FIGURE", "APPENDIX"};
        for (String type : types) {
            Map<String, Object> cit = buildValidCitationPayload();
            cit.put("citation_type", type);
            cit.put("citation_id", "CIT_" + type);

            Map<String, Object> result = citationService.importCitations(Arrays.asList(cit));
            assertEquals(1, result.get("imported_count"));
        }
    }

    @Test
    void importCitations_defaultCitationTypeIsSection() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        cit.remove("citation_type");

        Map<String, Object> result = citationService.importCitations(Arrays.asList(cit));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("citations");
        assertEquals("SECTION", imported.get(0).get("citation_type"));
    }

    @Test
    void importCitations_autoGeneratedCitationId_shouldGenerateId() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        cit.remove("citation_id");

        Map<String, Object> result = citationService.importCitations(Arrays.asList(cit));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("citations");
        assertNotNull(imported.get(0).get("citation_id"));
        assertTrue(((String) imported.get(0).get("citation_id")).startsWith("CIT_"));
    }

    @Test
    void importCitations_explicitCitationId_shouldUseProvidedId() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        cit.put("citation_id", "MY_CIT_001");

        Map<String, Object> result = citationService.importCitations(Arrays.asList(cit));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("citations");
        assertEquals("MY_CIT_001", imported.get(0).get("citation_id"));
    }

    @Test
    void importCitations_noLocationReference_shouldGenerateWarning() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = new LinkedHashMap<String, Object>();
        cit.put("document_code", "DOC_001");
        cit.put("citation_type", "SECTION");
        cit.put("citation_id", "CIT_NOLOC");

        Map<String, Object> result = citationService.importCitations(Arrays.asList(cit));
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertFalse(warnings.isEmpty());
        assertEquals("WARN", warnings.get(0).get("severity"));
        assertEquals("location", warnings.get(0).get("field"));
    }

    @Test
    void importCitations_withSection_shouldNotGenerateLocationWarning() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        cit.put("section", "3.1");

        Map<String, Object> result = citationService.importCitations(Arrays.asList(cit));
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertTrue(warnings.isEmpty());
    }

    @Test
    void importCitations_withPage_shouldNotGenerateLocationWarning() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        cit.put("page", "42");

        Map<String, Object> result = citationService.importCitations(Arrays.asList(cit));
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertTrue(warnings.isEmpty());
    }

    @Test
    void importCitations_withClause_shouldNotGenerateLocationWarning() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        cit.put("clause", "5.2.1");

        Map<String, Object> result = citationService.importCitations(Arrays.asList(cit));
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertTrue(warnings.isEmpty());
    }

    @Test
    void importCitations_withQuoteText_shouldNotGenerateLocationWarning() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        cit.put("quote_text", "some quoted text");

        Map<String, Object> result = citationService.importCitations(Arrays.asList(cit));
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertTrue(warnings.isEmpty());
    }

    @Test
    void importCitations_shouldCallAudit() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("operator_id", "admin");
        request.put("citations", Arrays.asList(cit));

        citationService.importCitations(request);

        verify(persistenceService).saveAuditLog(
                eq("PROVENANCE"), eq("IMPORT_CITATION"), eq("SRC_CITATION"),
                any(), any(), any(), eq("admin"), any(Map.class));
    }

    @Test
    void importCitations_auditFailure_shouldNotPropagate() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));
        doThrow(new RuntimeException("audit failed")).when(persistenceService)
                .saveAuditLog(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), any(Map.class));

        Map<String, Object> cit = buildValidCitationPayload();

        Map<String, Object> result = citationService.importCitations(Arrays.asList(cit));
        assertNotNull(result);
    }

    @Test
    void importCitations_updateExisting_shouldPreserveCreatedTime() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        // First import
        Map<String, Object> cit1 = buildValidCitationPayload();
        Map<String, Object> result1 = citationService.importCitations(Arrays.asList(cit1));
        List<Map<String, Object>> imported1 = (List<Map<String, Object>>) result1.get("citations");
        String originalCreatedTime = (String) imported1.get(0).get("created_time");

        // Second import of same citation
        Map<String, Object> cit2 = buildValidCitationPayload();
        Map<String, Object> result2 = citationService.importCitations(Arrays.asList(cit2));
        List<Map<String, Object>> imported2 = (List<Map<String, Object>>) result2.get("citations");
        assertEquals(originalCreatedTime, imported2.get(0).get("created_time"));
    }

    @Test
    void importCitations_multipleCitations_shouldImportAll() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit1 = buildValidCitationPayload();
        Map<String, Object> cit2 = buildValidCitationPayload();
        cit2.put("citation_id", "CIT_002");
        cit2.put("document_code", "DOC_002");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("citations", Arrays.asList(cit1, cit2));

        Map<String, Object> result = citationService.importCitations(request);
        assertEquals(2, result.get("imported_count"));
    }

    // =========================================================================
    // listCitations
    // =========================================================================

    @Test
    void listCitations_noFilters_shouldReturnAll() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit1 = buildValidCitationPayload();
        Map<String, Object> cit2 = buildValidCitationPayload();
        cit2.put("citation_id", "CIT_002");
        cit2.put("document_code", "DOC_002");
        citationService.importCitations(Arrays.asList(cit1, cit2));

        List<Map<String, Object>> result = citationService.listCitations(new HashMap<String, String>());
        assertTrue(result.size() >= 2);
    }

    @Test
    void listCitations_filterByDocumentCode_shouldReturnMatching() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit1 = buildValidCitationPayload();
        Map<String, Object> cit2 = buildValidCitationPayload();
        cit2.put("citation_id", "CIT_002");
        cit2.put("document_code", "DOC_002");
        citationService.importCitations(Arrays.asList(cit1, cit2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("documentCode", "DOC_001");
        List<Map<String, Object>> result = citationService.listCitations(filters);

        for (Map<String, Object> r : result) {
            assertEquals("DOC_001", r.get("document_code"));
        }
    }

    @Test
    void listCitations_filterByCitationType_shouldReturnMatching() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit1 = buildValidCitationPayload();
        cit1.put("citation_type", "SECTION");
        Map<String, Object> cit2 = buildValidCitationPayload();
        cit2.put("citation_id", "CIT_002");
        cit2.put("citation_type", "PAGE");
        citationService.importCitations(Arrays.asList(cit1, cit2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("citationType", "SECTION");
        List<Map<String, Object>> result = citationService.listCitations(filters);

        for (Map<String, Object> r : result) {
            assertEquals("SECTION", r.get("citation_type"));
        }
    }

    @Test
    void listCitations_filterBySection_shouldReturnMatching() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit1 = buildValidCitationPayload();
        cit1.put("section", "3.1");
        Map<String, Object> cit2 = buildValidCitationPayload();
        cit2.put("citation_id", "CIT_002");
        cit2.put("section", "4.2");
        citationService.importCitations(Arrays.asList(cit1, cit2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("section", "3.1");
        List<Map<String, Object>> result = citationService.listCitations(filters);

        for (Map<String, Object> r : result) {
            assertEquals("3.1", r.get("section"));
        }
    }

    @Test
    void listCitations_withLimit_shouldRespectLimit() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit1 = buildValidCitationPayload();
        Map<String, Object> cit2 = buildValidCitationPayload();
        cit2.put("citation_id", "CIT_002");
        Map<String, Object> cit3 = buildValidCitationPayload();
        cit3.put("citation_id", "CIT_003");
        citationService.importCitations(Arrays.asList(cit1, cit2, cit3));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("limit", "2");
        List<Map<String, Object>> result = citationService.listCitations(filters);

        assertTrue(result.size() <= 2);
    }

    @Test
    void listCitations_nullFilters_shouldReturnAll() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit1 = buildValidCitationPayload();
        citationService.importCitations(Arrays.asList(cit1));

        List<Map<String, Object>> result = citationService.listCitations(null);
        assertNotNull(result);
    }

    // =========================================================================
    // getCitation
    // =========================================================================

    @Test
    void getCitation_existingCitation_shouldReturnCitation() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        citationService.importCitations(Arrays.asList(cit));

        Map<String, Object> result = citationService.getCitation("CIT_001", null);
        assertEquals("CIT_001", result.get("citation_id"));
    }

    @Test
    void getCitation_nonExistingCitation_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> citationService.getCitation("NONEXISTENT", null));
    }

    @Test
    void getCitation_withTenantId_shouldResolveTenant() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("tenant_id", "tenant1");
        request.put("citations", Arrays.asList(cit));
        citationService.importCitations(request);

        Map<String, Object> result = citationService.getCitation("CIT_001", "tenant1");
        assertEquals("CIT_001", result.get("citation_id"));
    }

    @Test
    void getCitation_nullTenantId_shouldUseDefault() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit = buildValidCitationPayload();
        citationService.importCitations(Arrays.asList(cit));

        Map<String, Object> result = citationService.getCitation("CIT_001", null);
        assertNotNull(result);
    }

    // =========================================================================
    // getCitationsByDocument
    // =========================================================================

    @Test
    void getCitationsByDocument_shouldReturnMatchingCitations() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit1 = buildValidCitationPayload();
        Map<String, Object> cit2 = buildValidCitationPayload();
        cit2.put("citation_id", "CIT_002");
        cit2.put("document_code", "DOC_002");
        citationService.importCitations(Arrays.asList(cit1, cit2));

        List<Map<String, Object>> result = citationService.getCitationsByDocument("DOC_001", null);
        for (Map<String, Object> r : result) {
            assertEquals("DOC_001", r.get("document_code"));
        }
    }

    @Test
    void getCitationsByDocument_noMatching_shouldReturnEmpty() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        Map<String, Object> cit1 = buildValidCitationPayload();
        citationService.importCitations(Arrays.asList(cit1));

        List<Map<String, Object>> result = citationService.getCitationsByDocument("NONEXISTENT", null);
        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // citationCount
    // =========================================================================

    @Test
    void citationCount_shouldReturnStoreSize() {
        doNothing().when(persistenceService).saveSourceCitation(any(SourceCitation.class));

        assertEquals(0, citationService.citationCount());

        Map<String, Object> cit1 = buildValidCitationPayload();
        citationService.importCitations(Arrays.asList(cit1));

        assertEquals(1, citationService.citationCount());
    }

    // =========================================================================
    // rebuildFromPersistence
    // =========================================================================

    @Test
    void rebuildFromPersistence_shouldLoadPersistedCitations() {
        SourceCitation persisted = new SourceCitation();
        persisted.setTenantId("default");
        persisted.setCitationId("CIT_PERSISTED");
        persisted.setDocumentCode("DOC_P");

        when(persistenceService.listSourceCitations()).thenReturn(Arrays.asList(persisted));

        SourceCitationService freshService = new SourceCitationService(persistenceService);
        freshService.rebuildFromPersistence();

        assertEquals(1, freshService.citationCount());
        Map<String, Object> result = freshService.getCitation("CIT_PERSISTED", null);
        assertEquals("CIT_PERSISTED", result.get("citation_id"));
    }

    @Test
    void rebuildFromPersistence_emptyStore_shouldNotFail() {
        when(persistenceService.listSourceCitations()).thenReturn(new ArrayList<SourceCitation>());

        SourceCitationService freshService = new SourceCitationService(persistenceService);
        freshService.rebuildFromPersistence();

        assertEquals(0, freshService.citationCount());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private Map<String, Object> buildValidCitationPayload() {
        Map<String, Object> cit = new LinkedHashMap<String, Object>();
        cit.put("citation_id", "CIT_001");
        cit.put("document_code", "DOC_001");
        cit.put("citation_type", "SECTION");
        cit.put("section", "3.1");
        return cit;
    }
}
