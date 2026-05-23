package com.medkernel.advanced.llm;

import jakarta.validation.constraints.NotBlank;

public record LlmRequest(
    @NotBlank String prompt,
    Double temperature,
    Integer maxTokens
) {
    public LlmRequest {
        if (temperature == null) temperature = 0.3;
        if (maxTokens == null) maxTokens = 512;
    }
}
