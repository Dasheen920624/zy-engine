package com.medkernel.engine.evaluation;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record EvaluationRunRequest(
    @NotBlank String runCode,
    @NotNull EvaluationRunType runType,
    String sourceEventId,
    String contextSnapshotId,
    String patientId,
    String encounterId,
    @NotBlank String scenarioCode,
    String packageVersion,
    @NotBlank String inputDigest,
    Instant occurredAt,
    @NotEmpty List<@Valid EvaluationResultRequest> results
) {}
