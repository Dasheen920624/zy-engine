package com.medkernel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;

/**
 * 触发点匹配请求 DTO：替代 TriggerPointController.matchTriggers 的 Map&lt;String, Object&gt;。
 */
@Schema(description = "触发点匹配请求")
public class TriggerMatchRequest {

    @NotBlank(message = "businessScenario 不能为空")
    @Schema(description = "业务场景编码", example = "ORDER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessScenario;

    @Schema(description = "附加上下文参数")
    private java.util.Map<String, Object> context;

    public String getBusinessScenario() { return businessScenario; }
    public void setBusinessScenario(String businessScenario) { this.businessScenario = businessScenario; }
    public java.util.Map<String, Object> getContext() { return context; }
    public void setContext(java.util.Map<String, Object> context) { this.context = context; }
}
