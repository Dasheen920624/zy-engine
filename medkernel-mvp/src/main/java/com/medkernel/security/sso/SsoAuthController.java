package com.medkernel.security.sso;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * SSO 认证 API。
 * 提供 SSO 登录发起、回调处理、LDAP 直接认证、会话注销。
 */
@RestController
@RequestMapping("/api/auth/sso")
public class SsoAuthController {
    private final SsoAuthService ssoAuthService;
    private final OrganizationContextService organizationContextService;

    public SsoAuthController(SsoAuthService ssoAuthService,
                             OrganizationContextService organizationContextService) {
        this.ssoAuthService = ssoAuthService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 发起 SSO 登录：返回外部 SSO 系统的登录跳转 URL。
     * GET /api/auth/sso/login?provider=cas
     */
    @GetMapping("/login")
    public ApiResult<Map<String, Object>> initiateLogin(
            @RequestParam String provider,
            @RequestParam(required = false) String callback_base_url,
            HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        String callbackBaseUrl = callback_base_url != null ? callback_base_url : getDefaultCallbackBaseUrl(httpRequest);
        try {
            Map<String, Object> result = ssoAuthService.initiateLogin(provider, tenantId, callbackBaseUrl);
            return ApiResult.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * SSO 回调端点：接收外部 SSO 系统的回调。
     * GET /api/auth/sso/callback?provider=cas&ticket=xxx&state=xxx
     */
    @GetMapping("/callback")
    public ApiResult<Map<String, Object>> callback(
            @RequestParam String provider,
            @RequestParam(required = false) String ticket,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String SAMLResponse,
            @RequestParam(required = false) String state,
            HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        String callbackBaseUrl = getDefaultCallbackBaseUrl(httpRequest);

        // 根据协议类型选择凭据
        String credential = ticket;
        if (credential == null) credential = code;
        if (credential == null) credential = SAMLResponse;

        if (credential == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "No SSO credential provided (ticket/code/SAMLResponse)");
        }

        try {
            Map<String, Object> result = ssoAuthService.handleCallback(provider, credential, tenantId, callbackBaseUrl);
            return ApiResult.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * LDAP 直接认证（非浏览器跳转）。
     * POST /api/auth/sso/ldap
     */
    @PostMapping("/ldap")
    public ApiResult<Map<String, Object>> ldapLogin(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        String provider = (String) request.get("provider");
        String username = (String) request.get("username");
        String password = (String) request.get("password");

        if (provider == null || username == null || password == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "provider, username and password are required");
        }

        try {
            Map<String, Object> result = ssoAuthService.ldapDirectLogin(provider, username, password, tenantId);
            return ApiResult.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    /**
     * 注销 SSO 会话。
     * POST /api/auth/sso/logout
     */
    @PostMapping("/logout")
    public ApiResult<Boolean> logout(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("session_id");
        if (sessionId == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "session_id is required");
        }
        boolean success = ssoAuthService.logoutSession(sessionId);
        return ApiResult.success(success);
    }

    /**
     * 列出可用的 SSO 身份源。
     * GET /api/auth/sso/providers
     */
    @GetMapping("/providers")
    public ApiResult<Map<String, Object>> listProviders(HttpServletRequest httpRequest) {
        String tenantId = resolveTenantId(httpRequest);
        return ApiResult.success(ssoAuthService.listAvailableProviders(tenantId));
    }

    private String resolveTenantId(HttpServletRequest httpRequest) {
        String tenantId = httpRequest.getHeader("X-Tenant-Id");
        if (tenantId == null) tenantId = "TENANT_DEMO";
        return tenantId;
    }

    private String getDefaultCallbackBaseUrl(HttpServletRequest httpRequest) {
        String scheme = httpRequest.getScheme();
        String serverName = httpRequest.getServerName();
        int port = httpRequest.getServerPort();
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            return scheme + "://" + serverName;
        }
        return scheme + "://" + serverName + ":" + port;
    }
}
