package com.medkernel.knowledge;

import java.time.LocalDateTime;

/**
 * 质检发现实体。
 * 对应数据库表：quality_finding
 */
public class QualityFinding {
    private Long id;
    private Long tenantId;
    private String findingCode;
    private String findingType;       // MISSING_SOURCE/EXPIRED/UNCLEAR_AUTH/RULE_CONFLICT/LOW_CONFIDENCE/MULTI_CANDIDATE_CONFLICT
    private String severity;          // INFO/WARNING/CRITICAL
    private String assetType;         // RULE/TERMINOLOGY_MAPPING/KNOWLEDGE_ASSET/PATHWAY
    private String assetCode;         // 资产编码
    private String assetName;         // 资产名称
    private String assetVersion;      // 资产版本
    private String description;       // 发现描述
    private String detailJson;        // 详细信息 JSON
    private String detectionRule;     // 检测规则描述
    private String status;            // OPEN/ACKNOWLEDGED/RESOLVED/DISMISSED
    private String resolvedBy;
    private String resolutionNote;
    private LocalDateTime resolvedTime;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getFindingCode() { return findingCode; }
    public void setFindingCode(String findingCode) { this.findingCode = findingCode; }

    public String getFindingType() { return findingType; }
    public void setFindingType(String findingType) { this.findingType = findingType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public String getAssetVersion() { return assetVersion; }
    public void setAssetVersion(String assetVersion) { this.assetVersion = assetVersion; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }

    public String getDetectionRule() { return detectionRule; }
    public void setDetectionRule(String detectionRule) { this.detectionRule = detectionRule; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public LocalDateTime getResolvedTime() { return resolvedTime; }
    public void setResolvedTime(LocalDateTime resolvedTime) { this.resolvedTime = resolvedTime; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
