package com.medkernel.knowledge;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.knowledge.dto.CreateSubscriptionRequest;
import com.medkernel.knowledge.dto.RegisterSourceRequest;
import com.medkernel.knowledge.dto.ReviewSourceRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
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
            @Valid @RequestBody RegisterSourceRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> requestMap = toSourceMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, requestMap);
        try {
            KnowledgeSourceRegistry source = knowledgeService.registerSource(requestMap, orgContext);
            return ApiResult.success(source.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Update source")
    @PutMapping("/sources/{sourceCode}")
    public ApiResult<Map<String, Object>> updateSource(
            @PathVariable String sourceCode,
            @Valid @RequestBody UpdateSourceRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> requestMap = toUpdateSourceMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, requestMap);
        try {
            KnowledgeSourceRegistry source = knowledgeService.updateSource(sourceCode, requestMap, orgContext);
            return ApiResult.success(source.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Review source")
    @PostMapping("/sources/{sourceCode}/review")
    public ApiResult<Map<String, Object>> reviewSource(
            @PathVariable String sourceCode,
            @Valid @RequestBody ReviewSourceRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> requestMap = new LinkedHashMap<String, Object>();
        requestMap.put("review_status", request.getReviewStatus());
        requestMap.put("reviewed_by", request.getReviewedBy());
        requestMap.put("review_note", request.getReviewNote());
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, requestMap);
        try {
            KnowledgeSourceRegistry source = knowledgeService.reviewSource(sourceCode, request.getReviewStatus(), request.getReviewedBy(), orgContext);
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
            @Valid @RequestBody CreateSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> requestMap = toSubscriptionMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, requestMap);
        try {
            KnowledgeSubscription sub = knowledgeService.createSubscription(requestMap, orgContext);
            return ApiResult.success(sub.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Update subscription")
    @PutMapping("/subscriptions/{subscriptionId}")
    public ApiResult<Map<String, Object>> updateSubscription(
            @PathVariable String subscriptionId,
            @Valid @RequestBody UpdateSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> requestMap = toUpdateSubscriptionMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, requestMap);
        try {
            KnowledgeSubscription sub = knowledgeService.updateSubscription(subscriptionId, requestMap, orgContext);
            return ApiResult.success(sub.toView());
        } catch (IllegalArgumentException e) {
            return ApiResult.failure(ErrorCode.VALIDATION_ERROR, e.getMessage());
        }
    }

    @Operation(summary = "Pause subscription")
    @PostMapping("/subscriptions/{subscriptionId}/pause")
    public ApiResult<Map<String, Object>> pauseSubscription(
            @PathVariable String subscriptionId,
            @RequestBody UpdateSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> requestMap = toUpdateSubscriptionMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, requestMap);
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
            @RequestBody UpdateSubscriptionRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> requestMap = toUpdateSubscriptionMap(request);
        OrganizationContext orgContext = organizationContextService.resolveWithBody(httpRequest, requestMap);
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

    // ==================== DTO 转换辅助方法 ====================

    private Map<String, Object> toSourceMap(RegisterSourceRequest req) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (req.getSourceCode() != null) map.put("source_code", req.getSourceCode());
        if (req.getSourceName() != null) map.put("source_name", req.getSourceName());
        if (req.getSourceType() != null) map.put("source_type", req.getSourceType());
        if (req.getAuthorityLevel() != null) map.put("authority_level", req.getAuthorityLevel());
        if (req.getLanguage() != null) map.put("language", req.getLanguage());
        if (req.getRegion() != null) map.put("region", req.getRegion());
        if (req.getLicenseScope() != null) map.put("license_scope", req.getLicenseScope());
        if (req.getDescription() != null) map.put("description", req.getDescription());
        if (req.getEndpointUrl() != null) map.put("endpoint_url", req.getEndpointUrl());
        if (req.getAuthType() != null) map.put("auth_type", req.getAuthType());
        if (req.getAuthCredentials() != null) map.put("auth_credentials", req.getAuthCredentials());
        if (req.getSyncInterval() != null) map.put("sync_interval", req.getSyncInterval());
        if (req.getTenantId() != null) map.put("tenant_id", req.getTenantId());
        return map;
    }

    private Map<String, Object> toUpdateSourceMap(UpdateSourceRequest req) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (req.getSourceName() != null) map.put("source_name", req.getSourceName());
        if (req.getSourceType() != null) map.put("source_type", req.getSourceType());
        if (req.getAuthorityLevel() != null) map.put("authority_level", req.getAuthorityLevel());
        if (req.getLanguage() != null) map.put("language", req.getLanguage());
        if (req.getRegion() != null) map.put("region", req.getRegion());
        if (req.getLicenseScope() != null) map.put("license_scope", req.getLicenseScope());
        if (req.getDescription() != null) map.put("description", req.getDescription());
        if (req.getEndpointUrl() != null) map.put("endpoint_url", req.getEndpointUrl());
        if (req.getAuthType() != null) map.put("auth_type", req.getAuthType());
        if (req.getAuthCredentials() != null) map.put("auth_credentials", req.getAuthCredentials());
        if (req.getSyncInterval() != null) map.put("sync_interval", req.getSyncInterval());
        if (req.getTenantId() != null) map.put("tenant_id", req.getTenantId());
        return map;
    }

    private Map<String, Object> toSubscriptionMap(CreateSubscriptionRequest req) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (req.getTopicType() != null) map.put("topic_type", req.getTopicType());
        if (req.getTopicId() != null) map.put("topic_id", req.getTopicId());
        if (req.getSubscriberId() != null) map.put("subscriber_id", req.getSubscriberId());
        if (req.getSyncMode() != null) map.put("sync_mode", req.getSyncMode());
        if (req.getTenantId() != null) map.put("tenant_id", req.getTenantId());
        return map;
    }

    private Map<String, Object> toUpdateSubscriptionMap(UpdateSubscriptionRequest req) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (req.getTopicType() != null) map.put("topic_type", req.getTopicType());
        if (req.getTopicId() != null) map.put("topic_id", req.getTopicId());
        if (req.getSubscriberId() != null) map.put("subscriber_id", req.getSubscriberId());
        if (req.getSyncMode() != null) map.put("sync_mode", req.getSyncMode());
        if (req.getTenantId() != null) map.put("tenant_id", req.getTenantId());
        return map;
    }
}
