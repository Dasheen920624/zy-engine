package com.medkernel.quality;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.quality.dto.CreateTestCaseRequest;
import com.medkernel.quality.dto.UpdateTestCaseRequest;
import com.medkernel.quality.dto.RecordTestResultRequest;
import com.medkernel.quality.dto.ReviewResultRequest;
import com.medkernel.quality.dto.AttachEvidenceRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Acceptance Test")
@RestController
@RequestMapping("/api/acceptance")
public class AcceptanceTestController {

    private final AcceptanceTestService acceptanceTestService;
    private final OrganizationContextService orgContextService;

    public AcceptanceTestController(AcceptanceTestService acceptanceTestService,
                                    OrganizationContextService orgContextService) {
        this.acceptanceTestService = acceptanceTestService;
        this.orgContextService = orgContextService;
    }

    // =========================================================================
    // 用例管理
    // =========================================================================

    @Operation(summary = "Create test case")
    @PostMapping("/test-cases")
    public ApiResult<AcceptanceTestCase> createTestCase(
            @RequestBody @Valid CreateTestCaseRequest request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            AcceptanceTestCase testCase = toEntity(request);
            AcceptanceTestCase created = acceptanceTestService.createTestCase(testCase);
            return ApiResult.success(created);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "Update test case")
    @PutMapping("/test-cases/{testCaseId}")
    public ApiResult<AcceptanceTestCase> updateTestCase(
            @PathVariable("testCaseId") Long testCaseId,
            @RequestBody @Valid UpdateTestCaseRequest request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        AcceptanceTestCase testCase = toEntity(request);
        testCase.setId(testCaseId);
        try {
            AcceptanceTestCase updated = acceptanceTestService.updateTestCase(testCase);
            return ApiResult.success(updated);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "List test cases")
    @GetMapping("/test-cases")
    public ApiResult<List<AcceptanceTestCase>> listTestCases(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "feature_code", required = false) String featureCode,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            List<AcceptanceTestCase> cases = acceptanceTestService.listTestCases(tenantId, category, featureCode, status);
            return ApiResult.success(cases);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 测试执行
    // =========================================================================

    @Operation(summary = "Record test result")
    @PostMapping("/results")
    public ApiResult<AcceptanceTestResult> recordResult(
            @RequestBody @Valid RecordTestResultRequest request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            AcceptanceTestResult result = toEntity(request);
            AcceptanceTestResult recorded = acceptanceTestService.recordResult(result);
            return ApiResult.success(recorded);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "List results")
    @GetMapping("/results")
    public ApiResult<List<AcceptanceTestResult>> listResults(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "case_code", required = false) String caseCode,
            @RequestParam(value = "verdict", required = false) String verdict,
            @RequestParam(value = "status", required = false) String status,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            List<AcceptanceTestResult> results = acceptanceTestService.listResults(tenantId, caseCode, verdict, status);
            return ApiResult.success(results);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "Review result")
    @PostMapping("/results/{resultId}/review")
    public ApiResult<AcceptanceTestResult> reviewResult(
            @PathVariable("resultId") Long resultId,
            @RequestBody @Valid ReviewResultRequest request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        String status = request.getStatus();
        if (status == null || status.trim().isEmpty()) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "status is required");
        }
        try {
            AcceptanceTestResult reviewed = acceptanceTestService.reviewResult(resultId, request.getReviewedBy(), request.getReviewNote(), status);
            return ApiResult.success(reviewed);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 证据管理
    // =========================================================================

    @Operation(summary = "Attach evidence")
    @PostMapping("/evidence")
    public ApiResult<AcceptanceEvidence> attachEvidence(
            @RequestBody @Valid AttachEvidenceRequest request,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        try {
            AcceptanceEvidence evidence = toEntity(request);
            AcceptanceEvidence attached = acceptanceTestService.attachEvidence(evidence);
            return ApiResult.success(attached);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "List evidence")
    @GetMapping("/evidence")
    public ApiResult<List<AcceptanceEvidence>> listEvidence(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "result_code", required = false) String resultCode,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            List<AcceptanceEvidence> evidences = acceptanceTestService.listEvidence(tenantId, resultCode);
            return ApiResult.success(evidences);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 报告
    // =========================================================================

    @Operation(summary = "Get acceptance summary")
    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> getAcceptanceSummary(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "feature_code", required = false) String featureCode,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            Map<String, Object> summary = acceptanceTestService.getAcceptanceSummary(tenantId, featureCode);
            return ApiResult.success(summary);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    @Operation(summary = "Generate acceptance report")
    @GetMapping("/report")
    public ApiResult<Map<String, Object>> generateAcceptanceReport(
            @RequestParam(value = "tenant_id", required = false) Long tenantId,
            @RequestParam(value = "feature_code", required = false) String featureCode,
            HttpServletRequest httpRequest) {
        orgContextService.applyExplicitFilters(new LinkedHashMap<String, String>(), httpRequest);
        if (tenantId == null) {
            tenantId = resolveTenantId(httpRequest);
        }
        try {
            Map<String, Object> report = acceptanceTestService.generateAcceptanceReport(tenantId, featureCode);
            return ApiResult.success(report);
        } catch (IllegalStateException ex) {
            return ApiResult.failure(ErrorCode.DB_ERROR, ex.getMessage());
        }
    }

    // =========================================================================
    // 内部方法
    // =========================================================================

    private Long resolveTenantId(HttpServletRequest httpRequest) {
        String headerTenantId = httpRequest.getHeader("X-Tenant-Id");
        if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
            try {
                return Long.parseLong(headerTenantId.trim());
            } catch (NumberFormatException ex) {
                return 0L;
            }
        }
        return 0L;
    }

    private AcceptanceTestCase toEntity(CreateTestCaseRequest request) {
        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setCaseCode(request.getCaseCode());
        testCase.setCaseName(request.getCaseName());
        testCase.setCategory(request.getCategory());
        testCase.setFeatureCode(request.getFeatureCode());
        testCase.setDescription(request.getDescription());
        testCase.setSteps(request.getSteps());
        testCase.setExpectedResult(request.getExpectedResult());
        return testCase;
    }

    private AcceptanceTestCase toEntity(UpdateTestCaseRequest request) {
        AcceptanceTestCase testCase = new AcceptanceTestCase();
        testCase.setCaseName(request.getCaseName());
        testCase.setCategory(request.getCategory());
        testCase.setDescription(request.getDescription());
        testCase.setSteps(request.getSteps());
        testCase.setExpectedResult(request.getExpectedResult());
        testCase.setStatus(request.getStatus());
        return testCase;
    }

    private AcceptanceTestResult toEntity(RecordTestResultRequest request) {
        AcceptanceTestResult result = new AcceptanceTestResult();
        result.setCaseCode(request.getCaseCode());
        result.setVerdict(request.getVerdict());
        result.setActualResult(request.getActualResult());
        result.setEvidenceRefs(request.getEvidenceRefs());
        result.setExecutedBy(request.getTestedBy());
        return result;
    }

    private AcceptanceEvidence toEntity(AttachEvidenceRequest request) {
        AcceptanceEvidence evidence = new AcceptanceEvidence();
        evidence.setResultCode(request.getResultCode());
        evidence.setEvidenceType(request.getEvidenceType());
        evidence.setFilePath(request.getFilePath());
        evidence.setContent(request.getContent());
        evidence.setDescription(request.getDescription());
        return evidence;
    }
}
