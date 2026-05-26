package com.medkernel.shared.observability;

import java.io.IOException;

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
 * MedKernel v1.0 GA · GA-ENG-OBS-01 MDC 注入 Filter。
 *
 * <p>在请求进入时把 traceId / tenantId / userId / requestPath 写入 MDC，
 * 请求结束（含异常路径）清理。配合 logback JSON pattern 让每条日志带这些字段。
 *
 * <p>Order 设在 TraceIdFilter 之后（确保 RequestContext.traceId 已经填充）。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class MdcEnrichmentFilter extends OncePerRequestFilter {

    public static final String MDC_TRACE_ID    = "traceId";
    public static final String MDC_TENANT_ID   = "tenantId";
    public static final String MDC_USER_ID     = "userId";
    public static final String MDC_REQUEST_PATH = "requestPath";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String traceId = RequestContext.currentTraceId();
            if (traceId != null) {
                MDC.put(MDC_TRACE_ID, traceId);
            }
            OrgScope scope = RequestContext.currentOrgScope();
            if (scope != null && scope.hasTenant()) {
                MDC.put(MDC_TENANT_ID, scope.tenantId());
            }
            RequestContext.currentUserId().ifPresent(uid -> MDC.put(MDC_USER_ID, uid));
            MDC.put(MDC_REQUEST_PATH, request.getRequestURI());

            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_REQUEST_PATH);
        }
    }
}
