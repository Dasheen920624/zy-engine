package com.medkernel.security;

import com.medkernel.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证服务：登录、登出、当前用户查询。
 * 使用 BCryptPasswordEncoder 验证密码。
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SecurityPersistenceService persistenceService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(SecurityPersistenceService persistenceService,
                       JwtTokenProvider jwtTokenProvider,
                       SecurityProperties securityProperties) {
        this.persistenceService = persistenceService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityProperties = securityProperties;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 用户登录。
     *
     * @param username 用户名
     * @param password 明文密码
     * @param request  HTTP 请求（用于获取 IP）
     * @return 登录结果 Map，包含 token 和用户信息
     */
    public Map<String, Object> login(String username, String password, HttpServletRequest request) {
        String ip = getClientIp(request);

        SecurityUser user = persistenceService.findByUsername(username);
        if (user == null) {
            log.warn("[auth] login failed: user not found, username={}", username);
            throw new AuthException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            log.warn("[auth] login failed: user inactive, userId={}", user.getId());
            throw new AuthException(ErrorCode.FORBIDDEN, "账户已禁用");
        }

        // 检查锁定状态
        if (user.getLockedUntil() != null && LocalDateTime.now().isBefore(user.getLockedUntil())) {
            log.warn("[auth] login failed: user locked, userId={}", user.getId());
            throw new AuthException(ErrorCode.USER_LOCKED, "账户已锁定，请稍后再试");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int attempts = user.getLoginAttempts() + 1;
            persistenceService.updateLoginStatus(user.getId(), false, ip);

            if (attempts >= securityProperties.getLockThreshold()) {
                persistenceService.lockUser(user.getId(), securityProperties.getLockDurationMinutes());
                log.warn("[auth] user locked due to too many failed attempts, userId={}, attempts={}", user.getId(), attempts);
                persistenceService.writeAuditLog(user.getId(), user.getTenantId(), "USER_LOCKED", ip,
                        "locked after " + attempts + " failed attempts");
                throw new AuthException(ErrorCode.USER_LOCKED, "登录失败次数过多，账户已锁定");
            }

            persistenceService.writeAuditLog(user.getId(), user.getTenantId(), "LOGIN_FAILED", ip,
                    "attempt " + attempts);
            throw new AuthException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }

        // 登录成功
        persistenceService.updateLoginStatus(user.getId(), true, ip);
        persistenceService.writeAuditLog(user.getId(), user.getTenantId(), "LOGIN", ip, null);

        String token = jwtTokenProvider.createToken(
                user.getId(), user.getTenantId(), user.getUsername(), user.getDisplayName());

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("token", token);
        result.put("user", buildUserInfo(user));
        return result;
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 用户信息 Map
     */
    public Map<String, Object> getCurrentUser() {
        Long userId = SecurityContext.getUserId();
        if (userId == null) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        SecurityUser user = persistenceService.findById(userId);
        if (user == null) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }

        return buildUserInfo(user);
    }

    /**
     * 用户登出（写审计日志）。
     */
    public void logout(HttpServletRequest request) {
        Long userId = SecurityContext.getUserId();
        Long tenantId = SecurityContext.getTenantId();
        if (userId != null) {
            String ip = getClientIp(request);
            persistenceService.writeAuditLog(userId, tenantId != null ? tenantId : 0L, "LOGOUT", ip, null);
        }
    }

    private Map<String, Object> buildUserInfo(SecurityUser user) {
        Map<String, Object> info = new LinkedHashMap<String, Object>();
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

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            // 取第一个 IP（客户端真实 IP）
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
