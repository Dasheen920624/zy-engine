package com.medkernel.config;

import java.time.LocalDateTime;

/**
 * 配置包数据库实体类
 * 对应表：cfg_config_package
 */
public class ConfigPackageEntity {
    private Long id;
    private String tenantId;
    private String packageCode;
    private String packageVersion;
    private String assetType;
    private String scopeLevel;
    private String scopeCode;
    private String status;
    private String baseVersion;
    private String targetVersion;
    private String contentHash;
    private String declaredContentHash;
    private String manifestJson;
    private String diffJson;
    private String fullSnapshotJson;
    private String createdBy;
    private String reviewedBy;
    private String approvedBy;
    private LocalDateTime createdTime;
    private LocalDateTime reviewedTime;
    private LocalDateTime publishedTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public String getPackageVersion() {
        return packageVersion;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getScopeLevel() {
        return scopeLevel;
    }

    public void setScopeLevel(String scopeLevel) {
        this.scopeLevel = scopeLevel;
    }

    public String getScopeCode() {
        return scopeCode;
    }

    public void setScopeCode(String scopeCode) {
        this.scopeCode = scopeCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBaseVersion() {
        return baseVersion;
    }

    public void setBaseVersion(String baseVersion) {
        this.baseVersion = baseVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getDeclaredContentHash() {
        return declaredContentHash;
    }

    public void setDeclaredContentHash(String declaredContentHash) {
        this.declaredContentHash = declaredContentHash;
    }

    public String getManifestJson() {
        return manifestJson;
    }

    public void setManifestJson(String manifestJson) {
        this.manifestJson = manifestJson;
    }

    public String getDiffJson() {
        return diffJson;
    }

    public void setDiffJson(String diffJson) {
        this.diffJson = diffJson;
    }

    public String getFullSnapshotJson() {
        return fullSnapshotJson;
    }

    public void setFullSnapshotJson(String fullSnapshotJson) {
        this.fullSnapshotJson = fullSnapshotJson;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getReviewedTime() {
        return reviewedTime;
    }

    public void setReviewedTime(LocalDateTime reviewedTime) {
        this.reviewedTime = reviewedTime;
    }

    public LocalDateTime getPublishedTime() {
        return publishedTime;
    }

    public void setPublishedTime(LocalDateTime publishedTime) {
        this.publishedTime = publishedTime;
    }

    /**
     * 从ConfigPackage转换为ConfigPackageEntity
     */
    public static ConfigPackageEntity fromConfigPackage(ConfigPackage configPackage) {
        ConfigPackageEntity entity = new ConfigPackageEntity();
        entity.setTenantId(configPackage.getTenantId());
        entity.setPackageCode(configPackage.getPackageCode());
        entity.setPackageVersion(configPackage.getPackageVersion());
        entity.setAssetType(configPackage.getAssetType());
        entity.setScopeLevel(configPackage.getScopeLevel());
        entity.setScopeCode(configPackage.getScopeCode());
        entity.setStatus(configPackage.getStatus());
        entity.setBaseVersion(configPackage.getBaseVersion());
        entity.setTargetVersion(configPackage.getTargetVersion());
        entity.setContentHash(configPackage.getContentHash());
        entity.setDeclaredContentHash(configPackage.getDeclaredContentHash());
        entity.setCreatedBy(configPackage.getCreatedBy());
        entity.setReviewedBy(configPackage.getReviewedBy());
        entity.setApprovedBy(configPackage.getApprovedBy());
        // JSON字段需要在Service层序列化
        return entity;
    }

    /**
     * 转换为ConfigPackage对象
     */
    public ConfigPackage toConfigPackage() {
        ConfigPackage configPackage = new ConfigPackage();
        configPackage.setTenantId(this.tenantId);
        configPackage.setPackageCode(this.packageCode);
        configPackage.setPackageVersion(this.packageVersion);
        configPackage.setAssetType(this.assetType);
        configPackage.setScopeLevel(this.scopeLevel);
        configPackage.setScopeCode(this.scopeCode);
        configPackage.setStatus(this.status);
        configPackage.setBaseVersion(this.baseVersion);
        configPackage.setTargetVersion(this.targetVersion);
        configPackage.setContentHash(this.contentHash);
        configPackage.setDeclaredContentHash(this.declaredContentHash);
        configPackage.setCreatedBy(this.createdBy);
        configPackage.setReviewedBy(this.reviewedBy);
        configPackage.setApprovedBy(this.approvedBy);
        // JSON字段需要在Service层反序列化
        return configPackage;
    }
}