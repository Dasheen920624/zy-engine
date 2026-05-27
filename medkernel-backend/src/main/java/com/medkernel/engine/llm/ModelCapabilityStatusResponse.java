package com.medkernel.engine.llm;

/**
  * 模型能力可用状态响应对象。
  */
public record ModelCapabilityStatusResponse(
    String capabilityCode,
    String routeStrategy,
    String desensitizeStrategy,
    Boolean fallbackAvailable,
    String fallbackReason
) {}
