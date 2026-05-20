package com.medkernel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.common.TraceContext;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 安全过滤器：验证 JWT 令牌并将 platform_user_id 注入 ThreadLocal。
 * 不变量 #2：SecurityFilter 注入 ThreadLocal。
 */
@Component
@Order(100) // After TraceFilter (default LOWEST_PRECEDENCE is fine, but explicit ordering is safer)
public class SecurityFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /** 不需要认证的路径白名单。 */
    private static final Set<String> PUBLIC_PATHS = new HashSet<String>(Arrays.asList(
            "/api/auth/login",
            "/api/auth/health",
            "/api/health",
            "/api/system/org-context",
            "/api/security/sso/callback",
            "/api/security/sso/saml/acs",
            "/actuator/health"
    ));

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public SecurityFilter(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        // OPTIONS 请求（CORS 预检）和白名单路径跳过认证
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod()) || isPublicPath(path)) {
            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContext.clear();
            }
            return;
        }

        String token = extractToken(httpRequest);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            String traceId = TraceContext.getTraceId();
            log.warn("[traceId={}] unauthorized request to {}", traceId, path);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.setHeader(TraceContext.HEADER, traceId);
            // 走 Jackson 序列化 ApiResult，确保字段名（含 trace_id）与全局 envelope 契约一致，
            // 避免手写 JSON 引入字段名漂移或 message 转义破坏 JSON（AUDIT §1.4 + §5.2）。
            ApiResult<Void> body = ApiResult.failure(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage());
            objectMapper.writeValue(httpResponse.getWriter(), body);
            return;
        }

        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            Long userId = jwtTokenProvider.getUserId(claims);
            Long tenantId = jwtTokenProvider.getTenantId(claims);
            String username = jwtTokenProvider.getUsername(claims);

            SecurityContext.setUserId(userId);
            SecurityContext.setTenantId(tenantId);
            SecurityContext.setUsername(username);

            chain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
}
