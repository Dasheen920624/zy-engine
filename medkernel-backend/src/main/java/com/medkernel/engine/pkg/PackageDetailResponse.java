package com.medkernel.engine.pkg;

import java.util.List;

/**
 * 知识包详细响应 DTO（包含资产细项列表）。
 */
public record PackageDetailResponse(
    String packageId,
    String packageCode,
    String packageVersion,
    String name,
    String description,
    KnowledgePackageStatus status,
    List<PackageItemResponse> items
) {
    public static PackageDetailResponse from(KnowledgePackage entity, List<PackageItem> items) {
        return new PackageDetailResponse(
            entity.packageId(),
            entity.packageCode(),
            entity.packageVersion(),
            entity.name(),
            entity.description(),
            entity.status(),
            items.stream().map(PackageItemResponse::from).toList()
        );
    }
}
