package com.medkernel.knowledge;

import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private OrganizationContextService organizationContextService;

    private KnowledgeService knowledgeService;

    private OrganizationContext orgContext;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(organizationContextService);
        orgContext = new OrganizationContext();
        orgContext.setTenantId("TENANT-001");
    }

    // ==================== 来源注册 registerSource ====================

    @Test
    void registerSource_shouldCreateSourceWithAutoCode() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("source_name", "ICD-10");
        request.put("source_type", "TERMINOLOGY");
        request.put("publisher", "WHO");

        KnowledgeSourceRegistry source = knowledgeService.registerSource(request, orgContext);

        assertNotNull(source.getSourceCode());
        assertTrue(source.getSourceCode().startsWith("KS-"));
        assertEquals("TENANT-001", source.getTenantId());
        assertEquals("ICD-10", source.getSourceName());
        assertEquals("TERMINOLOGY", source.getSourceType());
        assertEquals("WHO", source.getPublisher());
        assertEquals("PENDING", source.getReviewStatus());
        assertNotNull(source.getCreatedTime());
        assertNotNull(source.getUpdatedTime());
    }

    @Test
    void registerSource_shouldUseProvidedSourceCode() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("source_code", "SRC-001");
        request.put("source_name", "Test Source");

        KnowledgeSourceRegistry source = knowledgeService.registerSource(request, orgContext);

        assertEquals("SRC-001", source.getSourceCode());
    }

    @Test
    void registerSource_shouldThrowWhenSourceCodeAlreadyExists() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("source_code", "DUP-001");
        request.put("source_name", "First");

        knowledgeService.registerSource(request, orgContext);

        Map<String, Object> request2 = new HashMap<String, Object>();
        request2.put("source_code", "DUP-001");
        request2.put("source_name", "Second");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> knowledgeService.registerSource(request2, orgContext));
        assertTrue(ex.getMessage().contains("DUP-001"));
    }

    @Test
    void registerSource_shouldSetAllFields() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("source_code", "FULL-001");
        request.put("source_name", "Full Source");
        request.put("source_type", "GUIDELINE");
        request.put("publisher", "NHC");
        request.put("region", "CN");
        request.put("language", "zh-CN");
        request.put("release_version", "2024v1");
        request.put("release_date", "2024-01-01");
        request.put("effective_date", "2024-02-01");
        request.put("expiry_date", "2025-02-01");
        request.put("authority_level", "OFFICIAL");
        request.put("license_scope", "NATIONAL");
        request.put("license_type", "OPEN");
        request.put("redistribution_allowed", "true");
        request.put("commercial_use_allowed", false);
        request.put("export_allowed", "true");
        request.put("fetch_method", "API");
        request.put("source_uri", "https://example.com");
        request.put("raw_hash", "abc123");
        request.put("parsed_hash", "def456");
        request.put("description", "Test description");
        request.put("created_by", "admin");

        KnowledgeSourceRegistry source = knowledgeService.registerSource(request, orgContext);

        assertEquals("FULL-001", source.getSourceCode());
        assertEquals("Full Source", source.getSourceName());
        assertEquals("GUIDELINE", source.getSourceType());
        assertEquals("NHC", source.getPublisher());
        assertEquals("CN", source.getRegion());
        assertEquals("zh-CN", source.getLanguage());
        assertEquals("2024v1", source.getReleaseVersion());
        assertEquals("2024-01-01", source.getReleaseDate());
        assertEquals("2024-02-01", source.getEffectiveDate());
        assertEquals("2025-02-01", source.getExpiryDate());
        assertEquals("OFFICIAL", source.getAuthorityLevel());
        assertEquals("NATIONAL", source.getLicenseScope());
        assertEquals("OPEN", source.getLicenseType());
        assertTrue(source.isRedistributionAllowed());
        assertFalse(source.isCommercialUseAllowed());
        assertTrue(source.isExportAllowed());
        assertEquals("API", source.getFetchMethod());
        assertEquals("https://example.com", source.getSourceUri());
        assertEquals("abc123", source.getRawHash());
        assertEquals("def456", source.getParsedHash());
        assertEquals("Test description", source.getDescription());
        assertEquals("admin", source.getCreatedBy());
    }

    @Test
    void registerSource_shouldHandleEmptySourceCode() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("source_code", "");
        request.put("source_name", "Empty Code Source");

        KnowledgeSourceRegistry source = knowledgeService.registerSource(request, orgContext);
        assertNotNull(source.getSourceCode());
        assertTrue(source.getSourceCode().startsWith("KS-"));
    }

    // ==================== 更新来源 updateSource ====================

    @Test
    void updateSource_shouldUpdateProvidedFields() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("source_code", "UPD-001");
        createReq.put("source_name", "Original");
        knowledgeService.registerSource(createReq, orgContext);

        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("source_name", "Updated");
        updateReq.put("region", "US");

        KnowledgeSourceRegistry updated = knowledgeService.updateSource("UPD-001", updateReq, orgContext);

        assertEquals("Updated", updated.getSourceName());
        assertEquals("US", updated.getRegion());
        assertNotNull(updated.getUpdatedTime());
    }

    @Test
    void updateSource_shouldThrowWhenSourceNotFound() {
        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("source_name", "Updated");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> knowledgeService.updateSource("NONEXISTENT", updateReq, orgContext));
        assertTrue(ex.getMessage().contains("NONEXISTENT"));
    }

    @Test
    void updateSource_shouldThrowWhenTenantMismatch() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("source_code", "TENANT-MISMATCH");
        createReq.put("source_name", "Original");
        knowledgeService.registerSource(createReq, orgContext);

        OrganizationContext otherContext = new OrganizationContext();
        otherContext.setTenantId("OTHER-TENANT");

        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("source_name", "Hacked");

        assertThrows(IllegalArgumentException.class,
                () -> knowledgeService.updateSource("TENANT-MISMATCH", updateReq, otherContext));
    }

    // ==================== 审核来源 reviewSource ====================

    @Test
    void reviewSource_shouldApproveSource() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("source_code", "REV-001");
        createReq.put("source_name", "Review Source");
        knowledgeService.registerSource(createReq, orgContext);

        KnowledgeSourceRegistry reviewed = knowledgeService.reviewSource("REV-001", "APPROVED", "reviewer1", orgContext);

        assertEquals("APPROVED", reviewed.getReviewStatus());
        assertEquals("reviewer1", reviewed.getReviewedBy());
        assertNotNull(reviewed.getReviewedTime());
    }

    @Test
    void reviewSource_shouldRejectSource() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("source_code", "REV-002");
        createReq.put("source_name", "Review Source 2");
        knowledgeService.registerSource(createReq, orgContext);

        KnowledgeSourceRegistry reviewed = knowledgeService.reviewSource("REV-002", "REJECTED", "reviewer2", orgContext);

        assertEquals("REJECTED", reviewed.getReviewStatus());
    }

    @Test
    void reviewSource_shouldThrowForInvalidStatus() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("source_code", "REV-003");
        createReq.put("source_name", "Review Source 3");
        knowledgeService.registerSource(createReq, orgContext);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> knowledgeService.reviewSource("REV-003", "INVALID", "reviewer", orgContext));
        assertTrue(ex.getMessage().contains("APPROVED") || ex.getMessage().contains("REJECTED"));
    }

    @Test
    void reviewSource_shouldThrowWhenSourceNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> knowledgeService.reviewSource("NONEXISTENT", "APPROVED", "reviewer", orgContext));
    }

    // ==================== 列出来源 listSources ====================

    @Test
    void listSources_shouldReturnSourcesForTenant() {
        Map<String, Object> req1 = new HashMap<String, Object>();
        req1.put("source_code", "LS-001");
        req1.put("source_name", "Source 1");
        req1.put("source_type", "TERMINOLOGY");
        knowledgeService.registerSource(req1, orgContext);

        Map<String, Object> req2 = new HashMap<String, Object>();
        req2.put("source_code", "LS-002");
        req2.put("source_name", "Source 2");
        req2.put("source_type", "GUIDELINE");
        knowledgeService.registerSource(req2, orgContext);

        List<KnowledgeSourceRegistry> sources = knowledgeService.listSources(null, orgContext);
        assertEquals(2, sources.size());
    }

    @Test
    void listSources_shouldFilterBySourceType() {
        Map<String, Object> req1 = new HashMap<String, Object>();
        req1.put("source_code", "FT-001");
        req1.put("source_name", "Term Source");
        req1.put("source_type", "TERMINOLOGY");
        knowledgeService.registerSource(req1, orgContext);

        Map<String, Object> req2 = new HashMap<String, Object>();
        req2.put("source_code", "FT-002");
        req2.put("source_name", "Guide Source");
        req2.put("source_type", "GUIDELINE");
        knowledgeService.registerSource(req2, orgContext);

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("source_type", "TERMINOLOGY");

        List<KnowledgeSourceRegistry> sources = knowledgeService.listSources(filters, orgContext);
        assertEquals(1, sources.size());
        assertEquals("TERMINOLOGY", sources.get(0).getSourceType());
    }

    @Test
    void listSources_shouldFilterByReviewStatus() {
        Map<String, Object> req1 = new HashMap<String, Object>();
        req1.put("source_code", "FRS-001");
        req1.put("source_name", "Source");
        knowledgeService.registerSource(req1, orgContext);
        knowledgeService.reviewSource("FRS-001", "APPROVED", "admin", orgContext);

        Map<String, Object> req2 = new HashMap<String, Object>();
        req2.put("source_code", "FRS-002");
        req2.put("source_name", "Source 2");
        knowledgeService.registerSource(req2, orgContext);

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("review_status", "APPROVED");

        List<KnowledgeSourceRegistry> sources = knowledgeService.listSources(filters, orgContext);
        assertEquals(1, sources.size());
        assertEquals("APPROVED", sources.get(0).getReviewStatus());
    }

    @Test
    void listSources_shouldNotReturnOtherTenantSources() {
        Map<String, Object> req = new HashMap<String, Object>();
        req.put("source_code", "CROSS-001");
        req.put("source_name", "Cross Source");
        knowledgeService.registerSource(req, orgContext);

        OrganizationContext otherContext = new OrganizationContext();
        otherContext.setTenantId("OTHER-TENANT");

        List<KnowledgeSourceRegistry> sources = knowledgeService.listSources(null, otherContext);
        assertTrue(sources.isEmpty());
    }

    // ==================== 获取来源 getSource ====================

    @Test
    void getSource_shouldReturnSource() {
        Map<String, Object> req = new HashMap<String, Object>();
        req.put("source_code", "GET-001");
        req.put("source_name", "Get Source");
        knowledgeService.registerSource(req, orgContext);

        KnowledgeSourceRegistry source = knowledgeService.getSource("GET-001", orgContext);
        assertNotNull(source);
        assertEquals("GET-001", source.getSourceCode());
    }

    @Test
    void getSource_shouldReturnNullWhenNotFound() {
        KnowledgeSourceRegistry source = knowledgeService.getSource("NONEXISTENT", orgContext);
        assertNull(source);
    }

    @Test
    void getSource_shouldReturnNullForOtherTenant() {
        Map<String, Object> req = new HashMap<String, Object>();
        req.put("source_code", "CROSS-GET-001");
        req.put("source_name", "Cross Source");
        knowledgeService.registerSource(req, orgContext);

        OrganizationContext otherContext = new OrganizationContext();
        otherContext.setTenantId("OTHER-TENANT");

        assertNull(knowledgeService.getSource("CROSS-GET-001", otherContext));
    }

    // ==================== 知识订阅 createSubscription ====================

    @Test
    void createSubscription_shouldCreateWithDefaults() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("subscriber_id", "USER-001");
        request.put("subscriber_name", "Dr. Zhang");
        request.put("topic_type", "DISEASE");
        request.put("topic_code", "I10");
        request.put("topic_name", "Hypertension");
        request.put("created_by", "admin");

        KnowledgeSubscription sub = knowledgeService.createSubscription(request, orgContext);

        assertNotNull(sub.getSubscriptionId());
        assertTrue(sub.getSubscriptionId().startsWith("SUB-"));
        assertEquals("TENANT-001", sub.getTenantId());
        assertEquals("USER-001", sub.getSubscriberId());
        assertEquals("DISEASE", sub.getTopicType());
        assertEquals("I10", sub.getTopicCode());
        assertEquals("ACTIVE", sub.getStatus());
        assertTrue(sub.isAutoSync());
        assertEquals("MANUAL", sub.getSyncFrequency());
    }

    @Test
    void createSubscription_shouldUseProvidedValues() {
        List<String> sourceTypes = new ArrayList<String>();
        sourceTypes.add("GUIDELINE");
        sourceTypes.add("TERMINOLOGY");

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("subscriber_id", "USER-002");
        request.put("topic_type", "DRUG");
        request.put("topic_code", "J01");
        request.put("source_types", sourceTypes);
        request.put("auto_sync", "false");
        request.put("sync_frequency", "WEEKLY");

        KnowledgeSubscription sub = knowledgeService.createSubscription(request, orgContext);

        assertFalse(sub.isAutoSync());
        assertEquals("WEEKLY", sub.getSyncFrequency());
        assertEquals(2, sub.getSourceTypes().size());
    }

    // ==================== 更新订阅 updateSubscription ====================

    @Test
    void updateSubscription_shouldUpdateFields() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("subscriber_id", "USER-001");
        createReq.put("topic_type", "DISEASE");
        createReq.put("topic_code", "I10");
        KnowledgeSubscription sub = knowledgeService.createSubscription(createReq, orgContext);
        String subId = sub.getSubscriptionId();

        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("topic_name", "Updated Topic");
        updateReq.put("sync_frequency", "DAILY");

        KnowledgeSubscription updated = knowledgeService.updateSubscription(subId, updateReq, orgContext);

        assertEquals("Updated Topic", updated.getTopicName());
        assertEquals("DAILY", updated.getSyncFrequency());
    }

    @Test
    void updateSubscription_shouldThrowWhenNotFound() {
        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("topic_name", "Updated");

        assertThrows(IllegalArgumentException.class,
                () -> knowledgeService.updateSubscription("SUB-NONEXISTENT", updateReq, orgContext));
    }

    @Test
    void updateSubscription_shouldThrowWhenTenantMismatch() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("subscriber_id", "USER-001");
        createReq.put("topic_type", "DISEASE");
        createReq.put("topic_code", "I10");
        KnowledgeSubscription sub = knowledgeService.createSubscription(createReq, orgContext);

        OrganizationContext otherContext = new OrganizationContext();
        otherContext.setTenantId("OTHER-TENANT");

        Map<String, Object> updateReq = new HashMap<String, Object>();
        updateReq.put("topic_name", "Hacked");

        assertThrows(IllegalArgumentException.class,
                () -> knowledgeService.updateSubscription(sub.getSubscriptionId(), updateReq, otherContext));
    }

    // ==================== 暂停/取消订阅 ====================

    @Test
    void pauseSubscription_shouldSetStatusToPaused() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("subscriber_id", "USER-001");
        createReq.put("topic_type", "DISEASE");
        createReq.put("topic_code", "I10");
        KnowledgeSubscription sub = knowledgeService.createSubscription(createReq, orgContext);

        KnowledgeSubscription paused = knowledgeService.pauseSubscription(sub.getSubscriptionId(), orgContext);
        assertEquals("PAUSED", paused.getStatus());
    }

    @Test
    void cancelSubscription_shouldSetStatusToCancelled() {
        Map<String, Object> createReq = new HashMap<String, Object>();
        createReq.put("subscriber_id", "USER-001");
        createReq.put("topic_type", "DISEASE");
        createReq.put("topic_code", "I10");
        KnowledgeSubscription sub = knowledgeService.createSubscription(createReq, orgContext);

        KnowledgeSubscription cancelled = knowledgeService.cancelSubscription(sub.getSubscriptionId(), orgContext);
        assertEquals("CANCELLED", cancelled.getStatus());
    }

    @Test
    void pauseSubscription_shouldThrowWhenNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> knowledgeService.pauseSubscription("SUB-FAKE", orgContext));
    }

    @Test
    void cancelSubscription_shouldThrowWhenNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> knowledgeService.cancelSubscription("SUB-FAKE", orgContext));
    }

    // ==================== 列出订阅 listSubscriptions ====================

    @Test
    void listSubscriptions_shouldReturnForTenant() {
        Map<String, Object> req1 = new HashMap<String, Object>();
        req1.put("subscriber_id", "USER-001");
        req1.put("topic_type", "DISEASE");
        req1.put("topic_code", "I10");
        knowledgeService.createSubscription(req1, orgContext);

        Map<String, Object> req2 = new HashMap<String, Object>();
        req2.put("subscriber_id", "USER-002");
        req2.put("topic_type", "DRUG");
        req2.put("topic_code", "J01");
        knowledgeService.createSubscription(req2, orgContext);

        List<KnowledgeSubscription> subs = knowledgeService.listSubscriptions(null, orgContext);
        assertEquals(2, subs.size());
    }

    @Test
    void listSubscriptions_shouldFilterByTopicType() {
        Map<String, Object> req1 = new HashMap<String, Object>();
        req1.put("subscriber_id", "USER-001");
        req1.put("topic_type", "DISEASE");
        req1.put("topic_code", "I10");
        knowledgeService.createSubscription(req1, orgContext);

        Map<String, Object> req2 = new HashMap<String, Object>();
        req2.put("subscriber_id", "USER-002");
        req2.put("topic_type", "DRUG");
        req2.put("topic_code", "J01");
        knowledgeService.createSubscription(req2, orgContext);

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("topic_type", "DISEASE");

        List<KnowledgeSubscription> subs = knowledgeService.listSubscriptions(filters, orgContext);
        assertEquals(1, subs.size());
        assertEquals("DISEASE", subs.get(0).getTopicType());
    }

    @Test
    void listSubscriptions_shouldFilterByStatus() {
        Map<String, Object> req1 = new HashMap<String, Object>();
        req1.put("subscriber_id", "USER-001");
        req1.put("topic_type", "DISEASE");
        req1.put("topic_code", "I10");
        KnowledgeSubscription sub1 = knowledgeService.createSubscription(req1, orgContext);
        knowledgeService.pauseSubscription(sub1.getSubscriptionId(), orgContext);

        Map<String, Object> req2 = new HashMap<String, Object>();
        req2.put("subscriber_id", "USER-002");
        req2.put("topic_type", "DRUG");
        req2.put("topic_code", "J01");
        knowledgeService.createSubscription(req2, orgContext);

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("status", "PAUSED");

        List<KnowledgeSubscription> subs = knowledgeService.listSubscriptions(filters, orgContext);
        assertEquals(1, subs.size());
        assertEquals("PAUSED", subs.get(0).getStatus());
    }

    @Test
    void listSubscriptions_shouldFilterBySubscriberId() {
        Map<String, Object> req1 = new HashMap<String, Object>();
        req1.put("subscriber_id", "USER-A");
        req1.put("topic_type", "DISEASE");
        req1.put("topic_code", "I10");
        knowledgeService.createSubscription(req1, orgContext);

        Map<String, Object> req2 = new HashMap<String, Object>();
        req2.put("subscriber_id", "USER-B");
        req2.put("topic_type", "DRUG");
        req2.put("topic_code", "J01");
        knowledgeService.createSubscription(req2, orgContext);

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("subscriber_id", "USER-A");

        List<KnowledgeSubscription> subs = knowledgeService.listSubscriptions(filters, orgContext);
        assertEquals(1, subs.size());
        assertEquals("USER-A", subs.get(0).getSubscriberId());
    }

    @Test
    void listSubscriptions_shouldNotReturnOtherTenantSubscriptions() {
        Map<String, Object> req = new HashMap<String, Object>();
        req.put("subscriber_id", "USER-001");
        req.put("topic_type", "DISEASE");
        req.put("topic_code", "I10");
        knowledgeService.createSubscription(req, orgContext);

        OrganizationContext otherContext = new OrganizationContext();
        otherContext.setTenantId("OTHER-TENANT");

        List<KnowledgeSubscription> subs = knowledgeService.listSubscriptions(null, otherContext);
        assertTrue(subs.isEmpty());
    }

    // ==================== 获取订阅 getSubscription ====================

    @Test
    void getSubscription_shouldReturnSubscription() {
        Map<String, Object> req = new HashMap<String, Object>();
        req.put("subscriber_id", "USER-001");
        req.put("topic_type", "DISEASE");
        req.put("topic_code", "I10");
        KnowledgeSubscription created = knowledgeService.createSubscription(req, orgContext);

        KnowledgeSubscription found = knowledgeService.getSubscription(created.getSubscriptionId(), orgContext);
        assertNotNull(found);
        assertEquals(created.getSubscriptionId(), found.getSubscriptionId());
    }

    @Test
    void getSubscription_shouldReturnNullWhenNotFound() {
        assertNull(knowledgeService.getSubscription("SUB-FAKE", orgContext));
    }

    @Test
    void getSubscription_shouldReturnNullForOtherTenant() {
        Map<String, Object> req = new HashMap<String, Object>();
        req.put("subscriber_id", "USER-001");
        req.put("topic_type", "DISEASE");
        req.put("topic_code", "I10");
        KnowledgeSubscription created = knowledgeService.createSubscription(req, orgContext);

        OrganizationContext otherContext = new OrganizationContext();
        otherContext.setTenantId("OTHER-TENANT");

        assertNull(knowledgeService.getSubscription(created.getSubscriptionId(), otherContext));
    }

    // ==================== toView 方法 ====================

    @Test
    void sourceRegistry_toView_shouldContainAllFields() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("source_code", "VIEW-001");
        request.put("source_name", "View Source");
        request.put("source_type", "TERMINOLOGY");
        KnowledgeSourceRegistry source = knowledgeService.registerSource(request, orgContext);

        Map<String, Object> view = source.toView();
        assertEquals("VIEW-001", view.get("source_code"));
        assertEquals("View Source", view.get("source_name"));
        assertEquals("TERMINOLOGY", view.get("source_type"));
        assertNotNull(view.get("license"));
        assertTrue(view.get("license") instanceof Map);
    }

    @Test
    void subscription_toView_shouldContainAllFields() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("subscriber_id", "USER-001");
        request.put("topic_type", "DISEASE");
        request.put("topic_code", "I10");
        KnowledgeSubscription sub = knowledgeService.createSubscription(request, orgContext);

        Map<String, Object> view = sub.toView();
        assertEquals("USER-001", view.get("subscriber_id"));
        assertEquals("DISEASE", view.get("topic_type"));
        assertEquals("I10", view.get("topic_code"));
    }
}
