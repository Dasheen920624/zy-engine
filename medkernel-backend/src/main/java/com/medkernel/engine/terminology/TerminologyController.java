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

@RestController
@RequestMapping("/api/v1/engine/terminology")
@DataScope(requireTenant = true)
public class TerminologyController {

    private final TerminologyService service;

    public TerminologyController(TerminologyService service) {
        this.service = service;
    }

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

    @PostMapping("/candidates/{id}/confirm")
    @PreAuthorize("@perm.has('term.write')")
    public ApiResult<TermMapping> confirmCandidate(@PathVariable Long id,
                                                   @Valid @RequestBody ConfirmMappingRequest request) {
        return ApiResult.ok(service.confirmCandidate(id, request));
    }

    @PostMapping("/conflicts/{id}/resolve")
    @PreAuthorize("@perm.has('term.write')")
    public ApiResult<MappingConflict> resolveConflict(@PathVariable Long id,
                                                      @Valid @RequestBody ResolveConflictRequest request) {
        return ApiResult.ok(service.resolveConflict(id, request));
    }

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

    @PostMapping("/packages")
    @PreAuthorize("@perm.has('term.write')")
    public ApiResult<TermMappingPackage> buildPackage(@Valid @RequestBody BuildTerminologyPackageRequest request) {
        return ApiResult.ok(service.buildPackage(request));
    }

    @PostMapping("/packages/{id}/publish")
    @PreAuthorize("@perm.has('term.publish')")
    public ApiResult<TermMappingPackage> publishPackage(@PathVariable Long id,
                                                        @Valid @RequestBody PublishTerminologyPackageRequest request) {
        return ApiResult.ok(service.publishPackage(id, request));
    }

    @PostMapping("/packages/{id}/rollback")
    @PreAuthorize("@perm.has('package.rollback')")
    public ApiResult<TermMappingPackage> rollbackPackage(@PathVariable Long id,
                                                         @Valid @RequestBody RollbackTerminologyPackageRequest request) {
        return ApiResult.ok(service.rollbackPackage(id, request));
    }
}
