package com.medkernel.engine.evaluation;

public record EvaluationRunResponse(
    String runId,
    EvaluationRunStatus status,
    int resultCount,
    int findingCount,
    int taskCount,
    String traceId
) {}
