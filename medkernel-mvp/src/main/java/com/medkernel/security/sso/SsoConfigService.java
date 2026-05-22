package com.medkernel.security.sso;

import com.medkernel.common.ErrorCode;
import com.medkernel.security.AuthException;
import com.medkernel.security.JwtTokenProvider;
import com.medkernel.security.SecurityUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SSO 配置服务门面：委托配置管理、回调处理、会话和审计日志到各子服务。
 */
@Service
public class SsoConfigService {

    private final SsoConfigRepository ssoConfigRepository;
    private final SsoCallbackHandler ssoCallbackHandler;
    private final SsoSessionService ssoSessionService;
    private final SsoAuditLogService ssoAuditLogService;
    private final JwtTokenProvider jwtTokenProvider;

    public SsoConfigService(SsoConfigRepository ssoConfigRepository,
                            SsoCallbackHandler ssoCallbackHandler,
                            SsoSessionService ssoSessionService,
                            SsoAuditLogService ssoAuditLogService,
                            JwtTokenProvider jwtTokenProvider) {
        this.ssoConfigRepository = ssoConfigRepository;
        this.ssoCallbackHandler = ssoCallbackHandler;
        this.ssoSessionService = ssoSessionService;
        this.ssoAuditLogService = ssoAuditLogService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public List<SsoConfig> listSsoConfigs(Long tenantId) {
        return ssoConfigRepository.listSsoConfigs(tenantId);
    }

    public SsoConfig getSsoConfig(Long configId) {
        return ssoConfigRepository.getSsoConfig(configId);
    }

    public void saveSsoConfig(SsoConfig config) {
        ssoConfigRepository.saveSsoConfig(config);
    }

    public void deleteSsoConfig(Long configId) {
        ssoConfigRepository.deleteSsoConfig(configId);
    }

    public SsoLoginResult handleSsoCallback(Long tenantId, String protocolType, String callbackData, String ipAddress, String userAgent) {
        SsoConfig config = ssoConfigRepository.findActiveSsoConfig(tenantId, protocolType);
        if (config == null) {
            throw new AuthException(ErrorCode.CONFIG_NOT_FOUND, "未找到有效的 SSO 配置");
        }

        SsoUserInfo externalUser;
        switch (protocolType.toUpperCase()) {
            case "CAS":
                externalUser = ssoCallbackHandler.handleCasCallback(config, callbackData);
                break;
            case "OIDC":
                externalUser = ssoCallbackHandler.handleOidcCallback(config, callbackData);
                break;
            case "SAML":
                externalUser = ssoCallbackHandler.handleSamlCallback(config, callbackData);
                break;
            case "LDAP-AD":
                externalUser = ssoCallbackHandler.handleLdapCallback(config, callbackData);
                break;
            default:
                throw new AuthException(ErrorCode.VALIDATION_ERROR, "不支持的 SSO 协议");
        }

        SecurityUser platformUser = ssoCallbackHandler.findOrCreatePlatformUser(tenantId, config, externalUser);

        SsoSession session = ssoSessionService.createSsoSession(tenantId, platformUser.getId(), config.getId(), externalUser, ipAddress, userAgent);

        String jwtToken = jwtTokenProvider.createToken(
                platformUser.getId(), platformUser.getTenantId(), platformUser.getUsername(), platformUser.getDisplayName());

        ssoAuditLogService.writeAuditLog(tenantId, platformUser.getId(), config.getId(), "LOGIN", "SUCCESS",
                externalUser.getSubject(), null, null, ipAddress, userAgent);

        return new SsoLoginResult(jwtToken, platformUser, session);
    }

    public void handleSsoLogout(Long tenantId, Long userId, String sessionToken, String ipAddress, String userAgent) {
        SsoSession session = ssoSessionService.findSessionByToken(tenantId, sessionToken);
        if (session != null) {
            ssoSessionService.invalidateSession(session.getId());
        }
        ssoAuditLogService.writeAuditLog(tenantId, userId, null, "LOGOUT", "SUCCESS", null, null, null, ipAddress, userAgent);
    }

    public List<SsoSession> listUserSessions(Long tenantId, Long userId) {
        return ssoSessionService.listUserSessions(tenantId, userId);
    }

    public List<SsoAuditLog> listAuditLogs(Long tenantId, int limit) {
        return ssoAuditLogService.listAuditLogs(tenantId, limit);
    }
}
