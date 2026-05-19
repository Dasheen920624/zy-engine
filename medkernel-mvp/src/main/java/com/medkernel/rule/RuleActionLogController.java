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
 * 规则动作日志控制器
 * 提供决策记录和查询的REST API
 */
@RestController
@RequestMapping("/api/rule-action-logs")
public class RuleActionLogController {
    private final RuleActionLogService actionLogService;
    private final OrganizationContextService organizationContextService;

    public RuleActionLogController(RuleActionLogService actionLogService,
                                   OrganizationContextService organizationContextService) {
        this.actionLogService = actionLogService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 记录用户决策
     */
    @PostMapping
    public ApiResult<RuleActionLog> recordDecision(@RequestBody Map<String, Object> request,
                                                   HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        RuleActionLog log = actionLogService.recordDecision(request, orgContext);
        return ApiResult.success(log);
    }

    /**
     * 查询决策日志列表
     */
    @GetMapping
    public ApiResult<List<RuleActionLog>> listActionLogs(@RequestParam(required = false) String patientId,
                                                         @RequestParam(required = false) String encounterId,
                                                         @RequestParam(required = false) String ruleCode,
                                                         @RequestParam(required = false) String decision,
                                                         @RequestParam(required = false) String decisionBy,
                                                         @RequestParam(required = false) String limit,
                                                         HttpServletRequest request) {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("patient_id", patientId);
        filters.put("encounter_id", encounterId);
        filters.put("rule_code", ruleCode);
        filters.put("decision", decision);
        filters.put("decision_by", decisionBy);
        filters.put("limit", limit);
        return ApiResult.success(actionLogService.listAll(filters));
    }

    /**
     * 根据ID获取决策日志
     */
    @GetMapping("/{logId}")
    public ApiResult<RuleActionLog> getActionLog(@PathVariable String logId) {
        RuleActionLog log = actionLogService.getById(logId);
        if (log == null) {
            return ApiResult.error("DECISION_LOG_NOT_FOUND", "决策日志不存在: " + logId);
        }
        return ApiResult.success(log);
    }

    /**
     * 根据患者查询决策日志
     */
    @GetMapping("/patient/{patientId}")
    public ApiResult<List<RuleActionLog>> getActionLogsByPatient(@PathVariable String patientId,
                                                                 @RequestParam(required = false) String encounterId) {
        return ApiResult.success(actionLogService.queryByPatient(patientId, encounterId));
    }

    /**
     * 根据订单查询决策日志
     */
    @GetMapping("/order/{orderId}")
    public ApiResult<List<RuleActionLog>> getActionLogsByOrder(@PathVariable String orderId) {
        return ApiResult.success(actionLogService.queryByOrder(orderId));
    }
}