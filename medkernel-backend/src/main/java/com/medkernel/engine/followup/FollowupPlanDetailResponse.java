package com.medkernel.engine.followup;

import java.util.List;

/**
 * 随访计划详情响应数据契约 (GA-ENG-API-09)。
 */
public record FollowupPlanDetailResponse(
    String planId,
    String tenantId,
    String patientId,
    String encounterId,
    String diseaseCode,
    FollowupPlanStatus status,
    List<FollowupTaskDetailResponse> tasks
) {}
