package com.medkernel.knowledge.dto;

import java.time.LocalDateTime;

import com.medkernel.knowledge.KnowledgePackage;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识包响应 DTO。
 */
@Schema(description = "知识包响应")
public class PackageResponse {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "知识包编码")
    private String packageCode;

    @Schema(description = "知识包名称")
    private String packageName;

    @Schema(description = "知识包版本")
    private String packageVersion;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "导出类型")
    private String exportType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "来源租户ID")
    private String sourceTenantId;

    @Schema(description = "来源租户名称")
    private String sourceTenantName;

    @Schema(description = "目标租户ID")
    private String targetTenantId;

    @Schema(description = "目标租户名称")
    private String targetTenantName;

    @Schema(description = "规则数量")
    private Integer ruleCount;

    @Schema(description = "术语数量")
    private Integer terminologyCount;

    @Schema(description = "路径数量")
    private Integer pathwayCount;

    @Schema(description = "图谱数量")
    private Integer graphCount;

    @Schema(description = "来源数量")
    private Integer sourceCount;

    @Schema(description = "内容哈希")
    private String contentHash;

    @Schema(description = "冲突策略")
    private String conflictStrategy;

    @Schema(description = "同步模式")
    private String syncMode;

    @Schema(description = "同步状态")
    private String syncStatus;

    @Schema(description = "同步错误信息")
    private String syncError;

    @Schema(description = "同步时间")
    private LocalDateTime syncTime;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新人")
    private String updatedBy;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    public static PackageResponse fromEntity(KnowledgePackage pkg) {
        if (pkg == null) {
            return null;
        }
        PackageResponse resp = new PackageResponse();
        resp.id = pkg.getId();
        resp.tenantId = pkg.getTenantId();
        resp.packageCode = pkg.getPackageCode();
        resp.packageName = pkg.getPackageName();
        resp.packageVersion = pkg.getPackageVersion();
        resp.description = pkg.getDescription();
        resp.exportType = pkg.getExportType();
        resp.status = pkg.getStatus();
        resp.sourceTenantId = pkg.getSourceTenantId();
        resp.sourceTenantName = pkg.getSourceTenantName();
        resp.targetTenantId = pkg.getTargetTenantId();
        resp.targetTenantName = pkg.getTargetTenantName();
        resp.ruleCount = pkg.getRuleCount();
        resp.terminologyCount = pkg.getTerminologyCount();
        resp.pathwayCount = pkg.getPathwayCount();
        resp.graphCount = pkg.getGraphCount();
        resp.sourceCount = pkg.getSourceCount();
        resp.contentHash = pkg.getContentHash();
        resp.conflictStrategy = pkg.getConflictStrategy();
        resp.syncMode = pkg.getSyncMode();
        resp.syncStatus = pkg.getSyncStatus();
        resp.syncError = pkg.getSyncError();
        resp.syncTime = pkg.getSyncTime();
        resp.createdBy = pkg.getCreatedBy();
        resp.createdTime = pkg.getCreatedTime();
        resp.updatedBy = pkg.getUpdatedBy();
        resp.updatedTime = pkg.getUpdatedTime();
        return resp;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getPackageVersion() { return packageVersion; }
    public void setPackageVersion(String packageVersion) { this.packageVersion = packageVersion; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getExportType() { return exportType; }
    public void setExportType(String exportType) { this.exportType = exportType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSourceTenantId() { return sourceTenantId; }
    public void setSourceTenantId(String sourceTenantId) { this.sourceTenantId = sourceTenantId; }

    public String getSourceTenantName() { return sourceTenantName; }
    public void setSourceTenantName(String sourceTenantName) { this.sourceTenantName = sourceTenantName; }

    public String getTargetTenantId() { return targetTenantId; }
    public void setTargetTenantId(String targetTenantId) { this.targetTenantId = targetTenantId; }

    public String getTargetTenantName() { return targetTenantName; }
    public void setTargetTenantName(String targetTenantName) { this.targetTenantName = targetTenantName; }

    public Integer getRuleCount() { return ruleCount; }
    public void setRuleCount(Integer ruleCount) { this.ruleCount = ruleCount; }

    public Integer getTerminologyCount() { return terminologyCount; }
    public void setTerminologyCount(Integer terminologyCount) { this.terminologyCount = terminologyCount; }

    public Integer getPathwayCount() { return pathwayCount; }
    public void setPathwayCount(Integer pathwayCount) { this.pathwayCount = pathwayCount; }

    public Integer getGraphCount() { return graphCount; }
    public void setGraphCount(Integer graphCount) { this.graphCount = graphCount; }

    public Integer getSourceCount() { return sourceCount; }
    public void setSourceCount(Integer sourceCount) { this.sourceCount = sourceCount; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getConflictStrategy() { return conflictStrategy; }
    public void setConflictStrategy(String conflictStrategy) { this.conflictStrategy = conflictStrategy; }

    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getSyncError() { return syncError; }
    public void setSyncError(String syncError) { this.syncError = syncError; }

    public LocalDateTime getSyncTime() { return syncTime; }
    public void setSyncTime(LocalDateTime syncTime) { this.syncTime = syncTime; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
