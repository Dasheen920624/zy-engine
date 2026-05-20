package com.medkernel.security.sso;

import com.medkernel.common.ErrorCode;
import com.medkernel.common.Ids;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.security.AuthException;
import com.medkernel.security.JwtTokenProvider;
import com.medkernel.security.SecurityPersistenceService;
import com.medkernel.security.SecurityUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SSO 服务：处理 CAS/OIDC/SAML/LDAP-AD 单点登录。
 */
@Service
public class SsoService {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);

    private final EnginePersistenceProperties properties;
    private final SecurityPersistenceService securityPersistenceService;
    private final JwtTokenProvider jwtTokenProvider;

    public SsoService(EnginePersistenceProperties properties,
                      SecurityPersistenceService securityPersistenceService,
                      JwtTokenProvider jwtTokenProvider) {
        this.properties = properties;
        this.securityPersistenceService = securityPersistenceService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 获取租户的所有 SSO 配置。
     */
    public List<SsoConfig> listSsoConfigs(Long tenantId) {
        String sql = "SELECT id, tenant_id, config_code, config_name, protocol_type, status, priority, "
                + "cas_server_url, cas_service_url, cas_callback_url, "
                + "oidc_issuer, oidc_client_id, oidc_client_secret, oidc_redirect_uri, oidc_scope, oidc_response_type, oidc_jwks_uri, "
                + "saml_entity_id, saml_sso_url, saml_slo_url, saml_certificate, saml_metadata_url, "
                + "ldap_url, ldap_base_dn, ldap_bind_dn, ldap_bind_password, ldap_user_search_base, ldap_user_search_filter, "
                + "ldap_group_search_base, ldap_group_search_filter, ldap_use_ssl, ldap_use_starttls, "
                + "attribute_mapping, role_mapping, auto_create_user, auto_update_user, session_timeout_minutes, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_sso_config WHERE tenant_id = ? ORDER BY priority";
        List<SsoConfig> configs = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    configs.add(mapSsoConfig(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list sso configs failed: " + ex.getMessage(), ex);
        }
        return configs;
    }

    /**
     * 根据 ID 获取 SSO 配置。
     */
    public SsoConfig getSsoConfig(Long configId) {
        String sql = "SELECT id, tenant_id, config_code, config_name, protocol_type, status, priority, "
                + "cas_server_url, cas_service_url, cas_callback_url, "
                + "oidc_issuer, oidc_client_id, oidc_client_secret, oidc_redirect_uri, oidc_scope, oidc_response_type, oidc_jwks_uri, "
                + "saml_entity_id, saml_sso_url, saml_slo_url, saml_certificate, saml_metadata_url, "
                + "ldap_url, ldap_base_dn, ldap_bind_dn, ldap_bind_password, ldap_user_search_base, ldap_user_search_filter, "
                + "ldap_group_search_base, ldap_group_search_filter, ldap_use_ssl, ldap_use_starttls, "
                + "attribute_mapping, role_mapping, auto_create_user, auto_update_user, session_timeout_minutes, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_sso_config WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, configId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSsoConfig(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get sso config failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 保存 SSO 配置。
     */
    public void saveSsoConfig(SsoConfig config) {
        String updateSql = "UPDATE sec_sso_config SET config_name = ?, protocol_type = ?, status = ?, priority = ?, "
                + "cas_server_url = ?, cas_service_url = ?, cas_callback_url = ?, "
                + "oidc_issuer = ?, oidc_client_id = ?, oidc_client_secret = ?, oidc_redirect_uri = ?, oidc_scope = ?, oidc_response_type = ?, oidc_jwks_uri = ?, "
                + "saml_entity_id = ?, saml_sso_url = ?, saml_slo_url = ?, saml_certificate = ?, saml_metadata_url = ?, "
                + "ldap_url = ?, ldap_base_dn = ?, ldap_bind_dn = ?, ldap_bind_password = ?, ldap_user_search_base = ?, ldap_user_search_filter = ?, "
                + "ldap_group_search_base = ?, ldap_group_search_filter = ?, ldap_use_ssl = ?, ldap_use_starttls = ?, "
                + "attribute_mapping = ?, role_mapping = ?, auto_create_user = ?, auto_update_user = ?, session_timeout_minutes = ?, "
                + "updated_by = ?, updated_time = ? WHERE tenant_id = ? AND config_code = ?";
        String insertSql = "INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority, "
                + "cas_server_url, cas_service_url, cas_callback_url, "
                + "oidc_issuer, oidc_client_id, oidc_client_secret, oidc_redirect_uri, oidc_scope, oidc_response_type, oidc_jwks_uri, "
                + "saml_entity_id, saml_sso_url, saml_slo_url, saml_certificate, saml_metadata_url, "
                + "ldap_url, ldap_base_dn, ldap_bind_dn, ldap_bind_password, ldap_user_search_base, ldap_user_search_filter, "
                + "ldap_group_search_base, ldap_group_search_filter, ldap_use_ssl, ldap_use_starttls, "
                + "attribute_mapping, role_mapping, auto_create_user, auto_update_user, session_timeout_minutes, "
                + "created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = connection()) {
            // Try UPDATE first
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setString(1, config.getConfigName());
                ps.setString(2, config.getProtocolType());
                ps.setString(3, config.getStatus());
                ps.setInt(4, config.getPriority());
                ps.setString(5, config.getCasServerUrl());
                ps.setString(6, config.getCasServiceUrl());
                ps.setString(7, config.getCasCallbackUrl());
                ps.setString(8, config.getOidcIssuer());
                ps.setString(9, config.getOidcClientId());
                ps.setString(10, config.getOidcClientSecret());
                ps.setString(11, config.getOidcRedirectUri());
                ps.setString(12, config.getOidcScope());
                ps.setString(13, config.getOidcResponseType());
                ps.setString(14, config.getOidcJwksUri());
                ps.setString(15, config.getSamlEntityId());
                ps.setString(16, config.getSamlSsoUrl());
                ps.setString(17, config.getSamlSloUrl());
                ps.setString(18, config.getSamlCertificate());
                ps.setString(19, config.getSamlMetadataUrl());
                ps.setString(20, config.getLdapUrl());
                ps.setString(21, config.getLdapBaseDn());
                ps.setString(22, config.getLdapBindDn());
                ps.setString(23, config.getLdapBindPassword());
                ps.setString(24, config.getLdapUserSearchBase());
                ps.setString(25, config.getLdapUserSearchFilter());
                ps.setString(26, config.getLdapGroupSearchBase());
                ps.setString(27, config.getLdapGroupSearchFilter());
                ps.setBoolean(28, config.isLdapUseSsl());
                ps.setBoolean(29, config.isLdapUseStarttls());
                ps.setString(30, config.getAttributeMapping());
                ps.setString(31, config.getRoleMapping());
                ps.setBoolean(32, config.isAutoCreateUser());
                ps.setBoolean(33, config.isAutoUpdateUser());
                ps.setInt(34, config.getSessionTimeoutMinutes());
                ps.setString(35, config.getUpdatedBy());
                ps.setTimestamp(36, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(37, config.getTenantId());
                ps.setString(38, config.getConfigCode());
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    return;
                }
            }

            // If no rows updated, INSERT
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setLong(1, config.getId() != null ? config.getId() : Ids.next());
                ps.setLong(2, config.getTenantId());
                ps.setString(3, config.getConfigCode());
                ps.setString(4, config.getConfigName());
                ps.setString(5, config.getProtocolType());
                ps.setString(6, config.getStatus());
                ps.setInt(7, config.getPriority());
                ps.setString(8, config.getCasServerUrl());
                ps.setString(9, config.getCasServiceUrl());
                ps.setString(10, config.getCasCallbackUrl());
                ps.setString(11, config.getOidcIssuer());
                ps.setString(12, config.getOidcClientId());
                ps.setString(13, config.getOidcClientSecret());
                ps.setString(14, config.getOidcRedirectUri());
                ps.setString(15, config.getOidcScope());
                ps.setString(16, config.getOidcResponseType());
                ps.setString(17, config.getOidcJwksUri());
                ps.setString(18, config.getSamlEntityId());
                ps.setString(19, config.getSamlSsoUrl());
                ps.setString(20, config.getSamlSloUrl());
                ps.setString(21, config.getSamlCertificate());
                ps.setString(22, config.getSamlMetadataUrl());
                ps.setString(23, config.getLdapUrl());
                ps.setString(24, config.getLdapBaseDn());
                ps.setString(25, config.getLdapBindDn());
                ps.setString(26, config.getLdapBindPassword());
                ps.setString(27, config.getLdapUserSearchBase());
                ps.setString(28, config.getLdapUserSearchFilter());
                ps.setString(29, config.getLdapGroupSearchBase());
                ps.setString(30, config.getLdapGroupSearchFilter());
                ps.setBoolean(31, config.isLdapUseSsl());
                ps.setBoolean(32, config.isLdapUseStarttls());
                ps.setString(33, config.getAttributeMapping());
                ps.setString(34, config.getRoleMapping());
                ps.setBoolean(35, config.isAutoCreateUser());
                ps.setBoolean(36, config.isAutoUpdateUser());
                ps.setInt(37, config.getSessionTimeoutMinutes());
                ps.setString(38, config.getCreatedBy());
                ps.setTimestamp(39, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save sso config failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 删除 SSO 配置。
     */
    public void deleteSsoConfig(Long configId) {
        String sql = "DELETE FROM sec_sso_config WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, configId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("delete sso config failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 处理 SSO 登录回调。
     * 根据协议类型调用相应的处理逻辑。
     */
    public SsoLoginResult handleSsoCallback(Long tenantId, String protocolType, String callbackData, String ipAddress, String userAgent) {
        // 1. 查找对应的 SSO 配置
        SsoConfig config = findActiveSsoConfig(tenantId, protocolType);
        if (config == null) {
            throw new AuthException(ErrorCode.SSO_CONFIG_NOT_FOUND, "未找到有效的 SSO 配置");
        }

        // 2. 根据协议类型处理回调
        SsoUserInfo externalUser;
        switch (protocolType.toUpperCase()) {
            case "CAS":
                externalUser = handleCasCallback(config, callbackData);
                break;
            case "OIDC":
                externalUser = handleOidcCallback(config, callbackData);
                break;
            case "SAML":
                externalUser = handleSamlCallback(config, callbackData);
                break;
            case "LDAP-AD":
                externalUser = handleLdapCallback(config, callbackData);
                break;
            default:
                throw new AuthException(ErrorCode.SSO_PROTOCOL_NOT_SUPPORTED, "不支持的 SSO 协议");
        }

        // 3. 查找或创建平台用户
        SecurityUser platformUser = findOrCreatePlatformUser(tenantId, config, externalUser);

        // 4. 创建 SSO 会话
        SsoSession session = createSsoSession(tenantId, platformUser.getId(), config.getId(), externalUser, ipAddress, userAgent);

        // 5. 生成 JWT 令牌
        String jwtToken = jwtTokenProvider.createToken(
                platformUser.getId(), platformUser.getTenantId(), platformUser.getUsername(), platformUser.getDisplayName());

        // 6. 写入审计日志
        writeAuditLog(tenantId, platformUser.getId(), config.getId(), "LOGIN", "SUCCESS",
                externalUser.getSubject(), null, null, ipAddress, userAgent);

        return new SsoLoginResult(jwtToken, platformUser, session);
    }

    /**
     * 处理 SSO 登出。
     */
    public void handleSsoLogout(Long tenantId, Long userId, String sessionToken, String ipAddress, String userAgent) {
        // 1. 查找并销毁 SSO 会话
        SsoSession session = findSessionByToken(tenantId, sessionToken);
        if (session != null) {
            invalidateSession(session.getId());
        }

        // 2. 写入审计日志
        writeAuditLog(tenantId, userId, null, "LOGOUT", "SUCCESS", null, null, null, ipAddress, userAgent);
    }

    /**
     * 获取用户的 SSO 会话列表。
     */
    public List<SsoSession> listUserSessions(Long tenantId, Long userId) {
        String sql = "SELECT id, tenant_id, user_id, config_id, external_subject, external_name, external_email, "
                + "session_token, access_token, refresh_token, id_token, token_type, issued_at, expires_at, "
                + "refresh_expires_at, status, ip_address, user_agent, last_access_time, created_time, updated_time "
                + "FROM sec_sso_session WHERE tenant_id = ? AND user_id = ? AND status = 'ACTIVE' ORDER BY created_time DESC";
        List<SsoSession> sessions = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapSsoSession(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list user sessions failed: " + ex.getMessage(), ex);
        }
        return sessions;
    }

    /**
     * 获取 SSO 审计日志。
     */
    public List<SsoAuditLog> listAuditLogs(Long tenantId, int limit) {
        String sql = "SELECT id, tenant_id, user_id, config_id, event_type, event_result, external_subject, "
                + "error_code, error_message, ip_address, user_agent, trace_id, created_time "
                + "FROM sec_sso_audit_log WHERE tenant_id = ? ORDER BY created_time DESC LIMIT ?";
        List<SsoAuditLog> logs = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapSsoAuditLog(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list audit logs failed: " + ex.getMessage(), ex);
        }
        return logs;
    }

    // 私有方法

    private SsoConfig findActiveSsoConfig(Long tenantId, String protocolType) {
        String sql = "SELECT id, tenant_id, config_code, config_name, protocol_type, status, priority, "
                + "cas_server_url, cas_service_url, cas_callback_url, "
                + "oidc_issuer, oidc_client_id, oidc_client_secret, oidc_redirect_uri, oidc_scope, oidc_response_type, oidc_jwks_uri, "
                + "saml_entity_id, saml_sso_url, saml_slo_url, saml_certificate, saml_metadata_url, "
                + "ldap_url, ldap_base_dn, ldap_bind_dn, ldap_bind_password, ldap_user_search_base, ldap_user_search_filter, "
                + "ldap_group_search_base, ldap_group_search_filter, ldap_use_ssl, ldap_use_starttls, "
                + "attribute_mapping, role_mapping, auto_create_user, auto_update_user, session_timeout_minutes, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_sso_config WHERE tenant_id = ? AND protocol_type = ? AND status = 'ACTIVE' ORDER BY priority LIMIT 1";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, protocolType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSsoConfig(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find active sso config failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    private SsoUserInfo handleCasCallback(SsoConfig config, String callbackData) {
        // TODO: 实现 CAS 协议处理逻辑
        // 1. 验证 CAS 票据
        // 2. 获取用户信息
        // 3. 返回 SsoUserInfo
        log.info("CAS callback handling not implemented yet");
        throw new AuthException(ErrorCode.SSO_PROTOCOL_NOT_SUPPORTED, "CAS 协议处理尚未实现");
    }

    private SsoUserInfo handleOidcCallback(SsoConfig config, String callbackData) {
        // TODO: 实现 OIDC 协议处理逻辑
        // 1. 用授权码换取令牌
        // 2. 验证 ID 令牌
        // 3. 获取用户信息
        // 4. 返回 SsoUserInfo
        log.info("OIDC callback handling not implemented yet");
        throw new AuthException(ErrorCode.SSO_PROTOCOL_NOT_SUPPORTED, "OIDC 协议处理尚未实现");
    }

    private SsoUserInfo handleSamlCallback(SsoConfig config, String callbackData) {
        // TODO: 实现 SAML 协议处理逻辑
        // 1. 验证 SAML 响应
        // 2. 解析断言
        // 3. 获取用户信息
        // 4. 返回 SsoUserInfo
        log.info("SAML callback handling not implemented yet");
        throw new AuthException(ErrorCode.SSO_PROTOCOL_NOT_SUPPORTED, "SAML 协议处理尚未实现");
    }

    private SsoUserInfo handleLdapCallback(SsoConfig config, String callbackData) {
        // TODO: 实现 LDAP-AD 协议处理逻辑
        // 1. 连接 LDAP 服务器
        // 2. 绑定用户
        // 3. 搜索用户信息
        // 4. 返回 SsoUserInfo
        log.info("LDAP-AD callback handling not implemented yet");
        throw new AuthException(ErrorCode.SSO_PROTOCOL_NOT_SUPPORTED, "LDAP-AD 协议处理尚未实现");
    }

    private SecurityUser findOrCreatePlatformUser(Long tenantId, SsoConfig config, SsoUserInfo externalUser) {
        // 1. 根据外部用户标识查找身份绑定
        // 2. 如果找到，返回绑定的平台用户
        // 3. 如果未找到且允许自动创建，创建新用户
        // 4. 如果未找到且不允许自动创建，抛出异常

        // 简化实现：查找或创建用户
        SecurityUser user = securityPersistenceService.findByTenantAndUsername(tenantId, externalUser.getSubject());
        if (user == null && config.isAutoCreateUser()) {
            Long userId = securityPersistenceService.createUser(tenantId, externalUser.getSubject(),
                    externalUser.getName(), externalUser.getEmail(), null, "ACTIVE", "sso");
            user = securityPersistenceService.findById(userId);
        }
        if (user == null) {
            throw new AuthException(ErrorCode.SSO_USER_NOT_FOUND, "未找到对应的平台用户");
        }
        return user;
    }

    private SsoSession createSsoSession(Long tenantId, Long userId, Long configId, SsoUserInfo externalUser,
                                         String ipAddress, String userAgent) {
        SsoSession session = new SsoSession();
        session.setId(Ids.next());
        session.setTenantId(tenantId);
        session.setUserId(userId);
        session.setConfigId(configId);
        session.setExternalSubject(externalUser.getSubject());
        session.setExternalName(externalUser.getName());
        session.setExternalEmail(externalUser.getEmail());
        session.setSessionToken(UUID.randomUUID().toString());
        session.setAccessToken(externalUser.getAccessToken());
        session.setRefreshToken(externalUser.getRefreshToken());
        session.setIdToken(externalUser.getIdToken());
        session.setTokenType(externalUser.getTokenType());
        session.setIssuedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(480)); // 默认 8 小时
        session.setStatus("ACTIVE");
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setLastAccessTime(LocalDateTime.now());
        session.setCreatedTime(LocalDateTime.now());

        String sql = "INSERT INTO sec_sso_session (id, tenant_id, user_id, config_id, external_subject, external_name, external_email, "
                + "session_token, access_token, refresh_token, id_token, token_type, issued_at, expires_at, "
                + "status, ip_address, user_agent, last_access_time, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, session.getId());
            ps.setLong(2, session.getTenantId());
            ps.setLong(3, session.getUserId());
            ps.setLong(4, session.getConfigId());
            ps.setString(5, session.getExternalSubject());
            ps.setString(6, session.getExternalName());
            ps.setString(7, session.getExternalEmail());
            ps.setString(8, session.getSessionToken());
            ps.setString(9, session.getAccessToken());
            ps.setString(10, session.getRefreshToken());
            ps.setString(11, session.getIdToken());
            ps.setString(12, session.getTokenType());
            ps.setTimestamp(13, Timestamp.valueOf(session.getIssuedAt()));
            ps.setTimestamp(14, Timestamp.valueOf(session.getExpiresAt()));
            ps.setString(15, session.getStatus());
            ps.setString(16, session.getIpAddress());
            ps.setString(17, session.getUserAgent());
            ps.setTimestamp(18, Timestamp.valueOf(session.getLastAccessTime()));
            ps.setTimestamp(19, Timestamp.valueOf(session.getCreatedTime()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create sso session failed: " + ex.getMessage(), ex);
        }
        return session;
    }

    private SsoSession findSessionByToken(Long tenantId, String sessionToken) {
        String sql = "SELECT id, tenant_id, user_id, config_id, external_subject, external_name, external_email, "
                + "session_token, access_token, refresh_token, id_token, token_type, issued_at, expires_at, "
                + "refresh_expires_at, status, ip_address, user_agent, last_access_time, created_time, updated_time "
                + "FROM sec_sso_session WHERE tenant_id = ? AND session_token = ? AND status = 'ACTIVE'";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, sessionToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSsoSession(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find session by token failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    private void invalidateSession(Long sessionId) {
        String sql = "UPDATE sec_sso_session SET status = 'REVOKED', updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("invalidate session failed for session {}", sessionId, ex);
        }
    }

    private void writeAuditLog(Long tenantId, Long userId, Long configId, String eventType, String eventResult,
                                String externalSubject, String errorCode, String errorMessage,
                                String ipAddress, String userAgent) {
        String sql = "INSERT INTO sec_sso_audit_log (id, tenant_id, user_id, config_id, event_type, event_result, "
                + "external_subject, error_code, error_message, ip_address, user_agent, trace_id, created_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, Ids.next());
            ps.setLong(2, tenantId);
            if (userId != null) {
                ps.setLong(3, userId);
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
            }
            if (configId != null) {
                ps.setLong(4, configId);
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setString(5, eventType);
            ps.setString(6, eventResult);
            ps.setString(7, externalSubject);
            ps.setString(8, errorCode);
            ps.setString(9, errorMessage);
            ps.setString(10, ipAddress);
            ps.setString(11, userAgent);
            ps.setString(12, null); // traceId
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("write audit log failed", ex);
        }
    }

    private SsoConfig mapSsoConfig(ResultSet rs) throws SQLException {
        SsoConfig config = new SsoConfig();
        config.setId(rs.getLong("id"));
        config.setTenantId(rs.getLong("tenant_id"));
        config.setConfigCode(rs.getString("config_code"));
        config.setConfigName(rs.getString("config_name"));
        config.setProtocolType(rs.getString("protocol_type"));
        config.setStatus(rs.getString("status"));
        config.setPriority(rs.getInt("priority"));

        // CAS
        config.setCasServerUrl(rs.getString("cas_server_url"));
        config.setCasServiceUrl(rs.getString("cas_service_url"));
        config.setCasCallbackUrl(rs.getString("cas_callback_url"));

        // OIDC
        config.setOidcIssuer(rs.getString("oidc_issuer"));
        config.setOidcClientId(rs.getString("oidc_client_id"));
        config.setOidcClientSecret(rs.getString("oidc_client_secret"));
        config.setOidcRedirectUri(rs.getString("oidc_redirect_uri"));
        config.setOidcScope(rs.getString("oidc_scope"));
        config.setOidcResponseType(rs.getString("oidc_response_type"));
        config.setOidcJwksUri(rs.getString("oidc_jwks_uri"));

        // SAML
        config.setSamlEntityId(rs.getString("saml_entity_id"));
        config.setSamlSsoUrl(rs.getString("saml_sso_url"));
        config.setSamlSloUrl(rs.getString("saml_slo_url"));
        config.setSamlCertificate(rs.getString("saml_certificate"));
        config.setSamlMetadataUrl(rs.getString("saml_metadata_url"));

        // LDAP
        config.setLdapUrl(rs.getString("ldap_url"));
        config.setLdapBaseDn(rs.getString("ldap_base_dn"));
        config.setLdapBindDn(rs.getString("ldap_bind_dn"));
        config.setLdapBindPassword(rs.getString("ldap_bind_password"));
        config.setLdapUserSearchBase(rs.getString("ldap_user_search_base"));
        config.setLdapUserSearchFilter(rs.getString("ldap_user_search_filter"));
        config.setLdapGroupSearchBase(rs.getString("ldap_group_search_base"));
        config.setLdapGroupSearchFilter(rs.getString("ldap_group_search_filter"));
        config.setLdapUseSsl(rs.getBoolean("ldap_use_ssl"));
        config.setLdapUseStarttls(rs.getBoolean("ldap_use_starttls"));

        // 通用
        config.setAttributeMapping(rs.getString("attribute_mapping"));
        config.setRoleMapping(rs.getString("role_mapping"));
        config.setAutoCreateUser(rs.getBoolean("auto_create_user"));
        config.setAutoUpdateUser(rs.getBoolean("auto_update_user"));
        config.setSessionTimeoutMinutes(rs.getInt("session_timeout_minutes"));

        config.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            config.setCreatedTime(createdTime.toLocalDateTime());
        }
        config.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            config.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return config;
    }

    private SsoSession mapSsoSession(ResultSet rs) throws SQLException {
        SsoSession session = new SsoSession();
        session.setId(rs.getLong("id"));
        session.setTenantId(rs.getLong("tenant_id"));
        session.setUserId(rs.getLong("user_id"));
        session.setConfigId(rs.getLong("config_id"));
        session.setExternalSubject(rs.getString("external_subject"));
        session.setExternalName(rs.getString("external_name"));
        session.setExternalEmail(rs.getString("external_email"));
        session.setSessionToken(rs.getString("session_token"));
        session.setAccessToken(rs.getString("access_token"));
        session.setRefreshToken(rs.getString("refresh_token"));
        session.setIdToken(rs.getString("id_token"));
        session.setTokenType(rs.getString("token_type"));

        Timestamp issuedAt = rs.getTimestamp("issued_at");
        if (issuedAt != null) {
            session.setIssuedAt(issuedAt.toLocalDateTime());
        }
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            session.setExpiresAt(expiresAt.toLocalDateTime());
        }
        Timestamp refreshExpiresAt = rs.getTimestamp("refresh_expires_at");
        if (refreshExpiresAt != null) {
            session.setRefreshExpiresAt(refreshExpiresAt.toLocalDateTime());
        }

        session.setStatus(rs.getString("status"));
        session.setIpAddress(rs.getString("ip_address"));
        session.setUserAgent(rs.getString("user_agent"));

        Timestamp lastAccessTime = rs.getTimestamp("last_access_time");
        if (lastAccessTime != null) {
            session.setLastAccessTime(lastAccessTime.toLocalDateTime());
        }
        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            session.setCreatedTime(createdTime.toLocalDateTime());
        }
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        if (updatedTime != null) {
            session.setUpdatedTime(updatedTime.toLocalDateTime());
        }
        return session;
    }

    private SsoAuditLog mapSsoAuditLog(ResultSet rs) throws SQLException {
        SsoAuditLog log = new SsoAuditLog();
        log.setId(rs.getLong("id"));
        log.setTenantId(rs.getLong("tenant_id"));
        long userId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            log.setUserId(userId);
        }
        long configId = rs.getLong("config_id");
        if (!rs.wasNull()) {
            log.setConfigId(configId);
        }
        log.setEventType(rs.getString("event_type"));
        log.setEventResult(rs.getString("event_result"));
        log.setExternalSubject(rs.getString("external_subject"));
        log.setErrorCode(rs.getString("error_code"));
        log.setErrorMessage(rs.getString("error_message"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setUserAgent(rs.getString("user_agent"));
        log.setTraceId(rs.getString("trace_id"));

        Timestamp createdTime = rs.getTimestamp("created_time");
        if (createdTime != null) {
            log.setCreatedTime(createdTime.toLocalDateTime());
        }
        return log;
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                properties.getUrl(), properties.getUsername(), properties.getPassword());
    }

    // 内部类

    public static class SsoUserInfo {
        private String subject;
        private String name;
        private String email;
        private String accessToken;
        private String refreshToken;
        private String idToken;
        private String tokenType;

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getIdToken() { return idToken; }
        public void setIdToken(String idToken) { this.idToken = idToken; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    }

    public static class SsoLoginResult {
        private String token;
        private SecurityUser user;
        private SsoSession session;

        public SsoLoginResult(String token, SecurityUser user, SsoSession session) {
            this.token = token;
            this.user = user;
            this.session = session;
        }

        public String getToken() { return token; }
        public SecurityUser getUser() { return user; }
        public SsoSession getSession() { return session; }
    }
}