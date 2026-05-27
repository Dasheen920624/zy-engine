package com.medkernel.engine.evaluation;

/**
 * 评估运行接收结果响应。
 *
 * <p>返回运行 ID、终态、写入结果数量、质控问题数量、自动派生整改任务数量和 traceId。
 */
public record EvaluationRunResponse(
    String runId,
    EvaluationRunStatus status,
    int resultCount,
    int findingCount,
    int taskCount,
    String traceId
) {}
