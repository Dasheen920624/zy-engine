package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.knowledge.dto.CreateSubscriptionRequest;
import com.medkernel.knowledge.dto.RegisterSourceRequest;
import com.medkernel.knowledge.dto.ReviewSourceRequest;
import com.medkernel.knowledge.dto.SourceQueryRequest;
import com.medkernel.knowledge.dto.SourceResponse;
import com.medkernel.knowledge.dto.SubscriptionQueryRequest;
import com.medkernel.knowledge.dto.SubscriptionResponse;
import com.medkernel.knowledge.dto.UpdateSourceRequest;
import com.medkernel.knowledge.dto.UpdateSubscriptionRequest;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 医疗知识需求订阅和来源注册 API。
 */
@Tag(name = "Knowledge")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;
    private final OrganizationContextService organizationContextService;

    public KnowledgeController(KnowledgeService knowledgeService,
                               OrganizationContextService organizationContextService) {
        this.knowledgeService = knowledgeService;
        this.organizationContextService = organizationContextService;
    }

    // ==================== 来源注册 API ====================

    @Operation(summary = "Register source")
    @PostMapping("/sources")
    public ApiResult<SourceResponse> registerSource(
            @Valid @RequestBody RegisterSourceRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            KnowledgeSourceRegistry source = knowledgeService.registerSource(request, orgContext);
            return ApiResult.success(SourceResponse.fromEntity(source));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Update source")
    @PutMapping("/sources/{sourceCode}")
    public ApiResult<SourceResponse> updateSource(
            @PathVariable String sourceCode,
            @Valid @RequestBody UpdateSourceRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            KnowledgeSourceRegistry source = knowledgeService.updateSource(sourceCode, request, orgContext);
            return ApiResult.success(SourceResponse.fromEntity(source));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Review source")
    @PostMapping("/sources/{sourceCode}/review")
    public ApiResult<SourceResponse> reviewSource(
            @PathVariable String sourceCode,
            @Valid @RequestBody ReviewSourceRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            KnowledgeSourceRegistry source = knowledgeService.reviewSource(sourceCode, request.getReviewStatus(), request.getReviewedBy(), orgContext);
            return ApiResult.success(SourceResponse.fromEntity(source));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "List sources")
    @GetMapping("/sources")
    public ApiResult<List<SourceResponse>> listSources(
            SourceQueryRequest query,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        List<KnowledgeSourceRegistry> sources = knowledgeService.listSources(query, orgContext);
        List<SourceResponse> views = new ArrayList<SourceResponse>();
        for (KnowledgeSourceRegistry source : sources) {
            views.add(SourceResponse.fromEntity(source));
        }
        return ApiResult.success(views);
    }

    @Operation(summary = "Get source")
    @GetMapping("/sources/{sourceCode}")
    public ApiResult<SourceResponse> getSource(
            @PathVariable String sourceCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        KnowledgeSourceRegistry source = knowledgeService.getSource(sourceCode, orgContext);
        if (source == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Source not found: " + sourceCode);
        }
        return ApiResult.success(SourceResponse.fromEntity(source));
    }

    // ==================== 知识订阅 API ====================

    @Operation(summary = "Create subscription")
    @PostMapping("/subscriptions")
    public ApiResult<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            KnowledgeSubscription sub = knowledgeService.createSubscription(request, orgContext);
            return ApiResult.success(SubscriptionResponse.fromEntity(sub));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Update subscription")
    @PutMapping("/subscriptions/{subscriptionId}")
    public ApiResult<SubscriptionResponse> updateSubscription(
            @PathVariable String subscriptionId,
            @Valid @RequestBody UpdateSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            KnowledgeSubscription sub = knowledgeService.updateSubscription(subscriptionId, request, orgContext);
            return ApiResult.success(SubscriptionResponse.fromEntity(sub));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Pause subscription")
    @PostMapping("/subscriptions/{subscriptionId}/pause")
    public ApiResult<SubscriptionResponse> pauseSubscription(
            @PathVariable String subscriptionId,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            KnowledgeSubscription sub = knowledgeService.pauseSubscription(subscriptionId, orgContext);
            return ApiResult.success(SubscriptionResponse.fromEntity(sub));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Cancel subscription")
    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    public ApiResult<SubscriptionResponse> cancelSubscription(
            @PathVariable String subscriptionId,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        try {
            KnowledgeSubscription sub = knowledgeService.cancelSubscription(subscriptionId, orgContext);
            return ApiResult.success(SubscriptionResponse.fromEntity(sub));
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "List subscriptions")
    @GetMapping("/subscriptions")
    public ApiResult<List<SubscriptionResponse>> listSubscriptions(
            SubscriptionQueryRequest query,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        List<KnowledgeSubscription> subs = knowledgeService.listSubscriptions(query, orgContext);
        List<SubscriptionResponse> views = new ArrayList<SubscriptionResponse>();
        for (KnowledgeSubscription sub : subs) {
            views.add(SubscriptionResponse.fromEntity(sub));
        }
        return ApiResult.success(views);
    }

    @Operation(summary = "Get subscription")
    @GetMapping("/subscriptions/{subscriptionId}")
    public ApiResult<SubscriptionResponse> getSubscription(
            @PathVariable String subscriptionId,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        KnowledgeSubscription sub = knowledgeService.getSubscription(subscriptionId, orgContext);
        if (sub == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Subscription not found: " + subscriptionId);
        }
        return ApiResult.success(SubscriptionResponse.fromEntity(sub));
    }
}
