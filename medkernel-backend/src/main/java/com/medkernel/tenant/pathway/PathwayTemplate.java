package com.medkernel.tenant.pathway;

public record PathwayTemplate(
    String id,
    String name,
    String disease,
    String department,
    Integer nodes,
    String status
) {}
