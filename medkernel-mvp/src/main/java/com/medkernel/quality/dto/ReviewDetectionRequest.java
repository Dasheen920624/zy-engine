package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 审核检测结果请求 DTO：替代 AiSafetyController.reviewDetection 的参数。
 */
@Schema(description = "审核检测结果请求")
public class ReviewDetectionRequest {

    @Schema(description = "审核人", example = "安全专家B")
    private String reviewer;

    @Schema(description = "审核备注")
    private String reviewNote;

    @NotBlank(message = "status 不能为空")
    @Schema(description = "审核状态", example = "CONFIRMED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
