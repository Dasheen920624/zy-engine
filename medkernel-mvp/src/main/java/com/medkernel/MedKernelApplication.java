package com.medkernel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;

/**
 * MedKernel 启动入口。
 *
 * <p>PR-FINAL-25：排除 Spring Boot 默认的 FlywayAutoConfiguration。
 * 它会用默认 locations {@code classpath:db/migration} 扫描所有子目录，
 * 导致我们按 vendor 分目录的 V1 出现重复版本号冲突。
 * Flyway 改由 {@code com.medkernel.persistence.flyway.MedKernelFlywayConfig} 显式管理，
 * 仅在 {@code medkernel.flyway.enabled=true} 时启用。
 */
@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
public class MedKernelApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedKernelApplication.class, args);
    }
}

