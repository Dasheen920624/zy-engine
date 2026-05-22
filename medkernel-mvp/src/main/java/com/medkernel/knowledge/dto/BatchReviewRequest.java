package com.medkernel.knowledge.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量审核候选请求 DTO。
 */
public class BatchReviewRequest {

    @NotEmpty(message = "候选ID列表不能为空")
    private List<Long> candidateIds;

    @NotBlank(message = "审核状态不能为空")
    private String reviewStatus;

    private String reviewedBy;
    private String reviewNote;

    public List<Long> getCandidateIds() { return candidateIds; }
    public void setCandidateIds(List<Long> candidateIds) { this.candidateIds = candidateIds; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}
