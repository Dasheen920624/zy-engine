package com.medkernel.compliance.audit;

public record AuditEvent(
    String id,
    String time,
    String user,
    String action,
    String traceId,
    String signature
) {}
