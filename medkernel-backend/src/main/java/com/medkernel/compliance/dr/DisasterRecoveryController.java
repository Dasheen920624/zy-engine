package com.medkernel.compliance.dr;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-16 + GA-EXT-17 · 多机房灾备 + 同城双活 + 异地容灾。
 * SLA 99.9% 必备：RPO ≤ 1h, RTO ≤ 4h。
 */
@RestController
@RequestMapping("/api/v1/compliance/dr")
public class DisasterRecoveryController {

    public record DrSite(String role, String city, String dc, String rpo, String rto, String status, String lastDrill) {}

    @GetMapping("/sites")
    public List<DrSite> sites() {
        return List.of(
            new DrSite("primary", "北京", "北京-1（主）", "real-time", "—", "active", "—"),
            new DrSite("active-standby", "北京", "北京-2（同城双活）", "≤ 5s", "≤ 30s", "active", "2026-04-12"),
            new DrSite("dr", "上海", "上海-1（异地灾备）", "≤ 1h", "≤ 4h", "standby", "2026-05-15")
        );
    }

    @PostMapping("/drill/start")
    public Map<String, Object> startDrill() {
        return Map.of(
            "drillId", "DRILL-2026-08-001",
            "type", "异地灾备切换演练",
            "estimatedDuration", "2 小时",
            "currentStage", "primary → dr 数据 lag 校验中",
            "rpoActual", "0.4h",
            "rtoEstimated", "2.5h"
        );
    }
}
