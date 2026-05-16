package com.zyengine.audit;

import com.zyengine.common.ApiResult;
import com.zyengine.organization.OrganizationContextService;
import com.zyengine.persistence.EnginePersistenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {
    private final EnginePersistenceService persistenceService;
    private final OrganizationContextService organizationContextService;

    public AuditController(EnginePersistenceService persistenceService,
                           OrganizationContextService organizationContextService) {
        this.persistenceService = persistenceService;
        this.organizationContextService = organizationContextService;
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(required = false) String traceId,
                                                     @RequestParam(required = false) String engineType,
                                                     @RequestParam(required = false) String actionType,
                                                     @RequestParam(required = false) String targetType,
                                                     @RequestParam(required = false) String targetCode,
                                                     @RequestParam(required = false) String patientId,
                                                     @RequestParam(required = false) String encounterId,
                                                     @RequestParam(required = false) String operatorId,
                                                     @RequestParam(required = false) String limit,
                                                     HttpServletRequest request) {
        Map<String, String> filters = filters(traceId, engineType, actionType,
                targetType, targetCode, patientId, encounterId, operatorId, limit);
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(persistenceService.listAuditLogs(filters));
    }

    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> summary(@RequestParam(required = false) String traceId,
                                                  @RequestParam(required = false) String engineType,
                                                  @RequestParam(required = false) String actionType,
                                                  @RequestParam(required = false) String targetType,
                                                  @RequestParam(required = false) String targetCode,
                                                  @RequestParam(required = false) String patientId,
                                                  @RequestParam(required = false) String encounterId,
                                                  @RequestParam(required = false) String operatorId,
                                                  HttpServletRequest request) {
        Map<String, String> filters = filters(traceId, engineType, actionType,
                targetType, targetCode, patientId, encounterId, operatorId, null);
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(persistenceService.summarizeAuditLogs(filters));
    }

    private Map<String, String> filters(String traceId, String engineType, String actionType,
                                        String targetType, String targetCode, String patientId,
                                        String encounterId, String operatorId, String limit) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("traceId", traceId);
        filters.put("engineType", engineType);
        filters.put("actionType", actionType);
        filters.put("targetType", targetType);
        filters.put("targetCode", targetCode);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("operatorId", operatorId);
        filters.put("limit", limit);
        return filters;
    }
}
