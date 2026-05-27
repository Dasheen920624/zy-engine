package com.medkernel.engine.context;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 临床事件接收请求。
 */
public record ClinicalEventRequest(
    @NotBlank String eventId,
    @NotNull ClinicalEventType eventType,
    @NotBlank String patientId,
    String encounterId,
    String sourceSystem,
    @NotBlank String packageVersion,
    @NotNull JsonNode payload,
    @NotNull Instant occurredAt
) {}
