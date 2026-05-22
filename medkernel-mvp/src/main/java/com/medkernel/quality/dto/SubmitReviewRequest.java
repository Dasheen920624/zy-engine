package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 提交审核请求 DTO：替代 EvalReportController.submitReview 的参数（共享审核模式）。
 */
@Schema(description = "提交审核请求")
public class SubmitReviewRequest {

    @NotBlank(message = "reviewStatus 不能为空")
    @Schema(description = "审核状态", example = "APPROVED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reviewStatus;

    @NotBlank(message = "reviewedBy 不能为空")
    @Schema(description = "审核人", example = "李四", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reviewedBy;

    @Schema(description = "审核备注")
    private String reviewNote;

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}
