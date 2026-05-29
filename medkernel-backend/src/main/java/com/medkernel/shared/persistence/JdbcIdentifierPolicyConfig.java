package com.medkernel.shared.persistence;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;

/**
 * Spring Data JDBC 标识符策略配置。
 *
 * <p>项目 Flyway 脚本统一使用未加双引号的表名与列名，依赖 Oracle、PostgreSQL、Kingbase、达梦等数据库
 * 自身的大小写折叠规则。Spring Data JDBC 默认强制加双引号会让 Oracle 把
 * {@code "user_role_assignment"} 解析为区分大小写的小写对象，和迁移脚本创建的对象不一致。
 */
@Configuration
public class JdbcIdentifierPolicyConfig {

    /**
     * 关闭 Spring Data JDBC 的强制双引号策略，保持运行时 SQL 与迁移脚本命名口径一致。
     *
     * @return Spring Bean 后置处理器
     */
    @Bean
    static BeanPostProcessor jdbcIdentifierPolicyPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof JdbcMappingContext mappingContext) {
                    mappingContext.setForceQuote(false);
                }
                return bean;
            }
        };
    }
}
