package com.medkernel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * API Key 管理服务。
 * <p>
 * SEC-003: 提供 API Key 的创建、查询、吊销和验证功能。
 * 当前使用内存存储，生产环境应迁移到数据库。
 */
@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

    private final ConcurrentHashMap<String, ApiKey> keyStore = new ConcurrentHashMap<String, ApiKey>();
    private final NonceService nonceService;

    public ApiKeyService() {
        this.nonceService = new NonceService();
    }

    @PostConstruct
    void init() {
        log.info("[SEC-003] ApiKeyService initialized, in-memory key store active");
    }

    /**
     * 创建新的 API Key。
     */
    public ApiKey createKey(String name, String tenantId, String role, String createdBy, LocalDateTime expiresAt) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("API Key name is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        ApiKey apiKey = ApiKey.generate(name, tenantId, role, createdBy, expiresAt);
        keyStore.put(apiKey.getId(), apiKey);
        log.info("[SEC-003] API Key created: name={}, tenant={}, prefix={}", name, tenantId, apiKey.getKeyPrefix());
        return apiKey;
    }

    /**
     * 列出所有 API Key（脱敏）。
     */
    public List<Map<String, Object>> listKeys(String tenantId) {
        return keyStore.values().stream()
                .filter(k -> tenantId == null || tenantId.equals(k.getTenantId()))
                .map(ApiKey::toMap)
                .collect(Collectors.toList());
    }

    /**
     * 吊销 API Key。
     */
    public boolean revokeKey(String keyId, String tenantId) {
        ApiKey apiKey = keyStore.get(keyId);
        if (apiKey == null) {
            return false;
        }
        if (tenantId != null && !tenantId.equals(apiKey.getTenantId())) {
            return false;
        }
        apiKey.setActive(false);
        log.info("[SEC-003] API Key revoked: id={}, prefix={}", keyId, apiKey.getKeyPrefix());
        return true;
    }

    /**
     * 根据 key 前缀查找 API Key。
     */
    public ApiKey findByKeyPrefix(String prefix) {
        return keyStore.values().stream()
                .filter(k -> prefix.equals(k.getKeyPrefix()) && k.isActive() && !k.isExpired())
                .findFirst()
                .orElse(null);
    }

    /**
     * 验证 API Key 并返回对应的 Key 对象。
     *
     * @param rawKey 原始 API Key（mk_xxx）
     * @return ApiKey 如果验证通过，否则 null
     */
    public ApiKey validateKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith("mk_")) {
            return null;
        }
        String prefix = rawKey.substring(0, 8);
        ApiKey apiKey = findByKeyPrefix(prefix);
        if (apiKey == null) {
            return null;
        }
        // 验证 key hash
        String hash = sha256(rawKey);
        if (!hash.equals(apiKey.getKeyHash())) {
            return null;
        }
        // 更新最后使用时间
        apiKey.setLastUsedAt(LocalDateTime.now());
        return apiKey;
    }

    /**
     * 验证 HMAC-SHA256 签名。
     * <p>
     * 签名计算方式：HMAC-SHA256(secret, timestamp + nonce + method + path + bodyHash)
     *
     * @param apiKey    API Key 对象
     * @param signature 请求中的签名
     * @param timestamp 请求时间戳
     * @param nonce     请求 nonce
     * @param method    HTTP 方法
     * @param path      请求路径
     * @param bodyHash  请求体 SHA-256 哈希（可为空）
     * @return true 如果签名验证通过
     */
    public boolean verifySignature(ApiKey apiKey, String signature, long timestamp, String nonce,
                                   String method, String path, String bodyHash) {
        if (signature == null || apiKey.getSecret() == null) {
            return false;
        }

        // 验证 nonce
        if (!nonceService.validateAndRecord(nonce, timestamp)) {
            log.warn("[SEC-003] Nonce validation failed: nonce={}, timestamp={}", nonce, timestamp);
            return false;
        }

        // 构造签名字符串
        String signPayload = timestamp + nonce + method.toUpperCase() + path;
        if (bodyHash != null && !bodyHash.isEmpty()) {
            signPayload += bodyHash;
        }

        // 计算 HMAC-SHA256
        String expected = hmacSha256(apiKey.getSecret(), signPayload);
        boolean valid = expected.equals(signature);

        if (!valid) {
            log.warn("[SEC-003] Signature verification failed for key prefix={}", apiKey.getKeyPrefix());
        }
        return valid;
    }

    /**
     * 获取 Nonce 服务统计。
     */
    public Map<String, Object> getNonceStats() {
        return nonceService.getStats();
    }

    private static String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String hmacSha256(String key, String data) {
        try {
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }
}
