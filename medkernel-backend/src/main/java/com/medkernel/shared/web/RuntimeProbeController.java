package com.medkernel.shared.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;

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
    public ApiResult<RuntimeProbeResponse> runtime() {
        Thread current = Thread.currentThread();
        RuntimeProbeResponse body = new RuntimeProbeResponse(
            System.getProperty("java.version"),
            System.getProperty("java.vendor"),
            System.getProperty("java.vm.name"),
            current.getName(),
            current.isVirtual(),
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().freeMemory() / 1024 / 1024,
            Runtime.getRuntime().totalMemory() / 1024 / 1024
        );
        return ApiResult.ok(body);
    }
}
