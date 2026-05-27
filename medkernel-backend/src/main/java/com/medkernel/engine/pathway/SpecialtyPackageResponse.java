package com.medkernel.engine.pathway;

public record SpecialtyPackageResponse(
    String packageId,
    SpecialtyPackageStatus status,
    String traceId
) {}
