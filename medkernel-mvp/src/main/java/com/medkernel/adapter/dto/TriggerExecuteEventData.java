package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 触发点执行事件数据 DTO：替代 TriggerExecuteRequest 中的 Map&lt;String, Object&gt; eventData。
 */
@Schema(description = "触发点执行事件数据")
public class TriggerExecuteEventData {

    @NotBlank(message = "eventType 不能为空")
    @Schema(description = "事件类型", example = "ORDER_CREATE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String eventType;

    @Schema(description = "患者ID", example = "P_AMI_001")
    private String patientId;

    @Schema(description = "就诊ID", example = "E_AMI_001")
    private String encounterId;

    @Schema(description = "事件载荷（用于承载无法预定义的动态业务数据）")
    private Map<String, Object> payload;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
