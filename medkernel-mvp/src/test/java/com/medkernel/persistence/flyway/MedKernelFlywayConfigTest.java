package com.medkernel.persistence.flyway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link MedKernelFlywayConfig#resolveVendor} 单测。
 *
 * <p>覆盖：dialect 字符串到 Flyway location vendor 的映射规则。
 * 这是 PR-FINAL-25 多方言支持的关键路径。
 */
class MedKernelFlywayConfigTest {

    @Test
    @DisplayName("h2 / local → h2")
    void h2Dialect() {
        assertEquals("h2", MedKernelFlywayConfig.resolveVendor("h2"));
        assertEquals("h2", MedKernelFlywayConfig.resolveVendor("local"));
        assertEquals("h2", MedKernelFlywayConfig.resolveVendor("local_h2"));
        assertEquals("h2", MedKernelFlywayConfig.resolveVendor("H2"));
    }

    @Test
    @DisplayName("postgres / pg → postgres")
    void postgresDialect() {
        assertEquals("postgres", MedKernelFlywayConfig.resolveVendor("postgres"));
        assertEquals("postgres", MedKernelFlywayConfig.resolveVendor("postgresql"));
        assertEquals("postgres", MedKernelFlywayConfig.resolveVendor("pg"));
    }

    @Test
    @DisplayName("kingbase / kingbasees → postgres （兼容映射）")
    void kingbaseDialect() {
        assertEquals("postgres", MedKernelFlywayConfig.resolveVendor("kingbase"));
        assertEquals("postgres", MedKernelFlywayConfig.resolveVendor("kingbasees"));
        assertEquals("postgres", MedKernelFlywayConfig.resolveVendor("KingbaseES"));
    }

    @Test
    @DisplayName("oracle → oracle")
    void oracleDialect() {
        assertEquals("oracle", MedKernelFlywayConfig.resolveVendor("oracle"));
        assertEquals("oracle", MedKernelFlywayConfig.resolveVendor("Oracle"));
    }

    @Test
    @DisplayName("dm / dameng → oracle （兼容映射）")
    void dmDialect() {
        assertEquals("oracle", MedKernelFlywayConfig.resolveVendor("dm"));
        assertEquals("oracle", MedKernelFlywayConfig.resolveVendor("dameng"));
        assertEquals("oracle", MedKernelFlywayConfig.resolveVendor("DM"));
    }

    @Test
    @DisplayName("未知 dialect / null → oracle （fail-safe 默认）")
    void unknownFallsBackToOracle() {
        assertEquals("oracle", MedKernelFlywayConfig.resolveVendor(null));
        assertEquals("oracle", MedKernelFlywayConfig.resolveVendor(""));
        assertEquals("oracle", MedKernelFlywayConfig.resolveVendor("mysql"));
        assertEquals("oracle", MedKernelFlywayConfig.resolveVendor("sqlserver"));
    }
}
