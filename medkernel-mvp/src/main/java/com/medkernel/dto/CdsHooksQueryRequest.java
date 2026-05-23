package com.medkernel.dto;

import com.medkernel.adapter.dto.CdsHooksContext;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * CDS Hooks 查询请求 DTO：替代 InteropController.queryCdsHooks 的 Map&lt;String, Object&gt;。
 */
@Schema(description = "CDS Hooks 查询请求")
public class CdsHooksQueryRequest {

    @NotBlank(message = "hook_id 不能为空")
    @Schema(description = "Hook 标识", example = "patient-risk-assessment", requiredMode = Schema.RequiredMode.REQUIRED)
    private String hook_id;

    @Schema(description = "Hook 类型", example = "patient-view")
    private String hook_type;

    @Schema(description = "患者ID", example = "P_AMI_001")
    private String patient_id;

    @Schema(description = "就诊ID", example = "E_AMI_001")
    private String encounter_id;

    @Valid
    @Schema(description = "附加上下文参数")
    private CdsHooksContext context;

    public String getHook_id() { return hook_id; }
    public void setHook_id(String hook_id) { this.hook_id = hook_id; }
    public String getHook_type() { return hook_type; }
    public void setHook_type(String hook_type) { this.hook_type = hook_type; }
    public String getPatient_id() { return patient_id; }
    public void setPatient_id(String patient_id) { this.patient_id = patient_id; }
    public String getEncounter_id() { return encounter_id; }
    public void setEncounter_id(String encounter_id) { this.encounter_id = encounter_id; }
    public CdsHooksContext getContext() { return context; }
    public void setContext(CdsHooksContext context) { this.context = context; }
}
