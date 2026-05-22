package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 问题分配请求 DTO：替代 QualityController.assignProblem 的参数。
 */
@Schema(description = "问题分配请求")
public class AssignProblemRequest {

    @NotBlank(message = "assignee 不能为空")
    @Schema(description = "分配对象", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String assignee;

    @Schema(description = "优先级", example = "HIGH")
    private String priority;

    @Schema(description = "备注")
    private String note;

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
