package com.zyengine.rule;

import com.zyengine.common.ApiResult;
import com.zyengine.dto.RuleResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
public class RuleController {
    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    public ApiResult<List<RuleDefinition>> importRules(@RequestBody Object request) {
        return ApiResult.success(ruleService.importRules(request));
    }

    @GetMapping
    public ApiResult<List<RuleDefinition>> listRules() {
        return ApiResult.success(ruleService.listRules());
    }

    @GetMapping("/{ruleCode}")
    public ApiResult<RuleDefinition> getRule(@PathVariable String ruleCode,
                                             @RequestParam(required = false) String versionNo) {
        return ApiResult.success(ruleService.getRule(ruleCode, versionNo));
    }

    @PostMapping("/{ruleCode}/publish")
    public ApiResult<RuleDefinition> publish(@PathVariable String ruleCode,
                                             @RequestBody Map<String, Object> request) {
        return ApiResult.success(ruleService.publish(ruleCode, request));
    }

    @PostMapping("/evaluate")
    public ApiResult<List<RuleResult>> evaluate(@RequestBody Map<String, Object> request) {
        return ApiResult.success(ruleService.evaluate(request));
    }

    @PostMapping("/simulate")
    public ApiResult<RuleResult> simulate(@RequestBody Map<String, Object> request) {
        return ApiResult.success(ruleService.simulate(request));
    }
}
