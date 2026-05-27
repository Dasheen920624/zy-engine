package com.medkernel.engine.context;

import java.util.List;

/**
 * 临床事件批量受理结果。
 */
public record ClinicalEventBatchResponse(
    String batchId,
    List<ClinicalEventAcceptedResponse> items,
    String traceId
) {}
