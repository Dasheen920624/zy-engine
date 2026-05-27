package com.medkernel.engine.recommendation;

import jakarta.validation.constraints.NotNull;

/**
 * 医师反馈入参：反馈类型必填，原因代码与说明可选（不采纳/关闭/稍后处理建议填）；
 * 操作者 id 由 RequestContext 取，角色由前端附带。
 */
public record RecommendationFeedbackRequest(
    @NotNull RecommendationFeedbackType feedbackType,
    String reasonCode,
    String reasonText,
    String operatorRole
) {}
