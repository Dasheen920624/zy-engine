package com.medkernel.knowledge;

import com.medkernel.knowledge.dto.CreateSubscriptionRequest;
import com.medkernel.knowledge.dto.RegisterSourceRequest;
import com.medkernel.knowledge.dto.SourceQueryRequest;
import com.medkernel.knowledge.dto.SubscriptionQueryRequest;
import com.medkernel.knowledge.dto.UpdateSourceRequest;
import com.medkernel.knowledge.dto.UpdateSubscriptionRequest;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 医疗知识需求订阅和来源注册服务。
 * 管理知识来源注册（含授权、语言、地区、证据等级）和知识订阅（按主题订阅）。
 */
@Service
public class KnowledgeService {
    private final OrganizationContextService organizationContextService;

    private static final AtomicLong SOURCE_SEQ = new AtomicLong(1);
    private static final AtomicLong SUB_SEQ = new AtomicLong(1);
    private final Map<String, KnowledgeSourceRegistry> sourceStore = new ConcurrentHashMap<String, KnowledgeSourceRegistry>();
    private final Map<String, KnowledgeSubscription> subscriptionStore = new ConcurrentHashMap<String, KnowledgeSubscription>();

    public KnowledgeService(OrganizationContextService organizationContextService) {
        this.organizationContextService = organizationContextService;
    }

    // ==================== 来源注册 CRUD ====================

    public KnowledgeSourceRegistry registerSource(RegisterSourceRequest request, OrganizationContext orgContext) {
        String sourceCode = request.getSourceCode();
        if (sourceCode == null || sourceCode.isEmpty()) {
            sourceCode = "KS-" + String.format("%04d", SOURCE_SEQ.getAndIncrement());
        }
        if (sourceStore.containsKey(sourceCode)) {
            throw new IllegalArgumentException("Source already registered: " + sourceCode);
        }

        KnowledgeSourceRegistry source = new KnowledgeSourceRegistry();
        source.setTenantId(orgContext.getTenantId());
        source.setSourceCode(sourceCode);
        source.setSourceName(request.getSourceName());
        source.setSourceType(request.getSourceType());
        source.setAuthorityLevel(request.getAuthorityLevel());
        source.setLanguage(request.getLanguage());
        source.setRegion(request.getRegion());
        source.setLicenseScope(request.getLicenseScope());
        source.setDescription(request.getDescription());
        source.setReviewStatus("PENDING");
        source.setCreatedBy(orgContext.getUserId());
        source.setCreatedTime(LocalDateTime.now().toString());
        source.setUpdatedTime(LocalDateTime.now().toString());

        sourceStore.put(sourceCode, source);
        return source;
    }

    public KnowledgeSourceRegistry updateSource(String sourceCode, UpdateSourceRequest request, OrganizationContext orgContext) {
        KnowledgeSourceRegistry source = sourceStore.get(sourceCode);
        if (source == null || !orgContext.getTenantId().equals(source.getTenantId())) {
            throw new IllegalArgumentException("Source not found: " + sourceCode);
        }
        if (request.getSourceName() != null) source.setSourceName(request.getSourceName());
        if (request.getSourceType() != null) source.setSourceType(request.getSourceType());
        if (request.getAuthorityLevel() != null) source.setAuthorityLevel(request.getAuthorityLevel());
        if (request.getLanguage() != null) source.setLanguage(request.getLanguage());
        if (request.getRegion() != null) source.setRegion(request.getRegion());
        if (request.getLicenseScope() != null) source.setLicenseScope(request.getLicenseScope());
        if (request.getDescription() != null) source.setDescription(request.getDescription());
        source.setUpdatedTime(LocalDateTime.now().toString());
        return source;
    }

    public KnowledgeSourceRegistry reviewSource(String sourceCode, String reviewStatus, String reviewedBy, OrganizationContext orgContext) {
        KnowledgeSourceRegistry source = sourceStore.get(sourceCode);
        if (source == null || !orgContext.getTenantId().equals(source.getTenantId())) {
            throw new IllegalArgumentException("Source not found: " + sourceCode);
        }
        if (!"APPROVED".equals(reviewStatus) && !"REJECTED".equals(reviewStatus)) {
            throw new IllegalArgumentException("reviewStatus must be APPROVED or REJECTED");
        }
        source.setReviewStatus(reviewStatus);
        source.setReviewedBy(reviewedBy);
        source.setReviewedTime(LocalDateTime.now().toString());
        source.setUpdatedTime(LocalDateTime.now().toString());
        return source;
    }

    public List<KnowledgeSourceRegistry> listSources(SourceQueryRequest query, OrganizationContext orgContext) {
        List<KnowledgeSourceRegistry> result = new ArrayList<KnowledgeSourceRegistry>();
        for (KnowledgeSourceRegistry source : sourceStore.values()) {
            if (!orgContext.getTenantId().equals(source.getTenantId())) continue;
            if (query != null) {
                if (query.getSourceType() != null && !query.getSourceType().equalsIgnoreCase(source.getSourceType())) continue;
                if (query.getReviewStatus() != null && !query.getReviewStatus().equalsIgnoreCase(source.getReviewStatus())) continue;
                if (query.getAuthorityLevel() != null && !query.getAuthorityLevel().equalsIgnoreCase(source.getAuthorityLevel())) continue;
            }
            result.add(source);
        }
        return result;
    }

    public KnowledgeSourceRegistry getSource(String sourceCode, OrganizationContext orgContext) {
        KnowledgeSourceRegistry source = sourceStore.get(sourceCode);
        if (source == null || !orgContext.getTenantId().equals(source.getTenantId())) {
            return null;
        }
        return source;
    }

    // ==================== 知识订阅 CRUD ====================

    public KnowledgeSubscription createSubscription(CreateSubscriptionRequest request, OrganizationContext orgContext) {
        String subscriptionId = "SUB-" + String.format("%04d", SUB_SEQ.getAndIncrement());

        KnowledgeSubscription sub = new KnowledgeSubscription();
        sub.setTenantId(orgContext.getTenantId());
        sub.setSubscriptionId(subscriptionId);
        sub.setSubscriberId(request.getSubscriberId());
        sub.setTopicType(request.getTopicType());
        sub.setTopicCode(request.getTopicId());
        sub.setSyncFrequency(request.getSyncMode());
        sub.setStatus("ACTIVE");
        sub.setCreatedBy(orgContext.getUserId());
        sub.setCreatedTime(LocalDateTime.now().toString());
        sub.setUpdatedTime(LocalDateTime.now().toString());

        subscriptionStore.put(subscriptionId, sub);
        return sub;
    }

    public KnowledgeSubscription updateSubscription(String subscriptionId, UpdateSubscriptionRequest request, OrganizationContext orgContext) {
        KnowledgeSubscription sub = subscriptionStore.get(subscriptionId);
        if (sub == null || !orgContext.getTenantId().equals(sub.getTenantId())) {
            throw new IllegalArgumentException("Subscription not found: " + subscriptionId);
        }
        if (request.getTopicType() != null) sub.setTopicType(request.getTopicType());
        if (request.getTopicId() != null) sub.setTopicCode(request.getTopicId());
        if (request.getSubscriberId() != null) sub.setSubscriberId(request.getSubscriberId());
        if (request.getSyncMode() != null) sub.setSyncFrequency(request.getSyncMode());
        sub.setUpdatedTime(LocalDateTime.now().toString());
        return sub;
    }

    public KnowledgeSubscription pauseSubscription(String subscriptionId, OrganizationContext orgContext) {
        KnowledgeSubscription sub = subscriptionStore.get(subscriptionId);
        if (sub == null || !orgContext.getTenantId().equals(sub.getTenantId())) {
            throw new IllegalArgumentException("Subscription not found: " + subscriptionId);
        }
        sub.setStatus("PAUSED");
        sub.setUpdatedTime(LocalDateTime.now().toString());
        return sub;
    }

    public KnowledgeSubscription cancelSubscription(String subscriptionId, OrganizationContext orgContext) {
        KnowledgeSubscription sub = subscriptionStore.get(subscriptionId);
        if (sub == null || !orgContext.getTenantId().equals(sub.getTenantId())) {
            throw new IllegalArgumentException("Subscription not found: " + subscriptionId);
        }
        sub.setStatus("CANCELLED");
        sub.setUpdatedTime(LocalDateTime.now().toString());
        return sub;
    }

    public List<KnowledgeSubscription> listSubscriptions(SubscriptionQueryRequest query, OrganizationContext orgContext) {
        List<KnowledgeSubscription> result = new ArrayList<KnowledgeSubscription>();
        for (KnowledgeSubscription sub : subscriptionStore.values()) {
            if (!orgContext.getTenantId().equals(sub.getTenantId())) continue;
            if (query != null) {
                if (query.getTopicType() != null && !query.getTopicType().equalsIgnoreCase(sub.getTopicType())) continue;
                if (query.getStatus() != null && !query.getStatus().equalsIgnoreCase(sub.getStatus())) continue;
                if (query.getSubscriberId() != null && !query.getSubscriberId().equals(sub.getSubscriberId())) continue;
            }
            result.add(sub);
        }
        return result;
    }

    public KnowledgeSubscription getSubscription(String subscriptionId, OrganizationContext orgContext) {
        KnowledgeSubscription sub = subscriptionStore.get(subscriptionId);
        if (sub == null || !orgContext.getTenantId().equals(sub.getTenantId())) {
            return null;
        }
        return sub;
    }
}
