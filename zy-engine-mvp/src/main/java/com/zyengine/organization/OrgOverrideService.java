package com.zyengine.organization;

import com.zyengine.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrgOverrideService {
    private static final String DEFAULT_TENANT_ID = "default";
    private static final List<String> SCOPE_LEVEL_ORDER = new ArrayList<String>();

    static {
        SCOPE_LEVEL_ORDER.add("DEPARTMENT");
        SCOPE_LEVEL_ORDER.add("SITE");
        SCOPE_LEVEL_ORDER.add("CAMPUS");
        SCOPE_LEVEL_ORDER.add("HOSPITAL");
        SCOPE_LEVEL_ORDER.add("GROUP");
        SCOPE_LEVEL_ORDER.add("PLATFORM");
    }

    private final EnginePersistenceService persistenceService;
    private final OrganizationDirectoryService organizationDirectoryService;
    private final Map<String, List<OrgOverrideEntry>> overrideStore =
            new ConcurrentHashMap<String, List<OrgOverrideEntry>>();

    public OrgOverrideService(EnginePersistenceService persistenceService,
                              OrganizationDirectoryService organizationDirectoryService) {
        this.persistenceService = persistenceService;
        this.organizationDirectoryService = organizationDirectoryService;
    }

    public List<Map<String, Object>> importEntries(Object request) {
        ImportEnvelope envelope = normalize(request);
        if (envelope.entries.isEmpty()) {
            throw new IllegalArgumentException("entries is required");
        }

        List<Map<String, Object>> imported = new ArrayList<>();
        String now = nowText();
        for (Map<String, Object> payload : envelope.entries) {
            OrgOverrideEntry entry = toEntry(payload, envelope, now);
            String storeKey = storeKey(entry);
            List<OrgOverrideEntry> bucket = overrideStore.get(storeKey);
            if (bucket == null) {
                bucket = new ArrayList<>();
                overrideStore.put(storeKey, bucket);
            }
            bucket.add(entry);
            imported.add(entry.toView());
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("imported_count", imported.size());
        audit("IMPORT", "ORG_OVERRIDE_BATCH", envelope.tenantId, envelope.operatorId, detail);
        return imported;
    }

    public List<Map<String, Object>> listEntries(Map<String, String> filters) {
        String tenantId = filterValue(filters, "tenantId");
        String scopeLevel = upper(filterValue(filters, "scopeLevel"));
        String scopeCode = filterValue(filters, "scopeCode");
        String assetType = filterValue(filters, "assetType");
        String overrideKey = filterValue(filters, "overrideKey");
        int limit = filterInt(filters, "limit", 200);
        if (limit <= 0) {
            limit = 200;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (List<OrgOverrideEntry> bucket : overrideStore.values()) {
            for (OrgOverrideEntry entry : bucket) {
                if (tenantId != null && !tenantId.equals(entry.getTenantId())) continue;
                if (scopeLevel != null && !scopeLevel.equals(entry.getScopeLevel())) continue;
                if (scopeCode != null && !scopeCode.equals(entry.getScopeCode())) continue;
                if (assetType != null && !assetType.equals(entry.getAssetType())) continue;
                if (overrideKey != null && !overrideKey.equals(entry.getOverrideKey())) continue;
                result.add(entry.toView());
                if (result.size() >= limit) break;
            }
            if (result.size() >= limit) break;
        }
        return result;
    }

    public Map<String, Object> computeOverride(OrganizationContext context, String assetType) {
        String tenantId = context.getTenantId() == null ? DEFAULT_TENANT_ID : context.getTenantId();
        List<Map<String, String>> inheritanceChain = buildInheritanceChain(context);
        Map<String, OverrideSource> merged = new LinkedHashMap<>();

        // 继承覆盖顺序：从粗粒度 (PLATFORM/GROUP) 到细粒度 (DEPARTMENT)，
        // 让细粒度的 put 覆盖粗粒度，符合"DEPARTMENT > SITE > CAMPUS > HOSPITAL > GROUP > PLATFORM"语义。
        // buildInheritanceChain 默认按 DEPARTMENT→PLATFORM 顺序生成，遍历时必须反转才能正确覆盖。
        List<Map<String, String>> reversedChain = new ArrayList<>(inheritanceChain);
        Collections.reverse(reversedChain);
        for (Map<String, String> scope : reversedChain) {
            String level = scope.get("scope_level");
            String code = scope.get("scope_code");
            List<OrgOverrideEntry> matches = findEntries(tenantId, level, code, assetType);
            for (OrgOverrideEntry entry : matches) {
                merged.put(entry.getOverrideKey(),
                        new OverrideSource(entry.getOverrideValue(), level + "/" + code, level));
            }
        }

        Map<String, Object> effective = new LinkedHashMap<>();
        List<Map<String, Object>> sources = new ArrayList<>();
        for (Map.Entry<String, OverrideSource> e : merged.entrySet()) {
            effective.put(e.getKey(), e.getValue().value);
            Map<String, Object> sourceInfo = new LinkedHashMap<>();
            sourceInfo.put("override_key", e.getKey());
            sourceInfo.put("resolved_value", e.getValue().value);
            sourceInfo.put("resolved_from", e.getValue().source);
            sourceInfo.put("resolved_level", e.getValue().level);
            sources.add(sourceInfo);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenant_id", tenantId);
        result.put("asset_type", assetType);
        result.put("effective", effective);
        result.put("source_resolved_count", sources.size());
        result.put("resolved_sources", sources);
        result.put("inheritance_chain", inheritanceChain);
        return result;
    }

    public Map<String, Object> resolveOverride(OrganizationContext context, String assetType, String overrideKey) {
        if (overrideKey == null || overrideKey.trim().isEmpty()) {
            throw new IllegalArgumentException("overrideKey is required");
        }

        Map<String, Object> computed = computeOverride(context, assetType);
        Map<String, Object> effective = asMap(computed.get("effective"));
        List<Map<String, Object>> sources = asListOfMap(computed.get("resolved_sources"));

        String resolvedFrom = null;
        String resolvedLevel = null;
        for (Map<String, Object> source : sources) {
            if (overrideKey.equals(source.get("override_key"))) {
                resolvedFrom = string(source.get("resolved_from"), null);
                resolvedLevel = string(source.get("resolved_level"), null);
                break;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("override_key", overrideKey);
        result.put("resolved_value", effective.get(overrideKey));
        result.put("resolved_from", resolvedFrom);
        result.put("resolved_level", resolvedLevel);
        result.put("tenant_id", computed.get("tenant_id"));
        result.put("asset_type", computed.get("asset_type"));
        result.put("inheritance_chain", computed.get("inheritance_chain"));
        return result;
    }

    private List<Map<String, String>> buildInheritanceChain(OrganizationContext context) {
        List<Map<String, String>> chain = new ArrayList<>();
        addScopeIfPresent(chain, "DEPARTMENT", context.getDepartmentCode());
        addScopeIfPresent(chain, "SITE", context.getSiteCode());
        addScopeIfPresent(chain, "CAMPUS", context.getCampusCode());
        addScopeIfPresent(chain, "HOSPITAL", context.getHospitalCode());
        addScopeIfPresent(chain, "GROUP", context.getGroupCode());
        addScopeIfPresent(chain, "PLATFORM", "DEFAULT");
        return chain;
    }

    private void addScopeIfPresent(List<Map<String, String>> chain, String level, String code) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        Map<String, String> scope = new LinkedHashMap<>();
        scope.put("scope_level", level);
        scope.put("scope_code", code);
        chain.add(scope);
    }

    private List<OrgOverrideEntry> findEntries(String tenantId, String level, String code, String assetType) {
        String storeKey = storeKey(tenantId, level, code);
        List<OrgOverrideEntry> bucket = overrideStore.get(storeKey);
        if (bucket == null) {
            return Collections.emptyList();
        }
        if (assetType == null) {
            return bucket;
        }
        List<OrgOverrideEntry> filtered = new ArrayList<>();
        for (OrgOverrideEntry entry : bucket) {
            if (assetType.equals(entry.getAssetType()) || entry.getAssetType() == null) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public boolean hasOverride(String tenantId, String scopeLevel, String scopeCode,
                               String assetType, String overrideKey) {
        String storeKey = storeKey(tenantId, scopeLevel, scopeCode);
        List<OrgOverrideEntry> bucket = overrideStore.get(storeKey);
        if (bucket == null) return false;
        for (OrgOverrideEntry entry : bucket) {
            if (overrideKey.equals(entry.getOverrideKey())) {
                if (assetType == null || assetType.equals(entry.getAssetType()) || entry.getAssetType() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    public int entryCount() {
        int count = 0;
        for (List<OrgOverrideEntry> bucket : overrideStore.values()) {
            count += bucket.size();
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private ImportEnvelope normalize(Object request) {
        ImportEnvelope envelope = new ImportEnvelope();
        envelope.tenantId = DEFAULT_TENANT_ID;
        if (request instanceof List) {
            for (Object item : (List<?>) request) {
                if (item instanceof Map) {
                    envelope.entries.add((Map<String, Object>) item);
                }
            }
            return envelope;
        }
        if (request instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) request;
            envelope.tenantId = string(value(body, "tenant_id", "tenantId"), DEFAULT_TENANT_ID);
            envelope.operatorId = string(value(body, "operator_id", "operatorId"), null);
            Object nested = body.get("entries");
            if (nested instanceof Collection) {
                for (Object item : (Collection<?>) nested) {
                    if (item instanceof Map) {
                        envelope.entries.add((Map<String, Object>) item);
                    }
                }
            } else {
                envelope.entries.add(body);
            }
        }
        return envelope;
    }

    private OrgOverrideEntry toEntry(Map<String, Object> payload, ImportEnvelope envelope, String now) {
        OrgOverrideEntry entry = new OrgOverrideEntry();
        entry.setTenantId(string(value(payload, "tenant_id", "tenantId"), envelope.tenantId));
        String scopeLevel = upper(required(payload, "scope_level", "scopeLevel"));
        if (!SCOPE_LEVEL_ORDER.contains(scopeLevel)) {
            throw new IllegalArgumentException("unsupported scope_level: " + scopeLevel);
        }
        entry.setScopeLevel(scopeLevel);
        entry.setScopeCode(required(payload, "scope_code", "scopeCode"));
        entry.setAssetType(upper(string(value(payload, "asset_type", "assetType"), null)));
        entry.setOverrideKey(required(payload, "override_key", "overrideKey"));
        entry.setOverrideValue(value(payload, "override_value", "overrideValue"));
        entry.setSourceLevel(scopeLevel);
        entry.setDescription(string(value(payload, "description", null), null));
        entry.setCreatedBy(string(value(payload, "created_by", "createdBy"), envelope.operatorId));
        entry.setCreatedTime(now);
        return entry;
    }

    private String storeKey(String tenantId, String scopeLevel, String scopeCode) {
        return text(tenantId, DEFAULT_TENANT_ID) + "::" + upper(scopeLevel) + "::" + scopeCode;
    }

    private String storeKey(OrgOverrideEntry entry) {
        return storeKey(entry.getTenantId(), entry.getScopeLevel(), entry.getScopeCode());
    }

    private String string(Object value, String defaultValue) {
        if (value == null) return defaultValue;
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text.trim();
    }

    private String text(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String required(Map<String, Object> map, String snakeKey, String camelKey) {
        Object value = map.get(snakeKey);
        if (value == null) value = map.get(camelKey);
        if (value == null) throw new IllegalArgumentException(snakeKey + " is required");
        return String.valueOf(value).trim();
    }

    private Object value(Map<String, Object> map, String snakeKey, String camelKey) {
        Object value = map.get(snakeKey);
        return value == null ? map.get(camelKey) : value;
    }

    private String filterValue(Map<String, String> filters, String key) {
        if (filters == null) return null;
        String value = filters.get(key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private int filterInt(Map<String, String> filters, String key, int defaultValue) {
        String value = filterValue(filters, key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object obj) {
        return obj instanceof Map ? (Map<String, Object>) obj : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object obj) {
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                return (List<Map<String, Object>>) list;
            }
        }
        return new ArrayList<>();
    }

    private String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }

    private void audit(String actionType, String targetType, String targetCode,
                       String operatorId, Map<String, Object> detail) {
        try {
            persistenceService.saveAuditLog("ORG_OVERRIDE", actionType, targetType, targetCode,
                    null, null, operatorId, detail);
        } catch (RuntimeException ignored) {
        }
    }

    private static class OverrideSource {
        private final Object value;
        private final String source;
        private final String level;

        OverrideSource(Object value, String source, String level) {
            this.value = value;
            this.source = source;
            this.level = level;
        }
    }

    private static class ImportEnvelope {
        private String tenantId;
        private String operatorId;
        private final List<Map<String, Object>> entries = new ArrayList<>();
    }
}