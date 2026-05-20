package com.medkernel.security;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 身份绑定管理 API：多身份源绑定、合并和解绑。
 */
@RestController
@RequestMapping("/api/security/identity")
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
    @GetMapping("/bindings/user/{userId}")
    public ApiResult<List<Map<String, Object>>> listBindingsByUser(@PathVariable Long userId,
                                                                    HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(toBindingViews(bindingService.listBindingsByUser(resolveTenantId(orgCtx), userId)));
    }

    @GetMapping("/bindings/{bindingId}")
    public ApiResult<Map<String, Object>> getBinding(@PathVariable Long bindingId) {
        IdentityBinding binding = bindingService.getBindingById(bindingId);
        if (binding == null) {
            return ApiResult.notFound("identity binding not found");
        }
        return ApiResult.success(toBindingView(binding));
    }

    /**
     * 绑定外部身份到平台用户。
     */
    @PostMapping("/bindings")
    public ApiResult<Long> bindIdentity(@RequestBody Map<String, Object> body,
                                        HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        Long userId = longValue(body, "user_id", "userId");
        Long providerId = longValue(body, "provider_id", "providerId");
        String externalSubject = stringValue(body, "external_subject", "externalSubject");
        String externalDisplayName = stringValue(body, "external_display_name", "externalDisplayName");
        String operator = resolveOperator(httpRequest);
        try {
            IdentityBinding binding = bindingService.bindIdentity(
                    resolveTenantId(orgCtx), userId, providerId, externalSubject, externalDisplayName, operator);
            return ApiResult.success(binding.getId());
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DUPLICATE_EXTERNAL_ACCOUNT, ex.getMessage());
        }
    }

    /**
     * 解绑：标记为 DETACHED，保留审计。
     */
    @DeleteMapping("/bindings/{bindingId}")
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
        Long sourceUserId = longValue(body, "source_user_id", "sourceUserId");
        Long targetUserId = longValue(body, "target_user_id", "targetUserId");
        String mergeReason = stringValue(body, "merge_reason", "mergeReason");
        String operator = resolveOperator(httpRequest);
        return ApiResult.success(bindingService.mergeBindings(
                resolveTenantId(orgCtx), sourceUserId, targetUserId, mergeReason, operator));
    }

    @GetMapping("/merge/user/{userId}")
    public ApiResult<List<Map<String, Object>>> listMergeRecords(@PathVariable Long userId,
                                                                  HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(bindingService.listMergeRecordsByUser(resolveTenantId(orgCtx), userId));
    }

    @GetMapping("/unbind/user/{userId}")
    public ApiResult<List<Map<String, Object>>> listUnbindRecords(@PathVariable Long userId,
                                                                   HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(bindingService.listUnbindRecordsByUser(resolveTenantId(orgCtx), userId));
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

    private Long longValue(Map<String, Object> body, String snakeKey, String camelKey) {
        Object value = body.get(snakeKey);
        if (value == null) {
            value = body.get(camelKey);
        }
        if (value == null) {
            throw new IllegalArgumentException(snakeKey + " is required");
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String stringValue(Map<String, Object> body, String snakeKey, String camelKey) {
        Object value = body.get(snakeKey);
        if (value == null) {
            value = body.get(camelKey);
        }
        return value == null ? "" : String.valueOf(value);
    }

    private List<Map<String, Object>> toBindingViews(List<IdentityBinding> bindings) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (IdentityBinding binding : bindings) {
            result.add(toBindingView(binding));
        }
        return result;
    }

    private Map<String, Object> toBindingView(IdentityBinding binding) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", binding.getId());
        view.put("tenant_id", binding.getTenantId());
        view.put("user_id", binding.getUserId());
        view.put("provider_id", binding.getProviderId());
        view.put("external_subject", binding.getExternalSubject());
        view.put("external_org_code", binding.getExternalOrgCode());
        view.put("external_display_name", binding.getExternalDisplayName());
        view.put("binding_status", binding.getBindingStatus());
        view.put("last_verified_time", binding.getLastVerifiedTime());
        view.put("created_by", binding.getCreatedBy());
        view.put("created_time", binding.getCreatedTime());
        view.put("updated_by", binding.getUpdatedBy());
        view.put("updated_time", binding.getUpdatedTime());
        return view;
    }
}
