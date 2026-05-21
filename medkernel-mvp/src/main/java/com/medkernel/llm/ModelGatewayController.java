package com.medkernel.llm;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/model-gateway")
public class ModelGatewayController {

    private final ModelGatewayService gatewayService;
    private final OrganizationContextService orgContextService;

    public ModelGatewayController(ModelGatewayService gatewayService,
                                  OrganizationContextService orgContextService) {
        this.gatewayService = gatewayService;
        this.orgContextService = orgContextService;
    }

    @PostMapping("/invoke")
    public ApiResult<Map<String, Object>> invoke(
            @RequestParam("call_type") String callType,
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest) {
        if (callType == null || callType.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "call_type is required");
        }

        Map<String, Object> safeRequest = request != null ? request : new LinkedHashMap<String, Object>();

        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);

        Map<String, Object> result = gatewayService.invoke(callType.trim().toUpperCase(), safeRequest);
        return ApiResult.success(result);
    }

    @GetMapping("/providers")
    public ApiResult<List<Map<String, Object>>> listProviders() {
        List<Map<String, Object>> providers = gatewayService.listProviders();
        return ApiResult.success(providers);
    }

    @GetMapping("/degradation-chains")
    public ApiResult<Map<String, Object>> getDegradationChains(
            @RequestParam(value = "call_type", required = false) String callType) {
        if (callType != null && !callType.trim().isEmpty()) {
            return ApiResult.success(gatewayService.getDegradationChain(callType.trim().toUpperCase()));
        }
        Map<String, Object> allChains = new LinkedHashMap<String, Object>();
        String[] types = {"RESEARCH", "EXTRACT", "EMBEDDING", "RERANK", "CRITIC", "WORKFLOW"};
        for (String type : types) {
            allChains.put(type, gatewayService.getDegradationChain(type));
        }
        return ApiResult.success(allChains);
    }

    @GetMapping("/providers/{providerType}/status")
    public ApiResult<Map<String, Object>> getProviderStatus(@PathVariable("providerType") String providerType) {
        Map<String, Object> status = gatewayService.getProviderStatus(providerType.toUpperCase());
        return ApiResult.success(status);
    }
}
