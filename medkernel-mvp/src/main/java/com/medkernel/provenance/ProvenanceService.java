package com.medkernel.provenance;

import com.medkernel.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProvenanceService {
    private static final String DEFAULT_TENANT_ID = "default";
    private static final List<String> SUPPORTED_SOURCE_TYPES = Arrays.asList(
            "GUIDELINE", "CONSENSUS", "REGULATION", "LITERATURE", "HOSPITAL_POLICY", "EXPERT_OPINION");
    private static final List<String> SUPPORTED_REVIEW_STATUS = Arrays.asList(
            "DRAFT", "REVIEWED", "APPROVED", "REJECTED", "EXPIRED");

    private final EnginePersistenceService persistenceService;
    private final Map<String, SourceDocument> documentStore =
            new ConcurrentHashMap<String, SourceDocument>();

    public ProvenanceService(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public Map<String, Object> importDocuments(Object request) {
        ImportEnvelope envelope = normalize(request);
        if (envelope.documents.isEmpty()) {
            throw new IllegalArgumentException("documents is required");
        }

        String now = nowText();
        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> warnings = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> payload : envelope.documents) {
            SourceDocument document = toDocument(payload, envelope, now);
            collectWarnings(document, warnings);
            String key = key(document.getTenantId(), document.getDocumentCode());
            SourceDocument existing = findStoredDocument(document.getTenantId(), document.getDocumentCode());
            if (existing != null && existing.getCreatedTime() != null) {
                document.setCreatedTime(existing.getCreatedTime());
                document.setUpdatedTime(now);
                if (document.getCreatedBy() == null) {
                    document.setCreatedBy(existing.getCreatedBy());
                }
            }
            persistenceService.saveSourceDocument(document);
            documentStore.put(key, document);
            imported.add(toView(document));
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("tenant_id", envelope.tenantId);
        result.put("imported_count", imported.size());
        result.put("warnings", warnings);
        result.put("documents", imported);
        audit("IMPORT", envelope.operatorId, result);
        return result;
    }

    public List<Map<String, Object>> listDocuments(Map<String, String> filters) {
        String tenantId = filterValue(filters, "tenantId");
        String sourceType = upper(filterValue(filters, "sourceType"));
        String reviewStatus = upper(filterValue(filters, "reviewStatus"));
        String publisher = filterValue(filters, "publisher");
        String keyword = filterValue(filters, "keyword");
        int limit = filterInt(filters, "limit", 100);
        if (limit <= 0) {
            limit = 100;
        }

        List<SourceDocument> documents = storedDocuments();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SourceDocument document : documents) {
            if (!matches(tenantId, document.getTenantId(), false)) {
                continue;
            }
            if (!matches(sourceType, document.getSourceType(), true)) {
                continue;
            }
            if (!matches(reviewStatus, document.getReviewStatus(), true)) {
                continue;
            }
            if (!matches(publisher, document.getPublisher(), false)) {
                continue;
            }
            if (keyword != null && !containsIgnoreCase(document.getTitle(), keyword)
                    && !containsIgnoreCase(document.getDocumentCode(), keyword)) {
                continue;
            }
            result.add(toView(document));
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    public Map<String, Object> getDocument(String documentCode, String tenantId) {
        String resolvedTenantId = string(tenantId, DEFAULT_TENANT_ID);
        SourceDocument document = findStoredDocument(resolvedTenantId, documentCode);
        if (document == null) {
            throw new IllegalArgumentException("source document not found: " + documentCode);
        }
        return toView(document);
    }

    @SuppressWarnings("unchecked")
    private ImportEnvelope normalize(Object request) {
        ImportEnvelope envelope = new ImportEnvelope();
        envelope.tenantId = DEFAULT_TENANT_ID;
        if (request instanceof List) {
            envelope.documents.addAll((List<Map<String, Object>>) request);
            return envelope;
        }
        if (request instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) request;
            envelope.tenantId = string(value(body, "tenant_id", "tenantId"), DEFAULT_TENANT_ID);
            envelope.operatorId = string(value(body, "operator_id", "operatorId"), null);
            Object nested = value(body, "documents", "documents");
            if (nested instanceof Collection) {
                for (Object item : (Collection<?>) nested) {
                    if (item instanceof Map) {
                        envelope.documents.add((Map<String, Object>) item);
                    }
                }
            } else {
                envelope.documents.add(body);
            }
        }
        return envelope;
    }

    private SourceDocument toDocument(Map<String, Object> payload, ImportEnvelope envelope, String now) {
        SourceDocument document = new SourceDocument();
        document.setTenantId(string(value(payload, "tenant_id", "tenantId"), envelope.tenantId));
        document.setDocumentCode(required(payload, "document_code", "documentCode"));
        document.setTitle(required(payload, "title", "title"));
        String sourceType = upper(string(value(payload, "source_type", "sourceType"), null));
        if (sourceType == null) {
            throw new IllegalArgumentException("source_type is required");
        }
        if (!SUPPORTED_SOURCE_TYPES.contains(sourceType)) {
            throw new IllegalArgumentException("unsupported source_type: " + sourceType);
        }
        document.setSourceType(sourceType);
        document.setSourceUri(string(value(payload, "source_uri", "sourceUri"), null));
        document.setPublisher(string(value(payload, "publisher", "publisher"), null));
        document.setEffectiveDate(string(value(payload, "effective_date", "effectiveDate"), null));
        document.setExpiryDate(string(value(payload, "expiry_date", "expiryDate"), null));
        String reviewStatus = upper(string(value(payload, "review_status", "reviewStatus"), "DRAFT"));
        if (!SUPPORTED_REVIEW_STATUS.contains(reviewStatus)) {
            throw new IllegalArgumentException("unsupported review_status: " + reviewStatus);
        }
        document.setReviewStatus(reviewStatus);
        document.setReviewedBy(string(value(payload, "reviewed_by", "reviewedBy"), null));
        document.setReviewedTime(string(value(payload, "reviewed_time", "reviewedTime"), null));
        document.setContentHash(string(value(payload, "content_hash", "contentHash"), null));
        document.setCreatedBy(string(value(payload, "created_by", "createdBy"), envelope.operatorId));
        document.setCreatedTime(now);
        document.setUpdatedTime(now);
        document.setMetadata(mapValue(value(payload, "metadata", "metadata")));
        return document;
    }

    private SourceDocument findStoredDocument(String tenantId, String documentCode) {
        if (persistenceService.enabled()) {
            return persistenceService.findSourceDocument(tenantId, documentCode);
        }
        return documentStore.get(key(tenantId, documentCode));
    }

    private List<SourceDocument> storedDocuments() {
        if (persistenceService.enabled()) {
            return persistenceService.listSourceDocuments();
        }
        return sortedDocuments(new ArrayList<SourceDocument>(documentStore.values()));
    }

    private List<SourceDocument> sortedDocuments(List<SourceDocument> documents) {
        Collections.sort(documents, new Comparator<SourceDocument>() {
            @Override
            public int compare(SourceDocument left, SourceDocument right) {
                int byTenant = string(left.getTenantId(), DEFAULT_TENANT_ID)
                        .compareTo(string(right.getTenantId(), DEFAULT_TENANT_ID));
                if (byTenant != 0) {
                    return byTenant;
                }
                int byCode = string(left.getDocumentCode(), "")
                        .compareTo(string(right.getDocumentCode(), ""));
                if (byCode != 0) {
                    return byCode;
                }
                return string(left.getUpdatedTime(), "").compareTo(string(right.getUpdatedTime(), ""));
            }
        });
        return documents;
    }

    private void collectWarnings(SourceDocument document, List<Map<String, Object>> warnings) {
        LocalDate effective = parseDate(document.getEffectiveDate());
        LocalDate expiry = parseDate(document.getExpiryDate());
        if (effective != null && expiry != null && effective.isAfter(expiry)) {
            warnings.add(warning(document, "effective_date", "effective_date is after expiry_date"));
        }
        if (expiry != null && expiry.isBefore(LocalDate.now())) {
            warnings.add(warning(document, "expiry_date", "source document already expired"));
        }
    }

    private Map<String, Object> warning(SourceDocument document, String field, String message) {
        Map<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("severity", "WARN");
        warning.put("document_code", document.getDocumentCode());
        warning.put("field", field);
        warning.put("message", message);
        return warning;
    }

    private Map<String, Object> toView(SourceDocument document) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("tenant_id", document.getTenantId());
        view.put("document_code", document.getDocumentCode());
        view.put("title", document.getTitle());
        view.put("source_type", document.getSourceType());
        view.put("source_uri", document.getSourceUri());
        view.put("publisher", document.getPublisher());
        view.put("effective_date", document.getEffectiveDate());
        view.put("expiry_date", document.getExpiryDate());
        view.put("review_status", document.getReviewStatus());
        view.put("reviewed_by", document.getReviewedBy());
        view.put("reviewed_time", document.getReviewedTime());
        view.put("content_hash", document.getContentHash());
        view.put("metadata", document.getMetadata());
        view.put("created_by", document.getCreatedBy());
        view.put("created_time", document.getCreatedTime());
        view.put("updated_time", document.getUpdatedTime());
        view.put("expired", isExpired(document.getExpiryDate()));
        return view;
    }

    private boolean isExpired(String expiryDate) {
        LocalDate expiry = parseDate(expiryDate);
        return expiry != null && expiry.isBefore(LocalDate.now());
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String key(String tenantId, String documentCode) {
        return string(tenantId, DEFAULT_TENANT_ID) + "::" + documentCode;
    }

    private String required(Map<String, Object> map, String snakeKey, String camelKey) {
        String value = string(value(map, snakeKey, camelKey), null);
        if (value == null) {
            throw new IllegalArgumentException(snakeKey + " is required");
        }
        return value;
    }

    private Object value(Map<String, Object> map, String snakeKey, String camelKey) {
        Object value = map.get(snakeKey);
        return value == null ? map.get(camelKey) : value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map) {
            return new LinkedHashMap<String, Object>((Map<String, Object>) value);
        }
        return new LinkedHashMap<String, Object>();
    }

    private String filterValue(Map<String, String> filters, String key) {
        if (filters == null) {
            return null;
        }
        String value = filters.get(key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private int filterInt(Map<String, String> filters, String key, int defaultValue) {
        String value = filterValue(filters, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        if (value == null || keyword == null) {
            return false;
        }
        return value.toLowerCase().contains(keyword.toLowerCase());
    }

    private boolean matches(String expected, String actual, boolean ignoreCase) {
        if (expected == null) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return ignoreCase ? expected.equalsIgnoreCase(actual) : expected.equals(actual);
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text.trim();
    }

    private String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }

    private void audit(String actionType, String operatorId, Map<String, Object> detail) {
        try {
            persistenceService.saveAuditLog("PROVENANCE", actionType, "SRC_DOCUMENT",
                    null, null, null, operatorId, detail);
        } catch (RuntimeException ignored) {
            // 来源文档导入在 DB-only 模式下不因审计失败中断。
        }
    }

    private static class ImportEnvelope {
        private String tenantId;
        private String operatorId;
        private final List<Map<String, Object>> documents = new ArrayList<Map<String, Object>>();
    }
}
