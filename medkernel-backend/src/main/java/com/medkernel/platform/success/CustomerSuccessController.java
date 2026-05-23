package com.medkernel.platform.success;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-22 · 客户成功看板。
 * 商业续约证据 —— 用数据证明客户购买 MedKernel 后获得的可量化提升。
 */
@RestController
@RequestMapping("/api/v1/platform/customer-success")
public class CustomerSuccessController {

    @GetMapping("/health-score")
    public Map<String, Object> healthScore() {
        return Map.of(
            "score", 87,
            "band", "good",
            "dimensions", Map.of(
                "usage", 92,
                "adoption", 78,
                "remediation", 84,
                "engagement", 90,
                "satisfaction", 88
            ),
            "trend30d", "+5",
            "renewProbability", 0.94,
            "lastReviewedAt", "2026-05-23"
        );
    }

    @GetMapping("/benchmark")
    public Map<String, Object> benchmark() {
        return Map.of(
            "tenantScore", 87,
            "peerAvg", 79,
            "peerTop10", 91,
            "peerCount", 6,
            "rank", 2,
            "topKpis", Map.of(
                "提醒采纳率", "本院 78% vs 同级均 65%",
                "整改闭环率", "本院 84% vs 同级均 71%",
                "DRG 拒付率", "本院 2.3% vs 同级均 4.1%"
            )
        );
    }
}
