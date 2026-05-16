package com.zyengine.rule;

import com.zyengine.common.ApiResult;
import com.zyengine.dto.RuleResult;
import com.zyengine.organization.OrganizationContext;
import com.zyengine.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
public class RuleController {
    private final RuleService ruleService;
    private final OrganizationContextService organizationContextService;

    public RuleController(RuleService ruleService,
                          OrganizationContextService organizationContextService) {
        this.ruleService = ruleService;
        this.organizationContextService = organizationContextService;
    }

    @PostMapping
    public ApiResult<List<RuleDefinition>> importRules(@RequestBody Object request,
                                                       HttpServletRequest httpRequest) {
        return ApiResult.success(ruleService.importRules(request, resolveWithBody(httpRequest, request)));
    }

    @GetMapping
    public ApiResult<List<RuleDefinition>> listRules(HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(ruleService.listRules(filters));
    }

    @GetMapping("/{ruleCode}")
    public ApiResult<RuleDefinition> getRule(@PathVariable String ruleCode,
                                             @RequestParam(required = false) String versionNo,
                                             HttpServletRequest request) {
        return ApiResult.success(ruleService.getRule(ruleCode, versionNo, organizationContextService.resolve(request)));
    }

    @PostMapping("/{ruleCode}/publish")
    public ApiResult<RuleDefinition> publish(@PathVariable String ruleCode,
                                             @RequestBody Map<String, Object> request,
                                             HttpServletRequest httpRequest) {
        return ApiResult.success(ruleService.publish(ruleCode, request,
                organizationContextService.resolveWithBody(httpRequest, request)));
    }

    @GetMapping("/packages/{packageCode}/review")
    public ApiResult<Map<String, Object>> reviewPackage(@PathVariable String packageCode,
                                                        @RequestParam(required = false) String packageVersion,
                                                        HttpServletRequest request) {
        return ApiResult.success(ruleService.reviewPackage(packageCode, packageVersion,
                organizationContextService.resolve(request)));
    }

    @PostMapping("/packages/{packageCode}/publish")
    public ApiResult<Map<String, Object>> publishPackage(@PathVariable String packageCode,
                                                         @RequestBody(required = false) Map<String, Object> request,
                                                         HttpServletRequest httpRequest) {
        return ApiResult.success(ruleService.publishPackage(packageCode, request,
                organizationContextService.resolveWithBody(httpRequest,
                        request == null ? new LinkedHashMap<String, Object>() : request)));
    }

    @PostMapping("/evaluate")
    public ApiResult<List<RuleResult>> evaluate(@RequestBody Map<String, Object> request,
                                                HttpServletRequest httpRequest) {
        return ApiResult.success(ruleService.evaluate(request,
                organizationContextService.resolveWithBody(httpRequest, request)));
    }

    @PostMapping("/simulate")
    public ApiResult<RuleResult> simulate(@RequestBody Map<String, Object> request,
                                          HttpServletRequest httpRequest) {
        return ApiResult.success(ruleService.simulate(request,
                organizationContextService.resolveWithBody(httpRequest, request)));
    }

    @GetMapping("/exec-logs")
    public ApiResult<List<RuleExecLogEntry>> listExecLogs(@RequestParam(required = false) String ruleCode,
                                                          @RequestParam(required = false) String traceId,
                                                          @RequestParam(required = false) String patientId,
                                                          @RequestParam(required = false) String encounterId,
                                                          @RequestParam(required = false) String resultStatus,
                                                          @RequestParam(required = false) String hit,
                                                          @RequestParam(required = false) String limit,
                                                          HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("ruleCode", ruleCode);
        filters.put("traceId", traceId);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("resultStatus", resultStatus);
        filters.put("hit", hit);
        filters.put("limit", limit);
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(ruleService.listExecLogs(filters));
    }

    @GetMapping("/exec-logs/summary")
    public ApiResult<Map<String, Object>> execLogSummary(@RequestParam(required = false) String ruleCode,
                                                         @RequestParam(required = false) String traceId,
                                                         @RequestParam(required = false) String patientId,
                                                         @RequestParam(required = false) String encounterId,
                                                         @RequestParam(required = false) String resultStatus,
                                                         @RequestParam(required = false) String hit,
                                                         HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("ruleCode", ruleCode);
        filters.put("traceId", traceId);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("resultStatus", resultStatus);
        filters.put("hit", hit);
        organizationContextService.applyExplicitFilters(filters, request);
        return ApiResult.success(ruleService.summarizeExecLogs(filters));
    }

    @GetMapping("/exec-logs/{logId}")
    public ApiResult<RuleExecLogEntry> getExecLog(@PathVariable String logId) {
        return ApiResult.success(ruleService.getExecLog(logId));
    }

    private OrganizationContext resolveWithBody(HttpServletRequest httpRequest, Object body) {
        if (body instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) body;
            return organizationContextService.resolveWithBody(httpRequest, map);
        }
        return organizationContextService.resolve(httpRequest);
    }
}
