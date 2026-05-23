package com.medkernel.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.graph.GraphService;
import com.medkernel.pathway.PathwayService;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.rule.RuleDefinition;
import com.medkernel.rule.RuleService;
import com.medkernel.terminology.TerminologyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KnowledgePackageServiceTest {

    @Mock
    private EnginePersistenceService persistenceService;

    @Mock
    private RuleService ruleService;

    @Mock
    private TerminologyService terminologyService;

    @Mock
    private PathwayService pathwayService;

    @Mock
    private GraphService graphService;

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private KnowledgePackageRepository packageRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private KnowledgePackageService packageService;

    @BeforeEach
    void setUp() {
        packageService = new KnowledgePackageService(
                persistenceService, ruleService, terminologyService,
                pathwayService, graphService, knowledgeService,
                objectMapper, packageRepository);
    }

    // ==================== exportPackage ====================

    @Test
    void exportPackage_shouldExportWithFullType() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-001");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("FULL");
        pkg.setTenantId(1L);
        pkg.setCreatedBy("admin");

        KnowledgePackage result = packageService.exportPackage(pkg);

        assertEquals("EXPORTED", result.getStatus());
        assertEquals("FULL", result.getExportType());
        assertNotNull(result.getContentJson());
        assertNotNull(result.getContentHash());
        assertTrue(result.getContentHash().startsWith("sha256:"));
        assertEquals("IDLE", result.getSyncStatus());
        assertNotNull(result.getId());
        verify(packageRepository).save(any(KnowledgePackage.class));
    }

    @Test
    void exportPackage_shouldDefaultToFullType() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-002");
        pkg.setPackageVersion("1.0");

        KnowledgePackage result = packageService.exportPackage(pkg);
        assertEquals("FULL", result.getExportType());
    }

    @Test
    void exportPackage_shouldThrowWhenPackageCodeMissing() {
        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageVersion("1.0");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> packageService.exportPackage(pkg));
        assertTrue(ex.getMessage().contains("package_code"));
    }

    @Test
    void exportPackage_shouldThrowWhenPackageVersionMissing() {
        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-003");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> packageService.exportPackage(pkg));
        assertTrue(ex.getMessage().contains("package_version"));
    }

    @Test
    void exportPackage_shouldThrowForInvalidExportType() {
        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-004");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("INVALID");

        assertThrows(IllegalArgumentException.class, () -> packageService.exportPackage(pkg));
    }

    @Test
    void exportPackage_shouldCollectRules() {
        RuleDefinition rule1 = new RuleDefinition();
        rule1.setRuleCode("R001");
        rule1.setRuleName("Rule 1");
        rule1.setRuleType("EMR_QC");
        rule1.setVersionNo("1.0");
        rule1.setPackageCode("PKG-005");
        rule1.setPackageVersion("1.0");
        rule1.setStatus("ACTIVE");
        rule1.setSeverity("HIGH");
        rule1.setEnabled(true);
        rule1.setRuleJson(new LinkedHashMap<String, Object>());
        rule1.setTenantId("1");
        rule1.setHospitalCode("H001");
        rule1.setScopeLevel("HOSPITAL");
        rule1.setScopeCode("H001");

        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(Arrays.asList(rule1));
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-005");
        pkg.setPackageVersion("1.0");
        pkg.setTenantId(1L);

        KnowledgePackage result = packageService.exportPackage(pkg);
        assertEquals(1, result.getRuleCount());
    }

    @Test
    void exportPackage_shouldHandleRuleCollectionFailure() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenThrow(new RuntimeException("DB error"));
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-ERR");
        pkg.setPackageVersion("1.0");

        KnowledgePackage result = packageService.exportPackage(pkg);
        assertEquals(0, result.getRuleCount());
    }

    // ==================== importPackage ====================

    @Test
    void importPackage_shouldThrowWhenPackageIdNull() {
        assertThrows(IllegalArgumentException.class,
                () -> packageService.importPackage(null, "SKIP"));
    }

    @Test
    void importPackage_shouldThrowForInvalidConflictStrategy() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-IMP-001");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("FULL");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        assertThrows(IllegalArgumentException.class,
                () -> packageService.importPackage(exported.getId(), "INVALID"));
    }

    @Test
    void importPackage_shouldImportSuccessfully() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-IMP-002");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("FULL");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        when(ruleService.getRule(anyString(), any())).thenReturn(null);

        Map<String, Object> result = packageService.importPackage(exported.getId(), "SKIP");

        assertEquals(exported.getId(), result.get("package_id"));
        assertEquals("PKG-IMP-002", result.get("package_code"));
        assertEquals("SKIP", result.get("conflict_strategy"));
        assertEquals("IMPORTED", exported.getStatus());
        verify(packageRepository).update(any(KnowledgePackage.class));
    }

    @Test
    void importPackage_shouldThrowWhenPackageNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> packageService.importPackage(99999L, "SKIP"));
    }

    @Test
    void importPackage_shouldDefaultConflictStrategyToSkip() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-IMP-DEF");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("FULL");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        when(ruleService.getRule(anyString(), any())).thenReturn(null);

        Map<String, Object> result = packageService.importPackage(exported.getId(), null);
        assertEquals("SKIP", result.get("conflict_strategy"));
    }

    // ==================== previewImport ====================

    @Test
    void previewImport_shouldReturnDiffAnalysis() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-PRE-001");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("FULL");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        when(ruleService.getRule(anyString(), any())).thenReturn(null);

        Map<String, Object> preview = packageService.previewImport(exported.getId());

        assertNotNull(preview.get("package_id"));
        assertNotNull(preview.get("rules"));
        assertNotNull(preview.get("summary"));
    }

    @Test
    void previewImport_shouldThrowWhenPackageIdNull() {
        assertThrows(IllegalArgumentException.class,
                () -> packageService.previewImport(null));
    }

    // ==================== listPackages ====================

    @Test
    void listPackages_shouldReturnFromMemoryWhenPersistenceDisabled() {
        when(persistenceService.enabled()).thenReturn(false);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-LIST-001");
        pkg.setPackageVersion("1.0");
        pkg.setTenantId(1L);
        packageService.exportPackage(pkg);

        List<KnowledgePackage> result = packageService.listPackages(1L, null);
        assertFalse(result.isEmpty());
    }

    @Test
    void listPackages_shouldFilterByStatus() {
        when(persistenceService.enabled()).thenReturn(false);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-STATUS-001");
        pkg.setPackageVersion("1.0");
        pkg.setTenantId(1L);
        packageService.exportPackage(pkg);

        List<KnowledgePackage> result = packageService.listPackages(1L, "EXPORTED");
        assertFalse(result.isEmpty());
        assertEquals("EXPORTED", result.get(0).getStatus());

        List<KnowledgePackage> noResult = packageService.listPackages(1L, "IMPORTED");
        assertTrue(noResult.isEmpty());
    }

    @Test
    void listPackages_shouldFilterByTenantId() {
        when(persistenceService.enabled()).thenReturn(false);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-TENANT-001");
        pkg.setPackageVersion("1.0");
        pkg.setTenantId(1L);
        packageService.exportPackage(pkg);

        List<KnowledgePackage> result = packageService.listPackages(1L, null);
        assertFalse(result.isEmpty());

        List<KnowledgePackage> otherTenant = packageService.listPackages(999L, null);
        assertTrue(otherTenant.isEmpty());
    }

    // ==================== getPackage ====================

    @Test
    void getPackage_shouldReturnPackage() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-GET-001");
        pkg.setPackageVersion("1.0");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        KnowledgePackage found = packageService.getPackage(exported.getId());
        assertNotNull(found);
        assertEquals("PKG-GET-001", found.getPackageCode());
    }

    @Test
    void getPackage_shouldThrowWhenIdNull() {
        assertThrows(IllegalArgumentException.class, () -> packageService.getPackage(null));
    }

    @Test
    void getPackage_shouldThrowWhenNotFound() {
        assertThrows(IllegalArgumentException.class, () -> packageService.getPackage(99999L));
    }

    // ==================== syncPackage ====================

    @Test
    void syncPackage_shouldSyncFullPackage() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-SYNC-001");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("FULL");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        Map<String, Object> result = packageService.syncPackage(exported.getId(), "MANUAL");

        assertEquals("FULL", result.get("sync_type"));
        assertEquals("SYNCED", exported.getSyncStatus());
        assertNotNull(exported.getSyncTime());
        verify(packageRepository, atLeast(2)).update(any(KnowledgePackage.class));
    }

    @Test
    void syncPackage_shouldSyncIncrementalPackage() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-SYNC-INC");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("INCREMENTAL");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        Map<String, Object> result = packageService.syncPackage(exported.getId(), "MANUAL");

        assertEquals("INCREMENTAL", result.get("sync_type"));
        assertEquals("SYNCED", exported.getSyncStatus());
    }

    @Test
    void syncPackage_shouldThrowWhenPackageIdNull() {
        assertThrows(IllegalArgumentException.class,
                () -> packageService.syncPackage(null, "MANUAL"));
    }

    @Test
    void syncPackage_shouldThrowForInvalidSyncMode() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-SYNC-INV");
        pkg.setPackageVersion("1.0");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        assertThrows(IllegalArgumentException.class,
                () -> packageService.syncPackage(exported.getId(), "INVALID"));
    }

    @Test
    void syncPackage_shouldDefaultToManualMode() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-SYNC-DEF");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("FULL");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        Map<String, Object> result = packageService.syncPackage(exported.getId(), null);
        assertEquals("MANUAL", exported.getSyncMode());
    }

    // ==================== getSyncStatus ====================

    @Test
    void getSyncStatus_shouldReturnStatusMap() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-SS-001");
        pkg.setPackageVersion("1.0");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        Map<String, Object> status = packageService.getSyncStatus(exported.getId());

        assertEquals(exported.getId(), status.get("package_id"));
        assertEquals("PKG-SS-001", status.get("package_code"));
        assertNotNull(status.get("sync_status"));
    }

    @Test
    void getSyncStatus_shouldThrowWhenPackageIdNull() {
        assertThrows(IllegalArgumentException.class, () -> packageService.getSyncStatus(null));
    }

    // ==================== importPackage with conflict strategies ====================

    @Test
    void importPackage_withOverwriteStrategyShouldOverwriteExistingRules() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-OVR-001");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("FULL");

        KnowledgePackage exported = packageService.exportPackage(pkg);

        // Manually set content with a rule
        Map<String, Object> content = new LinkedHashMap<String, Object>();
        content.put("package_code", "PKG-OVR-001");
        content.put("package_version", "1.0");
        content.put("export_type", "FULL");

        List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();
        Map<String, Object> ruleMap = new LinkedHashMap<String, Object>();
        ruleMap.put("rule_code", "R001");
        ruleMap.put("rule_name", "Rule 1");
        ruleMap.put("version_no", "1.0");
        rules.add(ruleMap);
        content.put("rules", rules);
        content.put("terminologies", new ArrayList<Map<String, Object>>());
        content.put("pathways", new ArrayList<Map<String, Object>>());
        content.put("graphs", new ArrayList<Map<String, Object>>());
        content.put("sources", new ArrayList<Map<String, Object>>());

        try {
            String contentJson = objectMapper.writeValueAsString(content);
            exported.setContentJson(contentJson);
        } catch (Exception e) {
            fail("JSON serialization failed");
        }

        // Rule exists - should be overwritten
        RuleDefinition existing = new RuleDefinition();
        existing.setRuleCode("R001");
        existing.setStatus("ACTIVE");
        when(ruleService.getRule("R001", "1.0")).thenReturn(existing);
        when(ruleService.importRules(any())).thenReturn(new ArrayList<RuleDefinition>());

        Map<String, Object> result = packageService.importPackage(exported.getId(), "OVERWRITE");

        assertEquals("OVERWRITE", result.get("conflict_strategy"));
        verify(ruleService).importRules(any());
    }

    @Test
    void importPackage_withSkipStrategyShouldSkipExistingRules() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-SKIP-001");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("FULL");

        KnowledgePackage exported = packageService.exportPackage(pkg);

        // Manually set content with a rule
        Map<String, Object> content = new LinkedHashMap<String, Object>();
        content.put("package_code", "PKG-SKIP-001");
        content.put("package_version", "1.0");
        content.put("export_type", "FULL");
        List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();
        Map<String, Object> ruleMap = new LinkedHashMap<String, Object>();
        ruleMap.put("rule_code", "R001");
        ruleMap.put("version_no", "1.0");
        rules.add(ruleMap);
        content.put("rules", rules);
        content.put("terminologies", new ArrayList<Map<String, Object>>());
        content.put("pathways", new ArrayList<Map<String, Object>>());
        content.put("graphs", new ArrayList<Map<String, Object>>());
        content.put("sources", new ArrayList<Map<String, Object>>());

        try {
            exported.setContentJson(objectMapper.writeValueAsString(content));
        } catch (Exception e) {
            fail("JSON serialization failed");
        }

        // Rule exists - should be skipped
        RuleDefinition existing = new RuleDefinition();
        existing.setRuleCode("R001");
        when(ruleService.getRule("R001", "1.0")).thenReturn(existing);

        Map<String, Object> result = packageService.importPackage(exported.getId(), "SKIP");

        // Rule should be skipped, imported_rules = 0
        assertEquals(0, result.get("imported_rules"));
        verify(ruleService, never()).importRules(any());
    }

    // ==================== importPackage not EXPORTED status ====================

    @Test
    void importPackage_shouldThrowWhenNotExportedStatus() {
        when(persistenceService.enabled()).thenReturn(true);
        when(ruleService.listRules()).thenReturn(new ArrayList<RuleDefinition>());
        when(terminologyService.listMappings()).thenReturn(new ArrayList<Map<String, Object>>());
        when(pathwayService.listPathways()).thenReturn(new ArrayList<Map<String, Object>>());
        when(graphService.listGraphVersions()).thenReturn(new ArrayList<Map<String, Object>>());

        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode("PKG-NEXP-001");
        pkg.setPackageVersion("1.0");
        pkg.setExportType("FULL");
        KnowledgePackage exported = packageService.exportPackage(pkg);

        // Import once to change status to IMPORTED
        when(ruleService.getRule(anyString(), any())).thenReturn(null);
        packageService.importPackage(exported.getId(), "SKIP");

        // Try to import again - should fail
        assertThrows(IllegalArgumentException.class,
                () -> packageService.importPackage(exported.getId(), "SKIP"));
    }
}
