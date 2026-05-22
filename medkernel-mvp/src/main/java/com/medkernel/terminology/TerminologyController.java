package com.medkernel.terminology;

import com.medkernel.common.ApiResult;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Terminology")
@RestController
@RequestMapping("/api/terminology")
public class TerminologyController {
    private final TerminologyService terminologyService;
    private final OrganizationContextService organizationContextService;

    public TerminologyController(TerminologyService terminologyService,
                                 OrganizationContextService organizationContextService) {
        this.terminologyService = terminologyService;
        this.organizationContextService = organizationContextService;
    }

    @Operation(summary = "Normalize")
    @PostMapping("/normalize")
    public ApiResult<Map<String, Object>> normalize(@RequestBody Map<String, Object> request,
                                                     HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(terminologyService.normalize(request));
    }

    @Operation(summary = "Import mappings")
    @PostMapping("/mappings")
    public ApiResult<List<Map<String, Object>>> importMappings(@RequestBody Object request,
                                                                HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(terminologyService.importMappings(request));
    }

    @Operation(summary = "List mappings")
    @GetMapping("/mappings")
    public ApiResult<List<Map<String, Object>>> listMappings(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(terminologyService.listMappings());
    }

    @Operation(summary = "Get mapping")
    @GetMapping("/mappings/{sourceSystem}/{sourceCode}")
    public ApiResult<Map<String, Object>> getMapping(@PathVariable String sourceSystem,
                                                     @PathVariable String sourceCode,
                                                     @RequestParam String conceptType,
                                                     HttpServletRequest httpRequest) {
        validatePathToken("sourceSystem", sourceSystem);
        validatePathToken("sourceCode", sourceCode);
        validatePathToken("conceptType", conceptType);
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(terminologyService.getMapping(sourceSystem, sourceCode, conceptType));
    }

    /**
     * 校验 path 与 query 字段的长度与字符集，避免传入空字符串或异常超长字符串导致后端 store key 异常。
     * 限定 1-128 字符、字母/数字/下划线/横线/点；不符合要求直接 400 拒绝。
     */
    private static void validatePathToken(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 128) {
            throw new IllegalArgumentException(fieldName + " exceeds max length 128");
        }
        if (!trimmed.matches("[A-Za-z0-9._\\-]+")) {
            throw new IllegalArgumentException(fieldName + " contains invalid characters");
        }
    }

    /**
     * 查询未映射治理队列。
     * 支持按 governance_status / source_system / concept_type 过滤。
     */
    @Operation(summary = "List pending mappings")
    @GetMapping("/pending")
    public ApiResult<List<Map<String, Object>>> listPendingMappings(
            @RequestParam(required = false) String governanceStatus,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) String conceptType,
            @RequestParam(required = false) String limit,
            HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("governanceStatus", governanceStatus);
        filters.put("sourceSystem", sourceSystem);
        filters.put("conceptType", conceptType);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(terminologyService.listPendingMappings(filters));
    }

    /**
     * 审批映射：将 PENDING_MAPPING 记录标记为 APPROVED，并写入映射缓存。
     */
    @Operation(summary = "Approve pending mapping")
    @PostMapping("/pending/{queueId}/approve")
    public ApiResult<Map<String, Object>> approvePendingMapping(@PathVariable String queueId,
                                                                 @RequestBody Map<String, Object> request,
                                                                 HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(terminologyService.approvePendingMapping(queueId, request));
    }

    /**
     * 驳回映射：将 PENDING_MAPPING 记录标记为 REJECTED。
     */
    @Operation(summary = "Reject pending mapping")
    @PostMapping("/pending/{queueId}/reject")
    public ApiResult<Map<String, Object>> rejectPendingMapping(@PathVariable String queueId,
                                                                @RequestBody Map<String, Object> request,
                                                                HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(terminologyService.rejectPendingMapping(queueId, request));
    }
}
