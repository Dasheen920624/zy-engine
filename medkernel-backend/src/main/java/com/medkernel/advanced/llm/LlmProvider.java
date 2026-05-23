package com.medkernel.advanced.llm;

/**
 * GA-ADVANCED-01 · LLM Gateway SPI（提供方接口）。
 */
public interface LlmProvider {
    String id();
    String name();
    boolean isLocal();
    boolean isHealthy();
    LlmResponse chat(LlmRequest request);
}
