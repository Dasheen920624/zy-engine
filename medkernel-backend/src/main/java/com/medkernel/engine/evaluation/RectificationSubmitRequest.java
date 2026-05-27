package com.medkernel.engine.evaluation;

import jakarta.validation.constraints.NotBlank;

/**
 * 整改提交请求。
 *
 * <p>责任方提交整改说明和证据引用，服务层要求二者均非空。
 */
public record RectificationSubmitRequest(
    @NotBlank String rectificationSummary,
    @NotBlank String evidenceRef
) {}
