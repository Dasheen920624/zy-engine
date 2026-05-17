package com.zyengine.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zyengine.database")
public class EnginePersistenceProperties {
    private boolean enabled;
    private boolean initSchema = true;
    private String dialect;
    private String url;
    private String username;
    private String password;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInitSchema() {
        return initSchema;
    }

    public void setInitSchema(boolean initSchema) {
        this.initSchema = initSchema;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public boolean hasPassword() {
        return password != null && !password.trim().isEmpty();
    }

    public boolean localFileDatabase() {
        String value = dialect == null ? "" : dialect.trim().toLowerCase();
        return "h2".equals(value) || "local".equals(value) || "local_h2".equals(value);
    }

    public boolean hasRequiredCredentials() {
        return localFileDatabase() || hasPassword();
    }

    public String providerName() {
        return localFileDatabase() ? "LOCAL_H2_FILE" : "ORACLE";
    }
}
