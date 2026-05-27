package com.medkernel.engine.pkg;

import java.util.List;

/**
 * 知识包同步与发布执行结果响应 DTO。
 */
public record PackageSyncResponse(
    String planId,
    String packageId,
    ReleasePlanStatus status,
    List<SyncLogResponse> logs
) {}
