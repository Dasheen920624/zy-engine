package com.medkernel.knowledge;

import com.medkernel.common.OrganizationContext;
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

    public KnowledgeSourceRegistry registerSource(Map<String, Object> request, OrganizationContext orgContext) {
        String sourceCode = (String) request.get("source_code");
        if (sourceCode == null || sourceCode.isEmpty()) {
            sourceCode = "KS-" + String.format("%04d", SOURCE_SEQ.getAndIncrement());
        }
        if (sourceStore.containsKey(sourceCode)) {
            throw new IllegalArgumentException("Source already registered: " + sourceCode);
        }

        KnowledgeSourceRegistry source = new KnowledgeSourceRegistry();
        source.setTenantId(orgContext.getTenantId());
        source.setSourceCode(sourceCode);
        source.setSourceName((String) request.get("source_name"));
        source.setSourceType((String) request.get("source_type"));
        source.setPublisher((String) request.get("publisher"));
        source.setRegion((String) request.get("region"));
        source.setLanguage((String) request.get("language"));
        source.setReleaseVersion((String) request.get("release_version"));
        source.setReleaseDate((String) request.get("release_date"));
        source.setEffectiveDate((String) request.get("effective_date"));
        source.setExpiryDate((String) request.get("expiry_date"));
        source.setAuthorityLevel((String) request.get("authority_level"));
        source.setLicenseScope((String) request.get("license_scope"));
        source.setLicenseType((String) request.get("license_type"));
        source.setRedistributionAllowed(toBool(request.get("redistribution_allowed")));
        source.setCommercialUseAllowed(toBool(request.get("commercial_use_allowed")));
        source.setExportAllowed(toBool(request.get("export_allowed")));
        source.setFetchMethod((String) request.get("fetch_method"));
        source.setSourceUri((String) request.get("source_uri"));
        source.setRawHash((String) request.get("raw_hash"));
        source.setParsedHash((String) request.get("parsed_hash"));
        source.setReviewStatus("PENDING");
        source.setDescription((String) request.get("description"));
        source.setCreatedBy((String) request.get("created_by"));
        source.setCreatedTime(LocalDateTime.now().toString());
        source.setUpdatedTime(LocalDateTime.now().toString());

        sourceStore.put(sourceCode, source);
        return source;
    }

    public KnowledgeSourceRegistry updateSource(String sourceCode, Map<String, Object> request, OrganizationContext orgContext) {
        KnowledgeSourceRegistry source = sourceStore.get(sourceCode);
        if (source == null || !orgContext.getTenantId().equals(source.getTenantId())) {
            throw new IllegalArgumentException("Source not found: " + sourceCode);
        }
        if (request.containsKey("source_name")) source.setSourceName((String) request.get("source_name"));
        if (request.containsKey("source_type")) source.setSourceType((String) request.get("source_type"));
        if (request.containsKey("publisher")) source.setPublisher((String) request.get("publisher"));
        if (request.containsKey("region")) source.setRegion((String) request.get("region"));
        if (request.containsKey("language")) source.setLanguage((String) request.get("language"));
        if (request.containsKey("release_version")) source.setReleaseVersion((String) request.get("release_version"));
        if (request.containsKey("release_date")) source.setReleaseDate((String) request.get("release_date"));
        if (request.containsKey("effective_date")) source.setEffectiveDate((String) request.get("effective_date"));
        if (request.containsKey("expiry_date")) source.setExpiryDate((String) request.get("expiry_date"));
        if (request.containsKey("authority_level")) source.setAuthorityLevel((String) request.get("authority_level"));
        if (request.containsKey("license_scope")) source.setLicenseScope((String) request.get("license_scope"));
        if (request.containsKey("license_type")) source.setLicenseType((String) request.get("license_type"));
        if (request.containsKey("redistribution_allowed")) source.setRedistributionAllowed(toBool(request.get("redistribution_allowed")));
        if (request.containsKey("commercial_use_allowed")) source.setCommercialUseAllowed(toBool(request.get("commercial_use_allowed")));
        if (request.containsKey("export_allowed")) source.setExportAllowed(toBool(request.get("export_allowed")));
        if (request.containsKey("fetch_method")) source.setFetchMethod((String) request.get("fetch_method"));
        if (request.containsKey("source_uri")) source.setSourceUri((String) request.get("source_uri"));
        if (request.containsKey("description")) source.setDescription((String) request.get("description"));
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

    public List<KnowledgeSourceRegistry> listSources(Map<String, String> filters, OrganizationContext orgContext) {
        List<KnowledgeSourceRegistry> result = new ArrayList<KnowledgeSourceRegistry>();
        for (KnowledgeSourceRegistry source : sourceStore.values()) {
            if (!orgContext.getTenantId().equals(source.getTenantId())) continue;
            if (filters != null) {
                String sourceType = filters.get("source_type");
                if (sourceType != null && !sourceType.equalsIgnoreCase(source.getSourceType())) continue;
                String reviewStatus = filters.get("review_status");
                if (reviewStatus != null && !reviewStatus.equalsIgnoreCase(source.getReviewStatus())) continue;
                String authorityLevel = filters.get("authority_level");
                if (authorityLevel != null && !authorityLevel.equalsIgnoreCase(source.getAuthorityLevel())) continue;
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

    public KnowledgeSubscription createSubscription(Map<String, Object> request, OrganizationContext orgContext) {
        String subscriptionId = "SUB-" + String.format("%04d", SUB_SEQ.getAndIncrement());

        KnowledgeSubscription sub = new KnowledgeSubscription();
        sub.setTenantId(orgContext.getTenantId());
        sub.setSubscriptionId(subscriptionId);
        sub.setSubscriberId((String) request.get("subscriber_id"));
        sub.setSubscriberName((String) request.get("subscriber_name"));
        sub.setTopicType((String) request.get("topic_type"));
        sub.setTopicCode((String) request.get("topic_code"));
        sub.setTopicName((String) request.get("topic_name"));
        sub.setSourceTypes(toStringList(request.get("source_types")));
        sub.setAutoSync(toBool(request.getOrDefault("auto_sync", "true")));
        sub.setSyncFrequency((String) request.getOrDefault("sync_frequency", "MANUAL"));
        sub.setStatus("ACTIVE");
        sub.setCreatedBy((String) request.get("created_by"));
        sub.setCreatedTime(LocalDateTime.now().toString());
        sub.setUpdatedTime(LocalDateTime.now().toString());

        subscriptionStore.put(subscriptionId, sub);
        return sub;
    }

    public KnowledgeSubscription updateSubscription(String subscriptionId, Map<String, Object> request, OrganizationContext orgContext) {
        KnowledgeSubscription sub = subscriptionStore.get(subscriptionId);
        if (sub == null || !orgContext.getTenantId().equals(sub.getTenantId())) {
            throw new IllegalArgumentException("Subscription not found: " + subscriptionId);
        }
        if (request.containsKey("topic_name")) sub.setTopicName((String) request.get("topic_name"));
        if (request.containsKey("source_types")) sub.setSourceTypes(toStringList(request.get("source_types")));
        if (request.containsKey("auto_sync")) sub.setAutoSync(toBool(request.get("auto_sync")));
        if (request.containsKey("sync_frequency")) sub.setSyncFrequency((String) request.get("sync_frequency"));
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

    public List<KnowledgeSubscription> listSubscriptions(Map<String, String> filters, OrganizationContext orgContext) {
        List<KnowledgeSubscription> result = new ArrayList<KnowledgeSubscription>();
        for (KnowledgeSubscription sub : subscriptionStore.values()) {
            if (!orgContext.getTenantId().equals(sub.getTenantId())) continue;
            if (filters != null) {
                String topicType = filters.get("topic_type");
                if (topicType != null && !topicType.equalsIgnoreCase(sub.getTopicType())) continue;
                String status = filters.get("status");
                if (status != null && !status.equalsIgnoreCase(sub.getStatus())) continue;
                String subscriberId = filters.get("subscriber_id");
                if (subscriberId != null && !subscriberId.equals(sub.getSubscriberId())) continue;
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

    // ==================== 辅助方法 ====================

    private boolean toBool(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return "true".equalsIgnoreCase(value.toString());
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value == null) return new ArrayList<String>();
        if (value instanceof List) {
            List<String> result = new ArrayList<String>();
            for (Object item : (List<Object>) value) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        List<String> result = new ArrayList<String>();
        result.add(value.toString());
        return result;
    }
}
