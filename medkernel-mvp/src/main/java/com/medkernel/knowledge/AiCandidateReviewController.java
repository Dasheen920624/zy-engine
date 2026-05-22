package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.knowledge.dto.BatchReviewRequest;
import com.medkernel.knowledge.dto.ReviewCandidateRequest;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * AI 候选配置审核台 API：提交候选、查询列表、审核、批量审核、统计和历史。
 */
@Tag(name = "Ai Candidate Review")
@RestController
@RequestMapping("/api/knowledge/candidates")
public class AiCandidateReviewController {

    private final AiCandidateReviewService reviewService;
    private final OrganizationContextService organizationContextService;

    public AiCandidateReviewController(AiCandidateReviewService reviewService,
                                        OrganizationContextService organizationContextService) {
        this.reviewService = reviewService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 提交 AI 候选。
     */
    @Operation(summary = "Submit candidate")
    @PostMapping
    public ApiResult<AiCandidateReview> submitCandidate(@RequestBody AiCandidateReview candidate,
                                                          HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        candidate.setTenantId(resolveTenantId(orgCtx));
        return ApiResult.success(reviewService.submitCandidate(candidate));
    }

    /**
     * 查询候选列表。
     */
    @Operation(summary = "List candidates")
    @GetMapping
    public ApiResult<List<AiCandidateReview>> listCandidates(
            @RequestParam(required = false) String candidateType,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false, defaultValue = "50") int limit,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(reviewService.listCandidates(
                resolveTenantId(orgCtx), candidateType, reviewStatus, priority, limit));
    }

    /**
     * 获取候选详情。
     */
    @Operation(summary = "Get candidate")
    @GetMapping("/{candidateId}")
    public ApiResult<AiCandidateReview> getCandidate(@PathVariable Long candidateId,
                                                       HttpServletRequest httpRequest) {
        return ApiResult.success(reviewService.getCandidate(candidateId));
    }

    /**
     * 审核候选。
     */
    @Operation(summary = "Review candidate")
    @PostMapping("/{candidateId}/review")
    public ApiResult<String> reviewCandidate(@PathVariable Long candidateId,
                                               @Valid @RequestBody ReviewCandidateRequest request,
                                               HttpServletRequest httpRequest) {
        String reviewStatus = request.getReviewStatus();
        String reviewedBy = request.getReviewedBy() != null ? request.getReviewedBy() : "system";
        String reviewNote = request.getReviewNote();
        String modifiedContent = request.getModifiedContent();
        reviewService.reviewCandidate(candidateId, reviewStatus, reviewedBy, reviewNote, modifiedContent);
        return ApiResult.success("审核成功");
    }

    /**
     * 批量审核。
     */
    @Operation(summary = "Batch review")
    @PostMapping("/batch-review")
    public ApiResult<String> batchReview(@Valid @RequestBody BatchReviewRequest request,
                                           HttpServletRequest httpRequest) {
        List<Long> candidateIds = request.getCandidateIds();
        String reviewStatus = request.getReviewStatus();
        String reviewedBy = request.getReviewedBy() != null ? request.getReviewedBy() : "system";
        String reviewNote = request.getReviewNote();
        reviewService.batchReview(candidateIds, reviewStatus, reviewedBy, reviewNote);
        return ApiResult.success("批量审核成功");
    }

    /**
     * 审核统计。
     */
    @Operation(summary = "Get review summary")
    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> getReviewSummary(HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(reviewService.getReviewSummary(resolveTenantId(orgCtx)));
    }

    /**
     * 审核历史。
     */
    @Operation(summary = "Get review history")
    @GetMapping("/history")
    public ApiResult<List<AiCandidateReview>> getReviewHistory(
            @RequestParam(required = false) String reviewedBy,
            @RequestParam(required = false, defaultValue = "50") int limit,
            HttpServletRequest httpRequest) {
        OrganizationContext orgCtx = organizationContextService.resolve(httpRequest);
        return ApiResult.success(reviewService.getReviewHistory(resolveTenantId(orgCtx), reviewedBy, limit));
    }

    // ---- 辅助方法 ----

    private Long resolveTenantId(OrganizationContext orgCtx) {
        try {
            return Long.parseLong(orgCtx.getTenantId());
        } catch (NumberFormatException ex) {
            return 1L;
        }
    }
}
