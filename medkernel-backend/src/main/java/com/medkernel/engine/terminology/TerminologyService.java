package com.medkernel.engine.terminology;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

@Service
public class TerminologyService {

    private final StandardTermRepository standardTermRepository;
    private final LocalTermRepository localTermRepository;
    private final TermMappingRepository mappingRepository;
    private final MappingCandidateRepository candidateRepository;
    private final MappingConflictRepository conflictRepository;
    private final TermMappingPackageRepository packageRepository;
    private final TermMappingPackageItemRepository packageItemRepository;
    private final TermMappingPackageReleaseRepository packageReleaseRepository;

    public TerminologyService(StandardTermRepository standardTermRepository,
                              LocalTermRepository localTermRepository,
                              TermMappingRepository mappingRepository,
                              MappingCandidateRepository candidateRepository,
                              MappingConflictRepository conflictRepository,
                              TermMappingPackageRepository packageRepository,
                              TermMappingPackageItemRepository packageItemRepository,
                              TermMappingPackageReleaseRepository packageReleaseRepository) {
        this.standardTermRepository = standardTermRepository;
        this.localTermRepository = localTermRepository;
        this.mappingRepository = mappingRepository;
        this.candidateRepository = candidateRepository;
        this.conflictRepository = conflictRepository;
        this.packageRepository = packageRepository;
        this.packageItemRepository = packageItemRepository;
        this.packageReleaseRepository = packageReleaseRepository;
    }

    public PageResponse<StandardTerm> pageStandardTerms(PageRequest request, StandardTermFilter filter) {
        String tenantId = requireCurrentTenant();
        String category = name(filter.category());
        String status = name(filter.status());
        String keyword = normalizeKeyword(filter.keyword());
        long total = standardTermRepository.countByFilter(tenantId, filter.standardSystem(), category, status, keyword);
        if (total == 0) {
            return PageResponse.empty(request);
        }
        return PageResponse.of(standardTermRepository.pageByFilter(
            tenantId, filter.standardSystem(), category, status, keyword, request.offset(), request.safeSize()
        ), request, total);
    }

    public PageResponse<LocalTerm> pageLocalTerms(PageRequest request, LocalTermFilter filter) {
        String tenantId = requireCurrentTenant();
        String category = name(filter.category());
        String status = name(filter.status());
        String keyword = normalizeKeyword(filter.keyword());
        long total = localTermRepository.countByFilter(tenantId, filter.sourceSystem(), category, status, keyword);
        if (total == 0) {
            return PageResponse.empty(request);
        }
        return PageResponse.of(localTermRepository.pageByFilter(
            tenantId, filter.sourceSystem(), category, status, keyword, request.offset(), request.safeSize()
        ), request, total);
    }

    public PageResponse<TermMapping> pageMappings(PageRequest request, MappingFilter filter) {
        String tenantId = requireCurrentTenant();
        String category = name(filter.category());
        String status = name(filter.status());
        String keyword = normalizeKeyword(filter.keyword());
        long total = mappingRepository.countByFilter(tenantId, filter.sourceSystem(), category, status, keyword);
        if (total == 0) {
            return PageResponse.empty(request);
        }
        return PageResponse.of(mappingRepository.pageByFilter(
            tenantId, filter.sourceSystem(), category, status, keyword, request.offset(), request.safeSize()
        ), request, total);
    }

    public PageResponse<MappingCandidate> pageCandidates(PageRequest request, CandidateFilter filter) {
        String tenantId = requireCurrentTenant();
        String status = name(filter.status());
        String riskLevel = name(filter.riskLevel());
        long total = candidateRepository.countByFilter(tenantId, status, riskLevel, filter.conflictFlag());
        if (total == 0) {
            return PageResponse.empty(request);
        }
        return PageResponse.of(candidateRepository.pageByFilter(
            tenantId, status, riskLevel, filter.conflictFlag(), request.offset(), request.safeSize()
        ), request, total);
    }

    public PageResponse<MappingConflict> pageConflicts(PageRequest request, ConflictFilter filter) {
        String tenantId = requireCurrentTenant();
        String status = name(filter.status());
        String riskLevel = name(filter.riskLevel());
        String conflictType = name(filter.conflictType());
        long total = conflictRepository.countByFilter(tenantId, status, riskLevel, conflictType);
        if (total == 0) {
            return PageResponse.empty(request);
        }
        return PageResponse.of(conflictRepository.pageByFilter(
            tenantId, status, riskLevel, conflictType, request.offset(), request.safeSize()
        ), request, total);
    }

    public PageResponse<TermMappingPackage> pagePackages(PageRequest request, PackageFilter filter) {
        String tenantId = requireCurrentTenant();
        String status = name(filter.status());
        long total = packageRepository.countByFilter(
            tenantId, filter.packageCode(), status, filter.scopeLevel(), filter.scopeCode());
        if (total == 0) {
            return PageResponse.empty(request);
        }
        return PageResponse.of(packageRepository.pageByFilter(
            tenantId, filter.packageCode(), status, filter.scopeLevel(), filter.scopeCode(),
            request.offset(), request.safeSize()
        ), request, total);
    }

    @Transactional
    public TermMapping confirmCandidate(Long candidateId, ConfirmMappingRequest request) {
        String tenantId = requireCurrentTenant();
        String userId = currentUserId();
        Instant now = Instant.now();
        MappingCandidate candidate = candidateRepository.findByTenantIdAndId(tenantId, candidateId)
            .orElseThrow(() -> ApiException.notFound("映射候选 id=" + candidateId));
        if (candidate.status() != MappingCandidateStatus.PENDING) {
            throw ApiException.conflict("映射候选 id=" + candidateId + " 不是待确认状态");
        }
        LocalTerm localTerm = localTermRepository.findByTenantIdAndId(tenantId, candidate.localTermId())
            .orElseThrow(() -> ApiException.notFound("院内字典 id=" + candidate.localTermId()));
        StandardTerm standardTerm = standardTermRepository.findByTenantIdAndId(tenantId, candidate.standardTermId())
            .orElseThrow(() -> ApiException.notFound("标准字典 id=" + candidate.standardTermId()));
        if (localTerm.category() != null && standardTerm.category() != null
                && localTerm.category() != standardTerm.category()) {
            throw ApiException.conflict("院内字典与标准字典分类不一致，禁止确认映射");
        }
        TermCategory mappingCategory = localTerm.category() == null ? standardTerm.category() : localTerm.category();

        TermMapping saved = mappingRepository
            .findByTenantIdAndLocalTermIdAndStandardTermId(tenantId, candidate.localTermId(), candidate.standardTermId())
            .map(existing -> mappingRepository.save(existing.confirmed(
                userId, now, evidence(request, candidate), localTerm.sourceSystem(), mappingCategory
            )))
            .orElseGet(() -> mappingRepository.save(new TermMapping(
                null, tenantId, candidate.localTermId(), candidate.standardTermId(), localTerm.sourceSystem(), mappingCategory,
                candidate.confidence(), candidate.riskLevel(), TermMappingStatus.CONFIRMED,
                evidence(request, candidate), userId, now, now, userId, now, userId
            )));
        candidateRepository.save(candidate.confirmed(request.reviewNote(), userId, now));
        return saved;
    }

    @Transactional
    public MappingConflict resolveConflict(Long conflictId, ResolveConflictRequest request) {
        String tenantId = requireCurrentTenant();
        MappingConflict conflict = conflictRepository.findByTenantIdAndId(tenantId, conflictId)
            .orElseThrow(() -> ApiException.notFound("映射冲突 id=" + conflictId));
        if (conflict.status() != MappingConflictStatus.OPEN) {
            throw ApiException.conflict("映射冲突 id=" + conflictId + " 不是打开状态");
        }
        return conflictRepository.save(conflict.resolved(request.resolutionNote(), currentUserId(), Instant.now()));
    }

    @Transactional
    public TermMappingPackage buildPackage(BuildTerminologyPackageRequest request) {
        String tenantId = requireCurrentTenant();
        String userId = currentUserId();
        Instant now = Instant.now();
        List<TermMapping> mappings = mappingRepository.findConfirmedByTenantIdAndScope(
            tenantId, request.scopeLevel(), request.scopeCode());
        if (mappings.isEmpty()) {
            throw ApiException.conflict("当前范围没有已确认映射，无法构建映射包");
        }
        String contentHash = hashMappings(request, mappings);
        TermMappingPackage saved = packageRepository.save(new TermMappingPackage(
            null, tenantId, request.packageCode(), request.packageVersion(), request.displayName(),
            request.scopeLevel(), request.scopeCode(), TermMappingPackageStatus.DRAFT,
            mappings.size(), contentHash, null, null, null, null, now, userId, now, userId
        ));
        for (TermMapping mapping : mappings) {
            packageItemRepository.save(new TermMappingPackageItem(
                null, tenantId, saved.id(), mapping.id(), snapshot(mapping), now, userId
            ));
        }
        return saved;
    }

    @Transactional
    public TermMappingPackage publishPackage(Long packageId, PublishTerminologyPackageRequest request) {
        String tenantId = requireCurrentTenant();
        String userId = currentUserId();
        Instant now = Instant.now();
        TermMappingPackage pkg = packageRepository.findByTenantIdAndId(tenantId, packageId)
            .orElseThrow(() -> ApiException.notFound("映射包 id=" + packageId));
        if (pkg.status() == TermMappingPackageStatus.ROLLED_BACK || pkg.status() == TermMappingPackageStatus.ARCHIVED) {
            throw ApiException.conflict("映射包 id=" + packageId + " 已不可发布");
        }
        TermMappingPackage next = pkg.withGrayScope(request.grayScopeJson())
            .withStatus(request.releaseMode() == PackageReleaseMode.FULL
                ? TermMappingPackageStatus.PUBLISHED
                : TermMappingPackageStatus.GRAY, userId, now);
        if (request.releaseMode() == PackageReleaseMode.FULL) {
            for (TermMappingPackage active : packageRepository.findActiveByTenantIdAndPackageCodeAndScope(
                    tenantId, pkg.packageCode(), pkg.scopeLevel(), pkg.scopeCode())) {
                if (!active.id().equals(pkg.id())) {
                    packageRepository.save(active.withStatus(TermMappingPackageStatus.SUPERSEDED, userId, now));
                }
            }
        }
        TermMappingPackage saved = packageRepository.save(next);
        packageReleaseRepository.save(new TermMappingPackageRelease(
            null, tenantId, pkg.id(), null, TermPackageReleaseEventType.PUBLISH,
            request.releaseMode(), request.reason(), request.grayScopeJson(), now, userId
        ));
        return saved;
    }

    @Transactional
    public TermMappingPackage rollbackPackage(Long packageId, RollbackTerminologyPackageRequest request) {
        String tenantId = requireCurrentTenant();
        String userId = currentUserId();
        Instant now = Instant.now();
        TermMappingPackage current = packageRepository.findByTenantIdAndId(tenantId, packageId)
            .orElseThrow(() -> ApiException.notFound("当前映射包 id=" + packageId));
        TermMappingPackage target = packageRepository.findByTenantIdAndId(tenantId, request.targetPackageId())
            .orElseThrow(() -> ApiException.notFound("目标映射包 id=" + request.targetPackageId()));
        if (!sameScope(current, target)) {
            throw ApiException.conflict("回滚目标必须与当前映射包同编码、同范围");
        }
        if (current.status() != TermMappingPackageStatus.PUBLISHED && current.status() != TermMappingPackageStatus.GRAY) {
            throw ApiException.conflict("当前映射包不是已发布或灰度状态，无法回滚");
        }
        if (target.status() != TermMappingPackageStatus.PUBLISHED && target.status() != TermMappingPackageStatus.SUPERSEDED) {
            throw ApiException.conflict("目标映射包不是可回滚发布点");
        }
        packageRepository.save(current.rolledBack(userId, now));
        TermMappingPackage restored = packageRepository.save(target.withStatus(TermMappingPackageStatus.PUBLISHED, userId, now));
        packageReleaseRepository.save(new TermMappingPackageRelease(
            null, tenantId, current.id(), target.id(), TermPackageReleaseEventType.ROLLBACK,
            PackageReleaseMode.FULL, request.reason(), null, now, userId
        ));
        return restored;
    }

    private String requireCurrentTenant() {
        OrgScope scope = RequestContext.currentOrgScope();
        if (scope == null || !scope.hasTenant()) {
            throw ApiException.tenantMissing();
        }
        return scope.tenantId();
    }

    private String currentUserId() {
        return RequestContext.currentUserId().orElse("system");
    }

    private String normalizeKeyword(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return "%" + raw.trim().toLowerCase() + "%";
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String evidence(ConfirmMappingRequest request, MappingCandidate candidate) {
        if (request.evidenceOverride() != null && !request.evidenceOverride().isBlank()) {
            return request.evidenceOverride().trim();
        }
        return candidate.evidenceText();
    }

    private boolean sameScope(TermMappingPackage current, TermMappingPackage target) {
        return current.packageCode().equals(target.packageCode())
            && current.scopeLevel().equals(target.scopeLevel())
            && current.scopeCode().equals(target.scopeCode());
    }

    private String hashMappings(BuildTerminologyPackageRequest request, List<TermMapping> mappings) {
        StringBuilder payload = new StringBuilder()
            .append(request.packageCode()).append('|')
            .append(request.packageVersion()).append('|')
            .append(request.scopeLevel()).append('|')
            .append(request.scopeCode());
        mappings.forEach(mapping -> payload.append('|')
            .append(mapping.id()).append(':')
            .append(mapping.localTermId()).append(':')
            .append(mapping.standardTermId()).append(':')
            .append(mapping.status()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String snapshot(TermMapping mapping) {
        return "{\"mappingId\":" + mapping.id()
            + ",\"localTermId\":" + mapping.localTermId()
            + ",\"standardTermId\":" + mapping.standardTermId()
            + ",\"status\":\"" + mapping.status() + "\"}";
    }
}
