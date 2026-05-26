package com.medkernel.engine.context.canonical;

import java.time.Instant;
import java.util.List;

import com.medkernel.engine.context.QualityStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 标准诊断报告（检验/影像/病理/内镜/心电）。对齐 detail spec §7.4 DiagnosticReport。
 */
public record CanonicalDiagnosticReport(
    @NotBlank String reportId,
    @NotBlank String reportType,
    @NotBlank String conclusion,
    List<String> keyFindings,
    String signedBy,
    Instant signedAt,
    String sourceSystem,
    String sourceRecordId,
    String mappedVersion,
    Instant eventTime,
    Instant receivedTime,
    @NotNull QualityStatus qualityStatus
) {}
