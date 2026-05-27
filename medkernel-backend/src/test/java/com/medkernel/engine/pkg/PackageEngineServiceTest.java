package com.medkernel.engine.pkg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.medkernel.engine.evaluation.EvaluationIndicator;
import com.medkernel.engine.evaluation.EvaluationIndicatorRepository;
import com.medkernel.engine.evaluation.EvaluationIndicatorStatus;
import com.medkernel.engine.pathway.PathwayTemplate;
import com.medkernel.engine.pathway.PathwayTemplateRepository;
import com.medkernel.engine.pathway.PathwayTemplateStatus;
import com.medkernel.engine.rule.RuleDefinition;
import com.medkernel.engine.rule.RuleDefinitionRepository;
import com.medkernel.engine.rule.RuleDefinitionStatus;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PackageEngineServiceTest {

    private KnowledgePackageRepository packageRepository;
    private PackageItemRepository itemRepository;
    private ReleasePlanRepository planRepository;
    private SyncTargetRepository targetRepository;
    private SyncLogRepository logRepository;

    private RuleDefinitionRepository ruleRepository;
    private PathwayTemplateRepository pathwayRepository;
    private EvaluationIndicatorRepository evaluationRepository;

    private PackageSyncPort syncPort;
    private AuditEventPublisher auditPublisher;

    private PackageEngineService service;

    @BeforeEach
    void setUp() {
        packageRepository = mock(KnowledgePackageRepository.class);
        itemRepository = mock(PackageItemRepository.class);
        planRepository = mock(ReleasePlanRepository.class);
        targetRepository = mock(SyncTargetRepository.class);
        logRepository = mock(SyncLogRepository.class);

        ruleRepository = mock(RuleDefinitionRepository.class);
        pathwayRepository = mock(PathwayTemplateRepository.class);
        evaluationRepository = mock(EvaluationIndicatorRepository.class);

        syncPort = mock(PackageSyncPort.class);
        auditPublisher = mock(AuditEventPublisher.class);

        service = new PackageEngineService(
            packageRepository, itemRepository, planRepository, targetRepository, logRepository,
            ruleRepository, pathwayRepository, evaluationRepository, syncPort, auditPublisher
        );

        when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(targetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequestContext.restore(new RequestContext.Snapshot(
            "trace-pkg", OrgScope.tenant("tenant-A"), "tester"));
    }

    @AfterEach
    void clear() {
        RequestContext.clear();
    }

    @Test
    void createPackageSucceedsAndPersists() {
        when(packageRepository.findByTenantIdAndPackageCodeAndPackageVersion("tenant-A", "PKG.COPD", "1.0.0"))
            .thenReturn(Optional.empty());

        PackageResponse response = service.createPackage(new PackageCreateRequest(
            "PKG.COPD", "1.0.0", "慢阻肺专病包", "资产说明"));

        assertThat(response.packageId()).isNotNull();
        assertThat(response.status()).isEqualTo(KnowledgePackageStatus.DRAFT);
        
        ArgumentCaptor<KnowledgePackage> packCap = ArgumentCaptor.forClass(KnowledgePackage.class);
        verify(packageRepository).save(packCap.capture());
        assertThat(packCap.getValue().tenantId()).isEqualTo("tenant-A");
        verify(auditPublisher).publish(eq(AuditAction.CREATE), eq("knowledge_package"), any(), any());
    }

    @Test
    void createPackageFailsWhenVersionDuplicate() {
        KnowledgePackage existing = new KnowledgePackage(
            1L, "pkg-1", "tenant-A", "PKG.COPD", "1.0.0", "已有包", null,
            KnowledgePackageStatus.DRAFT, Instant.now(), "tester", Instant.now(), "tester", "trace"
        );
        when(packageRepository.findByTenantIdAndPackageCodeAndPackageVersion("tenant-A", "PKG.COPD", "1.0.0"))
            .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createPackage(new PackageCreateRequest(
                "PKG.COPD", "1.0.0", "慢阻肺专病包", "资产说明")))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_PACKAGE_004);
    }

    @Test
    void addPackageItemFailsWhenAssetNotPublished() {
        KnowledgePackage pack = new KnowledgePackage(
            1L, "pkg-1", "tenant-A", "PKG.COPD", "1.0.0", "包草稿", null,
            KnowledgePackageStatus.DRAFT, Instant.now(), "tester", Instant.now(), "tester", "trace"
        );
        when(packageRepository.findByPackageIdAndTenantId("pkg-1", "tenant-A"))
            .thenReturn(Optional.of(pack));

        // 模拟一个草稿状态的规则，未审核通过不允许入包
        RuleDefinition rule = mock(RuleDefinition.class);
        when(rule.status()).thenReturn(RuleDefinitionStatus.DRAFT);
        when(ruleRepository.findByRuleIdAndTenantId("rule-1", "tenant-A"))
            .thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> service.addPackageItem("pkg-1", new PackageItemRequest(
                PackageItemAssetType.RULE, "rule-1", "1")))
            .isInstanceOf(ApiException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ENG_PACKAGE_002);
    }

    @Test
    void addPackageItemSucceedsWhenAssetPublished() {
        KnowledgePackage pack = new KnowledgePackage(
            1L, "pkg-1", "tenant-A", "PKG.COPD", "1.0.0", "包草稿", null,
            KnowledgePackageStatus.DRAFT, Instant.now(), "tester", Instant.now(), "tester", "trace"
        );
        when(packageRepository.findByPackageIdAndTenantId("pkg-1", "tenant-A"))
            .thenReturn(Optional.of(pack));

        RuleDefinition rule = mock(RuleDefinition.class);
        when(rule.status()).thenReturn(RuleDefinitionStatus.PUBLISHED);
        when(ruleRepository.findByRuleIdAndTenantId("rule-1", "tenant-A"))
            .thenReturn(Optional.of(rule));

        when(itemRepository.findByTenantIdAndPackageIdAndAssetTypeAndAssetId("tenant-A", "pkg-1", PackageItemAssetType.RULE, "rule-1"))
            .thenReturn(Optional.empty());

        PackageItemResponse response = service.addPackageItem("pkg-1", new PackageItemRequest(
            PackageItemAssetType.RULE, "rule-1", "1"));

        assertThat(response.itemId()).isNotNull();
        assertThat(response.assetId()).isEqualTo("rule-1");
        verify(auditPublisher).publish(eq(AuditAction.UPDATE), eq("knowledge_package"), eq("pkg-1"), any());
    }

    @Test
    void calculateDiffComputesCorrectStats() {
        KnowledgePackage targetPack = new KnowledgePackage(
            1L, "pkg-target", "tenant-A", "PKG.COPD", "2.0.0", "新包", null,
            KnowledgePackageStatus.DRAFT, Instant.now(), "tester", Instant.now(), "tester", "trace"
        );
        KnowledgePackage basePack = new KnowledgePackage(
            2L, "pkg-base", "tenant-A", "PKG.COPD", "1.0.0", "老包", null,
            KnowledgePackageStatus.ACTIVE, Instant.now(), "tester", Instant.now(), "tester", "trace"
        );

        when(packageRepository.findByPackageIdAndTenantId("pkg-target", "tenant-A")).thenReturn(Optional.of(targetPack));
        when(packageRepository.findByPackageIdAndTenantId("pkg-base", "tenant-A")).thenReturn(Optional.of(basePack));

        // 模拟老包资产：rule-1 (v1), pathway-1 (v1)
        List<PackageItem> baseItems = List.of(
            new PackageItem(1L, "i-1", "tenant-A", "pkg-base", PackageItemAssetType.RULE, "rule-1", "1", Instant.now(), "tester", Instant.now(), "tester", "trace"),
            new PackageItem(2L, "i-2", "tenant-A", "pkg-base", PackageItemAssetType.PATHWAY, "pathway-1", "1", Instant.now(), "tester", Instant.now(), "tester", "trace")
        );

        // 模拟新包资产：rule-1 (v2 - 更新), pathway-1 (v1 - 未变), evaluation-1 (v1 - 新增)
        List<PackageItem> targetItems = List.of(
            new PackageItem(3L, "i-3", "tenant-A", "pkg-target", PackageItemAssetType.RULE, "rule-1", "2", Instant.now(), "tester", Instant.now(), "tester", "trace"),
            new PackageItem(4L, "i-4", "tenant-A", "pkg-target", PackageItemAssetType.PATHWAY, "pathway-1", "1", Instant.now(), "tester", Instant.now(), "tester", "trace"),
            new PackageItem(5L, "i-5", "tenant-A", "pkg-target", PackageItemAssetType.EVALUATION, "eval-1", "1", Instant.now(), "tester", Instant.now(), "tester", "trace")
        );

        when(itemRepository.findByTenantIdAndPackageId("tenant-A", "pkg-base")).thenReturn(baseItems);
        when(itemRepository.findByTenantIdAndPackageId("tenant-A", "pkg-target")).thenReturn(targetItems);

        PackageDiffResponse response = service.calculateDiff("pkg-target", "pkg-base");

        assertThat(response.baseVersion()).isEqualTo("1.0.0");
        assertThat(response.targetVersion()).isEqualTo("2.0.0");
        assertThat(response.addedCount()).isEqualTo(1); // eval-1
        assertThat(response.updatedCount()).isEqualTo(1); // rule-1
        assertThat(response.removedCount()).isEqualTo(0); // pathway-1还在
    }

    @Test
    void syncPackageExecutesSyncOnAllChannelsAndActivatesPackage() throws Exception {
        KnowledgePackage pack = new KnowledgePackage(
            1L, "pkg-1", "tenant-A", "PKG.COPD", "1.0.0", "包草稿", null,
            KnowledgePackageStatus.PUBLISHED, Instant.now(), "tester", Instant.now(), "tester", "trace"
        );
        when(packageRepository.findByPackageIdAndTenantId("pkg-1", "tenant-A"))
            .thenReturn(Optional.of(pack));

        SyncTarget target = new SyncTarget(
            1L, "target-1", "tenant-A", "投影目标", SyncTargetType.DIFY, "config",
            SyncTargetStatus.ACTIVE, Instant.now(), "tester", Instant.now(), "tester", "trace"
        );
        when(targetRepository.findByTargetIdAndTenantId("target-1", "tenant-A"))
            .thenReturn(Optional.of(target));

        when(syncPort.sync(eq("tenant-A"), any(ReleasePlan.class), eq(target)))
            .thenReturn("EVIDENCE-DIFY-001");

        PackageSyncResponse response = service.syncPackage("pkg-1", new PackageSyncRequest(
            "org-1", ReleaseStrategy.FULL, ReleaseScopeType.ALL, null, List.of("target-1")
        ));

        assertThat(response.status()).isEqualTo(ReleasePlanStatus.SUCCESS);
        assertThat(response.logs()).hasSize(1);
        assertThat(response.logs().get(0).syncEvidence()).isEqualTo("EVIDENCE-DIFY-001");

        ArgumentCaptor<KnowledgePackage> packCap = ArgumentCaptor.forClass(KnowledgePackage.class);
        verify(packageRepository).save(packCap.capture());
        // 全量成功后，原包状态应该原子更新为 ACTIVE
        assertThat(packCap.getValue().status()).isEqualTo(KnowledgePackageStatus.ACTIVE);
        verify(auditPublisher).publish(eq(AuditAction.PUBLISH), eq("knowledge_package"), eq("pkg-1"), any());
    }

    @Test
    void rollbackPackageSwitchesActiveStatusAndRecordsAudit() {
        KnowledgePackage currentActive = new KnowledgePackage(
            1L, "pkg-1", "tenant-A", "PKG.COPD", "2.0.0", "当前在用包", null,
            KnowledgePackageStatus.ACTIVE, Instant.now(), "tester", Instant.now(), "tester", "trace"
        );
        KnowledgePackage targetRollback = new KnowledgePackage(
            2L, "pkg-2", "tenant-A", "PKG.COPD", "1.0.0", "历史老包", null,
            KnowledgePackageStatus.OFFLINE, Instant.now(), "tester", Instant.now(), "tester", "trace"
        );

        when(packageRepository.findByPackageIdAndTenantId("pkg-1", "tenant-A")).thenReturn(Optional.of(currentActive));
        when(packageRepository.findByPackageIdAndTenantId("pkg-2", "tenant-A")).thenReturn(Optional.of(targetRollback));

        PackageResponse response = service.rollbackPackage("pkg-1", "pkg-2");

        assertThat(response.packageId()).isEqualTo("pkg-2");
        assertThat(response.status()).isEqualTo(KnowledgePackageStatus.ACTIVE);

        ArgumentCaptor<KnowledgePackage> packCap = ArgumentCaptor.forClass(KnowledgePackage.class);
        // 保存两个包的状态切换
        verify(packageRepository, org.mockito.Mockito.times(2)).save(packCap.capture());
        List<KnowledgePackage> savedPacks = packCap.getAllValues();
        
        assertThat(savedPacks).anySatisfy(p -> {
            assertThat(p.packageId()).isEqualTo("pkg-1");
            assertThat(p.status()).isEqualTo(KnowledgePackageStatus.OFFLINE);
        });
        assertThat(savedPacks).anySatisfy(p -> {
            assertThat(p.packageId()).isEqualTo("pkg-2");
            assertThat(p.status()).isEqualTo(KnowledgePackageStatus.ACTIVE);
        });

        verify(auditPublisher).publish(eq(AuditAction.ROLLBACK), eq("knowledge_package"), eq("pkg-2"), any());
    }
}
