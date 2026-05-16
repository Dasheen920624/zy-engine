package com.zyengine.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zyengine.organization.OrganizationDirectoryService;
import com.zyengine.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
public class ConfigPackageService {
    private static final String DEFAULT_TENANT_ID = "default";
    private static final List<String> SUPPORTED_ASSET_TYPES = Arrays.asList(
            "PATHWAY", "RULE", "GRAPH", "DIFY", "WORKFLOW", "TERMINOLOGY", "ADAPTER", "MIXED");
    private static final List<String> SUPPORTED_SCOPE_LEVELS = Arrays.asList(
            "PLATFORM", "GROUP", "HOSPITAL", "CAMPUS", "SITE", "DEPARTMENT");
    private static final List<String> SUPPORTED_STATUSES = Arrays.asList(
            "DRAFT", "REVIEWED", "PUBLISHED", "SYNCED", "ACTIVE", "RETIRED");

    private final ObjectMapper canonicalMapper;
    private final EnginePersistenceService persistenceService;
    private final OrganizationDirectoryService organizationDirectoryService;
    private final Map<String, ConfigPackage> packageStore = new ConcurrentHashMap<String, ConfigPackage>();

    public ConfigPackageService(ObjectMapper objectMapper, EnginePersistenceService persistenceService,
                                OrganizationDirectoryService organizationDirectoryService) {
        this.canonicalMapper = objectMapper.copy();
        this.canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.persistenceService = persistenceService;
        this.organizationDirectoryService = organizationDirectoryService;
    }

    public List<Map<String, Object>> importPackages(Object request) {
        List<Map<String, Object>> payloads = normalizePackages(request);
        if (payloads.isEmpty()) {
            throw new IllegalArgumentException("packages is required");
        }

        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> payload : payloads) {
            ConfigPackage configPackage = toConfigPackage(payload);
            String storeKey = key(configPackage.getTenantId(), configPackage.getPackageCode(),
                    configPackage.getPackageVersion());
            ConfigPackage existing = packageStore.get(storeKey);
            if (existing != null && !existing.getContentHash().equals(configPackage.getContentHash())) {
                throw new IllegalArgumentException("config package version already exists with different content_hash: "
                        + configPackage.getPackageCode() + "@" + configPackage.getPackageVersion());
            }
            if (existing == null) {
                packageStore.put(storeKey, configPackage);
                audit("IMPORT", configPackage, configPackage.getCreatedBy(), summary(configPackage));
                imported.add(summary(configPackage));
            } else {
                imported.add(summary(existing));
            }
        }
        return imported;
    }

    public List<Map<String, Object>> listPackages(Map<String, String> filters) {
        String assetType = filterValue(filters, "assetType");
        String tenantId = filterValue(filters, "tenantId");
        String status = filterValue(filters, "status");
        String scopeLevel = filterValue(filters, "scopeLevel");
        String scopeCode = filterValue(filters, "scopeCode");

        List<ConfigPackage> packages = new ArrayList<ConfigPackage>(packageStore.values());
        Collections.sort(packages, new Comparator<ConfigPackage>() {
            @Override
            public int compare(ConfigPackage left, ConfigPackage right) {
                int byCode = left.getPackageCode().compareTo(right.getPackageCode());
                return byCode != 0 ? byCode : left.getPackageVersion().compareTo(right.getPackageVersion());
            }
        });

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (ConfigPackage configPackage : packages) {
            if (tenantId != null && !tenantId.equals(configPackage.getTenantId())) {
                continue;
            }
            if (assetType != null && !assetType.equalsIgnoreCase(configPackage.getAssetType())) {
                continue;
            }
            if (status != null && !status.equalsIgnoreCase(configPackage.getStatus())) {
                continue;
            }
            if (scopeLevel != null && !scopeLevel.equalsIgnoreCase(configPackage.getScopeLevel())) {
                continue;
            }
            if (scopeCode != null && !scopeCode.equalsIgnoreCase(configPackage.getScopeCode())) {
                continue;
            }
            result.add(summary(configPackage));
        }
        return result;
    }

    public Map<String, Object> getPackage(String packageCode, String packageVersion, String tenantId) {
        return detail(findPackage(packageCode, packageVersion, tenantId));
    }

    public Map<String, Object> reviewPackage(String packageCode, String packageVersion, String tenantId,
                                             Map<String, Object> request) {
        ConfigPackage configPackage = findPackage(packageCode, packageVersion, tenantId);
        Map<String, Object> review = buildReview(configPackage);
        String reviewedBy = string(request == null ? null : request.get("reviewed_by"),
                string(request == null ? null : request.get("reviewedBy"), null));
        if (reviewedBy != null && Boolean.TRUE.equals(review.get("ready_to_publish"))) {
            configPackage.setStatus("REVIEWED");
            configPackage.setReviewedBy(reviewedBy);
            configPackage.setReviewedTime(nowText());
            review = buildReview(configPackage);
            audit("REVIEW", configPackage, reviewedBy, review);
        }
        return review;
    }

    public Map<String, Object> publishPackage(String packageCode, String packageVersion, String tenantId,
                                             Map<String, Object> request) {
        ConfigPackage configPackage = findPackage(packageCode, packageVersion, tenantId);
        if ("PUBLISHED".equals(configPackage.getStatus())) {
            return detail(configPackage);
        }

        Map<String, Object> review = buildReview(configPackage);
        if (!Boolean.TRUE.equals(review.get("ready_to_publish"))) {
            throw new IllegalArgumentException("config package is not ready to publish: " + review.get("issues"));
        }

        String approvedBy = string(request == null ? null : request.get("approved_by"),
                string(request == null ? null : request.get("approvedBy"), null));
        if (approvedBy == null) {
            throw new IllegalArgumentException("approved_by is required");
        }
        configPackage.setStatus("PUBLISHED");
        configPackage.setApprovedBy(approvedBy);
        configPackage.setPublishedTime(nowText());

        Map<String, Object> result = detail(configPackage);
        audit("PUBLISH", configPackage, approvedBy, result);
        return result;
    }

    public Map<String, Object> exportPackage(String packageCode, String packageVersion, String tenantId) {
        ConfigPackage configPackage = findPackage(packageCode, packageVersion, tenantId);
        Map<String, Object> result = detail(configPackage);
        result.put("exported_time", nowText());
        result.put("export_format", "ZYENGINE_CONFIG_PACKAGE_V1");
        return result;
    }

    public Map<String, Object> buildReview(ConfigPackage configPackage) {
        List<Map<String, Object>> issues = new ArrayList<Map<String, Object>>();
        validateForReview(configPackage, issues);

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("asset_count", assetCount(configPackage.getFullSnapshot()));
        summary.put("manifest_keys", sortedKeys(configPackage.getManifest()));
        summary.put("full_snapshot_present", !configPackage.getFullSnapshot().isEmpty());
        summary.put("diff_present", !configPackage.getDiff().isEmpty());
        summary.put("scope_exists", organizationDirectoryService.scopeExists(configPackage.getTenantId(),
                configPackage.getScopeLevel(), configPackage.getScopeCode()));

        Map<String, Object> review = summary(configPackage);
        review.put("declared_content_hash", configPackage.getDeclaredContentHash());
        review.put("ready_to_publish", issues.isEmpty()
                && ("DRAFT".equals(configPackage.getStatus()) || "REVIEWED".equals(configPackage.getStatus())));
        review.put("issues", issues);
        review.put("summary", summary);
        review.put("manifest", configPackage.getManifest());
        review.put("scope_reference", organizationDirectoryService.scopeReference(configPackage.getTenantId(),
                configPackage.getScopeLevel(), configPackage.getScopeCode()));
        return review;
    }

    private void validateForReview(ConfigPackage configPackage, List<Map<String, Object>> issues) {
        if (string(configPackage.getPackageCode(), null) == null) {
            issues.add(issue("ERROR", "package_code", "package_code is required"));
        }
        if (string(configPackage.getPackageVersion(), null) == null) {
            issues.add(issue("ERROR", "package_version", "package_version is required"));
        }
        if (!SUPPORTED_ASSET_TYPES.contains(configPackage.getAssetType())) {
            issues.add(issue("ERROR", "asset_type", "unsupported asset_type: " + configPackage.getAssetType()));
        }
        if (!SUPPORTED_SCOPE_LEVELS.contains(configPackage.getScopeLevel())) {
            issues.add(issue("ERROR", "scope_level", "unsupported scope_level: " + configPackage.getScopeLevel()));
        } else if (!organizationDirectoryService.scopeExists(configPackage.getTenantId(),
                configPackage.getScopeLevel(), configPackage.getScopeCode())) {
            issues.add(issue("ERROR", "scope_code", "scope not found in organization directory: "
                    + configPackage.getScopeLevel() + "/" + configPackage.getScopeCode()));
        }
        if (!SUPPORTED_STATUSES.contains(configPackage.getStatus())) {
            issues.add(issue("ERROR", "status", "unsupported status: " + configPackage.getStatus()));
        }
        if (configPackage.getFullSnapshot().isEmpty()) {
            issues.add(issue("ERROR", "full_snapshot", "full_snapshot is required"));
        }
        if (configPackage.getDeclaredContentHash() != null
                && !configPackage.getDeclaredContentHash().equals(configPackage.getContentHash())) {
            issues.add(issue("ERROR", "content_hash", "declared content_hash does not match calculated content_hash"));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizePackages(Object request) {
        List<Map<String, Object>> packages = new ArrayList<Map<String, Object>>();
        if (request instanceof List) {
            for (Object item : (List<?>) request) {
                if (item instanceof Map) {
                    packages.add((Map<String, Object>) item);
                }
            }
            return packages;
        }
        if (request instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) request;
            Object nested = map.get("packages");
            if (nested instanceof List) {
                return normalizePackages(nested);
            }
            packages.add(map);
        }
        return packages;
    }

    private ConfigPackage toConfigPackage(Map<String, Object> payload) {
        String packageCode = required(payload, "package_code", "packageCode");
        String packageVersion = required(payload, "package_version", "packageVersion");
        String assetType = upper(string(value(payload, "asset_type", "assetType"), null));
        if (assetType == null) {
            throw new IllegalArgumentException("asset_type is required");
        }

        ConfigPackage configPackage = new ConfigPackage();
        configPackage.setTenantId(string(value(payload, "tenant_id", "tenantId"), DEFAULT_TENANT_ID));
        configPackage.setPackageCode(packageCode);
        configPackage.setPackageVersion(packageVersion);
        configPackage.setAssetType(assetType);
        configPackage.setScopeLevel(upper(string(value(payload, "scope_level", "scopeLevel"), "PLATFORM")));
        configPackage.setScopeCode(string(value(payload, "scope_code", "scopeCode"), "DEFAULT"));
        configPackage.setStatus(upper(string(payload.get("status"), "DRAFT")));
        configPackage.setBaseVersion(string(value(payload, "base_version", "baseVersion"), null));
        configPackage.setTargetVersion(string(value(payload, "target_version", "targetVersion"), packageVersion));
        configPackage.setCreatedBy(string(value(payload, "created_by", "createdBy"), null));
        configPackage.setCreatedTime(nowText());
        configPackage.setFullSnapshot(snapshot(payload));
        configPackage.setDiff(mapValue(value(payload, "diff", "diff")));
        configPackage.setContentHash(hash(configPackage.getFullSnapshot()));
        configPackage.setDeclaredContentHash(string(value(payload, "content_hash", "contentHash"), null));
        configPackage.setManifest(manifest(payload, configPackage));
        return configPackage;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> snapshot(Map<String, Object> payload) {
        Object value = value(payload, "full_snapshot", "fullSnapshot");
        if (value == null) {
            value = payload.get("content");
        }
        if (value == null) {
            value = payload.get("assets");
        }
        if (value instanceof Map) {
            return new LinkedHashMap<String, Object>((Map<String, Object>) value);
        }
        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        if (value instanceof Collection) {
            snapshot.put("items", value);
        }
        return snapshot;
    }

    private Map<String, Object> manifest(Map<String, Object> payload, ConfigPackage configPackage) {
        Map<String, Object> manifest = mapValue(value(payload, "manifest", "manifest"));
        if (!manifest.isEmpty()) {
            manifest.put("content_hash", configPackage.getContentHash());
            return manifest;
        }
        manifest.put("package_code", configPackage.getPackageCode());
        manifest.put("package_version", configPackage.getPackageVersion());
        manifest.put("tenant_id", configPackage.getTenantId());
        manifest.put("asset_type", configPackage.getAssetType());
        manifest.put("scope_level", configPackage.getScopeLevel());
        manifest.put("scope_code", configPackage.getScopeCode());
        manifest.put("asset_count", assetCount(configPackage.getFullSnapshot()));
        manifest.put("snapshot_keys", sortedKeys(configPackage.getFullSnapshot()));
        manifest.put("content_hash", configPackage.getContentHash());
        return manifest;
    }

    private ConfigPackage findPackage(String packageCode, String packageVersion, String tenantId) {
        String resolvedTenant = string(tenantId, DEFAULT_TENANT_ID);
        String resolvedVersion = string(packageVersion, null);
        if (resolvedVersion != null) {
            ConfigPackage configPackage = packageStore.get(key(resolvedTenant, packageCode, resolvedVersion));
            if (configPackage == null) {
                throw new IllegalArgumentException("config package not found: "
                        + resolvedTenant + "/" + packageCode + "@" + resolvedVersion);
            }
            return configPackage;
        }

        ConfigPackage latest = null;
        for (ConfigPackage configPackage : packageStore.values()) {
            if (!resolvedTenant.equals(configPackage.getTenantId())) {
                continue;
            }
            if (!packageCode.equals(configPackage.getPackageCode())) {
                continue;
            }
            if (latest == null || configPackage.getPackageVersion().compareTo(latest.getPackageVersion()) > 0) {
                latest = configPackage;
            }
        }
        if (latest == null) {
            throw new IllegalArgumentException("config package not found: " + resolvedTenant + "/" + packageCode);
        }
        return latest;
    }

    private Map<String, Object> summary(ConfigPackage configPackage) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("tenant_id", configPackage.getTenantId());
        summary.put("package_code", configPackage.getPackageCode());
        summary.put("package_version", configPackage.getPackageVersion());
        summary.put("asset_type", configPackage.getAssetType());
        summary.put("scope_level", configPackage.getScopeLevel());
        summary.put("scope_code", configPackage.getScopeCode());
        summary.put("scope_reference", organizationDirectoryService.scopeReference(configPackage.getTenantId(),
                configPackage.getScopeLevel(), configPackage.getScopeCode()));
        summary.put("status", configPackage.getStatus());
        summary.put("base_version", configPackage.getBaseVersion());
        summary.put("target_version", configPackage.getTargetVersion());
        summary.put("content_hash", configPackage.getContentHash());
        summary.put("created_by", configPackage.getCreatedBy());
        summary.put("reviewed_by", configPackage.getReviewedBy());
        summary.put("approved_by", configPackage.getApprovedBy());
        summary.put("created_time", configPackage.getCreatedTime());
        summary.put("reviewed_time", configPackage.getReviewedTime());
        summary.put("published_time", configPackage.getPublishedTime());
        return summary;
    }

    private Map<String, Object> detail(ConfigPackage configPackage) {
        Map<String, Object> detail = summary(configPackage);
        detail.put("declared_content_hash", configPackage.getDeclaredContentHash());
        detail.put("manifest", configPackage.getManifest());
        detail.put("diff", configPackage.getDiff());
        detail.put("full_snapshot", configPackage.getFullSnapshot());
        return detail;
    }

    private Map<String, Object> issue(String severity, String field, String message) {
        Map<String, Object> issue = new LinkedHashMap<String, Object>();
        issue.put("severity", severity);
        issue.put("field", field);
        issue.put("message", message);
        return issue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map) {
            return new LinkedHashMap<String, Object>((Map<String, Object>) value);
        }
        return new LinkedHashMap<String, Object>();
    }

    private int assetCount(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return 0;
        }
        for (Object value : snapshot.values()) {
            if (value instanceof Collection) {
                return ((Collection<?>) value).size();
            }
        }
        return 1;
    }

    private List<String> sortedKeys(Map<String, Object> map) {
        List<String> keys = new ArrayList<String>();
        if (map != null) {
            keys.addAll(map.keySet());
        }
        Collections.sort(keys);
        return keys;
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

    private String key(String tenantId, String packageCode, String packageVersion) {
        return tenantId + "::" + packageCode + "::" + packageVersion;
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text.trim();
    }

    private String upper(String text) {
        return text == null ? null : text.trim().toUpperCase();
    }

    private String hash(Object value) {
        try {
            byte[] json = canonicalMapper.writeValueAsBytes(value);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + hex(digest.digest(json));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("config package JSON serialization failed", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String text = Integer.toHexString(b & 0xff);
            if (text.length() == 1) {
                builder.append('0');
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private void audit(String actionType, ConfigPackage configPackage, String operatorId, Map<String, Object> detail) {
        try {
            persistenceService.saveAuditLog("CONFIG_PACKAGE", actionType, "CONFIG_PACKAGE",
                    configPackage.getPackageCode(), null, null, operatorId, detail);
        } catch (RuntimeException ignored) {
            // 配置包内存态流程不因审计落库失败中断。
        }
    }

    private String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }
}
