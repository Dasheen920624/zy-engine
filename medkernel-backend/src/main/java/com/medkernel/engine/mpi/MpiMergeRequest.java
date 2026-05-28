package com.medkernel.engine.mpi;

import jakarta.validation.constraints.NotBlank;

/**
 * 患者主索引合并请求 DTO。
 */
public record MpiMergeRequest(
    @NotBlank(message = "源患者主索引 ID 不能为空")
    String sourceMpiId,

    @NotBlank(message = "目标患者主索引 ID 不能为空")
    String targetMpiId
) {}
