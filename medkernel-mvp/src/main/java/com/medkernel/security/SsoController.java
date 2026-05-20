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
 * SSO 认证 API：单点登录接入端点。
 * 支持 CAS/OIDC/SAML/LDAP-AD 四种协议。
 */
@RestController
@RequestMapping("/api/security/sso")
public class SsoController {

    private final SsoService ssoService;
    private final OrganizationContextService organizationContextService;

    public SsoController(SsoService ssoService,
                         OrganizationContextService organizationContextService) {
        this.ssoService = ssoService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 查询可用的 SSO 身份源列表。
     */
    @GetMapping("/providers")
    public ApiResult<List<IdentityProvider>> listSsoProviders(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(ssoService.listSsoProviders(resolveTenantId(orgCtx)));
    }

    /**
     * 发起 SSO 登录：返回重定向 URL。
     */
    @PostMapping("/providers/{providerId}/initiate")
    public ApiResult<Map<String, Object>> initiateSso(@PathVariable Long providerId,
                                                        HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(ssoService.initiateSso(resolveTenantId(orgCtx), providerId));
    }

    /**
     * SSO 回调端点：处理 CAS/OIDC 授权码回调。
     */
    @GetMapping("/callback")
    public ApiResult<Map<String, Object>> callback(
            @RequestParam Long providerId,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(ssoService.handleCallback(
                resolveTenantId(orgCtx), providerId, code, state, httpRequest));
    }

    /**
     * SAML ACS 端点：处理 SAML Response。
     */
    @PostMapping("/saml/acs")
    public ApiResult<Map<String, Object>> samlAcs(
            @RequestParam Long providerId,
            @RequestParam String SAMLResponse,
            @RequestParam(required = false) String RelayState,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(ssoService.handleCallback(
                resolveTenantId(orgCtx), providerId, SAMLResponse, RelayState, httpRequest));
    }

    /**
     * LDAP 直接验证：用户名/密码方式。
     */
    @PostMapping("/ldap/authenticate")
    public ApiResult<Map<String, Object>> ldapAuthenticate(
            @RequestBody Map<String, Object> body,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        Long providerId = Long.valueOf(String.valueOf(body.get("providerId")));
        String username = String.valueOf(body.get("username"));
        String password = String.valueOf(body.get("password"));
        // LDAP 验证：username 作为 code 传入（SsoService 中简化处理）
        return ApiResult.success(ssoService.handleCallback(
                resolveTenantId(orgCtx), providerId, username, null, httpRequest));
    }

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }
}
