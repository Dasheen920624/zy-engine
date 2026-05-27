package com.medkernel.engine.rule;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.medkernel.shared.api.ApiResult;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.datascope.DataScope;
import com.medkernel.shared.observability.DiagnoseResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/engine/rules")
@DataScope(requireTenant = true)
public class RuleEngineController {

    private final RuleEngineService service;

    public RuleEngineController(RuleEngineService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("@perm.has('rule.write')")
    public ResponseEntity<ApiResult<RuleCreateResponse>> create(@RequestBody @Valid RuleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResult.ok(service.createRule(request)));
    }

    @GetMapping
    @PreAuthorize("@perm.has('rule.read')")
    public ApiResult<PageResponse<RuleDefinition>> list(
            @RequestParam(required = false) RuleDefinitionStatus status,
            @RequestParam(required = false) RuleType ruleType,
            @RequestParam(required = false) RuleRiskLevel riskLevel,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResult.ok(service.list(
            new RuleFilter(status, ruleType, riskLevel),
            new PageRequest(page, size, sort)));
    }

    @GetMapping("/{ruleId}")
    @PreAuthorize("@perm.has('rule.read')")
    public ApiResult<RuleDetailResponse> detail(@PathVariable String ruleId) {
        return ApiResult.ok(service.detail(ruleId));
    }

    @PostMapping("/{ruleId}/test-cases")
    @PreAuthorize("@perm.has('rule.write')")
    public ResponseEntity<ApiResult<RuleTestCaseResponse>> addTestCase(
            @PathVariable String ruleId,
            @RequestBody @Valid RuleTestCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResult.ok(service.addTestCase(ruleId, request)));
    }

    @PostMapping("/{ruleId}/simulate")
    @PreAuthorize("@perm.has('rule.write')")
    public ApiResult<RuleEvaluationItem> simulate(
            @PathVariable String ruleId,
            @RequestBody @Valid RuleSimulateRequest request) {
        return ApiResult.ok(service.simulate(ruleId, request));
    }

    @PostMapping("/{ruleId}/publish")
    @PreAuthorize("@perm.has('rule.publish')")
    public ApiResult<RulePublishResponse> publish(@PathVariable String ruleId) {
        return ApiResult.ok(service.publish(ruleId));
    }

    @PostMapping("/evaluate")
    @PreAuthorize("@perm.has('rule.read')")
    public ApiResult<RuleEvaluateResponse> evaluate(@RequestBody @Valid RuleEvaluateRequest request) {
        return ApiResult.ok(service.evaluate(request));
    }

    @GetMapping("/executions/{executionId}/diagnose")
    @PreAuthorize("@perm.has('rule.read')")
    public ApiResult<DiagnoseResponse> diagnose(@PathVariable String executionId) {
        return ApiResult.ok(service.diagnose(executionId));
    }
}
