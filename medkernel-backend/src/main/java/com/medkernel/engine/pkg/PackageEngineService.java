package com.medkernel.engine.pkg;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.medkernel.engine.evaluation.EvaluationIndicator;
import com.medkernel.engine.evaluation.EvaluationIndicatorRepository;
import com.medkernel.engine.pathway.PathwayTemplate;
import com.medkernel.engine.pathway.PathwayTemplateRepository;
import com.medkernel.engine.rule.RuleDefinition;
import com.medkernel.engine.rule.RuleDefinitionRepository;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEvent;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识包发布同步核心应用服务。
 *
 * <p>提供资产打包、差异比对、发布灰度校验、多物理通道投影以及快速一键回滚的完整应用层实现。
 */
@Service
@Transactional
public class PackageEngineService {

    private static final Logger log = LoggerFactory.getLogger(PackageEngineService.class);

    private final KnowledgePackageRepository packageRepository;
    private final PackageItemRepository itemRepository;
    private final ReleasePlanRepository planRepository;
    private final SyncTargetRepository targetRepository;
    private final SyncLogRepository logRepository;

    private final RuleDefinitionRepository ruleRepository;
    private final PathwayTemplateRepository pathwayRepository;
    private final EvaluationIndicatorRepository evaluationRepository;

    private final PackageSyncPort syncPort;
    private final AuditEventPublisher auditPublisher;

    public PackageEngineService(
            KnowledgePackageRepository packageRepository,
            PackageItemRepository itemRepository,
            ReleasePlanRepository planRepository,
            SyncTargetRepository targetRepository,
            SyncLogRepository logRepository,
            RuleDefinitionRepository ruleRepository,
            PathwayTemplateRepository pathwayRepository,
            EvaluationIndicatorRepository evaluationRepository,
            PackageSyncPort syncPort,
            AuditEventPublisher auditPublisher) {
        this.packageRepository = packageRepository;
        this.itemRepository = itemRepository;
        this.planRepository = planRepository;
        this.targetRepository = targetRepository;
        this.logRepository = logRepository;
        this.ruleRepository = ruleRepository;
        this.pathwayRepository = pathwayRepository;
        this.evaluationRepository = evaluationRepository;
        this.syncPort = syncPort;
        this.auditPublisher = auditPublisher;
    }

    /**
     * 创建知识包草稿。
     */
    public PackageResponse createPackage(PackageCreateRequest request) {
        String tenantId = currentTenantId();
        String traceId = RequestContext.currentTraceId();
        String actor = currentActor();

        // 唯一性检验（同一个编码和版本不能重复）
        Optional<KnowledgePackage> existing = packageRepository
            .findByTenantIdAndPackageCodeAndPackageVersion(tenantId, request.packageCode(), request.packageVersion());
        if (existing.isPresent()) {
            throw new ApiException(ErrorCode.ENG_PACKAGE_004, "知识包版本在该租户内已存在: " + request.packageVersion());
        }

        KnowledgePackage pack = new KnowledgePackage(
            null,
            UUID.randomUUID().toString(),
            tenantId,
            request.packageCode(),
            request.packageVersion(),
            request.name(),
            request.description(),
            KnowledgePackageStatus.DRAFT,
            Instant.now(),
            actor,
            Instant.now(),
            actor,
            traceId
        );

        KnowledgePackage saved = packageRepository.save(pack);
        auditPublisher.publish(AuditAction.CREATE, "knowledge_package", saved.packageId(), 
            "创建知识包草稿: " + saved.name() + " (" + saved.packageVersion() + ")");
        return PackageResponse.from(saved);
    }

    /**
     * 获取当前租户下的知识包列表。
     */
    public PageResponse<KnowledgePackage> listPackages(PageRequest page) {
        String tenantId = currentTenantId();
        int offset = page.offset();
        int limit = page.safeSize();

        List<KnowledgePackage> items = packageRepository.pageByTenantId(tenantId, offset, limit);
        long total = packageRepository.countByTenantId(tenantId);
        return PageResponse.of(items, page, total);
    }

    /**
     * 获取包详细信息以及包含的子资产列表。
     */
    public PackageDetailResponse packageDetail(String packageId) {
        String tenantId = currentTenantId();
        KnowledgePackage pack = packageRepository.findByPackageIdAndTenantId(packageId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_PACKAGE_001, "知识包不存在: " + packageId));

        List<PackageItem> items = itemRepository.findByTenantIdAndPackageId(tenantId, packageId);
        return PackageDetailResponse.from(pack, items);
    }

    /**
     * 向知识包草稿中添加一个子资产条目。
     */
    public PackageItemResponse addPackageItem(String packageId, PackageItemRequest request) {
        String tenantId = currentTenantId();
        String traceId = RequestContext.currentTraceId();
        String actor = currentActor();

        KnowledgePackage pack = packageRepository.findByPackageIdAndTenantId(packageId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_PACKAGE_001, "知识包不存在: " + packageId));

        if (pack.status() != KnowledgePackageStatus.DRAFT) {
            throw new ApiException(ErrorCode.ENG_PACKAGE_002, "只能向处于 DRAFT 草稿状态的知识包中添加资产");
        }

        // 验证所绑定的资产是否存在及其生命周期状态（未审核的资产如 DRAFT 不可入包）
        validateAssetStatus(tenantId, request.assetType(), request.assetId());

        // 避免重复添加同个资产
        Optional<PackageItem> existing = itemRepository
            .findByTenantIdAndPackageIdAndAssetTypeAndAssetId(tenantId, packageId, request.assetType(), request.assetId());
        if (existing.isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT, "资产细项已在当前包中声明，无需重复添加");
        }

        PackageItem item = new PackageItem(
            null,
            UUID.randomUUID().toString(),
            tenantId,
            packageId,
            request.assetType(),
            request.assetId(),
            request.assetVersion(),
            Instant.now(),
            actor,
            Instant.now(),
            actor,
            traceId
        );

        PackageItem saved = itemRepository.save(item);
        auditPublisher.publish(AuditAction.UPDATE, "knowledge_package", packageId,
            "向知识包添加资产条目 (" + request.assetType() + "): " + request.assetId());
        return PackageItemResponse.from(saved);
    }

    /**
     * 计算两个包版本之间的资产差异与变动影响分析。
     */
    public PackageDiffResponse calculateDiff(String packageId, String basePackageId) {
        String tenantId = currentTenantId();

        // 校验包存在
        KnowledgePackage targetPack = packageRepository.findByPackageIdAndTenantId(packageId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_PACKAGE_001, "目标知识包不存在: " + packageId));

        String baseVersion = "NONE";
        List<PackageItem> baseItems = new ArrayList<>();
        if (basePackageId != null && !basePackageId.isBlank()) {
            KnowledgePackage basePack = packageRepository.findByPackageIdAndTenantId(basePackageId, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENG_PACKAGE_001, "基准知识包不存在: " + basePackageId));
            baseVersion = basePack.packageVersion();
            baseItems = itemRepository.findByTenantIdAndPackageId(tenantId, basePackageId);
        }

        List<PackageItem> targetItems = itemRepository.findByTenantIdAndPackageId(tenantId, packageId);

        int added = 0;
        int updated = 0;
        int removed = 0;
        List<String> affectedDepts = new ArrayList<>();

        for (PackageItem target : targetItems) {
            Optional<PackageItem> matchedBase = baseItems.stream()
                .filter(b -> b.assetType() == target.assetType() && b.assetId().equals(target.assetId()))
                .findFirst();

            if (matchedBase.isEmpty()) {
                added++;
            } else if (!matchedBase.get().assetVersion().equals(target.assetVersion())) {
                updated++;
            }
            // 模拟受影响的责任科室分析
            String deptId = getAssetDepartment(tenantId, target.assetType(), target.assetId());
            if (deptId != null && !affectedDepts.contains(deptId)) {
                affectedDepts.add(deptId);
            }
        }

        for (PackageItem base : baseItems) {
            boolean existsInTarget = targetItems.stream()
                .anyMatch(t -> t.assetType() == base.assetType() && t.assetId().equals(base.assetId()));
            if (!existsInTarget) {
                removed++;
            }
        }

        return new PackageDiffResponse(
            packageId,
            baseVersion,
            targetPack.packageVersion(),
            added,
            updated,
            removed,
            affectedDepts
        );
    }

    /**
     * 触发包同步与发布执行（支持灰度、全量、回滚等多通道投影）。
     */
    public PackageSyncResponse syncPackage(String packageId, PackageSyncRequest request) {
        String tenantId = currentTenantId();
        String traceId = RequestContext.currentTraceId();
        String actor = currentActor();

        KnowledgePackage pack = packageRepository.findByPackageIdAndTenantId(packageId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_PACKAGE_001, "知识包不存在: " + packageId));

        // 灰度与全量发布策略的基本参数及签名规则校验
        if (request.strategy() == ReleaseStrategy.GRAYSCALE 
            && (request.scopeType() == ReleaseScopeType.ALL || request.scopeValue() == null || request.scopeValue().isBlank())) {
            throw new ApiException(ErrorCode.ENG_PACKAGE_003, "灰度发布时必须指定有效的作用域范围和具体过滤值");
        }

        // 创建发布计划
        ReleasePlan plan = new ReleasePlan(
            null,
            UUID.randomUUID().toString(),
            tenantId,
            packageId,
            request.targetOrgUnitId(),
            request.strategy(),
            request.scopeType(),
            request.scopeValue(),
            ReleasePlanStatus.EXECUTING,
            Instant.now(),
            actor,
            Instant.now(),
            actor,
            traceId
        );
        ReleasePlan savedPlan = planRepository.save(plan);

        List<SyncLogResponse> logs = new ArrayList<>();
        boolean anySuccess = false;
        boolean allSuccess = true;

        for (String targetId : request.targetIds()) {
            SyncTarget target = targetRepository.findByTargetIdAndTenantId(targetId, tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.ENG_PACKAGE_001, "同步通道目标不存在: " + targetId));

            SyncLog syncLog = new SyncLog(
                null,
                UUID.randomUUID().toString(),
                tenantId,
                savedPlan.planId(),
                targetId,
                SyncLogStatus.RUNNING,
                null, null, 0, null,
                Instant.now(), actor, Instant.now(), actor, traceId
            );
            SyncLog savedLog = logRepository.save(syncLog);

            try {
                // 执行物理投影同步
                String evidence = syncPort.sync(tenantId, savedPlan, target);
                
                SyncLog successLog = new SyncLog(
                    savedLog.id(),
                    savedLog.logId(),
                    tenantId,
                    savedPlan.planId(),
                    targetId,
                    SyncLogStatus.SUCCESS,
                    null, null, 0, evidence,
                    savedLog.createdAt(), savedLog.createdBy(), Instant.now(), actor, traceId
                );
                logRepository.save(successLog);
                logs.add(SyncLogResponse.from(successLog));
                anySuccess = true;
            } catch (Exception e) {
                log.error("物理同步失败, targetId: {}", targetId, e);
                allSuccess = false;
                
                SyncLog failedLog = new SyncLog(
                    savedLog.id(),
                    savedLog.logId(),
                    tenantId,
                    savedPlan.planId(),
                    targetId,
                    SyncLogStatus.FAILED,
                    "ENG-PACKAGE-005",
                    e.getMessage(),
                    0, null,
                    savedLog.createdAt(), savedLog.createdBy(), Instant.now(), actor, traceId
                );
                logRepository.save(failedLog);
                logs.add(SyncLogResponse.from(failedLog));
            }
        }

        ReleasePlanStatus finalStatus = allSuccess ? ReleasePlanStatus.SUCCESS : 
            (anySuccess ? ReleasePlanStatus.EXECUTING : ReleasePlanStatus.FAILED);
        planRepository.save(savedPlan.withStatus(finalStatus));

        // 如果全量发布成功，原子激活包状态并隔离失效旧版本包
        if (request.strategy() == ReleaseStrategy.FULL && allSuccess) {
            // 原子切换：失效既有 ACTIVE 包
            List<KnowledgePackage> activePacks = packageRepository.findByTenantIdOrderByUpdatedAtDesc(tenantId).stream()
                .filter(p -> p.status() == KnowledgePackageStatus.ACTIVE)
                .toList();
            for (KnowledgePackage active : activePacks) {
                packageRepository.save(active.withStatus(KnowledgePackageStatus.OFFLINE));
            }
            // 激活当前包
            packageRepository.save(pack.withStatus(KnowledgePackageStatus.ACTIVE));
            auditPublisher.publish(AuditAction.PUBLISH, "knowledge_package", packageId, 
                "知识包发布并同步全量成功: " + pack.name() + " (" + pack.packageVersion() + ")");
        } else {
            // 灰度发布仅更新包状态为已发布，不覆盖现有 active
            if (pack.status() == KnowledgePackageStatus.DRAFT) {
                packageRepository.save(pack.withStatus(KnowledgePackageStatus.PUBLISHED));
            }
            auditPublisher.publish(AuditAction.PUBLISH, "knowledge_package", packageId, 
                "知识包灰度发布计划执行完成, 状态为: " + finalStatus);
        }

        return new PackageSyncResponse(savedPlan.planId(), packageId, finalStatus, logs);
    }

    /**
     * 一键快速回滚包版本到指定历史点。
     */
    public PackageResponse rollbackPackage(String packageId, String targetPackageId) {
        String tenantId = currentTenantId();
        String traceId = RequestContext.currentTraceId();
        String actor = currentActor();

        KnowledgePackage currentActive = packageRepository.findByPackageIdAndTenantId(packageId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_PACKAGE_001, "当前在用包不存在: " + packageId));

        KnowledgePackage targetRollback = packageRepository.findByPackageIdAndTenantId(targetPackageId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_PACKAGE_001, "回滚目标包不存在: " + targetPackageId));

        if (targetRollback.status() != KnowledgePackageStatus.PUBLISHED 
            && targetRollback.status() != KnowledgePackageStatus.OFFLINE) {
            throw new ApiException(ErrorCode.ENG_PACKAGE_002, "回滚目标包必须是曾经成功发布的包（PUBLISHED 或 OFFLINE）");
        }

        // 执行状态原子转换：隔离当前包，激活历史包
        packageRepository.save(currentActive.withStatus(KnowledgePackageStatus.OFFLINE));
        KnowledgePackage savedTarget = packageRepository.save(targetRollback.withStatus(KnowledgePackageStatus.ACTIVE));

        // 异步发布回滚审计事实存证
        auditPublisher.publish(AuditAction.ROLLBACK, "knowledge_package", targetPackageId,
            "一键回滚包版本从 " + currentActive.packageVersion() + " 回退到 " + targetRollback.packageVersion());

        return PackageResponse.from(savedTarget);
    }

    // ────────────────────────── 辅助支撑逻辑 ──────────────────────────

    private String currentTenantId() {
        return RequestContext.snapshot().orgScope().tenantId();
    }

    private String currentActor() {
        return RequestContext.snapshot().userId() == null ? "system" : RequestContext.snapshot().userId();
    }

    private void validateAssetStatus(String tenantId, PackageItemAssetType type, String assetId) {
        switch (type) {
            case RULE -> {
                RuleDefinition rule = ruleRepository.findByRuleIdAndTenantId(assetId, tenantId)
                    .orElseThrow(() -> new ApiException(ErrorCode.ENG_RULE_002, "入包规则不存在: " + assetId));
                // 审核通过的规则方可入包
                String status = rule.status() == null ? "" : rule.status().name();
                if (!"PUBLISHED".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
                    throw new ApiException(ErrorCode.ENG_PACKAGE_002, "只允许 PUBLISHED 或 ACTIVE 状态的规则入包, 当前: " + status);
                }
            }
            case PATHWAY -> {
                PathwayTemplate template = pathwayRepository.findByTemplateIdAndTenantId(assetId, tenantId)
                    .orElseThrow(() -> new ApiException(ErrorCode.ENG_PATHWAY_002, "入包路径不存在: " + assetId));
                String status = template.status() == null ? "" : template.status().name();
                if (!"PUBLISHED".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
                    throw new ApiException(ErrorCode.ENG_PACKAGE_002, "只允许 PUBLISHED 或 ACTIVE 状态的路径入包, 当前: " + status);
                }
            }
            case EVALUATION -> {
                EvaluationIndicator indicator = evaluationRepository.findByIndicatorIdAndTenantId(assetId, tenantId)
                    .orElseThrow(() -> new ApiException(ErrorCode.ENG_EVAL_002, "入包评估指标不存在: " + assetId));
                String status = indicator.status() == null ? "" : indicator.status().name();
                if (!"PUBLISHED".equalsIgnoreCase(status) && !"ACTIVE".equalsIgnoreCase(status)) {
                    throw new ApiException(ErrorCode.ENG_PACKAGE_002, "只允许 PUBLISHED 或 ACTIVE 状态的评估指标入包, 当前: " + status);
                }
            }
            default -> {
                // TERMINOLOGY、KNOWLEDGE、FOLLOWUP 等宽限处理
            }
        }
    }

    private String getAssetDepartment(String tenantId, PackageItemAssetType type, String assetId) {
        try {
            switch (type) {
                case RULE -> {
                    return ruleRepository.findByRuleIdAndTenantId(assetId, tenantId)
                        .map(r -> "dept-default").orElse(null);
                }
                case PATHWAY -> {
                    return pathwayRepository.findByTemplateIdAndTenantId(assetId, tenantId)
                        .map(p -> "dept-default").orElse(null);
                }
                case EVALUATION -> {
                    return evaluationRepository.findByIndicatorIdAndTenantId(assetId, tenantId)
                        .map(EvaluationIndicator::responsibleDepartmentId).orElse(null);
                }
                default -> {
                    return "dept-default";
                }
            }
        } catch (Exception e) {
            return "dept-default";
        }
    }
}
