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

/**
 * 规则引擎 REST 入口（GA-ENG-API-05 {@code /api/v1/engine/rules}）。
 *
 * <p>承担规则定义、版本、测试用例、试运行、发布、真实执行与诊断的 HTTP 合同；
 * 权限由 {@code @PreAuthorize} 强校验 {@code rule.read}/{@code rule.write}/{@code rule.publish}，
 * 租户隔离由类级 {@link DataScope}{@code (requireTenant=true)} 兜底。
 */
@RestController
@RequestMapping("/api/v1/engine/rules")
@DataScope(requireTenant = true)
public class RuleEngineController {

    private final RuleEngineService service;

    /**
     * 注入规则引擎应用服务，控制器仅负责 HTTP 合同和权限入口编排。
     */
    public RuleEngineController(RuleEngineService service) {
        this.service = service;
    }

    /**
     * 创建规则定义并生成初始草稿版本。
     *
     * <p>权限：{@code rule.write}；DSL 必须可解析且包含 trigger/when/then/explain，否则抛
     * {@link com.medkernel.shared.api.error.ApiException} 错误码 {@code ENG-RULE-001}。
     */
    @PostMapping
    @PreAuthorize("@perm.has('rule.write')")
    public ResponseEntity<ApiResult<RuleCreateResponse>> create(@RequestBody @Valid RuleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResult.ok(service.createRule(request)));
    }

    /**
     * 按状态、类型、风险级别分页查询规则定义。
     *
     * <p>权限：{@code rule.read}；过滤参数全部可选，{@code null} 表示不过滤。
     */
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

    /**
     * 查看指定规则的定义、当前版本及测试用例覆盖情况。
     *
     * <p>权限：{@code rule.read}；规则不存在抛错误码 {@code ENG-RULE-002}。
     */
    @GetMapping("/{ruleId}")
    @PreAuthorize("@perm.has('rule.read')")
    public ApiResult<RuleDetailResponse> detail(@PathVariable String ruleId) {
        return ApiResult.ok(service.detail(ruleId));
    }

    /**
     * 新增规则测试用例（仅草稿状态可加）。
     *
     * <p>权限：{@code rule.write}；规则状态不为 {@code DRAFT} 时抛错误码 {@code ENG-RULE-006}。
     */
    @PostMapping("/{ruleId}/test-cases")
    @PreAuthorize("@perm.has('rule.write')")
    public ResponseEntity<ApiResult<RuleTestCaseResponse>> addTestCase(
            @PathVariable String ruleId,
            @RequestBody @Valid RuleTestCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResult.ok(service.addTestCase(ruleId, request)));
    }

    /**
     * 用指定上下文试运行执行该规则的当前版本。
     *
     * <p>权限：{@code rule.write}；试运行同样写 {@code rule_execution_log}，便于回放与诊断。
     */
    @PostMapping("/{ruleId}/simulate")
    @PreAuthorize("@perm.has('rule.write')")
    public ApiResult<RuleEvaluationItem> simulate(
            @PathVariable String ruleId,
            @RequestBody @Valid RuleSimulateRequest request) {
        return ApiResult.ok(service.simulate(ruleId, request));
    }

    /**
     * 执行规则发布门禁并把规则推进到 {@code PUBLISHED}。
     *
     * <p>权限：{@code rule.publish}；要求阳性/阴性/边界/冲突四类测试用例齐备且全部 PASS，
     * 否则抛错误码 {@code ENG-RULE-004}。
     */
    @PostMapping("/{ruleId}/publish")
    @PreAuthorize("@perm.has('rule.publish')")
    public ApiResult<RulePublishResponse> publish(@PathVariable String ruleId) {
        return ApiResult.ok(service.publish(ruleId));
    }

    /**
     * 按触发点和上下文执行所有匹配的已发布规则。
     *
     * <p>权限：{@code rule.read}；返回命中明细、最高严重度与 traceId，供临床嵌入提示消费。
     */
    @PostMapping("/evaluate")
    @PreAuthorize("@perm.has('rule.read')")
    public ApiResult<RuleEvaluateResponse> evaluate(@RequestBody @Valid RuleEvaluateRequest request) {
        return ApiResult.ok(service.evaluate(request));
    }

    /**
     * 查看一次规则执行的可解释诊断响应（输入摘要、解释快照、状态历史等）。
     *
     * <p>权限：{@code rule.read}；执行记录不存在抛错误码 {@code ENG-RULE-002}。
     */
    @GetMapping("/executions/{executionId}/diagnose")
    @PreAuthorize("@perm.has('rule.read')")
    public ApiResult<DiagnoseResponse> diagnose(@PathVariable String executionId) {
        return ApiResult.ok(service.diagnose(executionId));
    }
}
