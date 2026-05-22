package com.medkernel.quality.dto;

import javax.validation.constraints.NotBlank;

/**
 * 审核模型请求 DTO。
 */
public class ReviewModelRequest {

    @NotBlank(message = "审核状态不能为空")
    private String reviewStatus;

    private String reviewedBy;
    private String reviewNote;

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}
