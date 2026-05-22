package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新危险请求 DTO：用于 ClinicalSafetyController.updateHazard。
 */
@Schema(description = "更新危险请求")
public class UpdateHazardRequest {

    @Schema(description = "危险名称")
    private String hazardName;

    @Schema(description = "危险分类")
    private String hazardCategory;

    @Schema(description = "危险描述")
    private String hazardDescription;

    @Schema(description = "受影响流程")
    private String affectedProcess;

    @Schema(description = "可能性")
    private String likelihood;

    @Schema(description = "严重程度")
    private String severity;

    @Schema(description = "控制措施")
    private String controlMeasures;

    @Schema(description = "残余风险")
    private String residualRisk;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "阻断策略")
    private String blockingStrategy;

    @Schema(description = "更新人")
    private String updatedBy;

    public String getHazardName() { return hazardName; }
    public void setHazardName(String hazardName) { this.hazardName = hazardName; }
    public String getHazardCategory() { return hazardCategory; }
    public void setHazardCategory(String hazardCategory) { this.hazardCategory = hazardCategory; }
    public String getHazardDescription() { return hazardDescription; }
    public void setHazardDescription(String hazardDescription) { this.hazardDescription = hazardDescription; }
    public String getAffectedProcess() { return affectedProcess; }
    public void setAffectedProcess(String affectedProcess) { this.affectedProcess = affectedProcess; }
    public String getLikelihood() { return likelihood; }
    public void setLikelihood(String likelihood) { this.likelihood = likelihood; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getControlMeasures() { return controlMeasures; }
    public void setControlMeasures(String controlMeasures) { this.controlMeasures = controlMeasures; }
    public String getResidualRisk() { return residualRisk; }
    public void setResidualRisk(String residualRisk) { this.residualRisk = residualRisk; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBlockingStrategy() { return blockingStrategy; }
    public void setBlockingStrategy(String blockingStrategy) { this.blockingStrategy = blockingStrategy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
