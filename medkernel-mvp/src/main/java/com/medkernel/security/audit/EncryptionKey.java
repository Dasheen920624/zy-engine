package com.medkernel.security.audit;

import java.time.LocalDateTime;

/**
 * 加密密钥实体：管理加密密钥版本和生命周期。
 */
public class EncryptionKey {
    private Long id;
    private String keyId;
    private int keyVersion;
    private String algorithm;
    private String keyMaterial;
    private String status; // ACTIVE, DEPRECATED, REVOKED
    private LocalDateTime activatedAt;
    private LocalDateTime deprecatedAt;
    private LocalDateTime expiresAt;
    private String description;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public int getKeyVersion() { return keyVersion; }
    public void setKeyVersion(int keyVersion) { this.keyVersion = keyVersion; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getKeyMaterial() { return keyMaterial; }
    public void setKeyMaterial(String keyMaterial) { this.keyMaterial = keyMaterial; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }

    public LocalDateTime getDeprecatedAt() { return deprecatedAt; }
    public void setDeprecatedAt(LocalDateTime deprecatedAt) { this.deprecatedAt = deprecatedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
