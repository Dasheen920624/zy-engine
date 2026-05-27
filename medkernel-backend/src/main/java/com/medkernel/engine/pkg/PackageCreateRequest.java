package com.medkernel.engine.pkg;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识包创建请求 DTO。
 */
public record PackageCreateRequest(
    @NotBlank(message = "包编码不能为空")
    @Size(max = 128, message = "包编码长度不能超过128")
    String packageCode,

    @NotBlank(message = "包版本不能为空")
    @Size(max = 64, message = "包版本长度不能超过64")
    String packageVersion,

    @NotBlank(message = "包名称不能为空")
    @Size(max = 256, message = "包名称长度不能超过256")
    String name,

    String description
) {}
