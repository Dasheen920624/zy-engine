package com.medkernel.advanced.llm;

public record LlmResponse(
    String text,
    String providerId,
    Integer tokensUsed,
    Long latencyMs,
    Boolean degraded
) {}
