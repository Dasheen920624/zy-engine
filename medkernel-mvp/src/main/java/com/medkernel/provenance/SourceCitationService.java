package com.medkernel.provenance;

import com.medkernel.persistence.EnginePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

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
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SourceCitationService {
    private static final Logger log = LoggerFactory.getLogger(SourceCitationService.class);

    private static final String DEFAULT_TENANT_ID = "default";
    private static final List<String> SUPPORTED_CITATION_TYPES = Arrays.asList(
            "PAGE", "SECTION", "CLAUSE", "TABLE", "FIGURE", "APPENDIX");
    private final AtomicLong idSequence = new AtomicLong(1000);

    private final EnginePersistenceService persistenceService;
    private final Map<String, SourceCitation> citationStore =
            new ConcurrentHashMap<String, SourceCitation>();

    public SourceCitationService(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    /**
     * 启动期从持久化层重建内存索引，避免重启后已导入的 citation 在 list/get 接口中"消失"。
     * 参照 PathwayService 的 @PostConstruct rebuildFromPersistence 模式。
     */
    @PostConstruct
    public void rebuildFromPersistence() {
        List<SourceCitation> persisted = persistenceService.listSourceCitations();
        for (SourceCitation citation : persisted) {
            String key = key(citation.getTenantId(), citation.getCitationId());
            citationStore.putIfAbsent(key, citation);
        }
    }

    public Map<String, Object> importCitations(Object request) {
        ImportEnvelope envelope = normalize(request);
        if (envelope.citations.isEmpty()) {
            throw new IllegalArgumentException("citations is required");
        }

        String now = nowText();
        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> warnings = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> payload : envelope.citations) {
            SourceCitation citation = toCitation(payload, envelope, now);
            collectWarnings(citation, warnings);
            String key = key(citation.getTenantId(), citation.getCitationId());
            SourceCitation existing = findStoredCitation(citation.getTenantId(), citation.getCitationId());
            if (existing != null && existing.getCreatedTime() != null) {
                citation.setCreatedTime(existing.getCreatedTime());
                citation.setUpdatedTime(now);
                if (citation.getCreatedBy() == null) {
                    citation.setCreatedBy(existing.getCreatedBy());
                }
            }
            citationStore.put(key, citation);
            persistenceService.saveSourceCitation(citation);
            imported.add(citation.toView());
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("tenant_id", envelope.tenantId);
        result.put("imported_count", imported.size());
        result.put("warnings", warnings);
        result.put("citations", imported);
        audit("IMPORT_CITATION", envelope.operatorId, result);
        return result;
    }

    public List<Map<String, Object>> listCitations(Map<String, String> filters) {
        String tenantId = filterValue(filters, "tenantId");
        String documentCode = filterValue(filters, "documentCode");
        String citationType = upper(filterValue(filters, "citationType"));
        String section = filterValue(filters, "section");
        int limit = filterInt(filters, "limit", 100);
        if (limit <= 0) {
            limit = 100;
        }

        List<SourceCitation> citations = storedCitations();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SourceCitation citation : citations) {
            if (!matches(tenantId, citation.getTenantId(), false)) {
                continue;
            }
            if (!matches(documentCode, citation.getDocumentCode(), false)) {
                continue;
            }
            if (!matches(citationType, citation.getCitationType(), true)) {
                continue;
            }
            if (!matches(section, citation.getSection(), false)) {
                continue;
            }
            result.add(citation.toView());
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    public Map<String, Object> getCitation(String citationId, String tenantId) {
        String resolvedTenantId = string(tenantId, DEFAULT_TENANT_ID);
        SourceCitation citation = findStoredCitation(resolvedTenantId, citationId);
        if (citation == null) {
            throw new IllegalArgumentException("citation not found: " + citationId);
        }
        return citation.toView();
    }

    public List<Map<String, Object>> getCitationsByDocument(String documentCode, String tenantId) {
        String resolvedTenantId = string(tenantId, DEFAULT_TENANT_ID);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SourceCitation citation : storedCitations()) {
            if (resolvedTenantId.equals(citation.getTenantId())
                    && documentCode.equals(citation.getDocumentCode())) {
                result.add(citation.toView());
            }
        }
        return result;
    }

    public int citationCount() {
        return citationStore.size();
    }

    @SuppressWarnings("unchecked")
    private ImportEnvelope normalize(Object request) {
        ImportEnvelope envelope = new ImportEnvelope();
        envelope.tenantId = DEFAULT_TENANT_ID;
        if (request instanceof List) {
            envelope.citations.addAll((List<Map<String, Object>>) request);
            return envelope;
        }
        if (request instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) request;
            envelope.tenantId = string(value(body, "tenant_id", "tenantId"), DEFAULT_TENANT_ID);
            envelope.operatorId = string(value(body, "operator_id", "operatorId"), null);
            Object nested = value(body, "citations", "citations");
            if (nested instanceof Collection) {
                for (Object item : (Collection<?>) nested) {
                    if (item instanceof Map) {
                        envelope.citations.add((Map<String, Object>) item);
                    }
                }
            } else {
                envelope.citations.add(body);
            }
        }
        return envelope;
    }

    private SourceCitation toCitation(Map<String, Object> payload, ImportEnvelope envelope, String now) {
        SourceCitation citation = new SourceCitation();
        citation.setTenantId(string(value(payload, "tenant_id", "tenantId"), envelope.tenantId));
        String citationId = string(value(payload, "citation_id", "citationId"), null);
        if (citationId == null) {
            citationId = "CIT_" + idSequence.incrementAndGet();
        }
        citation.setCitationId(citationId);
        String documentCode = required(payload, "document_code", "documentCode");
        citation.setDocumentCode(documentCode);
        citation.setSection(string(value(payload, "section", "section"), null));
        citation.setPage(string(value(payload, "page", "page"), null));
        citation.setClause(string(value(payload, "clause", "clause"), null));
        citation.setQuoteText(string(value(payload, "quote_text", "quoteText"), null));
        String citationType = upper(string(value(payload, "citation_type", "citationType"), "SECTION"));
        if (!SUPPORTED_CITATION_TYPES.contains(citationType)) {
            throw new IllegalArgumentException("unsupported citation_type: " + citationType);
        }
        citation.setCitationType(citationType);
        citation.setDescription(string(value(payload, "description", "description"), null));
        citation.setCreatedBy(string(value(payload, "created_by", "createdBy"), envelope.operatorId));
        citation.setCreatedTime(now);
        citation.setUpdatedTime(now);
        return citation;
    }

    private SourceCitation findStoredCitation(String tenantId, String citationId) {
        return citationStore.get(key(tenantId, citationId));
    }

    private List<SourceCitation> storedCitations() {
        return sortedCitations(new ArrayList<SourceCitation>(citationStore.values()));
    }

    private List<SourceCitation> sortedCitations(List<SourceCitation> citations) {
        Collections.sort(citations, new Comparator<SourceCitation>() {
            @Override
            public int compare(SourceCitation left, SourceCitation right) {
                int byTenant = string(left.getTenantId(), DEFAULT_TENANT_ID)
                        .compareTo(string(right.getTenantId(), DEFAULT_TENANT_ID));
                if (byTenant != 0) {
                    return byTenant;
                }
                return string(left.getCitationId(), "")
                        .compareTo(string(right.getCitationId(), ""));
            }
        });
        return citations;
    }

    private void collectWarnings(SourceCitation citation, List<Map<String, Object>> warnings) {
        if (citation.getSection() == null && citation.getPage() == null
                && citation.getClause() == null && citation.getQuoteText() == null) {
            Map<String, Object> warning = new LinkedHashMap<String, Object>();
            warning.put("severity", "WARN");
            warning.put("citation_id", citation.getCitationId());
            warning.put("field", "location");
            warning.put("message", "citation has no location reference (section/page/clause/quote_text)");
            warnings.add(warning);
        }
    }

    private String key(String tenantId, String citationId) {
        return string(tenantId, DEFAULT_TENANT_ID) + "::" + citationId;
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
            persistenceService.saveAuditLog("PROVENANCE", actionType, "SRC_CITATION",
                    null, null, null, operatorId, detail);
        } catch (RuntimeException e) {
            log.warn("RuntimeException in source citation audit: {}", e.getMessage());
        }
    }

    private static class ImportEnvelope {
        private String tenantId;
        private String operatorId;
        private final List<Map<String, Object>> citations = new ArrayList<Map<String, Object>>();
    }
}
