package com.zyengine.terminology;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/terminology")
public class TerminologyController {
    private final TerminologyService terminologyService;

    public TerminologyController(TerminologyService terminologyService) {
        this.terminologyService = terminologyService;
    }

    @PostMapping("/normalize")
    public ApiResult<Map<String, Object>> normalize(@RequestBody Map<String, Object> request) {
        return ApiResult.success(terminologyService.normalize(request));
    }

    @PostMapping("/mappings")
    public ApiResult<List<Map<String, Object>>> importMappings(@RequestBody Object request) {
        return ApiResult.success(terminologyService.importMappings(request));
    }

    @GetMapping("/mappings")
    public ApiResult<List<Map<String, Object>>> listMappings() {
        return ApiResult.success(terminologyService.listMappings());
    }

    @GetMapping("/mappings/{sourceSystem}/{sourceCode}")
    public ApiResult<Map<String, Object>> getMapping(@PathVariable String sourceSystem,
                                                     @PathVariable String sourceCode,
                                                     @RequestParam String conceptType) {
        return ApiResult.success(terminologyService.getMapping(sourceSystem, sourceCode, conceptType));
    }

    /**
     * 查询未映射治理队列。
     * 支持按 governance_status / source_system / concept_type 过滤。
     */
    @GetMapping("/pending")
    public ApiResult<List<Map<String, Object>>> listPendingMappings(
            @RequestParam(required = false) String governanceStatus,
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(required = false) String conceptType,
            @RequestParam(required = false) String limit) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("governanceStatus", governanceStatus);
        filters.put("sourceSystem", sourceSystem);
        filters.put("conceptType", conceptType);
        filters.put("limit", limit);
        return ApiResult.success(terminologyService.listPendingMappings(filters));
    }

    /**
     * 审批映射：将 PENDING_MAPPING 记录标记为 APPROVED，并写入映射缓存。
     */
    @PostMapping("/pending/{queueId}/approve")
    public ApiResult<Map<String, Object>> approvePendingMapping(@PathVariable String queueId,
                                                                 @RequestBody Map<String, Object> request) {
        return ApiResult.success(terminologyService.approvePendingMapping(queueId, request));
    }

    /**
     * 驳回映射：将 PENDING_MAPPING 记录标记为 REJECTED。
     */
    @PostMapping("/pending/{queueId}/reject")
    public ApiResult<Map<String, Object>> rejectPendingMapping(@PathVariable String queueId,
                                                                 @RequestBody Map<String, Object> request) {
        return ApiResult.success(terminologyService.rejectPendingMapping(queueId, request));
    }
}
