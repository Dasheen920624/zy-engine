package com.medkernel.shared.observability;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * payload 存储装配。
 *
 * <p>默认注入 {@link InMemoryPayloadStorage}（dev/test/OBS-01 单独验收）；
 * 第三层 API-02 引入 {@code DbPayloadStorage} bean 时通过 {@code @ConditionalOnMissingBean}
 * 自动让位（API-02 那边声明 @Primary 即可覆盖）。
 */
@Configuration
public class PayloadStorageConfig {

    @Bean
    @ConditionalOnMissingBean(PayloadStoragePort.class)
    public PayloadStoragePort inMemoryPayloadStorage() {
        return new InMemoryPayloadStorage();
    }
}
