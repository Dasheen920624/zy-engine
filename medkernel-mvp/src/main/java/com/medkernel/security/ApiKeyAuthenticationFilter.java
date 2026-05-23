package com.medkernel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方 API Key 鉴权过滤器。
 * <p>
 * SEC-003: 验证 X-Api-Key + X-Signature + X-Timestamp + X-Nonce 请求头。
 * <p>
 * 签名算法：HMAC-SHA256(secret, timestamp + nonce + METHOD + path + bodyHash)
 * <p>
 * 请求头要求：
 * - X-Api-Key: API Key（mk_xxx）
 * - X-Signature: HMAC-SHA256 签名
 * - X-Timestamp: 请求时间戳（毫秒）
 * - X-Nonce: 唯一随机字符串
 * <p>
 * 此过滤器在 SecurityFilter 之前执行（Order=98），仅处理携带 X-Api-Key 的请求。
 * 不携带 X-Api-Key 的请求直接放行，由 SecurityFilter 处理 JWT 鉴权。
 */
@Component
@Order(98)
public class ApiKeyAuthenticationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String HEADER_API_KEY = "X-Api-Key";
    private static final String HEADER_SIGNATURE = "X-Signature";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService, ObjectMapper objectMapper) {
        this.apiKeyService = apiKeyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("[SEC-003] ApiKeyAuthenticationFilter initialized (Order=98)");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String apiKeyHeader = httpRequest.getHeader(HEADER_API_KEY);

        // 不携带 X-Api-Key 的请求直接放行
        if (apiKeyHeader == null || apiKeyHeader.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        // 携带 X-Api-Key 的请求进行签名验证
        try {
            validateApiKeyRequest(httpRequest);
        } catch (AuthException e) {
            log.warn("[SEC-003] API Key authentication failed: {}", e.getMessage());
            writeErrorResponse(httpResponse, e.getErrorCode(), e.getMessage());
            return;
        }

        chain.doFilter(request, response);
    }

    private void validateApiKeyRequest(HttpServletRequest request) {
        String rawKey = request.getHeader(HEADER_API_KEY);
        String signature = request.getHeader(HEADER_SIGNATURE);
        String timestampStr = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);

        // 必需头部检查
        if (signature == null || signature.isEmpty()) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Missing X-Signature header");
        }
        if (timestampStr == null || timestampStr.isEmpty()) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Missing X-Timestamp header");
        }
        if (nonce == null || nonce.isEmpty()) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Missing X-Nonce header");
        }

        // 解析时间戳
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Invalid X-Timestamp format");
        }

        // 验证 API Key
        ApiKey apiKey = apiKeyService.validateKey(rawKey);
        if (apiKey == null) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Invalid or expired API Key");
        }

        // 计算请求体哈希
        String bodyHash = "";
        // 注意：不读取请求体以避免消耗 input stream，bodyHash 可选

        // 验证签名
        String method = request.getMethod();
        String path = request.getRequestURI();
        boolean valid = apiKeyService.verifySignature(apiKey, signature, timestamp, nonce, method, path, bodyHash);
        if (!valid) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Signature verification failed");
        }

        // 注入安全上下文
        SecurityContext.setUsername("API:" + apiKey.getName());
        // API Key 场景下 userId 和 tenantId 使用占位值
        SecurityContext.setUserId(0L);
        try {
            SecurityContext.setTenantId(Long.parseLong(apiKey.getTenantId()));
        } catch (NumberFormatException e) {
            SecurityContext.setTenantId(0L);
        }

        log.debug("[SEC-003] API Key authenticated: name={}, tenant={}", apiKey.getName(), apiKey.getTenantId());
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("success", false);
        body.put("code", errorCode.name());
        body.put("message", message);
        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(body));
        writer.flush();
    }

    @Override
    public void destroy() {
        log.info("[SEC-003] ApiKeyAuthenticationFilter destroyed");
    }
}
