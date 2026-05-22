package com.medkernel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 互联互通适配器查询请求 DTO：替代 InteropController.query 的 Map&lt;String, Object&gt;。
 */
@Schema(description = "互联互通适配器查询请求")
public class InteropQueryRequest {

    @NotBlank(message = "adapter_code 不能为空")
    @Schema(description = "适配器编码", example = "HIS_FHIR_ADAPTER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adapter_code;

    @NotBlank(message = "query_code 不能为空")
    @Schema(description = "查询编码", example = "QUERY_FHIR_PATIENT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query_code;

    @Schema(description = "查询参数（如 patient_id、encounter_id 等）")
    private java.util.Map<String, Object> params;

    public String getAdapter_code() { return adapter_code; }
    public void setAdapter_code(String adapter_code) { this.adapter_code = adapter_code; }
    public String getQuery_code() { return query_code; }
    public void setQuery_code(String query_code) { this.query_code = query_code; }
    public java.util.Map<String, Object> getParams() { return params; }
    public void setParams(java.util.Map<String, Object> params) { this.params = params; }
}
