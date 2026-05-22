package com.medkernel.notification;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 工作流联动通知请求 DTO
 */
public class WorkflowNotificationRequest {
    @NotBlank(message = "任务编码不能为空")
    @Size(max = 50, message = "任务编码最长50字符")
    private String taskCode;

    private String businessType;

    @Size(max = 200, message = "标题最长200字符")
    private String title;

    @Size(max = 2000, message = "描述最长2000字符")
    private String description;

    @NotBlank(message = "指派人不能为空")
    private String assignedTo;

    private String createdBy;

    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
