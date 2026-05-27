package com.medkernel.engine.followup;

import java.time.Instant;

public record FollowupTaskDetailResponse(
    String taskId,
    FollowupTaskType taskType,
    Instant dueDate,
    FollowupTaskStatus status
) {}
