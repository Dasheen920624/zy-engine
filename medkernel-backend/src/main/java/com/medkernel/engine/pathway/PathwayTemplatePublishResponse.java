package com.medkernel.engine.pathway;

/**
 * 路径模板发布响应。
 *
 * <p>返回模板 ID、发布后状态和 traceId，用于确认发布门禁后的状态变更。
 */
public record PathwayTemplatePublishResponse(
    String templateId,
    PathwayTemplateStatus status,
    String traceId
) {}
