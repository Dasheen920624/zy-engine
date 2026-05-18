package com.medkernel.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigPackage {
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
    private Map<String, Object> manifest = new LinkedHashMap<String, Object>();
    private Map<String, Object> diff = new LinkedHashMap<String, Object>();
    private Map<String, Object> fullSnapshot = new LinkedHashMap<String, Object>();
    private String createdBy;
    private String reviewedBy;
    private String approvedBy;
    private String createdTime;
    private String reviewedTime;
    private String publishedTime;

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

    public Map<String, Object> getManifest() {
        return manifest;
    }

    public void setManifest(Map<String, Object> manifest) {
        this.manifest = manifest == null ? new LinkedHashMap<String, Object>() : manifest;
    }

    public Map<String, Object> getDiff() {
        return diff;
    }

    public void setDiff(Map<String, Object> diff) {
        this.diff = diff == null ? new LinkedHashMap<String, Object>() : diff;
    }

    public Map<String, Object> getFullSnapshot() {
        return fullSnapshot;
    }

    public void setFullSnapshot(Map<String, Object> fullSnapshot) {
        this.fullSnapshot = fullSnapshot == null ? new LinkedHashMap<String, Object>() : fullSnapshot;
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

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getReviewedTime() {
        return reviewedTime;
    }

    public void setReviewedTime(String reviewedTime) {
        this.reviewedTime = reviewedTime;
    }

    public String getPublishedTime() {
        return publishedTime;
    }

    public void setPublishedTime(String publishedTime) {
        this.publishedTime = publishedTime;
    }
}
