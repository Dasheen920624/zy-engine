package com.zyengine.common;

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

    public static void clear() {
        TRACE_ID.remove();
    }

    private static String newTraceId() {
        return "trace-" + UUID.randomUUID().toString().replace("-", "");
    }
}

