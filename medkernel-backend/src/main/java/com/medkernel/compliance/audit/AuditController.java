package com.medkernel.compliance.audit;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.crypto.SmCryptoService;
import com.medkernel.shared.observability.BusinessMetrics;

@RestController
@RequestMapping("/api/v1/compliance/audit")
public class AuditController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final SmCryptoService crypto;
    private final BusinessMetrics metrics;

    public AuditController(SmCryptoService crypto, BusinessMetrics metrics) {
        this.crypto = crypto;
        this.metrics = metrics;
    }

    @GetMapping("/events")
    public List<AuditEvent> events() {
        return List.of(
            new AuditEvent("1", "10:23:45.123", "张三", "发布规则 R-AB-024", "tr-83a9", "✓ 已验签"),
            new AuditEvent("2", "10:21:08.502", "李四", "查询患者 MPI-000123456", "tr-72c1", "✓ 已验签"),
            new AuditEvent("3", "10:18:55.991", "王医生", "采纳 CDSS 提醒 #4521", "tr-99ab", "✓ 已验签"),
            new AuditEvent("4", "10:15:32.001", "system", "国密密钥年度轮换", "tr-001f", "✓ 已验签 + TSA")
        );
    }

    @PostMapping("/snapshot")
    public AuditEvent snapshot(@RequestParam(defaultValue = "manual") String reason) {
        String now = LocalDateTime.now(ZoneId.systemDefault()).format(FMT);
        String trace = "tr-" + UUID.randomUUID().toString().substring(0, 4);
        String sigHex = crypto.sm3Hex(reason + Instant.now().toString());
        metrics.incAuditChainSigned();
        return new AuditEvent(
            UUID.randomUUID().toString(),
            now,
            "current-user",
            "导出审计快照（reason=" + reason + "）",
            trace,
            "✓ SM3 " + sigHex.substring(0, 12) + "…"
        );
    }
}
