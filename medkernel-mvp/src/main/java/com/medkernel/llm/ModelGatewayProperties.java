package com.medkernel.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "medkernel.model-gateway")
public class ModelGatewayProperties {
    private Map<String, String> degradationChains = new LinkedHashMap<String, String>();
    private Map<String, Integer> providerTimeouts = new LinkedHashMap<String, Integer>();
    /**
     * 多 Provider 配置 — 启动时由 LlmProviderFactory 为每项创建一个 ModelProvider Bean。
     * key 大写（如 QIANWEN / DEEPSEEK / KIMI / ZHIPU / OLLAMA_LOCAL），将成为 provider type。
     * 详见 ADR-0013。
     */
    private Map<String, LlmProviderConfig> providers = new LinkedHashMap<String, LlmProviderConfig>();
    private boolean enabled = true;
    private int defaultTimeoutMs = 5000;
    private int maxRetryCount = 2;

    public Map<String, LlmProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, LlmProviderConfig> providers) {
        this.providers = providers;
    }

    public Map<String, String> getDegradationChains() {
        return degradationChains;
    }

    public void setDegradationChains(Map<String, String> degradationChains) {
        this.degradationChains = degradationChains;
    }

    public Map<String, Integer> getProviderTimeouts() {
        return providerTimeouts;
    }

    public void setProviderTimeouts(Map<String, Integer> providerTimeouts) {
        this.providerTimeouts = providerTimeouts;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public void setDefaultTimeoutMs(int defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
}
