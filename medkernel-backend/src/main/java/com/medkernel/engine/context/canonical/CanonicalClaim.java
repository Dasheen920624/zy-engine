package com.medkernel.engine.context.canonical;

import java.math.BigDecimal;
import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准医保结算单。对齐 detail spec §7.4 Claim/Cost。
 */
public record CanonicalClaim(
    @NotBlank String claimId,
    @NotBlank String drgCode,
    BigDecimal totalCost,
    BigDecimal insurancePaid,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
