package com.medkernel.persistence.flyway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Flyway 集成测试：H2 内存库实测 V1/V2 migration。
 *
 * <p>验证：
 * <ul>
 *   <li>V1__baseline_flyway.sql 可成功 apply</li>
 *   <li>V2__pr_final_23_widen_md_patient_encrypted_columns.sql 修改列宽生效</li>
 *   <li>flyway_schema_history 表正确记录两条 migration</li>
 * </ul>
 *
 * <p>不引入 SpringBootTest，直接构造 Flyway，保持快速（< 1 秒）。
 */
class FlywayMigrationIntegrationTest {

    private HikariDataSource dataSource;

    @BeforeEach
    void initDataSource() {
        HikariConfig config = new HikariConfig();
        // 每个测试用独立 H2 内存库避免脏数据互染
        config.setJdbcUrl("jdbc:h2:mem:flyway_test_" + System.nanoTime()
                + ";MODE=Oracle;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(2);
        dataSource = new HikariDataSource(config);
    }

    @AfterEach
    void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName("Flyway H2 vendor — V1 + V2 全部 apply")
    void flywayMigratesAllH2Versions() throws Exception {
        // 预先创建 md_patient（模拟现有 PersistenceService 已建表场景）
        seedMdPatientTable(dataSource);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/common", "classpath:db/migration/h2")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        int applied = flyway.migrate().migrationsExecuted;

        // 预期 V1 + V2 都 apply（baseline=0 之后所有版本）
        assertTrue(applied >= 2, "至少 apply V1 + V2 两条 migration，实际：" + applied);

        // 验证列宽已扩展
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "MD_PATIENT", "PATIENT_NAME")) {
                assertTrue(rs.next(), "md_patient 表应存在");
                int size = rs.getInt("COLUMN_SIZE");
                assertEquals(256, size, "patient_name 列宽应已扩展到 256");
            }
            try (ResultSet rs = meta.getColumns(null, null, "MD_PATIENT", "ADDRESS")) {
                assertTrue(rs.next());
                int size = rs.getInt("COLUMN_SIZE");
                assertEquals(1024, size, "address 列宽应已扩展到 1024");
            }
        }
    }

    @Test
    @DisplayName("Flyway info — applied 数量与 migrate() 返回值一致")
    void flywayInfoConsistent() throws Exception {
        seedMdPatientTable(dataSource);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/common", "classpath:db/migration/h2")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        flyway.migrate();

        int applied = flyway.info().applied().length;
        int pending = flyway.info().pending().length;

        assertTrue(applied >= 2);
        assertEquals(0, pending, "全部 migration 应已 apply");
    }

    /** 创建初始 md_patient 表，模拟生产已存在的库（baselineOnMigrate=true 场景）。 */
    private static void seedMdPatientTable(DataSource ds) throws Exception {
        try (Connection conn = ds.getConnection();
             java.sql.Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS md_patient ("
                    + "id BIGINT PRIMARY KEY, "
                    + "tenant_id VARCHAR(64) NOT NULL, "
                    + "patient_id VARCHAR(64) NOT NULL, "
                    + "patient_name VARCHAR(100) NOT NULL, "
                    + "gender VARCHAR(10), "
                    + "birth_date DATE, "
                    + "id_card_no VARCHAR(18), "
                    + "phone VARCHAR(20), "
                    + "address VARCHAR(500), "
                    + "status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', "
                    + "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, "
                    + "updated_time TIMESTAMP"
                    + ")");
        }
    }
}
