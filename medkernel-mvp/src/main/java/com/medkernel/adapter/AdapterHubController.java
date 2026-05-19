package com.medkernel.adapter;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/query")
    public ApiResult<Map<String, Object>> query(@RequestBody Map<String, Object> request,
                                                 HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(adapterHubService.query(request, orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }

    @PostMapping("/definitions")
    public ApiResult<List<Map<String, Object>>> importDefinitions(@RequestBody Object request,
                                                                   HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(adapterHubService.importDefinitions(request, orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }

    @GetMapping("/definitions")
    public ApiResult<List<Map<String, Object>>> listDefinitions(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(adapterHubService.listDefinitions(orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }

    @GetMapping("/definitions/{adapterCode}/{queryCode}")
    public ApiResult<Map<String, Object>> getDefinition(@PathVariable String adapterCode,
                                                        @PathVariable String queryCode,
                                                        HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(adapterHubService.getDefinition(adapterCode, queryCode, orgCtx.getTenantId(), orgCtx.getHospitalCode()));
    }
}
