package com.medkernel.clinical.cdss;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.observability.BusinessMetrics;

@RestController
@RequestMapping("/api/v1/clinical/cdss")
public class CdssController {

    private static final List<CdssAlert> SEED = List.of(
        new CdssAlert("1", "张** · 氯吡格雷 + 阿司匹林联用警告", "ACS 指南 2024", 0.82, "closed", "心内 · 王医生"),
        new CdssAlert("2", "李** · 头孢曲松皮试缺失", "医嘱安全规则 R-AB-024", 0.45, "remediating", "急诊 · 刘医生"),
        new CdssAlert("3", "王** · 高血压未按时随访", "质控规则 Q-HBP-008", 0.91, "closed", "全科 · 张医生")
    );

    private final BusinessMetrics metrics;

    public CdssController(BusinessMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/alerts")
    public List<CdssAlert> alerts() {
        return SEED;
    }

    @PostMapping("/alerts/{id}/{decision}")
    public Map<String, Object> decide(@PathVariable String id, @PathVariable String decision,
                                       @RequestParam(required = false) String reason) {
        metrics.incCdssAlerts();
        return Map.of(
            "id", id,
            "decision", decision,
            "reason", reason != null ? reason : "",
            "recordedAt", java.time.Instant.now().toString()
        );
    }
}
