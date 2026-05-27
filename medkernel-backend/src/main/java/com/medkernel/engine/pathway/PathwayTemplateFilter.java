package com.medkernel.engine.pathway;

public record PathwayTemplateFilter(
    PathwayTemplateStatus status,
    String diseaseCode,
    String packageId
) {}
