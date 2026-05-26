package com.medkernel.engine.context.canonical;

import java.math.BigDecimal;
import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准用药。对齐 detail spec §7.4 Medication。
 */
public record CanonicalMedication(
    @NotBlank String medicationId,
    @NotBlank String code,
    @NotBlank String displayName,
    BigDecimal dose,
    String doseUnit,
    String route,
    String frequency,
    String durationDays,
    String prescriptionStatus,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
