package com.medkernel.engine.rule;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.shared.api.PageRequest;
import com.medkernel.shared.api.PageResponse;
import com.medkernel.shared.api.error.ApiException;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.audit.AuditAction;
import com.medkernel.shared.audit.AuditEventPublisher;
import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;
import com.medkernel.shared.observability.DiagnoseResponse;
import com.medkernel.shared.observability.DiagnoseResponseAssembler;
import com.medkernel.shared.observability.PayloadRef;
import com.medkernel.shared.observability.StateTransitionRecorder;

@Service
public class RuleEngineService {

    private static final String RULE_ENTITY = "rule_definition";
    private static final String EXECUTION_ENTITY = "rule_execution";
    private static final EnumSet<RuleTestCaseType> REQUIRED_CASE_TYPES =
        EnumSet.of(RuleTestCaseType.POSITIVE, RuleTestCaseType.NEGATIVE,
            RuleTestCaseType.BOUNDARY, RuleTestCaseType.CONFLICT);

    private final RuleDefinitionRepository definitions;
    private final RuleVersionRepository versions;
    private final RuleTestCaseRepository testCases;
    private final RuleExecutionLogRepository executions;
    private final RuleDslEvaluator evaluator;
    private final AuditEventPublisher auditPublisher;
    private final StateTransitionRecorder transitions;
    private final DiagnoseResponseAssembler diagnoseAssembler;
    private final ObjectMapper json;

    public RuleEngineService(RuleDefinitionRepository definitions,
                             RuleVersionRepository versions,
                             RuleTestCaseRepository testCases,
                             RuleExecutionLogRepository executions,
                             RuleDslEvaluator evaluator,
                             AuditEventPublisher auditPublisher,
                             StateTransitionRecorder transitions,
                             DiagnoseResponseAssembler diagnoseAssembler,
                             ObjectMapper json) {
        this.definitions = definitions;
        this.versions = versions;
        this.testCases = testCases;
        this.executions = executions;
        this.evaluator = evaluator;
        this.auditPublisher = auditPublisher;
        this.transitions = transitions;
        this.diagnoseAssembler = diagnoseAssembler;
        this.json = json;
    }

    @Transactional
    public RuleCreateResponse createRule(RuleCreateRequest request) {
        String tenantId = requireCurrentTenant();
        String traceId = RequestContext.currentTraceId();
        String actor = RequestContext.currentUserId().orElse("system");
        Instant now = Instant.now();
        validateDsl(request.dsl());

        String ruleId = "rule-" + UUID.randomUUID();
        String versionId = "rv-" + UUID.randomUUID();
        RuleDefinition definition = new RuleDefinition(
            null, ruleId, tenantId, request.ruleCode(), request.name(), request.ruleType(),
            request.authoringMode() == null ? RuleAuthoringMode.DSL : request.authoringMode(),
            request.riskLevel() == null ? RuleRiskLevel.MEDIUM : request.riskLevel(),
            RuleDefinitionStatus.DRAFT, versionId, request.packageVersion(), request.applicableOrgUnitId(),
            now, actor, now, actor, traceId);
        RuleVersion version = new RuleVersion(
            null, versionId, tenantId, ruleId, 1, request.sourceRef(), request.changeSummary(),
            writeJson(request.dsl()), writeJson(request.explanation()),
            RuleVersionStatus.DRAFT, null, null, null, now, actor, now, actor, traceId);

        definitions.save(definition);
        versions.save(version);
        transitions.record(RULE_ENTITY, ruleId, null, RuleDefinitionStatus.DRAFT.name(), "CREATE_RULE", null);
        auditPublisher.publish(AuditAction.CREATE, RULE_ENTITY, ruleId, "创建规则 " + request.ruleCode());
        return new RuleCreateResponse(ruleId, versionId, RuleDefinitionStatus.DRAFT, traceId);
    }

    @Transactional(readOnly = true)
    public RuleDetailResponse detail(String ruleId) {
        String tenantId = requireCurrentTenant();
        RuleDefinition rule = findRule(ruleId, tenantId);
        RuleVersion version = findVersion(rule.activeVersionId(), tenantId);
        return new RuleDetailResponse(
            rule, version, testCases.findByVersionIdAndTenantIdOrderByCreatedAtAsc(version.versionId(), tenantId));
    }

    @Transactional(readOnly = true)
    public PageResponse<RuleDefinition> list(RuleFilter filter, PageRequest page) {
        String tenantId = requireCurrentTenant();
        String status = filter == null || filter.status() == null ? null : filter.status().name();
        String type = filter == null || filter.ruleType() == null ? null : filter.ruleType().name();
        String risk = filter == null || filter.riskLevel() == null ? null : filter.riskLevel().name();
        long total = definitions.countByFilter(tenantId, status, type, risk);
        List<RuleDefinition> rows = total == 0 ? List.of()
            : definitions.pageByFilter(tenantId, status, type, risk, page.offset(), page.safeSize());
        return PageResponse.of(rows, page, total);
    }

    @Transactional
    public RuleTestCaseResponse addTestCase(String ruleId, RuleTestCaseRequest request) {
        String tenantId = requireCurrentTenant();
        String traceId = RequestContext.currentTraceId();
        String actor = RequestContext.currentUserId().orElse("system");
        Instant now = Instant.now();
        RuleDefinition rule = findRule(ruleId, tenantId);
        ensureDraft(rule);
        RuleVersion version = findVersion(rule.activeVersionId(), tenantId);
        String caseId = "rtc-" + UUID.randomUUID();
        RuleTestCase saved = testCases.save(new RuleTestCase(
            null, caseId, tenantId, ruleId, version.versionId(), request.caseType(),
            writeJson(request.inputPayload()), request.expectedHit(), request.expectedSeverity(),
            request.expectedActionCode(), null, RuleTestCaseStatus.NOT_RUN, null, null,
            now, actor, now, actor, traceId));
        auditPublisher.publish(AuditAction.UPDATE, RULE_ENTITY, ruleId, "新增规则测试用例 " + saved.caseId());
        return new RuleTestCaseResponse(saved.caseId(), saved.caseType(), saved.lastStatus());
    }

    @Transactional
    public RuleEvaluationItem simulate(String ruleId, RuleSimulateRequest request) {
        String tenantId = requireCurrentTenant();
        RuleDefinition rule = findRule(ruleId, tenantId);
        RuleVersion version = findVersion(rule.activeVersionId(), tenantId);
        String trigger = readJson(version.dslJson()).path("trigger").asText("SIMULATE");
        return evaluateAndLog(rule, version, request.context(), trigger, null);
    }

    @Transactional
    public RulePublishResponse publish(String ruleId) {
        String tenantId = requireCurrentTenant();
        RuleDefinition rule = findRule(ruleId, tenantId);
        ensureDraft(rule);
        RuleVersion version = findVersion(rule.activeVersionId(), tenantId);
        List<RuleTestCase> cases = testCases.findByVersionIdAndTenantIdOrderByCreatedAtAsc(version.versionId(), tenantId);
        ensureCoverage(cases);

        List<RuleTestCaseResult> results = cases.stream()
            .map(testCase -> runTestCase(version, testCase))
            .toList();
        boolean passed = results.stream().allMatch(result -> result.status() == RuleTestCaseStatus.PASS);
        if (!passed) {
            throw new ApiException(ErrorCode.ENG_RULE_004, "规则测试用例未全部通过");
        }

        Instant now = Instant.now();
        String actor = RequestContext.currentUserId().orElse("system");
        RuleVersion publishedVersion = copyVersion(
            version, RuleVersionStatus.PUBLISHED, now, actor, now, actor, RequestContext.currentTraceId());
        RuleDefinition publishedRule = copyRule(
            rule, RuleDefinitionStatus.PUBLISHED, version.versionId(), now, actor, RequestContext.currentTraceId());
        versions.save(publishedVersion);
        definitions.save(publishedRule);
        transitions.record(RULE_ENTITY, ruleId, rule.status().name(),
            RuleDefinitionStatus.PUBLISHED.name(), "PUBLISH_RULE", null);
        auditPublisher.publish(AuditAction.PUBLISH, RULE_ENTITY, ruleId, "发布规则版本 " + version.versionId());
        return new RulePublishResponse(
            ruleId, version.versionId(), RuleDefinitionStatus.PUBLISHED,
            RequestContext.currentTraceId(), results);
    }

    @Transactional
    public RuleEvaluateResponse evaluate(RuleEvaluateRequest request) {
        String tenantId = requireCurrentTenant();
        List<RuleDefinition> candidates = request.ruleIds().isEmpty()
            ? definitions.findPublishedByTenantId(tenantId)
            : request.ruleIds().stream().map(ruleId -> findRule(ruleId, tenantId)).toList();

        List<RuleEvaluationItem> items = candidates.stream()
            .filter(rule -> rule.status() == RuleDefinitionStatus.PUBLISHED)
            .map(rule -> Map.entry(rule, findVersion(rule.activeVersionId(), tenantId)))
            .filter(entry -> entry.getValue().status() == RuleVersionStatus.PUBLISHED)
            .filter(entry -> triggerMatches(entry.getValue(), request.triggerPoint()))
            .map(entry -> evaluateAndLog(entry.getKey(), entry.getValue(),
                request.context(), request.triggerPoint(), request.eventId()))
            .toList();
        RuleRiskLevel highest = items.stream()
            .map(RuleEvaluationItem::severity)
            .reduce(null, RuleRiskLevel::max);
        return new RuleEvaluateResponse("eval-" + UUID.randomUUID(), items, highest, RequestContext.currentTraceId());
    }

    @Transactional(readOnly = true)
    public DiagnoseResponse diagnose(String executionId) {
        String tenantId = requireCurrentTenant();
        RuleExecutionLog execution = executions.findByExecutionIdAndTenantId(executionId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_RULE_002, "规则执行记录不存在: " + executionId));
        PayloadRef payloadRef = new PayloadRef(
            PayloadRef.STORAGE_INLINE, execution.inputDigest(),
            "db://rule_execution_log/" + execution.executionId(), 0L);
        return diagnoseAssembler.assemble(
            EXECUTION_ENTITY, execution.executionId(), tenantId, execution.status().name(),
            execution, List.of(), Map.of(), payloadRef, execution.traceId());
    }

    private RuleTestCaseResult runTestCase(RuleVersion version, RuleTestCase testCase) {
        try {
            RuleDslEvaluation evaluation = evaluator.evaluate(readJson(version.dslJson()), readJson(testCase.inputPayload()));
            boolean pass = matchesExpectation(testCase, evaluation);
            RuleTestCaseStatus status = pass ? RuleTestCaseStatus.PASS : RuleTestCaseStatus.FAIL;
            String message = pass ? "测试通过" : "实际结果与期望不一致";
            testCases.save(copyTestCaseResult(testCase, evaluation.hit(), status, message));
            return new RuleTestCaseResult(
                testCase.caseId(), testCase.caseType(), Boolean.TRUE.equals(testCase.expectedHit()),
                evaluation.hit(), testCase.expectedSeverity(), evaluation.severity(), status, message);
        } catch (ApiException exception) {
            testCases.save(copyTestCaseResult(testCase, false, RuleTestCaseStatus.ERROR, exception.getMessage()));
            return new RuleTestCaseResult(
                testCase.caseId(), testCase.caseType(), Boolean.TRUE.equals(testCase.expectedHit()),
                false, testCase.expectedSeverity(), null, RuleTestCaseStatus.ERROR, exception.getMessage());
        }
    }

    private RuleEvaluationItem evaluateAndLog(RuleDefinition rule, RuleVersion version,
                                              JsonNode context, String triggerPoint, String eventId) {
        RuleDslEvaluation evaluation = evaluator.evaluate(readJson(version.dslJson()), context);
        String executionId = "rex-" + UUID.randomUUID();
        RuleExecutionStatus status = evaluation.hit() ? RuleExecutionStatus.SUCCESS : RuleExecutionStatus.MISS;
        RuleExecutionLog log = executions.save(new RuleExecutionLog(
            null, executionId, rule.tenantId(), rule.ruleId(), version.versionId(),
            triggerPoint, eventId, RequestContext.currentUserId().orElse(null),
            digest(context), evaluation.hit(), evaluation.severity(), writeObject(evaluation.actions()),
            writeJson(evaluation.explanation()), status, null, null,
            Instant.now(), Instant.now(), RequestContext.currentTraceId()));
        transitions.record(EXECUTION_ENTITY, log.executionId(), null, status.name(), "EXECUTE_RULE", null);
        auditPublisher.publish(AuditAction.EXECUTE, EXECUTION_ENTITY, log.executionId(), "执行规则 " + rule.ruleId());
        return new RuleEvaluationItem(
            log.executionId(), rule.ruleId(), version.versionId(), evaluation.hit(),
            evaluation.severity(), evaluation.actions(), evaluation.explanation());
    }

    private boolean matchesExpectation(RuleTestCase testCase, RuleDslEvaluation evaluation) {
        boolean expectedHit = Boolean.TRUE.equals(testCase.expectedHit());
        if (expectedHit != evaluation.hit()) {
            return false;
        }
        if (!expectedHit) {
            return true;
        }
        if (testCase.expectedSeverity() != null && testCase.expectedSeverity() != evaluation.severity()) {
            return false;
        }
        if (testCase.expectedActionCode() == null || testCase.expectedActionCode().isBlank()) {
            return true;
        }
        return evaluation.actions().stream()
            .anyMatch(action -> testCase.expectedActionCode().equals(action.actionCode()));
    }

    private void ensureCoverage(List<RuleTestCase> cases) {
        EnumSet<RuleTestCaseType> covered = EnumSet.noneOf(RuleTestCaseType.class);
        cases.forEach(testCase -> covered.add(testCase.caseType()));
        if (!covered.containsAll(REQUIRED_CASE_TYPES)) {
            throw new ApiException(ErrorCode.ENG_RULE_004,
                "规则发布必须覆盖阳性、阴性、边界、冲突四类测试用例");
        }
    }

    private void ensureDraft(RuleDefinition rule) {
        if (rule.status() != RuleDefinitionStatus.DRAFT) {
            throw new ApiException(ErrorCode.ENG_RULE_006, "仅草稿规则允许当前操作: " + rule.ruleId());
        }
    }

    private boolean triggerMatches(RuleVersion version, String triggerPoint) {
        String trigger = readJson(version.dslJson()).path("trigger").asText(null);
        return triggerPoint == null || triggerPoint.equals(trigger);
    }

    private void validateDsl(JsonNode dsl) {
        evaluator.evaluate(dsl, json.createObjectNode());
        if (dsl.path("trigger").asText(null) == null || dsl.path("trigger").asText().isBlank()) {
            throw new ApiException(ErrorCode.ENG_RULE_001, "规则 DSL 缺少 trigger");
        }
        if (!dsl.has("then") || !dsl.has("explain")) {
            throw new ApiException(ErrorCode.ENG_RULE_001, "规则 DSL 缺少 then 或 explain");
        }
    }

    private RuleDefinition findRule(String ruleId, String tenantId) {
        return definitions.findByRuleIdAndTenantId(ruleId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_RULE_002, "规则不存在: " + ruleId));
    }

    private RuleVersion findVersion(String versionId, String tenantId) {
        if (versionId == null || versionId.isBlank()) {
            throw new ApiException(ErrorCode.ENG_RULE_003, "规则未绑定当前版本");
        }
        return versions.findByVersionIdAndTenantId(versionId, tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENG_RULE_003, "规则版本不存在: " + versionId));
    }

    private String requireCurrentTenant() {
        OrgScope scope = RequestContext.currentOrgScope();
        if (scope == null || !scope.hasTenant()) {
            throw ApiException.tenantMissing();
        }
        return scope.tenantId();
    }

    private JsonNode readJson(String source) {
        try {
            return json.readTree(source);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.ENG_RULE_001, "规则 JSON 解析失败", exception);
        }
    }

    private String writeJson(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        return writeObject(node);
    }

    private String writeObject(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.ENG_RULE_005, "规则结果序列化失败", exception);
        }
    }

    private String digest(JsonNode context) {
        try {
            String payload = writeJson(context);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(md.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new ApiException(ErrorCode.ENG_RULE_005, "规则输入摘要计算失败", exception);
        }
    }

    private RuleDefinition copyRule(RuleDefinition source, RuleDefinitionStatus status,
                                    String activeVersionId, Instant updatedAt,
                                    String updatedBy, String traceId) {
        return new RuleDefinition(
            source.id(), source.ruleId(), source.tenantId(), source.ruleCode(),
            source.name(), source.ruleType(), source.authoringMode(), source.riskLevel(),
            status, activeVersionId, source.packageVersion(), source.applicableOrgUnitId(),
            source.createdAt(), source.createdBy(), updatedAt, updatedBy, traceId);
    }

    private RuleVersion copyVersion(RuleVersion source, RuleVersionStatus status,
                                    Instant publishedAt, String publishedBy,
                                    Instant updatedAt, String updatedBy, String traceId) {
        return new RuleVersion(
            source.id(), source.versionId(), source.tenantId(), source.ruleId(), source.versionNo(),
            source.sourceRef(), source.changeSummary(), source.dslJson(), source.explanationJson(),
            status, publishedAt, publishedBy, source.rollbackVersionId(),
            source.createdAt(), source.createdBy(), updatedAt, updatedBy, traceId);
    }

    private RuleTestCase copyTestCaseResult(RuleTestCase source, boolean actualHit,
                                            RuleTestCaseStatus status, String message) {
        Instant now = Instant.now();
        return new RuleTestCase(
            source.id(), source.caseId(), source.tenantId(), source.ruleId(), source.versionId(),
            source.caseType(), source.inputPayload(), source.expectedHit(), source.expectedSeverity(),
            source.expectedActionCode(), actualHit, status, message, now,
            source.createdAt(), source.createdBy(), now, RequestContext.currentUserId().orElse("system"),
            RequestContext.currentTraceId());
    }
}
