package com.medkernel.engine.followup;

import jakarta.validation.constraints.NotBlank;

/**
 * 随访问卷提交请求数据契约 (GA-ENG-API-09)。
 */
public record FollowupQuestionnaireSubmitRequest(
    @NotBlank String taskId,
    @NotBlank String formData,
    String executorId,
    String executorType
) {}
