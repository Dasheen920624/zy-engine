package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准症状/体征。对齐 detail spec §7.4 Symptom/Sign。
 */
public record CanonicalSymptom(
    @NotBlank String symptomId,
    @NotBlank String name,
    String severity,
    String negation,
    Instant observedAt,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
