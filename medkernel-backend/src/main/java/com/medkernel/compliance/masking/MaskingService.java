package com.medkernel.compliance.masking;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

import org.springframework.stereotype.Service;

import com.medkernel.shared.crypto.SmCryptoService;

/**
 * GA-EXT-10 · 数据脱敏中心。
 *
 * <p>统一入口：MaskingService.maskRecord(record, profile)
 * 根据 profile（DEV / TEST / TRAINING / EXPORT）应用对应脱敏强度。
 */
@Service
public class MaskingService {

    private final SmCryptoService crypto;

    public MaskingService(SmCryptoService crypto) {
        this.crypto = crypto;
    }

    public Map<String, String> maskRecord(Map<String, String> raw, MaskingProfile profile) {
        Map<String, String> out = new HashMap<>();
        raw.forEach((k, v) -> out.put(k, maskField(k, v, profile)));
        return out;
    }

    public String maskField(String fieldName, String value, MaskingProfile profile) {
        if (value == null) return null;
        return switch (fieldName) {
            case "idCard", "id_card" -> maskIdCard(value, profile);
            case "phone", "mobile" -> maskPhone(value, profile);
            case "name", "patientName" -> maskName(value, profile);
            case "address" -> maskAddress(value, profile);
            case "mrn", "patientId" -> maskMrn(value, profile);
            default -> value;
        };
    }

    private String maskIdCard(String v, MaskingProfile profile) {
        if (v.length() < 8) return "***";
        return switch (profile) {
            case DEV, TEST, TRAINING -> v.substring(0, 4) + "**********" + v.substring(v.length() - 4);
            case EXPORT -> pseudonymize("id:" + v);
        };
    }

    private String maskPhone(String v, MaskingProfile profile) {
        if (v.length() < 7) return "***";
        return switch (profile) {
            case DEV, TEST, TRAINING -> v.substring(0, 3) + "****" + v.substring(v.length() - 4);
            case EXPORT -> pseudonymize("ph:" + v);
        };
    }

    private String maskName(String v, MaskingProfile profile) {
        return switch (profile) {
            case DEV -> v.length() > 0 ? v.charAt(0) + "**" : "***";
            case TEST -> "***";
            case TRAINING -> "测试患者-" + (Math.abs(v.hashCode()) % 1000);
            case EXPORT -> pseudonymize("nm:" + v);
        };
    }

    private String maskAddress(String v, MaskingProfile profile) {
        return switch (profile) {
            case DEV, TEST, TRAINING -> v.length() > 6 ? v.substring(0, 6) + "***" : "***";
            case EXPORT -> "***";
        };
    }

    private String maskMrn(String v, MaskingProfile profile) {
        return profile == MaskingProfile.EXPORT ? pseudonymize("mrn:" + v) : v;
    }

    private String pseudonymize(String input) {
        byte[] hash = crypto.sm3(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder("p-");
        for (int i = 0; i < 8; i++) sb.append(String.format("%02x", hash[i] & 0xff));
        return sb.toString();
    }
}
