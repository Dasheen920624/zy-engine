package com.medkernel.shared.trace;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * MedKernel v1.0 GA · GA-ENG-BASE-03 traceId 入站 / 出站 + MDC 传播。
 *
 * <p>每个 HTTP 请求执行链：
 * <ol>
 *   <li>从 {@code X-Trace-Id} 请求头读取；缺失时生成 UUID v4</li>
 *   <li>放入 SLF4J MDC（key={@value #MDC_KEY}），所有日志自动带 traceId</li>
 *   <li>建立 {@link RequestContext.Snapshot}（仅 traceId；组织 / 用户由 GA-ENG-BASE-01/02 注入）</li>
 *   <li>回写到 {@code X-Trace-Id} 响应头</li>
 *   <li>无论成功失败都清理 MDC 和 ThreadLocal，避免 Virtual Thread 复用泄漏</li>
 * </ol>
 *
 * <p>Order = {@link Ordered#HIGHEST_PRECEDENCE} + 10 — 早于 Spring Security 过滤器链，
 * 确保未鉴权的请求返回错误时也带上 traceId，方便客户端反馈定位。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = sanitize(request.getHeader(HEADER));
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, traceId);
        RequestContext.restore(new RequestContext.Snapshot(traceId, OrgScope.empty(), null));
        response.setHeader(HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
            RequestContext.clear();
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 128) {
            return null;
        }
        // 仅允许 ASCII 可打印 + UUID/十六进制 / 短横线 / 下划线，防止日志注入
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) {
                return null;
            }
        }
        return trimmed;
    }
}
