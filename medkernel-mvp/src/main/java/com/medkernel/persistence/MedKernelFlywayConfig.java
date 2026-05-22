package com.medkernel.persistence;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * PR-FINAL-25：显式 Flyway 配置（零 baseline 设计）。
 *
 * <p>为何不用 Spring Boot {@code FlywayAutoConfiguration}：
 * <ul>
 *   <li>项目 {@link EngineDataSourceConfig} 用 {@code @ConditionalOnMissingBean(DataSource.class)}
 *       提供自定义 DataSource Bean，不走 Spring Boot {@code DataSourceAutoConfiguration}（项目用
 *       {@code medkernel.database.*} 命名空间而非 {@code spring.datasource.*}）。</li>
 *   <li>FlywayAutoConfiguration 用 {@code @AutoConfigureAfter(DataSourceAutoConfiguration.class)}，
 *       且 FlywayDataSourceCondition 要求标准的 spring.datasource.url 或 DataSource Bean 已就绪；
 *       项目场景下 autoconfig 链不完整，Flyway 不会被激活。</li>
 *   <li>显式构造 Flyway Bean + {@code initMethod="migrate"} 在 Bean 初始化时立即 migrate，
 *       保证所有 {@code @Service} 的 {@code @PostConstruct rebuildFromPersistence()} 之前 schema 就位。</li>
 * </ul>
 *
 * <p>详见 docs/engineering/adr/0014-flyway-zero-baseline.md。
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MedKernelFlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(MedKernelFlywayConfig.class);

    /**
     * 构造 Flyway Bean，在 init 阶段调用 {@code migrate()}。
     *
     * <p>Spring Bean lifecycle 中 {@code @Bean(initMethod="migrate")} 在 setter / autowired 之后、
     * 其它 {@code @PostConstruct} 之前执行。配合 {@code FlywaySchemaInitializer}（spring-aware 的依赖钩子）
     * 让所有 {@code @Service} 在 Flyway migrate 完成之后才初始化。
     */
    @Bean(name = "flyway", initMethod = "migrate")
    public Flyway flyway(DataSource dataSource, EnginePersistenceProperties properties) {
        String dialect = dialectFolder(properties);
        String location = "classpath:db/migration/" + dialect;
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table("medkernel_schema_history")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();
        log.info("[flyway] configured: dialect={} location={} table=medkernel_schema_history",
                dialect, location);
        return flyway;
    }

    /**
     * 把 EnginePersistenceProperties.providerName() 映射为 db/migration/<dialect> 子目录。
     */
    private String dialectFolder(EnginePersistenceProperties props) {
        String provider = props.providerName();
        if ("POSTGRESQL".equals(provider)) return "postgres";
        if ("DM".equals(provider)) return "dm";
        if ("KINGBASE".equals(provider)) return "kingbase";
        if ("LOCAL_H2_FILE".equals(provider)) return "h2";
        return "oracle";
    }
}
