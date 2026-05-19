package com.medkernel.security;

import com.medkernel.common.ApiResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 认证控制器：POST /api/auth/login, GET /api/auth/me, POST /api/auth/logout。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, String> body,
                                                HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return ApiResult.failure(com.medkernel.common.ErrorCode.VALIDATION_ERROR,
                    "用户名和密码不能为空");
        }
        Map<String, Object> data = authService.login(username.trim(), password, request);
        return ApiResult.success(data);
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        Map<String, Object> data = authService.getCurrentUser();
        return ApiResult.success(data);
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResult.success(null);
    }

    @GetMapping("/health")
    public ApiResult<Map<String, String>> health() {
        java.util.LinkedHashMap<String, String> data = new java.util.LinkedHashMap<String, String>();
        data.put("status", "ok");
        data.put("module", "security");
        return ApiResult.success(data);
    }
}
