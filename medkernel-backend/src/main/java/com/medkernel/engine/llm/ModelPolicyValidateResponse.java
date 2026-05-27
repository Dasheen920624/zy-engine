package com.medkernel.engine.llm;

/**
  * 场景路由与脱敏策略发布前校验结果响应对象。
  */
public record ModelPolicyValidateResponse(
    Boolean valid,
    String message,
    Boolean fallbackAvailable
) {}
