package com.medkernel.security;

import com.medkernel.common.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("安全服务单元测试")
class SecurityServiceTest {

    @Mock
    private SecurityPersistenceService persistenceService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SecurityProperties securityProperties;

    @Mock
    private HttpServletRequest request;

    private AuthService authService;
    private BCryptPasswordEncoder passwordEncoder;

    private static final String USERNAME = "admin";
    private static final String RAW_PASSWORD = "password123";
    private static final Long USER_ID = 1L;
    private static final Long TENANT_ID = 100L;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(persistenceService, jwtTokenProvider, securityProperties);
    }

    @AfterEach
    void tearDown() {
        SecurityContext.clear();
    }

    // ──────────────────────── 辅助方法 ────────────────────────

    private SecurityUser buildActiveUser() {
        SecurityUser user = new SecurityUser();
        user.setId(USER_ID);
        user.setTenantId(TENANT_ID);
        user.setUsername(USERNAME);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setDisplayName("管理员");
        user.setEmail("admin@medkernel.com");
        user.setPhone("13800138000");
        user.setStatus("ACTIVE");
        user.setLoginAttempts(0);
        user.setRoles(Arrays.asList("ADMIN"));
        user.setPermissions(Arrays.asList("system:manage", "user:read"));
        return user;
    }

    private SecurityUser buildLockedUser() {
        SecurityUser user = buildActiveUser();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        return user;
    }

    private SecurityUser buildInactiveUser() {
        SecurityUser user = buildActiveUser();
        user.setStatus("DISABLED");
        return user;
    }

    // ──────────────────────── 登录测试 ────────────────────────

    @Nested
    @DisplayName("用户登录")
    class LoginTests {

        @Test
        @DisplayName("登录成功 - 返回token和用户信息")
        void login_success_returnsTokenAndUserInfo() {
            SecurityUser user = buildActiveUser();
            when(persistenceService.findByUsername(USERNAME)).thenReturn(user);
            when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");
            when(jwtTokenProvider.createToken(USER_ID, TENANT_ID, USERNAME, "管理员"))
                    .thenReturn("jwt-token-xxx");

            Map<String, Object> result = authService.login(USERNAME, RAW_PASSWORD, request);

            assertNotNull(result);
            assertEquals("jwt-token-xxx", result.get("token"));
            assertNotNull(result.get("user"));

            Map<?, ?> userInfo = (Map<?, ?>) result.get("user");
            assertEquals(USER_ID, userInfo.get("id"));
            assertEquals(TENANT_ID, userInfo.get("tenant_id"));
            assertEquals(USERNAME, userInfo.get("username"));
            assertEquals("管理员", userInfo.get("display_name"));
            assertEquals("ACTIVE", userInfo.get("status"));

            verify(persistenceService).updateLoginStatus(USER_ID, true, "192.168.1.100");
            verify(persistenceService).writeAuditLog(USER_ID, TENANT_ID, "LOGIN", "192.168.1.100", null);
        }

        @Test
        @DisplayName("登录失败 - 用户不存在")
        void login_userNotFound_throwsAuthException() {
            when(persistenceService.findByUsername("unknown")).thenReturn(null);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.login("unknown", "any", request));
            assertEquals(ErrorCode.LOGIN_FAILED, ex.getErrorCode());
        }

        @Test
        @DisplayName("登录失败 - 用户已禁用")
        void login_userDisabled_throwsAuthException() {
            SecurityUser user = buildInactiveUser();
            when(persistenceService.findByUsername(USERNAME)).thenReturn(user);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.login(USERNAME, RAW_PASSWORD, request));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("登录失败 - 用户已锁定")
        void login_userLocked_throwsAuthException() {
            SecurityUser user = buildLockedUser();
            when(persistenceService.findByUsername(USERNAME)).thenReturn(user);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.login(USERNAME, RAW_PASSWORD, request));
            assertEquals(ErrorCode.USER_LOCKED, ex.getErrorCode());
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void login_wrongPassword_throwsAuthException() {
            SecurityUser user = buildActiveUser();
            when(persistenceService.findByUsername(USERNAME)).thenReturn(user);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(securityProperties.getLockThreshold()).thenReturn(5);

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.login(USERNAME, "wrong_password", request));
            assertEquals(ErrorCode.LOGIN_FAILED, ex.getErrorCode());
            verify(persistenceService).updateLoginStatus(USER_ID, false, "127.0.0.1");
        }

        @Test
        @DisplayName("登录失败 - 连续失败达到阈值后锁定账户")
        void login_exceedThreshold_locksUser() {
            SecurityUser user = buildActiveUser();
            user.setLoginAttempts(4); // 下一次失败就是第5次
            when(persistenceService.findByUsername(USERNAME)).thenReturn(user);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(securityProperties.getLockThreshold()).thenReturn(5);
            when(securityProperties.getLockDurationMinutes()).thenReturn(30);

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.login(USERNAME, "wrong_password", request));
            assertEquals(ErrorCode.USER_LOCKED, ex.getErrorCode());
            verify(persistenceService).lockUser(USER_ID, 30);
            verify(persistenceService).writeAuditLog(eq(USER_ID), eq(TENANT_ID), eq("USER_LOCKED"), anyString(), anyString());
        }
    }

    // ──────────────────────── 获取当前用户测试 ────────────────────────

    @Nested
    @DisplayName("获取当前用户")
    class GetCurrentUserTests {

        @Test
        @DisplayName("获取当前用户 - 成功返回用户信息")
        void getCurrentUser_success() {
            SecurityContext.setUserId(USER_ID);
            SecurityUser user = buildActiveUser();
            when(persistenceService.findById(USER_ID)).thenReturn(user);

            Map<String, Object> result = authService.getCurrentUser();

            assertNotNull(result);
            assertEquals(USER_ID, result.get("id"));
            assertEquals(USERNAME, result.get("username"));
        }

        @Test
        @DisplayName("获取当前用户 - 未登录时抛出异常")
        void getCurrentUser_notLoggedIn_throwsAuthException() {
            SecurityContext.clear();

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.getCurrentUser());
            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        }

        @Test
        @DisplayName("获取当前用户 - 用户不存在时抛出异常")
        void getCurrentUser_userNotFound_throwsAuthException() {
            SecurityContext.setUserId(999L);
            when(persistenceService.findById(999L)).thenReturn(null);

            AuthException ex = assertThrows(AuthException.class,
                    () -> authService.getCurrentUser());
            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        }
    }

    // ──────────────────────── 登出测试 ────────────────────────

    @Nested
    @DisplayName("用户登出")
    class LogoutTests {

        @Test
        @DisplayName("登出 - 写入审计日志")
        void logout_writesAuditLog() {
            SecurityContext.setUserId(USER_ID);
            SecurityContext.setTenantId(TENANT_ID);
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

            authService.logout(request);

            verify(persistenceService).writeAuditLog(USER_ID, TENANT_ID, "LOGOUT", "10.0.0.1", null);
        }

        @Test
        @DisplayName("登出 - 未登录时不写审计日志")
        void logout_notLoggedIn_noAuditLog() {
            SecurityContext.clear();
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            authService.logout(request);

            verify(persistenceService, never()).writeAuditLog(anyLong(), anyLong(), anyString(), anyString(), anyString());
        }
    }

    // ──────────────────────── JWT 令牌测试 ────────────────────────

    @Nested
    @DisplayName("JWT 令牌提供者")
    class JwtTokenProviderTests {

        private JwtTokenProvider realTokenProvider;

        @BeforeEach
        void initProvider() {
            SecurityProperties props = new SecurityProperties();
            props.setJwtSecret("test-secret-key-for-unit-testing-must-be-at-least-32-chars");
            realTokenProvider = new JwtTokenProvider(props);
            realTokenProvider.init();
        }

        @Test
        @DisplayName("创建令牌 - 成功签发JWT")
        void createToken_success() {
            String token = realTokenProvider.createToken(USER_ID, TENANT_ID, USERNAME, "管理员");

            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        @DisplayName("验证令牌 - 有效令牌返回true")
        void validateToken_validToken_returnsTrue() {
            String token = realTokenProvider.createToken(USER_ID, TENANT_ID, USERNAME, "管理员");

            assertTrue(realTokenProvider.validateToken(token));
        }

        @Test
        @DisplayName("验证令牌 - 无效令牌返回false")
        void validateToken_invalidToken_returnsFalse() {
            assertFalse(realTokenProvider.validateToken("invalid.token.string"));
        }

        @Test
        @DisplayName("验证令牌 - 空令牌返回false")
        void validateToken_emptyToken_returnsFalse() {
            assertFalse(realTokenProvider.validateToken(""));
        }

        @Test
        @DisplayName("解析令牌 - 正确提取用户ID和租户ID")
        void parseToken_extractsUserIdAndTenantId() {
            String token = realTokenProvider.createToken(USER_ID, TENANT_ID, USERNAME, "管理员");

            io.jsonwebtoken.Claims claims = realTokenProvider.parseToken(token);

            assertEquals(USER_ID, realTokenProvider.getUserId(claims));
            assertEquals(TENANT_ID, realTokenProvider.getTenantId(claims));
            assertEquals(USERNAME, realTokenProvider.getUsername(claims));
        }
    }

    // ──────────────────────── SecurityContext 测试 ────────────────────────

    @Nested
    @DisplayName("安全上下文")
    class SecurityContextTests {

        @Test
        @DisplayName("设置和获取用户ID")
        void setAndGetUserId() {
            SecurityContext.setUserId(USER_ID);
            assertEquals(USER_ID, SecurityContext.getUserId());
        }

        @Test
        @DisplayName("设置和获取租户ID")
        void setAndGetTenantId() {
            SecurityContext.setTenantId(TENANT_ID);
            assertEquals(TENANT_ID, SecurityContext.getTenantId());
        }

        @Test
        @DisplayName("清除上下文后返回null")
        void clear_removesAllValues() {
            SecurityContext.setUserId(USER_ID);
            SecurityContext.setTenantId(TENANT_ID);
            SecurityContext.setUsername(USERNAME);

            SecurityContext.clear();

            assertEquals(null, SecurityContext.getUserId());
            assertEquals(null, SecurityContext.getTenantId());
            assertEquals(null, SecurityContext.getUsername());
        }
    }

    // ──────────────────────── AuthException 测试 ────────────────────────

    @Nested
    @DisplayName("认证异常")
    class AuthExceptionTests {

        @Test
        @DisplayName("异常携带正确的错误码和消息")
        void authException_carriesErrorCodeAndMessage() {
            AuthException ex = new AuthException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");

            assertEquals(ErrorCode.LOGIN_FAILED, ex.getErrorCode());
            assertEquals("用户名或密码错误", ex.getMessage());
        }
    }

    // ──────────────────────── SecurityProperties 测试 ────────────────────────

    @Nested
    @DisplayName("安全配置属性")
    class SecurityPropertiesTests {

        @Test
        @DisplayName("默认锁定阈值为5")
        void defaultLockThreshold_isFive() {
            SecurityProperties props = new SecurityProperties();
            assertEquals(5, props.getLockThreshold());
        }

        @Test
        @DisplayName("默认锁定持续时间为30分钟")
        void defaultLockDuration_isThirtyMinutes() {
            SecurityProperties props = new SecurityProperties();
            assertEquals(30, props.getLockDurationMinutes());
        }

        @Test
        @DisplayName("可设置JWT密钥")
        void setJwtSecret() {
            SecurityProperties props = new SecurityProperties();
            props.setJwtSecret("my-super-secret-key-for-jwt-signing");
            assertEquals("my-super-secret-key-for-jwt-signing", props.getJwtSecret());
        }
    }
}
