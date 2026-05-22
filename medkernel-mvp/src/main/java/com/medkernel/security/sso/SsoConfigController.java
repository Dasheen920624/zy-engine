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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SSO 配置控制器：管理 SSO 配置、回调会话和审计查询。
 */
@Tag(name = "Sso Config")
@RestController
@RequestMapping("/api/sso")
public class SsoConfigController {

    private final SsoConfigService ssoConfigService;
    private final OrganizationContextService organizationContextService;

    public SsoConfigController(SsoConfigService ssoConfigService, OrganizationContextService organizationContextService) {
        this.ssoConfigService = ssoConfigService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 获取 SSO 配置列表。
     */
    @Operation(summary = "List configs")
    @GetMapping("/configs")
    public ApiResult<List<Map<String, Object>>> listConfigs(HttpServletRequest request) {
        Long tenantId = resolveNumericTenantId(request);
        List<SsoConfig> configs = ssoConfigService.listSsoConfigs(tenantId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SsoConfig config : configs) {
            result.add(toConfigView(config));
        }
        return ApiResult.success(result);
    }

    /**
     * 根据 ID 获取 SSO 配置。
     */
    @Operation(summary = "Get config")
    @GetMapping("/configs/detail")
    public ApiResult<Map<String, Object>> getConfig(@RequestParam("id") Long configId) {
        SsoConfig config = ssoConfigService.getSsoConfig(configId);
        if (config == null) {
            return ApiResult.failure(ErrorCode.CONFIG_NOT_FOUND, "SSO 配置不存在");
        }
        return ApiResult.success(toConfigView(config));
    }

    /**
     * 保存 SSO 配置（创建或更新）。
     */
    @Operation(summary = "Save config")
    @PostMapping("/configs")
    public ApiResult<Void> saveConfig(@RequestBody SsoConfig config, HttpServletRequest request) {
        Long tenantId = resolveNumericTenantId(request);
        config.setTenantId(tenantId);
        ssoConfigService.saveSsoConfig(config);
        return ApiResult.success(null);
    }

    /**
     * 删除 SSO 配置。
     */
    @Operation(summary = "Delete config")
    @PostMapping("/configs/delete")
    public ApiResult<Void> deleteConfig(@RequestBody Map<String, Long> body) {
        Long configId = body.get("id");
        if (configId == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "配置 ID 不能为空");
        }
        ssoConfigService.deleteSsoConfig(configId);
        return ApiResult.success(null);
    }

    /**
     * 处理 CAS 回调。
     */
    @Operation(summary = "Handle cas callback")
    @PostMapping("/cas/callback")
    public ApiResult<Map<String, Object>> handleCasCallback(@RequestBody Map<String, String> body,
                                                            HttpServletRequest request) {
        Long tenantId = resolveNumericTenantId(request);
        String ticket = body.get("ticket");
        if (ticket == null || ticket.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "CAS 票据不能为空");
        }

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        SsoConfigService.SsoLoginResult result = ssoConfigService.handleSsoCallback(tenantId, "CAS", ticket, ipAddress, userAgent);
        return ApiResult.success(buildLoginResponse(result));
    }

    /**
     * 处理 OIDC 回调。
     */
    @Operation(summary = "Handle oidc callback")
    @PostMapping("/oidc/callback")
    public ApiResult<Map<String, Object>> handleOidcCallback(@RequestBody Map<String, String> body,
                                                              HttpServletRequest request) {
        Long tenantId = resolveNumericTenantId(request);
        String code = body.get("code");
        if (code == null || code.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "OIDC 授权码不能为空");
        }

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        SsoConfigService.SsoLoginResult result = ssoConfigService.handleSsoCallback(tenantId, "OIDC", code, ipAddress, userAgent);
        return ApiResult.success(buildLoginResponse(result));
    }

    /**
     * 处理 SAML 回调。
     */
    @Operation(summary = "Handle saml callback")
    @PostMapping("/saml/callback")
    public ApiResult<Map<String, Object>> handleSamlCallback(@RequestBody Map<String, String> body,
                                                              HttpServletRequest request) {
        Long tenantId = resolveNumericTenantId(request);
        String samlResponse = body.get("SAMLResponse");
        if (samlResponse == null || samlResponse.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "SAML 响应不能为空");
        }

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        SsoConfigService.SsoLoginResult result = ssoConfigService.handleSsoCallback(tenantId, "SAML", samlResponse, ipAddress, userAgent);
        return ApiResult.success(buildLoginResponse(result));
    }

    /**
     * 处理 LDAP-AD 登录。
     */
    @Operation(summary = "Handle ldap login")
    @PostMapping("/ldap/login")
    public ApiResult<Map<String, Object>> handleLdapLogin(@RequestBody Map<String, String> body,
                                                           HttpServletRequest request) {
        Long tenantId = resolveNumericTenantId(request);
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "用户名和密码不能为空");
        }

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 将用户名和密码作为回调数据传递
        String callbackData = username + ":" + password;
        SsoConfigService.SsoLoginResult result = ssoConfigService.handleSsoCallback(tenantId, "LDAP-AD", callbackData, ipAddress, userAgent);
        return ApiResult.success(buildLoginResponse(result));
    }

    /**
     * SSO 登出。
     */
    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long tenantId = resolveNumericTenantId(request);
        Long userId = com.medkernel.security.SecurityContext.getUserId();
        String sessionToken = body.get("session_token");

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        ssoConfigService.handleSsoLogout(tenantId, userId, sessionToken, ipAddress, userAgent);
        return ApiResult.success(null);
    }

    /**
     * 获取当前用户的 SSO 会话列表。
     */
    @Operation(summary = "List sessions")
    @GetMapping("/sessions")
    public ApiResult<List<Map<String, Object>>> listSessions(HttpServletRequest request) {
        Long tenantId = resolveNumericTenantId(request);
        Long userId = com.medkernel.security.SecurityContext.getUserId();
        List<SsoSession> sessions = ssoConfigService.listUserSessions(tenantId, userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SsoSession session : sessions) {
            result.add(toSessionView(session));
        }
        return ApiResult.success(result);
    }

    /**
     * 获取 SSO 审计日志。
     */
    @Operation(summary = "List audit logs")
    @GetMapping("/audit-logs")
    public ApiResult<List<Map<String, Object>>> listAuditLogs(@RequestParam(value = "limit", defaultValue = "100") int limit,
                                                               HttpServletRequest request) {
        Long tenantId = resolveNumericTenantId(request);
        List<SsoAuditLog> logs = ssoConfigService.listAuditLogs(tenantId, limit);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SsoAuditLog log : logs) {
            result.add(toAuditLogView(log));
        }
        return ApiResult.success(result);
    }

    /**
     * 健康检查。
     */
    @Operation(summary = "Health")
    @GetMapping("/health")
    public ApiResult<Map<String, String>> health() {
        java.util.LinkedHashMap<String, String> data = new java.util.LinkedHashMap<>();
        data.put("status", "ok");
        data.put("module", "sso");
        return ApiResult.success(data);
    }

    private Map<String, Object> buildLoginResponse(SsoConfigService.SsoLoginResult result) {
        java.util.LinkedHashMap<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("token", result.getToken());
        response.put("user", buildUserInfo(result.getUser()));
        response.put("session", buildSessionInfo(result.getSession()));
        return response;
    }

    private Long resolveNumericTenantId(HttpServletRequest request) {
        Long tenantId = organizationContextService.getTenantId(request);
        return tenantId != null ? tenantId : 1L;
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

    private Map<String, Object> toConfigView(SsoConfig config) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", config.getId());
        view.put("tenant_id", config.getTenantId());
        view.put("config_code", config.getConfigCode());
        view.put("config_name", config.getConfigName());
        view.put("protocol_type", config.getProtocolType());
        view.put("status", config.getStatus());
        view.put("priority", config.getPriority());
        view.put("cas_server_url", config.getCasServerUrl());
        view.put("cas_service_url", config.getCasServiceUrl());
        view.put("cas_callback_url", config.getCasCallbackUrl());
        view.put("oidc_issuer", config.getOidcIssuer());
        view.put("oidc_client_id", config.getOidcClientId());
        view.put("oidc_redirect_uri", config.getOidcRedirectUri());
        view.put("oidc_scope", config.getOidcScope());
        view.put("oidc_response_type", config.getOidcResponseType());
        view.put("saml_entity_id", config.getSamlEntityId());
        view.put("saml_sso_url", config.getSamlSsoUrl());
        view.put("ldap_url", config.getLdapUrl());
        view.put("auto_create_user", config.isAutoCreateUser());
        view.put("auto_update_user", config.isAutoUpdateUser());
        view.put("session_timeout_minutes", config.getSessionTimeoutMinutes());
        view.put("created_by", config.getCreatedBy());
        view.put("created_time", config.getCreatedTime());
        view.put("updated_by", config.getUpdatedBy());
        view.put("updated_time", config.getUpdatedTime());
        return view;
    }

    private Map<String, Object> toSessionView(SsoSession session) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", session.getId());
        view.put("tenant_id", session.getTenantId());
        view.put("user_id", session.getUserId());
        view.put("config_id", session.getConfigId());
        view.put("external_subject", session.getExternalSubject());
        view.put("external_name", session.getExternalName());
        view.put("external_email", session.getExternalEmail());
        view.put("session_token", session.getSessionToken());
        view.put("expires_at", session.getExpiresAt());
        view.put("status", session.getStatus());
        view.put("created_time", session.getCreatedTime());
        return view;
    }

    private Map<String, Object> toAuditLogView(SsoAuditLog log) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", log.getId());
        view.put("tenant_id", log.getTenantId());
        view.put("user_id", log.getUserId());
        view.put("config_id", log.getConfigId());
        view.put("event_type", log.getEventType());
        view.put("event_result", log.getEventResult());
        view.put("external_subject", log.getExternalSubject());
        view.put("error_code", log.getErrorCode());
        view.put("error_message", log.getErrorMessage());
        view.put("ip_address", log.getIpAddress());
        view.put("trace_id", log.getTraceId());
        view.put("created_time", log.getCreatedTime());
        return view;
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
