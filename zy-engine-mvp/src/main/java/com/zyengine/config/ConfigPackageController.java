package com.zyengine.config;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/config-packages")
public class ConfigPackageController {
    private final ConfigPackageService configPackageService;

    public ConfigPackageController(ConfigPackageService configPackageService) {
        this.configPackageService = configPackageService;
    }

    @PostMapping
    public ApiResult<List<Map<String, Object>>> importPackages(@RequestBody Object request) {
        return ApiResult.success(configPackageService.importPackages(request));
    }

    @PostMapping("/import")
    public ApiResult<List<Map<String, Object>>> importPackagesAlias(@RequestBody Object request) {
        return ApiResult.success(configPackageService.importPackages(request));
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> listPackages(@RequestParam(required = false) String tenantId,
                                                            @RequestParam(required = false) String tenant_id,
                                                            @RequestParam(required = false) String assetType,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String scopeLevel,
                                                            @RequestParam(required = false) String scopeCode) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("tenantId", tenantId == null ? tenant_id : tenantId);
        filters.put("assetType", assetType);
        filters.put("status", status);
        filters.put("scopeLevel", scopeLevel);
        filters.put("scopeCode", scopeCode);
        return ApiResult.success(configPackageService.listPackages(filters));
    }

    @GetMapping("/{packageCode}")
    public ApiResult<Map<String, Object>> getLatestPackage(@PathVariable String packageCode,
                                                           @RequestParam(required = false) String packageVersion,
                                                           @RequestParam(required = false) String tenantId,
                                                           @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(configPackageService.getPackage(packageCode, packageVersion,
                tenantId == null ? tenant_id : tenantId));
    }

    @GetMapping("/{packageCode}/{packageVersion}")
    public ApiResult<Map<String, Object>> getPackage(@PathVariable String packageCode,
                                                     @PathVariable String packageVersion,
                                                     @RequestParam(required = false) String tenantId,
                                                     @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(configPackageService.getPackage(packageCode, packageVersion,
                tenantId == null ? tenant_id : tenantId));
    }

    @GetMapping("/{packageCode}/{packageVersion}/review")
    public ApiResult<Map<String, Object>> reviewPackageReadOnly(@PathVariable String packageCode,
                                                                @PathVariable String packageVersion,
                                                                @RequestParam(required = false) String tenantId,
                                                                @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(configPackageService.reviewPackage(packageCode, packageVersion,
                tenantId == null ? tenant_id : tenantId, null));
    }

    @PostMapping("/{packageCode}/{packageVersion}/review")
    public ApiResult<Map<String, Object>> reviewPackage(@PathVariable String packageCode,
                                                        @PathVariable String packageVersion,
                                                        @RequestParam(required = false) String tenantId,
                                                        @RequestParam(required = false) String tenant_id,
                                                        @RequestBody(required = false) Map<String, Object> request) {
        return ApiResult.success(configPackageService.reviewPackage(packageCode, packageVersion,
                tenantId == null ? tenant_id : tenantId, request));
    }

    @PostMapping("/{packageCode}/{packageVersion}/publish")
    public ApiResult<Map<String, Object>> publishPackage(@PathVariable String packageCode,
                                                         @PathVariable String packageVersion,
                                                         @RequestParam(required = false) String tenantId,
                                                         @RequestParam(required = false) String tenant_id,
                                                         @RequestBody(required = false) Map<String, Object> request) {
        return ApiResult.success(configPackageService.publishPackage(packageCode, packageVersion,
                tenantId == null ? tenant_id : tenantId, request));
    }

    @PostMapping("/{packageCode}/{packageVersion}/export")
    public ApiResult<Map<String, Object>> exportPackage(@PathVariable String packageCode,
                                                        @PathVariable String packageVersion,
                                                        @RequestParam(required = false) String tenantId,
                                                        @RequestParam(required = false) String tenant_id) {
        return ApiResult.success(configPackageService.exportPackage(packageCode, packageVersion,
                tenantId == null ? tenant_id : tenantId));
    }
}
