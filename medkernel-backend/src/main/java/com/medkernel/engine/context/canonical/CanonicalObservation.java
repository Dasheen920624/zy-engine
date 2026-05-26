package com.medkernel.engine.context.canonical;

import java.math.BigDecimal;
import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准检验/体征观察。对齐 detail spec §7.4 Observation。
 */
public record CanonicalObservation(
    @NotBlank String observationId,
    @NotBlank String code,
    @NotBlank String displayName,
    BigDecimal valueNumeric,
    String valueString,
    String unit,
    String referenceRange,
    String criticalFlag,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
