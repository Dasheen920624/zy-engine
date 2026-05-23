package com.medkernel.adapter;

import com.medkernel.adapter.dto.CdssTriggerPointResponse;
import com.medkernel.adapter.dto.TriggerExecuteEventData;
import com.medkernel.adapter.dto.TriggerExecuteResponse;
import com.medkernel.adapter.dto.TriggerMatchContext;
import com.medkernel.adapter.dto.TriggerMatchResponse;
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
import java.util.stream.Collectors;

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
    public ApiResult<CdssTriggerPointResponse> registerTrigger(
            @Valid @RequestBody TriggerRegisterRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        CdssTriggerPointEntity trigger = toEntity(request);
        trigger.setTenantId(resolveTenantId(orgCtx));
        return ApiResult.success(CdssTriggerPointResponse.fromEntity(
                triggerPointService.registerTrigger(trigger)));
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
    public ApiResult<List<CdssTriggerPointResponse>> listTriggers(
            @RequestParam(required = false) String businessScenario,
            @RequestParam(required = false) String accessStrategy,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<CdssTriggerPointEntity> triggers = triggerPointService.listTriggers(
                resolveTenantId(orgCtx), businessScenario, accessStrategy);
        return ApiResult.success(triggers.stream()
                .map(CdssTriggerPointResponse::fromEntity)
                .collect(Collectors.toList()));
    }

    /**
     * 匹配触发点：根据业务场景匹配适用的触发点。
     */
    @Operation(summary = "Match triggers")
    @PostMapping("/match")
    public ApiResult<List<TriggerMatchResponse>> matchTriggers(
            @Valid @RequestBody TriggerMatchRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> body = toMatchBody(request);
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        String businessScenario = request.getBusinessScenario();
        List<Map<String, Object>> result = triggerPointService.matchTriggers(
                resolveTenantId(orgCtx), businessScenario, body);
        return ApiResult.success(result.stream()
                .map(TriggerMatchResponse::fromMap)
                .collect(Collectors.toList()));
    }

    /**
     * 执行触发点。
     */
    @Operation(summary = "Execute trigger")
    @PostMapping("/{triggerCode}/execute")
    public ApiResult<TriggerExecuteResponse> executeTrigger(
            @PathVariable String triggerCode,
            @Valid @RequestBody TriggerExecuteRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> eventData = toEventDataMap(request.getEventData());
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, eventData);
        return ApiResult.success(TriggerExecuteResponse.fromMap(
                triggerPointService.executeTrigger(resolveTenantId(orgCtx), triggerCode, eventData)));
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
            TriggerMatchContext ctx = request.getContext();
            if (ctx.getPatientId() != null) body.put("patient_id", ctx.getPatientId());
            if (ctx.getEncounterId() != null) body.put("encounter_id", ctx.getEncounterId());
            if (ctx.getTriggerPoint() != null) body.put("triggerPoint", ctx.getTriggerPoint());
            if (ctx.getAdditionalData() != null) body.putAll(ctx.getAdditionalData());
        }
        return body;
    }

    private Map<String, Object> toEventDataMap(TriggerExecuteEventData eventData) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (eventData != null) {
            if (eventData.getEventType() != null) map.put("eventType", eventData.getEventType());
            if (eventData.getPatientId() != null) map.put("patient_id", eventData.getPatientId());
            if (eventData.getEncounterId() != null) map.put("encounter_id", eventData.getEncounterId());
            if (eventData.getPayload() != null) map.putAll(eventData.getPayload());
        }
        return map;
    }

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }
}
