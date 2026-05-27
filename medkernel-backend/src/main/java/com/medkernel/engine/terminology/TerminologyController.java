package com.medkernel.engine.terminology;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.datascope.DataScope;

import jakarta.validation.Valid;

/**
 * GA-ENG-API-04 术语映射 API（标准/本地术语查询、候选确认、冲突处置、术语包构建/发布/回滚）。
 *
 * <p>所有接口要求当前请求上下文携带租户（{@link DataScope#requireTenant}），
 * 读接口需要 {@code term.read}，写接口需要 {@code term.write}，
 * 发布/回滚为高风险操作分别需要 {@code term.publish} / {@code package.rollback}。
 */
@RestController
@RequestMapping("/api/v1/engine/terminology")
@DataScope(requireTenant = true)
public class TerminologyController {

    private final TerminologyService service;

    public TerminologyController(TerminologyService service) {
        this.service = service;
    }

    /**
     * 分页查询当前租户的标准术语，支持按 standardSystem / category / status / keyword 过滤。
     */
    @GetMapping("/standard-terms")
    @PreAuthorize("@perm.has('term.read')")
    public ApiResult<PageResponse<StandardTerm>> standardTerms(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String standardSystem,
            @RequestParam(required = false) TermCategory category,
            @RequestParam(required = false) StandardTermStatus status,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(service.pageStandardTerms(
            new PageRequest(page, size, sort),
            new StandardTermFilter(standardSystem, category, status, keyword)
        ));
    }

    /**
     * 分页查询当前租户的本地术语，支持按 sourceSystem / category / status / keyword 过滤。
     */
    @GetMapping("/local-terms")
    @PreAuthorize("@perm.has('term.read')")
    public ApiResult<PageResponse<LocalTerm>> localTerms(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) TermCategory category,
            @RequestParam(required = false) LocalTermStatus status,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(service.pageLocalTerms(
            new PageRequest(page, size, sort),
            new LocalTermFilter(sourceSystem, category, status, keyword)
        ));
    }

    /**
     * 分页查询当前租户的术语映射，支持按 sourceSystem / category / status / 证据关键词过滤。
     */
    @GetMapping("/mappings")
    @PreAuthorize("@perm.has('term.read')")
    public ApiResult<PageResponse<TermMapping>> mappings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) TermCategory category,
            @RequestParam(required = false) TermMappingStatus status,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(service.pageMappings(
            new PageRequest(page, size, sort),
            new MappingFilter(sourceSystem, category, status, keyword)
        ));
    }

    /**
     * 分页查询当前租户的候选映射，支持按 status / riskLevel / conflictFlag 过滤。
     */
    @GetMapping("/candidates")
    @PreAuthorize("@perm.has('term.read')")
    public ApiResult<PageResponse<MappingCandidate>> candidates(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) MappingCandidateStatus status,
            @RequestParam(required = false) TermRiskLevel riskLevel,
            @RequestParam(required = false) Boolean conflictFlag) {
        return ApiResult.ok(service.pageCandidates(
            new PageRequest(page, size, sort),
            new CandidateFilter(status, riskLevel, conflictFlag)
        ));
    }

    /**
     * 分页查询当前租户的映射冲突，支持按 status / riskLevel / conflictType 过滤。
     */
    @GetMapping("/conflicts")
    @PreAuthorize("@perm.has('term.read')")
    public ApiResult<PageResponse<MappingConflict>> conflicts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) MappingConflictStatus status,
            @RequestParam(required = false) TermRiskLevel riskLevel,
            @RequestParam(required = false) MappingConflictType conflictType) {
        return ApiResult.ok(service.pageConflicts(
            new PageRequest(page, size, sort),
            new ConflictFilter(status, riskLevel, conflictType)
        ));
    }

    /**
     * 确认指定候选映射，把它升级为 CONFIRMED 状态的正式 {@link TermMapping}。
     *
     * <p>候选必须存在且处于 PENDING 状态；本地与标准术语分类不一致时拒绝。
     */
    @PostMapping("/candidates/{id}/confirm")
    @PreAuthorize("@perm.has('term.write')")
    public ApiResult<TermMapping> confirmCandidate(@PathVariable Long id,
                                                   @Valid @RequestBody ConfirmMappingRequest request) {
        return ApiResult.ok(service.confirmCandidate(id, request));
    }

    /**
     * 处置指定冲突记录，要求冲突当前处于 OPEN 状态。
     */
    @PostMapping("/conflicts/{id}/resolve")
    @PreAuthorize("@perm.has('term.write')")
    public ApiResult<MappingConflict> resolveConflict(@PathVariable Long id,
                                                      @Valid @RequestBody ResolveConflictRequest request) {
        return ApiResult.ok(service.resolveConflict(id, request));
    }

    /**
     * 分页查询当前租户的术语映射包，支持按 packageCode / status / scopeLevel / scopeCode 过滤。
     */
    @GetMapping("/packages")
    @PreAuthorize("@perm.has('term.read')")
    public ApiResult<PageResponse<TermMappingPackage>> packages(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String packageCode,
            @RequestParam(required = false) TermMappingPackageStatus status,
            @RequestParam(required = false) String scopeLevel,
            @RequestParam(required = false) String scopeCode) {
        return ApiResult.ok(service.pagePackages(
            new PageRequest(page, size, sort),
            new PackageFilter(packageCode, status, scopeLevel, scopeCode)
        ));
    }

    /**
     * 基于给定范围内的全部 CONFIRMED 映射构建一个新的 DRAFT 状态术语映射包。
     *
     * <p>范围内若无任何已确认映射则拒绝构包。
     */
    @PostMapping("/packages")
    @PreAuthorize("@perm.has('term.write')")
    public ApiResult<TermMappingPackage> buildPackage(@Valid @RequestBody BuildTerminologyPackageRequest request) {
        return ApiResult.ok(service.buildPackage(request));
    }

    /**
     * 把指定 DRAFT/GRAY 映射包升级为 GRAY 或 PUBLISHED。
     *
     * <p>FULL 模式发布时，会把同 (packageCode + scope) 下旧 PUBLISHED 包置为 SUPERSEDED；
     * 同步写入一条 PUBLISH 发布事件流水。
     */
    @PostMapping("/packages/{id}/publish")
    @PreAuthorize("@perm.has('term.publish')")
    public ApiResult<TermMappingPackage> publishPackage(@PathVariable Long id,
                                                        @Valid @RequestBody PublishTerminologyPackageRequest request) {
        return ApiResult.ok(service.publishPackage(id, request));
    }

    /**
     * 把当前 PUBLISHED/GRAY 的映射包回滚到指定历史版本，同时写一条 ROLLBACK 事件流水。
     *
     * <p>目标包必须与当前包同 (packageCode + scope)，且处于 PUBLISHED 或 SUPERSEDED 状态。
     */
    @PostMapping("/packages/{id}/rollback")
    @PreAuthorize("@perm.has('package.rollback')")
    public ApiResult<TermMappingPackage> rollbackPackage(@PathVariable Long id,
                                                         @Valid @RequestBody RollbackTerminologyPackageRequest request) {
        return ApiResult.ok(service.rollbackPackage(id, request));
    }
}
