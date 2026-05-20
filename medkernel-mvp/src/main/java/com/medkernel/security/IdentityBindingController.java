package com.medkernel.security;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 身份绑定管理 API：多身份源绑定、合并和解绑。
 */
@RestController
@RequestMapping("/api/security/bindings")
public class IdentityBindingController {

    private final IdentityBindingService bindingService;
    private final OrganizationContextService organizationContextService;

    public IdentityBindingController(IdentityBindingService bindingService,
                                      OrganizationContextService organizationContextService) {
        this.bindingService = bindingService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 查询用户的身份绑定列表。
     */
    @GetMapping("/users/{userId}")
    public ApiResult<List<IdentityBinding>> listBindingsByUser(@PathVariable Long userId,
                                                                 HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(bindingService.listBindingsByUser(resolveTenantId(orgCtx), userId));
    }

    /**
     * 绑定外部身份到平台用户。
     */
    @PostMapping("/bind")
    public ApiResult<IdentityBinding> bindIdentity(@RequestBody Map<String, Object> body,
                                                     HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        Long userId = Long.valueOf(String.valueOf(body.get("userId")));
        Long providerId = Long.valueOf(String.valueOf(body.get("providerId")));
        String externalSubject = String.valueOf(body.get("externalSubject"));
        String externalDisplayName = String.valueOf(body.getOrDefault("externalDisplayName", ""));
        String operator = resolveOperator(httpRequest);
        return ApiResult.success(bindingService.bindIdentity(
                resolveTenantId(orgCtx), userId, providerId, externalSubject, externalDisplayName, operator));
    }

    /**
     * 解绑：标记为 DETACHED，保留审计。
     */
    @DeleteMapping("/{bindingId}")
    public ApiResult<String> unbindIdentity(@PathVariable Long bindingId,
                                              HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        String operator = resolveOperator(httpRequest);
        bindingService.unbindIdentity(bindingId, operator);
        return ApiResult.success("解绑成功");
    }

    /**
     * 合并绑定：将源用户绑定转移到目标用户。
     */
    @PostMapping("/merge")
    public ApiResult<Map<String, Object>> mergeBindings(@RequestBody Map<String, Object> body,
                                                          HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        Long sourceUserId = Long.valueOf(String.valueOf(body.get("sourceUserId")));
        Long targetUserId = Long.valueOf(String.valueOf(body.get("targetUserId")));
        String operator = resolveOperator(httpRequest);
        return ApiResult.success(bindingService.mergeBindings(
                resolveTenantId(orgCtx), sourceUserId, targetUserId, operator));
    }

    /**
     * 查找冲突绑定。
     */
    @GetMapping("/conflicts")
    public ApiResult<List<Map<String, Object>>> findConflicts(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(bindingService.findConflicts(resolveTenantId(orgCtx)));
    }

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
}
