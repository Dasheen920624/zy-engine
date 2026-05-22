package com.medkernel.security.sso;

import com.medkernel.persistence.Ids;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SsoConfigRepository {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final String ENCRYPTED_MASK = "***";

    private static final String SELECT_COLUMNS = "id, tenant_id, config_code, config_name, protocol_type, status, priority, "
            + "cas_server_url, cas_service_url, cas_callback_url, "
            + "oidc_issuer, oidc_client_id, oidc_client_secret, oidc_redirect_uri, oidc_scope, oidc_response_type, oidc_jwks_uri, "
            + "saml_entity_id, saml_sso_url, saml_slo_url, saml_certificate, saml_metadata_url, "
            + "ldap_url, ldap_base_dn, ldap_bind_dn, ldap_bind_password, ldap_user_search_base, ldap_user_search_filter, "
            + "ldap_group_search_base, ldap_group_search_filter, ldap_use_ssl, ldap_use_starttls, "
            + "attribute_mapping, role_mapping, auto_create_user, auto_update_user, session_timeout_minutes, "
            + "created_by, created_time, updated_by, updated_time";

    private final DataSource dataSource;

    public SsoConfigRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<SsoConfig> listSsoConfigs(Long tenantId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM sec_sso_config WHERE tenant_id = ? ORDER BY priority";
        List<SsoConfig> configs = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
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

    public SsoConfig getSsoConfig(Long configId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM sec_sso_config WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
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

    public void saveSsoConfig(SsoConfig config) {
        String oidcClientSecret = encryptIfNeeded(config.getOidcClientSecret());
        String ldapBindPassword = encryptIfNeeded(config.getLdapBindPassword());

        if (oidcClientSecret == null || ldapBindPassword == null) {
            SsoConfig existing = getSsoConfigByCode(config.getTenantId(), config.getConfigCode());
            if (existing != null) {
                if (oidcClientSecret == null) {
                    oidcClientSecret = existing.getOidcClientSecret();
                }
                if (ldapBindPassword == null) {
                    ldapBindPassword = existing.getLdapBindPassword();
                }
            }
        }

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
                + "created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection()) {
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
                ps.setString(10, oidcClientSecret);
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
                ps.setString(23, ldapBindPassword);
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
                ps.setString(13, oidcClientSecret);
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
                ps.setString(26, ldapBindPassword);
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

    public void deleteSsoConfig(Long configId) {
        String sql = "DELETE FROM sec_sso_config WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, configId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("delete sso config failed: " + ex.getMessage(), ex);
        }
    }

    public SsoConfig findActiveSsoConfig(Long tenantId, String protocolType) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM sec_sso_config WHERE tenant_id = ? AND protocol_type = ? AND status = 'ACTIVE' ORDER BY priority";
        try (Connection connection = dataSource.getConnection();
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

    SsoConfig getSsoConfigByCode(Long tenantId, String configCode) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM sec_sso_config WHERE tenant_id = ? AND config_code = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, configCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSsoConfigRaw(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find sso config by code failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    SsoConfig mapSsoConfig(ResultSet rs) throws SQLException {
        return mapSsoConfigInternal(rs, true);
    }

    SsoConfig mapSsoConfigRaw(ResultSet rs) throws SQLException {
        return mapSsoConfigInternal(rs, false);
    }

    private SsoConfig mapSsoConfigInternal(ResultSet rs, boolean maskSensitive) throws SQLException {
        SsoConfig config = new SsoConfig();
        config.setId(rs.getLong("id"));
        config.setTenantId(rs.getLong("tenant_id"));
        config.setConfigCode(rs.getString("config_code"));
        config.setConfigName(rs.getString("config_name"));
        config.setProtocolType(rs.getString("protocol_type"));
        config.setStatus(rs.getString("status"));
        config.setPriority(rs.getInt("priority"));

        config.setCasServerUrl(rs.getString("cas_server_url"));
        config.setCasServiceUrl(rs.getString("cas_service_url"));
        config.setCasCallbackUrl(rs.getString("cas_callback_url"));

        config.setOidcIssuer(rs.getString("oidc_issuer"));
        config.setOidcClientId(rs.getString("oidc_client_id"));
        config.setOidcClientSecret(maskSensitive ? maskIfEncrypted(rs.getString("oidc_client_secret")) : rs.getString("oidc_client_secret"));
        config.setOidcRedirectUri(rs.getString("oidc_redirect_uri"));
        config.setOidcScope(rs.getString("oidc_scope"));
        config.setOidcResponseType(rs.getString("oidc_response_type"));
        config.setOidcJwksUri(rs.getString("oidc_jwks_uri"));

        config.setSamlEntityId(rs.getString("saml_entity_id"));
        config.setSamlSsoUrl(rs.getString("saml_sso_url"));
        config.setSamlSloUrl(rs.getString("saml_slo_url"));
        config.setSamlCertificate(rs.getString("saml_certificate"));
        config.setSamlMetadataUrl(rs.getString("saml_metadata_url"));

        config.setLdapUrl(rs.getString("ldap_url"));
        config.setLdapBaseDn(rs.getString("ldap_base_dn"));
        config.setLdapBindDn(rs.getString("ldap_bind_dn"));
        config.setLdapBindPassword(maskSensitive ? maskIfEncrypted(rs.getString("ldap_bind_password")) : rs.getString("ldap_bind_password"));
        config.setLdapUserSearchBase(rs.getString("ldap_user_search_base"));
        config.setLdapUserSearchFilter(rs.getString("ldap_user_search_filter"));
        config.setLdapGroupSearchBase(rs.getString("ldap_group_search_base"));
        config.setLdapGroupSearchFilter(rs.getString("ldap_group_search_filter"));
        config.setLdapUseSsl(rs.getBoolean("ldap_use_ssl"));
        config.setLdapUseStarttls(rs.getBoolean("ldap_use_starttls"));

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

    static String encryptIfNeeded(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return rawPassword;
        }
        if (ENCRYPTED_MASK.equals(rawPassword)) {
            return null;
        }
        if (rawPassword.startsWith("$2a$")) {
            return rawPassword;
        }
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    static String maskIfEncrypted(String value) {
        if (value != null && value.startsWith("$2a$")) {
            return ENCRYPTED_MASK;
        }
        return value;
    }
}
