package com.medkernel.engine.security.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.security.AuthCookieProperties;

import jakarta.validation.Valid;

/**
 * 平台账号登录 / 登出控制器。/auth/login 与 /auth/logout 在 SecurityConfig 中 permitAll；
 * 登录成功写 httpOnly cookie，登出清 cookie。
 */
@RestController
@RequestMapping("/api/v1/auth")
@Profile({"dev", "test"})
public class AuthController {

    private final AuthService authService;
    private final AuthCookieProperties cookieProps;

    public AuthController(AuthService authService, AuthCookieProperties cookieProps) {
        this.authService = authService;
        this.cookieProps = cookieProps;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResult<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthService.AuthResult result = authService.login(req.tenantOrDefault(), req.username(), req.password());
        ResponseCookie cookie = buildCookie(result.jwt(), cookieProps.maxAgeSeconds());
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(ApiResult.ok(result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResult<Void>> logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(jwt == null ? null : jwt.getSubject());
        ResponseCookie cleared = buildCookie("", 0);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cleared.toString())
            .body(ApiResult.ok(null));
    }

    private ResponseCookie buildCookie(String value, long maxAge) {
        return ResponseCookie.from(cookieProps.name(), value)
            .httpOnly(true)
            .secure(cookieProps.secure())
            .sameSite(cookieProps.sameSite())
            .path(cookieProps.path())
            .maxAge(maxAge)
            .build();
    }
}
