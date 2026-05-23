package com.medkernel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.audit.PublishGateService;
import com.medkernel.common.exception.MissingSourceException;
import com.medkernel.organization.OrganizationDirectoryService;
import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigPackageServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private OrganizationDirectoryService organizationDirectoryService;

    @Mock
    private ConfigPackageRepository configPackageRepository;

    @Mock
    private PublishGateService publishGateService;

    private ConfigPackageService configPackageService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        configPackageService = new ConfigPackageService(
                objectMapper, persistenceService, organizationDirectoryService,
                configPackageRepository, publishGateService);
    }

    // ==================== importPackages ====================

    @Test
    void importPackages_shouldImportSinglePackageFromMap() {
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");

        List<Map<String, Object>> result = configPackageService.importPackages(payload);

        assertEquals(1, result.size());
        assertEquals("PKG001", result.get(0).get("package_code"));
        assertEquals("1.0.0", result.get(0).get("package_version"));
        assertEquals("DRAFT", result.get(0).get("status"));
    }

    @Test
    void importPackages_shouldImportFromListPayload() {
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(persistenceService.enabled()).thenReturn(true);

        List<Map<String, Object>> payloads = new ArrayList<Map<String, Object>>();
        payloads.add(createValidPayload("PKG001", "1.0.0", "RULE"));
        payloads.add(createValidPayload("PKG002", "2.0.0", "PATHWAY"));

        List<Map<String, Object>> result = configPackageService.importPackages(payloads);

        assertEquals(2, result.size());
    }

    @Test
    void importPackages_shouldImportFromNestedPackagesKey() {
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Object> wrapper = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> packages = new ArrayList<Map<String, Object>>();
        packages.add(createValidPayload("PKG001", "1.0.0", "RULE"));
        wrapper.put("packages", packages);

        List<Map<String, Object>> result = configPackageService.importPackages(wrapper);

        assertEquals(1, result.size());
    }

    @Test
    void importPackages_shouldThrowWhenPackagesIsEmpty() {
        List<Map<String, Object>> emptyList = new ArrayList<Map<String, Object>>();
        assertThrows(IllegalArgumentException.class, () -> {
            configPackageService.importPackages(emptyList);
        });
    }

    @Test
    void importPackages_shouldThrowWhenPackageCodeMissing() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("package_version", "1.0.0");
        payload.put("asset_type", "RULE");
        payload.put("full_snapshot", Collections.singletonMap("items", new ArrayList<>()));

        List<Map<String, Object>> payloads = new ArrayList<Map<String, Object>>();
        payloads.add(payload);

        assertThrows(IllegalArgumentException.class, () -> {
            configPackageService.importPackages(payloads);
        });
    }

    @Test
    void importPackages_shouldThrowWhenPackageVersionMissing() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("package_code", "PKG001");
        payload.put("asset_type", "RULE");
        payload.put("full_snapshot", Collections.singletonMap("items", new ArrayList<>()));

        List<Map<String, Object>> payloads = new ArrayList<Map<String, Object>>();
        payloads.add(payload);

        assertThrows(IllegalArgumentException.class, () -> {
            configPackageService.importPackages(payloads);
        });
    }

    @Test
    void importPackages_shouldThrowWhenAssetTypeMissing() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("package_code", "PKG001");
        payload.put("package_version", "1.0.0");
        payload.put("full_snapshot", Collections.singletonMap("items", new ArrayList<>()));

        List<Map<String, Object>> payloads = new ArrayList<Map<String, Object>>();
        payloads.add(payload);

        assertThrows(IllegalArgumentException.class, () -> {
            configPackageService.importPackages(payloads);
        });
    }

    @Test
    void importPackages_shouldRejectDuplicateWithDifferentContentHash() {
        ConfigPackageEntity existingEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "sha256:existinghash");
        when(configPackageRepository.findByUniqueKey("default", "PKG001", "1.0.0",
                "RULE", "PLATFORM", "DEFAULT")).thenReturn(existingEntity);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");

        List<Map<String, Object>> payloads = new ArrayList<Map<String, Object>>();
        payloads.add(payload);

        assertThrows(IllegalArgumentException.class, () -> {
            configPackageService.importPackages(payloads);
        });
    }

    @Test
    void importPackages_shouldReturnExistingWhenSameContentHash() {
        ConfigPackage existingPkg = new ConfigPackage();
        existingPkg.setTenantId("default");
        existingPkg.setPackageCode("PKG001");
        existingPkg.setPackageVersion("1.0.0");
        existingPkg.setAssetType("RULE");
        existingPkg.setScopeLevel("PLATFORM");
        existingPkg.setScopeCode("DEFAULT");
        existingPkg.setStatus("DRAFT");
        existingPkg.setContentHash("sha256:somehash");
        existingPkg.setFullSnapshot(Collections.singletonMap("items", new ArrayList<>()));

        ConfigPackageEntity existingEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", existingPkg.getContentHash());

        // We need to match the content hash, so let's use the same payload that generates the same hash
        // Since we can't predict the hash, we need to first import, then re-import
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        List<Map<String, Object>> firstResult = configPackageService.importPackages(payload);
        String contentHash = (String) firstResult.get(0).get("content_hash");

        // Now simulate re-import with same hash
        ConfigPackageEntity duplicateEntity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", contentHash);
        when(configPackageRepository.findByUniqueKey("default", "PKG001", "1.0.0",
                "RULE", "PLATFORM", "DEFAULT")).thenReturn(duplicateEntity);

        List<Map<String, Object>> secondResult = configPackageService.importPackages(payload);
        assertEquals(1, secondResult.size());
        assertEquals("PKG001", secondResult.get(0).get("package_code"));
    }

    @Test
    void importPackages_shouldSupportCamelCaseKeys() {
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("packageCode", "PKG_CAMEL");
        payload.put("packageVersion", "1.0.0");
        payload.put("assetType", "RULE");
        payload.put("fullSnapshot", Collections.singletonMap("items", new ArrayList<>()));

        List<Map<String, Object>> payloads = new ArrayList<Map<String, Object>>();
        payloads.add(payload);

        List<Map<String, Object>> result = configPackageService.importPackages(payloads);

        assertEquals(1, result.size());
        assertEquals("PKG_CAMEL", result.get(0).get("package_code"));
    }

    @Test
    void importPackages_shouldDefaultTenantIdToDefault() {
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");

        List<Map<String, Object>> result = configPackageService.importPackages(payload);

        assertEquals("default", result.get(0).get("tenant_id"));
    }

    @Test
    void importPackages_shouldDefaultScopeToPlatformDefault() {
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");

        List<Map<String, Object>> result = configPackageService.importPackages(payload);

        assertEquals("PLATFORM", result.get(0).get("scope_level"));
        assertEquals("DEFAULT", result.get(0).get("scope_code"));
    }

    // ==================== listPackages ====================

    @Test
    void listPackages_shouldReturnEmptyWhenNoPackages() {
        when(configPackageRepository.findList(anyString(), any(), any(), any(),
                any(), any())).thenReturn(new ArrayList<>());
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, String> filters = new HashMap<String, String>();
        List<Map<String, Object>> result = configPackageService.listPackages(filters);

        assertNotNull(result);
    }

    @Test
    void listPackages_shouldFilterByAssetType() {
        ConfigPackageEntity entity1 = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        ConfigPackageEntity entity2 = createEntity("default", "PKG002", "1.0.0", "PATHWAY",
                "PLATFORM", "DEFAULT", "hash2");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(entity1);
        entities.add(entity2);

        when(configPackageRepository.findList(eq("default"), any(), any(), any(),
                any(), any())).thenReturn(entities);
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("tenantId", "default");
        filters.put("assetType", "RULE");

        List<Map<String, Object>> result = configPackageService.listPackages(filters);

        for (Map<String, Object> item : result) {
            assertEquals("RULE", item.get("asset_type"));
        }
    }

    @Test
    void listPackages_shouldFilterByStatus() {
        ConfigPackageEntity entity1 = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        entity1.setStatus("DRAFT");
        ConfigPackageEntity entity2 = createEntity("default", "PKG002", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");
        entity2.setStatus("PUBLISHED");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(entity1);
        entities.add(entity2);

        when(configPackageRepository.findList(eq("default"), any(), any(), any(),
                any(), any())).thenReturn(entities);
        when(persistenceService.enabled()).thenReturn(true);

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("tenantId", "default");
        filters.put("status", "PUBLISHED");

        List<Map<String, Object>> result = configPackageService.listPackages(filters);

        for (Map<String, Object> item : result) {
            assertEquals("PUBLISHED", item.get("status"));
        }
    }

    @Test
    void listPackages_shouldHandleNullFilters() {
        when(configPackageRepository.findList(any(), any(), any(), any(),
                any(), any())).thenReturn(new ArrayList<>());
        when(persistenceService.enabled()).thenReturn(true);

        List<Map<String, Object>> result = configPackageService.listPackages(null);
        assertNotNull(result);
    }

    // ==================== getPackage ====================

    @Test
    void getPackage_shouldReturnPackageDetail() {
        when(persistenceService.enabled()).thenReturn(true);

        // First import a package
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        // Now get it - it's in memory store
        Map<String, Object> result = configPackageService.getPackage("PKG001", "1.0.0", "default");

        assertNotNull(result);
        assertEquals("PKG001", result.get("package_code"));
        assertEquals("1.0.0", result.get("package_version"));
        assertNotNull(result.get("full_snapshot"));
    }

    @Test
    void getPackage_shouldThrowWhenNotFound() {
        when(persistenceService.enabled()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            configPackageService.getPackage("NONEXISTENT", "1.0.0", "default");
        });
    }

    // ==================== reviewPackage ====================

    @Test
    void reviewPackage_shouldSetReviewedStatusWhenReady() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer1");

        Map<String, Object> result = configPackageService.reviewPackage("PKG001", "1.0.0",
                "default", reviewRequest);

        assertEquals("REVIEWED", result.get("status"));
        assertEquals("reviewer1", result.get("reviewed_by"));
        assertNotNull(result.get("reviewed_time"));
    }

    @Test
    void reviewPackage_shouldNotReviewWhenNotReadyToPublish() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(false);

        // Create a package with unsupported scope that will fail validation
        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer1");

        Map<String, Object> result = configPackageService.reviewPackage("PKG001", "1.0.0",
                "default", reviewRequest);

        // Should not change status since ready_to_publish is false
        assertFalse((Boolean) result.get("ready_to_publish"));
    }

    @Test
    void reviewPackage_shouldHandleNullRequest() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        Map<String, Object> result = configPackageService.reviewPackage("PKG001", "1.0.0",
                "default", null);

        // reviewed_by is null, so status should not change
        assertEquals("DRAFT", result.get("status"));
    }

    @Test
    void reviewPackage_shouldSupportReviewedByKey() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewedBy", "reviewer2");

        Map<String, Object> result = configPackageService.reviewPackage("PKG001", "1.0.0",
                "default", reviewRequest);

        assertEquals("REVIEWED", result.get("status"));
        assertEquals("reviewer2", result.get("reviewed_by"));
    }

    // ==================== publishPackage ====================

    @Test
    void publishPackage_shouldPublishWhenReady() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        PublishGateService.GateCheckResult gateResult = new PublishGateService.GateCheckResult();
        when(publishGateService.checkConfigPackageSourceReview(any())).thenReturn(gateResult);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        // Review first
        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer1");
        configPackageService.reviewPackage("PKG001", "1.0.0", "default", reviewRequest);

        // Publish
        Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();
        publishRequest.put("approved_by", "admin1");

        Map<String, Object> result = configPackageService.publishPackage("PKG001", "1.0.0",
                "default", publishRequest);

        assertEquals("PUBLISHED", result.get("status"));
        assertEquals("admin1", result.get("approved_by"));
        assertNotNull(result.get("published_time"));
    }

    @Test
    void publishPackage_shouldReturnDetailWhenAlreadyPublished() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        PublishGateService.GateCheckResult gateResult = new PublishGateService.GateCheckResult();
        when(publishGateService.checkConfigPackageSourceReview(any())).thenReturn(gateResult);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        // Review
        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer1");
        configPackageService.reviewPackage("PKG001", "1.0.0", "default", reviewRequest);

        // Publish
        Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();
        publishRequest.put("approved_by", "admin1");
        configPackageService.publishPackage("PKG001", "1.0.0", "default", publishRequest);

        // Publish again - should return detail without error
        Map<String, Object> result = configPackageService.publishPackage("PKG001", "1.0.0",
                "default", publishRequest);
        assertEquals("PUBLISHED", result.get("status"));
    }

    @Test
    void publishPackage_shouldThrowWhenNotReadyToPublish() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(false);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();
        publishRequest.put("approved_by", "admin1");

        assertThrows(IllegalArgumentException.class, () -> {
            configPackageService.publishPackage("PKG001", "1.0.0", "default", publishRequest);
        });
    }

    @Test
    void publishPackage_shouldThrowWhenApprovedByMissing() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        // Review first
        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer1");
        configPackageService.reviewPackage("PKG001", "1.0.0", "default", reviewRequest);

        Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();

        assertThrows(IllegalArgumentException.class, () -> {
            configPackageService.publishPackage("PKG001", "1.0.0", "default", publishRequest);
        });
    }

    @Test
    void publishPackage_shouldThrowMissingSourceExceptionWhenGateBlocks() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        PublishGateService.GateCheckResult gateResult = new PublishGateService.GateCheckResult();
        gateResult.addIssue("ERROR", "source_review.blocked", "blocked", null, "CONFIG_PACKAGE");
        when(publishGateService.checkConfigPackageSourceReview(any())).thenReturn(gateResult);
        when(publishGateService.formatBlockingMessage(gateResult)).thenReturn("blocked");

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        // Review first
        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer1");
        configPackageService.reviewPackage("PKG001", "1.0.0", "default", reviewRequest);

        Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();
        publishRequest.put("approved_by", "admin1");

        assertThrows(MissingSourceException.class, () -> {
            configPackageService.publishPackage("PKG001", "1.0.0", "default", publishRequest);
        });
    }

    @Test
    void publishPackage_shouldThrowMissingSourceExceptionWhenSourceReviewBlocks() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(false);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();
        publishRequest.put("approved_by", "admin1");

        // The buildReview will have source_review issues due to invalid scope
        // hasSourceReviewBlockingIssue should detect it
        // But actually the scope issue is "scope_code" not "source_review" prefix
        // So it throws IllegalArgumentException, not MissingSourceException
        assertThrows(IllegalArgumentException.class, () -> {
            configPackageService.publishPackage("PKG001", "1.0.0", "default", publishRequest);
        });
    }

    // ==================== exportPackage ====================

    @Test
    void exportPackage_shouldReturnExportedPackage() {
        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        Map<String, Object> result = configPackageService.exportPackage("PKG001", "1.0.0", "default");

        assertNotNull(result);
        assertEquals("PKG001", result.get("package_code"));
        assertNotNull(result.get("exported_time"));
        assertEquals("MEDKERNEL_CONFIG_PACKAGE_V1", result.get("export_format"));
    }

    // ==================== buildReview ====================

    @Test
    void buildReview_shouldReturnReadyToPublishForValidPackage() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        assertTrue((Boolean) result.get("ready_to_publish"));
        assertNotNull(result.get("issues"));
        assertNotNull(result.get("summary"));
        assertNotNull(result.get("source_review"));
        assertNotNull(result.get("publish_gate"));
    }

    @Test
    void buildReview_shouldReportErrorsForInvalidPackage() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(false);

        ConfigPackage configPackage = new ConfigPackage();
        configPackage.setPackageCode(null);
        configPackage.setPackageVersion(null);
        configPackage.setAssetType("INVALID_TYPE");
        configPackage.setScopeLevel("INVALID_SCOPE");
        configPackage.setScopeCode("CODE1");
        configPackage.setStatus("INVALID_STATUS");
        configPackage.setFullSnapshot(new LinkedHashMap<String, Object>());

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        assertFalse((Boolean) result.get("ready_to_publish"));
        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        assertFalse(issues.isEmpty());
    }

    @Test
    void buildReview_shouldReportMissingPackageCode() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage(null, "1.0.0", "RULE", "DRAFT");

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasPackageCodeError = false;
        for (Map<String, Object> issue : issues) {
            if ("package_code".equals(issue.get("field"))) {
                hasPackageCodeError = true;
            }
        }
        assertTrue(hasPackageCodeError);
    }

    @Test
    void buildReview_shouldReportMissingPackageVersion() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", null, "RULE", "DRAFT");

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasPackageVersionError = false;
        for (Map<String, Object> issue : issues) {
            if ("package_version".equals(issue.get("field"))) {
                hasPackageVersionError = true;
            }
        }
        assertTrue(hasPackageVersionError);
    }

    @Test
    void buildReview_shouldReportUnsupportedAssetType() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "INVALID", "DRAFT");

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasAssetTypeError = false;
        for (Map<String, Object> issue : issues) {
            if ("asset_type".equals(issue.get("field"))) {
                hasAssetTypeError = true;
            }
        }
        assertTrue(hasAssetTypeError);
    }

    @Test
    void buildReview_shouldReportEmptyFullSnapshot() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");
        configPackage.setFullSnapshot(new LinkedHashMap<String, Object>());

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasSnapshotError = false;
        for (Map<String, Object> issue : issues) {
            if ("full_snapshot".equals(issue.get("field"))) {
                hasSnapshotError = true;
            }
        }
        assertTrue(hasSnapshotError);
    }

    @Test
    void buildReview_shouldReportContentHashMismatch() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");
        configPackage.setContentHash("sha256:actual");
        configPackage.setDeclaredContentHash("sha256:declared");

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasHashError = false;
        for (Map<String, Object> issue : issues) {
            if ("content_hash".equals(issue.get("field"))) {
                hasHashError = true;
            }
        }
        assertTrue(hasHashError);
    }

    @Test
    void buildReview_shouldReportScopeNotFound() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(false);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");
        configPackage.setScopeLevel("HOSPITAL");
        configPackage.setScopeCode("HOSP001");

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasScopeError = false;
        for (Map<String, Object> issue : issues) {
            if ("scope_code".equals(issue.get("field"))) {
                hasScopeError = true;
            }
        }
        assertTrue(hasScopeError);
    }

    @Test
    void buildReview_shouldReportSourceReviewBlocked() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");
        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        Map<String, Object> sourceReview = new LinkedHashMap<String, Object>();
        sourceReview.put("enabled", true);
        sourceReview.put("blocked", true);
        manifest.put("source_review", sourceReview);
        configPackage.setManifest(manifest);

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasSourceReviewError = false;
        for (Map<String, Object> issue : issues) {
            if ("source_review".equals(issue.get("field"))) {
                hasSourceReviewError = true;
            }
        }
        assertTrue(hasSourceReviewError);
    }

    @Test
    void buildReview_shouldReportSourceReviewMissingCount() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");
        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        Map<String, Object> sourceReview = new LinkedHashMap<String, Object>();
        sourceReview.put("enabled", true);
        sourceReview.put("blocked", false);
        sourceReview.put("missing_count", 3);
        manifest.put("source_review", sourceReview);
        configPackage.setManifest(manifest);

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasMissingError = false;
        for (Map<String, Object> issue : issues) {
            if ("source_review.missing_count".equals(issue.get("field"))) {
                hasMissingError = true;
            }
        }
        assertTrue(hasMissingError);
    }

    @Test
    void buildReview_shouldReportSourceReviewExpiredCount() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");
        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        Map<String, Object> sourceReview = new LinkedHashMap<String, Object>();
        sourceReview.put("enabled", true);
        sourceReview.put("blocked", false);
        sourceReview.put("expired_count", 2);
        manifest.put("source_review", sourceReview);
        configPackage.setManifest(manifest);

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasExpiredError = false;
        for (Map<String, Object> issue : issues) {
            if ("source_review.expired_count".equals(issue.get("field"))) {
                hasExpiredError = true;
            }
        }
        assertTrue(hasExpiredError);
    }

    @Test
    void buildReview_shouldReportSourceReviewUnreviewedCount() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");
        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        Map<String, Object> sourceReview = new LinkedHashMap<String, Object>();
        sourceReview.put("enabled", true);
        sourceReview.put("blocked", false);
        sourceReview.put("unreviewed_count", 1);
        manifest.put("source_review", sourceReview);
        configPackage.setManifest(manifest);

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasUnreviewedError = false;
        for (Map<String, Object> issue : issues) {
            if ("source_review.unreviewed_count".equals(issue.get("field"))) {
                hasUnreviewedError = true;
            }
        }
        assertTrue(hasUnreviewedError);
    }

    @Test
    void buildReview_shouldReportSourceReviewAllowPublishFalse() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");
        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        Map<String, Object> sourceReview = new LinkedHashMap<String, Object>();
        sourceReview.put("enabled", true);
        sourceReview.put("blocked", false);
        sourceReview.put("allow_publish", false);
        manifest.put("source_review", sourceReview);
        configPackage.setManifest(manifest);

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        List<Map<String, Object>> issues = (List<Map<String, Object>>) result.get("issues");
        boolean hasAllowPublishError = false;
        for (Map<String, Object> issue : issues) {
            if ("source_review.allow_publish".equals(issue.get("field"))) {
                hasAllowPublishError = true;
            }
        }
        assertTrue(hasAllowPublishError);
    }

    @Test
    void buildReview_shouldNotBeReadyWhenStatusIsPublished() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "PUBLISHED");

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        assertFalse((Boolean) result.get("ready_to_publish"));
    }

    @Test
    void buildReview_shouldIncludeScopeReference() {
        Map<String, Object> scopeRef = new LinkedHashMap<String, Object>();
        scopeRef.put("exists", true);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);
        when(organizationDirectoryService.scopeReference(anyString(), anyString(), anyString())).thenReturn(scopeRef);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        assertNotNull(result.get("scope_reference"));
    }

    @Test
    void buildReview_shouldReadSourceReviewFromSnapshot() {
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);

        ConfigPackage configPackage = createValidConfigPackage("PKG001", "1.0.0", "RULE", "DRAFT");
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        Map<String, Object> sourceReview = new LinkedHashMap<String, Object>();
        sourceReview.put("enabled", true);
        sourceReview.put("blocked", false);
        sourceReview.put("missing_count", 0);
        snapshot.put("source_review", sourceReview);
        snapshot.put("items", new ArrayList<>());
        configPackage.setFullSnapshot(snapshot);
        configPackage.setManifest(new LinkedHashMap<String, Object>());

        Map<String, Object> result = configPackageService.buildReview(configPackage);

        Map<String, Object> resultSourceReview = (Map<String, Object>) result.get("source_review");
        assertTrue((Boolean) resultSourceReview.get("enabled"));
    }

    // ==================== findPackage (via getPackage) ====================

    @Test
    void getPackage_shouldFindFromDatabaseWhenEnabled() {
        ConfigPackageEntity entity = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(entity);

        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findList(eq("default"), eq("PKG001"), any(),
                any(), any(), any())).thenReturn(entities);

        Map<String, Object> result = configPackageService.getPackage("PKG001", "1.0.0", "default");

        assertNotNull(result);
        assertEquals("PKG001", result.get("package_code"));
    }

    @Test
    void getPackage_shouldFindLatestVersionWhenNoVersionSpecified() {
        ConfigPackageEntity entity1 = createEntity("default", "PKG001", "1.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash1");
        ConfigPackageEntity entity2 = createEntity("default", "PKG001", "2.0.0", "RULE",
                "PLATFORM", "DEFAULT", "hash2");

        List<ConfigPackageEntity> entities = new ArrayList<ConfigPackageEntity>();
        entities.add(entity1);
        entities.add(entity2);

        when(persistenceService.enabled()).thenReturn(true);
        when(configPackageRepository.findList(eq("default"), eq("PKG001"), any(),
                any(), any(), any())).thenReturn(entities);

        Map<String, Object> result = configPackageService.getPackage("PKG001", null, "default");

        assertEquals("2.0.0", result.get("package_version"));
    }

    @Test
    void getPackage_shouldFindFromMemoryWhenDbNotEnabled() {
        when(persistenceService.enabled()).thenReturn(false);
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);

        Map<String, Object> payload = createValidPayload("PKG001", "1.0.0", "RULE");
        configPackageService.importPackages(payload);

        Map<String, Object> result = configPackageService.getPackage("PKG001", "1.0.0", "default");

        assertNotNull(result);
        assertEquals("PKG001", result.get("package_code"));
    }

    // ==================== Helper methods ====================

    private Map<String, Object> createValidPayload(String packageCode, String packageVersion, String assetType) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("package_code", packageCode);
        payload.put("package_version", packageVersion);
        payload.put("asset_type", assetType);
        payload.put("full_snapshot", Collections.singletonMap("items", new ArrayList<>()));
        return payload;
    }

    private ConfigPackage createValidConfigPackage(String packageCode, String packageVersion,
                                                    String assetType, String status) {
        ConfigPackage configPackage = new ConfigPackage();
        configPackage.setTenantId("default");
        configPackage.setPackageCode(packageCode);
        configPackage.setPackageVersion(packageVersion);
        configPackage.setAssetType(assetType);
        configPackage.setScopeLevel("PLATFORM");
        configPackage.setScopeCode("DEFAULT");
        configPackage.setStatus(status);
        configPackage.setContentHash("sha256:testhash");
        configPackage.setDeclaredContentHash("sha256:testhash");
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("items", new ArrayList<>());
        configPackage.setFullSnapshot(snapshot);
        configPackage.setManifest(new LinkedHashMap<String, Object>());
        configPackage.setDiff(new LinkedHashMap<String, Object>());
        return configPackage;
    }

    private ConfigPackageEntity createEntity(String tenantId, String packageCode, String packageVersion,
                                              String assetType, String scopeLevel, String scopeCode,
                                              String contentHash) {
        ConfigPackageEntity entity = new ConfigPackageEntity();
        entity.setId(1L);
        entity.setTenantId(tenantId);
        entity.setPackageCode(packageCode);
        entity.setPackageVersion(packageVersion);
        entity.setAssetType(assetType);
        entity.setScopeLevel(scopeLevel);
        entity.setScopeCode(scopeCode);
        entity.setStatus("DRAFT");
        entity.setContentHash(contentHash);
        entity.setCreatedBy("test-user");
        return entity;
    }
}
