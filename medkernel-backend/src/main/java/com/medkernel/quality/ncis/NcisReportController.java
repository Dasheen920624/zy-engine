package com.medkernel.quality.ncis;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-05 · NCIS 国家医疗质量管理与控制信息平台 数据上报适配。
 * 国家卫健委要求公立医院定期上报，是年度考核硬指标。
 */
@RestController
@RequestMapping("/api/v1/quality/ncis")
public class NcisReportController {

    public record NcisJob(String id, String type, String period, String status, Integer recordCount) {}

    @GetMapping("/jobs")
    public List<NcisJob> jobs() {
        return List.of(
            new NcisJob("j-001", "DRG 月报", "2026-07", "submitted", 12431),
            new NcisJob("j-002", "VTE 季报", "2026-Q2", "submitted", 4282),
            new NcisJob("j-003", "DRG 月报", "2026-08", "draft", 12834),
            new NcisJob("j-004", "医院感染年报", "2026", "draft", 178)
        );
    }

    @PostMapping("/jobs/{type}/submit")
    public Map<String, Object> submit(@org.springframework.web.bind.annotation.PathVariable String type) {
        return Map.of(
            "type", type,
            "result", "ok",
            "trackingId", "NCIS-" + UUID.randomUUID().toString().substring(0, 8),
            "channel", "国家卫健委医疗质量管理与控制信息平台"
        );
    }
}
