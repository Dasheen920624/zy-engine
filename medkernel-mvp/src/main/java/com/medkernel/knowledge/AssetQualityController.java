package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.knowledge.dto.QualityCheckRequest;
import com.medkernel.knowledge.dto.ResolveFindingRequest;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
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
import java.util.List;
import java.util.Map;

/**
 * AI 候选资产自动质检 API：执行质检、查询发现、解决发现、摘要统计。
 */
@Tag(name = "Asset Quality")
@RestController
@RequestMapping("/api/knowledge/quality")
public class AssetQualityController {

    private final AssetQualityService qualityService;
    private final OrganizationContextService organizationContextService;

    public AssetQualityController(AssetQualityService qualityService,
                                   OrganizationContextService organizationContextService) {
        this.qualityService = qualityService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 执行质检（支持 assetType/assetCode 过滤）。
     */
    @Operation(summary = "Run quality check")
    @PostMapping("/check")
    public ApiResult<List<QualityFinding>> runQualityCheck(
            @RequestBody(required = false) QualityCheckRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = resolveTenantId(orgCtx);
        String assetType = request != null ? request.getAssetType() : null;
        String assetCode = request != null ? request.getAssetCode() : null;
        return ApiResult.success(qualityService.runQualityCheck(tenantId, assetType, assetCode));
    }

    /**
     * 全量质检。
     */
    @Operation(summary = "Run full quality check")
    @PostMapping("/full-check")
    public ApiResult<List<QualityFinding>> runFullQualityCheck(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = resolveTenantId(orgCtx);
        return ApiResult.success(qualityService.runFullQualityCheck(tenantId));
    }

    /**
     * 查询质检发现。
     */
    @Operation(summary = "List findings")
    @GetMapping("/findings")
    public ApiResult<List<QualityFinding>> listFindings(
            @RequestParam(required = false) String findingType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "50") int limit,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = resolveTenantId(orgCtx);
        return ApiResult.success(qualityService.listFindings(tenantId, findingType, severity, status, limit));
    }

    /**
     * 解决质检发现。
     */
    @Operation(summary = "Resolve finding")
    @PostMapping("/findings/{findingId}/resolve")
    public ApiResult<String> resolveFinding(@PathVariable Long findingId,
                                              @Valid @RequestBody ResolveFindingRequest request,
                                              HttpServletRequest httpRequest) {
        String resolvedBy = request.getResolvedBy() != null ? request.getResolvedBy() : "system";
        String resolutionNote = request.getResolutionNote();
        qualityService.resolveFinding(findingId, resolvedBy, resolutionNote);
        return ApiResult.success("质检发现已解决");
    }

    /**
     * 质检摘要统计。
     */
    @Operation(summary = "Get quality summary")
    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> getQualitySummary(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = resolveTenantId(orgCtx);
        return ApiResult.success(qualityService.getQualitySummary(tenantId));
    }

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }
}
