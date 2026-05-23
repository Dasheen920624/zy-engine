package com.medkernel.knowledge.dto;

import com.medkernel.knowledge.KnowledgeSourceRegistry;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 知识来源注册响应 DTO。
 */
@Schema(description = "知识来源注册响应")
public class SourceResponse {

    @Schema(description = "租户ID")
    private String tenantId;

    @NotBlank(message = "来源编码不能为空")
    @Schema(description = "来源编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceCode;

    @NotBlank(message = "来源名称不能为空")
    @Schema(description = "来源名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceName;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "发布者")
    private String publisher;

    @Schema(description = "地区")
    private String region;

    @Schema(description = "语言")
    private String language;

    @Schema(description = "发布版本")
    private String releaseVersion;

    @Schema(description = "发布日期")
    private String releaseDate;

    @Schema(description = "生效日期")
    private String effectiveDate;

    @Schema(description = "失效日期")
    private String expiryDate;

    @Schema(description = "权威等级")
    private String authorityLevel;

    @Schema(description = "许可范围")
    private String licenseScope;

    @Schema(description = "许可类型")
    private String licenseType;

    @Schema(description = "是否允许再分发")
    private boolean redistributionAllowed;

    @Schema(description = "是否允许商业使用")
    private boolean commercialUseAllowed;

    @Schema(description = "是否允许导出")
    private boolean exportAllowed;

    @Schema(description = "获取方式")
    private String fetchMethod;

    @Schema(description = "来源URI")
    private String sourceUri;

    @Schema(description = "原始数据哈希")
    private String rawHash;

    @Schema(description = "解析数据哈希")
    private String parsedHash;

    @Schema(description = "审核状态")
    private String reviewStatus;

    @Schema(description = "审核人")
    private String reviewedBy;

    @Schema(description = "审核时间")
    private String reviewedTime;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private String createdTime;

    @Schema(description = "更新时间")
    private String updatedTime;

    public static SourceResponse fromEntity(KnowledgeSourceRegistry source) {
        if (source == null) {
            return null;
        }
        SourceResponse resp = new SourceResponse();
        resp.tenantId = source.getTenantId();
        resp.sourceCode = source.getSourceCode();
        resp.sourceName = source.getSourceName();
        resp.sourceType = source.getSourceType();
        resp.publisher = source.getPublisher();
        resp.region = source.getRegion();
        resp.language = source.getLanguage();
        resp.releaseVersion = source.getReleaseVersion();
        resp.releaseDate = source.getReleaseDate();
        resp.effectiveDate = source.getEffectiveDate();
        resp.expiryDate = source.getExpiryDate();
        resp.authorityLevel = source.getAuthorityLevel();
        resp.licenseScope = source.getLicenseScope();
        resp.licenseType = source.getLicenseType();
        resp.redistributionAllowed = source.isRedistributionAllowed();
        resp.commercialUseAllowed = source.isCommercialUseAllowed();
        resp.exportAllowed = source.isExportAllowed();
        resp.fetchMethod = source.getFetchMethod();
        resp.sourceUri = source.getSourceUri();
        resp.rawHash = source.getRawHash();
        resp.parsedHash = source.getParsedHash();
        resp.reviewStatus = source.getReviewStatus();
        resp.reviewedBy = source.getReviewedBy();
        resp.reviewedTime = source.getReviewedTime();
        resp.description = source.getDescription();
        resp.createdBy = source.getCreatedBy();
        resp.createdTime = source.getCreatedTime();
        resp.updatedTime = source.getUpdatedTime();
        return resp;
    }

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
}
