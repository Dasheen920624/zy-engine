package com.medkernel.organization;

import com.medkernel.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Org Override")
@RestController
@RequestMapping("/api/organizations/override")
public class OrgOverrideController {
    private final OrgOverrideService orgOverrideService;
    private final OrganizationContextService organizationContextService;

    public OrgOverrideController(OrgOverrideService orgOverrideService,
                                 OrganizationContextService organizationContextService) {
        this.orgOverrideService = orgOverrideService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Import entries")
    @PostMapping("/entries")
    public ApiResult<List<Map<String, Object>>> importEntries(@RequestBody Object request,
                                                              HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(orgOverrideService.importEntries(request));
    }

    @Operation(summary = "List entries")
    @GetMapping("/entries")
    public ApiResult<List<Map<String, Object>>> listEntries(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String scopeLevel,
            @RequestParam(required = false) String scopeCode,
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String overrideKey,
            @RequestParam(required = false) String limit,
            HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("tenantId", tenantId);
        filters.put("scopeLevel", scopeLevel);
        filters.put("scopeCode", scopeCode);
        filters.put("assetType", assetType);
        filters.put("overrideKey", overrideKey);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(orgOverrideService.listEntries(filters));
    }

    @Operation(summary = "Compute override")
    @PostMapping("/compute")
    public ApiResult<Map<String, Object>> computeOverride(
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext context = organizationContextService.resolveWithBody(httpRequest, request);
        String assetType = null;
        if (request != null) {
            Object at = request.get("asset_type");
            if (at == null) at = request.get("assetType");
            assetType = at == null ? null : String.valueOf(at).trim().toUpperCase();
        }
        return ApiResult.success(orgOverrideService.computeOverride(context, assetType));
    }

    @Operation(summary = "Resolve override")
    @GetMapping("/resolve")
    public ApiResult<Map<String, Object>> resolveOverride(
            @RequestParam String overrideKey,
            @RequestParam(required = false) String assetType,
            HttpServletRequest httpRequest) {
        OrganizationContext context = organizationContextService.resolve(httpRequest);
        return ApiResult.success(orgOverrideService.resolveOverride(context, assetType, overrideKey));
    }

    @Operation(summary = "Entry count")
    @GetMapping("/count")
    public ApiResult<Map<String, Object>> entryCount(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("entry_count", orgOverrideService.entryCount());
        return ApiResult.success(result);
    }
}