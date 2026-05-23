package com.medkernel.advanced.llm;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * GA-ADVANCED-01 · LLM Gateway 入口（Controller + 降级链）。
 */
@RestController
@RequestMapping("/api/v1/advanced/llm")
public class LlmGateway {

    private final List<LlmProvider> providers;

    public LlmGateway(List<LlmProvider> providers) {
        this.providers = providers.stream()
            .sorted((a, b) -> Boolean.compare(!a.isLocal(), !b.isLocal()))
            .toList();
    }

    @GetMapping("/providers")
    public List<Map<String, Object>> listProviders() {
        return providers.stream()
            .map(p -> Map.<String, Object>of(
                "id", p.id(), "name", p.name(),
                "local", p.isLocal(), "healthy", p.isHealthy()
            ))
            .toList();
    }

    @PostMapping("/chat")
    public LlmResponse chat(@Valid @RequestBody LlmRequest request) {
        return invokeWithFallback(request);
    }

    public LlmResponse invokeWithFallback(LlmRequest request) {
        for (LlmProvider p : providers) {
            if (!p.isHealthy()) continue;
            try { return p.chat(request); }
            catch (RuntimeException ex) { /* try next */ }
        }
        throw new NoSuchElementException("LLM Gateway: all providers unavailable");
    }
}
