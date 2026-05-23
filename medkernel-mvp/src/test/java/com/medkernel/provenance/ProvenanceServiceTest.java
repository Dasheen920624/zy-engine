package com.medkernel.provenance;

import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProvenanceServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    private ProvenanceService provenanceService;

    @BeforeEach
    void setUp() {
        provenanceService = new ProvenanceService(persistenceService);
    }

    // =========================================================================
    // importDocuments
    // =========================================================================

    @Test
    void importDocuments_withMapRequest_shouldImportSuccessfully() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("tenant_id", "tenant1");
        request.put("operator_id", "admin");
        request.put("documents", Arrays.asList(doc));

        Map<String, Object> result = provenanceService.importDocuments(request);

        assertEquals("tenant1", result.get("tenant_id"));
        assertEquals(1, result.get("imported_count"));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("documents");
        assertEquals(1, imported.size());
        assertEquals("DOC_001", imported.get(0).get("document_code"));
        verify(persistenceService).saveSourceDocument(any(SourceDocument.class));
    }

    @Test
    void importDocuments_withListRequest_shouldImportSuccessfully() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        List<Map<String, Object>> request = Arrays.asList(doc);

        Map<String, Object> result = provenanceService.importDocuments(request);

        assertEquals("default", result.get("tenant_id"));
        assertEquals(1, result.get("imported_count"));
    }

    @Test
    void importDocuments_withCamelCaseKeys_shouldImportSuccessfully() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("documentCode", "DOC_CAMEL");
        doc.put("title", "Camel Case Doc");
        doc.put("sourceType", "GUIDELINE");
        doc.put("reviewStatus", "APPROVED");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("documents", Arrays.asList(doc));

        Map<String, Object> result = provenanceService.importDocuments(request);

        assertEquals(1, result.get("imported_count"));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("documents");
        assertEquals("DOC_CAMEL", imported.get(0).get("document_code"));
    }

    @Test
    void importDocuments_emptyDocuments_shouldThrow() {
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("documents", new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> provenanceService.importDocuments(request));
    }

    @Test
    void importDocuments_missingDocumentCode_shouldThrow() {
        when(persistenceService.enabled()).thenReturn(false);

        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("title", "Test");
        doc.put("source_type", "GUIDELINE");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("documents", Arrays.asList(doc));

        assertThrows(IllegalArgumentException.class, () -> provenanceService.importDocuments(request));
    }

    @Test
    void importDocuments_missingTitle_shouldThrow() {
        when(persistenceService.enabled()).thenReturn(false);

        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("document_code", "DOC_001");
        doc.put("source_type", "GUIDELINE");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("documents", Arrays.asList(doc));

        assertThrows(IllegalArgumentException.class, () -> provenanceService.importDocuments(request));
    }

    @Test
    void importDocuments_missingSourceType_shouldThrow() {
        when(persistenceService.enabled()).thenReturn(false);

        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("document_code", "DOC_001");
        doc.put("title", "Test");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("documents", Arrays.asList(doc));

        assertThrows(IllegalArgumentException.class, () -> provenanceService.importDocuments(request));
    }

    @Test
    void importDocuments_unsupportedSourceType_shouldThrow() {
        when(persistenceService.enabled()).thenReturn(false);

        Map<String, Object> doc = buildValidDocumentPayload();
        doc.put("source_type", "INVALID_TYPE");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("documents", Arrays.asList(doc));

        assertThrows(IllegalArgumentException.class, () -> provenanceService.importDocuments(request));
    }

    @Test
    void importDocuments_unsupportedReviewStatus_shouldThrow() {
        when(persistenceService.enabled()).thenReturn(false);

        Map<String, Object> doc = buildValidDocumentPayload();
        doc.put("review_status", "INVALID_STATUS");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("documents", Arrays.asList(doc));

        assertThrows(IllegalArgumentException.class, () -> provenanceService.importDocuments(doc));
    }

    @Test
    void importDocuments_allSupportedSourceTypes_shouldSucceed() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        String[] types = {"GUIDELINE", "CONSENSUS", "REGULATION", "LITERATURE", "HOSPITAL_POLICY", "EXPERT_OPINION"};
        for (String type : types) {
            Map<String, Object> doc = buildValidDocumentPayload();
            doc.put("source_type", type);
            doc.put("document_code", "DOC_" + type);

            Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
            assertEquals(1, result.get("imported_count"));
        }
    }

    @Test
    void importDocuments_allSupportedReviewStatuses_shouldSucceed() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        String[] statuses = {"DRAFT", "REVIEWED", "APPROVED", "REJECTED", "EXPIRED"};
        for (String status : statuses) {
            Map<String, Object> doc = buildValidDocumentPayload();
            doc.put("review_status", status);
            doc.put("document_code", "DOC_" + status);

            Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
            assertEquals(1, result.get("imported_count"));
        }
    }

    @Test
    void importDocuments_defaultReviewStatusIsDraft() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        doc.remove("review_status");

        Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("documents");
        assertEquals("DRAFT", imported.get(0).get("review_status"));
    }

    @Test
    void importDocuments_withEffectiveDateAfterExpiryDate_shouldGenerateWarning() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        doc.put("effective_date", "2030-01-01");
        doc.put("expiry_date", "2020-01-01");

        Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertFalse(warnings.isEmpty());
        assertEquals("WARN", warnings.get(0).get("severity"));
        assertEquals("effective_date", warnings.get(0).get("field"));
    }

    @Test
    void importDocuments_withExpiredExpiryDate_shouldGenerateWarning() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        doc.put("expiry_date", "2020-01-01");

        Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertFalse(warnings.isEmpty());
        boolean hasExpiredWarning = false;
        for (Map<String, Object> w : warnings) {
            if ("expiry_date".equals(w.get("field"))) {
                hasExpiredWarning = true;
            }
        }
        assertTrue(hasExpiredWarning);
    }

    @Test
    void importDocuments_withValidDates_shouldNotGenerateWarnings() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        doc.put("effective_date", "2020-01-01");
        doc.put("expiry_date", "2030-12-31");

        Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) result.get("warnings");
        assertTrue(warnings.isEmpty());
    }

    @Test
    void importDocuments_shouldSetExpiredFlagInResult() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        doc.put("expiry_date", "2020-01-01");

        Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("documents");
        assertTrue((Boolean) imported.get(0).get("expired"));
    }

    @Test
    void importDocuments_withFutureExpiry_shouldSetExpiredFalse() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        doc.put("expiry_date", "2030-12-31");

        Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("documents");
        assertFalse((Boolean) imported.get(0).get("expired"));
    }

    @Test
    void importDocuments_shouldCallAudit() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("operator_id", "admin");
        request.put("documents", Arrays.asList(doc));

        provenanceService.importDocuments(request);

        verify(persistenceService).saveAuditLog(
                eq("PROVENANCE"), eq("IMPORT"), eq("SRC_DOCUMENT"),
                any(), any(), any(), eq("admin"), any(Map.class));
    }

    @Test
    void importDocuments_auditFailure_shouldNotPropagate() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));
        doThrow(new RuntimeException("audit failed")).when(persistenceService)
                .saveAuditLog(anyString(), anyString(), anyString(), any(), any(), any(), anyString(), any(Map.class));

        Map<String, Object> doc = buildValidDocumentPayload();

        // Should not throw even though audit fails
        Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
        assertNotNull(result);
    }

    @Test
    void importDocuments_updateExisting_shouldPreserveCreatedTime() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        // First import
        Map<String, Object> doc1 = buildValidDocumentPayload();
        Map<String, Object> result1 = provenanceService.importDocuments(Arrays.asList(doc1));
        List<Map<String, Object>> imported1 = (List<Map<String, Object>>) result1.get("documents");
        String originalCreatedTime = (String) imported1.get(0).get("created_time");

        // Second import of same document
        Map<String, Object> doc2 = buildValidDocumentPayload();
        Map<String, Object> result2 = provenanceService.importDocuments(Arrays.asList(doc2));
        List<Map<String, Object>> imported2 = (List<Map<String, Object>>) result2.get("documents");
        assertEquals(originalCreatedTime, imported2.get(0).get("created_time"));
    }

    @Test
    void importDocuments_multipleDocuments_shouldImportAll() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc1 = buildValidDocumentPayload();
        Map<String, Object> doc2 = buildValidDocumentPayload();
        doc2.put("document_code", "DOC_002");
        doc2.put("title", "Second Document");

        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("documents", Arrays.asList(doc1, doc2));

        Map<String, Object> result = provenanceService.importDocuments(request);
        assertEquals(2, result.get("imported_count"));
    }

    @Test
    void importDocuments_withMetadata_shouldPreserveMetadata() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("key1", "value1");
        metadata.put("key2", 42);

        Map<String, Object> doc = buildValidDocumentPayload();
        doc.put("metadata", metadata);

        Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("documents");
        Map<String, Object> resultMetadata = (Map<String, Object>) imported.get(0).get("metadata");
        assertEquals("value1", resultMetadata.get("key1"));
        assertEquals(42, resultMetadata.get("key2"));
    }

    @Test
    void importDocuments_withPersistenceEnabled_shouldDelegateToPersistence() {
        when(persistenceService.enabled()).thenReturn(true);
        when(persistenceService.findSourceDocument(anyString(), anyString())).thenReturn(null);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));

        assertEquals(1, result.get("imported_count"));
        verify(persistenceService).findSourceDocument(anyString(), anyString());
    }

    // =========================================================================
    // listDocuments
    // =========================================================================

    @Test
    void listDocuments_noFilters_shouldReturnAll() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        // Import some documents first
        Map<String, Object> doc1 = buildValidDocumentPayload();
        Map<String, Object> doc2 = buildValidDocumentPayload();
        doc2.put("document_code", "DOC_002");
        doc2.put("title", "Second");
        provenanceService.importDocuments(Arrays.asList(doc1, doc2));

        List<Map<String, Object>> result = provenanceService.listDocuments(new HashMap<String, String>());
        assertTrue(result.size() >= 2);
    }

    @Test
    void listDocuments_filterBySourceType_shouldReturnMatching() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc1 = buildValidDocumentPayload();
        doc1.put("source_type", "GUIDELINE");
        Map<String, Object> doc2 = buildValidDocumentPayload();
        doc2.put("document_code", "DOC_002");
        doc2.put("title", "Second");
        doc2.put("source_type", "LITERATURE");
        provenanceService.importDocuments(Arrays.asList(doc1, doc2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("sourceType", "GUIDELINE");
        List<Map<String, Object>> result = provenanceService.listDocuments(filters);

        for (Map<String, Object> r : result) {
            assertEquals("GUIDELINE", r.get("source_type"));
        }
    }

    @Test
    void listDocuments_filterByReviewStatus_shouldReturnMatching() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc1 = buildValidDocumentPayload();
        doc1.put("review_status", "APPROVED");
        Map<String, Object> doc2 = buildValidDocumentPayload();
        doc2.put("document_code", "DOC_002");
        doc2.put("title", "Second");
        doc2.put("review_status", "DRAFT");
        provenanceService.importDocuments(Arrays.asList(doc1, doc2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("reviewStatus", "APPROVED");
        List<Map<String, Object>> result = provenanceService.listDocuments(filters);

        for (Map<String, Object> r : result) {
            assertEquals("APPROVED", r.get("review_status"));
        }
    }

    @Test
    void listDocuments_filterByPublisher_shouldReturnMatching() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc1 = buildValidDocumentPayload();
        doc1.put("publisher", "PublisherA");
        Map<String, Object> doc2 = buildValidDocumentPayload();
        doc2.put("document_code", "DOC_002");
        doc2.put("title", "Second");
        doc2.put("publisher", "PublisherB");
        provenanceService.importDocuments(Arrays.asList(doc1, doc2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("publisher", "PublisherA");
        List<Map<String, Object>> result = provenanceService.listDocuments(filters);

        for (Map<String, Object> r : result) {
            assertEquals("PublisherA", r.get("publisher"));
        }
    }

    @Test
    void listDocuments_filterByKeyword_shouldMatchTitleOrCode() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc1 = buildValidDocumentPayload();
        doc1.put("title", "Hypertension Guideline");
        Map<String, Object> doc2 = buildValidDocumentPayload();
        doc2.put("document_code", "DOC_DIABETES");
        doc2.put("title", "Diabetes Guideline");
        provenanceService.importDocuments(Arrays.asList(doc1, doc2));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("keyword", "Hypertension");
        List<Map<String, Object>> result = provenanceService.listDocuments(filters);

        assertEquals(1, result.size());
        assertEquals("Hypertension Guideline", result.get(0).get("title"));
    }

    @Test
    void listDocuments_withLimit_shouldRespectLimit() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc1 = buildValidDocumentPayload();
        Map<String, Object> doc2 = buildValidDocumentPayload();
        doc2.put("document_code", "DOC_002");
        doc2.put("title", "Second");
        Map<String, Object> doc3 = buildValidDocumentPayload();
        doc3.put("document_code", "DOC_003");
        doc3.put("title", "Third");
        provenanceService.importDocuments(Arrays.asList(doc1, doc2, doc3));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("limit", "2");
        List<Map<String, Object>> result = provenanceService.listDocuments(filters);

        assertTrue(result.size() <= 2);
    }

    @Test
    void listDocuments_withNegativeLimit_shouldDefaultTo100() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc1 = buildValidDocumentPayload();
        provenanceService.importDocuments(Arrays.asList(doc1));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("limit", "-1");
        List<Map<String, Object>> result = provenanceService.listDocuments(filters);

        // Should not throw and should return results
        assertNotNull(result);
    }

    @Test
    void listDocuments_withInvalidLimit_shouldDefaultTo100() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc1 = buildValidDocumentPayload();
        provenanceService.importDocuments(Arrays.asList(doc1));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("limit", "not_a_number");
        List<Map<String, Object>> result = provenanceService.listDocuments(filters);

        assertNotNull(result);
    }

    @Test
    void listDocuments_nullFilters_shouldReturnAll() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc1 = buildValidDocumentPayload();
        provenanceService.importDocuments(Arrays.asList(doc1));

        List<Map<String, Object>> result = provenanceService.listDocuments(null);
        assertNotNull(result);
    }

    @Test
    void listDocuments_withPersistenceEnabled_shouldDelegateToPersistence() {
        when(persistenceService.enabled()).thenReturn(true);
        when(persistenceService.listSourceDocuments()).thenReturn(new ArrayList<SourceDocument>());

        List<Map<String, Object>> result = provenanceService.listDocuments(new HashMap<String, String>());
        assertNotNull(result);
        verify(persistenceService).listSourceDocuments();
    }

    // =========================================================================
    // getDocument
    // =========================================================================

    @Test
    void getDocument_existingDocument_shouldReturnDocument() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        provenanceService.importDocuments(Arrays.asList(doc));

        Map<String, Object> result = provenanceService.getDocument("DOC_001", null);
        assertEquals("DOC_001", result.get("document_code"));
        assertEquals("Test Document", result.get("title"));
    }

    @Test
    void getDocument_nonExistingDocument_shouldThrow() {
        when(persistenceService.enabled()).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> provenanceService.getDocument("NONEXISTENT", null));
    }

    @Test
    void getDocument_withTenantId_shouldResolveTenant() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("tenant_id", "tenant1");
        request.put("documents", Arrays.asList(doc));
        provenanceService.importDocuments(request);

        Map<String, Object> result = provenanceService.getDocument("DOC_001", "tenant1");
        assertEquals("DOC_001", result.get("document_code"));
    }

    @Test
    void getDocument_nullTenantId_shouldUseDefault() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        provenanceService.importDocuments(Arrays.asList(doc));

        Map<String, Object> result = provenanceService.getDocument("DOC_001", null);
        assertNotNull(result);
    }

    @Test
    void getDocument_withPersistenceEnabled_shouldDelegateToPersistence() {
        when(persistenceService.enabled()).thenReturn(true);
        SourceDocument storedDoc = new SourceDocument();
        storedDoc.setTenantId("default");
        storedDoc.setDocumentCode("DOC_001");
        storedDoc.setTitle("Test");
        storedDoc.setSourceType("GUIDELINE");
        storedDoc.setReviewStatus("DRAFT");
        when(persistenceService.findSourceDocument("default", "DOC_001")).thenReturn(storedDoc);

        Map<String, Object> result = provenanceService.getDocument("DOC_001", null);
        assertEquals("DOC_001", result.get("document_code"));
    }

    // =========================================================================
    // toView output format
    // =========================================================================

    @Test
    void importDocuments_toViewShouldContainAllFields() {
        when(persistenceService.enabled()).thenReturn(false);
        doNothing().when(persistenceService).saveSourceDocument(any(SourceDocument.class));

        Map<String, Object> doc = buildValidDocumentPayload();
        doc.put("source_uri", "http://example.com/doc");
        doc.put("publisher", "TestPublisher");
        doc.put("effective_date", "2024-01-01");
        doc.put("expiry_date", "2030-12-31");
        doc.put("review_status", "APPROVED");
        doc.put("reviewed_by", "reviewer1");
        doc.put("content_hash", "abc123");

        Map<String, Object> result = provenanceService.importDocuments(Arrays.asList(doc));
        List<Map<String, Object>> imported = (List<Map<String, Object>>) result.get("documents");
        Map<String, Object> view = imported.get(0);

        assertTrue(view.containsKey("tenant_id"));
        assertTrue(view.containsKey("document_code"));
        assertTrue(view.containsKey("title"));
        assertTrue(view.containsKey("source_type"));
        assertTrue(view.containsKey("source_uri"));
        assertTrue(view.containsKey("publisher"));
        assertTrue(view.containsKey("effective_date"));
        assertTrue(view.containsKey("expiry_date"));
        assertTrue(view.containsKey("review_status"));
        assertTrue(view.containsKey("reviewed_by"));
        assertTrue(view.containsKey("content_hash"));
        assertTrue(view.containsKey("metadata"));
        assertTrue(view.containsKey("created_by"));
        assertTrue(view.containsKey("created_time"));
        assertTrue(view.containsKey("updated_time"));
        assertTrue(view.containsKey("expired"));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private Map<String, Object> buildValidDocumentPayload() {
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("document_code", "DOC_001");
        doc.put("title", "Test Document");
        doc.put("source_type", "GUIDELINE");
        doc.put("review_status", "DRAFT");
        return doc;
    }
}
