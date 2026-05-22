package com.medkernel.knowledge.dto;

import javax.validation.constraints.NotBlank;

/**
 * 导出知识包请求 DTO。
 */
public class ExportPackageRequest {

    @NotBlank(message = "包编码不能为空")
    private String packageCode;

    @NotBlank(message = "包名称不能为空")
    private String packageName;

    private String packageVersion;
    private String description;
    private String exportType;
    private String sourceTenantId;
    private String sourceTenantName;
    private String targetTenantId;
    private String targetTenantName;
    private String conflictStrategy;
    private String syncMode;
    private String createdBy;
    private String tenantId;

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

    public String getSourceTenantId() { return sourceTenantId; }
    public void setSourceTenantId(String sourceTenantId) { this.sourceTenantId = sourceTenantId; }

    public String getSourceTenantName() { return sourceTenantName; }
    public void setSourceTenantName(String sourceTenantName) { this.sourceTenantName = sourceTenantName; }

    public String getTargetTenantId() { return targetTenantId; }
    public void setTargetTenantId(String targetTenantId) { this.targetTenantId = targetTenantId; }

    public String getTargetTenantName() { return targetTenantName; }
    public void setTargetTenantName(String targetTenantName) { this.targetTenantName = targetTenantName; }

    public String getConflictStrategy() { return conflictStrategy; }
    public void setConflictStrategy(String conflictStrategy) { this.conflictStrategy = conflictStrategy; }

    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
