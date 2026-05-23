package com.medkernel.dto;

import com.medkernel.adapter.dto.TriggerMatchContext;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * 触发点匹配请求 DTO：替代 TriggerPointController.matchTriggers 的 Map&lt;String, Object&gt;。
 */
@Schema(description = "触发点匹配请求")
public class TriggerMatchRequest {

    @NotBlank(message = "businessScenario 不能为空")
    @Schema(description = "业务场景编码", example = "ORDER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessScenario;

    @Valid
    @Schema(description = "附加上下文参数")
    private TriggerMatchContext context;

    public String getBusinessScenario() { return businessScenario; }
    public void setBusinessScenario(String businessScenario) { this.businessScenario = businessScenario; }
    public TriggerMatchContext getContext() { return context; }
    public void setContext(TriggerMatchContext context) { this.context = context; }
}
