package com.medkernel.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 创建整改请求 DTO：替代 EvalReportController.createRectification 的参数。
 */
@Schema(description = "创建整改请求")
public class CreateRectificationRequest {

    @NotBlank(message = "rectificationType 不能为空")
    @Schema(description = "整改类型", example = "CORRECTIVE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rectificationType;

    @NotBlank(message = "description 不能为空")
    @Schema(description = "整改描述", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @Schema(description = "负责人", example = "王五")
    private String assignee;

    @Schema(description = "截止日期", example = "2024-12-31")
    private String dueDate;

    @Schema(description = "关联指标编码（逗号分隔）", example = "IND_ACC_001,IND_SAFE_002")
    private String relatedIndicatorCodes;

    public String getRectificationType() { return rectificationType; }
    public void setRectificationType(String rectificationType) { this.rectificationType = rectificationType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getRelatedIndicatorCodes() { return relatedIndicatorCodes; }
    public void setRelatedIndicatorCodes(String relatedIndicatorCodes) { this.relatedIndicatorCodes = relatedIndicatorCodes; }
}
