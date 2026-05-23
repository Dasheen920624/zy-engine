package com.medkernel.knowledge.dto;

import com.medkernel.knowledge.KnowledgePackage;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 知识包详情响应 DTO，包含内容JSON长度。
 */
@Schema(description = "知识包详情响应")
public class PackageDetailResponse extends PackageResponse {

    @Schema(description = "内容JSON长度")
    private int contentJsonLength;

    public static PackageDetailResponse fromEntity(KnowledgePackage pkg) {
        if (pkg == null) {
            return null;
        }
        PackageDetailResponse resp = new PackageDetailResponse();
        // 复制父类字段
        PackageResponse base = PackageResponse.fromEntity(pkg);
        resp.setId(base.getId());
        resp.setTenantId(base.getTenantId());
        resp.setPackageCode(base.getPackageCode());
        resp.setPackageName(base.getPackageName());
        resp.setPackageVersion(base.getPackageVersion());
        resp.setDescription(base.getDescription());
        resp.setExportType(base.getExportType());
        resp.setStatus(base.getStatus());
        resp.setSourceTenantId(base.getSourceTenantId());
        resp.setSourceTenantName(base.getSourceTenantName());
        resp.setTargetTenantId(base.getTargetTenantId());
        resp.setTargetTenantName(base.getTargetTenantName());
        resp.setRuleCount(base.getRuleCount());
        resp.setTerminologyCount(base.getTerminologyCount());
        resp.setPathwayCount(base.getPathwayCount());
        resp.setGraphCount(base.getGraphCount());
        resp.setSourceCount(base.getSourceCount());
        resp.setContentHash(base.getContentHash());
        resp.setConflictStrategy(base.getConflictStrategy());
        resp.setSyncMode(base.getSyncMode());
        resp.setSyncStatus(base.getSyncStatus());
        resp.setSyncError(base.getSyncError());
        resp.setSyncTime(base.getSyncTime());
        resp.setCreatedBy(base.getCreatedBy());
        resp.setCreatedTime(base.getCreatedTime());
        resp.setUpdatedBy(base.getUpdatedBy());
        resp.setUpdatedTime(base.getUpdatedTime());
        // 设置内容JSON长度
        resp.contentJsonLength = pkg.getContentJson() != null ? pkg.getContentJson().length() : 0;
        return resp;
    }

    public int getContentJsonLength() { return contentJsonLength; }
    public void setContentJsonLength(int contentJsonLength) { this.contentJsonLength = contentJsonLength; }
}
