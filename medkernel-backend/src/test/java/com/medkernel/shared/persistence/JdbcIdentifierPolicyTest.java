package com.medkernel.shared.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Data JDBC 标识符策略合同测试。
 *
 * <p>Flyway 迁移脚本统一使用未加双引号的表名与列名，由数据库方言自行折叠大小写；
 * 因此运行时 SQL 也必须关闭强制双引号，避免 Oracle 将 {@code "user_role_assignment"}
 * 解析成不存在的小写对象。
 */
@SpringBootTest
@ActiveProfiles("test")
class JdbcIdentifierPolicyTest {

    @Autowired
    JdbcMappingContext mappingContext;

    @Test
    void jdbcMappingShouldUseUnquotedIdentifiersCompatibleWithFlywayScripts() {
        assertThat(mappingContext.isForceQuote())
            .as("运行时 SQL 标识符必须与 Flyway 未加引号命名策略一致")
            .isFalse();
    }
}
