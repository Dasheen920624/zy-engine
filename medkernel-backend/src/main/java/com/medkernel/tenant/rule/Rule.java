package com.medkernel.tenant.rule;

public record Rule(
    String id,
    String name,
    String category,
    String severity,
    Long hits,
    String status
) {}
