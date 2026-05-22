package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 审核安全案例请求 DTO：用于 ClinicalSafetyController.reviewSafetyCase。
 */
@Schema(description = "审核安全案例请求")
public class ReviewSafetyCaseRequest {

    @NotBlank(message = "reviewStatus 不能为空")
    @Schema(description = "审核状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reviewStatus;

    @NotBlank(message = "reviewedBy 不能为空")
    @Schema(description = "审核人", requiredMode = Schema.RequiredMode.REQUIRED)
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
