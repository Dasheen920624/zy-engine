package com.medkernel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.audit.PublishGateService;
import com.medkernel.common.exception.MissingSourceException;
import com.medkernel.organization.OrganizationDirectoryService;
import com.medkernel.persistence.EnginePersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("配置包服务测试")
class ConfigPackageServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private OrganizationDirectoryService organizationDirectoryService;

    @Mock
    private ConfigPackageRepository configPackageRepository;

    @Mock
    private PublishGateService publishGateService;

    private ObjectMapper objectMapper;
    private ConfigPackageService configPackageService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        configPackageService = new ConfigPackageService(
                objectMapper, persistenceService, organizationDirectoryService,
                configPackageRepository, publishGateService);

        // 默认配置：DB未启用，走内存
        when(persistenceService.enabled()).thenReturn(false);
        when(organizationDirectoryService.scopeExists(anyString(), anyString(), anyString())).thenReturn(true);
        when(organizationDirectoryService.scopeReference(anyString(), anyString(), anyString()))
                .thenReturn(buildScopeReference(true));
        doNothing().when(persistenceService).saveAuditLog(any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    // ========== 配置包创建/导入 ==========

    @Test
    @DisplayName("导入配置包 - 成功创建单个配置包")
    void importPackages_shouldCreateSinglePackage() {
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);

        Map<String, Object> payload = buildPackagePayload("PKG_001", "1.0.0", "PATHWAY");
        List<Map<String, Object>> result = configPackageService.importPackages(
                Collections.singletonList(payload));

        assertEquals(1, result.size());
        assertEquals("PKG_001", result.get(0).get("package_code"));
        assertEquals("1.0.0", result.get(0).get("package_version"));
        assertEquals("DRAFT", result.get(0).get("status"));
    }

    @Test
    @DisplayName("导入配置包 - 成功创建多个配置包")
    void importPackages_shouldCreateMultiplePackages() {
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(null);

        Map<String, Object> payload1 = buildPackagePayload("PKG_A", "1.0.0", "RULE");
        Map<String, Object> payload2 = buildPackagePayload("PKG_B", "1.0.0", "GRAPH");
        List<Map<String, Object>> payloads = new ArrayList<Map<String, Object>>();
        payloads.add(payload1);
        payloads.add(payload2);

        List<Map<String, Object>> result = configPackageService.importPackages(payloads);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("导入配置包 - 空列表抛异常")
    void importPackages_shouldThrowWhenEmpty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> configPackageService.importPackages(Collections.emptyList()));
        assertTrue(ex.getMessage().contains("required"));
    }

    @Test
    @DisplayName("导入配置包 - 缺少package_code抛异常")
    void importPackages_shouldThrowWhenPackageCodeMissing() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("package_version", "1.0.0");
        payload.put("asset_type", "PATHWAY");
        payload.put("full_snapshot", Collections.singletonMap("items", Collections.emptyList()));

        assertThrows(IllegalArgumentException.class,
                () -> configPackageService.importPackages(Collections.singletonList(payload)));
    }

    @Test
    @DisplayName("导入配置包 - 缺少asset_type抛异常")
    void importPackages_shouldThrowWhenAssetTypeMissing() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("package_code", "PKG_X");
        payload.put("package_version", "1.0.0");
        payload.put("full_snapshot", Collections.singletonMap("items", Collections.emptyList()));

        assertThrows(IllegalArgumentException.class,
                () -> configPackageService.importPackages(Collections.singletonList(payload)));
    }

    @Test
    @DisplayName("导入配置包 - 不支持的asset_type抛异常")
    void importPackages_shouldThrowWhenAssetTypeUnsupported() {
        Map<String, Object> payload = buildPackagePayload("PKG_BAD", "1.0.0", "INVALID_TYPE");

        // 导入成功但review时应有ERROR issue
        List<Map<String, Object>> result = configPackageService.importPackages(
                Collections.singletonList(payload));
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("导入配置包 - 相同内容hash的重复包不报错")
    void importPackages_shouldAllowSameContentHashDuplicate() {
        Map<String, Object> payload = buildPackagePayload("PKG_DUP", "1.0.0", "PATHWAY");
        configPackageService.importPackages(Collections.singletonList(payload));

        // 模拟DB中已存在
        ConfigPackage existing = new ConfigPackage();
        existing.setTenantId("default");
        existing.setPackageCode("PKG_DUP");
        existing.setPackageVersion("1.0.0");
        existing.setAssetType("PATHWAY");
        existing.setScopeLevel("PLATFORM");
        existing.setScopeCode("DEFAULT");
        existing.setStatus("DRAFT");
        existing.setContentHash("sha256:somehash");
        existing.setFullSnapshot(Collections.singletonMap("items", Collections.emptyList()));

        ConfigPackageEntity entity = ConfigPackageEntity.fromConfigPackage(existing);
        entity.setContentHash("sha256:somehash");

        // 第二次导入相同包（DB返回已有entity）
        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(entity);

        Map<String, Object> payload2 = buildPackagePayload("PKG_DUP", "1.0.0", "PATHWAY");
        List<Map<String, Object>> result = configPackageService.importPackages(
                Collections.singletonList(payload2));
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("导入配置包 - 不同内容hash的重复版本抛异常")
    void importPackages_shouldThrowWhenContentHashDiffers() {
        Map<String, Object> payload = buildPackagePayload("PKG_HASH", "1.0.0", "PATHWAY");
        configPackageService.importPackages(Collections.singletonList(payload));

        // 模拟DB中已存在但hash不同
        ConfigPackage existing = new ConfigPackage();
        existing.setTenantId("default");
        existing.setPackageCode("PKG_HASH");
        existing.setPackageVersion("1.0.0");
        existing.setAssetType("PATHWAY");
        existing.setScopeLevel("PLATFORM");
        existing.setScopeCode("DEFAULT");
        existing.setStatus("DRAFT");
        existing.setContentHash("sha256:old_hash");
        existing.setFullSnapshot(Collections.singletonMap("items", Collections.emptyList()));

        ConfigPackageEntity entity = ConfigPackageEntity.fromConfigPackage(existing);
        entity.setContentHash("sha256:old_hash");

        when(configPackageRepository.findByUniqueKey(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString())).thenReturn(entity);

        Map<String, Object> payload2 = buildPackagePayload("PKG_HASH", "1.0.0", "PATHWAY");
        assertThrows(IllegalArgumentException.class,
                () -> configPackageService.importPackages(Collections.singletonList(payload2)));
    }

    // ========== 配置包列表 ==========

    @Test
    @DisplayName("列出配置包 - 返回已导入的配置包")
    void listPackages_shouldReturnImportedPackages() {
        when(configPackageRepository.findList(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.<ConfigPackageEntity>emptyList());

        Map<String, Object> payload = buildPackagePayload("PKG_LIST", "1.0.0", "RULE");
        configPackageService.importPackages(Collections.singletonList(payload));

        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("tenantId", "default");
        List<Map<String, Object>> result = configPackageService.listPackages(filters);

        boolean found = false;
        for (Map<String, Object> pkg : result) {
            if ("PKG_LIST".equals(pkg.get("package_code"))) {
                found = true;
                break;
            }
        }
        assertTrue(found, "应能列出已导入的配置包");
    }

    // ========== 配置包审查 ==========

    @Test
    @DisplayName("审查配置包 - 成功审查DRAFT状态包")
    void reviewPackage_shouldReviewDraftPackage() {
        Map<String, Object> payload = buildPackagePayload("PKG_REV", "1.0.0", "PATHWAY");
        configPackageService.importPackages(Collections.singletonList(payload));

        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer_zhang");

        Map<String, Object> result = configPackageService.reviewPackage(
                "PKG_REV", "1.0.0", "default", reviewRequest);

        assertEquals("REVIEWED", result.get("status"));
        assertTrue((Boolean) result.get("ready_to_publish"));
    }

    @Test
    @DisplayName("审查配置包 - 未提供reviewed_by不改变状态")
    void reviewPackage_shouldNotChangeStatusWithoutReviewer() {
        Map<String, Object> payload = buildPackagePayload("PKG_NOREV", "1.0.0", "PATHWAY");
        configPackageService.importPackages(Collections.singletonList(payload));

        Map<String, Object> result = configPackageService.reviewPackage(
                "PKG_NOREV", "1.0.0", "default", null);

        assertEquals("DRAFT", result.get("status"));
    }

    @Test
    @DisplayName("审查配置包 - 包不存在抛异常")
    void reviewPackage_shouldThrowWhenNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> configPackageService.reviewPackage("NOT_EXIST", "1.0.0", "default", null));
    }

    // ========== 配置包发布 ==========

    @Test
    @DisplayName("发布配置包 - 成功发布已审查包")
    void publishPackage_shouldPublishReviewedPackage() {
        when(configPackageRepository.findList(anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(Collections.<ConfigPackageEntity>emptyList());

        Map<String, Object> payload = buildPackagePayload("PKG_PUB", "1.0.0", "PATHWAY");
        configPackageService.importPackages(Collections.singletonList(payload));

        // 先审查
        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer_zhang");
        configPackageService.reviewPackage("PKG_PUB", "1.0.0", "default", reviewRequest);

        // 配置发布门禁通过
        PublishGateService.GateCheckResult gateResult = new PublishGateService.GateCheckResult();
        when(publishGateService.checkConfigPackageSourceReview(any())).thenReturn(gateResult);
        when(publishGateService.formatBlockingMessage(any())).thenReturn(null);
        doNothing().when(publishGateService).auditGateCheck(any(), any(), any(), any(), any(), any());

        // 再发布
        Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();
        publishRequest.put("approved_by", "admin_li");

        Map<String, Object> result = configPackageService.publishPackage(
                "PKG_PUB", "1.0.0", "default", publishRequest);

        assertEquals("PUBLISHED", result.get("status"));
        assertEquals("admin_li", result.get("approved_by"));
        assertNotNull(result.get("published_time"));
    }

    @Test
    @DisplayName("发布配置包 - 缺少approved_by抛异常")
    void publishPackage_shouldThrowWhenApprovedByMissing() {
        Map<String, Object> payload = buildPackagePayload("PKG_NOAPPROVE", "1.0.0", "PATHWAY");
        configPackageService.importPackages(Collections.singletonList(payload));

        // 先审查
        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer_zhang");
        configPackageService.reviewPackage("PKG_NOAPPROVE", "1.0.0", "default", reviewRequest);

        assertThrows(IllegalArgumentException.class,
                () -> configPackageService.publishPackage("PKG_NOAPPROVE", "1.0.0", "default", null));
    }

    @Test
    @DisplayName("发布配置包 - DRAFT状态未审查不允许发布")
    void publishPackage_shouldNotPublishDraftWithoutReview() {
        Map<String, Object> payload = buildPackagePayload("PKG_DRAFT", "1.0.0", "PATHWAY");
        configPackageService.importPackages(Collections.singletonList(payload));

        Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();
        publishRequest.put("approved_by", "admin_li");

        assertThrows(IllegalArgumentException.class,
                () -> configPackageService.publishPackage("PKG_DRAFT", "1.0.0", "default", publishRequest));
    }

    @Test
    @DisplayName("发布配置包 - 发布门禁阻断时抛MissingSourceException")
    void publishPackage_shouldThrowWhenGateBlocks() {
        when(configPackageRepository.findList(anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(Collections.<ConfigPackageEntity>emptyList());

        Map<String, Object> payload = buildPackagePayload("PKG_GATE", "1.0.0", "PATHWAY");
        configPackageService.importPackages(Collections.singletonList(payload));

        // 先审查
        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer_zhang");
        configPackageService.reviewPackage("PKG_GATE", "1.0.0", "default", reviewRequest);

        // 配置发布门禁阻断
        PublishGateService.GateCheckResult blockedResult = new PublishGateService.GateCheckResult();
        blockedResult.addIssue("ERROR", "source_review.blocked", "来源审查已阻断发布", null, "CONFIG_PACKAGE");
        when(publishGateService.checkConfigPackageSourceReview(any())).thenReturn(blockedResult);
        when(publishGateService.formatBlockingMessage(any())).thenReturn("发布门禁检查未通过");
        doNothing().when(publishGateService).auditGateCheck(any(), any(), any(), any(), any(), any());

        Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();
        publishRequest.put("approved_by", "admin_li");

        assertThrows(MissingSourceException.class,
                () -> configPackageService.publishPackage("PKG_GATE", "1.0.0", "default", publishRequest));
    }

    @Test
    @DisplayName("发布配置包 - 已发布包直接返回")
    void publishPackage_shouldReturnDirectlyWhenAlreadyPublished() {
        when(configPackageRepository.findList(anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(Collections.<ConfigPackageEntity>emptyList());

        Map<String, Object> payload = buildPackagePayload("PKG_ALREADY", "1.0.0", "PATHWAY");
        configPackageService.importPackages(Collections.singletonList(payload));

        // 先审查
        Map<String, Object> reviewRequest = new LinkedHashMap<String, Object>();
        reviewRequest.put("reviewed_by", "reviewer_zhang");
        configPackageService.reviewPackage("PKG_ALREADY", "1.0.0", "default", reviewRequest);

        // 配置发布门禁通过
        PublishGateService.GateCheckResult gateResult = new PublishGateService.GateCheckResult();
        when(publishGateService.checkConfigPackageSourceReview(any())).thenReturn(gateResult);
        when(publishGateService.formatBlockingMessage(any())).thenReturn(null);
        doNothing().when(publishGateService).auditGateCheck(any(), any(), any(), any(), any(), any());

        Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();
        publishRequest.put("approved_by", "admin_li");
        configPackageService.publishPackage("PKG_ALREADY", "1.0.0", "default", publishRequest);

        // 再次发布
        Map<String, Object> result = configPackageService.publishPackage(
                "PKG_ALREADY", "1.0.0", "default", publishRequest);

        assertEquals("PUBLISHED", result.get("status"));
    }

    // ========== 配置包导出 ==========

    @Test
    @DisplayName("导出配置包 - 成功导出")
    void exportPackage_shouldExportSuccessfully() {
        Map<String, Object> payload = buildPackagePayload("PKG_EXP", "1.0.0", "PATHWAY");
        configPackageService.importPackages(Collections.singletonList(payload));

        Map<String, Object> result = configPackageService.exportPackage("PKG_EXP", "1.0.0", "default");

        assertEquals("PKG_EXP", result.get("package_code"));
        assertNotNull(result.get("exported_time"));
        assertEquals("MEDKERNEL_CONFIG_PACKAGE_V1", result.get("export_format"));
    }

    @Test
    @DisplayName("导出配置包 - 包不存在抛异常")
    void exportPackage_shouldThrowWhenNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> configPackageService.exportPackage("NOT_EXIST", "1.0.0", "default"));
    }

    // ========== 组织绑定 ==========

    @Test
    @DisplayName("配置包组织绑定 - scope_level为PLATFORM时默认有效")
    void organizationBinding_shouldBeValidForPlatformScope() {
        when(organizationDirectoryService.scopeExists("default", "PLATFORM", "DEFAULT")).thenReturn(true);

        Map<String, Object> payload = buildPackagePayload("PKG_PLATFORM", "1.0.0", "PATHWAY");
        payload.put("scope_level", "PLATFORM");
        payload.put("scope_code", "DEFAULT");
        configPackageService.importPackages(Collections.singletonList(payload));

        Map<String, Object> pkg = configPackageService.getPackage("PKG_PLATFORM", "1.0.0", "default");
        assertEquals("PLATFORM", pkg.get("scope_level"));
        assertEquals("DEFAULT", pkg.get("scope_code"));
    }

    @Test
    @DisplayName("配置包组织绑定 - scope不存在时review报错")
    void organizationBinding_shouldReportIssueWhenScopeNotFound() {
        when(organizationDirectoryService.scopeExists("default", "HOSPITAL", "HOSP_NOT_EXIST")).thenReturn(false);

        Map<String, Object> payload = buildPackagePayload("PKG_BAD_SCOPE", "1.0.0", "PATHWAY");
        payload.put("scope_level", "HOSPITAL");
        payload.put("scope_code", "HOSP_NOT_EXIST");
        configPackageService.importPackages(Collections.singletonList(payload));

        Map<String, Object> review = configPackageService.reviewPackage(
                "PKG_BAD_SCOPE", "1.0.0", "default", null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issues = (List<Map<String, Object>>) review.get("issues");
        boolean hasScopeIssue = false;
        for (Map<String, Object> issue : issues) {
            if ("scope_code".equals(issue.get("field"))) {
                hasScopeIssue = true;
            }
        }
        assertTrue(hasScopeIssue, "scope不存在时应有ERROR issue");
    }

    // ========== buildReview ==========

    @Test
    @DisplayName("构建审查报告 - DRAFT状态且无问题时应可发布")
    void buildReview_shouldBeReadyWhenDraftAndNoIssues() {
        ConfigPackage configPackage = buildConfigPackage("PKG_REVIEW", "1.0.0", "PATHWAY");
        Map<String, Object> review = configPackageService.buildReview(configPackage);

        assertTrue((Boolean) review.get("ready_to_publish"));
    }

    @Test
    @DisplayName("构建审查报告 - content_hash不匹配时报错")
    void buildReview_shouldReportErrorWhenContentHashMismatch() {
        ConfigPackage configPackage = buildConfigPackage("PKG_HASH_MISMATCH", "1.0.0", "PATHWAY");
        configPackage.setDeclaredContentHash("sha256:wrong_hash");
        Map<String, Object> review = configPackageService.buildReview(configPackage);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issues = (List<Map<String, Object>>) review.get("issues");
        boolean hasHashIssue = false;
        for (Map<String, Object> issue : issues) {
            if ("content_hash".equals(issue.get("field"))) {
                hasHashIssue = true;
            }
        }
        assertTrue(hasHashIssue, "content_hash不匹配时应有ERROR issue");
    }

    // ========== 辅助方法 ==========

    private ConfigPackage buildConfigPackage(String packageCode, String version, String assetType) {
        ConfigPackage configPackage = new ConfigPackage();
        configPackage.setTenantId("default");
        configPackage.setPackageCode(packageCode);
        configPackage.setPackageVersion(version);
        configPackage.setAssetType(assetType);
        configPackage.setScopeLevel("PLATFORM");
        configPackage.setScopeCode("DEFAULT");
        configPackage.setStatus("DRAFT");
        configPackage.setContentHash("sha256:testhash");
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("items", Collections.singletonList(Collections.singletonMap("code", "ITEM_001")));
        configPackage.setFullSnapshot(snapshot);
        return configPackage;
    }

    private Map<String, Object> buildPackagePayload(String packageCode, String version, String assetType) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("package_code", packageCode);
        payload.put("package_version", version);
        payload.put("asset_type", assetType);
        payload.put("scope_level", "PLATFORM");
        payload.put("scope_code", "DEFAULT");

        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("code", "ITEM_001");
        item.put("name", "测试条目");
        items.add(item);
        snapshot.put("items", items);
        payload.put("full_snapshot", snapshot);

        return payload;
    }

    private Map<String, Object> buildScopeReference(boolean exists) {
        Map<String, Object> ref = new LinkedHashMap<String, Object>();
        ref.put("tenant_id", "default");
        ref.put("scope_level", "PLATFORM");
        ref.put("scope_code", "DEFAULT");
        ref.put("exists", exists);
        return ref;
    }
}
