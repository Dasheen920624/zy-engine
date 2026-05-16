package com.zyengine.rule;

import com.zyengine.common.ApiResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
