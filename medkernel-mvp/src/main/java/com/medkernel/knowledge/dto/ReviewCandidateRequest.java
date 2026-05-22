package com.medkernel.knowledge.dto;

import javax.validation.constraints.NotBlank;

/**
 * 审核候选请求 DTO。
 */
public class ReviewCandidateRequest {

    @NotBlank(message = "审核状态不能为空")
    private String reviewStatus;

    private String reviewedBy;
    private String reviewNote;
    private String modifiedContent;

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public String getModifiedContent() { return modifiedContent; }
    public void setModifiedContent(String modifiedContent) { this.modifiedContent = modifiedContent; }
}
