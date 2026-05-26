package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准随访计划。对齐 detail spec §7.4 FollowUp。
 */
public record CanonicalFollowUp(
    @NotBlank String followUpId,
    @NotBlank String planType,
    Instant plannedAt,
    String questionnaireId,
    String abnormalFlag,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
