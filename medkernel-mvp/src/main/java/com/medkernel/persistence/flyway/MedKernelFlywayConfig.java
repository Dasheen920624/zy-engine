package com.medkernel.persistence.flyway;

import javax.sql.DataSource;

import com.medkernel.persistence.EnginePersistenceProperties;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * PR-FINAL-25：Flyway DB migration 接入。
 *
 * <p>当 {@code medkernel.flyway.enabled=true} 时启用，启动时执行 migration。
 * 配置 locations 根据 {@link EnginePersistenceProperties#getDialect()} 动态选取：
 *
 * <pre>
 *   classpath:db/migration/common      // 跨方言（极少用，复杂场景见各 vendor）
 *   classpath:db/migration/{vendor}    // vendor 专属，按 dialect 选取
 * </pre>
 *
 * <h2>顺序保证</h2>
 *
 * <p>Flyway 的 {@code Bean} 通过 {@code @DependsOn("dataSource")} 保证在 DataSource 之后；
 * 但需要在 PersistenceService.@PostConstruct 之前执行。
 * Spring Boot 默认行为：Bean 初始化时如果带 InitializingBean / @PostConstruct，
 * 顺序由依赖图决定。这里 {@link Flyway#migrate()} 在 Bean 初始化时调用。
 * PersistenceService 通过 {@code @Autowired Flyway} 间接依赖（即便不直接用 Flyway 实例，
 * 也通过这里发布的 {@link MigrationLifecycleAdvisor} 保证启动顺序）。
 *
 * <h2>"实测 KingbaseES"</h2>
 *
 * <p>KingbaseES 完全 PostgreSQL 兼容（JDBC URL {@code jdbc:postgresql} 协议直接走），
 * Flyway 的 PostgreSQL provider 即可处理。dialect 值传 {@code kingbase} 或
 * {@code kingbasees}，本配置自动映射到 {@code db/migration/postgres} 目录。
 *
 * @see MedKernelFlywayProperties
 */
@Configuration
@ConditionalOnProperty(prefix = "medkernel.flyway", name = "enabled", havingValue = "true")
public class MedKernelFlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(MedKernelFlywayConfig.class);

    private static final String COMMON_LOCATION = "classpath:db/migration/common";
    private static final String LOCATION_PREFIX = "classpath:db/migration/";

    @Bean(initMethod = "migrate")
    @DependsOn("dataSource")
    public Flyway flyway(DataSource dataSource,
                          EnginePersistenceProperties engineProperties,
                          MedKernelFlywayProperties flywayProperties) {
        String vendor = resolveVendor(engineProperties.getDialect());
        String[] locations = new String[] {
                COMMON_LOCATION,
                LOCATION_PREFIX + vendor
        };

        FluentConfiguration config = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .table(flywayProperties.getTable())
                .baselineOnMigrate(flywayProperties.isBaselineOnMigrate())
                .baselineVersion(MigrationVersion.fromVersion(flywayProperties.getBaselineVersion()))
                .outOfOrder(flywayProperties.isOutOfOrder())
                .cleanDisabled(flywayProperties.isCleanDisabled())
                .placeholders(java.util.Collections.emptyMap());

        Flyway flyway = config.load();
        log.info("Flyway 已就绪：vendor={} dialect={} locations={} table={} baseline-on-migrate={}",
                vendor, engineProperties.getDialect(),
                java.util.Arrays.toString(locations),
                flywayProperties.getTable(),
                flywayProperties.isBaselineOnMigrate());
        return flyway;
    }

    /**
     * Migration 生命周期 advisor — 启动时打印结果。
     *
     * <p>独立 Bean 让运维可以通过 actuator / log 看到具体 migration 数量。
     */
    @Bean
    public MigrationLifecycleAdvisor flywayLifecycleAdvisor(Flyway flyway) {
        MigrateResult result = flyway.info().applied().length > 0
                ? null
                : null; // Flyway.info() 在 Bean 注入时已经反映 migration 后状态
        int applied = flyway.info().applied().length;
        int pending = flyway.info().pending().length;
        log.info("Flyway 状态：已应用={} 待应用={}", applied, pending);
        return new MigrationLifecycleAdvisor(applied, pending);
    }

    /**
     * 把 dialect 字符串映射为 Flyway location vendor 目录名。
     *
     * @param dialect medkernel.database.dialect 值（h2 / oracle / postgres / dm / kingbase / ...）
     * @return location 目录名（h2 / oracle / postgres）
     */
    static String resolveVendor(String dialect) {
        String value = dialect == null ? "" : dialect.trim().toLowerCase();
        switch (value) {
            case "h2":
            case "local":
            case "local_h2":
                return "h2";
            case "postgres":
            case "postgresql":
            case "pg":
            case "kingbase":
            case "kingbasees":
                return "postgres";
            case "dm":
            case "dameng":
            case "oracle":
            default:
                return "oracle";
        }
    }

    /** 仅供启动时记录 migration 数量。生产可被 actuator 端点读取。 */
    public static final class MigrationLifecycleAdvisor {
        private final int applied;
        private final int pending;

        public MigrationLifecycleAdvisor(int applied, int pending) {
            this.applied = applied;
            this.pending = pending;
        }

        public int getApplied() { return applied; }
        public int getPending() { return pending; }
    }
}
