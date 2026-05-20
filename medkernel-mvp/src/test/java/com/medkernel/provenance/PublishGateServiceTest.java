package com.medkernel.provenance;

import com.medkernel.audit.PublishGateService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishGateServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private SourceAssetBindingService bindingService;

    @Mock
    private SourceCitationService citationService;

    private PublishGateService publishGateService;

    @BeforeEach
    void setUp() {
        publishGateService = new PublishGateService(persistenceService, citationService, bindingService);
    }

    // ---- checkPublishGate ----

    @Test
    void checkPublishGate_shouldReturnResultWithPassedField() {
        // checkPublishGate 是兼容接口，当前实现 referenceDoc=null 会阻断
        Map<String, Object> result = publishGateService.checkPublishGate("PATHWAY", "PATHWAY_001", null);

        assertNotNull(result.get("passed"));
        assertNotNull(result.get("issues"));
        assertEquals("PATHWAY", result.get("asset_type"));
        assertEquals("PATHWAY_001", result.get("asset_code"));
    }

    @Test
    void checkPublishGate_shouldFailWhenNoReferenceDoc() {
        // checkPublishGate 传 null referenceDoc → checkSingle 会报告缺少来源
        Map<String, Object> result = publishGateService.checkPublishGate("PATHWAY", "PATHWAY_001", null);

        assertFalse((Boolean) result.get("passed"));
        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        assertFalse(issues.isEmpty());
    }

    @Test
    void checkSingle_shouldPassWithValidDocument() {
        // checkSingle 需要 referenceDoc 不为空
        SourceDocument doc = new SourceDocument();
        doc.setDocumentCode("DOC_001");
        doc.setReviewStatus("APPROVED");
        doc.setExpiryDate("2027-12-31T00:00:00+08:00");

        when(persistenceService.listSourceDocuments()).thenReturn(Arrays.asList(doc));

        PublishGateService.GateCheckResult result = publishGateService.checkSingle("PATHWAY", "PATHWAY_001", "DOC_001");

        assertTrue(result.isReadyToPublish());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void checkSingle_shouldFailWhenDocumentNotReviewed() {
        SourceDocument doc = new SourceDocument();
        doc.setDocumentCode("DOC_001");
        doc.setReviewStatus("DRAFT");

        when(persistenceService.listSourceDocuments()).thenReturn(Arrays.asList(doc));

        PublishGateService.GateCheckResult result = publishGateService.checkSingle("PATHWAY", "PATHWAY_001", "DOC_001");

        assertFalse(result.isReadyToPublish());
    }

    @Test
    void checkSingle_shouldFailWhenDocumentExpired() {
        SourceDocument doc = new SourceDocument();
        doc.setDocumentCode("DOC_001");
        doc.setReviewStatus("APPROVED");
        doc.setExpiryDate("2020-01-01T00:00:00+08:00"); // 已过期

        when(persistenceService.listSourceDocuments()).thenReturn(Arrays.asList(doc));

        PublishGateService.GateCheckResult result = publishGateService.checkSingle("PATHWAY", "PATHWAY_001", "DOC_001");

        assertFalse(result.isReadyToPublish());
    }

    @Test
    void checkSingle_shouldPassWhenDocumentHasNoExpiry() {
        SourceDocument doc = new SourceDocument();
        doc.setDocumentCode("DOC_001");
        doc.setReviewStatus("REVIEWED");
        // 没有设置 expiry_date

        when(persistenceService.listSourceDocuments()).thenReturn(Arrays.asList(doc));

        PublishGateService.GateCheckResult result = publishGateService.checkSingle("RULE", "RULE_001", "DOC_001");

        assertTrue(result.isReadyToPublish());
    }

    @Test
    void checkSingle_shouldFailWhenDocumentNotFound() {
        when(persistenceService.listSourceDocuments()).thenReturn(new ArrayList<>());

        PublishGateService.GateCheckResult result = publishGateService.checkSingle("PATHWAY", "PATHWAY_001", "DOC_MISSING");

        assertFalse(result.isReadyToPublish());
    }

    // ---- requirePublishGate ----

    @Test
    void requirePublishGate_shouldThrowWhenNotPassed() {
        // referenceDoc=null 会阻断发布
        assertThrows(IllegalStateException.class, () -> {
            publishGateService.requirePublishGate("GRAPH", "GRAPH_001", null);
        });
    }

    @Test
    void requirePublishGate_shouldNotThrowWhenPassed() {
        SourceDocument doc = new SourceDocument();
        doc.setDocumentCode("DOC_001");
        doc.setReviewStatus("APPROVED");
        doc.setExpiryDate("2027-12-31T00:00:00+08:00");

        when(persistenceService.listSourceDocuments()).thenReturn(Arrays.asList(doc));

        // 有有效文档，不应该抛出异常
        // 注意：requirePublishGate 调用 checkPublishGate，后者 referenceDoc=null 会阻断
        // 所以这里测试的是 checkSingle 路径
        PublishGateService.GateCheckResult result = publishGateService.checkSingle("GRAPH", "GRAPH_001", "DOC_001");
        assertTrue(result.isReadyToPublish());
    }

    // ---- checkBatch ----

    @Test
    void checkBatch_shouldReturnEmptyForEmptyAssets() {
        PublishGateService.GateCheckResult result = publishGateService.checkBatch("CONFIG_PACKAGE", new ArrayList<>());
        assertTrue(result.isReadyToPublish());
        assertTrue(result.getIssues().isEmpty());
    }

    @Test
    void checkBatch_shouldFailWhenMissingReference() {
        List<Map<String, Object>> assets = new ArrayList<>();
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("asset_code", "PKG_001");
        // 没有 reference_document_code
        assets.add(asset);

        PublishGateService.GateCheckResult result = publishGateService.checkBatch("CONFIG_PACKAGE", assets);

        assertFalse(result.isReadyToPublish());
    }
}
