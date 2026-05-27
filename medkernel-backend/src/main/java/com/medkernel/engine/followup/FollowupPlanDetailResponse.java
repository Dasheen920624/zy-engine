package com.medkernel.engine.followup;

import java.util.List;

public record FollowupPlanDetailResponse(
    String planId,
    String tenantId,
    String patientId,
    String encounterId,
    String diseaseCode,
    FollowupPlanStatus status,
    List<FollowupTaskDetailResponse> tasks
) {}
