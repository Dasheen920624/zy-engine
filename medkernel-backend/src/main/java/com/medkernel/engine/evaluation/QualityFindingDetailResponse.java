package com.medkernel.engine.evaluation;

import java.util.List;

public record QualityFindingDetailResponse(
    QualityFinding finding,
    RectificationTask rectificationTask,
    List<RectificationReview> reviews
) {}
