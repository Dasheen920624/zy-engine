package com.medkernel.engine.recommendation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 推荐卡来源解释入参：sourceType 与 sourceTitle 必填，
 * sourceRefId / sourceVersion / sourceHash 至少满足一项以保证可追溯。
 */
public record RecommendationSourceRequest(
    @NotNull RecommendationSourceType sourceType,
    String sourceRefId,
    String sourceVersion,
    @NotBlank String sourceTitle,
    String citationLocator,
    String sourceHash,
    String summary
) {}
