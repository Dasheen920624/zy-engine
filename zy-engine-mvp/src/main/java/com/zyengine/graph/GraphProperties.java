package com.zyengine.graph;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zyengine.graph")
public class GraphProperties {
    private boolean enabled;
    private String uri;
    private String username;
    private String password;
    private String database;
    private int timeoutMs;
    private String defaultVersion;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public int getTimeoutMs() {
        return timeoutMs <= 0 ? 3000 : timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public boolean ready() {
        return enabled && hasText(uri) && hasText(username) && hasText(password);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
