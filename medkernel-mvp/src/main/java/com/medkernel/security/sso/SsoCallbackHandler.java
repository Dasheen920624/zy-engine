package com.medkernel.security.sso;

import com.medkernel.common.ErrorCode;
import com.medkernel.security.AuthException;
import com.medkernel.security.SecurityPersistenceService;
import com.medkernel.security.SecurityUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SsoCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(SsoCallbackHandler.class);

    private final SecurityPersistenceService securityPersistenceService;

    public SsoCallbackHandler(SecurityPersistenceService securityPersistenceService) {
        this.securityPersistenceService = securityPersistenceService;
    }

    public SsoUserInfo handleCasCallback(SsoConfig config, String callbackData) {
        log.info("CAS callback handling not implemented yet");
        throw new AuthException(ErrorCode.VALIDATION_ERROR, "CAS 协议处理尚未实现");
    }

    public SsoUserInfo handleOidcCallback(SsoConfig config, String callbackData) {
        log.info("OIDC callback handling not implemented yet");
        throw new AuthException(ErrorCode.VALIDATION_ERROR, "OIDC 协议处理尚未实现");
    }

    public SsoUserInfo handleSamlCallback(SsoConfig config, String callbackData) {
        log.info("SAML callback handling not implemented yet");
        throw new AuthException(ErrorCode.VALIDATION_ERROR, "SAML 协议处理尚未实现");
    }

    public SsoUserInfo handleLdapCallback(SsoConfig config, String callbackData) {
        log.info("LDAP-AD callback handling not implemented yet");
        throw new AuthException(ErrorCode.VALIDATION_ERROR, "LDAP-AD 协议处理尚未实现");
    }

    public SecurityUser findOrCreatePlatformUser(Long tenantId, SsoConfig config, SsoUserInfo externalUser) {
        SecurityUser user = securityPersistenceService.findByTenantAndUsername(tenantId, externalUser.getSubject());
        if (user == null && config.isAutoCreateUser()) {
            Long userId = securityPersistenceService.createUser(tenantId, externalUser.getSubject(),
                    externalUser.getName(), externalUser.getEmail(), null, "ACTIVE", "sso");
            user = securityPersistenceService.findById(userId);
        }
        if (user == null) {
            throw new AuthException(ErrorCode.RESOURCE_NOT_FOUND, "未找到对应的平台用户");
        }
        return user;
    }
}
