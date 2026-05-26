package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准临床路径状态快照。对齐 detail spec §7.4 CarePlan/Pathway。
 */
public record CanonicalCarePlan(
    @NotBlank String planId,
    @NotBlank String pathwayId,
    String currentNodeId,
    String varianceCode,
    Instant plannedFinishAt,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
