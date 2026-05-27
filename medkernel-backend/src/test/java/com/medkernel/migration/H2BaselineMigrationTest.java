package com.medkernel.migration;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 不依赖 Docker 的 H2 Flyway smoke。
 *
 * <p>与 {@link FlywayMultiDialectSmokeTest} 区分：后者用 Testcontainers 起 postgres/oracle，必须有 Docker；
 * 本测试只跑 H2，本地或无 Docker 的 CI 也能跑，确保 baseline 迁移在 H2 方言下健全。
 */
class H2BaselineMigrationTest {

    @Test
    void h2AppliesCompleteAuthoritativeBaselineMigrations() {
        DataSource ds = new HikariDataSource(hikari());
        Flyway flyway = Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration/h2")
            .baselineOnMigrate(true)
            .load();

        var result = flyway.migrate();
        assertThat(result.success).as("H2 baseline migrations succeed").isTrue();
        assertThat(result.migrationsExecuted).as("V1 至 V19 全部应用").isEqualTo(19);

        var applied = flyway.info().applied();
        assertThat(applied).extracting(info -> info.getVersion().getVersion())
            .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19");
    }

    private HikariConfig hikari() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:flyway-h2-smoke-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setDriverClassName("org.h2.Driver");
        cfg.setMaximumPoolSize(2);
        return cfg;
    }
}
