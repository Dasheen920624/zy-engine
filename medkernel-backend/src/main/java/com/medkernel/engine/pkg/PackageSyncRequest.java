package com.medkernel.engine.pkg;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 知识包同步与发布请求 DTO。
 */
public record PackageSyncRequest(
    @NotBlank(message = "目标组织 ID 不能为空")
    String targetOrgUnitId,

    @NotNull(message = "发布策略不能为空")
    ReleaseStrategy strategy,

    @NotNull(message = "作用域范围类型不能为空")
    ReleaseScopeType scopeType,

    String scopeValue,

    @NotEmpty(message = "同步通道目标列表不能为空")
    List<String> targetIds
) {}
