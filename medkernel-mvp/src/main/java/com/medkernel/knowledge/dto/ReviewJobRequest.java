package com.medkernel.knowledge.dto;

import javax.validation.constraints.NotBlank;

/**
 * 审核任务请求 DTO。
 */
public class ReviewJobRequest {

    @NotBlank(message = "审核状态不能为空")
    private String reviewStatus;

    private String reviewedBy;
    private String reviewComment;

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
}
