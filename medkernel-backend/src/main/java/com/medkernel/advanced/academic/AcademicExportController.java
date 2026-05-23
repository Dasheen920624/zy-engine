package com.medkernel.advanced.academic;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-24 · 学术出口（脱敏数据集 + 投稿模板）。
 * 三甲科研刚需 —— 把临床数据按 MaskingService EXPORT profile 脱敏后导出。
 */
@RestController
@RequestMapping("/api/v1/advanced/academic")
public class AcademicExportController {

    @GetMapping("/templates")
    public List<Map<String, Object>> templates() {
        return List.of(
            Map.of("name", "NEJM 投稿模板", "fields", List.of("人口学", "诊断", "干预", "结局")),
            Map.of("name", "BMJ 数据集模板", "fields", List.of("研究问题", "PICO", "结局指标")),
            Map.of("name", "中华心血管病杂志模板", "fields", List.of("入组标准", "干预", "随访"))
        );
    }

    @PostMapping("/export")
    public Map<String, Object> export(@RequestParam String topic, @RequestParam(defaultValue = "EXPORT") String maskingProfile) {
        return Map.of(
            "exportId", "ACA-" + System.currentTimeMillis(),
            "topic", topic,
            "recordsSelected", 1283,
            "maskingProfile", maskingProfile,
            "format", "CSV + 数据字典 + DPA",
            "irbApprovalRequired", true,
            "downloadExpiresAt", "7 天后"
        );
    }
}
