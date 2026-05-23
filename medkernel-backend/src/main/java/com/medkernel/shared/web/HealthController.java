package com.medkernel.shared.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MedKernel v1.0 GA · 基础健康端点。
 * <p>真实 health 由 spring-boot-actuator 提供（/actuator/health）；
 * 本端点提供"产品宪法生效中"的极简心跳，供前端骨架校验后端是否启动。
 */
@RestController
@RequestMapping("/api/v1/system")
public class HealthController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
            "status", "ok",
            "product", "MedKernel",
            "version", "1.0.0-SNAPSHOT",
            "stage", "v1.0 GA Phase-1 skeleton",
            "timestamp", Instant.now().toString()
        );
    }
}
