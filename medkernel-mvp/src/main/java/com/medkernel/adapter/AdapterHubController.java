package com.medkernel.adapter;

import com.medkernel.adapter.dto.AdapterDefinitionResponse;
import com.medkernel.adapter.dto.AdapterQueryResponse;
import com.medkernel.common.ApiResult;
import com.medkernel.dto.AdapterQueryRequest;
import com.medkernel.dto.AdapterDefinitionImportRequest;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

@Tag(name = "Adapter Hub")
@RestController
@RequestMapping("/api/adapters")
public class AdapterHubController {
    private final AdapterHubService adapterHubService;
    private final OrganizationContextService organizationContextService;

    public AdapterHubController(AdapterHubService adapterHubService,
                                OrganizationContextService organizationContextService) {
        this.adapterHubService = adapterHubService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Query")
    @PostMapping("/query")
    public ApiResult<AdapterQueryResponse> query(@Valid @RequestBody AdapterQueryRequest request,
                                                 HttpServletRequest httpRequest) {
        Map<String, Object> body = toMap(request);
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, body);
        return ApiResult.success(AdapterQueryResponse.fromMap(
                adapterHubService.query(body, orgCtx.getTenantId(), orgCtx.getHospitalCode())));
    }

    @Operation(summary = "Import definitions")
    @PostMapping("/definitions")
    public ApiResult<List<AdapterDefinitionResponse>> importDefinitions(
            @Valid @RequestBody AdapterDefinitionImportRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> result = adapterHubService.importDefinitions(request, orgCtx.getTenantId(), orgCtx.getHospitalCode());
        return ApiResult.success(result.stream()
                .map(AdapterDefinitionResponse::fromMap)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "List definitions")
    @GetMapping("/definitions")
    public ApiResult<List<AdapterDefinitionResponse>> listDefinitions(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        List<Map<String, Object>> result = adapterHubService.listDefinitions(orgCtx.getTenantId(), orgCtx.getHospitalCode());
        return ApiResult.success(result.stream()
                .map(AdapterDefinitionResponse::fromMap)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "Get definition")
    @GetMapping("/definitions/{adapterCode}/{queryCode}")
    public ApiResult<AdapterDefinitionResponse> getDefinition(@PathVariable String adapterCode,
                                                        @PathVariable String queryCode,
                                                        HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(AdapterDefinitionResponse.fromMap(
                adapterHubService.getDefinition(adapterCode, queryCode, orgCtx.getTenantId(), orgCtx.getHospitalCode())));
    }

    private Map<String, Object> toMap(AdapterQueryRequest request) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("adapter_code", request.getAdapter_code());
        map.put("query_code", request.getQuery_code());
        if (request.getParams() != null) {
            map.put("params", request.getParams());
        }
        return map;
    }
}
