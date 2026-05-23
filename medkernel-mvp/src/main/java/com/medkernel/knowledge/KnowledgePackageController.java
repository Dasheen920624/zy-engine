package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.knowledge.dto.ExportPackageRequest;
import com.medkernel.knowledge.dto.ImportPackageRequest;
import com.medkernel.knowledge.dto.PackageDetailResponse;
import com.medkernel.knowledge.dto.PackageResponse;
import com.medkernel.knowledge.dto.SyncPackageRequest;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "Knowledge Package")
@RestController
@RequestMapping("/api/knowledge/packages")
public class KnowledgePackageController {
    private static final Logger log = LoggerFactory.getLogger(KnowledgePackageController.class);

    private final KnowledgePackageService knowledgePackageService;
    private final OrganizationContextService organizationContextService;

    public KnowledgePackageController(KnowledgePackageService knowledgePackageService,
                                      OrganizationContextService organizationContextService) {
        this.knowledgePackageService = knowledgePackageService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Export package")
    @PostMapping("/export")
    public ApiResult<PackageResponse> exportPackage(
            @Valid @RequestBody ExportPackageRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            KnowledgePackage pkg = toKnowledgePackage(request, orgContext);
            KnowledgePackage exported = knowledgePackageService.exportPackage(pkg);
            return ApiResult.success(PackageResponse.fromEntity(exported));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Knowledge package export failed", e);
            return ApiResult.failure(ErrorCode.UNKNOWN_ERROR, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "Import package")
    @PostMapping("/{packageId}/import")
    public ApiResult<PackageResponse> importPackage(
            @PathVariable Long packageId,
            @RequestBody(required = false) ImportPackageRequest request,
            HttpServletRequest httpRequest) {
        String conflictStrategy = "SKIP";
        if (request != null && request.getConflictStrategy() != null) {
            conflictStrategy = request.getConflictStrategy();
        }
        try {
            KnowledgePackage pkg = knowledgePackageService.importPackage(packageId, conflictStrategy);
            return ApiResult.success(PackageResponse.fromEntity(pkg));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Knowledge package import failed", e);
            return ApiResult.failure(ErrorCode.UNKNOWN_ERROR, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "Preview import")
    @PostMapping("/{packageId}/preview")
    public ApiResult<PackageDetailResponse> previewImport(
            @PathVariable Long packageId,
            HttpServletRequest httpRequest) {
        try {
            KnowledgePackage pkg = knowledgePackageService.getPackage(packageId);
            return ApiResult.success(PackageDetailResponse.fromEntity(pkg));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "List packages")
    @GetMapping
    public ApiResult<List<PackageResponse>> listPackages(
            @RequestParam(required = false) Long tenant_id,
            @RequestParam(required = false) String status,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Long effectiveTenantId = tenant_id;
        if (effectiveTenantId == null && orgContext.getTenantId() != null) {
            try {
                effectiveTenantId = Long.valueOf(orgContext.getTenantId());
            } catch (NumberFormatException ignored) {
                // 非数字 tenantId 不转换
            }
        }
        List<KnowledgePackage> packages = knowledgePackageService.listPackages(effectiveTenantId, status);
        List<PackageResponse> views = new ArrayList<PackageResponse>();
        for (KnowledgePackage pkg : packages) {
            views.add(PackageResponse.fromEntity(pkg));
        }
        return ApiResult.success(views);
    }

    @Operation(summary = "Get package")
    @GetMapping("/{packageId}")
    public ApiResult<PackageDetailResponse> getPackage(
            @PathVariable Long packageId,
            HttpServletRequest httpRequest) {
        try {
            KnowledgePackage pkg = knowledgePackageService.getPackage(packageId);
            return ApiResult.success(PackageDetailResponse.fromEntity(pkg));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    @Operation(summary = "Sync package")
    @PostMapping("/{packageId}/sync")
    public ApiResult<PackageResponse> syncPackage(
            @PathVariable Long packageId,
            @RequestBody(required = false) SyncPackageRequest request,
            HttpServletRequest httpRequest) {
        String syncMode = "MANUAL";
        if (request != null && request.getSyncMode() != null) {
            syncMode = request.getSyncMode();
        }
        try {
            KnowledgePackage pkg = knowledgePackageService.syncPackage(packageId, syncMode);
            return ApiResult.success(PackageResponse.fromEntity(pkg));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Knowledge package sync failed", e);
            return ApiResult.failure(ErrorCode.UNKNOWN_ERROR, "操作失败，请稍后重试");
        }
    }

    @Operation(summary = "Get sync status")
    @GetMapping("/{packageId}/sync-status")
    public ApiResult<PackageResponse> getSyncStatus(
            @PathVariable Long packageId,
            HttpServletRequest httpRequest) {
        try {
            KnowledgePackage pkg = knowledgePackageService.getPackage(packageId);
            return ApiResult.success(PackageResponse.fromEntity(pkg));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
        }
    }

    // ==================== 内部方法 ====================

    private KnowledgePackage toKnowledgePackage(ExportPackageRequest req, OrganizationContext orgContext) {
        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setPackageCode(req.getPackageCode());
        pkg.setPackageName(req.getPackageName());
        pkg.setPackageVersion(req.getPackageVersion());
        pkg.setDescription(req.getDescription());
        pkg.setExportType(req.getExportType() != null ? req.getExportType() : "FULL");
        pkg.setSourceTenantId(req.getSourceTenantId() != null ? req.getSourceTenantId()
                : orgContext != null ? orgContext.getTenantId() : null);
        pkg.setSourceTenantName(req.getSourceTenantName());
        pkg.setTargetTenantId(req.getTargetTenantId());
        pkg.setTargetTenantName(req.getTargetTenantName());
        pkg.setConflictStrategy(req.getConflictStrategy() != null ? req.getConflictStrategy() : "SKIP");
        pkg.setSyncMode(req.getSyncMode() != null ? req.getSyncMode() : "MANUAL");
        pkg.setCreatedBy(req.getCreatedBy());

        if (orgContext != null && orgContext.getTenantId() != null) {
            try {
                pkg.setTenantId(Long.valueOf(orgContext.getTenantId()));
            } catch (NumberFormatException ignored) {
                // 非数字 tenantId 不设置
            }
        }
        return pkg;
    }
}
