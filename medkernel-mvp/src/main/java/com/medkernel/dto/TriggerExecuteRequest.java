package com.medkernel.dto;

import com.medkernel.adapter.dto.TriggerExecuteEventData;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;

/**
 * 触发点执行请求 DTO：替代 TriggerPointController.executeTrigger 的 Map&lt;String, Object&gt;。
 */
@Schema(description = "触发点执行请求")
public class TriggerExecuteRequest {

    @Valid
    @Schema(description = "事件数据（触发点执行所需的业务数据）")
    private TriggerExecuteEventData eventData;

    public TriggerExecuteEventData getEventData() { return eventData; }
    public void setEventData(TriggerExecuteEventData eventData) { this.eventData = eventData; }
}
