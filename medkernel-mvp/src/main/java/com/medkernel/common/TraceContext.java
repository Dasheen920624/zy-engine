package com.medkernel.common;

import java.util.UUID;

public final class TraceContext {
    public static final String HEADER = "X-Trace-Id";
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<String>();

    private TraceContext() {
    }

    public static String getTraceId() {
        String traceId = TRACE_ID.get();
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = newTraceId();
            TRACE_ID.set(traceId);
        }
        return traceId;
    }

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId == null || traceId.trim().isEmpty() ? newTraceId() : traceId);
    }

    /**
     * 获取当前用户名（审计日志用）。MVP 阶段默认返回 "system"。
     */
    public static String getUsername() {
        return "system";
    }

    public static void clear() {
        TRACE_ID.remove();
    }

    private static String newTraceId() {
        return "trace-" + UUID.randomUUID().toString().replace("-", "");
    }
}

