package com.medkernel.engine.context;

/**
 * 临床事件列表筛选条件。
 */
public record ClinicalEventFilter(
    String patientId,
    String encounterId,
    ClinicalEventStatus status,
    ClinicalEventType eventType
) {}
