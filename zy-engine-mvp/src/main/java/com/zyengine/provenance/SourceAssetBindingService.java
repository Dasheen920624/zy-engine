package com.zyengine.provenance;

import com.zyengine.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

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
public class SourceAssetBindingService {
    private static final String DEFAULT_TENANT_ID = "default";
    private static final List<String> SUPPORTED_ASSET_TYPES = Arrays.asList(
            "RULE", "PATHWAY", "CONFIG_PACKAGE", "ADAPTER", "GRAPH", "QC_METRIC");
    private static final List<String> SUPPORTED_BINDING_TYPES = Arrays.asList(
            "EVIDENCE", "REFERENCE", "DERIVATION", "COMPLIANCE");
    private final AtomicLong idSequence = new AtomicLong(5000);

    private final EnginePersistenceService persistenceService;
    private final Map<String, SourceAssetBinding> bindingStore =
            new ConcurrentHashMap<String, SourceAssetBinding>();

    public SourceAssetBindingService(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public Map<String, Object> importBindings(Object request) {
        ImportEnvelope envelope = normalize(request);
        if (envelope.bindings.isEmpty()) {
            throw new IllegalArgumentException("bindings is required");
        }

        String now = nowText();
        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> warnings = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> payload : envelope.bindings) {
            SourceAssetBinding binding = toBinding(payload, envelope, now);
            collectWarnings(binding, warnings);
            String key = key(binding.getTenantId(), binding.getBindingId());
            SourceAssetBinding existing = findStoredBinding(binding.getTenantId(), binding.getBindingId());
            if (existing != null && existing.getCreatedTime() != null) {
                binding.setCreatedTime(existing.getCreatedTime());
                binding.setUpdatedTime(now);
                if (binding.getCreatedBy() == null) {
                    binding.setCreatedBy(existing.getCreatedBy());
                }
            }
            bindingStore.put(key, binding);
            imported.add(binding.toView());
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("tenant_id", envelope.tenantId);
        result.put("imported_count", imported.size());
        result.put("warnings", warnings);
        result.put("bindings", imported);
        audit("IMPORT_BINDING", envelope.operatorId, result);
        return result;
    }

    public List<Map<String, Object>> listBindings(Map<String, String> filters) {
        String tenantId = filterValue(filters, "tenantId");
        String assetType = upper(filterValue(filters, "assetType"));
        String assetCode = filterValue(filters, "assetCode");
        String documentCode = filterValue(filters, "documentCode");
        String bindingType = upper(filterValue(filters, "bindingType"));
        int limit = filterInt(filters, "limit", 100);
        if (limit <= 0) {
            limit = 100;
        }

        List<SourceAssetBinding> bindings = storedBindings();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SourceAssetBinding binding : bindings) {
            if (!matches(tenantId, binding.getTenantId(), false)) {
                continue;
            }
            if (!matches(assetType, binding.getAssetType(), true)) {
                continue;
            }
            if (!matches(assetCode, binding.getAssetCode(), false)) {
                continue;
            }
            if (!matches(documentCode, binding.getDocumentCode(), false)) {
                continue;
            }
            if (!matches(bindingType, binding.getBindingType(), true)) {
                continue;
            }
            result.add(binding.toView());
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    public Map<String, Object> getBinding(String bindingId, String tenantId) {
        String resolvedTenantId = string(tenantId, DEFAULT_TENANT_ID);
        SourceAssetBinding binding = findStoredBinding(resolvedTenantId, bindingId);
        if (binding == null) {
            throw new IllegalArgumentException("binding not found: " + bindingId);
        }
        return binding.toView();
    }

    public List<Map<String, Object>> getBindingsByAsset(String assetType, String assetCode, String tenantId) {
        String resolvedTenantId = string(tenantId, DEFAULT_TENANT_ID);
        String resolvedAssetType = upper(assetType);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SourceAssetBinding binding : storedBindings()) {
            if (resolvedTenantId.equals(binding.getTenantId())
                    && (resolvedAssetType == null || resolvedAssetType.equalsIgnoreCase(binding.getAssetType()))
                    && assetCode.equals(binding.getAssetCode())) {
                result.add(binding.toView());
            }
        }
        return result;
    }

    public List<Map<String, Object>> getBindingsByDocument(String documentCode, String tenantId) {
        String resolvedTenantId = string(tenantId, DEFAULT_TENANT_ID);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SourceAssetBinding binding : storedBindings()) {
            if (resolvedTenantId.equals(binding.getTenantId())
                    && documentCode.equals(binding.getDocumentCode())) {
                result.add(binding.toView());
            }
        }
        return result;
    }

    public int bindingCount() {
        return bindingStore.size();
    }

    @SuppressWarnings("unchecked")
    private ImportEnvelope normalize(Object request) {
        ImportEnvelope envelope = new ImportEnvelope();
        envelope.tenantId = DEFAULT_TENANT_ID;
        if (request instanceof List) {
            envelope.bindings.addAll((List<Map<String, Object>>) request);
            return envelope;
        }
        if (request instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) request;
            envelope.tenantId = string(value(body, "tenant_id", "tenantId"), DEFAULT_TENANT_ID);
            envelope.operatorId = string(value(body, "operator_id", "operatorId"), null);
            Object nested = value(body, "bindings", "bindings");
            if (nested instanceof Collection) {
                for (Object item : (Collection<?>) nested) {
                    if (item instanceof Map) {
                        envelope.bindings.add((Map<String, Object>) item);
                    }
                }
            } else {
                envelope.bindings.add(body);
            }
        }
        return envelope;
    }

    private SourceAssetBinding toBinding(Map<String, Object> payload, ImportEnvelope envelope, String now) {
        SourceAssetBinding binding = new SourceAssetBinding();
        binding.setTenantId(string(value(payload, "tenant_id", "tenantId"), envelope.tenantId));
        String bindingId = string(value(payload, "binding_id", "bindingId"), null);
        if (bindingId == null) {
            bindingId = "BIND_" + idSequence.incrementAndGet();
        }
        binding.setBindingId(bindingId);
        String assetType = upper(required(payload, "asset_type", "assetType"));
        if (!SUPPORTED_ASSET_TYPES.contains(assetType)) {
            throw new IllegalArgumentException("unsupported asset_type: " + assetType);
        }
        binding.setAssetType(assetType);
        binding.setAssetCode(required(payload, "asset_code", "assetCode"));
        binding.setDocumentCode(required(payload, "document_code", "documentCode"));
        binding.setCitationId(string(value(payload, "citation_id", "citationId"), null));
        String bindingType = upper(string(value(payload, "binding_type", "bindingType"), "REFERENCE"));
        if (!SUPPORTED_BINDING_TYPES.contains(bindingType)) {
            throw new IllegalArgumentException("unsupported binding_type: " + bindingType);
        }
        binding.setBindingType(bindingType);
        binding.setConfidence(string(value(payload, "confidence", "confidence"), null));
        binding.setDescription(string(value(payload, "description", "description"), null));
        binding.setCreatedBy(string(value(payload, "created_by", "createdBy"), envelope.operatorId));
        binding.setCreatedTime(now);
        binding.setUpdatedTime(now);
        return binding;
    }

    private SourceAssetBinding findStoredBinding(String tenantId, String bindingId) {
        return bindingStore.get(key(tenantId, bindingId));
    }

    private List<SourceAssetBinding> storedBindings() {
        return sortedBindings(new ArrayList<SourceAssetBinding>(bindingStore.values()));
    }

    private List<SourceAssetBinding> sortedBindings(List<SourceAssetBinding> bindings) {
        Collections.sort(bindings, new Comparator<SourceAssetBinding>() {
            @Override
            public int compare(SourceAssetBinding left, SourceAssetBinding right) {
                int byTenant = string(left.getTenantId(), DEFAULT_TENANT_ID)
                        .compareTo(string(right.getTenantId(), DEFAULT_TENANT_ID));
                if (byTenant != 0) {
                    return byTenant;
                }
                return string(left.getBindingId(), "")
                        .compareTo(string(right.getBindingId(), ""));
            }
        });
        return bindings;
    }

    private void collectWarnings(SourceAssetBinding binding, List<Map<String, Object>> warnings) {
        if (binding.getCitationId() == null) {
            Map<String, Object> warning = new LinkedHashMap<String, Object>();
            warning.put("severity", "INFO");
            warning.put("binding_id", binding.getBindingId());
            warning.put("field", "citation_id");
            warning.put("message", "binding has no citation_id; source reference is at document level only");
            warnings.add(warning);
        }
    }

    private String key(String tenantId, String bindingId) {
        return string(tenantId, DEFAULT_TENANT_ID) + "::" + bindingId;
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
            persistenceService.saveAuditLog("PROVENANCE", actionType, "SRC_ASSET_BINDING",
                    null, null, null, operatorId, detail);
        } catch (RuntimeException ignored) {
        }
    }

    private static class ImportEnvelope {
        private String tenantId;
        private String operatorId;
        private final List<Map<String, Object>> bindings = new ArrayList<Map<String, Object>>();
    }
}
