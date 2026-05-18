package com.medkernel.rule;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContext;
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

/**
 * 第三方独立调用入口：病历质控、医保质控、医嘱安全等场景统一从 /api/rule-engine/* 进入，
 * 与 /api/rules/* 的规则配置管理与内部演示接口解耦，便于后续灰度、限流和审计治理。
 *
 * 组织维度统一通过 OrganizationContextService 解析：Header/Query 提供默认，Body 字段优先覆盖。
 */
@RestController
@RequestMapping("/api/rule-engine")
public class RuleEngineController {
    private final RuleService ruleService;
    private final OrganizationContextService organizationContextService;

    public RuleEngineController(RuleService ruleService,
                                 OrganizationContextService organizationContextService) {
        this.ruleService = ruleService;
        this.organizationContextService = organizationContextService;
    }

    @PostMapping("/evaluate")
    public ApiResult<Map<String, Object>> evaluate(@RequestBody Map<String, Object> request,
                                                   HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(ruleService.evaluateForScenario(request, orgContext));
    }

    @PostMapping("/batch-evaluate")
    public ApiResult<Map<String, Object>> batchEvaluate(@RequestBody Map<String, Object> request,
                                                        HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        return ApiResult.success(ruleService.batchEvaluateForScenario(request, orgContext));
    }

    @GetMapping("/results")
    public ApiResult<List<Map<String, Object>>> listResults(@RequestParam(required = false) String scenarioCode,
                                                            @RequestParam(required = false) String packageCode,
                                                            @RequestParam(required = false) String batchId,
                                                            @RequestParam(required = false) String source,
                                                            @RequestParam(required = false) String patientId,
                                                            @RequestParam(required = false) String encounterId,
                                                            @RequestParam(required = false) String tenantId,
                                                            @RequestParam(required = false) String groupCode,
                                                            @RequestParam(required = false) String hospitalCode,
                                                            @RequestParam(required = false) String campusCode,
                                                            @RequestParam(required = false) String siteCode,
                                                            @RequestParam(required = false) String departmentCode,
                                                            @RequestParam(required = false) String scopeLevel,
                                                            @RequestParam(required = false) String scopeCode,
                                                            @RequestParam(required = false) String limit,
                                                            @RequestParam(required = false) String offset) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("scenarioCode", scenarioCode);
        filters.put("packageCode", packageCode);
        filters.put("batchId", batchId);
        filters.put("source", source);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("tenantId", tenantId);
        filters.put("groupCode", groupCode);
        filters.put("hospitalCode", hospitalCode);
        filters.put("campusCode", campusCode);
        filters.put("siteCode", siteCode);
        filters.put("departmentCode", departmentCode);
        filters.put("scopeLevel", scopeLevel);
        filters.put("scopeCode", scopeCode);
        filters.put("limit", limit);
        filters.put("offset", offset);
        return ApiResult.success(ruleService.listEvaluations(filters));
    }

    @GetMapping("/results/{resultId}")
    public ApiResult<Map<String, Object>> getResult(@PathVariable String resultId) {
        return ApiResult.success(ruleService.getEvaluation(resultId));
    }
}
