package com.medkernel.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 提交候选请求 DTO：用于 AiCandidateReviewController.submitCandidate。
 */
@Schema(description = "提交候选请求")
public class SubmitCandidateRequest {

    @NotBlank(message = "候选类型不能为空")
    @Schema(description = "候选类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String candidateType;

    @NotBlank(message = "候选编码不能为空")
    @Schema(description = "候选编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String candidateCode;

    @Schema(description = "来源任务ID")
    private Long sourceJobId;

    @NotBlank(message = "原始内容不能为空")
    @Schema(description = "原始内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String originalContent;

    @Schema(description = "置信度分数")
    private Double confidenceScore;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "审核备注")
    private String reviewNote;

    public String getCandidateType() { return candidateType; }
    public void setCandidateType(String candidateType) { this.candidateType = candidateType; }

    public String getCandidateCode() { return candidateCode; }
    public void setCandidateCode(String candidateCode) { this.candidateCode = candidateCode; }

    public Long getSourceJobId() { return sourceJobId; }
    public void setSourceJobId(Long sourceJobId) { this.sourceJobId = sourceJobId; }

    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}
