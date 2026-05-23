package com.medkernel.compliance.waf;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-18 · WAF / IPS 攻击溯源接入（等保 2.0 三级）。
 */
@RestController
@RequestMapping("/api/v1/compliance/waf")
public class WafController {

    public record AttackEvent(String id, String time, String sourceIp, String attackType, String target, String severity, String status) {}

    @GetMapping("/events")
    public List<AttackEvent> events() {
        return List.of(
            new AttackEvent("e-001", "10:23", "8.8.8.8", "SQL Injection", "/api/v1/clinical/mpi", "high", "blocked"),
            new AttackEvent("e-002", "10:18", "192.168.1.99", "Brute Force Login", "/login", "medium", "rate-limited"),
            new AttackEvent("e-003", "09:45", "1.2.3.4", "XSS attempt", "/admin/audit", "high", "blocked"),
            new AttackEvent("e-004", "08:12", "10.0.0.5", "Path Traversal", "/api/v1/configpack/", "high", "blocked")
        );
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of(
            "totalEvents24h", 1247,
            "blocked", 1238,
            "rateLimited", 9,
            "passedDownstream", 0,
            "topSrcCountry", "ZH",
            "topAttackType", "SQL Injection",
            "blacklistSize", 4823
        );
    }
}
