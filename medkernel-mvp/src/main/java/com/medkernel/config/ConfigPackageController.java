package com.medkernel.config;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Config Package")
@RestController
@RequestMapping("/api/config-packages")
public class ConfigPackageController {
    private final ConfigPackageService configPackageService;
    private final OrganizationContextService organizationContextService;

    public ConfigPackageController(ConfigPackageService configPackageService,
                                   OrganizationContextService organizationContextService) {
        this.configPackageService = configPackageService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Import packages")
    @PostMapping
    public ApiResult<List<Map<String, Object>>> importPackages(@RequestBody Object request,
                                                               HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(configPackageService.importPackages(request));
    }

    @Operation(summary = "Import packages alias")
    @PostMapping("/import")
    public ApiResult<List<Map<String, Object>>> importPackagesAlias(@RequestBody Object request,
                                                                     HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(configPackageService.importPackages(request));
    }

    @Operation(summary = "List packages")
    @GetMapping
    public ApiResult<List<Map<String, Object>>> listPackages(@RequestParam(required = false) String assetType,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String scopeLevel,
                                                            @RequestParam(required = false) String scopeCode,
                                                            HttpServletRequest httpRequest) {
        // 显式 query 参数优先；未显式声明的组织维度交给 OrgContext 解析（Header/Query → 默认值）
        Map<String, String> filters = new LinkedHashMap<String, String>();
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        filters.put("tenantId", orgContext.getTenantId());
        filters.put("groupCode", orgContext.getGroupCode());
        filters.put("hospitalCode", orgContext.getHospitalCode());
        filters.put("campusCode", orgContext.getCampusCode());
        filters.put("siteCode", orgContext.getSiteCode());
        filters.put("departmentCode", orgContext.getDepartmentCode());
        filters.put("assetType", assetType);
        filters.put("status", status);
        filters.put("scopeLevel", scopeLevel);
        filters.put("scopeCode", scopeCode);
        return ApiResult.success(configPackageService.listPackages(filters));
    }

    @Operation(summary = "Get latest package")
    @GetMapping("/{packageCode}")
    public ApiResult<Map<String, Object>> getLatestPackage(@PathVariable String packageCode,
                                                           @RequestParam(required = false) String packageVersion,
                                                           HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        return ApiResult.success(configPackageService.getPackage(packageCode, packageVersion,
                orgContext.getTenantId()));
    }

    @Operation(summary = "Get package")
    @GetMapping("/{packageCode}/{packageVersion}")
    public ApiResult<Map<String, Object>> getPackage(@PathVariable String packageCode,
                                                     @PathVariable String packageVersion,
                                                     HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        return ApiResult.success(configPackageService.getPackage(packageCode, packageVersion,
                orgContext.getTenantId()));
    }

    @Operation(summary = "Review package read only")
    @GetMapping("/{packageCode}/{packageVersion}/review")
    public ApiResult<Map<String, Object>> reviewPackageReadOnly(@PathVariable String packageCode,
                                                                @PathVariable String packageVersion,
                                                                HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        return ApiResult.success(configPackageService.reviewPackage(packageCode, packageVersion,
                orgContext.getTenantId(), null));
    }

    @Operation(summary = "Review package")
    @PostMapping("/{packageCode}/{packageVersion}/review")
    public ApiResult<Map<String, Object>> reviewPackage(@PathVariable String packageCode,
                                                        @PathVariable String packageVersion,
                                                        @RequestBody(required = false) Map<String, Object> request,
                                                        HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(configPackageService.reviewPackage(packageCode, packageVersion,
                orgContext.getTenantId(), request));
    }

    @Operation(summary = "Publish package")
    @PostMapping("/{packageCode}/{packageVersion}/publish")
    public ApiResult<Map<String, Object>> publishPackage(@PathVariable String packageCode,
                                                         @PathVariable String packageVersion,
                                                         @RequestBody(required = false) Map<String, Object> request,
                                                         HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(configPackageService.publishPackage(packageCode, packageVersion,
                orgContext.getTenantId(), request));
    }

    @Operation(summary = "Export package")
    @PostMapping("/{packageCode}/{packageVersion}/export")
    public ApiResult<Map<String, Object>> exportPackage(@PathVariable String packageCode,
                                                        @PathVariable String packageVersion,
                                                        HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        return ApiResult.success(configPackageService.exportPackage(packageCode, packageVersion,
                orgContext.getTenantId()));
    }
}
