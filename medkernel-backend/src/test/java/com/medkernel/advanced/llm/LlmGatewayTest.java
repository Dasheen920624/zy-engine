package com.medkernel.advanced.llm;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmGatewayTest {

    @Test
    void localProviderIsPreferred() {
        LlmGateway gw = new LlmGateway(List.of(new SaasFallbackProvider(), new OllamaMockProvider()));
        LlmResponse resp = gw.chat(new LlmRequest("胸痛 AMI 患者下一步建议", null, null));
        assertThat(resp.providerId()).startsWith("ollama");
        assertThat(resp.degraded()).isFalse();
    }

    @Test
    void fallsBackWhenLocalUnhealthy() {
        LlmProvider deadLocal = new LlmProvider() {
            public String id() { return "dead-local"; }
            public String name() { return "Dead Local"; }
            public boolean isLocal() { return true; }
            public boolean isHealthy() { return false; }
            public LlmResponse chat(LlmRequest r) { throw new RuntimeException("dead"); }
        };
        LlmGateway gw = new LlmGateway(List.of(deadLocal, new SaasFallbackProvider()));
        LlmResponse resp = gw.chat(new LlmRequest("test", null, null));
        assertThat(resp.providerId()).startsWith("saas");
        assertThat(resp.degraded()).isTrue();
    }
}
