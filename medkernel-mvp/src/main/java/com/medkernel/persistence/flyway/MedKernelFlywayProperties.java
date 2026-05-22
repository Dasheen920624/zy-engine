package com.medkernel.persistence.flyway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 配置（{@code medkernel.flyway.*}）。
 *
 * <p>开关默认 {@code false}：项目默认仍走 PersistenceService.@PostConstruct
 * loadSchemaStatements() 的旧路径，避免影响存量测试。
 *
 * <p>启用 Flyway 后：
 * <ul>
 *   <li>Spring Boot 启动时 Flyway Migration 先于 PersistenceService 跑</li>
 *   <li>对照 {@code flyway_schema_history} 表自动 apply 缺失版本</li>
 *   <li>locations 按 dialect 选取：{@code db/migration/common} + {@code db/migration/{vendor}}</li>
 * </ul>
 *
 * <h2>vendor 映射</h2>
 *
 * <table>
 *   <caption>medkernel.database.dialect → Flyway locations vendor</caption>
 *   <tr><th>dialect</th><th>vendor 目录</th><th>说明</th></tr>
 *   <tr><td>h2 / local</td><td>h2</td><td>开发态 H2 文件库</td></tr>
 *   <tr><td>oracle</td><td>oracle</td><td>生产 Oracle 19c+</td></tr>
 *   <tr><td>postgres / pg / postgresql</td><td>postgres</td><td>生产 PostgreSQL 12+</td></tr>
 *   <tr><td>kingbase / kingbasees</td><td>postgres</td><td>PG 兼容，复用 postgres 目录</td></tr>
 *   <tr><td>dm / dameng</td><td>oracle</td><td>DM 8 兼容 Oracle，复用 oracle 目录</td></tr>
 * </table>
 */
@Configuration
@ConfigurationProperties(prefix = "medkernel.flyway")
public class MedKernelFlywayProperties {

    /**
     * 是否启用 Flyway。
     * <p>默认 false 保持向后兼容；生产部署强制开启。
     */
    private boolean enabled = false;

    /**
     * 现有数据库已存在表时是否以「baseline」方式接入。
     * <p>true：跳过 {@code baselineVersion} 之前的 migration，避免对生产已存在的库重复 CREATE TABLE。
     */
    private boolean baselineOnMigrate = true;

    /**
     * baseline 版本号。
     * <p>对应「Flyway 接入前的初始库状态」。
     * <p>新版本号 > baselineVersion 的 migration 会被 apply。
     */
    private String baselineVersion = "0";

    /**
     * 是否允许 out-of-order migration（晚提交的低版本号也能 apply）。
     * <p>多分支并行开发期建议 true；生产稳定后改 false。
     */
    private boolean outOfOrder = false;

    /**
     * 是否在校验失败时清理（仅开发态可开）。
     * <p>生产强制 false（避免 misconfiguration 触发 drop 所有表）。
     */
    private boolean cleanDisabled = true;

    /**
     * Migration 表名（默认 flyway_schema_history）。
     */
    private String table = "flyway_schema_history";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isBaselineOnMigrate() { return baselineOnMigrate; }
    public void setBaselineOnMigrate(boolean baselineOnMigrate) { this.baselineOnMigrate = baselineOnMigrate; }

    public String getBaselineVersion() { return baselineVersion; }
    public void setBaselineVersion(String baselineVersion) { this.baselineVersion = baselineVersion; }

    public boolean isOutOfOrder() { return outOfOrder; }
    public void setOutOfOrder(boolean outOfOrder) { this.outOfOrder = outOfOrder; }

    public boolean isCleanDisabled() { return cleanDisabled; }
    public void setCleanDisabled(boolean cleanDisabled) { this.cleanDisabled = cleanDisabled; }

    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }
}
