package com.medkernel.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 密钥轮换服务。
 * 支持多版本密钥、旧令牌宽限期、密钥版本号嵌入 JWT。
 * 提供密钥轮换操作和密钥状态查询。
 */
public class KeyRotationService {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final AtomicLong KEY_VERSION_SEQ = new AtomicLong(1);
    private static final int DEFAULT_GRACE_PERIOD_HOURS = 24;

    private final Map<Long, KeyVersion> keyVersions = new ConcurrentHashMap<Long, KeyVersion>();
    private long activeKeyId = 0;

    /**
     * 密钥版本记录。
     */
    public static class KeyVersion {
        private long keyId;
        private String keyAlias;
        private String keyHash;       // 密钥指纹（SHA-256 哈希前8字节）
        private String algorithm;
        private String status;         // ACTIVE, GRACE, RETIRED, REVOKED
        private String createdAt;
        private String activatedAt;
        private String retiredAt;
        private String revokedAt;
        private int gracePeriodHours;
        private String rotatedBy;

        public Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("key_id", keyId);
            view.put("key_alias", keyAlias);
            view.put("key_hash", keyHash);
            view.put("algorithm", algorithm);
            view.put("status", status);
            view.put("created_at", createdAt);
            view.put("activated_at", activatedAt);
            view.put("retired_at", retiredAt);
            view.put("revoked_at", revokedAt);
            view.put("grace_period_hours", gracePeriodHours);
            view.put("rotated_by", rotatedBy);
            return view;
        }

        public long getKeyId() { return keyId; }
        public void setKeyId(long keyId) { this.keyId = keyId; }
        public String getKeyAlias() { return keyAlias; }
        public void setKeyAlias(String keyAlias) { this.keyAlias = keyAlias; }
        public String getKeyHash() { return keyHash; }
        public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getActivatedAt() { return activatedAt; }
        public void setActivatedAt(String activatedAt) { this.activatedAt = activatedAt; }
        public String getRetiredAt() { return retiredAt; }
        public void setRetiredAt(String retiredAt) { this.retiredAt = retiredAt; }
        public String getRevokedAt() { return revokedAt; }
        public void setRevokedAt(String revokedAt) { this.revokedAt = revokedAt; }
        public int getGracePeriodHours() { return gracePeriodHours; }
        public void setGracePeriodHours(int gracePeriodHours) { this.gracePeriodHours = gracePeriodHours; }
        public String getRotatedBy() { return rotatedBy; }
        public void setRotatedBy(String rotatedBy) { this.rotatedBy = rotatedBy; }
    }

    /**
     * 漏洞扫描结果。
     */
    public static class VulnerabilityScanResult {
        private String scanId;
        private String scanType;
        private String scannedAt;
        private int totalDependencies;
        private int criticalCount;
        private int highCount;
        private int mediumCount;
        private int lowCount;
        private List<Map<String, Object>> findings;

        public Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("scan_id", scanId);
            view.put("scan_type", scanType);
            view.put("scanned_at", scannedAt);
            view.put("total_dependencies", totalDependencies);
            view.put("critical_count", criticalCount);
            view.put("high_count", highCount);
            view.put("medium_count", mediumCount);
            view.put("low_count", lowCount);
            view.put("findings", findings);
            return view;
        }

        public void setScanId(String scanId) { this.scanId = scanId; }
        public void setScanType(String scanType) { this.scanType = scanType; }
        public void setScannedAt(String scannedAt) { this.scannedAt = scannedAt; }
        public void setTotalDependencies(int totalDependencies) { this.totalDependencies = totalDependencies; }
        public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }
        public void setHighCount(int highCount) { this.highCount = highCount; }
        public void setMediumCount(int mediumCount) { this.mediumCount = mediumCount; }
        public void setLowCount(int lowCount) { this.lowCount = lowCount; }
        public void setFindings(List<Map<String, Object>> findings) { this.findings = findings; }
    }

    /**
     * 初始化第一个密钥版本。
     */
    public KeyRotationService() {
        initializeFirstKey();
    }

    private void initializeFirstKey() {
        KeyVersion initial = new KeyVersion();
        initial.setKeyId(KEY_VERSION_SEQ.getAndIncrement());
        initial.setKeyAlias("initial-key-v1");
        initial.setKeyHash(computeKeyHash("initial-key-material"));
        initial.setAlgorithm("HS256");
        initial.setStatus("ACTIVE");
        initial.setCreatedAt(LocalDateTime.now().toString());
        initial.setActivatedAt(LocalDateTime.now().toString());
        initial.setGracePeriodHours(DEFAULT_GRACE_PERIOD_HOURS);
        initial.setRotatedBy("system");
        keyVersions.put(initial.getKeyId(), initial);
        activeKeyId = initial.getKeyId();
    }

    /**
     * 执行密钥轮换：当前 ACTIVE 变为 GRACE，新密钥变为 ACTIVE。
     */
    public KeyVersion rotateKey(String newKeyAlias, String newKeyMaterial, String rotatedBy) {
        // 当前活跃密钥进入宽限期
        KeyVersion current = keyVersions.get(activeKeyId);
        if (current != null && "ACTIVE".equals(current.getStatus())) {
            current.setStatus("GRACE");
            current.setRetiredAt(LocalDateTime.now().toString());
        }

        // 创建新密钥版本
        KeyVersion newKey = new KeyVersion();
        newKey.setKeyId(KEY_VERSION_SEQ.getAndIncrement());
        newKey.setKeyAlias(newKeyAlias != null ? newKeyAlias : "key-v" + newKey.getKeyId());
        newKey.setKeyHash(computeKeyHash(newKeyMaterial));
        newKey.setAlgorithm("HS256");
        newKey.setStatus("ACTIVE");
        newKey.setCreatedAt(LocalDateTime.now().toString());
        newKey.setActivatedAt(LocalDateTime.now().toString());
        newKey.setGracePeriodHours(DEFAULT_GRACE_PERIOD_HOURS);
        newKey.setRotatedBy(rotatedBy);

        keyVersions.put(newKey.getKeyId(), newKey);
        activeKeyId = newKey.getKeyId();
        return newKey;
    }

    /**
     * 撤销指定密钥版本（紧急撤销）。
     */
    public KeyVersion revokeKey(long keyId, String revokedBy) {
        KeyVersion key = keyVersions.get(keyId);
        if (key == null) {
            throw new IllegalArgumentException("Key version not found: " + keyId);
        }
        key.setStatus("REVOKED");
        key.setRevokedAt(LocalDateTime.now().toString());
        return key;
    }

    /**
     * 退役宽限期已过的密钥。
     */
    public List<KeyVersion> retireExpiredGraceKeys() {
        List<KeyVersion> retired = new ArrayList<KeyVersion>();
        LocalDateTime now = LocalDateTime.now();
        for (KeyVersion key : keyVersions.values()) {
            if (!"GRACE".equals(key.getStatus())) continue;
            // 简化判断：宽限期超过24小时自动退役
            if (key.getRetiredAt() != null) {
                LocalDateTime retiredTime = LocalDateTime.parse(key.getRetiredAt());
                if (retiredTime.plusHours(key.getGracePeriodHours()).isBefore(now)) {
                    key.setStatus("RETIRED");
                    retired.add(key);
                }
            }
        }
        return retired;
    }

    /**
     * 获取所有密钥版本列表。
     */
    public List<KeyVersion> listKeyVersions() {
        return new ArrayList<KeyVersion>(keyVersions.values());
    }

    /**
     * 获取当前活跃密钥。
     */
    public KeyVersion getActiveKey() {
        return keyVersions.get(activeKeyId);
    }

    /**
     * 获取安全基线状态。
     */
    public Map<String, Object> getSecurityBaselineStatus() {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("jwt_algorithm", "HS256");
        status.put("active_key_id", activeKeyId);
        status.put("total_key_versions", keyVersions.size());
        status.put("grace_keys", keyVersions.values().stream()
                .filter(k -> "GRACE".equals(k.getStatus())).count());
        status.put("retired_keys", keyVersions.values().stream()
                .filter(k -> "RETIRED".equals(k.getStatus())).count());
        status.put("revoked_keys", keyVersions.values().stream()
                .filter(k -> "REVOKED".equals(k.getStatus())).count());
        status.put("password_hash_algorithm", "BCrypt");
        status.put("tls_min_version", "1.2");
        status.put("hsts_enabled", true);
        status.put("sbom_format", "CycloneDX");
        return status;
    }

    /**
     * 执行漏洞扫描（MVP 模拟）。
     */
    public VulnerabilityScanResult performVulnerabilityScan() {
        VulnerabilityScanResult result = new VulnerabilityScanResult();
        result.setScanId("SCAN-" + System.currentTimeMillis());
        result.setScanType("DEPENDENCY_CHECK");
        result.setScannedAt(LocalDateTime.now().toString());
        result.setTotalDependencies(42);
        result.setCriticalCount(0);
        result.setHighCount(1);
        result.setMediumCount(3);
        result.setLowCount(5);

        List<Map<String, Object>> findings = new ArrayList<Map<String, Object>>();
        Map<String, Object> finding = new LinkedHashMap<String, Object>();
        finding.put("dependency", "jackson-databind");
        finding.put("version", "2.13.x");
        finding.put("severity", "HIGH");
        finding.put("cve", "CVE-2022-42003");
        finding.put("recommendation", "升级到 2.15.x+");
        findings.add(finding);
        result.setFindings(findings);
        return result;
    }

    private String computeKeyHash(String keyMaterial) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
            // 取前8字节作为指纹
            byte[] fingerprint = new byte[8];
            System.arraycopy(hash, 0, fingerprint, 0, 8);
            return Base64.getEncoder().encodeToString(fingerprint);
        } catch (Exception e) {
            return "ERROR";
        }
    }
}
