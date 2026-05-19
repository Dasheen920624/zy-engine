package com.medkernel.provenance;

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
    private SourceAssetBindingService bindingService;

    @Mock
    private ProvenanceService provenanceService;

    private PublishGateService publishGateService;

    @BeforeEach
    void setUp() {
        publishGateService = new PublishGateService(bindingService, provenanceService);
    }

    // ---- checkPublishGate ----

    @Test
    void checkPublishGate_shouldPassWhenValidBindingAndDocument() {
        // 准备有效的绑定和文档
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("binding_id", "BIND_001");
        binding.put("asset_type", "PATHWAY");
        binding.put("asset_code", "PATHWAY_001");
        binding.put("document_code", "DOC_001");

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("document_code", "DOC_001");
        document.put("review_status", "APPROVED");
        document.put("expiry_date", "2027-12-31");

        when(bindingService.getBindingsByAsset("PATHWAY", "PATHWAY_001", "default"))
                .thenReturn(Arrays.asList(binding));
        when(provenanceService.getDocument("DOC_001", "default"))
                .thenReturn(document);

        Map<String, Object> result = publishGateService.checkPublishGate("PATHWAY", "PATHWAY_001", null);

        assertTrue((Boolean) result.get("passed"));
        assertNotNull(result.get("issues"));
        assertTrue(((List<?>) result.get("issues")).isEmpty());
    }

    @Test
    void checkPublishGate_shouldFailWhenNoBindings() {
        when(bindingService.getBindingsByAsset("PATHWAY", "PATHWAY_001", "default"))
                .thenReturn(new ArrayList<>());

        Map<String, Object> result = publishGateService.checkPublishGate("PATHWAY", "PATHWAY_001", null);

        assertFalse((Boolean) result.get("passed"));
        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        assertFalse(issues.isEmpty());
        assertEquals("MISSING_SOURCE", issues.get(0).get("code"));
    }

    @Test
    void checkPublishGate_shouldFailWhenDocumentNotReviewed() {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("binding_id", "BIND_001");
        binding.put("asset_type", "PATHWAY");
        binding.put("asset_code", "PATHWAY_001");
        binding.put("document_code", "DOC_001");

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("document_code", "DOC_001");
        document.put("review_status", "DRAFT");

        when(bindingService.getBindingsByAsset("PATHWAY", "PATHWAY_001", "default"))
                .thenReturn(Arrays.asList(binding));
        when(provenanceService.getDocument("DOC_001", "default"))
                .thenReturn(document);

        Map<String, Object> result = publishGateService.checkPublishGate("PATHWAY", "PATHWAY_001", null);

        assertFalse((Boolean) result.get("passed"));
        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        assertFalse(issues.isEmpty());
        assertEquals("SOURCE_NOT_REVIEWED", issues.get(0).get("code"));
    }

    @Test
    void checkPublishGate_shouldFailWhenDocumentExpired() {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("binding_id", "BIND_001");
        binding.put("asset_type", "PATHWAY");
        binding.put("asset_code", "PATHWAY_001");
        binding.put("document_code", "DOC_001");

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("document_code", "DOC_001");
        document.put("review_status", "APPROVED");
        document.put("expiry_date", "2020-01-01"); // 已过期

        when(bindingService.getBindingsByAsset("PATHWAY", "PATHWAY_001", "default"))
                .thenReturn(Arrays.asList(binding));
        when(provenanceService.getDocument("DOC_001", "default"))
                .thenReturn(document);

        Map<String, Object> result = publishGateService.checkPublishGate("PATHWAY", "PATHWAY_001", null);

        assertFalse((Boolean) result.get("passed"));
        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        assertFalse(issues.isEmpty());
        assertEquals("SOURCE_EXPIRED", issues.get(0).get("code"));
    }

    @Test
    void checkPublishGate_shouldPassWhenDocumentHasNoExpiry() {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("binding_id", "BIND_001");
        binding.put("asset_type", "RULE");
        binding.put("asset_code", "RULE_001");
        binding.put("document_code", "DOC_001");

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("document_code", "DOC_001");
        document.put("review_status", "REVIEWED");
        // 没有设置 expiry_date

        when(bindingService.getBindingsByAsset("RULE", "RULE_001", "default"))
                .thenReturn(Arrays.asList(binding));
        when(provenanceService.getDocument("DOC_001", "default"))
                .thenReturn(document);

        Map<String, Object> result = publishGateService.checkPublishGate("RULE", "RULE_001", null);

        assertTrue((Boolean) result.get("passed"));
    }

    @Test
    void checkPublishGate_shouldPassWhenOneBindingIsValid() {
        // 两个绑定，一个无效一个有效
        Map<String, Object> invalidBinding = new LinkedHashMap<>();
        invalidBinding.put("binding_id", "BIND_001");
        invalidBinding.put("asset_type", "PATHWAY");
        invalidBinding.put("asset_code", "PATHWAY_001");
        invalidBinding.put("document_code", "DOC_EXPIRED");

        Map<String, Object> validBinding = new LinkedHashMap<>();
        validBinding.put("binding_id", "BIND_002");
        validBinding.put("asset_type", "PATHWAY");
        validBinding.put("asset_code", "PATHWAY_001");
        validBinding.put("document_code", "DOC_VALID");

        Map<String, Object> expiredDoc = new LinkedHashMap<>();
        expiredDoc.put("document_code", "DOC_EXPIRED");
        expiredDoc.put("review_status", "APPROVED");
        expiredDoc.put("expiry_date", "2020-01-01");

        Map<String, Object> validDoc = new LinkedHashMap<>();
        validDoc.put("document_code", "DOC_VALID");
        validDoc.put("review_status", "APPROVED");
        validDoc.put("expiry_date", "2027-12-31");

        when(bindingService.getBindingsByAsset("PATHWAY", "PATHWAY_001", "default"))
                .thenReturn(Arrays.asList(invalidBinding, validBinding));
        when(provenanceService.getDocument("DOC_EXPIRED", "default"))
                .thenReturn(expiredDoc);
        when(provenanceService.getDocument("DOC_VALID", "default"))
                .thenReturn(validDoc);

        Map<String, Object> result = publishGateService.checkPublishGate("PATHWAY", "PATHWAY_001", null);

        // 有一个有效绑定，应该通过
        assertTrue((Boolean) result.get("passed"));
    }

    // ---- requirePublishGate ----

    @Test
    void requirePublishGate_shouldThrowWhenNotPassed() {
        when(bindingService.getBindingsByAsset("GRAPH", "GRAPH_001", "default"))
                .thenReturn(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> {
            publishGateService.requirePublishGate("GRAPH", "GRAPH_001", null);
        });
    }

    @Test
    void requirePublishGate_shouldNotThrowWhenPassed() {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("binding_id", "BIND_001");
        binding.put("asset_type", "GRAPH");
        binding.put("asset_code", "GRAPH_001");
        binding.put("document_code", "DOC_001");

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("document_code", "DOC_001");
        document.put("review_status", "APPROVED");
        document.put("expiry_date", "2027-12-31");

        when(bindingService.getBindingsByAsset("GRAPH", "GRAPH_001", "default"))
                .thenReturn(Arrays.asList(binding));
        when(provenanceService.getDocument("DOC_001", "default"))
                .thenReturn(document);

        // 不应该抛出异常
        publishGateService.requirePublishGate("GRAPH", "GRAPH_001", null);
    }

    // ---- collectSourceIssues ----

    @Test
    void collectSourceIssues_shouldAddIssuesToList() {
        when(bindingService.getBindingsByAsset("CONFIG_PACKAGE", "PKG_001", "default"))
                .thenReturn(new ArrayList<>());

        List<Map<String, Object>> issues = new ArrayList<>();
        publishGateService.collectSourceIssues("CONFIG_PACKAGE", "PKG_001", null, issues);

        assertFalse(issues.isEmpty());
        assertEquals("MISSING_SOURCE", issues.get(0).get("code"));
    }

    @Test
    void collectSourceIssues_shouldNotAddIssuesWhenPassed() {
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("binding_id", "BIND_001");
        binding.put("asset_type", "CONFIG_PACKAGE");
        binding.put("asset_code", "PKG_001");
        binding.put("document_code", "DOC_001");

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("document_code", "DOC_001");
        document.put("review_status", "APPROVED");

        when(bindingService.getBindingsByAsset("CONFIG_PACKAGE", "PKG_001", "default"))
                .thenReturn(Arrays.asList(binding));
        when(provenanceService.getDocument("DOC_001", "default"))
                .thenReturn(document);

        List<Map<String, Object>> issues = new ArrayList<>();
        publishGateService.collectSourceIssues("CONFIG_PACKAGE", "PKG_001", null, issues);

        assertTrue(issues.isEmpty());
    }
}
