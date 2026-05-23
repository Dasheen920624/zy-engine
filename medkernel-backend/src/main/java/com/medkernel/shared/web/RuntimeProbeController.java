package com.medkernel.shared.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-CORE-07 / W1-G4 闸门：Virtual Threads + Tomcat 10 + Hikari 5 运行时探针。
 *
 * <p>访问 {@code /api/v1/system/runtime} 返回 JVM 是否在 Virtual Thread 中处理 HTTP 请求。
 * 配合 docs/CONSTITUTION.md 性能基线，验证 Spring Boot 3.3 默认开启的
 * {@code spring.threads.virtual.enabled=true} 真正生效。
 */
@RestController
@RequestMapping("/api/v1/system")
public class RuntimeProbeController {

    @GetMapping("/runtime")
    public Map<String, Object> runtime() {
        Thread current = Thread.currentThread();
        return Map.of(
            "javaVersion", System.getProperty("java.version"),
            "javaVendor", System.getProperty("java.vendor"),
            "vmName", System.getProperty("java.vm.name"),
            "threadName", current.getName(),
            "isVirtualThread", current.isVirtual(),
            "availableProcessors", Runtime.getRuntime().availableProcessors(),
            "freeMemoryMb", Runtime.getRuntime().freeMemory() / 1024 / 1024,
            "totalMemoryMb", Runtime.getRuntime().totalMemory() / 1024 / 1024
        );
    }
}
