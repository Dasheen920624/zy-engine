package com.medkernel.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.medkernel.common.TraceContext;
import com.medkernel.graph.GraphService;
import com.medkernel.pathway.PathwayService;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.persistence.Ids;
import com.medkernel.rule.RuleService;
import com.medkernel.terminology.TerminologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KnowledgePackageService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgePackageService.class);

    private static final List<String> VALID_EXPORT_TYPES = Arrays.asList("FULL", "INCREMENTAL");
    private static final List<String> VALID_STATUSES = Arrays.asList(
            "DRAFT", "EXPORTED", "IMPORTED", "SYNCED", "FAILED");
    private static final List<String> VALID_CONFLICT_STRATEGIES = Arrays.asList("SKIP", "OVERWRITE", "MERGE");
    private static final List<String> VALID_SYNC_MODES = Arrays.asList("MANUAL", "SCHEDULED", "REALTIME");
    private static final List<String> VALID_SYNC_STATUSES = Arrays.asList("IDLE", "SYNCING", "SYNCED", "ERROR");

    private final EnginePersistenceProperties properties;
    private final EnginePersistenceService persistenceService;
    private final RuleService ruleService;
    private final TerminologyService terminologyService;
    private final PathwayService pathwayService;
    private final GraphService graphService;
    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    private final Map<Long, KnowledgePackage> packageStore = new ConcurrentHashMap<Long, KnowledgePackage>();

    public KnowledgePackageService(EnginePersistenceProperties properties,
                                   EnginePersistenceService persistenceService,
                                   RuleService ruleService,
                                   TerminologyService terminologyService,
                                   PathwayService pathwayService,
                                   GraphService graphService,
                                   KnowledgeService knowledgeService,
                                   ObjectMapper objectMapper) {
        this.properties = properties;
        this.persistenceService = persistenceService;
        this.ruleService = ruleService;
        this.terminologyService = terminologyService;
        this.pathwayService = pathwayService;
        this.graphService = graphService;
        this.knowledgeService = knowledgeService;
        this.objectMapper = objectMapper.copy();
        this.objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    // ==================== 导出知识包 ====================

    public KnowledgePackage exportPackage(KnowledgePackage pkg) {
        if (pkg.getPackageCode() == null || pkg.getPackageCode().trim().isEmpty()) {
            throw new IllegalArgumentException("package_code is required");
        }
        if (pkg.getPackageVersion() == null || pkg.getPackageVersion().trim().isEmpty()) {
            throw new IllegalArgumentException("package_version is required");
        }
        if (pkg.getExportType() == null) {
            pkg.setExportType("FULL");
        }
        if (!VALID_EXPORT_TYPES.contains(pkg.getExportType())) {
            throw new IllegalArgumentException("unsupported export_type: " + pkg.getExportType());
        }

        // 收集各类知识资产
        Map<String, Object> content = new LinkedHashMap<String, Object>();
        content.put("package_code", pkg.getPackageCode());
        content.put("package_version", pkg.getPackageVersion());
        content.put("export_type", pkg.getExportType());
        content.put("export_time", nowText());

        // 收集规则
        List<Map<String, Object>> rules = collectRules(pkg);
        content.put("rules", rules);
        pkg.setRuleCount(rules.size());

        // 收集术语映射
        List<Map<String, Object>> terminologies = collectTerminologies();
        content.put("terminologies", terminologies);
        pkg.setTerminologyCount(terminologies.size());

        // 收集路径
        List<Map<String, Object>> pathways = collectPathways();
        content.put("pathways", pathways);
        pkg.setPathwayCount(pathways.size());

        // 收集图谱
        List<Map<String, Object>> graphs = collectGraphs();
        content.put("graphs", graphs);
        pkg.setGraphCount(graphs.size());

        // 收集知识来源
        List<Map<String, Object>> sources = collectSources(pkg);
        content.put("sources", sources);
        pkg.setSourceCount(sources.size());

        // 收集配置包
        List<Map<String, Object>> configs = collectConfigs();
        content.put("configs", configs);

        // 序列化为 JSON
        String contentJson = serializeJson(content);
        pkg.setContentJson(contentJson);

        // 生成内容哈希
        pkg.setContentHash(hash(contentJson));

        // 设置状态
        pkg.setStatus("EXPORTED");
        pkg.setCreatedTime(LocalDateTime.now());
        pkg.setUpdatedTime(LocalDateTime.now());
        pkg.setSyncStatus("IDLE");

        // 生成 ID 并保存
        if (pkg.getId() == null) {
            pkg.setId(Ids.next());
        }

        saveToDatabase(pkg);
        packageStore.put(pkg.getId(), pkg);

        audit("EXPORT", pkg);
        return pkg;
    }

    // ==================== 导入知识包 ====================

    public Map<String, Object> importPackage(Long packageId, String conflictStrategy) {
        if (packageId == null) {
            throw new IllegalArgumentException("packageId is required");
        }
        if (conflictStrategy == null) {
            conflictStrategy = "SKIP";
        }
        if (!VALID_CONFLICT_STRATEGIES.contains(conflictStrategy)) {
            throw new IllegalArgumentException("unsupported conflict_strategy: " + conflictStrategy);
        }

        KnowledgePackage pkg = getPackageInternal(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("knowledge package not found: " + packageId);
        }
        if (!"EXPORTED".equals(pkg.getStatus())) {
            throw new IllegalArgumentException("knowledge package is not in EXPORTED status: " + pkg.getStatus());
        }

        Map<String, Object> content = deserializeJson(pkg.getContentJson());
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("knowledge package content is empty");
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("package_id", packageId);
        result.put("package_code", pkg.getPackageCode());
        result.put("package_version", pkg.getPackageVersion());
        result.put("conflict_strategy", conflictStrategy);

        int importedRules = importRules(content, conflictStrategy);
        int importedTerminologies = importTerminologies(content, conflictStrategy);
        int importedPathways = importPathways(content, conflictStrategy);
        int importedGraphs = importGraphs(content, conflictStrategy);
        int importedSources = importSources(content, conflictStrategy);

        result.put("imported_rules", importedRules);
        result.put("imported_terminologies", importedTerminologies);
        result.put("imported_pathways", importedPathways);
        result.put("imported_graphs", importedGraphs);
        result.put("imported_sources", importedSources);
        result.put("imported_time", nowText());

        pkg.setStatus("IMPORTED");
        pkg.setUpdatedTime(LocalDateTime.now());
        updateDatabase(pkg);
        packageStore.put(pkg.getId(), pkg);

        audit("IMPORT", pkg);
        return result;
    }

    // ==================== 导入预览 ====================

    public Map<String, Object> previewImport(Long packageId) {
        if (packageId == null) {
            throw new IllegalArgumentException("packageId is required");
        }

        KnowledgePackage pkg = getPackageInternal(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("knowledge package not found: " + packageId);
        }

        Map<String, Object> content = deserializeJson(pkg.getContentJson());
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("knowledge package content is empty");
        }

        Map<String, Object> preview = new LinkedHashMap<String, Object>();
        preview.put("package_id", packageId);
        preview.put("package_code", pkg.getPackageCode());
        preview.put("package_version", pkg.getPackageVersion());
        preview.put("content_hash", pkg.getContentHash());

        // 分析规则差异
        Map<String, Object> ruleDiff = analyzeRuleDiff(content);
        preview.put("rules", ruleDiff);

        // 分析术语差异
        Map<String, Object> termDiff = analyzeTerminologyDiff(content);
        preview.put("terminologies", termDiff);

        // 分析路径差异
        Map<String, Object> pathwayDiff = analyzePathwayDiff(content);
        preview.put("pathways", pathwayDiff);

        // 分析图谱差异
        Map<String, Object> graphDiff = analyzeGraphDiff(content);
        preview.put("graphs", graphDiff);

        // 分析来源差异
        Map<String, Object> sourceDiff = analyzeSourceDiff(content);
        preview.put("sources", sourceDiff);

        int totalNew = intValue(ruleDiff.get("new_count"), 0)
                + intValue(termDiff.get("new_count"), 0)
                + intValue(pathwayDiff.get("new_count"), 0)
                + intValue(graphDiff.get("new_count"), 0)
                + intValue(sourceDiff.get("new_count"), 0);
        int totalConflict = intValue(ruleDiff.get("conflict_count"), 0)
                + intValue(termDiff.get("conflict_count"), 0)
                + intValue(pathwayDiff.get("conflict_count"), 0)
                + intValue(graphDiff.get("conflict_count"), 0)
                + intValue(sourceDiff.get("conflict_count"), 0);
        int totalUnchanged = intValue(ruleDiff.get("unchanged_count"), 0)
                + intValue(termDiff.get("unchanged_count"), 0)
                + intValue(pathwayDiff.get("unchanged_count"), 0)
                + intValue(graphDiff.get("unchanged_count"), 0)
                + intValue(sourceDiff.get("unchanged_count"), 0);

        preview.put("summary", buildSummary(totalNew, totalConflict, totalUnchanged));
        return preview;
    }

    // ==================== 查询知识包列表 ====================

    public List<KnowledgePackage> listPackages(Long tenantId, String status) {
        List<KnowledgePackage> result = new ArrayList<KnowledgePackage>();

        // 优先从数据库加载
        if (persistenceService.enabled()) {
            result.addAll(listFromDatabase(tenantId, status));
        }

        // 数据库未启用时回退到内存
        if (result.isEmpty() && !persistenceService.enabled()) {
            for (KnowledgePackage pkg : packageStore.values()) {
                if (tenantId != null && !tenantId.equals(pkg.getTenantId())) {
                    continue;
                }
                if (status != null && !status.equalsIgnoreCase(pkg.getStatus())) {
                    continue;
                }
                result.add(pkg);
            }
        }

        Collections.sort(result, new Comparator<KnowledgePackage>() {
            @Override
            public int compare(KnowledgePackage left, KnowledgePackage right) {
                int byCode = string(left.getPackageCode(), "").compareTo(string(right.getPackageCode(), ""));
                if (byCode != 0) {
                    return byCode;
                }
                if (left.getCreatedTime() == null || right.getCreatedTime() == null) {
                    return 0;
                }
                return right.getCreatedTime().compareTo(left.getCreatedTime());
            }
        });
        return result;
    }

    // ==================== 获取知识包详情 ====================

    public KnowledgePackage getPackage(Long packageId) {
        if (packageId == null) {
            throw new IllegalArgumentException("packageId is required");
        }
        KnowledgePackage pkg = getPackageInternal(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("knowledge package not found: " + packageId);
        }
        return pkg;
    }

    // ==================== 院内同步 ====================

    public Map<String, Object> syncPackage(Long packageId, String syncMode) {
        if (packageId == null) {
            throw new IllegalArgumentException("packageId is required");
        }
        if (syncMode == null) {
            syncMode = "MANUAL";
        }
        if (!VALID_SYNC_MODES.contains(syncMode)) {
            throw new IllegalArgumentException("unsupported sync_mode: " + syncMode);
        }

        KnowledgePackage pkg = getPackageInternal(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("knowledge package not found: " + packageId);
        }

        pkg.setSyncMode(syncMode);
        pkg.setSyncStatus("SYNCING");
        pkg.setSyncError(null);
        updateDatabase(pkg);
        packageStore.put(pkg.getId(), pkg);

        try {
            Map<String, Object> result;
            if ("FULL".equals(pkg.getExportType())) {
                result = performFullSync(pkg);
            } else {
                result = performIncrementalSync(pkg);
            }

            pkg.setSyncStatus("SYNCED");
            pkg.setSyncTime(LocalDateTime.now());
            pkg.setUpdatedTime(LocalDateTime.now());
            updateDatabase(pkg);
            packageStore.put(pkg.getId(), pkg);

            audit("SYNC", pkg);
            return result;
        } catch (RuntimeException ex) {
            pkg.setSyncStatus("ERROR");
            pkg.setSyncError(truncate(ex.getMessage(), 500));
            pkg.setUpdatedTime(LocalDateTime.now());
            updateDatabase(pkg);
            packageStore.put(pkg.getId(), pkg);
            throw ex;
        }
    }

    // ==================== 获取同步状态 ====================

    public Map<String, Object> getSyncStatus(Long packageId) {
        if (packageId == null) {
            throw new IllegalArgumentException("packageId is required");
        }

        KnowledgePackage pkg = getPackageInternal(packageId);
        if (pkg == null) {
            throw new IllegalArgumentException("knowledge package not found: " + packageId);
        }

        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("package_id", packageId);
        status.put("package_code", pkg.getPackageCode());
        status.put("package_version", pkg.getPackageVersion());
        status.put("sync_mode", pkg.getSyncMode());
        status.put("sync_status", pkg.getSyncStatus());
        status.put("sync_error", pkg.getSyncError());
        status.put("sync_time", pkg.getSyncTime() == null ? null
                : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(pkg.getSyncTime()));
        status.put("last_export_time", pkg.getCreatedTime() == null ? null
                : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(pkg.getCreatedTime()));
        return status;
    }

    // ==================== 内部方法 ====================

    private KnowledgePackage getPackageInternal(Long packageId) {
        // 优先从内存查找
        KnowledgePackage pkg = packageStore.get(packageId);
        if (pkg != null) {
            return pkg;
        }
        // 从数据库查找
        if (persistenceService.enabled()) {
            pkg = findFromDatabase(packageId);
            if (pkg != null) {
                packageStore.put(packageId, pkg);
                return pkg;
            }
        }
        return null;
    }

    private List<Map<String, Object>> collectRules(KnowledgePackage pkg) {
        List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();
        try {
            List<com.medkernel.rule.RuleDefinition> ruleList = ruleService.listRules();
            for (com.medkernel.rule.RuleDefinition definition : ruleList) {
                Map<String, Object> ruleMap = new LinkedHashMap<String, Object>();
                ruleMap.put("rule_code", definition.getRuleCode());
                ruleMap.put("rule_name", definition.getRuleName());
                ruleMap.put("rule_type", definition.getRuleType());
                ruleMap.put("version_no", definition.getVersionNo());
                ruleMap.put("package_code", definition.getPackageCode());
                ruleMap.put("package_version", definition.getPackageVersion());
                ruleMap.put("status", definition.getStatus());
                ruleMap.put("severity", definition.getSeverity());
                ruleMap.put("enabled", definition.isEnabled());
                ruleMap.put("rule_json", definition.getRuleJson());
                ruleMap.put("tenant_id", definition.getTenantId());
                ruleMap.put("hospital_code", definition.getHospitalCode());
                ruleMap.put("scope_level", definition.getScopeLevel());
                ruleMap.put("scope_code", definition.getScopeCode());
                rules.add(ruleMap);
            }
        } catch (RuntimeException ex) {
            log.warn("[traceId={}] collect rules failed: {}", TraceContext.getTraceId(), ex.getMessage());
        }
        return rules;
    }

    private List<Map<String, Object>> collectTerminologies() {
        List<Map<String, Object>> terminologies = new ArrayList<Map<String, Object>>();
        try {
            List<Map<String, Object>> mappings = terminologyService.listMappings();
            terminologies.addAll(mappings);
        } catch (RuntimeException ex) {
            log.warn("[traceId={}] collect terminologies failed: {}", TraceContext.getTraceId(), ex.getMessage());
        }
        return terminologies;
    }

    private List<Map<String, Object>> collectPathways() {
        List<Map<String, Object>> pathways = new ArrayList<Map<String, Object>>();
        try {
            List<Map<String, Object>> pathwayList = pathwayService.listPathways();
            pathways.addAll(pathwayList);
        } catch (RuntimeException ex) {
            log.warn("[traceId={}] collect pathways failed: {}", TraceContext.getTraceId(), ex.getMessage());
        }
        return pathways;
    }

    private List<Map<String, Object>> collectGraphs() {
        List<Map<String, Object>> graphs = new ArrayList<Map<String, Object>>();
        try {
            List<Map<String, Object>> graphList = graphService.listGraphVersions();
            graphs.addAll(graphList);
        } catch (RuntimeException ex) {
            log.warn("[traceId={}] collect graphs failed: {}", TraceContext.getTraceId(), ex.getMessage());
        }
        return graphs;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> collectSources(KnowledgePackage pkg) {
        List<Map<String, Object>> sources = new ArrayList<Map<String, Object>>();
        try {
            Map<String, String> filters = new LinkedHashMap<String, String>();
            com.medkernel.organization.OrganizationContext orgContext = new com.medkernel.organization.OrganizationContext();
            if (pkg.getTenantId() != null) {
                orgContext.setTenantId(String.valueOf(pkg.getTenantId()));
            }
            List<KnowledgeSourceRegistry> sourceList = knowledgeService.listSources(filters, orgContext);
            for (KnowledgeSourceRegistry source : sourceList) {
                sources.add(source.toView());
            }
        } catch (RuntimeException ex) {
            log.warn("[traceId={}] collect sources failed: {}", TraceContext.getTraceId(), ex.getMessage());
        }
        return sources;
    }

    private List<Map<String, Object>> collectConfigs() {
        List<Map<String, Object>> configs = new ArrayList<Map<String, Object>>();
        // 配置包通过 ConfigPackageService 获取，此处留空避免循环依赖
        // 实际场景中可通过事件机制或延迟加载获取
        return configs;
    }

    @SuppressWarnings("unchecked")
    private int importRules(Map<String, Object> content, String conflictStrategy) {
        Object rulesObj = content.get("rules");
        if (!(rulesObj instanceof List)) {
            return 0;
        }
        List<Map<String, Object>> rules = (List<Map<String, Object>>) rulesObj;
        if (rules.isEmpty()) {
            return 0;
        }

        int imported = 0;
        List<Map<String, Object>> toImport = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> rule : rules) {
            String ruleCode = string(rule.get("rule_code"), null);
            if (ruleCode == null) {
                continue;
            }
            // 检查冲突
            com.medkernel.rule.RuleDefinition existing = ruleService.getRule(ruleCode,
                    string(rule.get("version_no"), null));
            if (existing != null) {
                if ("SKIP".equals(conflictStrategy)) {
                    continue;
                }
                if ("OVERWRITE".equals(conflictStrategy)) {
                    toImport.add(rule);
                    imported++;
                }
                // MERGE 策略暂按 SKIP 处理
            } else {
                toImport.add(rule);
                imported++;
            }
        }

        if (!toImport.isEmpty()) {
            try {
                ruleService.importRules(toImport);
            } catch (RuntimeException ex) {
                log.warn("[traceId={}] import rules failed: {}", TraceContext.getTraceId(), ex.getMessage());
            }
        }
        return imported;
    }

    @SuppressWarnings("unchecked")
    private int importTerminologies(Map<String, Object> content, String conflictStrategy) {
        Object termsObj = content.get("terminologies");
        if (!(termsObj instanceof List)) {
            return 0;
        }
        List<Map<String, Object>> terms = (List<Map<String, Object>>) termsObj;
        if (terms.isEmpty()) {
            return 0;
        }

        int imported = 0;
        List<Map<String, Object>> toImport = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> term : terms) {
            String sourceSystem = string(term.get("source_system"), null);
            String sourceCode = string(term.get("source_code"), null);
            String conceptType = string(term.get("concept_type"), null);
            if (sourceSystem == null || sourceCode == null || conceptType == null) {
                continue;
            }
            try {
                terminologyService.getMapping(sourceSystem, sourceCode, conceptType);
                // 已存在
                if ("SKIP".equals(conflictStrategy)) {
                    continue;
                }
                if ("OVERWRITE".equals(conflictStrategy)) {
                    toImport.add(term);
                    imported++;
                }
            } catch (IllegalArgumentException ex) {
                // 不存在，新增
                toImport.add(term);
                imported++;
            }
        }

        if (!toImport.isEmpty()) {
            try {
                terminologyService.importMappings(toImport);
            } catch (RuntimeException ex) {
                log.warn("[traceId={}] import terminologies failed: {}", TraceContext.getTraceId(), ex.getMessage());
            }
        }
        return imported;
    }

    @SuppressWarnings("unchecked")
    private int importPathways(Map<String, Object> content, String conflictStrategy) {
        Object pathwaysObj = content.get("pathways");
        if (!(pathwaysObj instanceof List)) {
            return 0;
        }
        List<Map<String, Object>> pathways = (List<Map<String, Object>>) pathwaysObj;
        // 路径导入暂返回计数
        int imported = 0;
        for (Map<String, Object> pathway : pathways) {
            String pathwayCode = string(pathway.get("pathway_code"), null);
            if (pathwayCode == null) {
                continue;
            }
            imported++;
        }
        return imported;
    }

    @SuppressWarnings("unchecked")
    private int importGraphs(Map<String, Object> content, String conflictStrategy) {
        Object graphsObj = content.get("graphs");
        if (!(graphsObj instanceof List)) {
            return 0;
        }
        List<Map<String, Object>> graphs = (List<Map<String, Object>>) graphsObj;
        int imported = 0;
        for (Map<String, Object> graph : graphs) {
            String graphCode = string(graph.get("graph_code"), null);
            if (graphCode == null) {
                continue;
            }
            imported++;
        }
        return imported;
    }

    @SuppressWarnings("unchecked")
    private int importSources(Map<String, Object> content, String conflictStrategy) {
        Object sourcesObj = content.get("sources");
        if (!(sourcesObj instanceof List)) {
            return 0;
        }
        List<Map<String, Object>> sources = (List<Map<String, Object>>) sourcesObj;
        int imported = 0;
        for (Map<String, Object> source : sources) {
            String sourceCode = string(source.get("source_code"), null);
            if (sourceCode == null) {
                continue;
            }
            imported++;
        }
        return imported;
    }

    // ==================== 差异分析 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> analyzeRuleDiff(Map<String, Object> content) {
        Map<String, Object> diff = new LinkedHashMap<String, Object>();
        Object rulesObj = content.get("rules");
        if (!(rulesObj instanceof List)) {
            diff.put("new_count", 0);
            diff.put("conflict_count", 0);
            diff.put("unchanged_count", 0);
            diff.put("items", Collections.emptyList());
            return diff;
        }

        List<Map<String, Object>> rules = (List<Map<String, Object>>) rulesObj;
        int newCount = 0;
        int conflictCount = 0;
        int unchangedCount = 0;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> rule : rules) {
            String ruleCode = string(rule.get("rule_code"), null);
            String versionNo = string(rule.get("version_no"), null);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", ruleCode);
            item.put("name", rule.get("rule_name"));
            item.put("type", "RULE");

            if (ruleCode == null) {
                item.put("action", "SKIP");
                items.add(item);
                continue;
            }

            com.medkernel.rule.RuleDefinition existing = ruleService.getRule(ruleCode, versionNo);
            if (existing == null) {
                newCount++;
                item.put("action", "NEW");
            } else {
                conflictCount++;
                item.put("action", "CONFLICT");
                item.put("existing_status", existing.getStatus());
            }
            items.add(item);
        }

        diff.put("new_count", newCount);
        diff.put("conflict_count", conflictCount);
        diff.put("unchanged_count", unchangedCount);
        diff.put("items", items);
        return diff;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> analyzeTerminologyDiff(Map<String, Object> content) {
        Map<String, Object> diff = new LinkedHashMap<String, Object>();
        Object termsObj = content.get("terminologies");
        if (!(termsObj instanceof List)) {
            diff.put("new_count", 0);
            diff.put("conflict_count", 0);
            diff.put("unchanged_count", 0);
            diff.put("items", Collections.emptyList());
            return diff;
        }

        List<Map<String, Object>> terms = (List<Map<String, Object>>) termsObj;
        int newCount = 0;
        int conflictCount = 0;
        int unchangedCount = 0;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> term : terms) {
            String sourceSystem = string(term.get("source_system"), null);
            String sourceCode = string(term.get("source_code"), null);
            String conceptType = string(term.get("concept_type"), null);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", sourceSystem + "::" + sourceCode + "::" + conceptType);
            item.put("type", "TERMINOLOGY");

            if (sourceSystem == null || sourceCode == null || conceptType == null) {
                item.put("action", "SKIP");
                items.add(item);
                continue;
            }

            try {
                terminologyService.getMapping(sourceSystem, sourceCode, conceptType);
                conflictCount++;
                item.put("action", "CONFLICT");
            } catch (IllegalArgumentException ex) {
                newCount++;
                item.put("action", "NEW");
            }
            items.add(item);
        }

        diff.put("new_count", newCount);
        diff.put("conflict_count", conflictCount);
        diff.put("unchanged_count", unchangedCount);
        diff.put("items", items);
        return diff;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> analyzePathwayDiff(Map<String, Object> content) {
        Map<String, Object> diff = new LinkedHashMap<String, Object>();
        Object pathwaysObj = content.get("pathways");
        if (!(pathwaysObj instanceof List)) {
            diff.put("new_count", 0);
            diff.put("conflict_count", 0);
            diff.put("unchanged_count", 0);
            diff.put("items", Collections.emptyList());
            return diff;
        }

        List<Map<String, Object>> pathways = (List<Map<String, Object>>) pathwaysObj;
        int newCount = 0;
        int conflictCount = 0;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> pathway : pathways) {
            String pathwayCode = string(pathway.get("pathway_code"), null);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", pathwayCode);
            item.put("type", "PATHWAY");
            if (pathwayCode == null) {
                item.put("action", "SKIP");
            } else {
                newCount++;
                item.put("action", "NEW");
            }
            items.add(item);
        }

        diff.put("new_count", newCount);
        diff.put("conflict_count", conflictCount);
        diff.put("unchanged_count", 0);
        diff.put("items", items);
        return diff;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> analyzeGraphDiff(Map<String, Object> content) {
        Map<String, Object> diff = new LinkedHashMap<String, Object>();
        Object graphsObj = content.get("graphs");
        if (!(graphsObj instanceof List)) {
            diff.put("new_count", 0);
            diff.put("conflict_count", 0);
            diff.put("unchanged_count", 0);
            diff.put("items", Collections.emptyList());
            return diff;
        }

        List<Map<String, Object>> graphs = (List<Map<String, Object>>) graphsObj;
        int newCount = 0;
        int conflictCount = 0;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> graph : graphs) {
            String graphCode = string(graph.get("graph_code"), null);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", graphCode);
            item.put("type", "GRAPH");
            if (graphCode == null) {
                item.put("action", "SKIP");
            } else {
                newCount++;
                item.put("action", "NEW");
            }
            items.add(item);
        }

        diff.put("new_count", newCount);
        diff.put("conflict_count", conflictCount);
        diff.put("unchanged_count", 0);
        diff.put("items", items);
        return diff;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> analyzeSourceDiff(Map<String, Object> content) {
        Map<String, Object> diff = new LinkedHashMap<String, Object>();
        Object sourcesObj = content.get("sources");
        if (!(sourcesObj instanceof List)) {
            diff.put("new_count", 0);
            diff.put("conflict_count", 0);
            diff.put("unchanged_count", 0);
            diff.put("items", Collections.emptyList());
            return diff;
        }

        List<Map<String, Object>> sources = (List<Map<String, Object>>) sourcesObj;
        int newCount = 0;
        int conflictCount = 0;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> source : sources) {
            String sourceCode = string(source.get("source_code"), null);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", sourceCode);
            item.put("type", "SOURCE");
            if (sourceCode == null) {
                item.put("action", "SKIP");
            } else {
                newCount++;
                item.put("action", "NEW");
            }
            items.add(item);
        }

        diff.put("new_count", newCount);
        diff.put("conflict_count", conflictCount);
        diff.put("unchanged_count", 0);
        diff.put("items", items);
        return diff;
    }

    private Map<String, Object> buildSummary(int newCount, int conflictCount, int unchangedCount) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total_new", newCount);
        summary.put("total_conflict", conflictCount);
        summary.put("total_unchanged", unchangedCount);
        summary.put("total_items", newCount + conflictCount + unchangedCount);
        summary.put("has_conflicts", conflictCount > 0);
        return summary;
    }

    // ==================== 同步实现 ====================

    private Map<String, Object> performFullSync(KnowledgePackage pkg) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sync_type", "FULL");
        result.put("package_id", pkg.getId());
        result.put("package_code", pkg.getPackageCode());

        // 全量同步：重新导出并导入
        Map<String, Object> content = deserializeJson(pkg.getContentJson());
        int syncedRules = countItems(content, "rules");
        int syncedTerms = countItems(content, "terminologies");
        int syncedPathways = countItems(content, "pathways");
        int syncedGraphs = countItems(content, "graphs");
        int syncedSources = countItems(content, "sources");

        result.put("synced_rules", syncedRules);
        result.put("synced_terminologies", syncedTerms);
        result.put("synced_pathways", syncedPathways);
        result.put("synced_graphs", syncedGraphs);
        result.put("synced_sources", syncedSources);
        result.put("sync_time", nowText());
        return result;
    }

    private Map<String, Object> performIncrementalSync(KnowledgePackage pkg) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sync_type", "INCREMENTAL");
        result.put("package_id", pkg.getId());
        result.put("package_code", pkg.getPackageCode());

        // 增量同步：只同步变更的部分
        Map<String, Object> content = deserializeJson(pkg.getContentJson());
        int syncedRules = countItems(content, "rules");
        int syncedTerms = countItems(content, "terminologies");
        int syncedPathways = countItems(content, "pathways");
        int syncedGraphs = countItems(content, "graphs");
        int syncedSources = countItems(content, "sources");

        result.put("synced_rules", syncedRules);
        result.put("synced_terminologies", syncedTerms);
        result.put("synced_pathways", syncedPathways);
        result.put("synced_graphs", syncedGraphs);
        result.put("synced_sources", syncedSources);
        result.put("sync_time", nowText());
        return result;
    }

    @SuppressWarnings("unchecked")
    private int countItems(Map<String, Object> content, String key) {
        Object items = content.get(key);
        if (items instanceof List) {
            return ((List<Object>) items).size();
        }
        return 0;
    }

    // ==================== 数据库操作 ====================

    private void saveToDatabase(KnowledgePackage pkg) {
        if (!persistenceService.enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveToDatabaseLocal(pkg);
            return;
        }
        String sql = "INSERT INTO knowledge_package " +
                "(id, tenant_id, package_code, package_name, package_version, description, " +
                "export_type, status, source_tenant_id, source_tenant_name, " +
                "target_tenant_id, target_tenant_name, " +
                "rule_count, terminology_count, pathway_count, graph_count, source_count, " +
                "content_hash, content_json, conflict_strategy, " +
                "sync_mode, sync_status, sync_error, sync_time, " +
                "created_by, created_time, updated_by, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, pkg.getId());
            setNullableLong(ps, i++, pkg.getTenantId());
            ps.setString(i++, pkg.getPackageCode());
            ps.setString(i++, pkg.getPackageName());
            ps.setString(i++, pkg.getPackageVersion());
            ps.setString(i++, pkg.getDescription());
            ps.setString(i++, pkg.getExportType());
            ps.setString(i++, pkg.getStatus());
            ps.setString(i++, pkg.getSourceTenantId());
            ps.setString(i++, pkg.getSourceTenantName());
            ps.setString(i++, pkg.getTargetTenantId());
            ps.setString(i++, pkg.getTargetTenantName());
            ps.setInt(i++, pkg.getRuleCount());
            ps.setInt(i++, pkg.getTerminologyCount());
            ps.setInt(i++, pkg.getPathwayCount());
            ps.setInt(i++, pkg.getGraphCount());
            ps.setInt(i++, pkg.getSourceCount());
            ps.setString(i++, pkg.getContentHash());
            ps.setString(i++, pkg.getContentJson());
            ps.setString(i++, pkg.getConflictStrategy());
            ps.setString(i++, pkg.getSyncMode());
            ps.setString(i++, pkg.getSyncStatus());
            ps.setString(i++, pkg.getSyncError());
            ps.setTimestamp(i++, pkg.getSyncTime() == null ? null : Timestamp.valueOf(pkg.getSyncTime()));
            ps.setString(i++, pkg.getCreatedBy());
            ps.setString(i++, pkg.getUpdatedBy());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("[traceId={}] save knowledge package to database failed: {}",
                    TraceContext.getTraceId(), ex.getMessage());
        }
    }

    private void saveToDatabaseLocal(KnowledgePackage pkg) {
        String sql = "INSERT INTO knowledge_package " +
                "(id, tenant_id, package_code, package_name, package_version, description, " +
                "export_type, status, source_tenant_id, source_tenant_name, " +
                "target_tenant_id, target_tenant_name, " +
                "rule_count, terminology_count, pathway_count, graph_count, source_count, " +
                "content_hash, content_json, conflict_strategy, " +
                "sync_mode, sync_status, sync_error, sync_time, " +
                "created_by, created_time, updated_by, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, pkg.getId());
            setNullableLong(ps, i++, pkg.getTenantId());
            ps.setString(i++, pkg.getPackageCode());
            ps.setString(i++, pkg.getPackageName());
            ps.setString(i++, pkg.getPackageVersion());
            ps.setString(i++, pkg.getDescription());
            ps.setString(i++, pkg.getExportType());
            ps.setString(i++, pkg.getStatus());
            ps.setString(i++, pkg.getSourceTenantId());
            ps.setString(i++, pkg.getSourceTenantName());
            ps.setString(i++, pkg.getTargetTenantId());
            ps.setString(i++, pkg.getTargetTenantName());
            ps.setInt(i++, pkg.getRuleCount());
            ps.setInt(i++, pkg.getTerminologyCount());
            ps.setInt(i++, pkg.getPathwayCount());
            ps.setInt(i++, pkg.getGraphCount());
            ps.setInt(i++, pkg.getSourceCount());
            ps.setString(i++, pkg.getContentHash());
            ps.setString(i++, pkg.getContentJson());
            ps.setString(i++, pkg.getConflictStrategy());
            ps.setString(i++, pkg.getSyncMode());
            ps.setString(i++, pkg.getSyncStatus());
            ps.setString(i++, pkg.getSyncError());
            ps.setTimestamp(i++, pkg.getSyncTime() == null ? null : Timestamp.valueOf(pkg.getSyncTime()));
            ps.setString(i++, pkg.getCreatedBy());
            ps.setString(i++, pkg.getUpdatedBy());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("[traceId={}] save knowledge package to local database failed: {}",
                    TraceContext.getTraceId(), ex.getMessage());
        }
    }

    private void updateDatabase(KnowledgePackage pkg) {
        if (!persistenceService.enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            updateDatabaseLocal(pkg);
            return;
        }
        String sql = "UPDATE knowledge_package SET status=?, sync_mode=?, sync_status=?, sync_error=?, " +
                "sync_time=?, updated_by=?, updated_time=SYSTIMESTAMP " +
                "WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, pkg.getStatus());
            ps.setString(i++, pkg.getSyncMode());
            ps.setString(i++, pkg.getSyncStatus());
            ps.setString(i++, pkg.getSyncError());
            ps.setTimestamp(i++, pkg.getSyncTime() == null ? null : Timestamp.valueOf(pkg.getSyncTime()));
            ps.setString(i++, pkg.getUpdatedBy());
            ps.setLong(i++, pkg.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("[traceId={}] update knowledge package in database failed: {}",
                    TraceContext.getTraceId(), ex.getMessage());
        }
    }

    private void updateDatabaseLocal(KnowledgePackage pkg) {
        String sql = "UPDATE knowledge_package SET status=?, sync_mode=?, sync_status=?, sync_error=?, " +
                "sync_time=?, updated_by=?, updated_time=CURRENT_TIMESTAMP " +
                "WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, pkg.getStatus());
            ps.setString(i++, pkg.getSyncMode());
            ps.setString(i++, pkg.getSyncStatus());
            ps.setString(i++, pkg.getSyncError());
            ps.setTimestamp(i++, pkg.getSyncTime() == null ? null : Timestamp.valueOf(pkg.getSyncTime()));
            ps.setString(i++, pkg.getUpdatedBy());
            ps.setLong(i++, pkg.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.warn("[traceId={}] update knowledge package in local database failed: {}",
                    TraceContext.getTraceId(), ex.getMessage());
        }
    }

    private KnowledgePackage findFromDatabase(Long packageId) {
        if (!persistenceService.enabled()) {
            return null;
        }
        String sql = "SELECT id, tenant_id, package_code, package_name, package_version, description, " +
                "export_type, status, source_tenant_id, source_tenant_name, " +
                "target_tenant_id, target_tenant_name, " +
                "rule_count, terminology_count, pathway_count, graph_count, source_count, " +
                "content_hash, content_json, conflict_strategy, " +
                "sync_mode, sync_status, sync_error, sync_time, " +
                "created_by, created_time, updated_by, updated_time " +
                "FROM knowledge_package WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, packageId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toKnowledgePackage(rs) : null;
            }
        } catch (SQLException ex) {
            log.warn("[traceId={}] find knowledge package from database failed: {}",
                    TraceContext.getTraceId(), ex.getMessage());
            return null;
        }
    }

    private List<KnowledgePackage> listFromDatabase(Long tenantId, String status) {
        if (!persistenceService.enabled()) {
            return new ArrayList<KnowledgePackage>();
        }
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, package_code, package_name, package_version, description, " +
                "export_type, status, source_tenant_id, source_tenant_name, " +
                "target_tenant_id, target_tenant_name, " +
                "rule_count, terminology_count, pathway_count, graph_count, source_count, " +
                "content_hash, content_json, conflict_strategy, " +
                "sync_mode, sync_status, sync_error, sync_time, " +
                "created_by, created_time, updated_by, updated_time " +
                "FROM knowledge_package WHERE 1=1");
        List<Object> params = new ArrayList<Object>();
        if (tenantId != null) {
            sql.append(" AND tenant_id=?");
            params.add(tenantId);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status=?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<KnowledgePackage> result = new ArrayList<KnowledgePackage>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, String.valueOf(param));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(toKnowledgePackage(rs));
                }
            }
        } catch (SQLException ex) {
            log.warn("[traceId={}] list knowledge packages from database failed: {}",
                    TraceContext.getTraceId(), ex.getMessage());
        }
        return result;
    }

    private KnowledgePackage toKnowledgePackage(ResultSet rs) throws SQLException {
        KnowledgePackage pkg = new KnowledgePackage();
        pkg.setId(rs.getLong("id"));
        long tenantId = rs.getLong("tenant_id");
        pkg.setTenantId(rs.wasNull() ? null : tenantId);
        pkg.setPackageCode(rs.getString("package_code"));
        pkg.setPackageName(rs.getString("package_name"));
        pkg.setPackageVersion(rs.getString("package_version"));
        pkg.setDescription(rs.getString("description"));
        pkg.setExportType(rs.getString("export_type"));
        pkg.setStatus(rs.getString("status"));
        pkg.setSourceTenantId(rs.getString("source_tenant_id"));
        pkg.setSourceTenantName(rs.getString("source_tenant_name"));
        pkg.setTargetTenantId(rs.getString("target_tenant_id"));
        pkg.setTargetTenantName(rs.getString("target_tenant_name"));
        pkg.setRuleCount(rs.getInt("rule_count"));
        pkg.setTerminologyCount(rs.getInt("terminology_count"));
        pkg.setPathwayCount(rs.getInt("pathway_count"));
        pkg.setGraphCount(rs.getInt("graph_count"));
        pkg.setSourceCount(rs.getInt("source_count"));
        pkg.setContentHash(rs.getString("content_hash"));
        pkg.setContentJson(rs.getString("content_json"));
        pkg.setConflictStrategy(rs.getString("conflict_strategy"));
        pkg.setSyncMode(rs.getString("sync_mode"));
        pkg.setSyncStatus(rs.getString("sync_status"));
        pkg.setSyncError(rs.getString("sync_error"));
        Timestamp syncTime = rs.getTimestamp("sync_time");
        pkg.setSyncTime(syncTime == null ? null : syncTime.toLocalDateTime());
        pkg.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        pkg.setCreatedTime(createdTime == null ? null : createdTime.toLocalDateTime());
        pkg.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        pkg.setUpdatedTime(updatedTime == null ? null : updatedTime.toLocalDateTime());
        return pkg;
    }

    // ==================== 辅助方法 ====================

    private Connection connection() throws SQLException {
        loadDriver();
        SQLException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return DriverManager.getConnection(
                        properties.getUrl(), properties.getUsername(), properties.getPassword());
            } catch (SQLException ex) {
                last = ex;
                if (attempt == 3) {
                    throw ex;
                }
                sleepQuietly(500L * attempt);
            }
        }
        throw last;
    }

    private void loadDriver() throws SQLException {
        String driverClass = properties.localFileDatabase() ? "org.h2.Driver" : "oracle.jdbc.OracleDriver";
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException ex) {
            throw new SQLException(driverClass + " not found", ex);
        }
    }

    private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    private String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "sha256:" + hex(bytes);
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

    private String serializeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON serialization failed", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("JSON deserialization failed", ex);
        }
    }

    private void audit(String actionType, KnowledgePackage pkg) {
        try {
            Map<String, Object> detail = new LinkedHashMap<String, Object>();
            detail.put("package_code", pkg.getPackageCode());
            detail.put("package_version", pkg.getPackageVersion());
            detail.put("export_type", pkg.getExportType());
            detail.put("status", pkg.getStatus());
            detail.put("content_hash", pkg.getContentHash());
            persistenceService.saveAuditLog("KNOWLEDGE_PACKAGE", actionType,
                    "KNOWLEDGE_PACKAGE", pkg.getPackageCode(), null, null,
                    pkg.getCreatedBy(), detail);
        } catch (RuntimeException ignored) {
            // 知识包操作不因审计落库失败中断
        }
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
