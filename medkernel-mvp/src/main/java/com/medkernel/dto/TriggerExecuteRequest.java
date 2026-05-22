package com.medkernel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * 触发点执行请求 DTO：替代 TriggerPointController.executeTrigger 的 Map&lt;String, Object&gt;。
 */
@Schema(description = "触发点执行请求")
public class TriggerExecuteRequest {

    @Schema(description = "事件数据（触发点执行所需的业务数据）")
    private Map<String, Object> eventData;

    public Map<String, Object> getEventData() { return eventData; }
    public void setEventData(Map<String, Object> eventData) { this.eventData = eventData; }
}
