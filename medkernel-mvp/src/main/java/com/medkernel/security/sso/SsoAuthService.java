package com.medkernel.security.sso;

import com.medkernel.security.IdentityBinding;
import com.medkernel.security.JwtTokenProvider;
import com.medkernel.security.SecurityPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSO 登录服务。
 * 管理 SSO 适配器注册、SSO 登录流程、外部 subject 绑定平台用户、会话管理。
 */
@Service
public class SsoAuthService {
    private static final Logger log = LoggerFactory.getLogger(SsoAuthService.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityPersistenceService persistenceService;

    private final Map<String, SsoAdapter> adapters = new HashMap<String, SsoAdapter>();
    private final Map<String, SsoSession> sessions = new ConcurrentHashMap<String, SsoSession>();

    public SsoAuthService(JwtTokenProvider jwtTokenProvider,
                          SecurityPersistenceService persistenceService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.persistenceService = persistenceService;
        // 注册内置适配器
        registerAdapter(new CasSsoAdapter());
        registerAdapter(new OidcSsoAdapter());
        registerAdapter(new SamlSsoAdapter());
        registerAdapter(new LdapAdSsoAdapter());
    }

    public void registerAdapter(SsoAdapter adapter) {
        adapters.put(adapter.getProtocolType(), adapter);
    }

    /**
     * 发起 SSO 登录：构建外部 SSO 系统的登录跳转 URL。
     */
    public Map<String, Object> initiateLogin(String providerCode, String tenantId, String callbackBaseUrl) {
        // 查找身份源配置
        Map<String, String> providerConfig = findProviderConfig(providerCode, tenantId);
        if (providerConfig == null) {
            throw new IllegalArgumentException("Identity provider not found: " + providerCode);
        }

        String protocolType = providerConfig.get("provider_type");
        SsoAdapter adapter = adapters.get(protocolType);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported SSO protocol: " + protocolType);
        }

        String state = UUID.randomUUID().toString();
        String redirectUri = callbackBaseUrl + "/api/auth/sso/callback?provider=" + providerCode + "&state=" + state;
        String loginUrl = adapter.buildLoginUrl(providerConfig, redirectUri, state);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("login_url", loginUrl);
        result.put("state", state);
        result.put("provider_code", providerCode);
        result.put("protocol_type", protocolType);
        return result;
    }

    /**
     * SSO 回调处理：验证外部凭据，绑定/创建平台用户，签发 JWT。
     */
    public Map<String, Object> handleCallback(String providerCode, String credential,
                                               String tenantId, String callbackBaseUrl) {
        // 查找身份源配置
        Map<String, String> providerConfig = findProviderConfig(providerCode, tenantId);
        if (providerConfig == null) {
            throw new IllegalArgumentException("Identity provider not found: " + providerCode);
        }

        String protocolType = providerConfig.get("provider_type");
        SsoAdapter adapter = adapters.get(protocolType);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported SSO protocol: " + protocolType);
        }

        // 验证外部凭据
        String redirectUri = callbackBaseUrl + "/api/auth/sso/callback?provider=" + providerCode;
        SsoAdapter.SsoVerifyResult verifyResult = adapter.verify(providerConfig, credential, redirectUri);
        if (!verifyResult.isSuccess()) {
            Map<String, Object> failure = new LinkedHashMap<String, Object>();
            failure.put("success", false);
            failure.put("error", verifyResult.getErrorMessage());
            return failure;
        }

        String externalSubject = verifyResult.getExternalSubject();
        String externalDisplayName = verifyResult.getExternalDisplayName();

        // 查找绑定关系
        Long providerId = toLong(providerConfig.get("provider_id"));
        IdentityBinding binding = persistenceService.findIdentityBinding(toLong(tenantId), providerId, externalSubject);

        Long platformUserId;
        if (binding != null) {
            // 已有绑定，直接使用
            platformUserId = binding.getUserId();
        } else {
            // 无绑定，检查自动开户策略
            boolean autoProvision = "true".equalsIgnoreCase(providerConfig.getOrDefault("auto_provision", "false"));
            if (autoProvision) {
                // 自动开户：创建平台用户 + 绑定
                String username = "sso-" + externalSubject;
                String displayName = externalDisplayName != null ? externalDisplayName : externalSubject;
                platformUserId = persistenceService.createUser(toLong(tenantId), username, displayName,
                        null, null, "SSO", null, "ACTIVE");
                persistenceService.createIdentityBinding(toLong(tenantId), platformUserId, providerId,
                        externalSubject, providerConfig.getOrDefault("org_code", ""), externalDisplayName);
                log.info("[SSO] Auto-provisioned user {} for external subject {} via provider {}",
                        platformUserId, externalSubject, providerCode);
            } else {
                // 无自动开户，拒绝登录
                Map<String, Object> failure = new LinkedHashMap<String, Object>();
                failure.put("success", false);
                failure.put("error", "No binding found and auto-provision is disabled");
                failure.put("external_subject", externalSubject);
                failure.put("provider_code", providerCode);
                return failure;
            }
        }

        // 签发平台 JWT
        String token = jwtTokenProvider.createToken(platformUserId, toLong(tenantId), "sso-user", externalDisplayName);

        // 创建 SSO 会话
        SsoSession session = new SsoSession();
        session.sessionId = UUID.randomUUID().toString();
        session.platformUserId = platformUserId;
        session.tenantId = tenantId;
        session.providerCode = providerCode;
        session.externalSubject = externalSubject;
        session.loginMethod = protocolType;
        session.createdAt = LocalDateTime.now().toString();
        session.expiresAt = LocalDateTime.now().plusHours(8).toString();
        sessions.put(session.sessionId, session);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("token", token);
        result.put("session_id", session.sessionId);
        result.put("platform_user_id", platformUserId);
        result.put("external_subject", externalSubject);
        result.put("login_method", protocolType);
        return result;
    }

    /**
     * LDAP 直接认证（非浏览器跳转）。
     */
    public Map<String, Object> ldapDirectLogin(String providerCode, String username, String password,
                                                 String tenantId) {
        // LDAP 使用 "username:password" 作为 credential
        String credential = username + ":" + password;
        return handleCallback(providerCode, credential, tenantId, "");
    }

    /**
     * 注销 SSO 会话。
     */
    public boolean logoutSession(String sessionId) {
        SsoSession session = sessions.remove(sessionId);
        return session != null;
    }

    /**
     * 列出可用的 SSO 身份源。
     */
    public Map<String, Object> listAvailableProviders(String tenantId) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("protocols", adapters.keySet());
        result.put("tenant_id", tenantId);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> findProviderConfig(String providerCode, String tenantId) {
        // MVP: 返回模拟配置
        // 生产环境应从 sec_identity_provider 表查询
        Map<String, String> config = new HashMap<String, String>();
        config.put("provider_code", providerCode);
        config.put("provider_id", "1");
        config.put("tenant_id", tenantId);
        config.put("auto_provision", "true");

        if ("cas".equalsIgnoreCase(providerCode)) {
            config.put("provider_type", "CAS");
            config.put("login_url", "https://cas.example.com/cas/login");
            config.put("validate_url", "https://cas.example.com/cas/serviceValidate");
        } else if ("oidc".equalsIgnoreCase(providerCode)) {
            config.put("provider_type", "OIDC");
            config.put("authorization_endpoint", "https://keycloak.example.com/realms/master/protocol/openid-connect/auth");
            config.put("token_endpoint", "https://keycloak.example.com/realms/master/protocol/openid-connect/token");
            config.put("client_id", "medkernel");
            config.put("client_secret", "secret");
            config.put("issuer", "https://keycloak.example.com/realms/master");
        } else if ("saml".equalsIgnoreCase(providerCode)) {
            config.put("provider_type", "SAML");
            config.put("idp_sso_url", "https://idp.example.com/saml/sso");
            config.put("sp_entity_id", "medkernel-sp");
            config.put("idp_certificate", "MIID...");
        } else if ("ldap".equalsIgnoreCase(providerCode)) {
            config.put("provider_type", "LDAP-AD");
            config.put("ldap_url", "ldap://ad.example.com:389");
            config.put("base_dn", "DC=example,DC=com");
        } else {
            return null;
        }
        return config;
    }

    private long toLong(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * SSO 会话记录。
     */
    private static class SsoSession {
        String sessionId;
        Long platformUserId;
        String tenantId;
        String providerCode;
        String externalSubject;
        String loginMethod;
        String createdAt;
        String expiresAt;
    }
}
