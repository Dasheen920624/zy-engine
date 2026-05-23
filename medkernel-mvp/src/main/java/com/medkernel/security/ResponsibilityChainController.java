package com.medkernel.security;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SEC-005: 角色责任链审计控制器。
 */
@RestController
@RequestMapping("/api/security/responsibility-chain")
@Tag(name = "责任链审计", description = "SEC-005 关键操作责任人可追溯")
public class ResponsibilityChainController {

    private final ResponsibilityChainService responsibilityChainService;
    private final OrganizationContextService organizationContextService;

    public ResponsibilityChainController(ResponsibilityChainService responsibilityChainService,
                                          OrganizationContextService organizationContextService) {
        this.responsibilityChainService = responsibilityChainService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "记录责任链操作")
    @PostMapping("/actions")
    public ResponseEntity<ApiResult<Map<String, Object>>> recordAction(
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, request);
        String tenantId = filters.get("tenantId");

        String assetType = (String) body.get("asset_type");
        String assetCode = (String) body.get("asset_code");
        String action = (String) body.get("action");
        String operatorId = (String) body.getOrDefault("operator_id", request.getHeader("X-Operator-Id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> detail = (Map<String, Object>) body.get("detail");

        responsibilityChainService.recordAction(assetType, assetCode, action, operatorId, tenantId, detail);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("recorded", true);
        result.put("asset_type", assetType);
        result.put("asset_code", assetCode);
        result.put("action", action);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @Operation(summary = "查询责任链")
    @GetMapping("/{assetType}/{assetCode}")
    public ResponseEntity<ApiResult<Map<String, Object>>> getChain(
            @PathVariable String assetType, @PathVariable String assetCode,
            HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, request);
        String tenantId = filters.get("tenantId");
        return ResponseEntity.ok(ApiResult.success(responsibilityChainService.getChain(assetType, assetCode, tenantId)));
    }

    @Operation(summary = "查询资产操作者列表")
    @GetMapping("/{assetType}/{assetCode}/operators")
    public ResponseEntity<ApiResult<List<String>>> getOperators(
            @PathVariable String assetType, @PathVariable String assetCode,
            HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, request);
        String tenantId = filters.get("tenantId");
        return ResponseEntity.ok(ApiResult.success(responsibilityChainService.getOperators(assetType, assetCode, tenantId)));
    }
}
