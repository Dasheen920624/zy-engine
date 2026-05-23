package com.medkernel.platform.license;

import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.crypto.SmCryptoService;

/**
 * GA-EXT-13 · 离线许可证签发（内网医院无法联网激活的兜底）。
 * 业务流程：销售在外网生成 license 文件 → 医院信息科 USB 拷入 → 系统启动时校验。
 */
@RestController
@RequestMapping("/api/v1/platform/license/offline")
public class OfflineLicenseController {

    private final SmCryptoService crypto;

    public OfflineLicenseController(SmCryptoService crypto) {
        this.crypto = crypto;
    }

    @PostMapping("/issue")
    public Map<String, Object> issue(@RequestParam String hospitalId,
                                      @RequestParam String validUntil,
                                      @RequestParam(defaultValue = "enterprise") String tier) {
        String payload = hospitalId + "|" + validUntil + "|" + tier;
        String sigHex = bytesToHex(crypto.sm3(payload.getBytes()));
        return Map.of(
            "licenseId", "LIC-" + UUID.randomUUID().toString().substring(0, 8),
            "hospitalId", hospitalId,
            "validUntil", validUntil,
            "tier", tier,
            "issuedAt", java.time.Instant.now().toString(),
            "sm3Signature", sigHex,
            "fileFormat", "medkernel-license-v1"
        );
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "currentLicenseId", "LIC-A38291BC",
            "hospitalId", "main-hospital",
            "tier", "enterprise",
            "issuedAt", "2026-05-01",
            "validUntil", "2027-05-01",
            "daysRemaining", 343,
            "usage", Map.of(
                "physicians", 1283,
                "physiciansLimit", 2000,
                "apiCallsThisMonth", 4_283_000,
                "apiCallsLimit", 10_000_000
            )
        );
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
