package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public ApiResult<Map<String, Object>> registerSource(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            KnowledgeSourceRegistry source = knowledgeService.registerSource(request, orgContext);
            return ApiResult.success(source.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Update source")
    @PutMapping("/sources/{sourceCode}")
    public ApiResult<Map<String, Object>> updateSource(
            @PathVariable String sourceCode,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            KnowledgeSourceRegistry source = knowledgeService.updateSource(sourceCode, request, orgContext);
            return ApiResult.success(source.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Review source")
    @PostMapping("/sources/{sourceCode}/review")
    public ApiResult<Map<String, Object>> reviewSource(
            @PathVariable String sourceCode,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        String reviewStatus = (String) request.get("review_status");
        String reviewedBy = (String) request.get("reviewed_by");
        if (reviewStatus == null || reviewedBy == null) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, "review_status and reviewed_by are required");
        }
        try {
            KnowledgeSourceRegistry source = knowledgeService.reviewSource(sourceCode, reviewStatus, reviewedBy, orgContext);
            return ApiResult.success(source.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "List sources")
    @GetMapping("/sources")
    public ApiResult<List<Map<String, Object>>> listSources(
            @RequestParam(required = false) String source_type,
            @RequestParam(required = false) String review_status,
            @RequestParam(required = false) String authority_level,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Map<String, String> filters = new LinkedHashMap<String, String>();
        if (source_type != null) filters.put("source_type", source_type);
        if (review_status != null) filters.put("review_status", review_status);
        if (authority_level != null) filters.put("authority_level", authority_level);
        List<KnowledgeSourceRegistry> sources = knowledgeService.listSources(filters, orgContext);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (KnowledgeSourceRegistry source : sources) {
            views.add(source.toView());
        }
        return ApiResult.success(views);
    }

    @Operation(summary = "Get source")
    @GetMapping("/sources/{sourceCode}")
    public ApiResult<Map<String, Object>> getSource(
            @PathVariable String sourceCode,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        KnowledgeSourceRegistry source = knowledgeService.getSource(sourceCode, orgContext);
        if (source == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Source not found: " + sourceCode);
        }
        return ApiResult.success(source.toView());
    }

    // ==================== 知识订阅 API ====================

    @Operation(summary = "Create subscription")
    @PostMapping("/subscriptions")
    public ApiResult<Map<String, Object>> createSubscription(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            KnowledgeSubscription sub = knowledgeService.createSubscription(request, orgContext);
            return ApiResult.success(sub.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Update subscription")
    @PutMapping("/subscriptions/{subscriptionId}")
    public ApiResult<Map<String, Object>> updateSubscription(
            @PathVariable String subscriptionId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            KnowledgeSubscription sub = knowledgeService.updateSubscription(subscriptionId, request, orgContext);
            return ApiResult.success(sub.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Pause subscription")
    @PostMapping("/subscriptions/{subscriptionId}/pause")
    public ApiResult<Map<String, Object>> pauseSubscription(
            @PathVariable String subscriptionId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            KnowledgeSubscription sub = knowledgeService.pauseSubscription(subscriptionId, orgContext);
            return ApiResult.success(sub.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Cancel subscription")
    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    public ApiResult<Map<String, Object>> cancelSubscription(
            @PathVariable String subscriptionId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, request);
        try {
            KnowledgeSubscription sub = knowledgeService.cancelSubscription(subscriptionId, orgContext);
            return ApiResult.success(sub.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "List subscriptions")
    @GetMapping("/subscriptions")
    public ApiResult<List<Map<String, Object>>> listSubscriptions(
            @RequestParam(required = false) String topic_type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String subscriber_id,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        Map<String, String> filters = new LinkedHashMap<String, String>();
        if (topic_type != null) filters.put("topic_type", topic_type);
        if (status != null) filters.put("status", status);
        if (subscriber_id != null) filters.put("subscriber_id", subscriber_id);
        List<KnowledgeSubscription> subs = knowledgeService.listSubscriptions(filters, orgContext);
        List<Map<String, Object>> views = new ArrayList<Map<String, Object>>();
        for (KnowledgeSubscription sub : subs) {
            views.add(sub.toView());
        }
        return ApiResult.success(views);
    }

    @Operation(summary = "Get subscription")
    @GetMapping("/subscriptions/{subscriptionId}")
    public ApiResult<Map<String, Object>> getSubscription(
            @PathVariable String subscriptionId,
            HttpServletRequest httpRequest) {
        OrganizationContext orgContext = organizationContextService.resolve(httpRequest);
        KnowledgeSubscription sub = knowledgeService.getSubscription(subscriptionId, orgContext);
        if (sub == null) {
            return ApiResult.failure(ErrorCode.RESOURCE_NOT_FOUND, "Subscription not found: " + subscriptionId);
        }
        return ApiResult.success(sub.toView());
    }
}
