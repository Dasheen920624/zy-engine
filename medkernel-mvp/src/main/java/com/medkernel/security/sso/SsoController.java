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
import java.util.List;
import java.util.Map;

/**
 * SSO 控制器：单点登录配置管理和登录回调处理。
 */
@RestController
@RequestMapping("/api/sso")
public class SsoController {

    private final SsoService ssoService;
    private final OrganizationContextService organizationContextService;

    public SsoController(SsoService ssoService, OrganizationContextService organizationContextService) {
        this.ssoService = ssoService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 获取 SSO 配置列表。
     */
    @GetMapping("/configs")
    public ApiResult<List<SsoConfig>> listConfigs(HttpServletRequest request) {
        Long tenantId = organizationContextService.resolve(request);
        List<SsoConfig> configs = ssoService.listSsoConfigs(tenantId);
        return ApiResult.success(configs);
    }

    /**
     * 根据 ID 获取 SSO 配置。
     */
    @GetMapping("/configs/detail")
    public ApiResult<SsoConfig> getConfig(@RequestParam("id") Long configId) {
        SsoConfig config = ssoService.getSsoConfig(configId);
        if (config == null) {
            return ApiResult.failure(ErrorCode.SSO_CONFIG_NOT_FOUND, "SSO 配置不存在");
        }
        return ApiResult.success(config);
    }

    /**
     * 保存 SSO 配置（创建或更新）。
     */
    @PostMapping("/configs")
    public ApiResult<Void> saveConfig(@RequestBody SsoConfig config, HttpServletRequest request) {
        Long tenantId = organizationContextService.resolve(request);
        config.setTenantId(tenantId);
        ssoService.saveSsoConfig(config);
        return ApiResult.success(null);
    }

    /**
     * 删除 SSO 配置。
     */
    @PostMapping("/configs/delete")
    public ApiResult<Void> deleteConfig(@RequestBody Map<String, Long> body) {
        Long configId = body.get("id");
        if (configId == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "配置 ID 不能为空");
        }
        ssoService.deleteSsoConfig(configId);
        return ApiResult.success(null);
    }

    /**
     * 处理 CAS 回调。
     */
    @PostMapping("/cas/callback")
    public ApiResult<Map<String, Object>> handleCasCallback(@RequestBody Map<String, String> body,
                                                            HttpServletRequest request) {
        Long tenantId = organizationContextService.resolve(request);
        String ticket = body.get("ticket");
        if (ticket == null || ticket.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "CAS 票据不能为空");
        }

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        SsoService.SsoLoginResult result = ssoService.handleSsoCallback(tenantId, "CAS", ticket, ipAddress, userAgent);
        return ApiResult.success(buildLoginResponse(result));
    }

    /**
     * 处理 OIDC 回调。
     */
    @PostMapping("/oidc/callback")
    public ApiResult<Map<String, Object>> handleOidcCallback(@RequestBody Map<String, String> body,
                                                              HttpServletRequest request) {
        Long tenantId = organizationContextService.resolve(request);
        String code = body.get("code");
        if (code == null || code.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "OIDC 授权码不能为空");
        }

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        SsoService.SsoLoginResult result = ssoService.handleSsoCallback(tenantId, "OIDC", code, ipAddress, userAgent);
        return ApiResult.success(buildLoginResponse(result));
    }

    /**
     * 处理 SAML 回调。
     */
    @PostMapping("/saml/callback")
    public ApiResult<Map<String, Object>> handleSamlCallback(@RequestBody Map<String, String> body,
                                                              HttpServletRequest request) {
        Long tenantId = organizationContextService.resolve(request);
        String samlResponse = body.get("SAMLResponse");
        if (samlResponse == null || samlResponse.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "SAML 响应不能为空");
        }

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        SsoService.SsoLoginResult result = ssoService.handleSsoCallback(tenantId, "SAML", samlResponse, ipAddress, userAgent);
        return ApiResult.success(buildLoginResponse(result));
    }

    /**
     * 处理 LDAP-AD 登录。
     */
    @PostMapping("/ldap/login")
    public ApiResult<Map<String, Object>> handleLdapLogin(@RequestBody Map<String, String> body,
                                                           HttpServletRequest request) {
        Long tenantId = organizationContextService.resolve(request);
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "用户名和密码不能为空");
        }

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 将用户名和密码作为回调数据传递
        String callbackData = username + ":" + password;
        SsoService.SsoLoginResult result = ssoService.handleSsoCallback(tenantId, "LDAP-AD", callbackData, ipAddress, userAgent);
        return ApiResult.success(buildLoginResponse(result));
    }

    /**
     * SSO 登出。
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long tenantId = organizationContextService.resolve(request);
        Long userId = com.medkernel.security.SecurityContext.getUserId();
        String sessionToken = body.get("session_token");

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        ssoService.handleSsoLogout(tenantId, userId, sessionToken, ipAddress, userAgent);
        return ApiResult.success(null);
    }

    /**
     * 获取当前用户的 SSO 会话列表。
     */
    @GetMapping("/sessions")
    public ApiResult<List<SsoSession>> listSessions(HttpServletRequest request) {
        Long tenantId = organizationContextService.resolve(request);
        Long userId = com.medkernel.security.SecurityContext.getUserId();
        List<SsoSession> sessions = ssoService.listUserSessions(tenantId, userId);
        return ApiResult.success(sessions);
    }

    /**
     * 获取 SSO 审计日志。
     */
    @GetMapping("/audit-logs")
    public ApiResult<List<SsoAuditLog>> listAuditLogs(@RequestParam(value = "limit", defaultValue = "100") int limit,
                                                       HttpServletRequest request) {
        Long tenantId = organizationContextService.resolve(request);
        List<SsoAuditLog> logs = ssoService.listAuditLogs(tenantId, limit);
        return ApiResult.success(logs);
    }

    /**
     * 健康检查。
     */
    @GetMapping("/health")
    public ApiResult<Map<String, String>> health() {
        java.util.LinkedHashMap<String, String> data = new java.util.LinkedHashMap<>();
        data.put("status", "ok");
        data.put("module", "sso");
        return ApiResult.success(data);
    }

    private Map<String, Object> buildLoginResponse(SsoService.SsoLoginResult result) {
        java.util.LinkedHashMap<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("token", result.getToken());
        response.put("user", buildUserInfo(result.getUser()));
        response.put("session", buildSessionInfo(result.getSession()));
        return response;
    }

    private Map<String, Object> buildUserInfo(com.medkernel.security.SecurityUser user) {
        java.util.LinkedHashMap<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("tenant_id", user.getTenantId());
        info.put("username", user.getUsername());
        info.put("display_name", user.getDisplayName());
        info.put("email", user.getEmail());
        info.put("phone", user.getPhone());
        info.put("avatar_url", user.getAvatarUrl());
        info.put("status", user.getStatus());
        info.put("roles", user.getRoles());
        info.put("permissions", user.getPermissions());
        info.put("org_scopes", user.getOrgScopes());
        info.put("last_login_time", user.getLastLoginTime());
        return info;
    }

    private Map<String, Object> buildSessionInfo(SsoSession session) {
        java.util.LinkedHashMap<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("id", session.getId());
        info.put("session_token", session.getSessionToken());
        info.put("expires_at", session.getExpiresAt());
        info.put("status", session.getStatus());
        return info;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            int commaIndex = ip.indexOf(',');
            if (commaIndex > 0) {
                ip = ip.substring(0, commaIndex).trim();
            }
            return ip;
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}