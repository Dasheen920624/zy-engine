package com.medkernel.security;

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
 * 用户同步 API：院内身份源用户同步管理。
 */
@RestController
@RequestMapping("/api/security/sync")
public class UserSyncController {

    private final UserSyncService userSyncService;
    private final OrganizationContextService organizationContextService;

    public UserSyncController(UserSyncService userSyncService,
                              OrganizationContextService organizationContextService) {
        this.userSyncService = userSyncService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 查询身份源列表。
     */
    @GetMapping("/providers")
    public ApiResult<List<IdentityProvider>> listProviders(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(userSyncService.listProviders(resolveTenantId(orgCtx)));
    }

    /**
     * 保存身份源配置。
     */
    @PostMapping("/providers")
    public ApiResult<IdentityProvider> saveProvider(@RequestBody IdentityProvider provider,
                                                     HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, toMap(provider));
        provider.setTenantId(resolveTenantId(orgCtx));
        return ApiResult.success(userSyncService.saveProvider(provider));
    }

    /**
     * 全量同步。
     */
    @PostMapping("/providers/{providerId}/full")
    public ApiResult<SyncReport> syncFull(@PathVariable Long providerId,
                                           HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        String operator = resolveOperator(httpRequest);
        return ApiResult.success(userSyncService.syncAll(resolveTenantId(orgCtx), providerId, operator));
    }

    /**
     * 增量同步。
     */
    @PostMapping("/providers/{providerId}/incremental")
    public ApiResult<SyncReport> syncIncremental(@PathVariable Long providerId,
                                                   HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        String operator = resolveOperator(httpRequest);
        return ApiResult.success(userSyncService.syncIncremental(resolveTenantId(orgCtx), providerId, operator));
    }

    /**
     * 手动同步。
     */
    @PostMapping("/providers/{providerId}/manual")
    public ApiResult<SyncReport> syncManual(@PathVariable Long providerId,
                                              HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        String operator = resolveOperator(httpRequest);
        return ApiResult.success(userSyncService.syncManual(resolveTenantId(orgCtx), providerId, operator));
    }

    /**
     * 查询同步日志。
     */
    @GetMapping("/logs")
    public ApiResult<List<Map<String, Object>>> listSyncLogs(
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false, defaultValue = "20") int limit,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(userSyncService.listSyncLogs(resolveTenantId(orgCtx), providerId, limit));
    }

    // ---- 内部方法 ----

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }

    private String resolveOperator(HttpServletRequest request) {
        String username = request.getHeader("X-Username");
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return "system";
    }

    private Map<String, Object> toMap(IdentityProvider provider) {
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        if (provider.getTenantId() != null) {
            map.put("tenant_id", String.valueOf(provider.getTenantId()));
        }
        if (provider.getProviderType() != null) {
            map.put("provider_type", provider.getProviderType());
        }
        if (provider.getProviderName() != null) {
            map.put("provider_name", provider.getProviderName());
        }
        return map;
    }
}
