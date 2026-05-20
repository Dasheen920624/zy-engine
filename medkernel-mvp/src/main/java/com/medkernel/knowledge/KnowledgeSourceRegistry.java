package com.medkernel.knowledge;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 知识来源注册。
 * 记录知识来源的元数据，含授权、语言、地区、证据等级。
 * 对应数据库表：aik_source_registry
 */
public class KnowledgeSourceRegistry {
    private String tenantId;
    private String sourceCode;
    private String sourceName;
    private String sourceType;
    private String publisher;
    private String region;
    private String language;
    private String releaseVersion;
    private String releaseDate;
    private String effectiveDate;
    private String expiryDate;
    private String authorityLevel; // OFFICIAL, ACADEMIC, INDUSTRY, HOSPITAL
    private String licenseScope;   // INTERNAL, REGIONAL, NATIONAL, GLOBAL
    private String licenseType;    // OPEN, RESTRICTED, COMMERCIAL, PROPRIETARY
    private boolean redistributionAllowed;
    private boolean commercialUseAllowed;
    private boolean exportAllowed;
    private String fetchMethod;    // API, FILE, MANUAL, CRAWLER
    private String sourceUri;
    private String rawHash;
    private String parsedHash;
    private String reviewStatus;   // PENDING, APPROVED, REJECTED, DEPRECATED
    private String reviewedBy;
    private String reviewedTime;
    private String description;
    private String createdBy;
    private String createdTime;
    private String updatedTime;

    // Getters and Setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getReleaseVersion() { return releaseVersion; }
    public void setReleaseVersion(String releaseVersion) { this.releaseVersion = releaseVersion; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getAuthorityLevel() { return authorityLevel; }
    public void setAuthorityLevel(String authorityLevel) { this.authorityLevel = authorityLevel; }

    public String getLicenseScope() { return licenseScope; }
    public void setLicenseScope(String licenseScope) { this.licenseScope = licenseScope; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public boolean isRedistributionAllowed() { return redistributionAllowed; }
    public void setRedistributionAllowed(boolean redistributionAllowed) { this.redistributionAllowed = redistributionAllowed; }

    public boolean isCommercialUseAllowed() { return commercialUseAllowed; }
    public void setCommercialUseAllowed(boolean commercialUseAllowed) { this.commercialUseAllowed = commercialUseAllowed; }

    public boolean isExportAllowed() { return exportAllowed; }
    public void setExportAllowed(boolean exportAllowed) { this.exportAllowed = exportAllowed; }

    public String getFetchMethod() { return fetchMethod; }
    public void setFetchMethod(String fetchMethod) { this.fetchMethod = fetchMethod; }

    public String getSourceUri() { return sourceUri; }
    public void setSourceUri(String sourceUri) { this.sourceUri = sourceUri; }

    public String getRawHash() { return rawHash; }
    public void setRawHash(String rawHash) { this.rawHash = rawHash; }

    public String getParsedHash() { return parsedHash; }
    public void setParsedHash(String parsedHash) { this.parsedHash = parsedHash; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewedTime() { return reviewedTime; }
    public void setReviewedTime(String reviewedTime) { this.reviewedTime = reviewedTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedTime() { return createdTime; }
    public void setCreatedTime(String createdTime) { this.createdTime = createdTime; }

    public String getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(String updatedTime) { this.updatedTime = updatedTime; }

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("tenant_id", tenantId);
        view.put("source_code", sourceCode);
        view.put("source_name", sourceName);
        view.put("source_type", sourceType);
        view.put("publisher", publisher);
        view.put("region", region);
        view.put("language", language);
        view.put("release_version", releaseVersion);
        view.put("release_date", releaseDate);
        view.put("effective_date", effectiveDate);
        view.put("expiry_date", expiryDate);
        view.put("authority_level", authorityLevel);
        view.put("license", buildLicenseView());
        view.put("fetch_method", fetchMethod);
        view.put("source_uri", sourceUri);
        view.put("raw_hash", rawHash);
        view.put("parsed_hash", parsedHash);
        view.put("review_status", reviewStatus);
        view.put("reviewed_by", reviewedBy);
        view.put("reviewed_time", reviewedTime);
        view.put("description", description);
        view.put("created_by", createdBy);
        view.put("created_time", createdTime);
        view.put("updated_time", updatedTime);
        return view;
    }

    private Map<String, Object> buildLicenseView() {
        Map<String, Object> license = new LinkedHashMap<String, Object>();
        license.put("license_type", licenseType);
        license.put("license_scope", licenseScope);
        license.put("redistribution_allowed", redistributionAllowed);
        license.put("commercial_use_allowed", commercialUseAllowed);
        license.put("export_allowed", exportAllowed);
        return license;
    }
}
