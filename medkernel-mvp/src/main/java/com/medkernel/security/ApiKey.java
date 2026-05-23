package com.medkernel.security;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 第三方 API Key 实体。
 * <p>
 * SEC-003: 用于第三方系统接入鉴权，每个 Key 绑定一个租户和可选的角色。
 */
public class ApiKey {

    private String id;
    private String keyHash;
    private String keyPrefix;
    private String secret;
    private String name;
    private String tenantId;
    private String role;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private boolean active;

    public ApiKey() {
        this.id = UUID.randomUUID().toString().replace("-", "");
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    /**
     * 生成新的 API Key 对（key + secret）。
     */
    public static ApiKey generate(String name, String tenantId, String role, String createdBy, LocalDateTime expiresAt) {
        ApiKey apiKey = new ApiKey();
        String rawKey = "mk_" + UUID.randomUUID().toString().replace("-", "");
        String rawSecret = "mks_" + UUID.randomUUID().toString().replace("-", "");
        apiKey.keyPrefix = rawKey.substring(0, 8);
        apiKey.keyHash = sha256(rawKey);
        apiKey.secret = rawSecret;
        apiKey.name = name;
        apiKey.tenantId = tenantId;
        apiKey.role = role;
        apiKey.createdBy = createdBy;
        apiKey.expiresAt = expiresAt;
        // 返回时附带原始 key（仅此一次可见）
        apiKey.rawKey = rawKey;
        return apiKey;
    }

    /** 临时字段：仅生成时可见的原始 key，不持久化 */
    private transient String rawKey;

    public String getRawKey() {
        return rawKey;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("key_prefix", keyPrefix);
        m.put("name", name);
        m.put("tenant_id", tenantId);
        m.put("role", role);
        m.put("created_by", createdBy);
        m.put("created_at", createdAt);
        m.put("expires_at", expiresAt);
        m.put("last_used_at", lastUsedAt);
        m.put("active", active);
        return m;
    }

    public Map<String, Object> toCreateResponse() {
        Map<String, Object> m = toMap();
        m.put("key", rawKey);
        m.put("secret", secret);
        return m;
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

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
