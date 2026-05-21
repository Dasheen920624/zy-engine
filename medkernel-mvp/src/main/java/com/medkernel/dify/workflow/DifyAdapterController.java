package com.medkernel.dify.workflow;

import com.medkernel.common.ApiResult;
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
@RequestMapping("/api/dify")
public class DifyAdapterController {
    private final DifyService difyService;
    private final OrganizationContextService organizationContextService;

    public DifyAdapterController(DifyService difyService,
                                 OrganizationContextService organizationContextService) {
        this.difyService = difyService;
        this.organizationContextService = organizationContextService;
    }

    @PostMapping("/workflows/run")
    public ApiResult<Map<String, Object>> run(@RequestBody Map<String, Object> request,
                                               HttpServletRequest httpRequest) {
        organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(difyService.runWorkflow(request));
    }

    @PostMapping("/workflows")
    public ApiResult<List<DifyWorkflowTemplate>> importTemplates(@RequestBody Object request,
                                                                  HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(difyService.importTemplates(request));
    }

    @GetMapping("/workflows")
    public ApiResult<List<DifyWorkflowTemplate>> listTemplates(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(difyService.listTemplates());
    }

    @GetMapping("/workflows/stats")
    public ApiResult<Map<String, Object>> invocationStats(@RequestParam(required = false) String workflowCode,
                                                          @RequestParam(required = false) String workflowVersion,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String provider,
                                                          @RequestParam(required = false) String patientId,
                                                          @RequestParam(required = false) String encounterId,
                                                          @RequestParam(required = false) String limit,
                                                          HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("workflowCode", workflowCode);
        filters.put("workflowVersion", workflowVersion);
        filters.put("status", status);
        filters.put("provider", provider);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(difyService.summarizeInvocations(filters));
    }

    @GetMapping("/workflows/{workflowCode}")
    public ApiResult<DifyWorkflowTemplate> getTemplate(@PathVariable String workflowCode,
                                                       @RequestParam(required = false) String workflowVersion,
                                                       HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        return ApiResult.success(difyService.getTemplate(workflowCode, workflowVersion));
    }
}
