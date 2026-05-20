package com.medkernel.dify;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "medkernel.model-gateway")
public class ModelGatewayProperties {
    private Map<String, String> degradationChains = new LinkedHashMap<String, String>();
    private Map<String, Integer> providerTimeouts = new LinkedHashMap<String, Integer>();
    private boolean enabled = true;
    private int defaultTimeoutMs = 5000;
    private int maxRetryCount = 2;

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
