package com.medkernel.adapter;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * CDSS 触发点 API：触发点注册、匹配和执行。
 */
@RestController
@RequestMapping("/api/cdss/triggers")
public class TriggerPointController {

    private final TriggerPointService triggerPointService;
    private final OrganizationContextService organizationContextService;

    public TriggerPointController(TriggerPointService triggerPointService,
                                   OrganizationContextService organizationContextService) {
        this.triggerPointService = triggerPointService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 注册触发点。
     */
    @PostMapping
    public ApiResult<CdssTriggerPointEntity> registerTrigger(@RequestBody CdssTriggerPointEntity trigger,
                                                         HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        trigger.setTenantId(resolveTenantId(orgCtx));
        return ApiResult.success(triggerPointService.registerTrigger(trigger));
    }

    /**
     * 更新触发点。
     */
    @PostMapping("/{triggerId}")
    public ApiResult<String> updateTrigger(@PathVariable Long triggerId,
                                             @RequestBody CdssTriggerPointEntity trigger,
                                             HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        trigger.setId(triggerId);
        trigger.setUpdatedBy("system");
        triggerPointService.updateTrigger(trigger);
        return ApiResult.success("更新成功");
    }

    /**
     * 查询触发点列表。
     */
    @GetMapping
    public ApiResult<List<CdssTriggerPointEntity>> listTriggers(
            @RequestParam(required = false) String businessScenario,
            @RequestParam(required = false) String accessStrategy,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(triggerPointService.listTriggers(
                resolveTenantId(orgCtx), businessScenario, accessStrategy));
    }

    /**
     * 匹配触发点：根据业务场景匹配适用的触发点。
     */
    @PostMapping("/match")
    public ApiResult<List<Map<String, Object>>> matchTriggers(
            @RequestBody Map<String, Object> body,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        String businessScenario = String.valueOf(body.get("businessScenario"));
        return ApiResult.success(triggerPointService.matchTriggers(
                resolveTenantId(orgCtx), businessScenario, body));
    }

    /**
     * 执行触发点。
     */
    @PostMapping("/{triggerCode}/execute")
    public ApiResult<Map<String, Object>> executeTrigger(
            @PathVariable String triggerCode,
            @RequestBody Map<String, Object> eventData,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, eventData);
        return ApiResult.success(triggerPointService.executeTrigger(
                resolveTenantId(orgCtx), triggerCode, eventData));
    }

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }
}
