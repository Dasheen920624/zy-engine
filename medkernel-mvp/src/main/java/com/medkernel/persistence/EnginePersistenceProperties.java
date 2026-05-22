package com.medkernel.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "medkernel.database")
public class EnginePersistenceProperties {
    private boolean enabled;
    private String role = "production";
    private String dialect;
    private String url;
    private String username;
    private String password;
    /** PR-FINAL-15: HikariCP 连接池嵌套配置。null 时 EngineDataSourceConfig 用默认值。 */
    private HikariOptions hikari = new HikariOptions();

    public HikariOptions getHikari() {
        return hikari;
    }

    public void setHikari(HikariOptions hikari) {
        this.hikari = hikari;
    }

    /**
     * HikariCP 连接池配置（绑定 medkernel.database.hikari.*）。
     * 字段命名与 HikariCP 语义对齐但加 -Ms 后缀以避免与 Spring Boot
     * spring.datasource.hikari.connection-timeout 配置冲突。
     */
    public static class HikariOptions {
        private int maximumPoolSize = 20;
        private int minimumIdle = 2;
        private long connectionTimeoutMs = 3000;
        private long idleTimeoutMs = 600000;
        private long maxLifetimeMs = 1800000;
        private long leakDetectionThresholdMs = 2000;
        private String poolName = "MedKernelHikari";

        public int getMaximumPoolSize() { return maximumPoolSize; }
        public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }
        public int getMinimumIdle() { return minimumIdle; }
        public void setMinimumIdle(int minimumIdle) { this.minimumIdle = minimumIdle; }
        public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
        public void setConnectionTimeoutMs(long connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }
        public long getIdleTimeoutMs() { return idleTimeoutMs; }
        public void setIdleTimeoutMs(long idleTimeoutMs) { this.idleTimeoutMs = idleTimeoutMs; }
        public long getMaxLifetimeMs() { return maxLifetimeMs; }
        public void setMaxLifetimeMs(long maxLifetimeMs) { this.maxLifetimeMs = maxLifetimeMs; }
        public long getLeakDetectionThresholdMs() { return leakDetectionThresholdMs; }
        public void setLeakDetectionThresholdMs(long leakDetectionThresholdMs) { this.leakDetectionThresholdMs = leakDetectionThresholdMs; }
        public String getPoolName() { return poolName; }
        public void setPoolName(String poolName) { this.poolName = poolName; }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    /**
     * 获取 JDBC URL（兼容旧代码，等同 getUrl）。
     */
    public String getJdbcUrl() {
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
        if (localFileDatabase()) {
            return "LOCAL_H2_FILE";
        }
        String value = dialect == null ? "" : dialect.trim().toLowerCase();
        if ("dm".equals(value) || "dameng".equals(value)) {
            return "DM";
        }
        if ("postgres".equals(value) || "postgresql".equals(value) || "pg".equals(value)) {
            return "POSTGRESQL";
        }
        if ("kingbase".equals(value) || "kingbasees".equals(value)) {
            return "KINGBASE";
        }
        return "ORACLE";
    }

    public String roleName() {
        String value = role == null ? "" : role.trim().toLowerCase();
        if (localFileDatabase() || "dev".equals(value) || "development".equals(value) || "local".equals(value)) {
            return "DEVELOPMENT_LOCAL";
        }
        return "PRODUCTION_AUTHORITY";
    }

    public boolean productionAuthority() {
        return "PRODUCTION_AUTHORITY".equals(roleName());
    }
}
