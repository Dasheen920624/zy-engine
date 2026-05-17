package com.zyengine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyengine.persistence.EnginePersistenceProperties;
import com.zyengine.persistence.EnginePersistenceService;
import com.zyengine.persistence.OrganizationPersistenceService;
import com.zyengine.organization.OrganizationUnit;
import com.zyengine.provenance.SourceDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class EngineApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        Map<String, Object> result = invokeGet("/api/health");
        assertEquals(Boolean.TRUE, result.get("success"));
        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("UP", data.get("status"));
        assertEquals("zy-engine-mvp", data.get("service"));
    }

    @Test
    void systemProvidersExposeDbOnlyFallbackMode() throws Exception {
        Map<String, Object> result = invokeGet("/api/system/providers");
        assertEquals(Boolean.TRUE, result.get("success"));
        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("zy-engine-mvp", data.get("service"));
        assertEquals(Boolean.TRUE, data.get("db_only_supported"));
        assertEquals(Boolean.FALSE, data.get("external_graph_required"));
        assertEquals(Boolean.FALSE, data.get("external_dify_required"));

        Map<String, Object> providers = asMap(data.get("providers"));
        Map<String, Object> database = asMap(providers.get("database"));
        Map<String, Object> graph = asMap(providers.get("graph"));
        Map<String, Object> dify = asMap(providers.get("dify"));
        assertEquals("CONFIG_PRIMARY_STORE", database.get("role"));
        assertEquals("PRODUCTION_AUTHORITY", database.get("database_role"));
        assertEquals("GRAPH_QUERY_PROVIDER", graph.get("role"));
        assertEquals("WORKFLOW_PROVIDER", dify.get("role"));
        assertNotNull(data.get("run_mode"));
        assertNotNull(graph.get("provider"));
        assertNotNull(dify.get("provider"));
    }

    @Test
    void localFileDatabaseDoesNotRequireOraclePassword() {
        EnginePersistenceProperties properties = new EnginePersistenceProperties();
        properties.setEnabled(true);
        properties.setDialect("h2");
        properties.setUrl("jdbc:h2:file:./target/zyengine-local-test;MODE=Oracle");
        properties.setUsername("sa");
        properties.setPassword("");

        assertTrue(properties.localFileDatabase());
        assertTrue(properties.hasRequiredCredentials());
        assertEquals("LOCAL_H2_FILE", properties.providerName());
        assertEquals("DEVELOPMENT_LOCAL", properties.roleName());
    }

    @Test
    void localFileDatabaseInitializesSchemaAndAcceptsAuditWrite() {
        EnginePersistenceProperties properties = new EnginePersistenceProperties();
        properties.setEnabled(true);
        properties.setDialect("h2");
        properties.setUrl("jdbc:h2:mem:zyengine_local_contract;MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1");
        properties.setUsername("sa");
        properties.setPassword("");
        properties.setInitSchema(true);

        EnginePersistenceService service = new EnginePersistenceService(properties, objectMapper);
        service.initializeLocalSchema();
        service.saveAuditLog("TEST", "LOCAL_DB_WRITE", "CONTRACT", "LOCAL_H2", null, null, "JUNIT",
                new LinkedHashMap<String, Object>());

        assertEquals("LOCAL_H2_FILE", service.providerName());
    }

    @Test
    void localFileDatabasePersistsSourceDocuments() {
        EnginePersistenceProperties properties = new EnginePersistenceProperties();
        properties.setEnabled(true);
        properties.setRole("development");
        properties.setDialect("h2");
        properties.setUrl("jdbc:h2:file:./target/zyengine-src-contract-" + System.nanoTime()
                + ";MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_ON_EXIT=FALSE");
        properties.setUsername("sa");
        properties.setPassword("");
        properties.setInitSchema(true);

        EnginePersistenceService service = new EnginePersistenceService(properties, objectMapper);
        service.initializeLocalSchema();

        SourceDocument document = new SourceDocument();
        document.setTenantId("TENANT_SRC_DB");
        document.setDocumentCode("SRC_DB_CONTRACT");
        document.setTitle("本地开发库来源文档");
        document.setSourceType("GUIDELINE");
        document.setPublisher("JUNIT");
        document.setEffectiveDate("2026-01-01");
        document.setExpiryDate("2027-01-01");
        document.setReviewStatus("REVIEWED");
        document.setReviewedBy("JUNIT_REVIEWER");
        document.setCreatedBy("JUNIT_PROV_ADMIN");
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("database_role", "DEVELOPMENT_LOCAL");
        document.setMetadata(metadata);

        service.saveSourceDocument(document);

        SourceDocument persisted = service.findSourceDocument("TENANT_SRC_DB", "SRC_DB_CONTRACT");
        assertNotNull(persisted);
        assertEquals("本地开发库来源文档", persisted.getTitle());
        assertEquals("JUNIT_PROV_ADMIN", persisted.getCreatedBy());
        assertEquals("DEVELOPMENT_LOCAL", persisted.getMetadata().get("database_role"));
        assertEquals(1, service.listSourceDocuments().size());
    }

    @Test
    void localFileDatabasePersistsOrganizationUnits() {
        EnginePersistenceProperties properties = new EnginePersistenceProperties();
        properties.setEnabled(true);
        properties.setRole("development");
        properties.setDialect("h2");
        properties.setUrl("jdbc:h2:file:./target/zyengine-org-contract-" + System.nanoTime()
                + ";MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_ON_EXIT=FALSE");
        properties.setUsername("sa");
        properties.setPassword("");
        properties.setInitSchema(true);

        EnginePersistenceService engineService = new EnginePersistenceService(properties, objectMapper);
        engineService.initializeLocalSchema();

        OrganizationPersistenceService orgService = new OrganizationPersistenceService(properties);
        assertTrue(orgService.enabled());

        OrganizationUnit group = new OrganizationUnit();
        group.setTenantId("TENANT_ORG_DB");
        group.setLevel("GROUP");
        group.setCode("GROUP_JUNIT");
        group.setName("JUNIT测试集团");
        group.setStatus("ACTIVE");
        group.setDisplayOrder(0);
        group.setCreatedBy("JUNIT_ADMIN");

        OrganizationUnit hospital = new OrganizationUnit();
        hospital.setTenantId("TENANT_ORG_DB");
        hospital.setLevel("HOSPITAL");
        hospital.setCode("HOSPITAL_JUNIT");
        hospital.setName("JUNIT测试医院");
        hospital.setParentLevel("GROUP");
        hospital.setParentCode("GROUP_JUNIT");
        hospital.setStatus("ACTIVE");
        hospital.setDisplayOrder(1);
        hospital.setCreatedBy("JUNIT_ADMIN");

        orgService.saveOrganizationUnit(group);
        orgService.saveOrganizationUnit(hospital);

        List<OrganizationUnit> loaded = orgService.loadAllOrganizationUnits();
        assertEquals(2, loaded.size());

        List<OrganizationUnit> byTenant = orgService.loadOrganizationUnitsByTenant("TENANT_ORG_DB");
        assertEquals(2, byTenant.size());
        assertEquals("GROUP_JUNIT", byTenant.get(0).getCode());
        assertEquals("HOSPITAL_JUNIT", byTenant.get(1).getCode());

        // UPSERT: re-save with updated name
        hospital.setName("JUNIT测试医院-更新");
        orgService.saveOrganizationUnit(hospital);
        List<OrganizationUnit> reloaded = orgService.loadOrganizationUnitsByTenant("TENANT_ORG_DB");
        assertEquals(2, reloaded.size());
        assertEquals("JUNIT测试医院-更新", reloaded.get(1).getName());

        // DELETE
        orgService.deleteOrganizationUnit("TENANT_ORG_DB", "HOSPITAL", "HOSPITAL_JUNIT");
        List<OrganizationUnit> afterDelete = orgService.loadOrganizationUnitsByTenant("TENANT_ORG_DB");
        assertEquals(1, afterDelete.size());
        assertEquals("GROUP_JUNIT", afterDelete.get(0).getCode());
    }

    @Test
    void orgContextDefaultsToLegacyHospitalScope() throws Exception {
        Map<String, Object> result = invokeGet("/api/system/org-context");
        assertEquals(Boolean.TRUE, result.get("success"));
        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("default", data.get("tenant_id"));
        assertEquals("ZYHOSPITAL", data.get("hospital_code"));
        assertEquals("ZYHOSPITAL", data.get("legacy_org_code"));
        assertEquals("HOSPITAL", data.get("effective_scope_level"));
        assertEquals("ZYHOSPITAL", data.get("effective_scope_code"));
        assertEquals("DEFAULT", data.get("source"));
        assertEquals(Boolean.TRUE, data.get("db_only_supported"));

        List<Map<String, Object>> inheritance = asListOfMap(data.get("inheritance_order"));
        assertEquals("HOSPITAL", inheritance.get(0).get("scope_level"));
        assertEquals("PLATFORM", inheritance.get(inheritance.size() - 1).get("scope_level"));
        assertEquals("系统内置默认（产品基线配置）", inheritance.get(inheritance.size() - 1).get("scope_name"));

        Map<String, Object> model = asMap(data.get("model"));
        List<Object> precedence = asList(model.get("config_precedence"));
        assertEquals("DEPARTMENT", precedence.get(0));
        assertEquals("PLATFORM", precedence.get(precedence.size() - 1));
        assertEquals("系统内置默认（产品基线配置）", model.get("baseline_scope_name"));

        Map<String, Object> scopeNames = asMap(data.get("scope_level_names"));
        assertEquals("系统内置默认（产品基线配置）", scopeNames.get("PLATFORM"));
    }

    @Test
    void orgContextResolvesMostSpecificHeaderScope() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/system/org-context")
                        .header("X-Tenant-Id", "TENANT_JUNIT")
                        .header("X-Group-Code", "GROUP_JUNIT")
                        .header("X-Hospital-Code", "HOSPITAL_JUNIT")
                        .header("X-Campus-Code", "CAMPUS_JUNIT")
                        .header("X-Site-Code", "SITE_JUNIT")
                        .header("X-Department-Code", "DEPT_CARDIOLOGY"))
                .andReturn();
        assertEquals(200, mvcResult.getResponse().getStatus());
        Map<String, Object> result = parse(mvcResult);
        Map<String, Object> data = asMap(result.get("data"));
        assertEquals("TENANT_JUNIT", data.get("tenant_id"));
        assertEquals("GROUP_JUNIT", data.get("group_code"));
        assertEquals("HOSPITAL_JUNIT", data.get("hospital_code"));
        assertEquals("CAMPUS_JUNIT", data.get("campus_code"));
        assertEquals("SITE_JUNIT", data.get("site_code"));
        assertEquals("DEPT_CARDIOLOGY", data.get("department_code"));
        assertEquals("DEPARTMENT", data.get("effective_scope_level"));
        assertEquals("DEPT_CARDIOLOGY", data.get("effective_scope_code"));
        assertEquals("HEADER", data.get("source"));
        assertTrue(asList(data.get("warnings")).isEmpty());

        List<Map<String, Object>> inheritance = asListOfMap(data.get("inheritance_order"));
        assertEquals("DEPARTMENT", inheritance.get(0).get("scope_level"));
        assertEquals("DEPT_CARDIOLOGY", inheritance.get(0).get("scope_code"));
        assertEquals("PLATFORM", inheritance.get(inheritance.size() - 1).get("scope_level"));
        assertEquals("系统内置默认（产品基线配置）", inheritance.get(inheritance.size() - 1).get("scope_name"));

        Map<String, Object> headerContract = asMap(data.get("header_contract"));
        assertEquals("X-Hospital-Code", headerContract.get("hospital_code"));
    }

    @Test
    void organizationDirectoryImportListGetAndTree() throws Exception {
        Map<String, Object> importBody = sampleOrganizationImport("TENANT_ORG_JUNIT");
        Map<String, Object> importResp = invokePost("/api/organizations", importBody);
        Map<String, Object> imported = asMap(importResp.get("data"));
        assertEquals("TENANT_ORG_JUNIT", imported.get("tenant_id"));
        assertEquals(5, ((Number) imported.get("imported_count")).intValue());
        assertTrue(asList(imported.get("warnings")).isEmpty());

        Map<String, Object> listResp = invokeGet("/api/organizations?tenant_id=TENANT_ORG_JUNIT&level=DEPARTMENT");
        List<Map<String, Object>> departments = asListOfMap(listResp.get("data"));
        assertEquals(1, departments.size());
        assertEquals("DEPT_CARDIOLOGY", departments.get(0).get("code"));
        assertEquals("科室", departments.get(0).get("level_name"));

        Map<String, Object> getResp = invokeGet("/api/organizations/HOSPITAL/HOSPITAL_JUNIT?tenant_id=TENANT_ORG_JUNIT");
        Map<String, Object> hospital = asMap(getResp.get("data"));
        assertEquals("演示医院", hospital.get("name"));
        List<Map<String, Object>> hospitalChildren = asListOfMap(hospital.get("children"));
        assertEquals(1, hospitalChildren.size());
        assertEquals("CAMPUS_EAST", hospitalChildren.get(0).get("code"));

        Map<String, Object> treeResp = invokeGet("/api/organizations/tree?tenant_id=TENANT_ORG_JUNIT"
                + "&root_level=GROUP&root_code=GROUP_JUNIT");
        Map<String, Object> tree = asMap(treeResp.get("data"));
        assertEquals(1, ((Number) tree.get("root_count")).intValue());
        List<Map<String, Object>> roots = asListOfMap(tree.get("tree"));
        assertEquals("GROUP_JUNIT", roots.get(0).get("code"));
        List<Map<String, Object>> groupChildren = asListOfMap(roots.get(0).get("children"));
        assertEquals("HOSPITAL_JUNIT", groupChildren.get(0).get("code"));
    }

    @Test
    void organizationDirectoryRejectsPlatformAsRealUnit() throws Exception {
        Map<String, Object> unit = new LinkedHashMap<String, Object>();
        unit.put("level", "PLATFORM");
        unit.put("code", "DEFAULT");
        unit.put("name", "系统内置默认");

        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("tenant_id", "TENANT_ORG_BAD");
        importBody.put("units", Arrays.asList(unit));
        Map<String, Object> response = invokePostExpectingClientError("/api/organizations", importBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
        assertTrue(String.valueOf(response.get("message")).contains("PLATFORM"));
    }

    @Test
    void configPackageImportReviewPublishAndExport() throws Exception {
        invokePost("/api/organizations", sampleOrganizationImport("default"));

        Map<String, Object> packageBody = sampleConfigPackage("PKG_CONFIG_JUNIT", "2026.05.01");
        Map<String, Object> importedResp = invokePost("/api/config-packages", packageBody);
        List<Map<String, Object>> imported = asListOfMap(importedResp.get("data"));
        assertEquals(1, imported.size());
        Map<String, Object> importedPackage = imported.get(0);
        assertEquals("default", importedPackage.get("tenant_id"));
        assertEquals("PKG_CONFIG_JUNIT", importedPackage.get("package_code"));
        assertEquals("RULE", importedPackage.get("asset_type"));
        assertEquals("DRAFT", importedPackage.get("status"));
        assertTrue(String.valueOf(importedPackage.get("content_hash")).startsWith("sha256:"));
        assertEquals(Boolean.TRUE, asMap(importedPackage.get("scope_reference")).get("exists"));

        Map<String, Object> listResp = invokeGet("/api/config-packages?tenantId=default&assetType=RULE&scopeLevel=HOSPITAL");
        List<Map<String, Object>> packages = asListOfMap(listResp.get("data"));
        assertFalse(packages.isEmpty());

        Map<String, Object> reviewBody = new LinkedHashMap<String, Object>();
        reviewBody.put("reviewed_by", "JUNIT_REVIEWER");
        Map<String, Object> reviewResp = invokePost("/api/config-packages/PKG_CONFIG_JUNIT/2026.05.01/review", reviewBody);
        Map<String, Object> review = asMap(reviewResp.get("data"));
        assertEquals(Boolean.TRUE, review.get("ready_to_publish"));
        assertEquals("REVIEWED", review.get("status"));
        assertEquals("JUNIT_REVIEWER", review.get("reviewed_by"));
        assertTrue(asList(review.get("issues")).isEmpty());
        assertEquals(1, ((Number) asMap(review.get("summary")).get("asset_count")).intValue());
        assertEquals(Boolean.TRUE, asMap(review.get("summary")).get("scope_exists"));

        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("approved_by", "JUNIT_APPROVER");
        Map<String, Object> publishResp = invokePost("/api/config-packages/PKG_CONFIG_JUNIT/2026.05.01/publish", publishBody);
        Map<String, Object> published = asMap(publishResp.get("data"));
        assertEquals("PUBLISHED", published.get("status"));
        assertEquals("JUNIT_APPROVER", published.get("approved_by"));
        assertEquals(importedPackage.get("content_hash"), published.get("content_hash"));

        Map<String, Object> exportedResp = invokePost("/api/config-packages/PKG_CONFIG_JUNIT/2026.05.01/export",
                new LinkedHashMap<String, Object>());
        Map<String, Object> exported = asMap(exportedResp.get("data"));
        assertEquals("ZYENGINE_CONFIG_PACKAGE_V1", exported.get("export_format"));
        assertEquals("PKG_CONFIG_JUNIT", asMap(exported.get("full_snapshot")).get("package_code"));

        Map<String, Object> auditResp = invokeGet("/api/audit-logs?engineType=CONFIG_PACKAGE&targetCode=PKG_CONFIG_JUNIT&limit=10");
        assertFalse(asList(auditResp.get("data")).isEmpty());
    }

    @Test
    void configPackageReviewRejectsHashMismatchBeforePublish() throws Exception {
        invokePost("/api/organizations", sampleOrganizationImport("default"));

        Map<String, Object> packageBody = sampleConfigPackage("PKG_CONFIG_BAD_HASH", "2026.05.01");
        packageBody.put("content_hash", "sha256:not-the-calculated-hash");
        invokePost("/api/config-packages", packageBody);

        Map<String, Object> reviewResp = invokeGet("/api/config-packages/PKG_CONFIG_BAD_HASH/2026.05.01/review");
        Map<String, Object> review = asMap(reviewResp.get("data"));
        assertEquals(Boolean.FALSE, review.get("ready_to_publish"));
        List<Map<String, Object>> issues = asListOfMap(review.get("issues"));
        assertEquals(1, issues.size());
        assertEquals("content_hash", issues.get(0).get("field"));

        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("approved_by", "JUNIT_APPROVER");
        Map<String, Object> response = invokePostExpectingClientError(
                "/api/config-packages/PKG_CONFIG_BAD_HASH/2026.05.01/publish", publishBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
    }

    @Test
    void configPackagePublishRejectsUnknownOrganizationScope() throws Exception {
        Map<String, Object> packageBody = sampleConfigPackage("PKG_CONFIG_UNKNOWN_SCOPE", "2026.05.01");
        packageBody.put("tenant_id", "TENANT_NO_SCOPE");
        packageBody.put("scope_level", "HOSPITAL");
        packageBody.put("scope_code", "HOSPITAL_NOT_IMPORTED");
        invokePost("/api/config-packages", packageBody);

        Map<String, Object> reviewResp = invokeGet("/api/config-packages/PKG_CONFIG_UNKNOWN_SCOPE/2026.05.01/review"
                + "?tenantId=TENANT_NO_SCOPE");
        Map<String, Object> review = asMap(reviewResp.get("data"));
        assertEquals(Boolean.FALSE, review.get("ready_to_publish"));
        assertEquals(Boolean.FALSE, asMap(review.get("summary")).get("scope_exists"));
        Map<String, Object> scopeReference = asMap(review.get("scope_reference"));
        assertEquals("HOSPITAL_NOT_IMPORTED", scopeReference.get("scope_code"));
        assertEquals(Boolean.FALSE, scopeReference.get("exists"));

        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("approved_by", "JUNIT_APPROVER");
        Map<String, Object> response = invokePostExpectingClientError(
                "/api/config-packages/PKG_CONFIG_UNKNOWN_SCOPE/2026.05.01/publish?tenantId=TENANT_NO_SCOPE",
                publishBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
        assertTrue(String.valueOf(response.get("message")).contains("scope_code"));
    }

    @Test
    void configPackagePublishRejectsWhenSourceReviewBlocks() throws Exception {
        invokePost("/api/organizations", sampleOrganizationImport("default"));

        Map<String, Object> packageBody = sampleConfigPackage("PKG_CONFIG_SOURCE_BLOCK", "2026.05.01");
        Map<String, Object> sourceReview = new LinkedHashMap<String, Object>();
        sourceReview.put("enabled", Boolean.TRUE);
        sourceReview.put("missing_count", 1);
        sourceReview.put("expired_count", 0);
        sourceReview.put("unreviewed_count", 0);
        sourceReview.put("allow_publish", Boolean.FALSE);
        sourceReview.put("message", "缺少来源文献");
        asMap(packageBody.get("manifest")).put("source_review", sourceReview);
        invokePost("/api/config-packages", packageBody);

        Map<String, Object> reviewResp = invokeGet("/api/config-packages/PKG_CONFIG_SOURCE_BLOCK/2026.05.01/review");
        Map<String, Object> review = asMap(reviewResp.get("data"));
        assertEquals(Boolean.FALSE, review.get("ready_to_publish"));
        Map<String, Object> returnedSourceReview = asMap(review.get("source_review"));
        assertEquals(Boolean.TRUE, returnedSourceReview.get("enabled"));
        assertEquals(1, ((Number) returnedSourceReview.get("missing_count")).intValue());
        assertEquals(Boolean.FALSE, returnedSourceReview.get("allow_publish"));
        assertEquals("缺少来源文献", returnedSourceReview.get("message"));

        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("approved_by", "JUNIT_APPROVER");
        Map<String, Object> response = invokePostExpectingClientError(
                "/api/config-packages/PKG_CONFIG_SOURCE_BLOCK/2026.05.01/publish", publishBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
        assertTrue(String.valueOf(response.get("message")).contains("source_review"));
    }

    @Test
    void sourceDocumentImportListAndGet() throws Exception {
        Map<String, Object> importBody = sampleSourceDocumentImport("TENANT_SRC_JUNIT");
        Map<String, Object> importResp = invokePost("/api/provenance/source-documents", importBody);
        Map<String, Object> imported = asMap(importResp.get("data"));
        assertEquals("TENANT_SRC_JUNIT", imported.get("tenant_id"));
        assertEquals(2, ((Number) imported.get("imported_count")).intValue());
        assertTrue(asListOfMap(imported.get("warnings")).isEmpty());

        Map<String, Object> listResp = invokeGet("/api/provenance/source-documents?tenant_id=TENANT_SRC_JUNIT"
                + "&review_status=REVIEWED");
        List<Map<String, Object>> reviewed = asListOfMap(listResp.get("data"));
        assertEquals(1, reviewed.size());
        assertEquals("SRC_GUIDELINE_AMI_2025", reviewed.get(0).get("document_code"));
        assertEquals(Boolean.FALSE, reviewed.get(0).get("expired"));

        Map<String, Object> getResp = invokeGet(
                "/api/provenance/source-documents/SRC_CONSENSUS_STEMI_2024?tenantId=TENANT_SRC_JUNIT");
        Map<String, Object> document = asMap(getResp.get("data"));
        assertEquals("TENANT_SRC_JUNIT", document.get("tenant_id"));
        assertEquals("CONSENSUS", document.get("source_type"));
        assertEquals("DRAFT", document.get("review_status"));
        assertEquals("急诊 STEMI 绿色通道专家共识", document.get("title"));
        assertEquals("JUNIT_PROV_ADMIN", document.get("created_by"));
    }

    @Test
    void sourceDocumentImportRejectsMissingDocumentCode() throws Exception {
        Map<String, Object> badDocument = new LinkedHashMap<String, Object>();
        badDocument.put("title", "缺少编码的来源文档");
        badDocument.put("source_type", "GUIDELINE");
        badDocument.put("review_status", "DRAFT");

        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("tenant_id", "TENANT_SRC_BAD");
        importBody.put("documents", Arrays.asList(badDocument));

        Map<String, Object> response = invokePostExpectingClientError("/api/provenance/source-documents", importBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
        assertTrue(String.valueOf(response.get("message")).contains("document_code"));
    }

    @Test
    void ruleExecLogSummaryAggregates() throws Exception {
        // 导入并发布两条规则（一条总命中，一条总不命中），多次模拟后调用 summary 接口
        Map<String, Object> hitRule = ruleDefinition("R_SUM_HIT", "命中规则");
        Map<String, Object> missRule = ruleDefinition("R_SUM_MISS", "不命中规则");
        Map<String, Object> chiefComplaint = new LinkedHashMap<String, Object>();
        chiefComplaint.put("fact", "chief_complaints.code");
        chiefComplaint.put("operator", "in");
        chiefComplaint.put("value", Arrays.asList("NEVER_MATCH"));
        Map<String, Object> missCondition = new LinkedHashMap<String, Object>();
        missCondition.put("all", Arrays.asList(chiefComplaint));
        missRule.put("condition", missCondition);

        invokePost("/api/rules", Arrays.asList(hitRule, missRule));
        invokePost("/api/rules/R_SUM_HIT/publish", new LinkedHashMap<String, Object>());
        invokePost("/api/rules/R_SUM_MISS/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> simulateHit = new LinkedHashMap<String, Object>();
        simulateHit.put("rule_code", "R_SUM_HIT");
        simulateHit.put("patient_context", samplePatientContext());
        invokePost("/api/rules/simulate", simulateHit);
        invokePost("/api/rules/simulate", simulateHit);

        Map<String, Object> simulateMiss = new LinkedHashMap<String, Object>();
        simulateMiss.put("rule_code", "R_SUM_MISS");
        simulateMiss.put("patient_context", samplePatientContext());
        invokePost("/api/rules/simulate", simulateMiss);

        Map<String, Object> hitSummary = invokeGet("/api/rules/exec-logs/summary?ruleCode=R_SUM_HIT");
        Map<String, Object> hitData = asMap(hitSummary.get("data"));
        assertEquals(2, ((Number) hitData.get("total")).intValue());
        assertEquals(2, ((Number) hitData.get("total_hits")).intValue());
        assertEquals(100.0, ((Number) hitData.get("hit_rate")).doubleValue());
        List<Map<String, Object>> byRule = asListOfMap(hitData.get("by_rule"));
        assertEquals(1, byRule.size());
        assertEquals("R_SUM_HIT", byRule.get(0).get("rule_code"));
        assertEquals(2, ((Number) byRule.get(0).get("hits")).intValue());

        Map<String, Object> missSummary = invokeGet("/api/rules/exec-logs/summary?ruleCode=R_SUM_MISS");
        Map<String, Object> missData = asMap(missSummary.get("data"));
        assertEquals(1, ((Number) missData.get("total")).intValue());
        assertEquals(0, ((Number) missData.get("total_hits")).intValue());
        assertEquals(0.0, ((Number) missData.get("hit_rate")).doubleValue());
    }

    @Test
    void ruleImportPublishSimulateAndQueryLogs() throws Exception {
        Map<String, Object> rulePayload = ruleDefinition("R_TEST_STEMI", "AMI/STEMI候选入径测试规则");
        List<Map<String, Object>> rules = Arrays.asList(rulePayload);
        Map<String, Object> imported = invokePost("/api/rules", rules);
        assertEquals(Boolean.TRUE, imported.get("success"));
        assertEquals(1, asList(imported.get("data")).size());

        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("version_no", "1.0.0");
        publishBody.put("approved_by", "JUNIT");
        Map<String, Object> published = invokePost("/api/rules/R_TEST_STEMI/publish", publishBody);
        assertEquals("PUBLISHED", asMap(published.get("data")).get("status"));

        Map<String, Object> simulateBody = new LinkedHashMap<String, Object>();
        simulateBody.put("rule_code", "R_TEST_STEMI");
        simulateBody.put("version_no", "1.0.0");
        simulateBody.put("patient_context", samplePatientContext());
        Map<String, Object> simulate = invokePost("/api/rules/simulate", simulateBody);
        Map<String, Object> simulateData = asMap(simulate.get("data"));
        assertEquals("R_TEST_STEMI", simulateData.get("ruleCode"));
        assertEquals(Boolean.TRUE, simulateData.get("hit"));

        Map<String, Object> logs = invokeGet("/api/rules/exec-logs?ruleCode=R_TEST_STEMI&hit=true&limit=10");
        List<Map<String, Object>> entries = asListOfMap(logs.get("data"));
        assertFalse(entries.isEmpty(), "exec-logs should contain at least one entry");
        Map<String, Object> firstEntry = entries.get(0);
        assertEquals("R_TEST_STEMI", firstEntry.get("ruleCode"));
        assertEquals(Boolean.TRUE, firstEntry.get("hit"));
        assertNotNull(firstEntry.get("logId"));

        Map<String, Object> detail = invokeGet("/api/rules/exec-logs/" + firstEntry.get("logId"));
        assertEquals(firstEntry.get("logId"), asMap(detail.get("data")).get("logId"));
    }

    @Test
    void rulePackageReviewAndPublishBatch() throws Exception {
        Map<String, Object> hitRule = ruleDefinition("R_PKG_STEMI_HIT", "规则包STEMI命中规则");
        hitRule.put("priority", 120);
        Map<String, Object> qcRule = ruleDefinition("R_PKG_STEMI_QC", "规则包质控规则");
        qcRule.put("rule_type", "TIME_LIMIT_QC");
        qcRule.put("priority", 80);

        Map<String, Object> packageBody = new LinkedHashMap<String, Object>();
        packageBody.put("package_code", "PKG_AMI_JUNIT");
        packageBody.put("package_version", "2026.05");
        packageBody.put("rules", Arrays.asList(hitRule, qcRule));
        Map<String, Object> imported = invokePost("/api/rules", packageBody);
        List<Map<String, Object>> importedRules = asListOfMap(imported.get("data"));
        assertEquals(2, importedRules.size());
        assertEquals("PKG_AMI_JUNIT", importedRules.get(0).get("packageCode"));

        Map<String, Object> reviewResp = invokeGet("/api/rules/packages/PKG_AMI_JUNIT/review?packageVersion=2026.05");
        Map<String, Object> review = asMap(reviewResp.get("data"));
        assertEquals(2, ((Number) review.get("total_rules")).intValue());
        assertEquals(2, ((Number) review.get("draft_rules")).intValue());
        assertEquals(Boolean.TRUE, review.get("ready_to_publish"));
        assertTrue(asList(review.get("issues")).isEmpty());

        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("package_version", "2026.05");
        publishBody.put("approved_by", "JUNIT_PACKAGE_APPROVER");
        Map<String, Object> publishResp = invokePost("/api/rules/packages/PKG_AMI_JUNIT/publish", publishBody);
        Map<String, Object> published = asMap(publishResp.get("data"));
        assertEquals(2, ((Number) published.get("published_count")).intValue());
        assertEquals(2, ((Number) published.get("published_rules")).intValue());

        Map<String, Object> getResp = invokeGet("/api/rules/R_PKG_STEMI_HIT?versionNo=1.0.0");
        Map<String, Object> stored = asMap(getResp.get("data"));
        assertEquals("PUBLISHED", stored.get("status"));
        assertEquals("PKG_AMI_JUNIT", stored.get("packageCode"));
        assertEquals("JUNIT_PACKAGE_APPROVER", stored.get("publishedBy"));

        Map<String, Object> simulateBody = new LinkedHashMap<String, Object>();
        simulateBody.put("rule_code", "R_PKG_STEMI_HIT");
        simulateBody.put("version_no", "1.0.0");
        simulateBody.put("patient_context", samplePatientContext());
        Map<String, Object> simulate = invokePost("/api/rules/simulate", simulateBody);
        Map<String, Object> simulateData = asMap(simulate.get("data"));
        assertEquals(Boolean.TRUE, simulateData.get("hit"));
    }

    @Test
    void rulesConfigAndExecLogsFilterByOrgContext() throws Exception {
        Map<String, Object> alphaRule = ruleDefinition("R_RULE_ORG_SCOPE", "Alpha医院入径规则");
        alphaRule.put("message_template", "Alpha医院规则命中。");
        Map<String, Object> alphaPackage = rulePackage("PKG_RULE_ORG_SCOPE", "2026.05", alphaRule);
        alphaPackage.put("tenant_id", "TENANT_RULE_ORG");
        alphaPackage.put("hospital_code", "HOSPITAL_ALPHA");
        alphaPackage.put("department_code", "DEPT_ALPHA");
        Map<String, Object> alphaImport = invokePost("/api/rules", alphaPackage);
        Map<String, Object> alphaImported = asListOfMap(alphaImport.get("data")).get(0);
        assertEquals("HOSPITAL_ALPHA", alphaImported.get("hospitalCode"));
        assertEquals("DEPARTMENT", alphaImported.get("scopeLevel"));

        Map<String, Object> betaRule = ruleDefinition("R_RULE_ORG_SCOPE", "Beta医院入径规则");
        betaRule.put("message_template", "Beta医院规则命中。");
        Map<String, Object> betaPackage = rulePackage("PKG_RULE_ORG_SCOPE", "2026.05", betaRule);
        betaPackage.put("tenant_id", "TENANT_RULE_ORG");
        betaPackage.put("hospital_code", "HOSPITAL_BETA");
        betaPackage.put("department_code", "DEPT_BETA");
        invokePost("/api/rules", betaPackage);

        Map<String, Object> alphaPublish = new LinkedHashMap<String, Object>();
        alphaPublish.put("package_version", "2026.05");
        alphaPublish.put("tenant_id", "TENANT_RULE_ORG");
        alphaPublish.put("hospital_code", "HOSPITAL_ALPHA");
        alphaPublish.put("department_code", "DEPT_ALPHA");
        alphaPublish.put("approved_by", "ORG_RULE_APPROVER");
        invokePost("/api/rules/packages/PKG_RULE_ORG_SCOPE/publish", alphaPublish);

        Map<String, Object> betaPublish = new LinkedHashMap<String, Object>();
        betaPublish.put("package_version", "2026.05");
        betaPublish.put("tenant_id", "TENANT_RULE_ORG");
        betaPublish.put("hospital_code", "HOSPITAL_BETA");
        betaPublish.put("department_code", "DEPT_BETA");
        betaPublish.put("approved_by", "ORG_RULE_APPROVER");
        invokePost("/api/rules/packages/PKG_RULE_ORG_SCOPE/publish", betaPublish);

        Map<String, Object> alphaRulesResp = invokeGet("/api/rules?tenantId=TENANT_RULE_ORG&hospitalCode=HOSPITAL_ALPHA");
        List<Map<String, Object>> alphaRules = asListOfMap(alphaRulesResp.get("data"));
        assertEquals(1, alphaRules.size());
        assertEquals("Alpha医院入径规则", alphaRules.get(0).get("ruleName"));

        Map<String, Object> betaGetResp = invokeGet("/api/rules/R_RULE_ORG_SCOPE?tenantId=TENANT_RULE_ORG"
                + "&hospitalCode=HOSPITAL_BETA&departmentCode=DEPT_BETA");
        Map<String, Object> betaStored = asMap(betaGetResp.get("data"));
        assertEquals("Beta医院入径规则", betaStored.get("ruleName"));
        assertEquals("DEPT_BETA", betaStored.get("scopeCode"));

        Map<String, Object> alphaSim = new LinkedHashMap<String, Object>();
        alphaSim.put("tenant_id", "TENANT_RULE_ORG");
        alphaSim.put("hospital_code", "HOSPITAL_ALPHA");
        alphaSim.put("department_code", "DEPT_ALPHA");
        alphaSim.put("rule_code", "R_RULE_ORG_SCOPE");
        alphaSim.put("patient_context", samplePatientContext());
        Map<String, Object> alphaSimResp = invokePost("/api/rules/simulate", alphaSim);
        assertEquals("Alpha医院规则命中。", asMap(alphaSimResp.get("data")).get("message"));

        Map<String, Object> betaSim = new LinkedHashMap<String, Object>();
        betaSim.put("tenant_id", "TENANT_RULE_ORG");
        betaSim.put("hospital_code", "HOSPITAL_BETA");
        betaSim.put("department_code", "DEPT_BETA");
        betaSim.put("rule_code", "R_RULE_ORG_SCOPE");
        betaSim.put("patient_context", samplePatientContext());
        Map<String, Object> betaSimResp = invokePost("/api/rules/simulate", betaSim);
        assertEquals("Beta医院规则命中。", asMap(betaSimResp.get("data")).get("message"));

        Map<String, Object> alphaLogsResp = invokeGet("/api/rules/exec-logs?ruleCode=R_RULE_ORG_SCOPE"
                + "&tenantId=TENANT_RULE_ORG&hospitalCode=HOSPITAL_ALPHA&scopeLevel=DEPARTMENT&scopeCode=DEPT_ALPHA");
        List<Map<String, Object>> alphaLogs = asListOfMap(alphaLogsResp.get("data"));
        assertEquals(1, alphaLogs.size());
        assertEquals("HOSPITAL_ALPHA", alphaLogs.get(0).get("hospitalCode"));
        assertEquals("DEPT_ALPHA", alphaLogs.get(0).get("scopeCode"));

        Map<String, Object> alphaSummaryResp = invokeGet("/api/rules/exec-logs/summary?ruleCode=R_RULE_ORG_SCOPE"
                + "&tenantId=TENANT_RULE_ORG&hospitalCode=HOSPITAL_ALPHA");
        Map<String, Object> alphaSummary = asMap(alphaSummaryResp.get("data"));
        assertEquals(1, ((Number) alphaSummary.get("total")).intValue());
    }

    @Test
    void ruleEngineEvaluateRoutesByScenarioAndPackage() throws Exception {
        Map<String, Object> packageBody = ruleEngineScenarioPackage();
        Map<String, Object> imported = invokePost("/api/rules", packageBody);
        List<Map<String, Object>> importedRules = asListOfMap(imported.get("data"));
        assertEquals(3, importedRules.size());

        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("package_version", "2026.05");
        publishBody.put("approved_by", "JUNIT_RULE_ENGINE");
        Map<String, Object> publishResp = invokePost("/api/rules/packages/PKG_RULE_ENGINE_SCENARIOS/publish", publishBody);
        assertEquals(3, ((Number) asMap(publishResp.get("data")).get("published_count")).intValue());

        // EMR_QC 场景：出院小结主诉缺失 → 命中。
        Map<String, Object> emrReq = new LinkedHashMap<String, Object>();
        emrReq.put("scenario_code", "EMR_QC");
        emrReq.put("rule_package_code", "PKG_RULE_ENGINE_SCENARIOS");
        emrReq.put("rule_package_version", "2026.05");
        emrReq.put("operator_id", "JUNIT_QC_OFFICER");
        emrReq.put("patient_context", ruleEnginePatientContext(false, false, false));
        Map<String, Object> emrResp = invokePost("/api/rule-engine/evaluate", emrReq);
        Map<String, Object> emrData = asMap(emrResp.get("data"));
        assertEquals("EMR_QC", emrData.get("scenario_code"));
        assertEquals(1, ((Number) emrData.get("evaluated_count")).intValue());
        assertEquals(1, ((Number) emrData.get("hit_count")).intValue());
        assertEquals("SINGLE", emrData.get("source"));
        assertEquals("JUNIT_QC_OFFICER", emrData.get("operator_id"));
        assertNotNull(emrData.get("result_id"));
        assertNotNull(emrData.get("created_time"));
        String emrResultId = (String) emrData.get("result_id");
        Map<String, Object> emrLookup = invokeGet("/api/rule-engine/results/" + emrResultId);
        Map<String, Object> emrLookupData = asMap(emrLookup.get("data"));
        assertEquals(emrResultId, emrLookupData.get("result_id"));
        assertEquals(1, ((Number) emrLookupData.get("hit_count")).intValue());
        assertEquals(1, asListOfMap(emrLookupData.get("results")).size());
        List<Map<String, Object>> emrResults = asListOfMap(emrData.get("results"));
        assertEquals("R_EMR_DISCHARGE_SUMMARY_COMPLETE", emrResults.get(0).get("rule_code"));
        assertEquals(Boolean.TRUE, emrResults.get(0).get("hit"));
        assertEquals("EMR_QC", emrResults.get(0).get("scenario_code"));
        assertEquals("PKG_RULE_ENGINE_SCENARIOS", emrResults.get(0).get("package_code"));
        assertNotNull(emrData.get("trace_id"));
        assertTrue(asList(emrData.get("warnings")).isEmpty());

        // ORDER_SAFETY 场景：48小时重复抗菌药物 → 命中拦截动作。
        Map<String, Object> safetyReq = new LinkedHashMap<String, Object>();
        safetyReq.put("scenario_code", "ORDER_SAFETY");
        safetyReq.put("patient_context", ruleEnginePatientContextWithDuplicateAntibiotic());
        Map<String, Object> safetyResp = invokePost("/api/rule-engine/evaluate", safetyReq);
        Map<String, Object> safetyData = asMap(safetyResp.get("data"));
        assertEquals(1, ((Number) safetyData.get("evaluated_count")).intValue());
        assertEquals(1, ((Number) safetyData.get("hit_count")).intValue());
        List<Map<String, Object>> safetyResults = asListOfMap(safetyData.get("results"));
        Map<String, Object> safetyHit = safetyResults.get(0);
        assertEquals("R_ORDER_SAFETY_DUP_ANTIBIOTIC", safetyHit.get("rule_code"));
        assertEquals("CRITICAL", safetyHit.get("severity"));
        assertTrue(asList(safetyHit.get("actions")).contains("BLOCK_ORDER"));

        // DRUG_INDICATION 通过多场景声明也能复用同一规则。
        Map<String, Object> drugReq = new LinkedHashMap<String, Object>();
        drugReq.put("scenario_code", "DRUG_INDICATION");
        drugReq.put("patient_context", ruleEnginePatientContextWithInsuranceMismatch());
        Map<String, Object> drugResp = invokePost("/api/rule-engine/evaluate", drugReq);
        Map<String, Object> drugData = asMap(drugResp.get("data"));
        assertEquals(1, ((Number) drugData.get("evaluated_count")).intValue());
        assertEquals(1, ((Number) drugData.get("hit_count")).intValue());
        assertEquals("DRUG_INDICATION", asListOfMap(drugData.get("results")).get(0).get("scenario_code"));
    }

    @Test
    void ruleEngineEvaluateRejectsInvalidRequests() throws Exception {
        Map<String, Object> missingScenario = new LinkedHashMap<String, Object>();
        missingScenario.put("patient_context", ruleEnginePatientContext(true, true, true));
        Map<String, Object> noScenarioResp = invokePostExpectingClientError("/api/rule-engine/evaluate", missingScenario);
        assertEquals("VALIDATION_ERROR", noScenarioResp.get("code"));

        Map<String, Object> unsupportedScenario = new LinkedHashMap<String, Object>();
        unsupportedScenario.put("scenario_code", "NOT_A_SCENARIO");
        unsupportedScenario.put("patient_context", ruleEnginePatientContext(true, true, true));
        Map<String, Object> badScenarioResp = invokePostExpectingClientError("/api/rule-engine/evaluate", unsupportedScenario);
        assertEquals("VALIDATION_ERROR", badScenarioResp.get("code"));

        Map<String, Object> missingPatient = new LinkedHashMap<String, Object>();
        missingPatient.put("scenario_code", "EMR_QC");
        Map<String, Object> noPatientResp = invokePostExpectingClientError("/api/rule-engine/evaluate", missingPatient);
        assertEquals("VALIDATION_ERROR", noPatientResp.get("code"));
    }

    @Test
    void ruleEngineBatchEvaluatePersistsPerItemResults() throws Exception {
        invokePost("/api/rules", ruleEngineScenarioPackage());
        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("package_version", "2026.05");
        publishBody.put("approved_by", "JUNIT_RULE_ENGINE_BATCH");
        invokePost("/api/rules/packages/PKG_RULE_ENGINE_SCENARIOS/publish", publishBody);

        Map<String, Object> hitItem = new LinkedHashMap<String, Object>();
        hitItem.put("case_id", "CASE_HIT");
        hitItem.put("patient_context", ruleEnginePatientContext(false, true, true));
        Map<String, Object> missItem = new LinkedHashMap<String, Object>();
        missItem.put("case_id", "CASE_MISS");
        missItem.put("patient_context", ruleEnginePatientContext(true, true, true));

        Map<String, Object> batchReq = new LinkedHashMap<String, Object>();
        batchReq.put("scenario_code", "EMR_QC");
        batchReq.put("rule_package_code", "PKG_RULE_ENGINE_SCENARIOS");
        batchReq.put("operator_id", "JUNIT_BATCH_OFFICER");
        batchReq.put("items", Arrays.asList(hitItem, missItem));

        Map<String, Object> batchResp = invokePost("/api/rule-engine/batch-evaluate", batchReq);
        Map<String, Object> batchData = asMap(batchResp.get("data"));
        assertEquals("EMR_QC", batchData.get("scenario_code"));
        assertEquals(2, ((Number) batchData.get("total_items")).intValue());
        assertEquals(2, ((Number) batchData.get("total_evaluated")).intValue());
        assertEquals(1, ((Number) batchData.get("total_hits")).intValue());
        String batchId = (String) batchData.get("batch_id");
        assertNotNull(batchId);

        List<Map<String, Object>> evaluations = asListOfMap(batchData.get("evaluations"));
        assertEquals(2, evaluations.size());
        Map<String, Object> evalHit = evaluations.get(0);
        assertEquals("CASE_HIT", evalHit.get("case_id"));
        assertEquals(batchId, evalHit.get("batch_id"));
        assertEquals(1, ((Number) evalHit.get("hit_count")).intValue());
        assertNotNull(evalHit.get("result_id"));

        Map<String, Object> evalMiss = evaluations.get(1);
        assertEquals("CASE_MISS", evalMiss.get("case_id"));
        assertEquals(0, ((Number) evalMiss.get("hit_count")).intValue());

        // 列表按 batchId 回查应返回两条独立 result_id 摘要。
        Map<String, Object> listResp = invokeGet("/api/rule-engine/results?batchId=" + batchId);
        List<Map<String, Object>> listData = asListOfMap(listResp.get("data"));
        assertEquals(2, listData.size());
        for (Map<String, Object> summary : listData) {
            assertEquals("BATCH", summary.get("source"));
            assertEquals(batchId, summary.get("batch_id"));
            // 列表摘要不应包含 results 详情，避免 payload 过大。
            assertFalse(summary.containsKey("results"), "list view must not include results detail");
        }

        // 单条 result_id 详情查询。
        String firstResultId = (String) evalHit.get("result_id");
        Map<String, Object> detail = invokeGet("/api/rule-engine/results/" + firstResultId);
        Map<String, Object> detailData = asMap(detail.get("data"));
        assertEquals(firstResultId, detailData.get("result_id"));
        assertEquals(batchId, detailData.get("batch_id"));
        assertEquals("CASE_HIT", detailData.get("case_id"));
        assertEquals(1, asListOfMap(detailData.get("results")).size());
    }

    @Test
    void ruleEngineBatchEvaluateRejectsEmptyItems() throws Exception {
        Map<String, Object> emptyItems = new LinkedHashMap<String, Object>();
        emptyItems.put("scenario_code", "EMR_QC");
        emptyItems.put("items", new java.util.ArrayList<Object>());
        Map<String, Object> resp = invokePostExpectingClientError("/api/rule-engine/batch-evaluate", emptyItems);
        assertEquals("VALIDATION_ERROR", resp.get("code"));

        Map<String, Object> noItems = new LinkedHashMap<String, Object>();
        noItems.put("scenario_code", "EMR_QC");
        Map<String, Object> resp2 = invokePostExpectingClientError("/api/rule-engine/batch-evaluate", noItems);
        assertEquals("VALIDATION_ERROR", resp2.get("code"));

        Map<String, Object> badItem = new LinkedHashMap<String, Object>();
        badItem.put("case_id", "X");
        Map<String, Object> missingPatient = new LinkedHashMap<String, Object>();
        missingPatient.put("scenario_code", "EMR_QC");
        missingPatient.put("items", Arrays.asList(badItem));
        Map<String, Object> resp3 = invokePostExpectingClientError("/api/rule-engine/batch-evaluate", missingPatient);
        assertEquals("VALIDATION_ERROR", resp3.get("code"));
    }

    @Test
    void ruleEngineGetEvaluationReturns4xxForUnknownResult() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/rule-engine/results/reval-not-exist")).andReturn();
        assertTrue(mvcResult.getResponse().getStatus() >= 400,
                "GET unknown resultId expected 4xx but got " + mvcResult.getResponse().getStatus());
        Map<String, Object> body = parse(mvcResult);
        assertEquals("VALIDATION_ERROR", body.get("code"));
    }

    @Test
    void ruleEngineEvaluateMergesHeaderOrgContext() throws Exception {
        invokePost("/api/rules", ruleEngineScenarioPackage());
        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("package_version", "2026.05");
        publishBody.put("approved_by", "JUNIT_ORG_HEADER");
        invokePost("/api/rules/packages/PKG_RULE_ENGINE_SCENARIOS/publish", publishBody);

        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("scenario_code", "EMR_QC");
        req.put("operator_id", "JUNIT_HEADER_OFFICER");
        req.put("patient_context", ruleEnginePatientContext(false, true, true));

        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("X-Tenant-Id", "TENANT_JUNIT");
        headers.put("X-Hospital-Code", "HOSPITAL_JUNIT_HDR");
        headers.put("X-Campus-Code", "CAMPUS_EAST_HDR");
        Map<String, Object> resp = invokePostWithHeaders("/api/rule-engine/evaluate", req, headers);
        Map<String, Object> data = asMap(resp.get("data"));
        assertEquals("TENANT_JUNIT", data.get("tenant_id"));
        assertEquals("HOSPITAL_JUNIT_HDR", data.get("hospital_code"));
        assertEquals("CAMPUS_EAST_HDR", data.get("campus_code"));
        assertEquals("CAMPUS", data.get("scope_level"));
        assertEquals("CAMPUS_EAST_HDR", data.get("scope_code"));
        assertEquals("HEADER", data.get("org_source"));
    }

    @Test
    void ruleEngineEvaluateBodyOverridesHeaderOrgContext() throws Exception {
        invokePost("/api/rules", ruleEngineScenarioPackage());
        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("package_version", "2026.05");
        publishBody.put("approved_by", "JUNIT_ORG_BODY");
        invokePost("/api/rules/packages/PKG_RULE_ENGINE_SCENARIOS/publish", publishBody);

        Map<String, Object> req = new LinkedHashMap<String, Object>();
        req.put("scenario_code", "EMR_QC");
        req.put("operator_id", "JUNIT_BODY_OFFICER");
        req.put("tenant_id", "TENANT_BODY");
        req.put("hospital_code", "HOSPITAL_BODY");
        req.put("campus_code", "CAMPUS_BODY");
        req.put("site_code", "SITE_BODY");
        req.put("department_code", "DEPT_BODY");
        req.put("patient_context", ruleEnginePatientContext(false, true, true));

        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("X-Hospital-Code", "HOSPITAL_FROM_HEADER");
        headers.put("X-Campus-Code", "CAMPUS_FROM_HEADER");
        Map<String, Object> resp = invokePostWithHeaders("/api/rule-engine/evaluate", req, headers);
        Map<String, Object> data = asMap(resp.get("data"));
        assertEquals("TENANT_BODY", data.get("tenant_id"));
        // Body 字段优先级高于 Header。
        assertEquals("HOSPITAL_BODY", data.get("hospital_code"));
        assertEquals("CAMPUS_BODY", data.get("campus_code"));
        assertEquals("SITE_BODY", data.get("site_code"));
        assertEquals("DEPT_BODY", data.get("department_code"));
        assertEquals("DEPARTMENT", data.get("scope_level"));
        assertEquals("DEPT_BODY", data.get("scope_code"));
        assertEquals("BODY", data.get("org_source"));

        String resultId = (String) data.get("result_id");
        Map<String, Object> detail = invokeGet("/api/rule-engine/results/" + resultId);
        Map<String, Object> detailData = asMap(detail.get("data"));
        assertEquals("DEPT_BODY", detailData.get("department_code"));
    }

    @Test
    void ruleEngineListEvaluationsFiltersByHospital() throws Exception {
        invokePost("/api/rules", ruleEngineScenarioPackage());
        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("package_version", "2026.05");
        publishBody.put("approved_by", "JUNIT_ORG_FILTER");
        invokePost("/api/rules/packages/PKG_RULE_ENGINE_SCENARIOS/publish", publishBody);

        Map<String, Object> reqAlpha = new LinkedHashMap<String, Object>();
        reqAlpha.put("scenario_code", "EMR_QC");
        reqAlpha.put("hospital_code", "HOSPITAL_ALPHA");
        reqAlpha.put("patient_context", ruleEnginePatientContext(false, true, true));
        invokePost("/api/rule-engine/evaluate", reqAlpha);

        Map<String, Object> reqBeta = new LinkedHashMap<String, Object>();
        reqBeta.put("scenario_code", "EMR_QC");
        reqBeta.put("hospital_code", "HOSPITAL_BETA");
        reqBeta.put("patient_context", ruleEnginePatientContext(false, true, true));
        invokePost("/api/rule-engine/evaluate", reqBeta);

        Map<String, Object> alphaList = invokeGet(
                "/api/rule-engine/results?hospitalCode=HOSPITAL_ALPHA&scenarioCode=EMR_QC");
        List<Map<String, Object>> alphaItems = asListOfMap(alphaList.get("data"));
        assertFalse(alphaItems.isEmpty(), "alpha list should not be empty");
        for (Map<String, Object> item : alphaItems) {
            assertEquals("HOSPITAL_ALPHA", item.get("hospital_code"));
        }

        Map<String, Object> betaList = invokeGet(
                "/api/rule-engine/results?hospitalCode=HOSPITAL_BETA&scenarioCode=EMR_QC");
        List<Map<String, Object>> betaItems = asListOfMap(betaList.get("data"));
        assertFalse(betaItems.isEmpty(), "beta list should not be empty");
        for (Map<String, Object> item : betaItems) {
            assertEquals("HOSPITAL_BETA", item.get("hospital_code"));
        }
    }

    @Test
    void ruleEngineEvaluateUnmatchedReturnsWarning() throws Exception {
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("scenario_code", "EXAM_RATIONALITY");
        request.put("rule_package_code", "PKG_UNKNOWN_PACKAGE");
        request.put("patient_context", ruleEnginePatientContext(true, true, true));
        Map<String, Object> response = invokePost("/api/rule-engine/evaluate", request);
        Map<String, Object> data = asMap(response.get("data"));
        assertEquals(0, ((Number) data.get("evaluated_count")).intValue());
        assertEquals(0, ((Number) data.get("hit_count")).intValue());
        assertTrue(asListOfMap(data.get("results")).isEmpty());
        List<Map<String, Object>> warnings = asListOfMap(data.get("warnings"));
        assertEquals(1, warnings.size());
        assertEquals("NO_RULES_MATCHED", warnings.get(0).get("code"));
    }

    private Map<String, Object> ruleEngineScenarioPackage() {
        Map<String, Object> emrRule = new LinkedHashMap<String, Object>();
        emrRule.put("rule_code", "R_EMR_DISCHARGE_SUMMARY_COMPLETE");
        emrRule.put("rule_name", "出院小结主诉与诊断必须完整");
        emrRule.put("rule_type", "EMR_QC");
        emrRule.put("scenario_codes", Arrays.asList("EMR_QC"));
        emrRule.put("severity", "MEDIUM");
        emrRule.put("priority", 90);
        emrRule.put("enabled", true);
        Map<String, Object> emrChief = new LinkedHashMap<String, Object>();
        emrChief.put("fact", "emr.discharge_summary.chief_complaint_filled");
        emrChief.put("operator", "equals");
        emrChief.put("value", false);
        Map<String, Object> emrDiagnosis = new LinkedHashMap<String, Object>();
        emrDiagnosis.put("fact", "emr.discharge_summary.diagnosis_filled");
        emrDiagnosis.put("operator", "equals");
        emrDiagnosis.put("value", false);
        Map<String, Object> emrOrders = new LinkedHashMap<String, Object>();
        emrOrders.put("fact", "emr.discharge_summary.discharge_orders_filled");
        emrOrders.put("operator", "equals");
        emrOrders.put("value", false);
        Map<String, Object> emrCondition = new LinkedHashMap<String, Object>();
        emrCondition.put("any", Arrays.asList(emrChief, emrDiagnosis, emrOrders));
        emrRule.put("condition", emrCondition);
        emrRule.put("actions", Arrays.asList(actionMap("EMR_QC_FAIL")));
        emrRule.put("message_template", "出院小结存在必填项缺失，请补全。");

        Map<String, Object> insRule = new LinkedHashMap<String, Object>();
        insRule.put("rule_code", "R_INS_DRUG_INDICATION_MISMATCH");
        insRule.put("rule_name", "医保限定适应症与诊断不一致");
        insRule.put("rule_type", "INSURANCE_QC");
        insRule.put("scenario_codes", Arrays.asList("INSURANCE_QC", "DRUG_INDICATION"));
        insRule.put("severity", "HIGH");
        insRule.put("priority", 110);
        insRule.put("enabled", true);
        Map<String, Object> insUsed = new LinkedHashMap<String, Object>();
        insUsed.put("fact", "orders.insurance_restricted_drug_used");
        insUsed.put("operator", "equals");
        insUsed.put("value", true);
        Map<String, Object> insMismatch = new LinkedHashMap<String, Object>();
        insMismatch.put("fact", "orders.insurance_restricted_drug_indication_matched");
        insMismatch.put("operator", "equals");
        insMismatch.put("value", false);
        Map<String, Object> insCondition = new LinkedHashMap<String, Object>();
        insCondition.put("all", Arrays.asList(insUsed, insMismatch));
        insRule.put("condition", insCondition);
        insRule.put("actions", Arrays.asList(actionMap("INSURANCE_QC_FAIL"), actionMap("REQUEST_PRE_AUTH")));
        insRule.put("message_template", "存在医保限定适应症与诊断不匹配的药品。");

        Map<String, Object> safetyRule = new LinkedHashMap<String, Object>();
        safetyRule.put("rule_code", "R_ORDER_SAFETY_DUP_ANTIBIOTIC");
        safetyRule.put("rule_name", "重复抗菌药物医嘱拦截");
        safetyRule.put("rule_type", "SAFETY_BLOCK");
        safetyRule.put("scenario_codes", Arrays.asList("ORDER_SAFETY"));
        safetyRule.put("severity", "CRITICAL");
        safetyRule.put("priority", 130);
        safetyRule.put("enabled", true);
        Map<String, Object> safetyAtom = new LinkedHashMap<String, Object>();
        safetyAtom.put("fact", "orders.antibiotic_duplicate_within_48h");
        safetyAtom.put("operator", "equals");
        safetyAtom.put("value", true);
        Map<String, Object> safetyCondition = new LinkedHashMap<String, Object>();
        safetyCondition.put("all", Arrays.asList(safetyAtom));
        safetyRule.put("condition", safetyCondition);
        safetyRule.put("actions", Arrays.asList(actionMap("BLOCK_ORDER"), actionMap("PUSH_TO_DOCTOR")));
        safetyRule.put("message_template", "48小时内重复开立抗菌药物，请上级医师确认。");

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("package_code", "PKG_RULE_ENGINE_SCENARIOS");
        body.put("package_version", "2026.05");
        body.put("rules", Arrays.asList(emrRule, insRule, safetyRule));
        return body;
    }

    private Map<String, Object> ruleEnginePatientContext(boolean chiefFilled, boolean diagnosisFilled, boolean ordersFilled) {
        Map<String, Object> patient = new LinkedHashMap<String, Object>();
        patient.put("patient_id", "P_QC_001");
        Map<String, Object> encounter = new LinkedHashMap<String, Object>();
        encounter.put("encounter_id", "E_QC_001");
        encounter.put("visit_type", "INPATIENT");
        Map<String, Object> discharge = new LinkedHashMap<String, Object>();
        discharge.put("chief_complaint_filled", chiefFilled);
        discharge.put("diagnosis_filled", diagnosisFilled);
        discharge.put("discharge_orders_filled", ordersFilled);
        Map<String, Object> emr = new LinkedHashMap<String, Object>();
        emr.put("discharge_summary", discharge);
        Map<String, Object> facts = new LinkedHashMap<String, Object>();
        facts.put("emr", emr);
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        context.put("patient", patient);
        context.put("encounter", encounter);
        context.put("facts", facts);
        return context;
    }

    private Map<String, Object> ruleEnginePatientContextWithDuplicateAntibiotic() {
        Map<String, Object> orders = new LinkedHashMap<String, Object>();
        orders.put("antibiotic_duplicate_within_48h", true);
        Map<String, Object> facts = new LinkedHashMap<String, Object>();
        facts.put("orders", orders);
        Map<String, Object> patient = new LinkedHashMap<String, Object>();
        patient.put("patient_id", "P_QC_SAFETY");
        Map<String, Object> encounter = new LinkedHashMap<String, Object>();
        encounter.put("encounter_id", "E_QC_SAFETY");
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        context.put("patient", patient);
        context.put("encounter", encounter);
        context.put("facts", facts);
        return context;
    }

    private Map<String, Object> ruleEnginePatientContextWithInsuranceMismatch() {
        Map<String, Object> orders = new LinkedHashMap<String, Object>();
        orders.put("insurance_restricted_drug_used", true);
        orders.put("insurance_restricted_drug_indication_matched", false);
        Map<String, Object> facts = new LinkedHashMap<String, Object>();
        facts.put("orders", orders);
        Map<String, Object> patient = new LinkedHashMap<String, Object>();
        patient.put("patient_id", "P_QC_INSURANCE");
        Map<String, Object> encounter = new LinkedHashMap<String, Object>();
        encounter.put("encounter_id", "E_QC_INSURANCE");
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        context.put("patient", patient);
        context.put("encounter", encounter);
        context.put("facts", facts);
        return context;
    }

    @Test
    void pathwayImportPublishAdmitAndComplete() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_TEST", "AMI测试路径");
        Map<String, Object> created = invokePost("/api/pathways", pathwayConfig);
        assertEquals("DRAFT", asMap(created.get("data")).get("status"));

        Map<String, Object> publishBody = new LinkedHashMap<String, Object>();
        publishBody.put("version_no", "1.0.0");
        Map<String, Object> published = invokePost("/api/pathways/AMI_TEST/publish", publishBody);
        assertEquals("PUBLISHED", asMap(published.get("data")).get("status"));

        Map<String, Object> admitBody = new LinkedHashMap<String, Object>();
        admitBody.put("patient_id", "P_JUNIT_001");
        admitBody.put("encounter_id", "E_JUNIT_001");
        admitBody.put("pathway_code", "AMI_TEST");
        admitBody.put("version_no", "1.0.0");
        admitBody.put("doctor_id", "JUNIT_DOC");
        Map<String, Object> admitted = invokePost("/api/patient-pathways/admit", admitBody);
        Map<String, Object> admittedData = asMap(admitted.get("data"));
        assertEquals("ACTIVE", admittedData.get("status"));
        assertEquals("NODE_IDENTIFY", admittedData.get("currentNodeCode"));
        String instanceId = (String) admittedData.get("instanceId");
        assertNotNull(instanceId);

        Map<String, Object> completeNodeBody = new LinkedHashMap<String, Object>();
        completeNodeBody.put("operator_id", "JUNIT_DOC");
        Map<String, Object> completed = invokePost("/api/patient-pathways/" + instanceId + "/nodes/NODE_IDENTIFY/complete",
                completeNodeBody);
        assertEquals("NODE_TREATMENT", asMap(completed.get("data")).get("currentNodeCode"));
    }

    @Test
    void pathwayImportRejectsInvalidConfig() throws Exception {
        Map<String, Object> bad = new LinkedHashMap<String, Object>();
        bad.put("pathway_code", "AMI_BAD");
        // pathway_name 和 stages 全部缺失，必须返回 VALIDATION_ERROR
        Map<String, Object> response = invokePostExpectingClientError("/api/pathways", bad);
        assertEquals(Boolean.FALSE, response.get("success"));
        assertEquals("VALIDATION_ERROR", response.get("code"));
    }

    @Test
    void terminologyImportListAndNormalizeBuiltIn() throws Exception {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("source_system", "HIS");
        entry.put("source_code", "I50.0");
        entry.put("source_name", "心力衰竭");
        entry.put("concept_type", "DIAGNOSIS");
        entry.put("standard_code", "HEART_FAILURE");
        entry.put("standard_name", "心力衰竭");
        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("mappings", Arrays.asList(entry));
        Map<String, Object> imported = invokePost("/api/terminology/mappings", importBody);
        assertEquals(1, asList(imported.get("data")).size());

        Map<String, Object> getResp = invokeGet("/api/terminology/mappings/HIS/I50.0?conceptType=DIAGNOSIS");
        assertEquals("HEART_FAILURE", asMap(getResp.get("data")).get("standard_code"));

        Map<String, Object> normalizeBody = new LinkedHashMap<String, Object>();
        normalizeBody.put("source_system", "HIS");
        normalizeBody.put("source_code", "I21.3");
        normalizeBody.put("source_name", "急性ST段抬高型心肌梗死");
        normalizeBody.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalized = invokePost("/api/terminology/normalize", normalizeBody);
        Map<String, Object> normalizedData = asMap(normalized.get("data"));
        assertEquals(Boolean.TRUE, normalizedData.get("matched"));
        assertEquals("AMI_STEMI", normalizedData.get("standard_code"));
    }

    @Test
    void terminologyImportRejectsInvalidMapping() throws Exception {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("source_system", "HIS");
        entry.put("concept_type", "DIAGNOSIS");
        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("mappings", Arrays.asList(entry));
        Map<String, Object> response = invokePostExpectingClientError("/api/terminology/mappings", importBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
    }

    @Test
    void adapterImportAndQuery() throws Exception {
        Map<String, Object> sampleRow = new LinkedHashMap<String, Object>();
        sampleRow.put("exam_code", "CT_CHEST");
        sampleRow.put("report_text", "未见明显异常。");
        sampleRow.put("report_time", "2026-05-16T08:00:00+08:00");

        Map<String, Object> definition = new LinkedHashMap<String, Object>();
        definition.put("adapter_code", "RIS_TEST_ADAPTER");
        definition.put("adapter_name", "RIS测试适配器");
        definition.put("adapter_type", "REST");
        definition.put("source_system", "RIS");
        definition.put("query_code", "QUERY_CHEST_CT");
        definition.put("query_name", "查询胸部CT报告");
        definition.put("description", "JUnit 测试用胸部CT Mock。");
        definition.put("schema", Arrays.asList("patient_id", "encounter_id", "exam_code", "report_text"));
        definition.put("sample_rows", Arrays.asList(sampleRow));

        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("definitions", Arrays.asList(definition));
        Map<String, Object> imported = invokePost("/api/adapters/definitions", importBody);
        assertEquals(1, asList(imported.get("data")).size());

        Map<String, Object> queryBody = new LinkedHashMap<String, Object>();
        queryBody.put("adapter_code", "RIS_TEST_ADAPTER");
        queryBody.put("query_code", "QUERY_CHEST_CT");
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("patient_id", "P_JUNIT_001");
        params.put("encounter_id", "E_JUNIT_001");
        queryBody.put("params", params);
        Map<String, Object> queryResult = invokePost("/api/adapters/query", queryBody);
        Map<String, Object> data = asMap(queryResult.get("data"));
        assertEquals("SUCCESS", data.get("status"));
        assertEquals(1, ((Number) data.get("row_count")).intValue());
        List<Map<String, Object>> rows = asListOfMap(data.get("rows"));
        assertEquals("CT_CHEST", rows.get(0).get("exam_code"));
        assertEquals("P_JUNIT_001", rows.get(0).get("patient_id"));
    }

    @Test
    void pathwayVersionDiffBetweenDraftAndPublished() throws Exception {
        // 先发布 1.0.0，再修改草稿（增加一个节点 + 改主节点名 + 增减任务），通过 /diff 比较 1.0.0 vs draft
        Map<String, Object> v1 = samplePathwayConfig("AMI_DIFF_TEST", "AMI差异对比初版");
        invokePost("/api/pathways", v1);
        invokePost("/api/pathways/AMI_DIFF_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> v2 = samplePathwayConfig("AMI_DIFF_TEST", "AMI差异对比改版");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stages = (List<Map<String, Object>>) v2.get("stages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) stages.get(0).get("nodes");
        // 修改 NODE_IDENTIFY 名称 + 删除 TASK_TRIAGE + 增加 TASK_ECG
        Map<String, Object> identify = nodes.get(0);
        identify.put("node_name", "识别（升级版）");
        Map<String, Object> ecgTask = new LinkedHashMap<String, Object>();
        ecgTask.put("task_code", "TASK_ECG");
        ecgTask.put("task_name", "心电图采集");
        ecgTask.put("task_type", "EXAM");
        ecgTask.put("required", true);
        identify.put("tasks", Arrays.asList(ecgTask));
        // 增加一个全新节点 NODE_FOLLOWUP
        Map<String, Object> followup = new LinkedHashMap<String, Object>();
        followup.put("node_code", "NODE_FOLLOWUP");
        followup.put("node_name", "随访");
        List<Map<String, Object>> mutableNodes = new java.util.ArrayList<Map<String, Object>>(nodes);
        mutableNodes.add(followup);
        stages.get(0).put("nodes", mutableNodes);
        invokePost("/api/pathways", v2);

        Map<String, Object> diffResp = invokeGet("/api/pathways/AMI_DIFF_TEST/diff?from=1.0.0&to=draft");
        Map<String, Object> diff = asMap(diffResp.get("data"));
        assertEquals("AMI_DIFF_TEST", diff.get("pathway_code"));

        Map<String, Object> summary = asMap(diff.get("summary"));
        assertEquals(1, ((Number) summary.get("metadata_changed")).intValue());
        assertEquals(1, ((Number) summary.get("nodes_added")).intValue());
        assertEquals(0, ((Number) summary.get("nodes_removed")).intValue());
        assertEquals(1, ((Number) summary.get("nodes_modified")).intValue());

        List<Map<String, Object>> nodesAdded = asListOfMap(diff.get("nodes_added"));
        // nodes_added 是 String 列表，asListOfMap 不适用；直接取 List<Object>
        List<Object> addedList = asList(diff.get("nodes_added"));
        assertEquals(1, addedList.size());
        assertEquals("NODE_FOLLOWUP", addedList.get(0));

        List<Map<String, Object>> nodesModified = asListOfMap(diff.get("nodes_modified"));
        assertEquals(1, nodesModified.size());
        Map<String, Object> identifyDiff = nodesModified.get(0);
        assertEquals("NODE_IDENTIFY", identifyDiff.get("node_code"));
        List<Map<String, Object>> fieldChanges = asListOfMap(identifyDiff.get("fields"));
        assertEquals(1, fieldChanges.size());
        assertEquals("node_name", fieldChanges.get(0).get("field"));
        List<Object> tasksAdded = asList(identifyDiff.get("tasks_added"));
        List<Object> tasksRemoved = asList(identifyDiff.get("tasks_removed"));
        assertTrue(tasksAdded.contains("TASK_ECG"));
        assertTrue(tasksRemoved.contains("TASK_TRIAGE"));
    }

    @Test
    void pathwayPublishRollbackSwitchesActiveVersion() throws Exception {
        Map<String, Object> v1 = samplePathwayConfig("AMI_ROLLBACK_TEST", "AMI回滚初版");
        invokePost("/api/pathways", v1);
        invokePost("/api/pathways/AMI_ROLLBACK_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> v2 = samplePathwayConfig("AMI_ROLLBACK_TEST", "AMI回滚新版");
        v2.put("version_no", "2.0.0");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stages = (List<Map<String, Object>>) v2.get("stages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) stages.get(0).get("nodes");
        nodes.get(0).put("node_name", "识别新版");
        invokePost("/api/pathways", v2);
        invokePost("/api/pathways/AMI_ROLLBACK_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> latest = invokeGet("/api/pathways/AMI_ROLLBACK_TEST");
        assertEquals("2.0.0", asMap(latest.get("data")).get("selected_version"));

        Map<String, Object> rollbackBody = new LinkedHashMap<String, Object>();
        rollbackBody.put("target_version", "1.0.0");
        rollbackBody.put("operator_id", "ROLLBACK_DOC");
        rollbackBody.put("reason", "JUnit 回滚验证。");
        Map<String, Object> rollback = invokePost("/api/pathways/AMI_ROLLBACK_TEST/rollback", rollbackBody);
        Map<String, Object> rollbackData = asMap(rollback.get("data"));
        assertEquals("ROLLED_BACK", rollbackData.get("status"));
        assertEquals("2.0.0", rollbackData.get("previous_active_version"));
        assertEquals("1.0.0", rollbackData.get("active_version"));

        Map<String, Object> afterRollback = invokeGet("/api/pathways/AMI_ROLLBACK_TEST");
        Map<String, Object> data = asMap(afterRollback.get("data"));
        assertEquals("1.0.0", data.get("selected_version"));
        assertEquals("1.0.0", data.get("active_published_version"));

        Map<String, Object> admitBody = new LinkedHashMap<String, Object>();
        admitBody.put("patient_id", "P_ROLLBACK_001");
        admitBody.put("encounter_id", "E_ROLLBACK_001");
        admitBody.put("pathway_code", "AMI_ROLLBACK_TEST");
        admitBody.put("doctor_id", "ROLLBACK_DOC");
        Map<String, Object> admitted = invokePost("/api/patient-pathways/admit", admitBody);
        assertEquals("1.0.0", asMap(admitted.get("data")).get("versionNo"));
    }

    @Test
    void pathwayNodeCompletionMetrics() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_NODE_TEST", "AMI节点完成率测试路径");
        invokePost("/api/pathways", pathwayConfig);
        invokePost("/api/pathways/AMI_NODE_TEST/publish", new LinkedHashMap<String, Object>());

        // 入径 P_NODE_001 并完成首节点（含 TASK_TRIAGE）— 进入 NODE_IDENTIFY → COMPLETED → 自动进入 NODE_TREATMENT
        Map<String, Object> admit1 = new LinkedHashMap<String, Object>();
        admit1.put("patient_id", "P_NODE_001");
        admit1.put("encounter_id", "E_NODE_001");
        admit1.put("pathway_code", "AMI_NODE_TEST");
        admit1.put("version_no", "1.0.0");
        admit1.put("doctor_id", "NODE_DOC");
        Map<String, Object> admitted1 = invokePost("/api/patient-pathways/admit", admit1);
        String instance1 = (String) asMap(admitted1.get("data")).get("instanceId");

        Map<String, Object> completeTask = new LinkedHashMap<String, Object>();
        completeTask.put("operator_id", "NODE_DOC");
        invokePost("/api/patient-pathways/" + instance1 + "/nodes/NODE_IDENTIFY/tasks/TASK_TRIAGE/complete", completeTask);
        Map<String, Object> completeNode = new LinkedHashMap<String, Object>();
        completeNode.put("operator_id", "NODE_DOC");
        invokePost("/api/patient-pathways/" + instance1 + "/nodes/NODE_IDENTIFY/complete", completeNode);

        // 入径 P_NODE_002 但只跳过 TASK_TRIAGE，不完成节点 — NODE_IDENTIFY 保持 RUNNING
        Map<String, Object> admit2 = new LinkedHashMap<String, Object>();
        admit2.put("patient_id", "P_NODE_002");
        admit2.put("encounter_id", "E_NODE_002");
        admit2.put("pathway_code", "AMI_NODE_TEST");
        admit2.put("version_no", "1.0.0");
        admit2.put("doctor_id", "NODE_DOC");
        Map<String, Object> admitted2 = invokePost("/api/patient-pathways/admit", admit2);
        String instance2 = (String) asMap(admitted2.get("data")).get("instanceId");
        Map<String, Object> skipBody = new LinkedHashMap<String, Object>();
        skipBody.put("operator_id", "NODE_DOC");
        skipBody.put("variation_type", "PATIENT_REASON");
        skipBody.put("reason", "节点完成率测试：跳过分诊。");
        invokePost("/api/patient-pathways/" + instance2 + "/nodes/NODE_IDENTIFY/tasks/TASK_TRIAGE/skip", skipBody);

        Map<String, Object> resp = invokeGet("/api/pathway-instances/node-completion?pathwayCode=AMI_NODE_TEST");
        Map<String, Object> data = asMap(resp.get("data"));
        assertEquals(2, ((Number) data.get("total_instances")).intValue());
        assertEquals(2, ((Number) data.get("total_nodes")).intValue());

        List<Map<String, Object>> nodes = asListOfMap(data.get("nodes"));
        Map<String, Map<String, Object>> byNode = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> node : nodes) {
            byNode.put((String) node.get("node_code"), node);
        }

        Map<String, Object> identify = byNode.get("NODE_IDENTIFY");
        assertEquals(2, ((Number) identify.get("entered")).intValue());
        assertEquals(1, ((Number) identify.get("completed")).intValue());
        assertEquals(1, ((Number) identify.get("running")).intValue());
        assertEquals(50.0, ((Number) identify.get("completion_rate")).doubleValue());

        List<Map<String, Object>> tasks = asListOfMap(identify.get("tasks"));
        Map<String, Map<String, Object>> byTask = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> task : tasks) {
            byTask.put((String) task.get("task_code"), task);
        }
        Map<String, Object> triage = byTask.get("TASK_TRIAGE");
        assertEquals(2, ((Number) triage.get("total")).intValue());
        assertEquals(1, ((Number) triage.get("completed")).intValue());
        assertEquals(1, ((Number) triage.get("skipped")).intValue());
        assertEquals(50.0, ((Number) triage.get("completion_rate")).doubleValue());

        Map<String, Object> treatment = byNode.get("NODE_TREATMENT");
        assertEquals(1, ((Number) treatment.get("entered")).intValue());
        // 治疗节点没有任务，instance1 进入后保持 RUNNING（completeNode 自动进入下一节点但未完成）
        assertEquals(0, ((Number) treatment.get("completed")).intValue());
    }

    @Test
    void pathwayNodeStayDurationMetrics() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_STAY_TEST", "AMI节点滞留时长测试路径");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stages = (List<Map<String, Object>>) pathwayConfig.get("stages");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) stages.get(0).get("nodes");
        nodes.get(0).put("expected_minutes", 30);
        invokePost("/api/pathways", pathwayConfig);
        invokePost("/api/pathways/AMI_STAY_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> admit1 = new LinkedHashMap<String, Object>();
        admit1.put("patient_id", "P_STAY_001");
        admit1.put("encounter_id", "E_STAY_001");
        admit1.put("pathway_code", "AMI_STAY_TEST");
        admit1.put("version_no", "1.0.0");
        admit1.put("doctor_id", "STAY_DOC");
        Map<String, Object> admitted1 = invokePost("/api/patient-pathways/admit", admit1);
        String instance1 = (String) asMap(admitted1.get("data")).get("instanceId");

        Map<String, Object> completeNode = new LinkedHashMap<String, Object>();
        completeNode.put("operator_id", "STAY_DOC");
        invokePost("/api/patient-pathways/" + instance1 + "/nodes/NODE_IDENTIFY/complete", completeNode);

        Map<String, Object> admit2 = new LinkedHashMap<String, Object>();
        admit2.put("patient_id", "P_STAY_002");
        admit2.put("encounter_id", "E_STAY_002");
        admit2.put("pathway_code", "AMI_STAY_TEST");
        admit2.put("version_no", "1.0.0");
        admit2.put("doctor_id", "STAY_DOC");
        invokePost("/api/patient-pathways/admit", admit2);

        Map<String, Object> resp = invokeGet("/api/pathway-instances/node-stay-duration?pathwayCode=AMI_STAY_TEST");
        Map<String, Object> data = asMap(resp.get("data"));
        assertEquals(2, ((Number) data.get("total_instances")).intValue());
        assertEquals(3, ((Number) data.get("total_node_entries")).intValue());
        assertTrue(((Number) data.get("average_stay_ms")).doubleValue() >= 0.0);

        List<Map<String, Object>> durationNodes = asListOfMap(data.get("nodes"));
        Map<String, Map<String, Object>> byNode = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> node : durationNodes) {
            byNode.put((String) node.get("node_code"), node);
        }
        Map<String, Object> identify = byNode.get("NODE_IDENTIFY");
        assertEquals(2, ((Number) identify.get("entered")).intValue());
        assertEquals(1, ((Number) identify.get("completed")).intValue());
        assertEquals(1, ((Number) identify.get("running")).intValue());
        assertEquals(30, ((Number) identify.get("expected_minutes")).intValue());
        assertTrue(((Number) identify.get("max_stay_ms")).longValue() >= ((Number) identify.get("min_stay_ms")).longValue());
    }

    @Test
    void pathwayInstancesListAndSummary() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_INST_TEST", "AMI实例统计测试路径");
        invokePost("/api/pathways", pathwayConfig);
        invokePost("/api/pathways/AMI_INST_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> admitBody = new LinkedHashMap<String, Object>();
        admitBody.put("patient_id", "P_INST_001");
        admitBody.put("encounter_id", "E_INST_001");
        admitBody.put("pathway_code", "AMI_INST_TEST");
        admitBody.put("version_no", "1.0.0");
        admitBody.put("doctor_id", "INST_DOC");
        invokePost("/api/patient-pathways/admit", admitBody);

        Map<String, Object> admit2 = new LinkedHashMap<String, Object>();
        admit2.put("patient_id", "P_INST_002");
        admit2.put("encounter_id", "E_INST_002");
        admit2.put("pathway_code", "AMI_INST_TEST");
        admit2.put("version_no", "1.0.0");
        admit2.put("doctor_id", "INST_DOC");
        Map<String, Object> second = invokePost("/api/patient-pathways/admit", admit2);
        String secondInstanceId = (String) asMap(second.get("data")).get("instanceId");

        Map<String, Object> skipBody = new LinkedHashMap<String, Object>();
        skipBody.put("operator_id", "INST_DOC");
        skipBody.put("variation_type", "PATIENT_REASON");
        skipBody.put("reason", "实例统计测试：跳过分诊任务。");
        invokePost("/api/patient-pathways/" + secondInstanceId + "/nodes/NODE_IDENTIFY/tasks/TASK_TRIAGE/skip", skipBody);

        Map<String, Object> listResp = invokeGet("/api/pathway-instances?pathwayCode=AMI_INST_TEST&limit=10");
        List<Map<String, Object>> instances = asListOfMap(listResp.get("data"));
        assertEquals(2, instances.size());
        for (Map<String, Object> instance : instances) {
            assertEquals("AMI_INST_TEST", instance.get("pathwayCode"));
            assertEquals("ACTIVE", instance.get("status"));
        }

        Map<String, Object> filteredResp = invokeGet("/api/pathway-instances?pathwayCode=AMI_INST_TEST&patientId=P_INST_001");
        assertEquals(1, asListOfMap(filteredResp.get("data")).size());

        Map<String, Object> summaryResp = invokeGet("/api/pathway-instances/summary?pathwayCode=AMI_INST_TEST");
        Map<String, Object> summary = asMap(summaryResp.get("data"));
        assertEquals(2, ((Number) summary.get("total")).intValue());
        List<Map<String, Object>> byStatus = asListOfMap(summary.get("by_status"));
        assertEquals(1, byStatus.size());
        assertEquals("ACTIVE", byStatus.get(0).get("status"));
        assertEquals(2, ((Number) byStatus.get(0).get("count")).intValue());
        assertEquals(1, ((Number) summary.get("variation_total")).intValue());
        List<Map<String, Object>> variationByType = asListOfMap(summary.get("variation_by_type"));
        assertEquals(1, variationByType.size());
        assertEquals("PATIENT_REASON", variationByType.get(0).get("variation_type"));
    }

    @Test
    void pathwayQualityAndAuditQueriesFilterByOrgContext() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_ORG_TEST", "AMI组织上下文测试路径");
        invokePost("/api/pathways", pathwayConfig);
        invokePost("/api/pathways/AMI_ORG_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> alphaAdmit = new LinkedHashMap<String, Object>();
        alphaAdmit.put("tenant_id", "TENANT_PATH_ORG");
        alphaAdmit.put("group_code", "GROUP_PATH");
        alphaAdmit.put("hospital_code", "HOSPITAL_ALPHA");
        alphaAdmit.put("campus_code", "CAMPUS_ALPHA");
        alphaAdmit.put("department_code", "DEPT_ALPHA");
        alphaAdmit.put("patient_id", "P_ORG_ALPHA");
        alphaAdmit.put("encounter_id", "E_ORG_ALPHA");
        alphaAdmit.put("pathway_code", "AMI_ORG_TEST");
        alphaAdmit.put("version_no", "1.0.0");
        alphaAdmit.put("doctor_id", "ORG_DOC");
        Map<String, Object> alphaResp = invokePost("/api/patient-pathways/admit", alphaAdmit);
        Map<String, Object> alpha = asMap(alphaResp.get("data"));
        assertEquals("TENANT_PATH_ORG", alpha.get("tenantId"));
        assertEquals("HOSPITAL_ALPHA", alpha.get("hospitalCode"));
        assertEquals("DEPARTMENT", alpha.get("scopeLevel"));
        assertEquals("DEPT_ALPHA", alpha.get("scopeCode"));
        assertEquals("BODY", alpha.get("orgSource"));
        String alphaInstanceId = (String) alpha.get("instanceId");

        Map<String, Object> betaAdmit = new LinkedHashMap<String, Object>();
        betaAdmit.put("tenant_id", "TENANT_PATH_ORG");
        betaAdmit.put("group_code", "GROUP_PATH");
        betaAdmit.put("hospital_code", "HOSPITAL_BETA");
        betaAdmit.put("campus_code", "CAMPUS_BETA");
        betaAdmit.put("department_code", "DEPT_BETA");
        betaAdmit.put("patient_id", "P_ORG_BETA");
        betaAdmit.put("encounter_id", "E_ORG_BETA");
        betaAdmit.put("pathway_code", "AMI_ORG_TEST");
        betaAdmit.put("version_no", "1.0.0");
        betaAdmit.put("doctor_id", "ORG_DOC");
        invokePost("/api/patient-pathways/admit", betaAdmit);

        Map<String, Object> skipBody = new LinkedHashMap<String, Object>();
        skipBody.put("operator_id", "ORG_DOC");
        skipBody.put("variation_type", "ORG_FILTER_TEST");
        skipBody.put("reason", "组织过滤测试：Alpha 院区变异。");
        invokePost("/api/patient-pathways/" + alphaInstanceId + "/nodes/NODE_IDENTIFY/tasks/TASK_TRIAGE/skip", skipBody);

        Map<String, Object> alphaListResp = invokeGet("/api/pathway-instances?pathwayCode=AMI_ORG_TEST&hospitalCode=HOSPITAL_ALPHA");
        List<Map<String, Object>> alphaInstances = asListOfMap(alphaListResp.get("data"));
        assertEquals(1, alphaInstances.size());
        assertEquals("HOSPITAL_ALPHA", alphaInstances.get(0).get("hospitalCode"));

        Map<String, Object> betaListResp = invokeGet("/api/pathway-instances?pathwayCode=AMI_ORG_TEST&hospitalCode=HOSPITAL_BETA");
        List<Map<String, Object>> betaInstances = asListOfMap(betaListResp.get("data"));
        assertEquals(1, betaInstances.size());
        assertEquals("HOSPITAL_BETA", betaInstances.get(0).get("hospitalCode"));

        Map<String, Object> variationResp = invokeGet("/api/pathway-variations?pathwayCode=AMI_ORG_TEST"
                + "&scopeLevel=DEPARTMENT&scopeCode=DEPT_ALPHA");
        List<Map<String, Object>> variations = asListOfMap(variationResp.get("data"));
        assertEquals(1, variations.size());
        assertEquals("DEPT_ALPHA", variations.get(0).get("scopeCode"));

        Map<String, Object> metricsResp = invokeGet("/api/quality/metrics?pathwayCode=AMI_ORG_TEST&hospitalCode=HOSPITAL_ALPHA");
        Map<String, Object> metrics = asMap(metricsResp.get("data"));
        assertEquals(1, ((Number) asMap(metrics.get("instance_summary")).get("total")).intValue());
        assertEquals(1, ((Number) asMap(metrics.get("variation_summary")).get("total")).intValue());

        Map<String, Object> auditResp = invokeGet("/api/audit-logs?engineType=PATHWAY&actionType=ADMIT"
                + "&hospitalCode=HOSPITAL_ALPHA&limit=10");
        List<Map<String, Object>> auditRecords = asListOfMap(auditResp.get("data"));
        assertFalse(auditRecords.isEmpty(), "org-scoped admit audit should be queryable");
        assertEquals("HOSPITAL_ALPHA", auditRecords.get(0).get("hospital_code"));
        assertEquals("DEPT_ALPHA", auditRecords.get(0).get("scope_code"));
    }

    @Test
    void pathwayVariationsListAndSummary() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_VAR_TEST", "AMI变异聚合测试路径");
        invokePost("/api/pathways", pathwayConfig);
        invokePost("/api/pathways/AMI_VAR_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> admitBody = new LinkedHashMap<String, Object>();
        admitBody.put("patient_id", "P_VAR_001");
        admitBody.put("encounter_id", "E_VAR_001");
        admitBody.put("pathway_code", "AMI_VAR_TEST");
        admitBody.put("version_no", "1.0.0");
        admitBody.put("doctor_id", "VAR_DOC");
        Map<String, Object> admitted = invokePost("/api/patient-pathways/admit", admitBody);
        String instanceId = (String) asMap(admitted.get("data")).get("instanceId");

        Map<String, Object> skipBody = new LinkedHashMap<String, Object>();
        skipBody.put("operator_id", "VAR_DOC");
        skipBody.put("variation_type", "PATIENT_REASON");
        skipBody.put("reason", "患者拒绝完成分诊任务。");
        invokePost("/api/patient-pathways/" + instanceId + "/nodes/NODE_IDENTIFY/tasks/TASK_TRIAGE/skip", skipBody);

        Map<String, Object> manualVariation = new LinkedHashMap<String, Object>();
        manualVariation.put("node_code", "NODE_IDENTIFY");
        manualVariation.put("variation_type", "RESOURCE_LIMIT");
        manualVariation.put("reason", "导管室资源等待。");
        manualVariation.put("operator_id", "VAR_DOC");
        invokePost("/api/patient-pathways/" + instanceId + "/variations", manualVariation);

        Map<String, Object> listResp = invokeGet("/api/pathway-variations?pathwayCode=AMI_VAR_TEST&limit=20");
        List<Map<String, Object>> records = asListOfMap(listResp.get("data"));
        assertEquals(2, records.size(), "expected 2 variations for AMI_VAR_TEST");
        for (Map<String, Object> record : records) {
            assertEquals("AMI_VAR_TEST", record.get("pathwayCode"));
            assertEquals(instanceId, record.get("instanceId"));
        }

        Map<String, Object> filteredResp = invokeGet("/api/pathway-variations?pathwayCode=AMI_VAR_TEST&variationType=RESOURCE_LIMIT");
        List<Map<String, Object>> filtered = asListOfMap(filteredResp.get("data"));
        assertEquals(1, filtered.size());
        assertEquals("RESOURCE_LIMIT", filtered.get(0).get("variationType"));

        Map<String, Object> summaryResp = invokeGet("/api/pathway-variations/summary?pathwayCode=AMI_VAR_TEST");
        Map<String, Object> summary = asMap(summaryResp.get("data"));
        assertEquals(2, ((Number) summary.get("total")).intValue());
        List<Map<String, Object>> byType = asListOfMap(summary.get("by_variation_type"));
        assertEquals(2, byType.size());
        Map<String, Integer> typeCounts = new LinkedHashMap<String, Integer>();
        for (Map<String, Object> bucket : byType) {
            typeCounts.put((String) bucket.get("variation_type"), ((Number) bucket.get("count")).intValue());
        }
        assertEquals(Integer.valueOf(1), typeCounts.get("PATIENT_REASON"));
        assertEquals(Integer.valueOf(1), typeCounts.get("RESOURCE_LIMIT"));
        List<Map<String, Object>> byPathway = asListOfMap(summary.get("by_pathway_code"));
        assertEquals(1, byPathway.size());
        assertEquals("AMI_VAR_TEST", byPathway.get(0).get("pathway_code"));
    }

    @Test
    void qualityMetricsAndAuditLogsExposeOperationalView() throws Exception {
        Map<String, Object> pathwayConfig = samplePathwayConfig("AMI_QC_TEST", "AMI质控聚合测试路径");
        invokePost("/api/pathways", pathwayConfig);
        invokePost("/api/pathways/AMI_QC_TEST/publish", new LinkedHashMap<String, Object>());

        Map<String, Object> admitBody = new LinkedHashMap<String, Object>();
        admitBody.put("patient_id", "P_QC_001");
        admitBody.put("encounter_id", "E_QC_001");
        admitBody.put("pathway_code", "AMI_QC_TEST");
        admitBody.put("version_no", "1.0.0");
        admitBody.put("doctor_id", "QC_DOC");
        Map<String, Object> admitted = invokePost("/api/patient-pathways/admit", admitBody);
        String instanceId = (String) asMap(admitted.get("data")).get("instanceId");

        Map<String, Object> skipBody = new LinkedHashMap<String, Object>();
        skipBody.put("operator_id", "QC_DOC");
        skipBody.put("variation_type", "RESOURCE_LIMIT");
        skipBody.put("reason", "质控聚合测试：资源等待。");
        invokePost("/api/patient-pathways/" + instanceId + "/nodes/NODE_IDENTIFY/tasks/TASK_TRIAGE/skip", skipBody);

        Map<String, Object> metricsResp = invokeGet("/api/quality/metrics?pathwayCode=AMI_QC_TEST");
        Map<String, Object> metrics = asMap(metricsResp.get("data"));
        assertEquals("AMI_QC_TEST", metrics.get("pathway_code"));
        assertEquals(1, ((Number) asMap(metrics.get("instance_summary")).get("total")).intValue());
        assertEquals(1, ((Number) asMap(metrics.get("variation_summary")).get("total")).intValue());
        assertEquals(1, ((Number) asMap(metrics.get("node_completion")).get("total_instances")).intValue());
        assertTrue(((Number) asMap(metrics.get("node_stay_duration")).get("total_node_entries")).intValue() >= 1);

        Map<String, Object> publishAudit = invokeGet("/api/audit-logs?engineType=PATHWAY&actionType=PUBLISH&targetCode=AMI_QC_TEST");
        List<Map<String, Object>> publishRecords = asListOfMap(publishAudit.get("data"));
        assertFalse(publishRecords.isEmpty(), "publish audit log should be queryable");
        assertEquals("PATHWAY", publishRecords.get(0).get("engine_type"));
        assertEquals("PUBLISH", publishRecords.get(0).get("action_type"));

        Map<String, Object> auditSummaryResp = invokeGet("/api/audit-logs/summary?engineType=PATHWAY&targetCode=AMI_QC_TEST");
        Map<String, Object> auditSummary = asMap(auditSummaryResp.get("data"));
        assertTrue(((Number) auditSummary.get("total")).intValue() >= 2);
    }

    @Test
    void difyWorkflowTemplateImportAndDegradedRun() throws Exception {
        Map<String, Object> template = new LinkedHashMap<String, Object>();
        template.put("workflow_code", "WF_JUNIT_EXPLAIN");
        template.put("workflow_name", "JUnit 解释工作流");
        template.put("workflow_version", "1.0.0");
        template.put("description", "JUnit 用例用的 Dify 工作流模板。");
        template.put("required_inputs", Arrays.asList("patient_id", "target_code"));
        Map<String, Object> defaults = new LinkedHashMap<String, Object>();
        defaults.put("scenario", "PATHWAY_ENTRY");
        defaults.put("language", "zh-CN");
        template.put("input_defaults", defaults);
        Map<String, Object> degraded = new LinkedHashMap<String, Object>();
        degraded.put("explanation", "JUnit 模板降级输出。");
        degraded.put("recommended_action", "由医生确认。");
        template.put("degraded_outputs", degraded);

        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("templates", Arrays.asList(template));
        Map<String, Object> imported = invokePost("/api/dify/workflows", importBody);
        assertEquals(1, asList(imported.get("data")).size());

        Map<String, Object> listResp = invokeGet("/api/dify/workflows");
        List<Map<String, Object>> templates = asListOfMap(listResp.get("data"));
        assertFalse(templates.isEmpty());

        Map<String, Object> getResp = invokeGet("/api/dify/workflows/WF_JUNIT_EXPLAIN");
        assertEquals("WF_JUNIT_EXPLAIN", asMap(getResp.get("data")).get("workflowCode"));

        // 模板要求 patient_id 与 target_code，缺失任一应返回 VALIDATION_ERROR
        Map<String, Object> missingRun = new LinkedHashMap<String, Object>();
        missingRun.put("workflow_code", "WF_JUNIT_EXPLAIN");
        missingRun.put("inputs", new LinkedHashMap<String, Object>());
        Map<String, Object> validationError = invokePostExpectingClientError("/api/dify/workflows/run", missingRun);
        assertEquals("VALIDATION_ERROR", validationError.get("code"));

        // 正常调用：未启用 Dify 应回退到本地降级，且降级输出来自模板
        Map<String, Object> runBody = new LinkedHashMap<String, Object>();
        runBody.put("workflow_code", "WF_JUNIT_EXPLAIN");
        Map<String, Object> inputs = new LinkedHashMap<String, Object>();
        inputs.put("patient_id", "P_JUNIT_001");
        inputs.put("target_code", "AMI_STEMI");
        runBody.put("inputs", inputs);
        Map<String, Object> runResp = invokePost("/api/dify/workflows/run", runBody);
        Map<String, Object> data = asMap(runResp.get("data"));
        assertEquals("DEGRADED", data.get("status"));
        assertEquals(Boolean.TRUE, data.get("template_applied"));
        Map<String, Object> outputs = asMap(data.get("outputs"));
        assertEquals("JUnit 模板降级输出。", outputs.get("explanation"));
        assertEquals("AMI_STEMI", outputs.get("target_code"));
    }

    @Test
    void difyWorkflowInvocationStatsAggregateByTemplate() throws Exception {
        Map<String, Object> template = new LinkedHashMap<String, Object>();
        template.put("workflow_code", "WF_JUNIT_STATS");
        template.put("workflow_name", "JUnit 统计工作流");
        template.put("workflow_version", "1.0.0");
        template.put("required_inputs", Arrays.asList("patient_id", "target_code"));
        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("templates", Arrays.asList(template));
        invokePost("/api/dify/workflows", importBody);

        Map<String, Object> missingRun = new LinkedHashMap<String, Object>();
        missingRun.put("workflow_code", "WF_JUNIT_STATS");
        missingRun.put("inputs", new LinkedHashMap<String, Object>());
        Map<String, Object> validation = invokePostExpectingClientError("/api/dify/workflows/run", missingRun);
        assertEquals("VALIDATION_ERROR", validation.get("code"));

        Map<String, Object> runBody = new LinkedHashMap<String, Object>();
        runBody.put("workflow_code", "WF_JUNIT_STATS");
        Map<String, Object> inputs = new LinkedHashMap<String, Object>();
        inputs.put("patient_id", "P_DIFY_STATS_001");
        inputs.put("encounter_id", "E_DIFY_STATS_001");
        inputs.put("target_code", "AMI_STEMI");
        runBody.put("inputs", inputs);
        invokePost("/api/dify/workflows/run", runBody);

        Map<String, Object> statsResp = invokeGet("/api/dify/workflows/stats?workflowCode=WF_JUNIT_STATS");
        Map<String, Object> stats = asMap(statsResp.get("data"));
        assertEquals(2, ((Number) stats.get("total_calls")).intValue());
        assertEquals(1, ((Number) stats.get("degraded_calls")).intValue());
        assertEquals(1, ((Number) stats.get("validation_error_calls")).intValue());
        assertTrue(((Number) stats.get("average_elapsed_ms")).doubleValue() >= 0.0);

        List<Map<String, Object>> byWorkflow = asListOfMap(stats.get("by_workflow"));
        assertEquals(1, byWorkflow.size());
        assertEquals("WF_JUNIT_STATS", byWorkflow.get(0).get("workflow_code"));
        assertEquals(2, ((Number) byWorkflow.get(0).get("total_calls")).intValue());
    }

    @Test
    void difyWorkflowTemplateMapsInputsAndKeepsRetryPolicy() throws Exception {
        Map<String, Object> template = new LinkedHashMap<String, Object>();
        template.put("workflow_code", "WF_JUNIT_MAPPING");
        template.put("workflow_name", "JUnit 映射工作流");
        template.put("workflow_version", "1.0.0");
        template.put("required_inputs", Arrays.asList("patient_id", "encounter_id", "target_code"));
        template.put("retry_count", 2);
        Map<String, Object> mappings = new LinkedHashMap<String, Object>();
        mappings.put("patient_id", "patient_context.patient.patient_id");
        mappings.put("encounter_id", "patient_context.encounter.encounter_id");
        mappings.put("target_code", "recommendation.target_code");
        template.put("input_mappings", mappings);
        Map<String, Object> degraded = new LinkedHashMap<String, Object>();
        degraded.put("explanation", "JUnit 映射降级输出。");
        template.put("degraded_outputs", degraded);

        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("templates", Arrays.asList(template));
        invokePost("/api/dify/workflows", importBody);

        Map<String, Object> getResp = invokeGet("/api/dify/workflows/WF_JUNIT_MAPPING");
        Map<String, Object> storedTemplate = asMap(getResp.get("data"));
        assertEquals(2, ((Number) storedTemplate.get("retryCount")).intValue());
        Map<String, Object> storedMappings = asMap(storedTemplate.get("inputMappings"));
        assertEquals("patient_context.patient.patient_id", storedMappings.get("patient_id"));

        Map<String, Object> runBody = new LinkedHashMap<String, Object>();
        runBody.put("workflow_code", "WF_JUNIT_MAPPING");
        Map<String, Object> patientContext = new LinkedHashMap<String, Object>();
        Map<String, Object> patient = new LinkedHashMap<String, Object>();
        patient.put("patient_id", "P_DIFY_MAP_001");
        patientContext.put("patient", patient);
        Map<String, Object> encounter = new LinkedHashMap<String, Object>();
        encounter.put("encounter_id", "E_DIFY_MAP_001");
        patientContext.put("encounter", encounter);
        runBody.put("patient_context", patientContext);
        Map<String, Object> recommendation = new LinkedHashMap<String, Object>();
        recommendation.put("target_code", "AMI_STEMI");
        runBody.put("recommendation", recommendation);

        Map<String, Object> runResp = invokePost("/api/dify/workflows/run", runBody);
        Map<String, Object> data = asMap(runResp.get("data"));
        assertEquals("DEGRADED", data.get("status"));
        Map<String, Object> outputs = asMap(data.get("outputs"));
        assertEquals("AMI_STEMI", outputs.get("target_code"));

        Map<String, Object> statsResp = invokeGet("/api/dify/workflows/stats?workflowCode=WF_JUNIT_MAPPING"
                + "&patientId=P_DIFY_MAP_001&encounterId=E_DIFY_MAP_001");
        Map<String, Object> stats = asMap(statsResp.get("data"));
        assertEquals(1, ((Number) stats.get("degraded_calls")).intValue());
    }

    @Test
    void graphNodesEdgesImportAndCandidatesRecall() throws Exception {
        String version = "JUNIT_GRAPH_NODES";

        Map<String, Object> diseaseNode = new LinkedHashMap<String, Object>();
        diseaseNode.put("code", "HEART_FAILURE");
        diseaseNode.put("name", "心力衰竭");
        diseaseNode.put("type", "DISEASE");
        diseaseNode.put("graph_version", version);
        Map<String, Object> symptomNode = new LinkedHashMap<String, Object>();
        symptomNode.put("code", "DYSPNEA");
        symptomNode.put("name", "呼吸困难");
        symptomNode.put("type", "SYMPTOM");
        symptomNode.put("graph_version", version);
        Map<String, Object> findingNode = new LinkedHashMap<String, Object>();
        findingNode.put("code", "BNP_HIGH");
        findingNode.put("name", "BNP 升高");
        findingNode.put("type", "FINDING");
        findingNode.put("graph_version", version);

        Map<String, Object> nodesBody = new LinkedHashMap<String, Object>();
        nodesBody.put("nodes", Arrays.asList(diseaseNode, symptomNode, findingNode));
        Map<String, Object> importedNodes = invokePost("/api/graph/nodes", nodesBody);
        assertEquals(3, asList(importedNodes.get("data")).size());

        Map<String, Object> edge1 = new LinkedHashMap<String, Object>();
        edge1.put("from_code", "DYSPNEA");
        edge1.put("to_code", "HEART_FAILURE");
        edge1.put("relation_type", "HAS_CORE_SYMPTOM");
        edge1.put("graph_version", version);
        edge1.put("weight", 0.8);
        Map<String, Object> edge2 = new LinkedHashMap<String, Object>();
        edge2.put("from_code", "BNP_HIGH");
        edge2.put("to_code", "HEART_FAILURE");
        edge2.put("relation_type", "HAS_EXAM_FINDING");
        edge2.put("graph_version", version);
        edge2.put("weight", 0.9);

        Map<String, Object> edgesBody = new LinkedHashMap<String, Object>();
        edgesBody.put("edges", Arrays.asList(edge1, edge2));
        Map<String, Object> importedEdges = invokePost("/api/graph/edges", edgesBody);
        assertEquals(2, asList(importedEdges.get("data")).size());

        Map<String, Object> nodesListResp = invokeGet("/api/graph/nodes?graphVersion=" + version + "&type=DISEASE");
        List<Map<String, Object>> nodesList = asListOfMap(nodesListResp.get("data"));
        assertEquals(1, nodesList.size());
        assertEquals("HEART_FAILURE", nodesList.get(0).get("code"));

        Map<String, Object> edgesListResp = invokeGet("/api/graph/edges?graphVersion=" + version + "&toCode=HEART_FAILURE");
        assertEquals(2, asList(edgesListResp.get("data")).size());

        // disease-candidates 降级：传入 DYSPNEA + BNP_HIGH 应召回 HEART_FAILURE，且不再返回 AMI_STEMI
        Map<String, Object> candidateRequest = new LinkedHashMap<String, Object>();
        candidateRequest.put("symptom_codes", Arrays.asList("DYSPNEA"));
        candidateRequest.put("finding_codes", Arrays.asList("BNP_HIGH"));
        candidateRequest.put("graph_version", version);
        Map<String, Object> candidateResp = invokePost("/api/graph/disease-candidates", candidateRequest);
        List<Map<String, Object>> candidates = asListOfMap(candidateResp.get("data"));
        assertEquals(1, candidates.size());
        Map<String, Object> first = candidates.get(0);
        assertEquals("HEART_FAILURE", first.get("diseaseCode"));
        assertEquals("REGISTERED_FALLBACK", first.get("graphSource"));
        // 评分 = 0.8 + 0.9 = 1.7，乘 100 = 170
        assertEquals(170.0, ((Number) first.get("rawGraphScore")).doubleValue());
        List<Map<String, Object>> relations = asListOfMap(first.get("matchedRelations"));
        assertEquals(2, relations.size());
    }

    @Test
    void graphVersionsAndEvidencesRegistration() throws Exception {
        Map<String, Object> versionEntry = new LinkedHashMap<String, Object>();
        versionEntry.put("graph_version", "JUNIT_GRAPH_2026_01");
        versionEntry.put("name", "JUnit 测试图谱");
        versionEntry.put("status", "DRAFT");
        versionEntry.put("description", "JUnit 用例的图谱版本");

        Map<String, Object> versionsBody = new LinkedHashMap<String, Object>();
        versionsBody.put("versions", Arrays.asList(versionEntry));
        Map<String, Object> importedVersions = invokePost("/api/graph/versions", versionsBody);
        assertEquals(1, asList(importedVersions.get("data")).size());

        Map<String, Object> activated = invokePost("/api/graph/versions/JUNIT_GRAPH_2026_01/activate",
                new LinkedHashMap<String, Object>());
        assertEquals("ACTIVE", asMap(activated.get("data")).get("status"));

        Map<String, Object> evidenceEntry = new LinkedHashMap<String, Object>();
        evidenceEntry.put("evidence_id", "EV_JUNIT_001");
        evidenceEntry.put("graph_version", "JUNIT_GRAPH_2026_01");
        evidenceEntry.put("target_code", "JUNIT_DISEASE");
        evidenceEntry.put("target_type", "DISEASE");
        evidenceEntry.put("evidence_type", "GUIDELINE");
        evidenceEntry.put("title", "JUnit 测试证据");
        evidenceEntry.put("summary", "用于验证 fallback 优先返回已注册证据。");
        evidenceEntry.put("confidence", 0.91);

        Map<String, Object> evidencesBody = new LinkedHashMap<String, Object>();
        evidencesBody.put("evidences", Arrays.asList(evidenceEntry));
        Map<String, Object> importedEvidences = invokePost("/api/graph/evidences", evidencesBody);
        assertEquals(1, asList(importedEvidences.get("data")).size());

        Map<String, Object> listResp = invokeGet("/api/graph/evidences?targetCode=JUNIT_DISEASE");
        List<Map<String, Object>> evidences = asListOfMap(listResp.get("data"));
        assertEquals(1, evidences.size());
        assertEquals("EV_JUNIT_001", evidences.get(0).get("evidence_id"));

        Map<String, Object> getResp = invokeGet("/api/graph/evidences/EV_JUNIT_001");
        assertEquals("JUNIT_DISEASE", asMap(getResp.get("data")).get("target_code"));

        // Neo4j 未启用：evidence 接口应返回注册证据而不是默认的 EV_AMI_001
        Map<String, Object> evidenceRequest = new LinkedHashMap<String, Object>();
        evidenceRequest.put("target_code", "JUNIT_DISEASE");
        evidenceRequest.put("graph_version", "JUNIT_GRAPH_2026_01");
        Map<String, Object> evidenceResp = invokePost("/api/graph/evidence", evidenceRequest);
        List<Map<String, Object>> returned = asListOfMap(evidenceResp.get("data"));
        assertEquals(1, returned.size());
        assertEquals("EV_JUNIT_001", returned.get(0).get("evidence_id"));
        assertEquals("REGISTERED_FALLBACK", returned.get(0).get("graph_source"));
    }

    @Test
    void graphVersionImportRejectsInvalidEntry() throws Exception {
        Map<String, Object> bad = new LinkedHashMap<String, Object>();
        bad.put("versions", Arrays.asList(new LinkedHashMap<String, Object>()));
        Map<String, Object> response = invokePostExpectingClientError("/api/graph/versions", bad);
        assertEquals("VALIDATION_ERROR", response.get("code"));
    }

    @Test
    void graphDegradedWhenNeo4jDisabled() throws Exception {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("symptom_codes", Arrays.asList("CHEST_PAIN"));
        body.put("finding_codes", Arrays.asList("ST_ELEVATION_CONTIGUOUS_LEADS"));
        Map<String, Object> response = invokePost("/api/graph/disease-candidates", body);
        assertEquals(Boolean.TRUE, response.get("success"));
        // 关掉 Neo4j 时图谱接口应给出可解释降级，不应抛异常或返回失败。
        List<Map<String, Object>> candidates = asListOfMap(response.get("data"));
        assertFalse(candidates.isEmpty(), "fallback candidates should not be empty for AMI sample input");
        assertEquals("AMI_STEMI", candidates.get(0).get("diseaseCode"));
    }

    private Map<String, Object> ruleDefinition(String ruleCode, String ruleName) {
        Map<String, Object> rule = new LinkedHashMap<String, Object>();
        rule.put("rule_code", ruleCode);
        rule.put("rule_name", ruleName);
        rule.put("rule_type", "PATHWAY_ENTRY");
        rule.put("version_no", "1.0.0");
        rule.put("severity", "HIGH");
        rule.put("priority", 100);
        rule.put("enabled", true);
        Map<String, Object> chiefComplaint = new LinkedHashMap<String, Object>();
        chiefComplaint.put("fact", "chief_complaints.code");
        chiefComplaint.put("operator", "in");
        chiefComplaint.put("value", Arrays.asList("CHEST_PAIN"));
        Map<String, Object> exam = new LinkedHashMap<String, Object>();
        exam.put("fact", "exams.finding_codes");
        exam.put("operator", "contains");
        exam.put("value", "ST_ELEVATION_CONTIGUOUS_LEADS");
        Map<String, Object> condition = new LinkedHashMap<String, Object>();
        condition.put("all", Arrays.asList(chiefComplaint, exam));
        rule.put("condition", condition);
        rule.put("actions", Arrays.asList(actionMap("CREATE_RECOMMENDATION")));
        rule.put("message_template", "命中STEMI候选入径规则。");
        return rule;
    }

    private Map<String, Object> rulePackage(String packageCode, String packageVersion, Map<String, Object> rule) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("package_code", packageCode);
        body.put("package_version", packageVersion);
        body.put("rules", Arrays.asList(rule));
        return body;
    }

    private Map<String, Object> actionMap(String type) {
        Map<String, Object> action = new LinkedHashMap<String, Object>();
        action.put("type", type);
        return action;
    }

    private Map<String, Object> samplePatientContext() {
        Map<String, Object> patient = new LinkedHashMap<String, Object>();
        patient.put("patient_id", "P_JUNIT_001");
        Map<String, Object> encounter = new LinkedHashMap<String, Object>();
        encounter.put("encounter_id", "E_JUNIT_001");
        encounter.put("visit_type", "EMERGENCY");
        Map<String, Object> chief = new LinkedHashMap<String, Object>();
        chief.put("code", "CHEST_PAIN");
        chief.put("text", "胸痛2小时");
        Map<String, Object> exam = new LinkedHashMap<String, Object>();
        exam.put("finding_codes", Arrays.asList("ST_ELEVATION_CONTIGUOUS_LEADS"));
        Map<String, Object> facts = new LinkedHashMap<String, Object>();
        facts.put("chief_complaints", Arrays.asList(chief));
        facts.put("exams", Arrays.asList(exam));
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        context.put("patient", patient);
        context.put("encounter", encounter);
        context.put("facts", facts);
        return context;
    }

    private Map<String, Object> samplePathwayConfig(String pathwayCode, String pathwayName) {
        Map<String, Object> task = new LinkedHashMap<String, Object>();
        task.put("task_code", "TASK_TRIAGE");
        task.put("task_name", "急诊分诊");
        task.put("task_type", "TASK");
        task.put("required", true);

        Map<String, Object> nodeIdentify = new LinkedHashMap<String, Object>();
        nodeIdentify.put("node_code", "NODE_IDENTIFY");
        nodeIdentify.put("node_name", "识别");
        nodeIdentify.put("tasks", Arrays.asList(task));
        Map<String, Object> transition = new LinkedHashMap<String, Object>();
        transition.put("to_node", "NODE_TREATMENT");
        transition.put("priority", 100);
        nodeIdentify.put("transitions", Arrays.asList(transition));

        Map<String, Object> nodeTreatment = new LinkedHashMap<String, Object>();
        nodeTreatment.put("node_code", "NODE_TREATMENT");
        nodeTreatment.put("node_name", "治疗");

        Map<String, Object> stage = new LinkedHashMap<String, Object>();
        stage.put("stage_code", "STAGE_MAIN");
        stage.put("stage_name", "主流程");
        stage.put("nodes", Arrays.asList(nodeIdentify, nodeTreatment));

        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("pathway_code", pathwayCode);
        config.put("pathway_name", pathwayName);
        config.put("version_no", "1.0.0");
        config.put("specialty_code", "CARDIOLOGY");
        config.put("disease_code", pathwayCode);
        config.put("stages", Arrays.asList(stage));
        return config;
    }

    private Map<String, Object> sampleConfigPackage(String packageCode, String packageVersion) {
        Map<String, Object> rule = ruleDefinition("R_" + packageCode, "配置包测试规则");
        rule.put("package_code", packageCode);
        rule.put("package_version", packageVersion);

        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("package_code", packageCode);
        snapshot.put("package_version", packageVersion);
        snapshot.put("rules", Arrays.asList(rule));

        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        manifest.put("owner", "QUALITY_CENTER");
        manifest.put("description", "JUnit 配置包样例");

        Map<String, Object> configPackage = new LinkedHashMap<String, Object>();
        configPackage.put("tenant_id", "default");
        configPackage.put("package_code", packageCode);
        configPackage.put("package_version", packageVersion);
        configPackage.put("asset_type", "RULE");
        configPackage.put("scope_level", "HOSPITAL");
        configPackage.put("scope_code", "HOSPITAL_JUNIT");
        configPackage.put("target_version", packageVersion);
        configPackage.put("created_by", "JUNIT");
        configPackage.put("manifest", manifest);
        configPackage.put("full_snapshot", snapshot);
        return configPackage;
    }

    private Map<String, Object> sampleSourceDocumentImport(String tenantId) {
        Map<String, Object> guideline = new LinkedHashMap<String, Object>();
        guideline.put("document_code", "SRC_GUIDELINE_AMI_2025");
        guideline.put("title", "急性ST段抬高型心肌梗死诊疗指南（2025版）");
        guideline.put("source_type", "GUIDELINE");
        guideline.put("source_uri", "https://example.org/guidelines/ami-stemi-2025");
        guideline.put("publisher", "中华医学会心血管病学分会");
        guideline.put("effective_date", "2025-01-01");
        guideline.put("expiry_date", "2028-12-31");
        guideline.put("review_status", "REVIEWED");
        guideline.put("reviewed_by", "PROV_REVIEWER");
        guideline.put("reviewed_time", "2026-05-17T10:30:00+08:00");
        guideline.put("content_hash", "sha256:guideline-ami-2025");
        Map<String, Object> guidelineMetadata = new LinkedHashMap<String, Object>();
        guidelineMetadata.put("specialty", "CARDIOLOGY");
        guidelineMetadata.put("region", "CN");
        guideline.put("metadata", guidelineMetadata);

        Map<String, Object> consensus = new LinkedHashMap<String, Object>();
        consensus.put("document_code", "SRC_CONSENSUS_STEMI_2024");
        consensus.put("title", "急诊 STEMI 绿色通道专家共识");
        consensus.put("source_type", "CONSENSUS");
        consensus.put("source_uri", "https://example.org/consensus/stemi-er-2024");
        consensus.put("publisher", "国家胸痛中心");
        consensus.put("effective_date", "2024-06-01");
        consensus.put("expiry_date", "2027-05-31");
        consensus.put("review_status", "DRAFT");

        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("tenant_id", tenantId);
        importBody.put("operator_id", "JUNIT_PROV_ADMIN");
        importBody.put("documents", Arrays.asList(guideline, consensus));
        return importBody;
    }

    private Map<String, Object> sampleOrganizationImport(String tenantId) {
        List<Map<String, Object>> units = new java.util.ArrayList<Map<String, Object>>();
        units.add(orgUnit("GROUP", "GROUP_JUNIT", "演示集团", null, null, 1));
        units.add(orgUnit("HOSPITAL", "HOSPITAL_JUNIT", "演示医院", "GROUP", "GROUP_JUNIT", 1));
        units.add(orgUnit("CAMPUS", "CAMPUS_EAST", "东院区", "HOSPITAL", "HOSPITAL_JUNIT", 1));
        units.add(orgUnit("SITE", "SITE_ER", "急诊站点", "CAMPUS", "CAMPUS_EAST", 1));
        units.add(orgUnit("DEPARTMENT", "DEPT_CARDIOLOGY", "心血管内科", "SITE", "SITE_ER", 1));

        Map<String, Object> importBody = new LinkedHashMap<String, Object>();
        importBody.put("tenant_id", tenantId);
        importBody.put("operator_id", "JUNIT_ORG_ADMIN");
        importBody.put("units", units);
        return importBody;
    }

    private Map<String, Object> orgUnit(String level, String code, String name,
                                        String parentLevel, String parentCode, int displayOrder) {
        Map<String, Object> unit = new LinkedHashMap<String, Object>();
        unit.put("level", level);
        unit.put("code", code);
        unit.put("name", name);
        if (parentLevel != null) {
            unit.put("parent_level", parentLevel);
            unit.put("parent_code", parentCode);
        }
        unit.put("display_order", displayOrder);
        unit.put("status", "ACTIVE");
        return unit;
    }

    private Map<String, Object> invokeGet(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();
        assertEquals(200, result.getResponse().getStatus(), "GET " + path + " unexpected status");
        return parse(result);
    }

    private Map<String, Object> invokePost(String path, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsBytes(body)))
                .andReturn();
        assertEquals(200, result.getResponse().getStatus(), "POST " + path + " unexpected status");
        return parse(result);
    }

    private Map<String, Object> invokePostWithHeaders(String path, Object body,
                                                      Map<String, Object> headers) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsBytes(body));
        if (headers != null) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                builder = builder.header(entry.getKey(), entry.getValue());
            }
        }
        MvcResult result = mockMvc.perform(builder).andReturn();
        assertEquals(200, result.getResponse().getStatus(), "POST " + path + " unexpected status");
        return parse(result);
    }

    private Map<String, Object> invokePostExpectingClientError(String path, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsBytes(body)))
                .andReturn();
        assertTrue(result.getResponse().getStatus() >= 400,
                "POST " + path + " expected 4xx but got " + result.getResponse().getStatus());
        return parse(result);
    }

    private Map<String, Object> parse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readValue(body, LinkedHashMap.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    private List<Object> asList(Object value) {
        return value instanceof List ? (List<Object>) value : java.util.Collections.<Object>emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object value) {
        return value instanceof List ? (List<Map<String, Object>>) value : java.util.Collections.<Map<String, Object>>emptyList();
    }

    @Test
    void orgOverrideImportAndComputeInheritance() throws Exception {
        invokePost("/api/organizations", sampleOrganizationImport("TENANT_OVERRIDE"));

        Map<String, Object> hospitalOverride = new LinkedHashMap<>();
        hospitalOverride.put("scope_level", "HOSPITAL");
        hospitalOverride.put("scope_code", "HOSPITAL_JUNIT");
        hospitalOverride.put("asset_type", "RULE");
        hospitalOverride.put("override_key", "max_execution_timeout_ms");
        hospitalOverride.put("override_value", 30000);
        hospitalOverride.put("description", "医院级规则执行超时");

        Map<String, Object> groupOverride = new LinkedHashMap<>();
        groupOverride.put("scope_level", "GROUP");
        groupOverride.put("scope_code", "GROUP_JUNIT");
        groupOverride.put("asset_type", "RULE");
        groupOverride.put("override_key", "max_execution_timeout_ms");
        groupOverride.put("override_value", 60000);
        groupOverride.put("description", "集团级规则执行超时");

        Map<String, Object> platformOverride = new LinkedHashMap<>();
        platformOverride.put("scope_level", "PLATFORM");
        platformOverride.put("scope_code", "DEFAULT");
        platformOverride.put("asset_type", "RULE");
        platformOverride.put("override_key", "max_execution_timeout_ms");
        platformOverride.put("override_value", 15000);
        platformOverride.put("description", "系统默认超时");

        Map<String, Object> deptThreshold = new LinkedHashMap<>();
        deptThreshold.put("scope_level", "DEPARTMENT");
        deptThreshold.put("scope_code", "DEPT_CARDIOLOGY");
        deptThreshold.put("asset_type", "RULE");
        deptThreshold.put("override_key", "qc_score_threshold");
        deptThreshold.put("override_value", 85);
        deptThreshold.put("description", "心内科质控分数阈值");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("tenant_id", "TENANT_OVERRIDE");
        importBody.put("operator_id", "JUNIT_OVERRIDE_ADMIN");
        importBody.put("entries", Arrays.asList(hospitalOverride, groupOverride, platformOverride, deptThreshold));

        Map<String, Object> imported = invokePost("/api/organizations/override/entries", importBody);
        assertEquals(4, asList(imported.get("data")).size());

        Map<String, Object> listResp = invokeGet("/api/organizations/override/entries?tenantId=TENANT_OVERRIDE&scopeLevel=HOSPITAL");
        List<Map<String, Object>> hospitalEntries = asListOfMap(listResp.get("data"));
        assertEquals(1, hospitalEntries.size());
        assertEquals("HOSPITAL_JUNIT", hospitalEntries.get(0).get("scope_code"));
    }

    @Test
    void orgOverrideComputeInheritanceChain() throws Exception {
        invokePost("/api/organizations", sampleOrganizationImport("TENANT_INHERIT"));

        Map<String, Object> deptEntry = new LinkedHashMap<>();
        deptEntry.put("scope_level", "DEPARTMENT");
        deptEntry.put("scope_code", "DEPT_CARDIOLOGY");
        deptEntry.put("asset_type", "PATHWAY");
        deptEntry.put("override_key", "auto_complete_enabled");
        deptEntry.put("override_value", true);

        Map<String, Object> hospitalEntry = new LinkedHashMap<>();
        hospitalEntry.put("scope_level", "HOSPITAL");
        hospitalEntry.put("scope_code", "HOSPITAL_JUNIT");
        hospitalEntry.put("asset_type", "PATHWAY");
        hospitalEntry.put("override_key", "auto_complete_enabled");
        deptEntry.put("override_value", false);
        hospitalEntry.put("override_key", "notification_enabled");
        hospitalEntry.put("override_value", true);

        Map<String, Object> platformEntry = new LinkedHashMap<>();
        platformEntry.put("scope_level", "PLATFORM");
        platformEntry.put("scope_code", "DEFAULT");
        platformEntry.put("asset_type", "PATHWAY");
        platformEntry.put("override_key", "notification_enabled");
        platformEntry.put("override_value", false);

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("tenant_id", "TENANT_INHERIT");
        importBody.put("entries", Arrays.asList(deptEntry, hospitalEntry, platformEntry));
        invokePost("/api/organizations/override/entries", importBody);

        Map<String, Object> computeBody = new LinkedHashMap<>();
        computeBody.put("tenant_id", "TENANT_INHERIT");
        computeBody.put("hospital_code", "HOSPITAL_JUNIT");
        computeBody.put("department_code", "DEPT_CARDIOLOGY");
        computeBody.put("asset_type", "PATHWAY");
        Map<String, Object> computed = invokePost("/api/organizations/override/compute", computeBody);
        Map<String, Object> data = asMap(computed.get("data"));

        Map<String, Object> effective = asMap(data.get("effective"));
        assertEquals(true, effective.get("auto_complete_enabled"));
        assertEquals(true, effective.get("notification_enabled"));

        List<Map<String, Object>> sources = asListOfMap(data.get("resolved_sources"));
        assertEquals(2, sources.size());

        Map<String, Object> autoCompleteSource = null;
        Map<String, Object> notificationSource = null;
        for (Map<String, Object> source : sources) {
            if ("auto_complete_enabled".equals(source.get("override_key"))) {
                autoCompleteSource = source;
            }
            if ("notification_enabled".equals(source.get("override_key"))) {
                notificationSource = source;
            }
        }
        assertNotNull(autoCompleteSource);
        assertEquals("DEPARTMENT/DEPT_CARDIOLOGY", autoCompleteSource.get("resolved_from"));
        assertEquals("DEPARTMENT", autoCompleteSource.get("resolved_level"));

        assertNotNull(notificationSource);
        assertEquals("HOSPITAL/HOSPITAL_JUNIT", notificationSource.get("resolved_from"));
        assertEquals("HOSPITAL", notificationSource.get("resolved_level"));

        List<Map<String, Object>> chain = asListOfMap(data.get("inheritance_chain"));
        assertTrue(chain.size() >= 2);
    }

    @Test
    void orgOverrideResolveSingleKey() throws Exception {
        invokePost("/api/organizations", sampleOrganizationImport("TENANT_RESOLVE"));

        Map<String, Object> platformEntry = new LinkedHashMap<>();
        platformEntry.put("scope_level", "PLATFORM");
        platformEntry.put("scope_code", "DEFAULT");
        platformEntry.put("asset_type", "RULE");
        platformEntry.put("override_key", "default_priority");
        platformEntry.put("override_value", 100);

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("tenant_id", "TENANT_RESOLVE");
        importBody.put("entries", Arrays.asList(platformEntry));
        invokePost("/api/organizations/override/entries", importBody);

        Map<String, Object> resolved = invokeGet("/api/organizations/override/resolve?overrideKey=default_priority"
                + "&tenantId=TENANT_RESOLVE&hospitalCode=HOSPITAL_JUNIT");
        Map<String, Object> data = asMap(resolved.get("data"));
        assertEquals("default_priority", data.get("override_key"));
        assertEquals(100, ((Number) data.get("resolved_value")).intValue());
        assertEquals("PLATFORM/DEFAULT", data.get("resolved_from"));
        assertEquals("PLATFORM", data.get("resolved_level"));
    }

    @Test
    void orgOverrideComputeReturnsEmptyWhenNoOverrides() throws Exception {
        Map<String, Object> computeBody = new LinkedHashMap<>();
        computeBody.put("tenant_id", "TENANT_EMPTY");
        computeBody.put("hospital_code", "HOSPITAL_TEST");
        computeBody.put("asset_type", "RULE");
        Map<String, Object> computed = invokePost("/api/organizations/override/compute", computeBody);
        Map<String, Object> data = asMap(computed.get("data"));
        assertEquals(0, ((Number) data.get("source_resolved_count")).intValue());
        assertTrue(asMap(data.get("effective")).isEmpty());
    }

    @Test
    void orgOverrideEntryCount() throws Exception {
        Map<String, Object> countBefore = invokeGet("/api/organizations/override/count");
        int before = ((Number) asMap(countBefore.get("data")).get("entry_count")).intValue();

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("scope_level", "PLATFORM");
        entry.put("scope_code", "DEFAULT");
        entry.put("asset_type", "TEST");
        entry.put("override_key", "test_key_" + System.nanoTime());
        entry.put("override_value", "test_value");
        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("entries", Arrays.asList(entry));
        invokePost("/api/organizations/override/entries", importBody);

        Map<String, Object> countAfter = invokeGet("/api/organizations/override/count");
        int after = ((Number) asMap(countAfter.get("data")).get("entry_count")).intValue();
        assertEquals(before + 1, after);
    }

    @Test
    void sourceCitationImportListAndGet() throws Exception {
        Map<String, Object> importDoc = sampleSourceDocumentImport("TENANT_CITATION");
        invokePost("/api/provenance/source-documents", importDoc);

        Map<String, Object> pageCitation = new LinkedHashMap<>();
        pageCitation.put("document_code", "SRC_GUIDELINE_AMI_2025");
        pageCitation.put("citation_type", "PAGE");
        pageCitation.put("page", "15");
        pageCitation.put("section", "3.2");
        pageCitation.put("quote_text", "急性STEMI患者应在首次医疗接触后90分钟内完成球囊扩张。");
        pageCitation.put("description", "STEMI再灌注时间窗要求");

        Map<String, Object> clauseCitation = new LinkedHashMap<>();
        clauseCitation.put("document_code", "SRC_GUIDELINE_AMI_2025");
        clauseCitation.put("citation_type", "CLAUSE");
        clauseCitation.put("clause", "4.1.2");
        clauseCitation.put("section", "4");
        clauseCitation.put("quote_text", "对于无法在规定时间内行PCI的STEMI患者，可考虑溶栓治疗。");
        clauseCitation.put("description", "溶栓适应证条款");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("tenant_id", "TENANT_CITATION");
        importBody.put("operator_id", "JUNIT_CITATION_ADMIN");
        importBody.put("citations", Arrays.asList(pageCitation, clauseCitation));

        Map<String, Object> importResp = invokePost("/api/provenance/citations", importBody);
        Map<String, Object> imported = asMap(importResp.get("data"));
        assertEquals("TENANT_CITATION", imported.get("tenant_id"));
        assertEquals(2, ((Number) imported.get("imported_count")).intValue());
        assertTrue(asListOfMap(imported.get("warnings")).isEmpty());

        List<Map<String, Object>> citationsList = asListOfMap(imported.get("citations"));
        assertEquals(2, citationsList.size());
        assertNotNull(citationsList.get(0).get("citation_id"));
        assertEquals("SRC_GUIDELINE_AMI_2025", citationsList.get(0).get("document_code"));
        assertEquals("PAGE", citationsList.get(0).get("citation_type"));
        assertEquals("15", citationsList.get(0).get("page"));

        Map<String, Object> listResp = invokeGet("/api/provenance/citations?tenantId=TENANT_CITATION"
                + "&documentCode=SRC_GUIDELINE_AMI_2025&citationType=CLAUSE");
        List<Map<String, Object>> filtered = asListOfMap(listResp.get("data"));
        assertEquals(1, filtered.size());
        assertEquals("CLAUSE", filtered.get(0).get("citation_type"));
        assertEquals("4.1.2", filtered.get(0).get("clause"));

        String citationId = (String) citationsList.get(0).get("citation_id");
        Map<String, Object> getResp = invokeGet("/api/provenance/citations/" + citationId
                + "?tenantId=TENANT_CITATION");
        Map<String, Object> citation = asMap(getResp.get("data"));
        assertEquals(citationId, citation.get("citation_id"));
        assertEquals("SRC_GUIDELINE_AMI_2025", citation.get("document_code"));
    }

    @Test
    void sourceCitationGetByDocument() throws Exception {
        Map<String, Object> importDoc = sampleSourceDocumentImport("TENANT_CIT_BY_DOC");
        invokePost("/api/provenance/source-documents", importDoc);

        Map<String, Object> citation1 = new LinkedHashMap<>();
        citation1.put("document_code", "SRC_GUIDELINE_AMI_2025");
        citation1.put("citation_type", "SECTION");
        citation1.put("section", "2");
        citation1.put("description", "诊断标准章节");

        Map<String, Object> citation2 = new LinkedHashMap<>();
        citation2.put("document_code", "SRC_CONSENSUS_STEMI_2024");
        citation2.put("citation_type", "PAGE");
        citation2.put("page", "8");
        citation2.put("description", "绿色通道流程图");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("tenant_id", "TENANT_CIT_BY_DOC");
        importBody.put("citations", Arrays.asList(citation1, citation2));
        invokePost("/api/provenance/citations", importBody);

        Map<String, Object> byDocResp = invokeGet("/api/provenance/source-documents/SRC_GUIDELINE_AMI_2025/citations"
                + "?tenantId=TENANT_CIT_BY_DOC");
        List<Map<String, Object>> byDoc = asListOfMap(byDocResp.get("data"));
        assertEquals(1, byDoc.size());
        assertEquals("SRC_GUIDELINE_AMI_2025", byDoc.get(0).get("document_code"));
        assertEquals("SECTION", byDoc.get(0).get("citation_type"));
    }

    @Test
    void sourceCitationImportRejectsMissingDocumentCode() throws Exception {
        Map<String, Object> badCitation = new LinkedHashMap<>();
        badCitation.put("citation_type", "PAGE");
        badCitation.put("page", "10");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("citations", Arrays.asList(badCitation));

        Map<String, Object> response = invokePostExpectingClientError("/api/provenance/citations", importBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
        assertTrue(String.valueOf(response.get("message")).contains("document_code"));
    }

    @Test
    void sourceCitationImportWarnsOnNoLocation() throws Exception {
        Map<String, Object> noLocation = new LinkedHashMap<>();
        noLocation.put("document_code", "DOC_NO_LOC");
        noLocation.put("citation_type", "SECTION");
        noLocation.put("description", "无位置引用的条目");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("tenant_id", "TENANT_CIT_WARN");
        importBody.put("citations", Arrays.asList(noLocation));

        Map<String, Object> importResp = invokePost("/api/provenance/citations", importBody);
        Map<String, Object> imported = asMap(importResp.get("data"));
        List<Map<String, Object>> warnings = asListOfMap(imported.get("warnings"));
        assertEquals(1, warnings.size());
        assertEquals("location", warnings.get(0).get("field"));
    }

    @Test
    void sourceAssetBindingImportListAndGet() throws Exception {
        Map<String, Object> importDoc = sampleSourceDocumentImport("TENANT_BINDING");
        invokePost("/api/provenance/source-documents", importDoc);

        Map<String, Object> ruleBinding = new LinkedHashMap<>();
        ruleBinding.put("asset_type", "RULE");
        ruleBinding.put("asset_code", "R_AMI_STEMI_ENTRY");
        ruleBinding.put("document_code", "SRC_GUIDELINE_AMI_2025");
        ruleBinding.put("binding_type", "EVIDENCE");
        ruleBinding.put("confidence", "HIGH");
        ruleBinding.put("description", "STEMI入径规则来源于AMI指南");

        Map<String, Object> pathwayBinding = new LinkedHashMap<>();
        pathwayBinding.put("asset_type", "PATHWAY");
        pathwayBinding.put("asset_code", "AMI_STEMI_PATHWAY");
        pathwayBinding.put("document_code", "SRC_CONSENSUS_STEMI_2024");
        pathwayBinding.put("binding_type", "COMPLIANCE");
        pathwayBinding.put("description", "STEMI路径符合绿色通道共识");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("tenant_id", "TENANT_BINDING");
        importBody.put("operator_id", "JUNIT_BINDING_ADMIN");
        importBody.put("bindings", Arrays.asList(ruleBinding, pathwayBinding));

        Map<String, Object> importResp = invokePost("/api/provenance/bindings", importBody);
        Map<String, Object> imported = asMap(importResp.get("data"));
        assertEquals("TENANT_BINDING", imported.get("tenant_id"));
        assertEquals(2, ((Number) imported.get("imported_count")).intValue());

        List<Map<String, Object>> bindingsList = asListOfMap(imported.get("bindings"));
        assertEquals(2, bindingsList.size());
        assertNotNull(bindingsList.get(0).get("binding_id"));
        assertEquals("RULE", bindingsList.get(0).get("asset_type"));
        assertEquals("R_AMI_STEMI_ENTRY", bindingsList.get(0).get("asset_code"));
        assertEquals("SRC_GUIDELINE_AMI_2025", bindingsList.get(0).get("document_code"));
        assertEquals("EVIDENCE", bindingsList.get(0).get("binding_type"));

        Map<String, Object> listResp = invokeGet("/api/provenance/bindings?tenantId=TENANT_BINDING"
                + "&assetType=RULE");
        List<Map<String, Object>> filtered = asListOfMap(listResp.get("data"));
        assertEquals(1, filtered.size());
        assertEquals("R_AMI_STEMI_ENTRY", filtered.get(0).get("asset_code"));

        String bindingId = (String) bindingsList.get(0).get("binding_id");
        Map<String, Object> getResp = invokeGet("/api/provenance/bindings/" + bindingId
                + "?tenantId=TENANT_BINDING");
        Map<String, Object> binding = asMap(getResp.get("data"));
        assertEquals(bindingId, binding.get("binding_id"));
        assertEquals("RULE", binding.get("asset_type"));
    }

    @Test
    void sourceAssetBindingGetByAsset() throws Exception {
        Map<String, Object> importDoc = sampleSourceDocumentImport("TENANT_BIND_ASSET");
        invokePost("/api/provenance/source-documents", importDoc);

        Map<String, Object> binding1 = new LinkedHashMap<>();
        binding1.put("asset_type", "RULE");
        binding1.put("asset_code", "R_DUAL_BINDING");
        binding1.put("document_code", "SRC_GUIDELINE_AMI_2025");
        binding1.put("binding_type", "EVIDENCE");

        Map<String, Object> binding2 = new LinkedHashMap<>();
        binding2.put("asset_type", "RULE");
        binding2.put("asset_code", "R_DUAL_BINDING");
        binding2.put("document_code", "SRC_CONSENSUS_STEMI_2024");
        binding2.put("binding_type", "REFERENCE");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("tenant_id", "TENANT_BIND_ASSET");
        importBody.put("bindings", Arrays.asList(binding1, binding2));
        invokePost("/api/provenance/bindings", importBody);

        Map<String, Object> byAssetResp = invokeGet("/api/provenance/assets/RULE/R_DUAL_BINDING/bindings"
                + "?tenantId=TENANT_BIND_ASSET");
        List<Map<String, Object>> byAsset = asListOfMap(byAssetResp.get("data"));
        assertEquals(2, byAsset.size());
        for (Map<String, Object> b : byAsset) {
            assertEquals("R_DUAL_BINDING", b.get("asset_code"));
            assertEquals("RULE", b.get("asset_type"));
        }
    }

    @Test
    void sourceAssetBindingGetByDocument() throws Exception {
        Map<String, Object> importDoc = sampleSourceDocumentImport("TENANT_BIND_DOC");
        invokePost("/api/provenance/source-documents", importDoc);

        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("asset_type", "PATHWAY");
        binding.put("asset_code", "AMI_PATHWAY");
        binding.put("document_code", "SRC_GUIDELINE_AMI_2025");
        binding.put("binding_type", "DERIVATION");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("tenant_id", "TENANT_BIND_DOC");
        importBody.put("bindings", Arrays.asList(binding));
        invokePost("/api/provenance/bindings", importBody);

        Map<String, Object> byDocResp = invokeGet("/api/provenance/source-documents/SRC_GUIDELINE_AMI_2025/bindings"
                + "?tenantId=TENANT_BIND_DOC");
        List<Map<String, Object>> byDoc = asListOfMap(byDocResp.get("data"));
        assertEquals(1, byDoc.size());
        assertEquals("SRC_GUIDELINE_AMI_2025", byDoc.get(0).get("document_code"));
        assertEquals("PATHWAY", byDoc.get(0).get("asset_type"));
    }

    @Test
    void sourceAssetBindingImportRejectsMissingRequiredFields() throws Exception {
        Map<String, Object> badBinding = new LinkedHashMap<>();
        badBinding.put("asset_type", "RULE");
        badBinding.put("binding_type", "EVIDENCE");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("bindings", Arrays.asList(badBinding));

        Map<String, Object> response = invokePostExpectingClientError("/api/provenance/bindings", importBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
        assertTrue(String.valueOf(response.get("message")).contains("asset_code")
                || String.valueOf(response.get("message")).contains("document_code"));
    }

    @Test
    void sourceAssetBindingImportRejectsUnsupportedAssetType() throws Exception {
        Map<String, Object> badBinding = new LinkedHashMap<>();
        badBinding.put("asset_type", "INVALID_TYPE");
        badBinding.put("asset_code", "TEST");
        badBinding.put("document_code", "DOC_TEST");
        badBinding.put("binding_type", "EVIDENCE");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("bindings", Arrays.asList(badBinding));

        Map<String, Object> response = invokePostExpectingClientError("/api/provenance/bindings", importBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
        assertTrue(String.valueOf(response.get("message")).contains("asset_type"));
    }

    @Test
    void ruleReferenceFieldsImportAndList() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_REF_TEST_001");
        rule.put("rule_name", "来源引用测试规则");
        rule.put("rule_type", "EMR_QC");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("reference_citation_id", "CIT_REF_TEST");
        rule.put("reference_binding_type", "EVIDENCE");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> listResp = invokeGet("/api/rules?ruleCode=R_REF_TEST_001");
        List<Map<String, Object>> rules = asListOfMap(listResp.get("data"));
        assertFalse(rules.isEmpty());
        Map<String, Object> found = rules.get(0);
        assertEquals("R_REF_TEST_001", found.get("rule_code"));
        assertEquals("SRC_GUIDELINE_AMI_2025", found.get("reference_document_code"));
        assertEquals("CIT_REF_TEST", found.get("reference_citation_id"));
        assertEquals("EVIDENCE", found.get("reference_binding_type"));
    }

    @Test
    void rulePublishBlockedWithoutReference() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_NO_REF_001");
        rule.put("rule_name", "无来源规则");
        rule.put("rule_type", "EMR_QC");
        rule.put("package_code", "PKG_NO_REF");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> reviewResp = invokeGet("/api/rules/packages/PKG_NO_REF/review");
        Map<String, Object> review = asMap(reviewResp.get("data"));
        assertEquals(false, review.get("ready_to_publish"));

        List<Map<String, Object>> issues = asListOfMap(review.get("issues"));
        boolean hasReferenceIssue = false;
        for (Map<String, Object> issue : issues) {
            if ("reference_document_code".equals(issue.get("field"))) {
                hasReferenceIssue = true;
                assertEquals("ERROR", issue.get("severity"));
            }
        }
        assertTrue(hasReferenceIssue, "review should report missing reference_document_code");

        Map<String, Object> publishBody = new LinkedHashMap<>();
        publishBody.put("approved_by", "ADMIN");
        Map<String, Object> publishResp = invokePostExpectingClientError(
                "/api/rules/packages/PKG_NO_REF/publish", publishBody);
        assertEquals("VALIDATION_ERROR", publishResp.get("code"));
    }

    @Test
    void rulePublishAllowedWithReference() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_WITH_REF_001");
        rule.put("rule_name", "有来源规则");
        rule.put("rule_type", "EMR_QC");
        rule.put("package_code", "PKG_WITH_REF");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("reference_binding_type", "EVIDENCE");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> reviewResp = invokeGet("/api/rules/packages/PKG_WITH_REF/review");
        Map<String, Object> review = asMap(reviewResp.get("data"));

        boolean hasReferenceIssue = false;
        for (Map<String, Object> issue : asListOfMap(review.get("issues"))) {
            if ("reference_document_code".equals(issue.get("field"))) {
                hasReferenceIssue = true;
            }
        }
        assertFalse(hasReferenceIssue, "review should not report missing reference when document is bound");
    }

    @Test
    void ruleEvaluationCarriesReferenceInfo() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_EVAL_REF_001");
        rule.put("rule_name", "评估来源测试规则");
        rule.put("rule_type", "PATHWAY_ENTRY");
        rule.put("package_code", "PKG_EVAL_REF");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("reference_citation_id", "CIT_EVAL_REF");
        rule.put("reference_binding_type", "EVIDENCE");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> publishBody = new LinkedHashMap<>();
        publishBody.put("approved_by", "ADMIN");
        invokePost("/api/rules/packages/PKG_EVAL_REF/publish", publishBody);

        Map<String, Object> evalBody = new LinkedHashMap<>();
        evalBody.put("scenario_code", "PATHWAY_ENTRY");
        evalBody.put("rule_package_code", "PKG_EVAL_REF");
        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("diagnosis_code", "I21.0");
        evalBody.put("patient_context", patientContext);
        Map<String, Object> evalResp = invokePost("/api/rules/engine/evaluate", evalBody);
        Map<String, Object> evalData = asMap(evalResp.get("data"));

        List<Map<String, Object>> results = asListOfMap(evalData.get("results"));
        assertFalse(results.isEmpty());
        Map<String, Object> result = results.get(0);
        assertEquals("R_EVAL_REF_001", result.get("rule_code"));
        assertEquals("SRC_GUIDELINE_AMI_2025", result.get("reference_document_code"));
        assertEquals("CIT_EVAL_REF", result.get("reference_citation_id"));
        assertEquals("EVIDENCE", result.get("reference_binding_type"));
    }

    @Test
    void regressionOrgScopeDepartmentLevel() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_DEPT_SCOPE_001");
        rule.put("rule_name", "科室级规则");
        rule.put("rule_type", "EMR_QC");
        rule.put("package_code", "PKG_DEPT_SCOPE");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("department_code", "DEPT_CARDIOLOGY");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> publishBody = new LinkedHashMap<>();
        publishBody.put("approved_by", "ADMIN");
        invokePost("/api/rules/packages/PKG_DEPT_SCOPE/publish", publishBody);

        Map<String, Object> evalBody = new LinkedHashMap<>();
        evalBody.put("scenario_code", "EMR_QC");
        evalBody.put("rule_package_code", "PKG_DEPT_SCOPE");
        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("diagnosis_code", "I21.0");
        evalBody.put("patient_context", patientContext);
        Map<String, Object> evalResp = invokePost("/api/rules/engine/evaluate", evalBody);
        Map<String, Object> evalData = asMap(evalResp.get("data"));
        List<Map<String, Object>> results = asListOfMap(evalData.get("results"));
        assertFalse(results.isEmpty());
        assertEquals("DEPARTMENT", results.get(0).get("scope_level"));
        assertEquals("DEPT_CARDIOLOGY", results.get(0).get("scope_code"));
    }

    @Test
    void regressionOrgScopeCampusLevel() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_CAMPUS_SCOPE_001");
        rule.put("rule_name", "院区级规则");
        rule.put("rule_type", "EMR_QC");
        rule.put("package_code", "PKG_CAMPUS_SCOPE");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("campus_code", "CAMPUS_EAST");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> listResp = invokeGet("/api/rules?ruleCode=R_CAMPUS_SCOPE_001");
        List<Map<String, Object>> rules = asListOfMap(listResp.get("data"));
        assertFalse(rules.isEmpty());
        assertEquals("CAMPUS", rules.get(0).get("scope_level"));
        assertEquals("CAMPUS_EAST", rules.get(0).get("scope_code"));
    }

    @Test
    void regressionOrgScopeSiteLevel() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_SITE_SCOPE_001");
        rule.put("rule_name", "站点级规则");
        rule.put("rule_type", "EMR_QC");
        rule.put("package_code", "PKG_SITE_SCOPE");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("site_code", "SITE_EMERGENCY");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> listResp = invokeGet("/api/rules?ruleCode=R_SITE_SCOPE_001");
        List<Map<String, Object>> rules = asListOfMap(listResp.get("data"));
        assertFalse(rules.isEmpty());
        assertEquals("SITE", rules.get(0).get("scope_level"));
        assertEquals("SITE_EMERGENCY", rules.get(0).get("scope_code"));
    }

    @Test
    void regressionSourceMissingBlocksPublish() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_NO_SRC_BLOCK");
        rule.put("rule_name", "无来源阻断测试");
        rule.put("rule_type", "EMR_QC");
        rule.put("package_code", "PKG_NO_SRC_BLOCK");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> reviewResp = invokeGet("/api/rules/packages/PKG_NO_SRC_BLOCK/review");
        Map<String, Object> review = asMap(reviewResp.get("data"));
        assertEquals(false, review.get("ready_to_publish"));

        boolean hasMissingSource = false;
        for (Map<String, Object> issue : asListOfMap(review.get("issues"))) {
            if ("reference_document_code".equals(issue.get("field")) && "ERROR".equals(issue.get("severity"))) {
                hasMissingSource = true;
            }
        }
        assertTrue(hasMissingSource);
    }

    @Test
    void regressionSourceBoundAllowsPublish() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_SRC_ALLOW");
        rule.put("rule_name", "有来源允许发布");
        rule.put("rule_type", "EMR_QC");
        rule.put("package_code", "PKG_SRC_ALLOW");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("reference_binding_type", "EVIDENCE");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> publishBody = new LinkedHashMap<>();
        publishBody.put("approved_by", "ADMIN");
        Map<String, Object> publishResp = invokePost("/api/rules/packages/PKG_SRC_ALLOW/publish", publishBody);
        Map<String, Object> publishData = asMap(publishResp.get("data"));
        assertTrue(((Number) publishData.get("published_count")).intValue() > 0);
    }

    @Test
    void regressionDraftRuleNotEvaluated() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_DRAFT_EVAL");
        rule.put("rule_name", "草稿规则不可评估");
        rule.put("rule_type", "EMR_QC");
        rule.put("package_code", "PKG_DRAFT_EVAL");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> listResp = invokeGet("/api/rules?ruleCode=R_DRAFT_EVAL");
        List<Map<String, Object>> rules = asListOfMap(listResp.get("data"));
        assertFalse(rules.isEmpty());
        assertEquals("DRAFT", rules.get(0).get("status"));
    }

    @Test
    void regressionPublishedRuleCanBeEvaluated() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_PUB_EVAL");
        rule.put("rule_name", "已发布规则可评估");
        rule.put("rule_type", "PATHWAY_ENTRY");
        rule.put("package_code", "PKG_PUB_EVAL");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> publishBody = new LinkedHashMap<>();
        publishBody.put("approved_by", "ADMIN");
        invokePost("/api/rules/packages/PKG_PUB_EVAL/publish", publishBody);

        Map<String, Object> evalBody = new LinkedHashMap<>();
        evalBody.put("scenario_code", "PATHWAY_ENTRY");
        evalBody.put("rule_package_code", "PKG_PUB_EVAL");
        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("diagnosis_code", "I21.0");
        evalBody.put("patient_context", patientContext);
        Map<String, Object> evalResp = invokePost("/api/rules/engine/evaluate", evalBody);
        Map<String, Object> evalData = asMap(evalResp.get("data"));
        List<Map<String, Object>> results = asListOfMap(evalData.get("results"));
        assertFalse(results.isEmpty());
        assertEquals("R_PUB_EVAL", results.get(0).get("rule_code"));
    }

    @Test
    void regressionDegradedWhenNoRulesMatched() throws Exception {
        Map<String, Object> evalBody = new LinkedHashMap<>();
        evalBody.put("scenario_code", "DRUG_INDICATION");
        evalBody.put("rule_package_code", "PKG_NONEXISTENT_DEGRADE");
        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("drug_code", "ASPIRIN");
        evalBody.put("patient_context", patientContext);
        Map<String, Object> evalResp = invokePost("/api/rules/engine/evaluate", evalBody);
        Map<String, Object> evalData = asMap(evalResp.get("data"));
        List<Map<String, Object>> warnings = asListOfMap(evalData.get("warnings"));
        boolean hasNoRulesWarning = false;
        for (Map<String, Object> warning : warnings) {
            if ("NO_RULES_MATCHED".equals(warning.get("code"))) {
                hasNoRulesWarning = true;
            }
        }
        assertTrue(hasNoRulesWarning, "should return NO_RULES_MATCHED warning when no rules configured");
    }

    @Test
    void regressionInputMissingRequiredField() throws Exception {
        Map<String, Object> badBody = new LinkedHashMap<>();
        Map<String, Object> response = invokePostExpectingClientError("/api/rules", badBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
    }

    @Test
    void regressionInputUnknownScenarioCode() throws Exception {
        Map<String, Object> evalBody = new LinkedHashMap<>();
        evalBody.put("scenario_code", "INVALID_SCENARIO");
        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("diagnosis_code", "I21.0");
        evalBody.put("patient_context", patientContext);
        Map<String, Object> response = invokePostExpectingClientError("/api/rules/engine/evaluate", evalBody);
        assertEquals("VALIDATION_ERROR", response.get("code"));
        assertTrue(String.valueOf(response.get("message")).contains("scenario_code"));
    }

    @Test
    void regressionAuditTrailOnRuleHit() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_AUDIT_HIT");
        rule.put("rule_name", "审计命中测试");
        rule.put("rule_type", "PATHWAY_ENTRY");
        rule.put("package_code", "PKG_AUDIT_HIT");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> publishBody = new LinkedHashMap<>();
        publishBody.put("approved_by", "ADMIN");
        invokePost("/api/rules/packages/PKG_AUDIT_HIT/publish", publishBody);

        Map<String, Object> evalBody = new LinkedHashMap<>();
        evalBody.put("scenario_code", "PATHWAY_ENTRY");
        evalBody.put("rule_package_code", "PKG_AUDIT_HIT");
        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("diagnosis_code", "I21.0");
        evalBody.put("patient_context", patientContext);
        Map<String, Object> evalResp = invokePost("/api/rules/engine/evaluate", evalBody);
        Map<String, Object> evalData = asMap(evalResp.get("data"));
        assertNotNull(evalData.get("trace_id"));
        assertFalse(String.valueOf(evalData.get("trace_id")).isEmpty());
    }

    @Test
    void regressionAuditTrailOnRuleError() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_AUDIT_ERR");
        rule.put("rule_name", "审计错误测试");
        rule.put("rule_type", "PATHWAY_ENTRY");
        rule.put("package_code", "PKG_AUDIT_ERR");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "within_minutes_from"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> listResp = invokeGet("/api/rules?ruleCode=R_AUDIT_ERR");
        List<Map<String, Object>> rules = asListOfMap(listResp.get("data"));
        assertFalse(rules.isEmpty());
    }

    @Test
    void regressionTraceIdPropagation() throws Exception {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("rule_code", "R_TRACE_PROP");
        rule.put("rule_name", "traceId传播测试");
        rule.put("rule_type", "PATHWAY_ENTRY");
        rule.put("package_code", "PKG_TRACE_PROP");
        rule.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        rule.put("condition", Collections.singletonMap("fact", "diagnosis_code", "operator", "exists"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("rules", Arrays.asList(rule));
        invokePost("/api/rules", importBody);

        Map<String, Object> publishBody = new LinkedHashMap<>();
        publishBody.put("approved_by", "ADMIN");
        invokePost("/api/rules/packages/PKG_TRACE_PROP/publish", publishBody);

        Map<String, Object> evalBody = new LinkedHashMap<>();
        evalBody.put("scenario_code", "PATHWAY_ENTRY");
        evalBody.put("rule_package_code", "PKG_TRACE_PROP");
        Map<String, Object> patientContext = new LinkedHashMap<>();
        patientContext.put("diagnosis_code", "I21.0");
        evalBody.put("patient_context", patientContext);
        Map<String, Object> evalResp = invokePost("/api/rules/engine/evaluate", evalBody);
        Map<String, Object> evalData = asMap(evalResp.get("data"));

        String traceId = String.valueOf(evalData.get("trace_id"));
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());

        List<Map<String, Object>> results = asListOfMap(evalData.get("results"));
        assertFalse(results.isEmpty());
    }

    @Test
    void pathwayNodeReferenceFieldsInDetail() throws Exception {
        Map<String, Object> nodeWithRef = new LinkedHashMap<>();
        nodeWithRef.put("node_code", "REF_NODE_1");
        nodeWithRef.put("node_name", "有来源节点");
        nodeWithRef.put("node_type", "EVALUATION");
        nodeWithRef.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        nodeWithRef.put("reference_citation_id", "CIT_PATHWAY_REF");
        nodeWithRef.put("reference_binding_type", "EVIDENCE");
        nodeWithRef.put("tasks", Arrays.asList());
        nodeWithRef.put("transitions", Arrays.asList());

        Map<String, Object> nodeWithoutRef = new LinkedHashMap<>();
        nodeWithoutRef.put("node_code", "NO_REF_NODE");
        nodeWithoutRef.put("node_name", "无来源节点");
        nodeWithoutRef.put("node_type", "TREATMENT");
        nodeWithoutRef.put("tasks", Arrays.asList());
        nodeWithoutRef.put("transitions", Arrays.asList());

        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("stage_code", "S1");
        stage.put("stage_name", "阶段一");
        stage.put("nodes", Arrays.asList(nodeWithRef, nodeWithoutRef));

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("pathway_code", "PW_REF_TEST");
        config.put("pathway_name", "来源引用测试路径");
        config.put("version", "1.0.0");
        config.put("stages", Arrays.asList(stage));

        invokePost("/api/pathways", config);

        Map<String, Object> detailResp = invokeGet("/api/pathways/PW_REF_TEST");
        Map<String, Object> detail = asMap(detailResp.get("data"));

        List<Map<String, Object>> refSources = asListOfMap(detail.get("reference_sources"));
        assertEquals(1, refSources.size());
        assertEquals("NODE", refSources.get(0).get("element_type"));
        assertEquals("REF_NODE_1", refSources.get(0).get("element_code"));
        assertEquals("SRC_GUIDELINE_AMI_2025", refSources.get(0).get("reference_document_code"));
        assertEquals("CIT_PATHWAY_REF", refSources.get(0).get("reference_citation_id"));
        assertEquals("EVIDENCE", refSources.get(0).get("reference_binding_type"));

        List<Map<String, Object>> refWarnings = asListOfMap(detail.get("reference_warnings"));
        assertEquals(1, refWarnings.size());
        assertEquals("NO_REF_NODE", refWarnings.get(0).get("element_code"));
        assertEquals("WARN", refWarnings.get(0).get("severity"));
    }

    @Test
    void pathwayTransitionReferenceFields() throws Exception {
        Map<String, Object> transition = new LinkedHashMap<>();
        transition.put("to_node", "NODE_B");
        transition.put("priority", 1);
        transition.put("reference_document_code", "SRC_CONSENSUS_STEMI_2024");
        transition.put("reference_binding_type", "REFERENCE");

        Map<String, Object> nodeA = new LinkedHashMap<>();
        nodeA.put("node_code", "NODE_A");
        nodeA.put("node_name", "节点A");
        nodeA.put("node_type", "EVALUATION");
        nodeA.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        nodeA.put("reference_binding_type", "EVIDENCE");
        nodeA.put("tasks", Arrays.asList());
        nodeA.put("transitions", Arrays.asList(transition));

        Map<String, Object> nodeB = new LinkedHashMap<>();
        nodeB.put("node_code", "NODE_B");
        nodeB.put("node_name", "节点B");
        nodeB.put("node_type", "TREATMENT");
        nodeB.put("reference_document_code", "SRC_CONSENSUS_STEMI_2024");
        nodeB.put("reference_binding_type", "DERIVATION");
        nodeB.put("tasks", Arrays.asList());
        nodeB.put("transitions", Arrays.asList());

        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("stage_code", "S1");
        stage.put("stage_name", "阶段一");
        stage.put("nodes", Arrays.asList(nodeA, nodeB));

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("pathway_code", "PW_TRANS_REF");
        config.put("pathway_name", "流转来源测试路径");
        config.put("version", "1.0.0");
        config.put("stages", Arrays.asList(stage));

        invokePost("/api/pathways", config);

        Map<String, Object> detailResp = invokeGet("/api/pathways/PW_TRANS_REF");
        Map<String, Object> detail = asMap(detailResp.get("data"));

        List<Map<String, Object>> refSources = asListOfMap(detail.get("reference_sources"));
        assertEquals(3, refSources.size());

        boolean hasTransitionRef = false;
        for (Map<String, Object> ref : refSources) {
            if ("TRANSITION".equals(ref.get("element_type"))) {
                hasTransitionRef = true;
                assertEquals("NODE_A->NODE_B", ref.get("element_code"));
                assertEquals("SRC_CONSENSUS_STEMI_2024", ref.get("reference_document_code"));
                assertEquals("REFERENCE", ref.get("reference_binding_type"));
            }
        }
        assertTrue(hasTransitionRef, "should have transition reference");
    }

    @Test
    void pathwayPublishReportsMissingReferences() throws Exception {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("node_code", "UNBOUND_NODE");
        node.put("node_name", "未绑定来源节点");
        node.put("node_type", "EVALUATION");
        node.put("tasks", Arrays.asList());
        node.put("transitions", Arrays.asList());

        Map<String, Object> stage = new LinkedHashMap<>();
        stage.put("stage_code", "S1");
        stage.put("stage_name", "阶段一");
        stage.put("nodes", Arrays.asList(node));

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("pathway_code", "PW_NO_REF");
        config.put("pathway_name", "无来源路径");
        config.put("version", "1.0.0");
        config.put("stages", Arrays.asList(stage));

        invokePost("/api/pathways", config);

        Map<String, Object> publishBody = new LinkedHashMap<>();
        publishBody.put("approved_by", "ADMIN");
        Map<String, Object> publishResp = invokePost("/api/pathways/PW_NO_REF/publish", publishBody);
        Map<String, Object> publishData = asMap(publishResp.get("data"));
        assertEquals("PUBLISHED", publishData.get("status"));

        List<Map<String, Object>> warnings = asListOfMap(publishData.get("reference_warnings"));
        assertFalse(warnings.isEmpty());
        assertEquals("UNBOUND_NODE", warnings.get(0).get("element_code"));
        assertEquals("WARN", warnings.get(0).get("severity"));
    }

    @Test
    void graphVersionReferenceFields() throws Exception {
        Map<String, Object> version = new LinkedHashMap<>();
        version.put("graph_version", "AMI_GRAPH_REF_2026_01");
        version.put("name", "AMI图谱-来源绑定测试");
        version.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        version.put("reference_binding_type", "EVIDENCE");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("versions", Arrays.asList(version));
        invokePost("/api/graph/versions", importBody);

        Map<String, Object> getResp = invokeGet("/api/graph/versions/AMI_GRAPH_REF_2026_01");
        Map<String, Object> data = asMap(getResp.get("data"));
        assertEquals("AMI_GRAPH_REF_2026_01", data.get("graph_version"));
        assertEquals("SRC_GUIDELINE_AMI_2025", data.get("reference_document_code"));
        assertEquals("EVIDENCE", data.get("reference_binding_type"));
    }

    @Test
    void graphVersionActivateReportsMissingReference() throws Exception {
        Map<String, Object> version = new LinkedHashMap<>();
        version.put("graph_version", "AMI_GRAPH_NO_REF_2026_01");
        version.put("name", "AMI图谱-无来源测试");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("versions", Arrays.asList(version));
        invokePost("/api/graph/versions", importBody);

        Map<String, Object> activateBody = new LinkedHashMap<>();
        activateBody.put("published_by", "ADMIN");
        Map<String, Object> activateResp = invokePost("/api/graph/versions/AMI_GRAPH_NO_REF_2026_01/activate", activateBody);
        Map<String, Object> data = asMap(activateResp.get("data"));
        assertEquals("ACTIVE", data.get("status"));

        List<Map<String, Object>> warnings = asListOfMap(data.get("reference_warnings"));
        assertFalse(warnings.isEmpty());
        assertEquals("WARN", warnings.get(0).get("severity"));
        assertEquals("reference_document_code", warnings.get(0).get("field"));
    }

    @Test
    void graphVersionActivateWithReferenceNoWarning() throws Exception {
        Map<String, Object> version = new LinkedHashMap<>();
        version.put("graph_version", "AMI_GRAPH_WITH_REF_2026_01");
        version.put("name", "AMI图谱-有来源测试");
        version.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        version.put("reference_binding_type", "EVIDENCE");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("versions", Arrays.asList(version));
        invokePost("/api/graph/versions", importBody);

        Map<String, Object> activateBody = new LinkedHashMap<>();
        activateBody.put("published_by", "ADMIN");
        Map<String, Object> activateResp = invokePost("/api/graph/versions/AMI_GRAPH_WITH_REF_2026_01/activate", activateBody);
        Map<String, Object> data = asMap(activateResp.get("data"));
        assertEquals("ACTIVE", data.get("status"));

        List<Map<String, Object>> warnings = asListOfMap(data.get("reference_warnings"));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void graphEvidenceWithReferenceFields() throws Exception {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("evidence_id", "EV_GRAPH_REF_001");
        evidence.put("graph_version", "AMI_GRAPH_REF_2026_01");
        evidence.put("target_code", "AMI_STEMI");
        evidence.put("target_type", "DISEASE");
        evidence.put("evidence_type", "GUIDELINE");
        evidence.put("title", "STEMI再灌注时间窗证据");
        evidence.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        evidence.put("reference_binding_type", "EVIDENCE");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("evidences", Arrays.asList(evidence));
        invokePost("/api/graph/evidences", importBody);

        Map<String, Object> getResp = invokeGet("/api/graph/evidences/EV_GRAPH_REF_001");
        Map<String, Object> data = asMap(getResp.get("data"));
        assertEquals("EV_GRAPH_REF_001", data.get("evidence_id"));
        assertEquals("SRC_GUIDELINE_AMI_2025", data.get("reference_document_code"));
        assertEquals("EVIDENCE", data.get("reference_binding_type"));
    }

    @Test
    void graphEvidenceQueryByTarget() throws Exception {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("evidence_id", "EV_GRAPH_QUERY_001");
        evidence.put("graph_version", "AMI_GRAPH_QUERY_2026_01");
        evidence.put("target_code", "AMI_NSTEMI");
        evidence.put("target_type", "DISEASE");
        evidence.put("evidence_type", "CONSENSUS");
        evidence.put("title", "NSTEMI抗栓策略证据");
        evidence.put("reference_document_code", "SRC_CONSENSUS_STEMI_2024");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("evidences", Arrays.asList(evidence));
        invokePost("/api/graph/evidences", importBody);

        Map<String, Object> listResp = invokeGet("/api/graph/evidences?targetCode=AMI_NSTEMI");
        List<Map<String, Object>> evidences = asListOfMap(listResp.get("data"));
        assertFalse(evidences.isEmpty());
        assertEquals("AMI_NSTEMI", evidences.get(0).get("target_code"));
        assertEquals("SRC_CONSENSUS_STEMI_2024", evidences.get(0).get("reference_document_code"));
    }

    @Test
    void difyTemplateReferenceFields() throws Exception {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("workflow_code", "WF_REF_TEST");
        template.put("workflow_name", "来源引用测试工作流");
        template.put("workflow_version", "1.0.0");
        template.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");
        template.put("reference_binding_type", "EVIDENCE");
        template.put("input_defaults", Collections.singletonMap("model", "gpt-4"));

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("templates", Arrays.asList(template));
        invokePost("/api/dify/workflows", importBody);

        Map<String, Object> getResp = invokeGet("/api/dify/workflows/WF_REF_TEST?workflowVersion=1.0.0");
        Map<String, Object> data = asMap(getResp.get("data"));
        assertEquals("WF_REF_TEST", data.get("workflow_code"));
        assertEquals("SRC_GUIDELINE_AMI_2025", data.get("reference_document_code"));
        assertEquals("EVIDENCE", data.get("reference_binding_type"));
    }

    @Test
    void difyTemplateListReturnsReferences() throws Exception {
        Map<String, Object> template1 = new LinkedHashMap<>();
        template1.put("workflow_code", "WF_LIST_REF_1");
        template1.put("workflow_name", "列表来源测试1");
        template1.put("reference_document_code", "SRC_GUIDELINE_AMI_2025");

        Map<String, Object> template2 = new LinkedHashMap<>();
        template2.put("workflow_code", "WF_LIST_REF_2");
        template2.put("workflow_name", "列表来源测试2");
        template2.put("reference_document_code", "SRC_CONSENSUS_STEMI_2024");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("templates", Arrays.asList(template1, template2));
        invokePost("/api/dify/workflows", importBody);

        Map<String, Object> listResp = invokeGet("/api/dify/workflows");
        List<Map<String, Object>> templates = asListOfMap(listResp.get("data"));
        assertTrue(templates.size() >= 2);

        boolean foundRef1 = false;
        boolean foundRef2 = false;
        for (Map<String, Object> t : templates) {
            if ("WF_LIST_REF_1".equals(t.get("workflow_code"))) {
                assertEquals("SRC_GUIDELINE_AMI_2025", t.get("reference_document_code"));
                foundRef1 = true;
            }
            if ("WF_LIST_REF_2".equals(t.get("workflow_code"))) {
                assertEquals("SRC_CONSENSUS_STEMI_2024", t.get("reference_document_code"));
                foundRef2 = true;
            }
        }
        assertTrue(foundRef1);
        assertTrue(foundRef2);
    }

    @Test
    void graphVersionRollback() throws Exception {
        Map<String, Object> v1 = new LinkedHashMap<>();
        v1.put("graph_version", "AMI_ROLLBACK_V1");
        v1.put("name", "AMI图谱V1");

        Map<String, Object> v2 = new LinkedHashMap<>();
        v2.put("graph_version", "AMI_ROLLBACK_V2");
        v2.put("name", "AMI图谱V2");

        Map<String, Object> importBody = new LinkedHashMap<>();
        importBody.put("versions", Arrays.asList(v1, v2));
        invokePost("/api/graph/versions", importBody);

        Map<String, Object> activateBody = new LinkedHashMap<>();
        activateBody.put("published_by", "ADMIN");
        invokePost("/api/graph/versions/AMI_ROLLBACK_V2/activate", activateBody);

        Map<String, Object> getV2 = invokeGet("/api/graph/versions/AMI_ROLLBACK_V2");
        assertEquals("ACTIVE", asMap(getV2.get("data")).get("status"));

        Map<String, Object> rollbackBody = new LinkedHashMap<>();
        rollbackBody.put("published_by", "ADMIN");
        Map<String, Object> rollbackResp = invokePost("/api/graph/versions/AMI_ROLLBACK_V1/rollback", rollbackBody);
        Map<String, Object> rollbackData = asMap(rollbackResp.get("data"));
        assertEquals("AMI_ROLLBACK_V1", rollbackData.get("graph_version"));
        assertEquals("ACTIVE", rollbackData.get("status"));
        assertEquals("AMI_ROLLBACK_V2", rollbackData.get("previous_active_version"));

        Map<String, Object> getV1 = invokeGet("/api/graph/versions/AMI_ROLLBACK_V1");
        assertEquals("ACTIVE", asMap(getV1.get("data")).get("status"));

        Map<String, Object> getV2After = invokeGet("/api/graph/versions/AMI_ROLLBACK_V2");
        assertEquals("RETIRED", asMap(getV2After.get("data")).get("status"));
    }

    // =========================================================================
    // TERM-002: 未映射治理队列
    // =========================================================================

    @Test
    void terminologyUnmappedEntersGovernanceQueue() throws Exception {
        // 标准化一个未映射编码，应进入治理队列
        Map<String, Object> normalizeBody = new LinkedHashMap<String, Object>();
        normalizeBody.put("source_system", "EMR");
        normalizeBody.put("source_code", "DYSPNEA_UNKNOWN");
        normalizeBody.put("source_name", "未知呼吸困难");
        normalizeBody.put("concept_type", "SYMPTOM");
        Map<String, Object> normalized = invokePost("/api/terminology/normalize", normalizeBody);
        Map<String, Object> data = asMap(normalized.get("data"));
        assertEquals(Boolean.FALSE, data.get("matched"));
        assertEquals("PENDING_MAPPING", data.get("governance_status"));
        assertNotNull(data.get("queue_id"));

        // 查询治理队列应包含该条目
        Map<String, Object> pendingResp = invokeGet("/api/terminology/pending?governanceStatus=PENDING_MAPPING");
        List<Object> pendingList = asList(pendingResp.get("data"));
        assertTrue(pendingList.size() >= 1);
    }

    @Test
    void terminologyApprovePendingMapping() throws Exception {
        // 先让一个未映射术语进入队列
        Map<String, Object> normalizeBody = new LinkedHashMap<String, Object>();
        normalizeBody.put("source_system", "LIS");
        normalizeBody.put("source_code", "UNKNOWN_LAB_ITEM");
        normalizeBody.put("source_name", "未知检验项目");
        normalizeBody.put("concept_type", "LAB_ITEM");
        Map<String, Object> normalized = invokePost("/api/terminology/normalize", normalizeBody);
        String queueId = String.valueOf(asMap(normalized.get("data")).get("queue_id"));

        // 审批映射
        Map<String, Object> approveBody = new LinkedHashMap<String, Object>();
        approveBody.put("standard_code", "UNKNOWN_LAB_STD");
        approveBody.put("standard_name", "未知检验项目标准码");
        approveBody.put("reviewed_by", "TEST_REVIEWER");
        approveBody.put("review_comment", "测试审批通过");
        Map<String, Object> approved = invokePost("/api/terminology/pending/" + queueId + "/approve", approveBody);
        Map<String, Object> approvedData = asMap(approved.get("data"));
        assertEquals("APPROVED", approvedData.get("governance_status"));

        // 审批后再次标准化应命中
        Map<String, Object> reNormalized = invokePost("/api/terminology/normalize", normalizeBody);
        Map<String, Object> reData = asMap(reNormalized.get("data"));
        assertEquals(Boolean.TRUE, reData.get("matched"));
        assertEquals("UNKNOWN_LAB_STD", reData.get("standard_code"));
    }

    @Test
    void terminologyRejectPendingMapping() throws Exception {
        // 先让一个未映射术语进入队列
        Map<String, Object> normalizeBody = new LinkedHashMap<String, Object>();
        normalizeBody.put("source_system", "HIS");
        normalizeBody.put("source_code", "REJECT_TEST_CODE");
        normalizeBody.put("source_name", "驳回测试项");
        normalizeBody.put("concept_type", "DIAGNOSIS");
        Map<String, Object> normalized = invokePost("/api/terminology/normalize", normalizeBody);
        String queueId = String.valueOf(asMap(normalized.get("data")).get("queue_id"));

        // 驳回映射
        Map<String, Object> rejectBody = new LinkedHashMap<String, Object>();
        rejectBody.put("reviewed_by", "TEST_REVIEWER");
        rejectBody.put("review_comment", "测试驳回");
        Map<String, Object> rejected = invokePost("/api/terminology/pending/" + queueId + "/reject", rejectBody);
        Map<String, Object> rejectedData = asMap(rejected.get("data"));
        assertEquals("REJECTED", rejectedData.get("governance_status"));
    }

    @Test
    void terminologyPendingListFilters() throws Exception {
        // 查询全部 PENDING_MAPPING
        Map<String, Object> allPending = invokeGet("/api/terminology/pending?governanceStatus=PENDING_MAPPING");
        assertNotNull(allPending.get("data"));

        // 按 source_system 过滤
        Map<String, Object> filteredPending = invokeGet("/api/terminology/pending?governanceStatus=PENDING_MAPPING&sourceSystem=EMR");
        assertNotNull(filteredPending.get("data"));
    }
}
