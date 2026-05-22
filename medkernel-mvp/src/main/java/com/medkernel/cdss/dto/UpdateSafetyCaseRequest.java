package com.medkernel.cdss.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新安全案例请求 DTO：用于 ClinicalSafetyController.updateSafetyCase。
 */
@Schema(description = "更新安全案例请求")
public class UpdateSafetyCaseRequest {

    @Schema(description = "案例名称")
    private String caseName;

    @Schema(description = "案例类型")
    private String caseType;

    @Schema(description = "范围")
    private String scope;

    @Schema(description = "目标")
    private String goal;

    @Schema(description = "论证")
    private String argument;

    @Schema(description = "证据引用")
    private String evidenceRefs;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "更新人")
    private String updatedBy;

    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }
    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getArgument() { return argument; }
    public void setArgument(String argument) { this.argument = argument; }
    public String getEvidenceRefs() { return evidenceRefs; }
    public void setEvidenceRefs(String evidenceRefs) { this.evidenceRefs = evidenceRefs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
