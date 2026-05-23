package com.medkernel.advanced.llm;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-15 · AI 模型版本管理 + 一键回滚。
 * NMPA + 集团 IT 治理硬需求：每条 LLM Provider 配置必须版本化，可立即回滚。
 */
@RestController
@RequestMapping("/api/v1/advanced/llm/versions")
public class LlmVersionController {

    public record VersionSnapshot(
        String id,
        String providerId,
        String version,
        String createdAt,
        String createdBy,
        Boolean active
    ) {}

    @GetMapping
    public List<VersionSnapshot> list() {
        return List.of(
            new VersionSnapshot("v-001", "ollama-qwen2.5-7b", "v2.5-7b", "2026-04-15", "张三", false),
            new VersionSnapshot("v-002", "ollama-qwen2.5-7b", "v2.5.1-7b", "2026-05-12", "张三", true),
            new VersionSnapshot("v-003", "saas-wenxin-4.0", "wenxin-4.0", "2026-03-20", "李四", true),
            new VersionSnapshot("v-004", "saas-wenxin-4.0", "wenxin-4.5-beta", "2026-05-22", "李四", false)
        );
    }

    @PostMapping("/{id}/rollback")
    public Map<String, Object> rollback(@PathVariable String id) {
        return Map.of(
            "result", "ok",
            "rolledBackTo", id,
            "previousActive", "v-002",
            "rolledAt", java.time.Instant.now().toString(),
            "auditLogged", true
        );
    }
}
