package com.medkernel.engine.evaluation;

import jakarta.validation.constraints.NotNull;

/**
 * 整改复核请求。
 *
 * <p>包含复核结论、复核意见和证据引用；通过或豁免场景需满足服务层的说明与证据约束。
 */
public record RectificationReviewRequest(
    @NotNull RectificationReviewDecision decision,
    String comment,
    String evidenceRef
) {}
