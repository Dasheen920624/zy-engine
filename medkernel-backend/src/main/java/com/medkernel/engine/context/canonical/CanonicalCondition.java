package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准诊断。对齐 detail spec §7.4 Condition。
 */
public record CanonicalCondition(
    @NotBlank String conditionId,
    @NotBlank String code,
    @NotBlank String codeSystem,
    @NotBlank String displayName,
    String stage,
    String severity,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant onsetTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
