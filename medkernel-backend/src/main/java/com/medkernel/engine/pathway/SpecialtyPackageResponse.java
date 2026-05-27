package com.medkernel.engine.pathway;

/**
 * 专病包创建响应。
 *
 * <p>返回专病包业务 ID、初始状态和 traceId，便于后续创建路径模板时引用。
 */
public record SpecialtyPackageResponse(
    String packageId,
    SpecialtyPackageStatus status,
    String traceId
) {}
