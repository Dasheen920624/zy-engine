package com.medkernel.knowledge.dto;

import javax.validation.constraints.NotBlank;

/**
 * 审核来源请求 DTO。
 */
public class ReviewSourceRequest {

    @NotBlank(message = "审核状态不能为空")
    private String reviewStatus;

    @NotBlank(message = "审核人不能为空")
    private String reviewedBy;

    private String reviewNote;

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}
