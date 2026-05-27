package com.medkernel.engine.pkg;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 知识包添加子项资产请求 DTO。
 */
public record PackageItemRequest(
    @NotNull(message = "资产类型不能为空")
    PackageItemAssetType assetType,

    @NotBlank(message = "资产 ID 不能为空")
    String assetId,

    @NotBlank(message = "资产版本号不能为空")
    String assetVersion
) {}
