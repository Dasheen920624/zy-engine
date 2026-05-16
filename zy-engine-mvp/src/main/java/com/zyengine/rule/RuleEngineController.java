package com.zyengine.rule;

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

/**
 * 第三方独立调用入口：病历质控、医保质控、医嘱安全等场景统一从 /api/rule-engine/* 进入，
 * 与 /api/rules/* 的规则配置管理与内部演示接口解耦，便于后续灰度、限流和审计治理。
 */
@RestController
@RequestMapping("/api/rule-engine")
public class RuleEngineController {
    private final RuleService ruleService;

    public RuleEngineController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping("/evaluate")
    public ApiResult<Map<String, Object>> evaluate(@RequestBody Map<String, Object> request) {
        return ApiResult.success(ruleService.evaluateForScenario(request));
    }

    @PostMapping("/batch-evaluate")
    public ApiResult<Map<String, Object>> batchEvaluate(@RequestBody Map<String, Object> request) {
        return ApiResult.success(ruleService.batchEvaluateForScenario(request));
    }

    @GetMapping("/results")
    public ApiResult<List<Map<String, Object>>> listResults(@RequestParam(required = false) String scenarioCode,
                                                            @RequestParam(required = false) String packageCode,
                                                            @RequestParam(required = false) String batchId,
                                                            @RequestParam(required = false) String source,
                                                            @RequestParam(required = false) String patientId,
                                                            @RequestParam(required = false) String encounterId,
                                                            @RequestParam(required = false) String limit,
                                                            @RequestParam(required = false) String offset) {
        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put("scenarioCode", scenarioCode);
        filters.put("packageCode", packageCode);
        filters.put("batchId", batchId);
        filters.put("source", source);
        filters.put("patientId", patientId);
        filters.put("encounterId", encounterId);
        filters.put("limit", limit);
        filters.put("offset", offset);
        return ApiResult.success(ruleService.listEvaluations(filters));
    }

    @GetMapping("/results/{resultId}")
    public ApiResult<Map<String, Object>> getResult(@PathVariable String resultId) {
        return ApiResult.success(ruleService.getEvaluation(resultId));
    }
}
