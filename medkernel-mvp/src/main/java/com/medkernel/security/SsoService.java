package com.medkernel.security;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SSO 认证服务：支持 CAS/OIDC/SAML/LDAP-AD 四种协议的单点登录。
 * SSO 登录后复用 sec_identity_binding 绑定平台用户，签发 JWT。
 */
@Service
public class SsoService {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);

    private final SecurityPersistenceService persistenceService;
    private final JwtTokenProvider jwtTokenProvider;
    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public SsoService(SecurityPersistenceService persistenceService,
                      JwtTokenProvider jwtTokenProvider,
                      EnginePersistenceProperties properties,
                      DataSource dataSource) {
        this.persistenceService = persistenceService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 发起 SSO 登录：根据身份源配置生成重定向 URL。
     *
     * @param tenantId   租户 ID
     * @param providerId 身份源 ID
     * @return 重定向信息（包含 redirectUrl 和 state）
     */
    public Map<String, Object> initiateSso(Long tenantId, Long providerId) {
        IdentityProvider provider = findProvider(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("身份源不存在: " + providerId);
        }
        if (!"ACTIVE".equals(provider.getStatus())) {
            throw new IllegalStateException("身份源已停用: " + provider.getProviderCode());
        }

        String state = generateState();
        String redirectUrl = buildSsoRedirectUrl(provider, state);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("providerId", providerId);
        result.put("providerType", provider.getProviderType());
        result.put("redirectUrl", redirectUrl);
        result.put("state", state);
        return result;
    }

    /**
     * 处理 SSO 回调：验证外部身份，查找或创建绑定，签发 JWT。
     *
     * @param tenantId   租户 ID
     * @param providerId 身份源 ID
     * @param code       授权码（CAS/OIDC）或 SAML Response
     * @param state      防 CSRF state
     * @param request    HTTP 请求
     * @return 登录结果（包含 token 和用户信息）
     */
    public Map<String, Object> handleCallback(Long tenantId, Long providerId,
                                                String code, String state,
                                                HttpServletRequest request) {
        IdentityProvider provider = findProvider(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("身份源不存在: " + providerId);
        }

        String ip = getClientIp(request);

        // 根据协议类型验证外部身份
        Map<String, Object> ssoUser = verifySsoIdentity(provider, code);
        String externalSubject = String.valueOf(ssoUser.get("subject"));
        String externalDisplayName = String.valueOf(ssoUser.getOrDefault("displayName", ""));

        // 查找身份绑定
        SsoIdentityBinding binding = findBinding(tenantId, providerId, externalSubject);

        SecurityUser user;
        if (binding != null) {
            // 已有绑定 → 查找平台用户
            user = persistenceService.findById(binding.getUserId());
            if (user == null) {
                throw new IllegalStateException("绑定用户不存在: " + binding.getUserId());
            }
            if (!"ACTIVE".equals(user.getStatus())) {
                throw new IllegalStateException("用户已禁用: " + user.getUsername());
            }
            // 更新绑定验证时间
            updateBindingVerifiedTime(binding.getId());
        } else {
            // 无绑定 → 尝试按 employeeId 匹配
            user = findUserByEmployeeId(tenantId, externalSubject);
            if (user != null) {
                // 创建绑定
                createBinding(tenantId, user.getId(), providerId, externalSubject, externalDisplayName, "sso");
            } else {
                // 无匹配用户 → 自动创建平台用户
                user = createSsoUser(tenantId, externalSubject, externalDisplayName, "sso");
                createBinding(tenantId, user.getId(), providerId, externalSubject, externalDisplayName, "sso");
            }
        }

        // 更新登录状态
        persistenceService.updateLoginStatus(user.getId(), true, ip);
        persistenceService.writeAuditLog(user.getId(), user.getTenantId(), "SSO_LOGIN", ip,
                "provider=" + provider.getProviderCode());

        // 签发 JWT
        String token = jwtTokenProvider.createToken(
                user.getId(), user.getTenantId(), user.getUsername(), user.getDisplayName());

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("token", token);
        result.put("user", buildUserInfo(user));
        return result;
    }

    /**
     * 查询 SSO 身份源列表（仅 SSO 类型）。
     */
    public List<IdentityProvider> listSsoProviders(Long tenantId) {
        List<IdentityProvider> allProviders = findProvidersByTenant(tenantId);
        List<IdentityProvider> ssoProviders = new ArrayList<IdentityProvider>();
        for (IdentityProvider p : allProviders) {
            String type = p.getProviderType();
            if ("CAS".equalsIgnoreCase(type) || "OIDC".equalsIgnoreCase(type)
                    || "SAML".equalsIgnoreCase(type) || "LDAP".equalsIgnoreCase(type)) {
                ssoProviders.add(p);
            }
        }
        return ssoProviders;
    }

    // ---- 内部方法 ----

    /**
     * 根据 SSO 协议构建重定向 URL。
     */
    private String buildSsoRedirectUrl(IdentityProvider provider, String state) {
        String providerType = provider.getProviderType();
        String adapterCode = provider.getAdapterCode();

        // 简化实现：根据协议类型生成标准 SSO URL
        // 实际生产环境中，这些 URL 应从身份源配置中获取
        switch (providerType.toUpperCase()) {
            case "CAS":
                return adapterCode + "/login?service=" + buildCallbackUrl(provider.getId()) + "&state=" + state;
            case "OIDC":
                return adapterCode + "/authorize?response_type=code&client_id=medkernel"
                        + "&redirect_uri=" + buildCallbackUrl(provider.getId())
                        + "&state=" + state + "&scope=openid+profile+email";
            case "SAML":
                return "/api/security/sso/saml/initiate?providerId=" + provider.getId() + "&state=" + state;
            case "LDAP":
                // LDAP 不走重定向，直接验证
                return "/api/security/sso/ldap/form?providerId=" + provider.getId() + "&state=" + state;
            default:
                throw new IllegalArgumentException("不支持的 SSO 协议: " + providerType);
        }
    }

    private String buildCallbackUrl(Long providerId) {
        return "/api/security/sso/callback?providerId=" + providerId;
    }

    /**
     * 验证 SSO 身份：根据协议类型验证授权码/凭据。
     * 简化实现：Mock 验证，生产环境需对接真实 SSO 服务。
     */
    private Map<String, Object> verifySsoIdentity(IdentityProvider provider, String code) {
        Map<String, Object> ssoUser = new HashMap<String, Object>();
        String providerType = provider.getProviderType();

        switch (providerType.toUpperCase()) {
            case "CAS":
            case "OIDC":
                // 简化：code 即为外部 subject（生产环境需调用 SSO 服务验证 ticket/code）
                ssoUser.put("subject", code);
                ssoUser.put("displayName", code);
                break;
            case "SAML":
                // 简化：SAML Response 解析（生产环境需 XML 解析）
                ssoUser.put("subject", code);
                ssoUser.put("displayName", code);
                break;
            case "LDAP":
                // LDAP 直接验证已在 controller 层处理
                ssoUser.put("subject", code);
                ssoUser.put("displayName", code);
                break;
            default:
                throw new IllegalArgumentException("不支持的 SSO 协议: " + providerType);
        }

        return ssoUser;
    }

    private String generateState() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private IdentityProvider findProvider(Long providerId) {
        String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, "
                + "adapter_code, sync_mode, sync_cron, priority, status, "
                + "last_sync_time, last_sync_result, last_sync_summary, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_provider WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProvider(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询身份源失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    private List<IdentityProvider> findProvidersByTenant(Long tenantId) {
        String sql = "SELECT id, tenant_id, provider_code, provider_name, provider_type, "
                + "adapter_code, sync_mode, sync_cron, priority, status, "
                + "last_sync_time, last_sync_result, last_sync_summary, "
                + "created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_provider WHERE tenant_id = ? ORDER BY priority";
        List<IdentityProvider> providers = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    providers.add(mapProvider(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询身份源列表失败: " + ex.getMessage(), ex);
        }
        return providers;
    }

    private SsoIdentityBinding findBinding(Long tenantId, Long providerId, String externalSubject) {
        String sql = "SELECT id, tenant_id, user_id, provider_id, external_subject, "
                + "external_org_code, external_display_name, binding_status, "
                + "last_verified_time, created_by, created_time, updated_by, updated_time "
                + "FROM sec_identity_binding "
                + "WHERE tenant_id = ? AND provider_id = ? AND external_subject = ? AND binding_status = 'ACTIVE'";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setLong(2, providerId);
            ps.setString(3, externalSubject);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SsoIdentityBinding binding = new SsoIdentityBinding();
                    binding.setId(rs.getLong("id"));
                    binding.setTenantId(rs.getLong("tenant_id"));
                    binding.setUserId(rs.getLong("user_id"));
                    binding.setProviderId(rs.getLong("provider_id"));
                    binding.setExternalSubject(rs.getString("external_subject"));
                    binding.setExternalDisplayName(rs.getString("external_display_name"));
                    binding.setBindingStatus(rs.getString("binding_status"));
                    return binding;
                }
            }
        } catch (SQLException ex) {
            log.error("查询身份绑定失败", ex);
        }
        return null;
    }

    private SecurityUser findUserByEmployeeId(Long tenantId, String employeeId) {
        String sql = "SELECT id, tenant_id, username, password_hash, display_name, "
                + "email, phone, avatar_url, status, user_type, employee_id, "
                + "last_login_time, last_login_ip, login_attempts, locked_until "
                + "FROM sec_user WHERE tenant_id = ? AND employee_id = ? AND status = 'ACTIVE'";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SecurityUser user = new SecurityUser();
                    user.setId(rs.getLong("id"));
                    user.setTenantId(rs.getLong("tenant_id"));
                    user.setUsername(rs.getString("username"));
                    user.setDisplayName(rs.getString("display_name"));
                    user.setStatus(rs.getString("status"));
                    user.setUserType(rs.getString("user_type"));
                    user.setEmployeeId(rs.getString("employee_id"));
                    return user;
                }
            }
        } catch (SQLException ex) {
            log.error("按工号查询用户失败", ex);
        }
        return null;
    }

    private SecurityUser createSsoUser(Long tenantId, String externalSubject,
                                        String displayName, String operator) {
        long userId = Ids.next();
        String username = "sso_" + externalSubject;
        String defaultPasswordHash = "$2a$10$GNycXiBqR1ydr7zFjdVuKec3GoB4Y1x.YUyLK2jvhYsUTBEZlSjkC";
        String sql = "INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, "
                + "status, user_type, employee_id, created_by, created_time) "
                + "VALUES (?, ?, ?, ?, ?, 'ACTIVE', 'HOSPITAL', ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, tenantId);
            ps.setString(3, username);
            ps.setString(4, defaultPasswordHash);
            ps.setString(5, displayName);
            ps.setString(6, externalSubject);
            ps.setString(7, operator);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建 SSO 用户失败: " + ex.getMessage(), ex);
        }

        SecurityUser user = new SecurityUser();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setStatus("ACTIVE");
        user.setUserType("HOSPITAL");
        user.setEmployeeId(externalSubject);
        return user;
    }

    private void createBinding(Long tenantId, Long userId, Long providerId,
                                String externalSubject, String displayName, String operator) {
        long bindingId = Ids.next();
        String sql = "INSERT INTO sec_identity_binding (id, tenant_id, user_id, provider_id, "
                + "external_subject, external_display_name, binding_status, last_verified_time, "
                + "created_by, created_time) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, bindingId);
            ps.setLong(2, tenantId);
            ps.setLong(3, userId);
            ps.setLong(4, providerId);
            ps.setString(5, externalSubject);
            ps.setString(6, displayName);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(8, operator);
            ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建身份绑定失败: " + ex.getMessage(), ex);
        }
    }

    private void updateBindingVerifiedTime(Long bindingId) {
        String sql = "UPDATE sec_identity_binding SET last_verified_time = ?, updated_time = ? WHERE id = ?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, bindingId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("更新绑定验证时间失败: bindingId={}", bindingId, ex);
        }
    }

    private Map<String, Object> buildUserInfo(SecurityUser user) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
        info.put("id", user.getId());
        info.put("tenant_id", user.getTenantId());
        info.put("username", user.getUsername());
        info.put("display_name", user.getDisplayName());
        info.put("status", user.getStatus());
        info.put("user_type", user.getUserType());
        return info;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            int commaIndex = ip.indexOf(',');
            if (commaIndex > 0) ip = ip.substring(0, commaIndex).trim();
            return ip;
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) return ip;
        return request.getRemoteAddr();
    }

    private IdentityProvider mapProvider(ResultSet rs) throws SQLException {
        IdentityProvider provider = new IdentityProvider();
        provider.setId(rs.getLong("id"));
        provider.setTenantId(rs.getLong("tenant_id"));
        provider.setProviderCode(rs.getString("provider_code"));
        provider.setProviderName(rs.getString("provider_name"));
        provider.setProviderType(rs.getString("provider_type"));
        provider.setAdapterCode(rs.getString("adapter_code"));
        provider.setSyncMode(rs.getString("sync_mode"));
        provider.setSyncCron(rs.getString("sync_cron"));
        provider.setPriority(rs.getInt("priority"));
        provider.setStatus(rs.getString("status"));
        Timestamp lastSync = rs.getTimestamp("last_sync_time");
        if (lastSync != null) provider.setLastSyncTime(lastSync.toLocalDateTime());
        provider.setLastSyncResult(rs.getString("last_sync_result"));
        provider.setLastSyncSummary(rs.getString("last_sync_summary"));
        provider.setCreatedBy(rs.getString("created_by"));
        Timestamp created = rs.getTimestamp("created_time");
        if (created != null) provider.setCreatedTime(created.toLocalDateTime());
        provider.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updated = rs.getTimestamp("updated_time");
        if (updated != null) provider.setUpdatedTime(updated.toLocalDateTime());
        return provider;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15: 走 HikariCP 连接池（EngineDataSourceConfig 暴露的 DataSource）。
        return dataSource.getConnection();
    }
}
