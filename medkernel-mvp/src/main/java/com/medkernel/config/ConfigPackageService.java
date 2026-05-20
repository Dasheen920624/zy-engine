package com.medkernel.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.medkernel.audit.PublishGateService;
import com.medkernel.organization.OrganizationDirectoryService;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.provenance.PublishGateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ConfigPackageService.class);
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
    private final ConfigPackageRepository configPackageRepository;
    private final PublishGateService publishGateService;
    private final Map<String, ConfigPackage> packageStore = new ConcurrentHashMap<String, ConfigPackage>();

    public ConfigPackageService(ObjectMapper objectMapper, EnginePersistenceService persistenceService,
                                OrganizationDirectoryService organizationDirectoryService,
                                ConfigPackageRepository configPackageRepository,
                                PublishGateService publishGateService) {
        this.canonicalMapper = objectMapper.copy();
        this.canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.persistenceService = persistenceService;
        this.organizationDirectoryService = organizationDirectoryService;
        this.configPackageRepository = configPackageRepository;
        this.publishGateService = publishGateService;
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

            // Check if package already exists in database
            ConfigPackageEntity existingEntity = configPackageRepository.findByUniqueKey(
                    configPackage.getTenantId(), configPackage.getPackageCode(),
                    configPackage.getPackageVersion(), configPackage.getAssetType(),
                    configPackage.getScopeLevel(), configPackage.getScopeCode());

            if (existingEntity != null) {
                ConfigPackage existing = existingEntity.toConfigPackage();
                if (!existing.getContentHash().equals(configPackage.getContentHash())) {
                    throw new IllegalArgumentException("config package version already exists with different content_hash: "
                            + configPackage.getPackageCode() + "@" + configPackage.getPackageVersion());
                }
                imported.add(summary(existing));
            } else {
                // Save to database
                saveToDatabase(configPackage);
                // Also keep in memory store for backward compatibility
                packageStore.put(storeKey, configPackage);
                audit("IMPORT", configPackage, configPackage.getCreatedBy(), summary(configPackage));
                imported.add(summary(configPackage));
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

        // Try to load from database first
        List<ConfigPackageEntity> entities = configPackageRepository.findList(
                tenantId, null, assetType, status, scopeLevel, scopeCode);

        List<ConfigPackage> packages = new ArrayList<ConfigPackage>();
        for (ConfigPackageEntity entity : entities) {
            ConfigPackage configPackage = entity.toConfigPackage();
            // Load JSON fields from database
            loadJsonFields(configPackage, entity);
            packages.add(configPackage);
        }

        // If database is not enabled, fall back to in-memory store
        if (packages.isEmpty() && !persistenceService.enabled()) {
            packages.addAll(packageStore.values());
        }

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
            // Update database
            updateDatabase(configPackage);
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

        // 发布门禁：配置包来源审查校验（阻断级，对应产品不变量 H4）
        @SuppressWarnings("unchecked")
        Map<String, Object> sourceReview = (Map<String, Object>) review.get("source_review");
        PublishGateService.GateCheckResult gateResult = publishGateService.checkConfigPackageSourceReview(sourceReview);
        publishGateService.auditGateCheck("CONFIG_PACKAGE", "PUBLISH_GATE", "CONFIG_PACKAGE", packageCode, approvedBy, gateResult);
        if (!gateResult.isReadyToPublish()) {
            throw new IllegalStateException(publishGateService.formatBlockingMessage(gateResult));
        }

        configPackage.setStatus("PUBLISHED");
        configPackage.setApprovedBy(approvedBy);
        configPackage.setPublishedTime(nowText());

        // Update database
        updateDatabase(configPackage);

        Map<String, Object> result = detail(configPackage);
        audit("PUBLISH", configPackage, approvedBy, result);
        return result;
    }

    public Map<String, Object> exportPackage(String packageCode, String packageVersion, String tenantId) {
        ConfigPackage configPackage = findPackage(packageCode, packageVersion, tenantId);
        Map<String, Object> result = detail(configPackage);
        result.put("exported_time", nowText());
        result.put("export_format", "MEDKERNEL_CONFIG_PACKAGE_V1");
        return result;
    }

    public Map<String, Object> buildReview(ConfigPackage configPackage) {
        List<Map<String, Object>> issues = new ArrayList<Map<String, Object>>();
        Map<String, Object> sourceReview = sourceReview(configPackage);
        validateForReview(configPackage, issues);
        validateSourceReview(sourceReview, issues);

        // REFIT-003: 发布门禁 - 使用真实的来源系统检查
        Map<String, Object> publishGateResult = publishGateService.checkPublishGate(
                "CONFIG_PACKAGE", configPackage.getPackageCode(), configPackage.getTenantId());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> gateIssues = (List<Map<String, Object>>) publishGateResult.get("issues");
        if (gateIssues != null) {
            issues.addAll(gateIssues);
        }

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
        review.put("source_review", sourceReview);
        review.put("publish_gate", publishGateResult);
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

        // DB 优先：仅在 DB enabled 时尝试查 DB，否则直接走内存（避免在 DB-only=false 时空查询后误判 not found）。
        if (persistenceService.enabled()) {
            if (resolvedVersion != null) {
                List<ConfigPackageEntity> entities = configPackageRepository.findList(
                        resolvedTenant, packageCode, null, null, null, null);
                for (ConfigPackageEntity entity : entities) {
                    if (resolvedVersion.equals(entity.getPackageVersion())) {
                        ConfigPackage configPackage = entity.toConfigPackage();
                        loadJsonFields(configPackage, entity);
                        return configPackage;
                    }
                }
                // DB 未命中时也允许 fall back 到内存：导入操作仍在 packageStore 中保留副本。
                return findPackageFromMemory(packageCode, resolvedVersion, resolvedTenant);
            }

            List<ConfigPackageEntity> entities = configPackageRepository.findList(
                    resolvedTenant, packageCode, null, null, null, null);
            ConfigPackage latest = null;
            for (ConfigPackageEntity entity : entities) {
                ConfigPackage configPackage = entity.toConfigPackage();
                loadJsonFields(configPackage, entity);
                if (latest == null || configPackage.getPackageVersion().compareTo(latest.getPackageVersion()) > 0) {
                    latest = configPackage;
                }
            }
            if (latest != null) {
                return latest;
            }
        }

        return findPackageFromMemory(packageCode, resolvedVersion, resolvedTenant);
    }

    private ConfigPackage findPackageFromMemory(String packageCode, String packageVersion, String tenantId) {
        if (packageVersion != null) {
            ConfigPackage configPackage = packageStore.get(key(tenantId, packageCode, packageVersion));
            if (configPackage == null) {
                throw new IllegalArgumentException("config package not found: "
                        + tenantId + "/" + packageCode + "@" + packageVersion);
            }
            return configPackage;
        }

        ConfigPackage latest = null;
        for (ConfigPackage configPackage : packageStore.values()) {
            if (!tenantId.equals(configPackage.getTenantId())) {
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
            throw new IllegalArgumentException("config package not found: " + tenantId + "/" + packageCode);
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
    private Map<String, Object> sourceReview(ConfigPackage configPackage) {
        Object fromManifest = configPackage.getManifest() == null ? null : configPackage.getManifest().get("source_review");
        if (!(fromManifest instanceof Map)) {
            fromManifest = configPackage.getFullSnapshot() == null ? null : configPackage.getFullSnapshot().get("source_review");
        }
        Map<String, Object> raw = fromManifest instanceof Map
                ? new LinkedHashMap<String, Object>((Map<String, Object>) fromManifest)
                : new LinkedHashMap<String, Object>();
        Map<String, Object> review = new LinkedHashMap<String, Object>();
        review.put("enabled", bool(raw.get("enabled"), false));
        review.put("blocked", bool(raw.get("blocked"), false));
        review.put("missing_count", intValue(raw.get("missing_count"), 0));
        review.put("expired_count", intValue(raw.get("expired_count"), 0));
        review.put("unreviewed_count", intValue(raw.get("unreviewed_count"), 0));
        review.put("allow_publish", bool(raw.get("allow_publish"), true));
        review.put("message", string(raw.get("message"), "SOURCE_REVIEW_NOT_ENABLED"));
        return review;
    }

    private void validateSourceReview(Map<String, Object> sourceReview, List<Map<String, Object>> issues) {
        if (!bool(sourceReview.get("enabled"), false)) {
            return;
        }
        if (bool(sourceReview.get("blocked"), false)) {
            issues.add(issue("ERROR", "source_review", "source review blocked publish"));
            return;
        }
        int missing = intValue(sourceReview.get("missing_count"), 0);
        int expired = intValue(sourceReview.get("expired_count"), 0);
        int unreviewed = intValue(sourceReview.get("unreviewed_count"), 0);
        if (missing > 0) {
            issues.add(issue("ERROR", "source_review.missing_count", "source review has missing sources: " + missing));
        }
        if (expired > 0) {
            issues.add(issue("ERROR", "source_review.expired_count", "source review has expired sources: " + expired));
        }
        if (unreviewed > 0) {
            issues.add(issue("ERROR", "source_review.unreviewed_count", "source review has unreviewed sources: " + unreviewed));
        }
        if (!bool(sourceReview.get("allow_publish"), true)) {
            issues.add(issue("ERROR", "source_review.allow_publish", "source review does not allow publish"));
        }
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

    private boolean bool(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return "true".equalsIgnoreCase(String.valueOf(value).trim());
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
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

    private void saveToDatabase(ConfigPackage configPackage) {
        try {
            ConfigPackageEntity entity = ConfigPackageEntity.fromConfigPackage(configPackage);
            // Serialize JSON fields
            entity.setManifestJson(serializeJson(configPackage.getManifest()));
            entity.setDiffJson(serializeJson(configPackage.getDiff()));
            entity.setFullSnapshotJson(serializeJson(configPackage.getFullSnapshot()));
            configPackageRepository.save(entity);
        } catch (RuntimeException ex) {
            // Log error but don't fail the operation
            System.err.println("Failed to save config package to database: " + ex.getMessage());
        }
    }

    private void updateDatabase(ConfigPackage configPackage) {
        try {
            // Find existing entity to get the ID
            List<ConfigPackageEntity> entities = configPackageRepository.findList(
                    configPackage.getTenantId(), configPackage.getPackageCode(),
                    null, null, null, null);
            for (ConfigPackageEntity entity : entities) {
                if (entity.getPackageVersion().equals(configPackage.getPackageVersion())) {
                    entity.setStatus(configPackage.getStatus());
                    entity.setReviewedBy(configPackage.getReviewedBy());
                    entity.setApprovedBy(configPackage.getApprovedBy());
                    entity.setReviewedTime(parseDateTime(configPackage.getReviewedTime()));
                    entity.setPublishedTime(parseDateTime(configPackage.getPublishedTime()));
                    configPackageRepository.save(entity);
                    break;
                }
            }
        } catch (RuntimeException ex) {
            // Log error but don't fail the operation
            System.err.println("Failed to update config package in database: " + ex.getMessage());
        }
    }

    private void loadJsonFields(ConfigPackage configPackage, ConfigPackageEntity entity) {
        try {
            if (entity.getManifestJson() != null) {
                configPackage.setManifest(deserializeJson(entity.getManifestJson()));
            }
            if (entity.getDiffJson() != null) {
                configPackage.setDiff(deserializeJson(entity.getDiffJson()));
            }
            if (entity.getFullSnapshotJson() != null) {
                configPackage.setFullSnapshot(deserializeJson(entity.getFullSnapshotJson()));
            }
        } catch (RuntimeException ex) {
            // Log error but don't fail the operation
            System.err.println("Failed to load JSON fields from database: " + ex.getMessage());
        }
    }

    private String serializeJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return canonicalMapper.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize JSON", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeJson(String json) {
        if (json == null || json.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return canonicalMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to deserialize JSON", ex);
        }
    }

    private java.time.LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return java.time.OffsetDateTime.parse(dateTimeStr).toLocalDateTime();
        } catch (Exception ex) {
            // 配置包元数据时间格式异常不阻断整包加载，但要落日志便于线上定位脏数据来源。
            if (log.isWarnEnabled()) {
                log.warn("ConfigPackage parseDateTime failed: text='{}', err={}", dateTimeStr, ex.getMessage());
            }
            return null;
        }
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
