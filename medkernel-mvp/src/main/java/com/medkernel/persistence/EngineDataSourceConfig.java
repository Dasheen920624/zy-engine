package com.medkernel.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * PR-FINAL-15：HikariCP DataSource 接入。
 *
 * 设计原则：
 *  1. 不依赖 Spring Boot DataSourceAutoConfiguration（项目用自定义 medkernel.database.*
 *     而非 spring.datasource.* 命名空间），因此显式构造 HikariDataSource。
 *  2. @ConditionalOnMissingBean 让单元测试可以注入 mock DataSource 覆盖。
 *  3. 由 Spring 容器管理 lifecycle（容器关闭时自动 dataSource.close() → 释放 pool）。
 *  4. medkernel.database.enabled=false（默认）时返回 NoOpDataSource 占位，避免应用启动失败；
 *     此时 PersistenceService.@PostConstruct initializeXxxSchema 通过 properties.isEnabled()
 *     早退，不会真的去拿 connection。
 *
 * 关键：所有 PersistenceService 通过注入 DataSource 拿连接，
 * 替代历史上 29 处散落的直连 JDBC 取连接调用。
 * 这使得：
 *  - @Transactional 真正生效（同 Service 内多 DAO 走同 connection）
 *  - 连接池监控可观测（pool name = MedKernelHikari）
 *  - 连接泄漏检测（leak-detection-threshold-ms = 2s）
 *  - 高并发下不再无限制创建连接（max-pool-size = 20，可配置）
 *
 * 配套约束（verify-pr.ps1 守门）：
 *  - 任何新代码不允许自行绕过 DataSource 取连接
 *  - 任何新代码不允许重复实现连接重试循环
 *  - 例外：本文件自身
 */
@Configuration
public class EngineDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(EngineDataSourceConfig.class);

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource(EnginePersistenceProperties properties) {
        if (!properties.isEnabled()) {
            // DB 未启用时使用 H2 内存库占位，避免连接外部数据库失败导致启动中断
            log.warn("medkernel.database.enabled=false → using H2 in-memory DataSource as placeholder");
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:mem:medkernel-noop;DB_CLOSE_DELAY=-1");
            config.setUsername("sa");
            config.setPassword("");
            config.setPoolName("MedKernelNoOpH2");
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(0);
            return new HikariDataSource(config);
        }
        EnginePersistenceProperties.HikariOptions opts = properties.getHikari();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        // 本地 H2 文件库通常无密码；其它 dialect 必须有密码（生产 Oracle/DM/PG 强制）
        if (properties.hasPassword()) {
            config.setPassword(properties.getPassword());
        }
        // pool 调优
        config.setMaximumPoolSize(opts.getMaximumPoolSize());
        config.setMinimumIdle(opts.getMinimumIdle());
        config.setConnectionTimeout(opts.getConnectionTimeoutMs());
        config.setIdleTimeout(opts.getIdleTimeoutMs());
        config.setMaxLifetime(opts.getMaxLifetimeMs());
        config.setLeakDetectionThreshold(opts.getLeakDetectionThresholdMs());
        config.setPoolName(opts.getPoolName());

        // 国产化兼容：H2 file 库的初始化 SQL 必须按 schema 分次执行，
        // 因此不在 HikariCP 层做 init-sql；schema 初始化仍由各 PersistenceService 的
        // @PostConstruct loadSchemaStatements(...) 负责（保持职责单一）。

        HikariDataSource dataSource = new HikariDataSource(config);
        log.info("HikariCP DataSource initialized: provider={} role={} pool={} max={}",
                properties.providerName(), properties.roleName(),
                opts.getPoolName(), opts.getMaximumPoolSize());
        return dataSource;
    }
}
