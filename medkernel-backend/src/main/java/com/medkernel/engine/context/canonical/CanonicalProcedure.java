package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准手术/操作。对齐 detail spec §7.4 Procedure。
 */
public record CanonicalProcedure(
    @NotBlank String procedureId,
    @NotBlank String code,
    @NotBlank String displayName,
    String anesthesiaType,
    String surgeonId,
    Instant performedAt,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
