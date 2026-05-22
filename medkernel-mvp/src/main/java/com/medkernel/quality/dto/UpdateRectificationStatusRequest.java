package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 更新整改状态请求 DTO：替代 EvalReportController.updateRectificationStatus 的参数。
 */
@Schema(description = "更新整改状态请求")
public class UpdateRectificationStatusRequest {

    @NotBlank(message = "status 不能为空")
    @Schema(description = "整改状态", example = "COMPLETED", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    @Schema(description = "更新人", example = "王五")
    private String updatedBy;

    @Schema(description = "备注")
    private String note;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
