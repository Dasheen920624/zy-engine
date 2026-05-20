package com.medkernel.patientindex.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MPI 哈希和脱敏工具类
 */
public class MpiHashUtil {
    
    private static final String HASH_ALGORITHM = "SHA-256";
    
    /**
     * 计算字符串的 SHA-256 哈希值
     */
    public static String hash(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * 脱敏姓名（保留姓，名用*代替）
     * 例：张三 -> 张*
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
    
    /**
     * 脱敏身份证号（保留前3位和后4位）
     * 例：110101199001011234 -> 110***1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 7) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***" + idCard.substring(idCard.length() - 4);
    }
    
    /**
     * 脱敏手机号（保留前3位和后4位）
     * 例：13812345678 -> 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
