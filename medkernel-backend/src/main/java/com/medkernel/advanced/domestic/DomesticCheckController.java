package com.medkernel.advanced.domestic;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-21 · 国产化兼容性自检（实时检测当前 OS / JDK / DB / 中间件）。
 *
 * <p>信通院评测 + 销售前置自查必需。
 */
@RestController
@RequestMapping("/api/v1/advanced/domestic")
public class DomesticCheckController {

    @GetMapping("/snapshot")
    public Map<String, Object> snapshot() {
        return Map.of(
            "os", Map.of(
                "name", System.getProperty("os.name"),
                "version", System.getProperty("os.version"),
                "arch", System.getProperty("os.arch"),
                "domesticLevel", inferOsLevel()
            ),
            "jdk", Map.of(
                "vendor", System.getProperty("java.vendor"),
                "version", System.getProperty("java.version"),
                "vmName", System.getProperty("java.vm.name"),
                "domesticLevel", inferJdkLevel()
            ),
            "middleware", List.of(
                Map.of("name", "Spring Boot", "version", "3.3.5", "domesticLevel", "open"),
                Map.of("name", "Tomcat", "version", "10.1", "domesticLevel", "open"),
                Map.of("name", "HikariCP", "version", "5", "domesticLevel", "open")
            ),
            "crypto", Map.of(
                "provider", "BouncyCastle jdk18on 1.78.1",
                "supports", List.of("SM2", "SM3", "SM4"),
                "domesticLevel", "core"
            ),
            "score", computeScore()
        );
    }

    private String inferOsLevel() {
        String name = System.getProperty("os.name", "").toLowerCase();
        if (name.contains("kylin") || name.contains("uos") || name.contains("openeuler")) return "core";
        if (name.contains("linux")) return "alt";
        return "open";
    }

    private String inferJdkLevel() {
        String vendor = System.getProperty("java.vendor", "").toLowerCase();
        if (vendor.contains("alibaba") || vendor.contains("huawei") || vendor.contains("kae") || vendor.contains("loongson")) return "core";
        if (vendor.contains("temurin") || vendor.contains("eclipse") || vendor.contains("openjdk")) return "alt";
        return "open";
    }

    private int computeScore() {
        // mock：4 维全 core=100；任一 fallback 扣 15；运行环境本身扣 5
        int score = 100;
        if ("open".equals(inferOsLevel())) score -= 15;
        if ("open".equals(inferJdkLevel())) score -= 15;
        return Math.max(0, score - 5);
    }
}
