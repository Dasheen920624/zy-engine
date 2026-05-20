package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * AI 候选资产自动质检 API：执行质检、查询发现、解决发现、摘要统计。
 */
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
    @PostMapping("/check")
    public ApiResult<List<QualityFinding>> runQualityCheck(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = resolveTenantId(orgCtx);
        String assetType = body != null ? body.get("assetType") : null;
        String assetCode = body != null ? body.get("assetCode") : null;
        return ApiResult.success(qualityService.runQualityCheck(tenantId, assetType, assetCode));
    }

    /**
     * 全量质检。
     */
    @PostMapping("/full-check")
    public ApiResult<List<QualityFinding>> runFullQualityCheck(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        Long tenantId = resolveTenantId(orgCtx);
        return ApiResult.success(qualityService.runFullQualityCheck(tenantId));
    }

    /**
     * 查询质检发现。
     */
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
    @PostMapping("/findings/{findingId}/resolve")
    public ApiResult<String> resolveFinding(@PathVariable Long findingId,
                                              @RequestBody Map<String, String> body,
                                              HttpServletRequest httpRequest) {
        String resolvedBy = body.get("resolvedBy") != null ? body.get("resolvedBy") : "system";
        String resolutionNote = body.get("resolutionNote");
        qualityService.resolveFinding(findingId, resolvedBy, resolutionNote);
        return ApiResult.success("质检发现已解决");
    }

    /**
     * 质检摘要统计。
     */
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
