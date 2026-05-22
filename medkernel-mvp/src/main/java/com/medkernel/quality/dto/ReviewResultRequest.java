package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 审核验收结果请求 DTO：替代 AcceptanceTestController.reviewResult 的参数。
 */
@Schema(description = "审核验收结果请求")
public class ReviewResultRequest {

    @Schema(description = "审核人", example = "验收负责人C")
    private String reviewedBy;

    @Schema(description = "审核备注")
    private String reviewNote;

    @NotBlank(message = "status 不能为空")
    @Schema(description = "审核状态", example = "PASSED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
