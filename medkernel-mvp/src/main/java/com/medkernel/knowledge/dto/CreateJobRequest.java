package com.medkernel.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 创建知识生产任务请求 DTO：用于 AiKnowledgeJobController.createJob。
 */
@Schema(description = "创建知识生产任务请求")
public class CreateJobRequest {

    @NotBlank(message = "任务类型不能为空")
    @Schema(description = "任务类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobType;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "来源编码")
    private String sourceCode;

    @Schema(description = "任务参数")
    private String parameters;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "计划执行时间")
    private String scheduledTime;

    @Schema(description = "创建人")
    private String createdBy;

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
