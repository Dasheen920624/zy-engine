package com.medkernel.adapter;

import com.medkernel.common.ApiResult;
import com.medkernel.dto.TriggerMatchRequest;
import com.medkernel.dto.TriggerExecuteRequest;
import com.medkernel.dto.TriggerRegisterRequest;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CDSS 触发点 API：触发点注册、匹配和执行。
 */
@Tag(name = "Trigger Point")
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
    @Operation(summary = "Register trigger")
    @PostMapping
    public ApiResult<CdssTriggerPointEntity> registerTrigger(
            @Valid @RequestBody TriggerRegisterRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        CdssTriggerPointEntity trigger = toEntity(request);
        trigger.setTenantId(resolveTenantId(orgCtx));
        return ApiResult.success(triggerPointService.registerTrigger(trigger));
    }

    /**
     * 更新触发点。
     */
    @Operation(summary = "Update trigger")
    @PostMapping("/{triggerId}")
    public ApiResult<String> updateTrigger(@PathVariable Long triggerId,
                                             @Valid @RequestBody TriggerRegisterRequest request,
                                             HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        CdssTriggerPointEntity trigger = toEntity(request);
        trigger.setId(triggerId);
        trigger.setUpdatedBy("system");
        triggerPointService.updateTrigger(trigger);
        return ApiResult.success("更新成功");
    }

    /**
     * 查询触发点列表。
     */
    @Operation(summary = "List triggers")
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
    @Operation(summary = "Match triggers")
    @PostMapping("/match")
    public ApiResult<List<Map<String, Object>>> matchTriggers(
            @Valid @RequestBody TriggerMatchRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMatchBody(request);
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        String businessScenario = request.getBusinessScenario();
        return ApiResult.success(triggerPointService.matchTriggers(
                resolveTenantId(orgCtx), businessScenario, body));
    }

    /**
     * 执行触发点。
     */
    @Operation(summary = "Execute trigger")
    @PostMapping("/{triggerCode}/execute")
    public ApiResult<Map<String, Object>> executeTrigger(
            @PathVariable String triggerCode,
            @Valid @RequestBody TriggerExecuteRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> eventData = request.getEventData() != null
                ? request.getEventData() : new LinkedHashMap<String, Object>();
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, eventData);
        return ApiResult.success(triggerPointService.executeTrigger(
                resolveTenantId(orgCtx), triggerCode, eventData));
    }

    private CdssTriggerPointEntity toEntity(TriggerRegisterRequest request) {
        CdssTriggerPointEntity entity = new CdssTriggerPointEntity();
        entity.setTriggerCode(request.getTriggerCode());
        entity.setTriggerName(request.getTriggerName());
        entity.setTriggerType(request.getTriggerType());
        entity.setBusinessScenario(request.getBusinessScenario());
        entity.setAccessStrategy(request.getAccessStrategy());
        entity.setAdapterCode(request.getAdapterCode());
        entity.setEndpointUrl(request.getEndpointUrl());
        entity.setRuleCodes(request.getRuleCodes());
        entity.setPathwayCodes(request.getPathwayCodes());
        entity.setPriority(request.getPriority());
        entity.setRiskLevel(request.getRiskLevel());
        entity.setTimeoutMs(request.getTimeoutMs());
        entity.setEnabled(request.getEnabled());
        entity.setDescription(request.getDescription());
        return entity;
    }

    private Map<String, Object> toMatchBody(TriggerMatchRequest request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("businessScenario", request.getBusinessScenario());
        if (request.getContext() != null) {
            body.putAll(request.getContext());
        }
        return body;
    }

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }
}
