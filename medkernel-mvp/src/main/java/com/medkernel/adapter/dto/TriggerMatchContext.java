package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import java.util.Map;

/**
 * 触发点匹配上下文 DTO：替代 TriggerMatchRequest 中的 Map&lt;String, Object&gt; context。
 */
@Schema(description = "触发点匹配上下文")
public class TriggerMatchContext {

    @Schema(description = "患者ID", example = "P_AMI_001")
    private String patientId;

    @Schema(description = "就诊ID", example = "E_AMI_001")
    private String encounterId;

    @Schema(description = "触发点标识", example = "ORDER_CREATE")
    private String triggerPoint;

    @Schema(description = "附加扩展数据（用于承载无法预定义的动态字段）")
    private Map<String, Object> additionalData;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public String getTriggerPoint() { return triggerPoint; }
    public void setTriggerPoint(String triggerPoint) { this.triggerPoint = triggerPoint; }
    public Map<String, Object> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }
}
