package com.medkernel.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等键过滤器。
 *
 * <p>对携带 {@code X-Idempotency-Key} 请求头的写操作（POST/PUT/PATCH）进行幂等校验：
 * <ul>
 *   <li>首次请求：正常执行，将 key 与响应缓存</li>
 *   <li>重复请求：直接返回缓存的响应（HTTP 409 + IDEMPOTENT_DUPLICATE）</li>
 * </ul>
 *
 * <p>缓存策略：内存 Map，最多保留 10000 条，FIFO 淘汰。生产环境应替换为 Redis。
 */
@Component
@Order(50) // 在 TraceFilter(1) 之后、SecurityFilter(100) 之前
public class IdempotencyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String HEADER = "X-Idempotency-Key";
    private static final int MAX_CACHE_SIZE = 10000;

    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    @Override
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String idempotencyKey = httpRequest.getHeader(HEADER);

        // 仅对写操作且携带幂等键的请求生效
        if (!isWriteMethod(method) || idempotencyKey == null || idempotencyKey.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        // 检查缓存
        CachedResponse cached = cache.get(idempotencyKey);
        if (cached != null) {
            log.info("[traceId={}] idempotent duplicate detected for key: {}",
                    TraceContext.getTraceId(), idempotencyKey);
            httpResponse.setStatus(HttpStatus.CONFLICT.value());
            httpResponse.setContentType("application/json;charset=UTF-8");
            String traceId = TraceContext.getTraceId();
            httpResponse.getWriter().write(String.format(
                    "{\"success\":false,\"code\":\"IDEMPOTENT_DUPLICATE\"," +
                    "\"message\":\"duplicate request detected\"," +
                    "\"message_key\":\"error.idempotent_duplicate\"," +
                    "\"trace_id\":\"%s\"}", traceId));
            return;
        }

        // 首次请求：正常执行后缓存
        chain.doFilter(request, response);

        // 缓存响应（仅缓存成功响应）
        if (httpResponse.getStatus() >= 200 && httpResponse.getStatus() < 300) {
            if (cache.size() >= MAX_CACHE_SIZE) {
                // 简单 FIFO：清除最早 10% 的条目
                cache.keySet().stream().limit(MAX_CACHE_SIZE / 10).forEach(cache::remove);
            }
            cache.put(idempotencyKey, new CachedResponse(httpResponse.getStatus()));
        }
    }

    @Override
    public void destroy() {
        cache.clear();
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }

    private static class CachedResponse {
        final int status;
        final long timestamp;

        CachedResponse(int status) {
            this.status = status;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
