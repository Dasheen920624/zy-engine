package com.medkernel.audit;

import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.provenance.SourceAssetBindingService;
import com.medkernel.provenance.SourceCitationService;
import com.medkernel.provenance.SourceDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishGateServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private SourceCitationService citationService;

    @Mock
    private SourceAssetBindingService bindingService;

    private PublishGateService publishGateService;

    @BeforeEach
    void setUp() {
        publishGateService = new PublishGateService(persistenceService, citationService, bindingService);
    }

    // ---- checkSingle ----

    @Test
    void checkSingle_shouldBlockWhenReferenceDocIsNull() {
        PublishGateService.GateCheckResult result = publishGateService.checkSingle("RULE", "R001", null);

        assertFalse(result.isReadyToPublish());
        assertEquals(1, result.getIssues().size());
        assertEquals("ERROR", result.getIssues().get(0).severity);
        assertTrue(result.getIssues().get(0).message.contains("缺少来源文档绑定"));
    }

    @Test
    void checkSingle_shouldBlockWhenReferenceDocIsEmpty() {
        PublishGateService.GateCheckResult result = publishGateService.checkSingle("RULE", "R001", "  ");

        assertFalse(result.isReadyToPublish());
        assertEquals(1, result.getIssues().size());
        assertEquals("ERROR", result.getIssues().get(0).severity);
    }

    @Test
    void checkSingle_shouldBlockWhenSourceDocNotFound() {
        when(persistenceService.listSourceDocuments()).thenReturn(Collections.<SourceDocument>emptyList());

        PublishGateService.GateCheckResult result = publishGateService.checkSingle("RULE", "R001", "DOC_MISSING");

        assertFalse(result.isReadyToPublish());
        assertEquals(1, result.getIssues().size());
        assertTrue(result.getIssues().get(0).message.contains("来源文档不存在"));
    }

    @Test
    void checkSingle_shouldBlockWhenSourceDocExpired() {
        SourceDocument expiredDoc = new SourceDocument();
        expiredDoc.setDocumentCode("DOC_EXP");
        expiredDoc.setExpiryDate("2020-01-01T00:00:00+08:00");
        expiredDoc.setReviewStatus("REVIEWED");

        when(persistenceService.listSourceDocuments()).thenReturn(Arrays.asList(expiredDoc));

        PublishGateService.GateCheckResult result = publishGateService.checkSingle("RULE", "R001", "DOC_EXP");

        assertFalse(result.isReadyToPublish());
        boolean hasExpiredIssue = false;
        for (PublishGateService.GateCheckResult.Issue issue : result.getIssues()) {
            if (issue.message.contains("已过期")) {
                hasExpiredIssue = true;
            }
        }
        assertTrue(hasExpiredIssue, "Should have expired issue");
    }

    @Test
    void checkSingle_shouldBlockWhenSourceDocNotReviewed() {
        SourceDocument unreviewedDoc = new SourceDocument();
        unreviewedDoc.setDocumentCode("DOC_UR");
        unreviewedDoc.setExpiryDate("2099-12-31T23:59:59+08:00");
        unreviewedDoc.setReviewStatus("DRAFT");

        when(persistenceService.listSourceDocuments()).thenReturn(Arrays.asList(unreviewedDoc));

        PublishGateService.GateCheckResult result = publishGateService.checkSingle("RULE", "R001", "DOC_UR");

        assertFalse(result.isReadyToPublish());
        boolean hasUnreviewedIssue = false;
        for (PublishGateService.GateCheckResult.Issue issue : result.getIssues()) {
            if (issue.message.contains("未经审核")) {
                hasUnreviewedIssue = true;
            }
        }
        assertTrue(hasUnreviewedIssue, "Should have unreviewed issue");
    }

    @Test
    void checkSingle_shouldPassWhenSourceDocValid() {
        SourceDocument validDoc = new SourceDocument();
        validDoc.setDocumentCode("DOC_OK");
        validDoc.setExpiryDate("2099-12-31T23:59:59+08:00");
        validDoc.setReviewStatus("REVIEWED");

        when(persistenceService.listSourceDocuments()).thenReturn(Arrays.asList(validDoc));

        PublishGateService.GateCheckResult result = publishGateService.checkSingle("RULE", "R001", "DOC_OK");

        assertTrue(result.isReadyToPublish());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void checkSingle_shouldPassWhenSourceDocApproved() {
        SourceDocument approvedDoc = new SourceDocument();
        approvedDoc.setDocumentCode("DOC_APP");
        approvedDoc.setExpiryDate("2099-12-31T23:59:59+08:00");
        approvedDoc.setReviewStatus("APPROVED");

        when(persistenceService.listSourceDocuments()).thenReturn(Arrays.asList(approvedDoc));

        PublishGateService.GateCheckResult result = publishGateService.checkSingle("RULE", "R001", "DOC_APP");

        assertTrue(result.isReadyToPublish());
    }

    // ---- checkGraphReference ----

    @Test
    void checkGraphReference_shouldBlockWhenNoReference() {
        PublishGateService.GateCheckResult result = publishGateService.checkGraphReference("GRAPH_V1", null);

        assertFalse(result.isReadyToPublish());
        assertEquals("GRAPH", result.getIssues().get(0).assetType);
    }

    // ---- checkRuleReference ----

    @Test
    void checkRuleReference_shouldBlockWhenNoReference() {
        PublishGateService.GateCheckResult result = publishGateService.checkRuleReference("R001", null);

        assertFalse(result.isReadyToPublish());
        assertEquals("RULE", result.getIssues().get(0).assetType);
    }

    // ---- checkPathwayReferences ----

    @Test
    void checkPathwayReferences_shouldPassWhenEmpty() {
        PublishGateService.GateCheckResult result = publishGateService.checkPathwayReferences(null);

        assertTrue(result.isReadyToPublish());
    }

    @Test
    void checkPathwayReferences_shouldPassWhenNoMissingRefs() {
        PublishGateService.GateCheckResult result = publishGateService.checkPathwayReferences(
                Collections.<Map<String, Object>>emptyList());

        assertTrue(result.isReadyToPublish());
    }

    @Test
    void checkPathwayReferences_shouldBlockWhenMissingRefsExist() {
        List<Map<String, Object>> missingRefs = new ArrayList<>();
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("element_code", "NODE_001");
        ref.put("severity", "WARN");
        ref.put("message", "节点缺少来源文档绑定");
        missingRefs.add(ref);

        PublishGateService.GateCheckResult result = publishGateService.checkPathwayReferences(missingRefs);

        assertFalse(result.isReadyToPublish());
        assertEquals("PATHWAY", result.getIssues().get(0).assetType);
    }

    // ---- checkBatch ----

    @Test
    void checkBatch_shouldPassWhenEmpty() {
        PublishGateService.GateCheckResult result = publishGateService.checkBatch("RULE", null);

        assertTrue(result.isReadyToPublish());
    }

    @Test
    void checkBatch_shouldBlockWhenAnyAssetMissing() {
        List<Map<String, Object>> assets = new ArrayList<>();
        Map<String, Object> asset1 = new LinkedHashMap<>();
        asset1.put("asset_code", "R001");
        asset1.put("reference_document_code", null);
        assets.add(asset1);

        PublishGateService.GateCheckResult result = publishGateService.checkBatch("RULE", assets);

        assertFalse(result.isReadyToPublish());
    }

    // ---- checkConfigPackageSourceReview ----

    @Test
    void checkConfigPackageSourceReview_shouldPassWhenNull() {
        PublishGateService.GateCheckResult result = publishGateService.checkConfigPackageSourceReview(null);

        assertTrue(result.isReadyToPublish());
    }

    @Test
    void checkConfigPackageSourceReview_shouldPassWhenNotEnabled() {
        Map<String, Object> sourceReview = new LinkedHashMap<>();
        sourceReview.put("enabled", false);

        PublishGateService.GateCheckResult result = publishGateService.checkConfigPackageSourceReview(sourceReview);

        assertTrue(result.isReadyToPublish());
    }

    @Test
    void checkConfigPackageSourceReview_shouldBlockWhenBlocked() {
        Map<String, Object> sourceReview = new LinkedHashMap<>();
        sourceReview.put("enabled", true);
        sourceReview.put("blocked", true);

        PublishGateService.GateCheckResult result = publishGateService.checkConfigPackageSourceReview(sourceReview);

        assertFalse(result.isReadyToPublish());
    }

    @Test
    void checkConfigPackageSourceReview_shouldBlockWhenMissingSources() {
        Map<String, Object> sourceReview = new LinkedHashMap<>();
        sourceReview.put("enabled", true);
        sourceReview.put("blocked", false);
        sourceReview.put("missing_count", 3);
        sourceReview.put("expired_count", 0);
        sourceReview.put("unreviewed_count", 0);
        sourceReview.put("allow_publish", true);

        PublishGateService.GateCheckResult result = publishGateService.checkConfigPackageSourceReview(sourceReview);

        assertFalse(result.isReadyToPublish());
    }

    // ---- formatBlockingMessage ----

    @Test
    void formatBlockingMessage_shouldReturnNullWhenReady() {
        PublishGateService.GateCheckResult result = new PublishGateService.GateCheckResult();

        assertNull(publishGateService.formatBlockingMessage(result));
    }

    @Test
    void formatBlockingMessage_shouldReturnMessageWhenBlocked() {
        PublishGateService.GateCheckResult result = new PublishGateService.GateCheckResult();
        result.addIssue("ERROR", "ref", "missing source", "R001", "RULE");

        String message = publishGateService.formatBlockingMessage(result);

        assertNotNull(message);
        assertTrue(message.contains("发布门禁检查未通过"));
        assertTrue(message.contains("missing source"));
    }

    // ---- GateCheckResult ----

    @Test
    void gateCheckResult_shouldBeReadyWhenNoIssues() {
        PublishGateService.GateCheckResult result = new PublishGateService.GateCheckResult();

        assertTrue(result.isReadyToPublish());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void gateCheckResult_shouldNotBeReadyWhenHasError() {
        PublishGateService.GateCheckResult result = new PublishGateService.GateCheckResult();
        result.addIssue("ERROR", "field", "message", "code", "type");

        assertFalse(result.isReadyToPublish());
    }

    @Test
    void gateCheckResult_shouldBeReadyWhenOnlyWarnings() {
        PublishGateService.GateCheckResult result = new PublishGateService.GateCheckResult();
        result.addIssue("WARN", "field", "warning", "code", "type");

        assertTrue(result.isReadyToPublish());
    }

    @Test
    void gateCheckResult_mergeShouldCombineIssues() {
        PublishGateService.GateCheckResult result1 = new PublishGateService.GateCheckResult();
        result1.addIssue("ERROR", "f1", "msg1", "c1", "t1");

        PublishGateService.GateCheckResult result2 = new PublishGateService.GateCheckResult();
        result2.addIssue("WARN", "f2", "msg2", "c2", "t2");

        result1.merge(result2);

        assertEquals(2, result1.getIssues().size());
    }

    // ---- auditGateCheck ----

    @Test
    void auditGateCheck_shouldNotThrow() {
        PublishGateService.GateCheckResult result = new PublishGateService.GateCheckResult();
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(), any(), any(), any(), any());

        // Should not throw
        publishGateService.auditGateCheck("GRAPH", "ACTIVATE", "GRAPH_VERSION", "V1", "SYSTEM", result);
    }

    private static void assertNull(Object obj) {
        org.junit.jupiter.api.Assertions.assertNull(obj);
    }
}
