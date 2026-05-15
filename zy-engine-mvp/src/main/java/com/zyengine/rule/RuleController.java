package com.zyengine.rule;

import com.zyengine.common.ApiResult;
import com.zyengine.dto.RuleResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/evaluate")
    public ApiResult<List<RuleResult>> evaluate(@RequestBody Map<String, Object> request) {
        return ApiResult.success(ruleService.evaluate(request));
    }

    @PostMapping("/simulate")
    public ApiResult<RuleResult> simulate(@RequestBody Map<String, Object> request) {
        return ApiResult.success(ruleService.simulate(request));
    }
}

