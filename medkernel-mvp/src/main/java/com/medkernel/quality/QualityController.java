package com.medkernel.quality;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/quality")
public class QualityController {
    private final QualityService qualityService;
    private final OrganizationContextService organizationContextService;

    public QualityController(QualityService qualityService,
                             OrganizationContextService organizationContextService) {
        this.qualityService = qualityService;
        this.organizationContextService = organizationContextService;
    }

    @GetMapping("/metrics")
    public ApiResult<Map<String, Object>> metrics(@RequestParam(required = false) String pathwayCode,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String patientId,
                                                  @RequestParam(required = false) String encounterId,
                                                  @RequestParam(required = false) String currentNodeCode,
                                                  @RequestParam(required = false) String workflowCode,
                                                  @RequestParam(required = false) String workflowVersion,
                                                  @RequestParam(required = false) String workflowStatus,
                                                  HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("pathwayCode", pathwayCode);
        filters.put("status", status);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("currentNodeCode", currentNodeCode);
        filters.put("workflowCode", workflowCode);
        filters.put("workflowVersion", workflowVersion);
        filters.put("workflowStatus", workflowStatus);
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(qualityService.metrics(filters));
    }
}
