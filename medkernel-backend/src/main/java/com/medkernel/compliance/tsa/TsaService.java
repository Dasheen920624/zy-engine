package com.medkernel.compliance.tsa;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.medkernel.shared.crypto.SmCryptoService;

/**
 * GA-EXT-08 · 可信时间戳服务（TSA + 国密 CA）。
 *
 * <p>生产环境对接 BJCA / GDCA / 上海 CA / 第三方 TSA 公共服务（GM/T 0033）。
 * 当前为本地 mock：用 SM3 + 内部时间戳模拟 RFC 3161 TSA 响应。
 *
 * <p>合规依据：
 * - GB/T 38540-2020 信息安全技术 安全电子签章密码技术规范
 * - GM/T 0033-2014 时间戳接口规范
 * - 国家电子病历应用管理规范（必须有 TSA）
 */
@Service
public class TsaService {

    private final SmCryptoService crypto;

    @Value("${medkernel.tsa.provider:bjca-local-mock}")
    private String provider;

    public TsaService(SmCryptoService crypto) {
        this.crypto = crypto;
    }

    /**
     * 对任意数据生成 TSA 时间戳（mock）。
     * 真实情况下返回的是 RFC 3161 TSP 响应 + X.509 证书链。
     */
    public TsaToken stamp(byte[] payload) {
        Instant now = Instant.now();
        byte[] sm3 = crypto.sm3(payload);
        String hex = bytesToHex(sm3);
        String serial = "TSA-" + UUID.randomUUID().toString().substring(0, 8);
        return new TsaToken(serial, now.toString(), hex, provider);
    }

    public TsaToken stamp(String text) {
        return stamp(text.getBytes(StandardCharsets.UTF_8));
    }

    public record TsaToken(String serial, String timestamp, String sm3HashHex, String provider) {}

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
