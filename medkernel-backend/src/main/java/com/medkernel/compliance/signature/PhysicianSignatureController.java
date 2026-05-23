package com.medkernel.compliance.signature;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.crypto.SmCryptoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * GA-EXT-11 · 医师 CA 电子签名（SM2）。
 *
 * <p>电子病历应用管理规范要求：医师对病历、处方、医嘱的签名必须通过国家认可的 CA 提供。
 * 生产环境对接 BJCA / GDCA / 福建数字 CA 等第三方 CA。
 * 当前为本地 mock：用 SM2 现场生成密钥对完成签名（用于演示签验流程）。
 */
@RestController
@RequestMapping("/api/v1/compliance/signature")
public class PhysicianSignatureController {

    private final SmCryptoService crypto;
    private final Map<String, KeyPair> physicianKeyStore = new HashMap<>();

    public PhysicianSignatureController(SmCryptoService crypto) {
        this.crypto = crypto;
    }

    public record SignRequest(@NotBlank String physicianId, @NotBlank String document) {}
    public record SignResponse(
        String physicianId,
        String documentHashHex,
        String signatureBase64,
        String publicKeyBase64,
        String algorithm,
        String caProvider
    ) {}

    @PostMapping("/sign")
    public SignResponse sign(@Valid @RequestBody SignRequest req) throws Exception {
        KeyPair kp = physicianKeyStore.computeIfAbsent(req.physicianId(), k -> {
            try {
                return crypto.generateSm2KeyPair();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        byte[] docBytes = req.document().getBytes(StandardCharsets.UTF_8);
        byte[] hash = crypto.sm3(docBytes);
        // SM2 签名走 SM2 加密接口模拟（v1.0 GA 实装时切真实 Signature API）
        byte[] sig = crypto.sm2Encrypt(kp.getPublic(), hash);
        return new SignResponse(
            req.physicianId(),
            bytesToHex(hash),
            crypto.base64Encode(sig),
            crypto.base64Encode(kp.getPublic().getEncoded()),
            "SM2-with-SM3",
            "BJCA-Local-Mock"
        );
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
