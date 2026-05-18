package com.medkernel.organization;

import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.persistence.OrganizationPersistenceService;
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

@Service
public class OrganizationDirectoryService {
    private static final Logger log = LoggerFactory.getLogger(OrganizationDirectoryService.class);
    private static final String DEFAULT_TENANT_ID = "default";
    private static final List<String> REAL_ORG_LEVELS = Arrays.asList(
            "GROUP", "HOSPITAL", "CAMPUS", "SITE", "DEPARTMENT");
    private static final Map<String, String> EXPECTED_PARENT = expectedParentMap();

    private final EnginePersistenceService persistenceService;
    private final OrganizationPersistenceService orgPersistenceService;
    private final Map<String, OrganizationUnit> organizationStore =
            new ConcurrentHashMap<String, OrganizationUnit>();

    public OrganizationDirectoryService(EnginePersistenceService persistenceService,
                                        OrganizationPersistenceService orgPersistenceService) {
        this.persistenceService = persistenceService;
        this.orgPersistenceService = orgPersistenceService;
    }

    /**
     * 启动时从数据库加载组织目录到内存缓存。
     * 数据库不可用时保持空内存态，不影响内存演示。
     */
    @PostConstruct
    public void loadFromDatabase() {
        if (!orgPersistenceService.enabled()) {
            log.info("organization persistence disabled, starting with empty in-memory store");
            return;
        }
        try {
            List<OrganizationUnit> units = orgPersistenceService.loadAllOrganizationUnits();
            for (OrganizationUnit unit : units) {
                organizationStore.put(key(unit.getTenantId(), unit.getLevel(), unit.getCode()), unit);
            }
            log.info("loaded {} organization units from database into memory cache", units.size());
        } catch (RuntimeException ex) {
            log.warn("failed to load organization units from database, starting with empty store: {}", ex.getMessage());
        }
    }

    public Map<String, Object> importUnits(Object request) {
        ImportEnvelope envelope = normalize(request);
        if (envelope.units.isEmpty()) {
            throw new IllegalArgumentException("units is required");
        }

        List<Map<String, Object>> warnings = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        String now = nowText();
        for (Map<String, Object> payload : envelope.units) {
            OrganizationUnit unit = toUnit(payload, envelope, now);
            collectWarnings(unit, warnings);
            OrganizationUnit existing = organizationStore.get(key(unit.getTenantId(), unit.getLevel(), unit.getCode()));
            if (existing != null && existing.getCreatedTime() != null) {
                unit.setCreatedTime(existing.getCreatedTime());
                unit.setUpdatedTime(now);
            }
            organizationStore.put(key(unit.getTenantId(), unit.getLevel(), unit.getCode()), unit);
            persistUnit(unit);
            imported.add(toView(unit));
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("tenant_id", envelope.tenantId);
        result.put("imported_count", imported.size());
        result.put("warnings", warnings);
        result.put("units", imported);
        audit("IMPORT", "ORGANIZATION_BATCH", envelope.tenantId, envelope.operatorId, result);
        return result;
    }

    public List<Map<String, Object>> listUnits(Map<String, String> filters) {
        String tenantId = filterValue(filters, "tenantId");
        String level = upper(filterValue(filters, "level"));
        String parentLevel = upper(filterValue(filters, "parentLevel"));
        String parentCode = filterValue(filters, "parentCode");
        String status = upper(filterValue(filters, "status"));
        int limit = filterInt(filters, "limit", 200);
        if (limit <= 0) {
            limit = 200;
        }

        List<OrganizationUnit> units = sortedUnits();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (OrganizationUnit unit : units) {
            if (tenantId != null && !tenantId.equals(unit.getTenantId())) {
                continue;
            }
            if (level != null && !level.equals(unit.getLevel())) {
                continue;
            }
            if (parentLevel != null && !parentLevel.equals(unit.getParentLevel())) {
                continue;
            }
            if (parentCode != null && !parentCode.equals(unit.getParentCode())) {
                continue;
            }
            if (status != null && !status.equals(unit.getStatus())) {
                continue;
            }
            result.add(toView(unit));
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    public Map<String, Object> getUnit(String level, String code, String tenantId) {
        OrganizationUnit unit = organizationStore.get(key(text(tenantId, DEFAULT_TENANT_ID), upper(level), code));
        if (unit == null) {
            throw new IllegalArgumentException("organization unit not found: " + upper(level) + "/" + code);
        }
        Map<String, Object> view = toView(unit);
        view.put("children", childrenOf(unit));
        return view;
    }

    public boolean scopeExists(String tenantId, String level, String code) {
        String resolvedLevel = upper(level);
        String resolvedCode = text(code, null);
        if ("PLATFORM".equals(resolvedLevel)) {
            return "DEFAULT".equals(resolvedCode);
        }
        return organizationStore.containsKey(key(text(tenantId, DEFAULT_TENANT_ID), resolvedLevel, resolvedCode));
    }

    public Map<String, Object> scopeReference(String tenantId, String level, String code) {
        String resolvedTenant = text(tenantId, DEFAULT_TENANT_ID);
        String resolvedLevel = upper(level);
        String resolvedCode = text(code, null);
        Map<String, Object> reference = new LinkedHashMap<String, Object>();
        reference.put("tenant_id", resolvedTenant);
        reference.put("scope_level", resolvedLevel);
        reference.put("scope_code", resolvedCode);
        reference.put("scope_name", levelName(resolvedLevel));
        reference.put("exists", scopeExists(resolvedTenant, resolvedLevel, resolvedCode));
        reference.put("baseline", "PLATFORM".equals(resolvedLevel));
        if (!"PLATFORM".equals(resolvedLevel)) {
            OrganizationUnit unit = organizationStore.get(key(resolvedTenant, resolvedLevel, resolvedCode));
            if (unit != null) {
                reference.put("organization_name", unit.getName());
                reference.put("organization_status", unit.getStatus());
            }
        }
        return reference;
    }

    public Map<String, Object> tree(Map<String, String> filters) {
        String tenantId = text(filterValue(filters, "tenantId"), DEFAULT_TENANT_ID);
        String rootLevel = upper(filterValue(filters, "rootLevel"));
        String rootCode = filterValue(filters, "rootCode");

        List<Map<String, Object>> roots = new ArrayList<Map<String, Object>>();
        if (rootLevel != null || rootCode != null) {
            if (rootLevel == null || rootCode == null) {
                throw new IllegalArgumentException("rootLevel and rootCode must be provided together");
            }
            OrganizationUnit root = organizationStore.get(key(tenantId, rootLevel, rootCode));
            if (root == null) {
                throw new IllegalArgumentException("organization root not found: " + rootLevel + "/" + rootCode);
            }
            roots.add(treeNode(root));
        } else {
            for (OrganizationUnit unit : sortedUnits()) {
                if (!tenantId.equals(unit.getTenantId())) {
                    continue;
                }
                if (unit.getParentCode() == null
                        || organizationStore.get(key(tenantId, unit.getParentLevel(), unit.getParentCode())) == null) {
                    roots.add(treeNode(unit));
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("tenant_id", tenantId);
        result.put("root_count", roots.size());
        result.put("tree", roots);
        return result;
    }

    private List<Map<String, Object>> childrenOf(OrganizationUnit parent) {
        List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
        for (OrganizationUnit unit : sortedUnits()) {
            if (!parent.getTenantId().equals(unit.getTenantId())) {
                continue;
            }
            if (parent.getLevel().equals(unit.getParentLevel()) && parent.getCode().equals(unit.getParentCode())) {
                children.add(toView(unit));
            }
        }
        return children;
    }

    private Map<String, Object> treeNode(OrganizationUnit unit) {
        Map<String, Object> node = toView(unit);
        List<Map<String, Object>> children = new ArrayList<Map<String, Object>>();
        for (OrganizationUnit child : sortedUnits()) {
            if (!unit.getTenantId().equals(child.getTenantId())) {
                continue;
            }
            if (unit.getLevel().equals(child.getParentLevel()) && unit.getCode().equals(child.getParentCode())) {
                children.add(treeNode(child));
            }
        }
        node.put("children", children);
        return node;
    }

    private void collectWarnings(OrganizationUnit unit, List<Map<String, Object>> warnings) {
        if (unit.getParentCode() == null && !"GROUP".equals(unit.getLevel()) && !"HOSPITAL".equals(unit.getLevel())) {
            warnings.add(warning(unit, "parent_code", "缺少上级组织，目录树会把该节点作为根节点展示。"));
            return;
        }
        if (unit.getParentCode() != null) {
            OrganizationUnit parent = organizationStore.get(
                    key(unit.getTenantId(), unit.getParentLevel(), unit.getParentCode()));
            if (parent == null) {
                warnings.add(warning(unit, "parent_code", "上级组织尚未导入，后续导入父节点后目录树会自动归位。"));
            }
        }
    }

    private Map<String, Object> warning(OrganizationUnit unit, String field, String message) {
        Map<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("severity", "WARN");
        warning.put("level", unit.getLevel());
        warning.put("code", unit.getCode());
        warning.put("field", field);
        warning.put("message", message);
        return warning;
    }

    @SuppressWarnings("unchecked")
    private ImportEnvelope normalize(Object request) {
        ImportEnvelope envelope = new ImportEnvelope();
        envelope.tenantId = DEFAULT_TENANT_ID;
        if (request instanceof List) {
            envelope.units.addAll((List<Map<String, Object>>) request);
            return envelope;
        }
        if (request instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) request;
            envelope.tenantId = string(value(body, "tenant_id", "tenantId"), DEFAULT_TENANT_ID);
            envelope.operatorId = string(value(body, "operator_id", "operatorId"), null);
            Object nested = body.get("units");
            if (nested instanceof Collection) {
                for (Object item : (Collection<?>) nested) {
                    if (item instanceof Map) {
                        envelope.units.add((Map<String, Object>) item);
                    }
                }
            } else {
                envelope.units.add(body);
            }
        }
        return envelope;
    }

    private OrganizationUnit toUnit(Map<String, Object> payload, ImportEnvelope envelope, String now) {
        OrganizationUnit unit = new OrganizationUnit();
        unit.setTenantId(string(value(payload, "tenant_id", "tenantId"), envelope.tenantId));
        unit.setLevel(upper(required(payload, "level")));
        if (!REAL_ORG_LEVELS.contains(unit.getLevel())) {
            throw new IllegalArgumentException("unsupported organization level: " + unit.getLevel()
                    + "; PLATFORM is system baseline, not a real organization unit");
        }
        unit.setCode(required(payload, "code"));
        unit.setName(string(value(payload, "name", "orgName"), unit.getCode()));
        unit.setParentLevel(upper(string(value(payload, "parent_level", "parentLevel"), expectedParent(unit.getLevel()))));
        unit.setParentCode(string(value(payload, "parent_code", "parentCode"), null));
        if (unit.getParentCode() == null) {
            unit.setParentLevel(null);
        }
        validateParentLevel(unit);
        unit.setStatus(upper(string(payload.get("status"), "ACTIVE")));
        unit.setDisplayOrder(integer(value(payload, "display_order", "displayOrder"), 0));
        unit.setCreatedBy(string(value(payload, "created_by", "createdBy"), envelope.operatorId));
        unit.setCreatedTime(now);
        return unit;
    }

    private void validateParentLevel(OrganizationUnit unit) {
        if (unit.getParentLevel() == null) {
            return;
        }
        String expected = expectedParent(unit.getLevel());
        if (expected == null) {
            throw new IllegalArgumentException(unit.getLevel() + " must not have parent_level");
        }
        if (!expected.equals(unit.getParentLevel())) {
            throw new IllegalArgumentException(unit.getLevel() + " parent_level must be " + expected);
        }
    }

    private String expectedParent(String level) {
        return EXPECTED_PARENT.get(level);
    }

    private Map<String, Object> toView(OrganizationUnit unit) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("tenant_id", unit.getTenantId());
        view.put("level", unit.getLevel());
        view.put("level_name", levelName(unit.getLevel()));
        view.put("code", unit.getCode());
        view.put("name", unit.getName());
        view.put("parent_level", unit.getParentLevel());
        view.put("parent_code", unit.getParentCode());
        view.put("status", unit.getStatus());
        view.put("display_order", unit.getDisplayOrder());
        view.put("created_by", unit.getCreatedBy());
        view.put("created_time", unit.getCreatedTime());
        view.put("updated_time", unit.getUpdatedTime());
        return view;
    }

    private List<OrganizationUnit> sortedUnits() {
        List<OrganizationUnit> units = new ArrayList<OrganizationUnit>(organizationStore.values());
        Collections.sort(units, new Comparator<OrganizationUnit>() {
            @Override
            public int compare(OrganizationUnit left, OrganizationUnit right) {
                int byTenant = left.getTenantId().compareTo(right.getTenantId());
                if (byTenant != 0) {
                    return byTenant;
                }
                int byLevel = Integer.valueOf(levelOrder(left.getLevel())).compareTo(levelOrder(right.getLevel()));
                if (byLevel != 0) {
                    return byLevel;
                }
                int byDisplay = left.getDisplayOrder().compareTo(right.getDisplayOrder());
                return byDisplay != 0 ? byDisplay : left.getCode().compareTo(right.getCode());
            }
        });
        return units;
    }

    private int levelOrder(String level) {
        int index = REAL_ORG_LEVELS.indexOf(level);
        return index < 0 ? 99 : index;
    }

    private String levelName(String level) {
        if ("GROUP".equals(level)) {
            return "集团";
        }
        if ("HOSPITAL".equals(level)) {
            return "医院";
        }
        if ("CAMPUS".equals(level)) {
            return "院区";
        }
        if ("SITE".equals(level)) {
            return "卫生所/站点";
        }
        if ("DEPARTMENT".equals(level)) {
            return "科室";
        }
        if ("PLATFORM".equals(level)) {
            return "系统内置默认（产品基线配置）";
        }
        return level;
    }

    private static Map<String, String> expectedParentMap() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("GROUP", null);
        map.put("HOSPITAL", "GROUP");
        map.put("CAMPUS", "HOSPITAL");
        map.put("SITE", "CAMPUS");
        map.put("DEPARTMENT", "SITE");
        return map;
    }

    private String key(String tenantId, String level, String code) {
        return text(tenantId, DEFAULT_TENANT_ID) + "::" + upper(level) + "::" + code;
    }

    private String required(Map<String, Object> map, String key) {
        String value = string(map.get(key), null);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
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

    private Integer integer(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text.trim();
    }

    private String text(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }

    private void audit(String actionType, String targetType, String targetCode,
                       String operatorId, Map<String, Object> detail) {
        try {
            persistenceService.saveAuditLog("ORGANIZATION", actionType, targetType, targetCode,
                    null, null, operatorId, detail);
        } catch (RuntimeException ignored) {
            // 组织目录导入在 DB-only 内存态下不能因审计落库失败中断。
        }
    }

    /**
     * 将组织单元写入持久化层。持久化失败不阻断内存操作，仅记录警告。
     */
    private void persistUnit(OrganizationUnit unit) {
        try {
            orgPersistenceService.saveOrganizationUnit(unit);
        } catch (RuntimeException ex) {
            log.warn("persist organization unit failed, in-memory store still valid: {}/{} - {}",
                    unit.getLevel(), unit.getCode(), ex.getMessage());
        }
    }

    private static class ImportEnvelope {
        private String tenantId;
        private String operatorId;
        private final List<Map<String, Object>> units = new ArrayList<Map<String, Object>>();
    }
}
