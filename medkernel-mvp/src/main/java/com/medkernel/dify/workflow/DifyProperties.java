package com.medkernel.dify.workflow;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "medkernel.dify")
public class DifyProperties {
    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private int timeoutMs;
    private String user;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getTimeoutMs() {
        return timeoutMs <= 0 ? 3000 : timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean ready() {
        return enabled && hasText(baseUrl) && hasText(apiKey);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
