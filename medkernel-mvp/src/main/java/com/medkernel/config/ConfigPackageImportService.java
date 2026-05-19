package com.medkernel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ConfigPackageImportService {

    private static final List<String> REQUIRED_MANIFEST_FIELDS = new ArrayList<String>() {{
        add("package_code");
        add("package_version");
        add("asset_type");
    }};

    private final ConfigPackageService configPackageService;
    private final ObjectMapper objectMapper;

    private final Map<String, Map<String, Object>> importSessionStore = new HashMap<String, Map<String, Object>>();

    public ConfigPackageImportService(ConfigPackageService configPackageService, ObjectMapper objectMapper) {
        this.configPackageService = configPackageService;
        this.objectMapper = objectMapper;
    }

    /**
     * Step 1: 上传配置包文件，解析 JSON/ZIP，提取 manifest，生成 import_id
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> upload(MultipartFile file, String tenantId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        String originalFilename = file.getOriginalFilename();
        Map<String, Object> manifest;
        Object packagesPayload;

        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".zip")) {
            Map<String, Object> zipResult = parseZip(file);
            manifest = (Map<String, Object>) zipResult.get("manifest");
            packagesPayload = zipResult.get("packages");
        } else {
            // 默认按 JSON 解析
            try {
                Object parsed = objectMapper.readValue(file.getInputStream(), Object.class);
                if (parsed instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) parsed;
                    manifest = (Map<String, Object>) map.get("manifest");
                    if (manifest == null) {
                        manifest = extractManifestFromPackage(map);
                    }
                    packagesPayload = map.get("packages");
                    if (packagesPayload == null) {
                        packagesPayload = map;
                    }
                } else if (parsed instanceof List) {
                    packagesPayload = parsed;
                    manifest = extractManifestFromFirstItem((List<Map<String, Object>>) parsed);
                } else {
                    throw new IllegalArgumentException("unsupported file content format");
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("failed to parse file: " + e.getMessage(), e);
            }
        }

        if (manifest == null) {
            manifest = new LinkedHashMap<String, Object>();
        }

        String importId = UUID.randomUUID().toString();
        String contentHash = computeHash(file);

        Map<String, Object> session = new LinkedHashMap<String, Object>();
        session.put("import_id", importId);
        session.put("tenant_id", tenantId);
        session.put("manifest", manifest);
        session.put("packages_payload", packagesPayload);
        session.put("content_hash", contentHash);
        session.put("original_filename", originalFilename);
        session.put("status", "UPLOADED");

        importSessionStore.put(importId, session);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("import_id", importId);
        result.put("manifest", manifest);
        result.put("content_hash", contentHash);
        result.put("status", "UPLOADED");
        return result;
    }

    /**
     * Step 2: 校验包完整性（必填字段、content_hash 校验）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> validate(Map<String, Object> request, String tenantId) {
        String importId = string(request.get("import_id"));
        if (importId == null) {
            throw new IllegalArgumentException("import_id is required");
        }

        Map<String, Object> session = importSessionStore.get(importId);
        if (session == null) {
            throw new IllegalArgumentException("import session not found: " + importId);
        }

        Map<String, Object> manifest = (Map<String, Object>) session.get("manifest");
        List<String> errors = new ArrayList<String>();
        List<String> warnings = new ArrayList<String>();

        // 校验 manifest 必填字段
        for (String field : REQUIRED_MANIFEST_FIELDS) {
            if (manifest == null || string(manifest.get(field)) == null) {
                errors.add(field + " is required in manifest");
            }
        }

        // content_hash 校验
        String declaredHash = string(request.get("content_hash"));
        if (declaredHash != null) {
            String actualHash = string(session.get("content_hash"));
            if (!declaredHash.equals(actualHash)) {
                errors.add("content_hash mismatch: declared=" + declaredHash + ", actual=" + actualHash);
            }
        }

        session.put("status", errors.isEmpty() ? "VALIDATED" : "VALIDATION_FAILED");

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("import_id", importId);
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("status", session.get("status"));
        return result;
    }

    /**
     * Step 3: 检查来源引用完整性，MISSING_SOURCE 必须阻断
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> sourceCheck(Map<String, Object> request, String tenantId) {
        String importId = string(request.get("import_id"));
        if (importId == null) {
            throw new IllegalArgumentException("import_id is required");
        }

        Map<String, Object> session = importSessionStore.get(importId);
        if (session == null) {
            throw new IllegalArgumentException("import session not found: " + importId);
        }

        Map<String, Object> manifest = (Map<String, Object>) session.get("manifest");
        List<String> missingSources = new ArrayList<String>();

        // 检查 manifest 中的来源引用
        if (manifest != null) {
            Object sources = manifest.get("sources");
            if (sources instanceof List) {
                List<Map<String, Object>> sourceList = (List<Map<String, Object>>) sources;
                for (Map<String, Object> source : sourceList) {
                    String sourceType = string(source.get("type"));
                    String sourceRef = string(source.get("ref"));
                    if (sourceType == null || sourceRef == null) {
                        missingSources.add("source entry missing type or ref");
                    } else if (!isSourceAvailable(sourceType, sourceRef, tenantId)) {
                        missingSources.add(sourceType + "/" + sourceRef);
                    }
                }
            }
        }

        boolean allowPublish = missingSources.isEmpty();
        session.put("allow_publish", allowPublish);
        session.put("missing_sources", missingSources);
        session.put("status", allowPublish ? "SOURCE_CHECK_PASSED" : "SOURCE_CHECK_FAILED");

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("import_id", importId);
        result.put("allow_publish", allowPublish);
        result.put("missing_sources", missingSources);
        result.put("status", session.get("status"));
        return result;
    }

    /**
     * Step 4: 评估对目标环境的影响范围
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> impact(Map<String, Object> request, String tenantId) {
        String importId = string(request.get("import_id"));
        if (importId == null) {
            throw new IllegalArgumentException("import_id is required");
        }

        Map<String, Object> session = importSessionStore.get(importId);
        if (session == null) {
            throw new IllegalArgumentException("import session not found: " + importId);
        }

        Map<String, Object> manifest = (Map<String, Object>) session.get("manifest");

        List<Map<String, Object>> affectedItems = new ArrayList<Map<String, Object>>();
        int newCount = 0;
        int updateCount = 0;
        int retireCount = 0;

        // 根据 manifest 评估影响
        if (manifest != null) {
            String packageCode = string(manifest.get("package_code"));
            String packageVersion = string(manifest.get("package_version"));
            String assetType = string(manifest.get("asset_type"));

            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("package_code", packageCode);
            item.put("package_version", packageVersion);
            item.put("asset_type", assetType);
            item.put("action", "UPSERT");
            affectedItems.add(item);
            updateCount = 1;
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total", affectedItems.size());
        summary.put("new_count", newCount);
        summary.put("update_count", updateCount);
        summary.put("retire_count", retireCount);

        session.put("impact_summary", summary);
        session.put("status", "IMPACT_ASSESSED");

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("import_id", importId);
        result.put("affected_items", affectedItems);
        result.put("summary", summary);
        result.put("status", session.get("status"));
        return result;
    }

    /**
     * Step 5: 执行最终导入，调用 ConfigPackageService.importPackages()
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> confirm(Map<String, Object> request, String tenantId) {
        String importId = string(request.get("import_id"));
        if (importId == null) {
            throw new IllegalArgumentException("import_id is required");
        }

        Map<String, Object> session = importSessionStore.get(importId);
        if (session == null) {
            throw new IllegalArgumentException("import session not found: " + importId);
        }

        // 检查前置步骤状态
        Boolean allowPublish = (Boolean) session.get("allow_publish");
        if (Boolean.FALSE.equals(allowPublish)) {
            throw new IllegalStateException("source check not passed, cannot confirm import");
        }

        Object packagesPayload = session.get("packages_payload");
        if (packagesPayload == null) {
            throw new IllegalStateException("no packages payload found in import session");
        }

        // 调用已有的 ConfigPackageService.importPackages()
        List<Map<String, Object>> imported = configPackageService.importPackages(packagesPayload);

        session.put("status", "CONFIRMED");
        session.put("imported", imported);

        // 清理会话
        importSessionStore.remove(importId);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("import_id", importId);
        result.put("status", "CONFIRMED");
        result.put("imported", imported);
        return result;
    }

    // ---- internal helpers ----

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseZip(MultipartFile file) {
        Map<String, Object> manifest = null;
        Object packages = null;

        try (InputStream is = file.getInputStream();
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                byte[] bytes = readAll(zis);
                if ("manifest.json".equals(name)) {
                    manifest = objectMapper.readValue(bytes, Map.class);
                } else if ("packages.json".equals(name)) {
                    packages = objectMapper.readValue(bytes, Object.class);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to parse zip file: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("manifest", manifest);
        result.put("packages", packages);
        return result;
    }

    private byte[] readAll(ZipInputStream zis) throws IOException {
        byte[] buffer = new byte[4096];
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int len;
        while ((len = zis.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    private Map<String, Object> extractManifestFromPackage(Map<String, Object> map) {
        Map<String, Object> manifest = new LinkedHashMap<String, Object>();
        copyIfExists(manifest, map, "package_code");
        copyIfExists(manifest, map, "package_version");
        copyIfExists(manifest, map, "asset_type");
        copyIfExists(manifest, map, "scope_level");
        copyIfExists(manifest, map, "scope_code");
        copyIfExists(manifest, map, "created_by");
        copyIfExists(manifest, map, "sources");
        return manifest;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractManifestFromFirstItem(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }
        return extractManifestFromPackage(items.get(0));
    }

    private void copyIfExists(Map<String, Object> target, Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }

    private boolean isSourceAvailable(String sourceType, String sourceRef, String tenantId) {
        // 简单实现：对于 RULE/PATHWAY/GRAPH 等来源类型，检查是否已存在
        // 当前版本默认返回 true，后续可接入实际检查
        return true;
    }

    private String computeHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "unsupported";
        } catch (IOException e) {
            return "error";
        }
    }

    private String string(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
