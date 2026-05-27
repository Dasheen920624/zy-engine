package com.medkernel.shared.observability;

/**
 * 持久化 payload 时的元描述。
 *
 * @param tenantId     租户
 * @param entityType   关联实体类型（如 "clinical_event"）
 * @param entityId     关联实体 ID
 * @param contentType  payload MIME（如 "application/json"、"application/cda+xml"）
 */
public record PayloadDescriptor(
    String tenantId,
    String entityType,
    String entityId,
    String contentType
) {}
