package com.medkernel.adapter;

import com.medkernel.adapter.dto.InteropAdapterListItemResponse;
import com.medkernel.adapter.dto.InteropQueryResponse;
import com.medkernel.common.ApiResult;
import com.medkernel.dto.InteropQueryRequest;
import com.medkernel.dto.CdsHooksQueryRequest;
import com.medkernel.dto.SmartAppQueryRequest;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
 * 互联互通标准适配器REST接口
 * 暴露HL7 v2、FHIR、CDA、IHE、CDS Hooks、SMART on FHIR、DICOM等标准的查询接口
 */
@Tag(name = "Interop")
@RestController
@RequestMapping("/api/interop")
public class InteropController {
    private final InteropAdapterService interopAdapterService;
    private final OrganizationContextService organizationContextService;

    public InteropController(InteropAdapterService interopAdapterService,
                             OrganizationContextService organizationContextService) {
        this.interopAdapterService = interopAdapterService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 查询互联互通适配器（HL7/FHIR/CDA/DICOM/IHE/REST）
     */
    @Operation(summary = "Query")
    @PostMapping("/query")
    public ApiResult<InteropQueryResponse> query(@Valid @RequestBody InteropQueryRequest request,
                                                 HttpServletRequest httpRequest) {
        Map<String, Object> body = toInteropMap(request);
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        return ApiResult.success(InteropQueryResponse.fromMap(
                interopAdapterService.query(body, orgCtx.getTenantId(), orgCtx.getHospitalCode())));
    }

    /**
     * 查询CDS Hooks服务
     */
    @Operation(summary = "Query cds hooks")
    @PostMapping("/cds-hooks")
    public ApiResult<InteropQueryResponse> queryCdsHooks(@Valid @RequestBody CdsHooksQueryRequest request,
                                                         HttpServletRequest httpRequest) {
        Map<String, Object> body = toCdsHooksMap(request);
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        return ApiResult.success(InteropQueryResponse.fromMap(
                interopAdapterService.queryCdsHooks(body, orgCtx.getTenantId(), orgCtx.getHospitalCode())));
    }

    /**
     * 查询SMART on FHIR应用
     */
    @Operation(summary = "Query smart app")
    @PostMapping("/smart-apps")
    public ApiResult<InteropQueryResponse> querySmartApp(@Valid @RequestBody SmartAppQueryRequest request,
                                                         HttpServletRequest httpRequest) {
        Map<String, Object> body = toSmartAppMap(request);
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        return ApiResult.success(InteropQueryResponse.fromMap(
                interopAdapterService.querySmartApp(body, orgCtx.getTenantId(), orgCtx.getHospitalCode())));
    }

    /**
     * 列出所有互联互通适配器定义
     */
    @Operation(summary = "List interop adapters")
    @GetMapping("/adapters")
    public ApiResult<List<InteropAdapterListItemResponse>> listInteropAdapters(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> result = interopAdapterService.listInteropAdapters(orgCtx.getTenantId(), orgCtx.getHospitalCode());
        return ApiResult.success(result.stream()
                .map(InteropAdapterListItemResponse::fromMap)
                .collect(Collectors.toList()));
    }

    /**
     * 列出所有CDS Hooks服务定义
     */
    @Operation(summary = "List cds hooks services")
    @GetMapping("/cds-hooks")
    public ApiResult<List<InteropAdapterListItemResponse>> listCdsHooksServices(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> result = interopAdapterService.listCdsHooksServices(orgCtx.getTenantId(), orgCtx.getHospitalCode());
        return ApiResult.success(result.stream()
                .map(InteropAdapterListItemResponse::fromMap)
                .collect(Collectors.toList()));
    }

    /**
     * 列出所有SMART on FHIR应用定义
     */
    @Operation(summary = "List smart apps")
    @GetMapping("/smart-apps")
    public ApiResult<List<InteropAdapterListItemResponse>> listSmartApps(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> result = interopAdapterService.listSmartApps(orgCtx.getTenantId(), orgCtx.getHospitalCode());
        return ApiResult.success(result.stream()
                .map(InteropAdapterListItemResponse::fromMap)
                .collect(Collectors.toList()));
    }

    private Map<String, Object> toInteropMap(InteropQueryRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("adapter_code", request.getAdapter_code());
        map.put("query_code", request.getQuery_code());
        if (request.getParams() != null) {
            map.put("params", request.getParams());
        }
        return map;
    }

    private Map<String, Object> toCdsHooksMap(CdsHooksQueryRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("hook_id", request.getHook_id());
        if (request.getHook_type() != null) {
            map.put("hook_type", request.getHook_type());
        }
        if (request.getPatient_id() != null) {
            map.put("patient_id", request.getPatient_id());
        }
        if (request.getEncounter_id() != null) {
            map.put("encounter_id", request.getEncounter_id());
        }
        if (request.getContext() != null) {
            map.putAll(request.getContext());
        }
        return map;
    }

    private Map<String, Object> toSmartAppMap(SmartAppQueryRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("launch_id", request.getLaunch_id());
        if (request.getClient_id() != null) {
            map.put("client_id", request.getClient_id());
        }
        if (request.getPatient_id() != null) {
            map.put("patient_id", request.getPatient_id());
        }
        if (request.getEncounter_id() != null) {
            map.put("encounter_id", request.getEncounter_id());
        }
        if (request.getContext() != null) {
            map.putAll(request.getContext());
        }
        return map;
    }
}
