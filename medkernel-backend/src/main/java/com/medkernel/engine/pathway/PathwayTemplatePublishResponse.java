package com.medkernel.engine.pathway;

public record PathwayTemplatePublishResponse(
    String templateId,
    PathwayTemplateStatus status,
    String traceId
) {}
