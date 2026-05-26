package com.medkernel.engine.context.canonical;

import java.time.Instant;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准病历文书。对齐 detail spec §7.4 Document。
 */
public record CanonicalDocument(
    @NotBlank String documentId,
    @NotBlank String documentType,
    String contentDigest,
    String signedBy,
    Instant signedAt,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
