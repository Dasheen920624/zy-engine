package com.medkernel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * SMART on FHIR 应用查询请求 DTO：替代 InteropController.querySmartApp 的 Map&lt;String, Object&gt;。
 */
@Schema(description = "SMART on FHIR 应用查询请求")
public class SmartAppQueryRequest {

    @NotBlank(message = "launch_id 不能为空")
    @Schema(description = "启动标识", example = "LAUNCH_202605150001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String launch_id;

    @Schema(description = "客户端ID", example = "medkernel-smart")
    private String client_id;

    @Schema(description = "患者ID", example = "P_AMI_001")
    private String patient_id;

    @Schema(description = "就诊ID", example = "E_AMI_001")
    private String encounter_id;

    @Schema(description = "附加上下文参数")
    private java.util.Map<String, Object> context;

    public String getLaunch_id() { return launch_id; }
    public void setLaunch_id(String launch_id) { this.launch_id = launch_id; }
    public String getClient_id() { return client_id; }
    public void setClient_id(String client_id) { this.client_id = client_id; }
    public String getPatient_id() { return patient_id; }
    public void setPatient_id(String patient_id) { this.patient_id = patient_id; }
    public String getEncounter_id() { return encounter_id; }
    public void setEncounter_id(String encounter_id) { this.encounter_id = encounter_id; }
    public java.util.Map<String, Object> getContext() { return context; }
    public void setContext(java.util.Map<String, Object> context) { this.context = context; }
}
