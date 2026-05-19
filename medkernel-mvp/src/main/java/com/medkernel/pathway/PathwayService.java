package com.medkernel.pathway;

import com.medkernel.adapter.AdapterHubService;
import com.medkernel.dto.PatientPathwayInstance;
import com.medkernel.dto.PatientNodeState;
import com.medkernel.dto.PatientTaskState;
import com.medkernel.dto.PathwayVariationRecord;
import com.medkernel.dto.RecommendationCard;
import com.medkernel.dto.RuleResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.rule.RuleService;
import com.medkernel.util.ClinicalFactUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PathwayService {
    private static final Logger log = LoggerFactory.getLogger(PathwayService.class);
    private final RuleService ruleService;
    private final AdapterHubService adapterHubService;
    private final EnginePersistenceService persistenceService;
    private final PathwayConfigSupport configSupport = new PathwayConfigSupport();
    private final Map<String, PatientPathwayInstance> activeInstances = new ConcurrentHashMap<String, PatientPathwayInstance>();
    private final Map<String, Map<String, Object>> pathwayDrafts = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> publishedPathways = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, String> activePublishedVersions = new ConcurrentHashMap<String, String>();
    private final Map<String, Map<String, PatientNodeState>> nodeStates = new ConcurrentHashMap<String, Map<String, PatientNodeState>>();
    private final Map<String, List<PathwayVariationRecord>> variationRecords = new ConcurrentHashMap<String, List<PathwayVariationRecord>>();

    public PathwayService(RuleService ruleService, AdapterHubService adapterHubService,
                          EnginePersistenceService persistenceService) {
        this.ruleService = ruleService;
        this.adapterHubService = adapterHubService;
        this.persistenceService = persistenceService;
    }

    /**
     * 启动时从 DB 重建路径草稿、已发布版本和活动版本索引，避免进程重启后内存清空导致
     * 已发布路径在 list/get 接口上"消失"，与产品原则"Oracle 是配置/版本的主数据源"对齐。
     *
     * 仅在 EnginePersistenceService.enabled() 时执行；DB-only 不可用时回到纯内存模式，
     * 保持 LOCAL_H2 与 Oracle 双模式行为一致。
     */
    @PostConstruct
    public void rebuildFromPersistence() {
        if (!persistenceService.enabled()) {
            return;
        }
        try {
            Map<String, Map<String, Object>> drafts = persistenceService.loadAllPathwayDrafts();
            for (Map.Entry<String, Map<String, Object>> entry : drafts.entrySet()) {
                pathwayDrafts.putIfAbsent(entry.getKey(), entry.getValue());
            }
            List<Map<String, Object>> versions = persistenceService.loadAllPathwayPublishedVersions();
            for (Map<String, Object> row : versions) {
                String code = string(row.get("pathway_code"), null);
                String versionNo = string(row.get("version_no"), null);
                String status = string(row.get("status"), null);
                @SuppressWarnings("unchecked")
                Map<String, Object> config = (Map<String, Object>) row.get("config");
                if (code == null || versionNo == null) {
                    continue;
                }
                publishedPathways.putIfAbsent(pathwayKey(code, versionNo),
                        config == null ? new LinkedHashMap<String, Object>() : config);
                // 沿用"最后一次写入的版本作为 active"的语义；ORDER BY created_time 保证最新版本在后。
                if ("PUBLISHED".equals(status)) {
                    activePublishedVersions.put(code, versionNo);
                }
            }
            log.info("PathwayService rebuilt from persistence: drafts={}, versions={}, active={}",
                    drafts.size(), versions.size(), activePublishedVersions.size());
        } catch (RuntimeException ex) {
            // 启动期 DB 异常不阻止应用启动，仅记录日志；内存仍可工作，后续可以通过手动重启或运维介入处理。
            log.warn("PathwayService rebuild from persistence failed: {}", ex.getMessage(), ex);
        }
    }

    public Map<String, Object> createPathway(Map<String, Object> config) {
        String pathwayCode = required(config, "pathway_code");
        List<String> validationErrors = configSupport.validate(config);
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("pathway config invalid: " + validationErrors);
        }
        pathwayDrafts.put(pathwayCode, config);
        persistenceService.savePathwayDraft(pathwayCode, config);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("pathway_code", pathwayCode);
        result.put("status", "DRAFT");
        result.put("validation", "PASSED");
        result.put("persistence", persistenceService.providerName());
        audit("CREATE_DRAFT", "PATHWAY", pathwayCode, null, result, null);
        return result;
    }

    public Map<String, Object> publish(String pathwayCode, Map<String, Object> request) {
        Map<String, Object> config = pathwayDrafts.get(pathwayCode);
        String versionNo = string(request.get("version_no"), configSupport.versionNo(config, "1.0.0"));
        if (config == null) {
            config = new HashMap<String, Object>();
            config.put("pathway_code", pathwayCode);
            config.put("version", versionNo);
        }
        publishedPathways.put(pathwayKey(pathwayCode, versionNo), config);
        activePublishedVersions.put(pathwayCode, versionNo);
        persistenceService.savePathwayVersion(pathwayCode, versionNo, "PUBLISHED", config);
        persistenceService.updatePathwayStatus(pathwayCode, "PUBLISHED",
                string(config.get("tenant_id"), null), string(config.get("org_code"), null));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("pathway_code", pathwayCode);
        result.put("version_no", versionNo);
        result.put("status", "PUBLISHED");
        result.put("persistence", persistenceService.providerName());
        result.put("reference_warnings", configSupport.collectMissingReferences(config));
        audit("PUBLISH", "PATHWAY", pathwayCode, null, result,
                string(request == null ? null : request.get("approved_by"), null));
        return result;
    }

    public Map<String, Object> rollback(String pathwayCode, Map<String, Object> request) {
        String targetVersion = string(request == null ? null : request.get("target_version"), null);
        if (targetVersion == null) {
            targetVersion = string(request == null ? null : request.get("rollback_to_version"), null);
        }
        if (targetVersion == null) {
            targetVersion = string(request == null ? null : request.get("version_no"), null);
        }
        if (targetVersion == null) {
            throw new IllegalArgumentException("target_version is required");
        }

        Map<String, Object> targetConfig = publishedPathways.get(pathwayKey(pathwayCode, targetVersion));
        if (targetConfig == null) {
            throw new IllegalArgumentException("pathway version not found: " + pathwayCode + "@" + targetVersion);
        }

        String previousVersion = activeVersion(pathwayCode, null);
        activePublishedVersions.put(pathwayCode, targetVersion);
        persistenceService.savePathwayVersion(pathwayCode, targetVersion, "PUBLISHED", targetConfig);
        persistenceService.updatePathwayStatus(pathwayCode, "PUBLISHED",
                string(targetConfig.get("tenant_id"), null), string(targetConfig.get("org_code"), null));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("pathway_code", pathwayCode);
        result.put("previous_active_version", previousVersion);
        result.put("active_version", targetVersion);
        result.put("status", "ROLLED_BACK");
        result.put("operator_id", string(request == null ? null : request.get("operator_id"), null));
        result.put("reason", string(request == null ? null : request.get("reason"), null));
        result.put("persistence", persistenceService.providerName());
        audit("ROLLBACK", "PATHWAY", pathwayCode, null, result,
                string(request == null ? null : request.get("operator_id"), null));
        return result;
    }

    public List<Map<String, Object>> listPathways() {
        Set<String> pathwayCodes = new TreeSet<String>();
        pathwayCodes.addAll(pathwayDrafts.keySet());
        for (String key : publishedPathways.keySet()) {
            pathwayCodes.add(pathwayCodeFromKey(key));
        }

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (String pathwayCode : pathwayCodes) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("pathway_code", pathwayCode);
            item.put("draft_status", pathwayDrafts.containsKey(pathwayCode) ? "DRAFT" : "NONE");
            List<String> versions = publishedVersions(pathwayCode);
            item.put("published_versions", versions);
            item.put("latest_published_version", versions.isEmpty() ? null : versions.get(versions.size() - 1));
            item.put("active_published_version", activeVersion(pathwayCode, versions.isEmpty() ? null : versions.get(versions.size() - 1)));
            list.add(item);
        }
        return list;
    }

    public Map<String, Object> listPathwaysFiltered(Map<String, String> filters) {
        List<Map<String, Object>> all = listPathways();

        // 为每条记录补充 name/status/instanceCount/completionRate/dept 等前端所需字段
        for (Map<String, Object> item : all) {
            String pathwayCode = string(item.get("pathway_code"), null);
            Map<String, Object> draft = pathwayCode == null ? null : pathwayDrafts.get(pathwayCode);
            if (draft != null) {
                item.put("pathway_name", string(draft.get("pathway_name"), pathwayCode));
                item.put("specialty_code", string(draft.get("specialty_code"), null));
                item.put("disease_code", string(draft.get("disease_code"), null));
                item.put("dept", string(draft.get("dept"), null));
            } else {
                item.put("pathway_name", pathwayCode);
            }
            // 综合状态：有草稿且无已发布=DRAFT；有已发布=PUBLISHED；有草稿+已发布=DRAFT+PUBLISHED
            String draftStatus = string(item.get("draft_status"), "NONE");
            List<?> versions = (List<?>) item.get("published_versions");
            boolean hasPublished = versions != null && !versions.isEmpty();
            if ("DRAFT".equals(draftStatus) && hasPublished) {
                item.put("status", "DRAFT");
            } else if ("DRAFT".equals(draftStatus)) {
                item.put("status", "DRAFT");
            } else if (hasPublished) {
                item.put("status", "PUBLISHED");
            } else {
                item.put("status", "NONE");
            }
            // 入径数和完成率来自实例聚合
            String activeVersion = string(item.get("active_published_version"), null);
            Map<String, String> instanceFilters = new LinkedHashMap<String, String>();
            instanceFilters.put("pathwayCode", pathwayCode);
            instanceFilters.put("limit", String.valueOf(Integer.MAX_VALUE));
            List<PatientPathwayInstance> instances = listInstances(instanceFilters);
            int instanceCount = instances.size();
            int completedCount = 0;
            for (PatientPathwayInstance inst : instances) {
                if ("COMPLETED".equals(inst.getStatus()) || "EXITED".equals(inst.getStatus())) {
                    completedCount++;
                }
            }
            item.put("instance_count", instanceCount);
            item.put("completion_rate", instanceCount == 0 ? 0.0
                    : Math.round(completedCount * 10000.0 / instanceCount) / 100.0);
        }

        // 筛选
        String search = filterValue(filters, "search");
        String statusFilter = filterValue(filters, "status");
        String deptFilter = filterValue(filters, "dept");

        List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : all) {
            if (statusFilter != null && !statusFilter.equalsIgnoreCase(string(item.get("status"), null))) {
                continue;
            }
            if (deptFilter != null && !deptFilter.equalsIgnoreCase(string(item.get("dept"), null))
                    && !deptFilter.equalsIgnoreCase(string(item.get("specialty_code"), null))) {
                continue;
            }
            if (search != null) {
                String name = string(item.get("pathway_name"), "").toLowerCase();
                String code = string(item.get("pathway_code"), "").toLowerCase();
                String keyword = search.toLowerCase();
                if (!name.contains(keyword) && !code.contains(keyword)) {
                    continue;
                }
            }
            filtered.add(item);
        }

        // 分页
        int page = filterInt(filters, "page", 1);
        int size = filterInt(filters, "size", 20);
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        int total = filtered.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageData = fromIndex < total
                ? filtered.subList(fromIndex, toIndex) : Collections.<Map<String, Object>>emptyList();

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("items", pageData);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("total_pages", (total + size - 1) / size);
        return result;
    }

    public Map<String, Object> deletePathway(String pathwayCode) {
        Map<String, Object> draft = pathwayDrafts.get(pathwayCode);
        if (draft == null) {
            throw new IllegalArgumentException("pathway draft not found: " + pathwayCode);
        }
        List<String> versions = publishedVersions(pathwayCode);
        if (!versions.isEmpty()) {
            throw new IllegalArgumentException("cannot delete published pathway, please retire first: " + pathwayCode);
        }
        pathwayDrafts.remove(pathwayCode);
        persistenceService.deletePathwayDraft(pathwayCode);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("pathway_code", pathwayCode);
        result.put("status", "DELETED");
        result.put("persistence", persistenceService.providerName());
        audit("DELETE_DRAFT", "PATHWAY", pathwayCode, null, result, null);
        return result;
    }

    public Map<String, Object> diffPathway(String pathwayCode, String fromVersion, String toVersion) {
        Map<String, Object> fromConfig = loadVersionConfig(pathwayCode, fromVersion);
        Map<String, Object> toConfig = loadVersionConfig(pathwayCode, toVersion);
        if (fromConfig == null) {
            throw new IllegalArgumentException("pathway version not found: " + pathwayCode + "@" + fromVersion);
        }
        if (toConfig == null) {
            throw new IllegalArgumentException("pathway version not found: " + pathwayCode + "@" + toVersion);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("pathway_code", pathwayCode);
        result.put("from_version", fromVersion);
        result.put("to_version", toVersion);

        List<Map<String, Object>> metadataChanges = new ArrayList<Map<String, Object>>();
        for (String field : new String[] {"pathway_name", "specialty_code", "disease_code", "description"}) {
            String fromValue = string(fromConfig.get(field), null);
            String toValue = string(toConfig.get(field), null);
            if (!equalsNullable(fromValue, toValue)) {
                Map<String, Object> change = new LinkedHashMap<String, Object>();
                change.put("field", field);
                change.put("from", fromValue);
                change.put("to", toValue);
                metadataChanges.add(change);
            }
        }
        result.put("metadata_changes", metadataChanges);

        Map<String, Map<String, Object>> fromNodes = indexNodes(fromConfig);
        Map<String, Map<String, Object>> toNodes = indexNodes(toConfig);

        List<String> nodesAdded = new ArrayList<String>();
        List<String> nodesRemoved = new ArrayList<String>();
        List<Map<String, Object>> nodesModified = new ArrayList<Map<String, Object>>();

        for (String nodeCode : toNodes.keySet()) {
            if (!fromNodes.containsKey(nodeCode)) {
                nodesAdded.add(nodeCode);
            }
        }
        for (String nodeCode : fromNodes.keySet()) {
            if (!toNodes.containsKey(nodeCode)) {
                nodesRemoved.add(nodeCode);
                continue;
            }
            Map<String, Object> nodeDiff = diffNode(nodeCode, fromNodes.get(nodeCode), toNodes.get(nodeCode));
            if (nodeDiff != null) {
                nodesModified.add(nodeDiff);
            }
        }
        Collections.sort(nodesAdded);
        Collections.sort(nodesRemoved);

        result.put("nodes_added", nodesAdded);
        result.put("nodes_removed", nodesRemoved);
        result.put("nodes_modified", nodesModified);

        // 顶层 summary 让看板与 PR 评审能一眼看到变更规模。
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("metadata_changed", metadataChanges.size());
        summary.put("nodes_added", nodesAdded.size());
        summary.put("nodes_removed", nodesRemoved.size());
        summary.put("nodes_modified", nodesModified.size());
        result.put("summary", summary);

        return result;
    }

    private Map<String, Object> loadVersionConfig(String pathwayCode, String versionNo) {
        if (versionNo == null || versionNo.trim().isEmpty() || "draft".equalsIgnoreCase(versionNo)) {
            return pathwayDrafts.get(pathwayCode);
        }
        return publishedPathways.get(pathwayKey(pathwayCode, versionNo));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> indexNodes(Map<String, Object> config) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<String, Map<String, Object>>();
        Object stages = config == null ? null : config.get("stages");
        if (!(stages instanceof java.util.Collection)) {
            return map;
        }
        for (Object stageObject : (java.util.Collection<?>) stages) {
            if (!(stageObject instanceof Map)) {
                continue;
            }
            Object nodes = ((Map<String, Object>) stageObject).get("nodes");
            if (!(nodes instanceof java.util.Collection)) {
                continue;
            }
            for (Object nodeObject : (java.util.Collection<?>) nodes) {
                if (nodeObject instanceof Map) {
                    Map<String, Object> node = (Map<String, Object>) nodeObject;
                    String nodeCode = string(node.get("node_code"), null);
                    if (nodeCode != null) {
                        map.put(nodeCode, node);
                    }
                }
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> diffNode(String nodeCode, Map<String, Object> fromNode, Map<String, Object> toNode) {
        List<Map<String, Object>> fieldChanges = new ArrayList<Map<String, Object>>();
        for (String field : new String[] {"node_name", "node_type", "owner_role", "expected_minutes"}) {
            String fromValue = string(fromNode.get(field), null);
            String toValue = string(toNode.get(field), null);
            if (!equalsNullable(fromValue, toValue)) {
                Map<String, Object> change = new LinkedHashMap<String, Object>();
                change.put("field", field);
                change.put("from", fromValue);
                change.put("to", toValue);
                fieldChanges.add(change);
            }
        }

        Map<String, Map<String, Object>> fromTasks = indexByCode(fromNode.get("tasks"), "task_code");
        Map<String, Map<String, Object>> toTasks = indexByCode(toNode.get("tasks"), "task_code");
        List<String> tasksAdded = new ArrayList<String>();
        List<String> tasksRemoved = new ArrayList<String>();
        List<Map<String, Object>> tasksModified = new ArrayList<Map<String, Object>>();
        for (String code : toTasks.keySet()) {
            if (!fromTasks.containsKey(code)) {
                tasksAdded.add(code);
            }
        }
        for (String code : fromTasks.keySet()) {
            if (!toTasks.containsKey(code)) {
                tasksRemoved.add(code);
                continue;
            }
            List<Map<String, Object>> taskFieldChanges = diffFields(fromTasks.get(code), toTasks.get(code),
                    new String[] {"task_name", "task_type", "required", "source.adapter_code", "source.query_code"});
            if (!taskFieldChanges.isEmpty()) {
                Map<String, Object> taskDiff = new LinkedHashMap<String, Object>();
                taskDiff.put("task_code", code);
                taskDiff.put("fields", taskFieldChanges);
                tasksModified.add(taskDiff);
            }
        }
        Collections.sort(tasksAdded);
        Collections.sort(tasksRemoved);

        Map<String, Map<String, Object>> fromTransitions = indexByCode(fromNode.get("transitions"), "to_node");
        Map<String, Map<String, Object>> toTransitions = indexByCode(toNode.get("transitions"), "to_node");
        List<String> transitionsAdded = new ArrayList<String>();
        List<String> transitionsRemoved = new ArrayList<String>();
        for (String code : toTransitions.keySet()) {
            if (!fromTransitions.containsKey(code)) {
                transitionsAdded.add(code);
            }
        }
        for (String code : fromTransitions.keySet()) {
            if (!toTransitions.containsKey(code)) {
                transitionsRemoved.add(code);
            }
        }
        Collections.sort(transitionsAdded);
        Collections.sort(transitionsRemoved);

        if (fieldChanges.isEmpty() && tasksAdded.isEmpty() && tasksRemoved.isEmpty()
                && tasksModified.isEmpty() && transitionsAdded.isEmpty() && transitionsRemoved.isEmpty()) {
            return null;
        }

        Map<String, Object> nodeDiff = new LinkedHashMap<String, Object>();
        nodeDiff.put("node_code", nodeCode);
        nodeDiff.put("fields", fieldChanges);
        nodeDiff.put("tasks_added", tasksAdded);
        nodeDiff.put("tasks_removed", tasksRemoved);
        nodeDiff.put("tasks_modified", tasksModified);
        nodeDiff.put("transitions_added", transitionsAdded);
        nodeDiff.put("transitions_removed", transitionsRemoved);
        return nodeDiff;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> indexByCode(Object collection, String codeField) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<String, Map<String, Object>>();
        if (!(collection instanceof java.util.Collection)) {
            return map;
        }
        for (Object item : (java.util.Collection<?>) collection) {
            if (item instanceof Map) {
                Map<String, Object> entry = (Map<String, Object>) item;
                String code = string(entry.get(codeField), null);
                if (code != null) {
                    map.put(code, entry);
                }
            }
        }
        return map;
    }

    private List<Map<String, Object>> diffFields(Map<String, Object> fromMap, Map<String, Object> toMap, String[] fields) {
        List<Map<String, Object>> changes = new ArrayList<Map<String, Object>>();
        for (String field : fields) {
            String fromValue = readNested(fromMap, field);
            String toValue = readNested(toMap, field);
            if (!equalsNullable(fromValue, toValue)) {
                Map<String, Object> change = new LinkedHashMap<String, Object>();
                change.put("field", field);
                change.put("from", fromValue);
                change.put("to", toValue);
                changes.add(change);
            }
        }
        return changes;
    }

    @SuppressWarnings("unchecked")
    private String readNested(Map<String, Object> map, String path) {
        if (map == null) {
            return null;
        }
        String[] parts = path.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
        }
        return current == null ? null : String.valueOf(current);
    }

    private boolean equalsNullable(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    public Map<String, Object> getPathway(String pathwayCode, String versionNo) {
        Map<String, Object> draft = pathwayDrafts.get(pathwayCode);
        List<String> versions = publishedVersions(pathwayCode);
        String selectedVersion = string(versionNo, activeVersion(pathwayCode, versions.isEmpty() ? null : versions.get(versions.size() - 1)));
        Map<String, Object> published = selectedVersion == null ? null
                : publishedPathways.get(pathwayKey(pathwayCode, selectedVersion));

        if (draft == null && published == null) {
            throw new IllegalArgumentException("pathway not found: " + pathwayCode);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("pathway_code", pathwayCode);
        result.put("draft_status", draft == null ? "NONE" : "DRAFT");
        result.put("published_versions", versions);
        result.put("active_published_version", activeVersion(pathwayCode, versions.isEmpty() ? null : versions.get(versions.size() - 1)));
        result.put("selected_version", selectedVersion);
        result.put("draft_config", draft);
        result.put("published_config", published);
        result.put("reference_sources", configSupport.collectNodeReferences(
                published != null ? published : draft));
        result.put("reference_warnings", configSupport.collectMissingReferences(
                published != null ? published : draft));
        return result;
    }

    public List<RecommendationCard> candidates(Map<String, Object> patientContext) {
        Map<String, Object> ruleRequest = new HashMap<String, Object>();
        ruleRequest.put("patient_context", patientContext);
        List<RuleResult> ruleResults = ruleService.evaluate(ruleRequest);
        boolean stemiHit = false;
        for (RuleResult ruleResult : ruleResults) {
            if ("R_AMI_STEMI_CANDIDATE".equals(ruleResult.getRuleCode()) && ruleResult.isHit()) {
                stemiHit = true;
                break;
            }
        }

        List<RecommendationCard> cards = new ArrayList<RecommendationCard>();
        if (stemiHit) {
            RecommendationCard card = buildAmiRecommendation(patientContext);
            persistenceService.saveRecommendation(card);
            cards.add(card);
        }
        return cards;
    }

    public PatientPathwayInstance admit(Map<String, Object> request) {
        return admit(request, null);
    }

    public PatientPathwayInstance admit(Map<String, Object> request, OrganizationContext orgContext) {
        String encounterId = String.valueOf(request.get("encounter_id"));
        String pathwayCode = String.valueOf(request.get("pathway_code"));
        String versionNo = string(request.get("version_no"), activeVersion(pathwayCode, "1.0.0"));
        Map<String, Object> config = publishedPathways.get(pathwayKey(pathwayCode, versionNo));
        PatientPathwayInstance orgProbe = new PatientPathwayInstance();
        applyOrganization(orgProbe, orgContext, request);
        String key = instanceKey(orgProbe, encounterId, pathwayCode);
        PatientPathwayInstance existing = activeInstances.get(key);
        if (existing != null) {
            return existing;
        }

        // 入径首节点优先来自已发布路径配置；没有配置时保留AMI演示兜底，避免旧演示中断。
        String firstNodeCode = string(configSupport.firstNodeCode(config), "AMI_CHEST_PAIN_IDENTIFY");
        PatientPathwayInstance instance = new PatientPathwayInstance();
        instance.setInstanceId("ppi-" + UUID.randomUUID().toString().replace("-", ""));
        instance.setPatientId(String.valueOf(request.get("patient_id")));
        instance.setEncounterId(encounterId);
        instance.setPathwayCode(pathwayCode);
        instance.setVersionNo(versionNo);
        instance.setStatus("ACTIVE");
        instance.setCurrentNodeCode(firstNodeCode);
        applyOrganization(instance, orgContext, request);
        activeInstances.put(key, instance);

        persistenceService.savePatientInstance(instance, String.valueOf(request.get("doctor_id")));
        enterNode(instance, config, firstNodeCode);
        Map<String, Object> auditDetail = new LinkedHashMap<String, Object>();
        auditDetail.put("pathway_code", pathwayCode);
        auditDetail.put("version_no", versionNo);
        auditDetail.put("current_node_code", firstNodeCode);
        audit("ADMIT", "PATIENT_PATHWAY", pathwayCode, instance, auditDetail,
                string(request.get("doctor_id"), null));
        return instance;
    }

    public Map<String, Object> getInstanceDetail(String instanceId) {
        PatientPathwayInstance instance = findInstance(instanceId);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("instance", instance);
        result.put("nodes", new ArrayList<PatientNodeState>(instanceNodeStates(instanceId).values()));
        result.put("current_node", getNodeState(instanceId, instance.getCurrentNodeCode()));
        result.put("variations", variations(instanceId));
        return result;
    }

    public PatientNodeState getNodeState(String instanceId, String nodeCode) {
        PatientPathwayInstance instance = findInstance(instanceId);
        Map<String, Object> config = publishedConfig(instance);
        return ensureNodeState(instance, config, nodeCode, "WAITING");
    }

    public PatientTaskState completeTask(String instanceId, String nodeCode, String taskCode, Map<String, Object> request) {
        return updateTask(instanceId, nodeCode, taskCode, "COMPLETED", request);
    }

    public PatientTaskState skipTask(String instanceId, String nodeCode, String taskCode, Map<String, Object> request) {
        PatientTaskState taskState = updateTask(instanceId, nodeCode, taskCode, "SKIPPED", request);
        PatientPathwayInstance instance = findInstance(instanceId);
        Map<String, Object> variationRequest = new LinkedHashMap<String, Object>();
        variationRequest.put("node_code", nodeCode);
        variationRequest.put("variation_type", string(request == null ? null : request.get("variation_type"), "TASK_SKIPPED"));
        variationRequest.put("reason", string(request == null ? null : request.get("reason"), "任务未按路径执行：" + taskCode));
        variationRequest.put("operator_id", request == null ? null : request.get("operator_id"));
        createVariation(instance, variationRequest);
        return taskState;
    }

    public PathwayVariationRecord recordVariation(String instanceId, Map<String, Object> request) {
        return createVariation(findInstance(instanceId), request);
    }

    public PatientPathwayInstance completeNode(String instanceId, String nodeCode) {
        return completeNode(instanceId, nodeCode, new LinkedHashMap<String, Object>());
    }

    public PatientPathwayInstance completeNode(String instanceId, String nodeCode, Map<String, Object> request) {
        for (PatientPathwayInstance instance : activeInstances.values()) {
            if (instanceId.equals(instance.getInstanceId())) {
                Map<String, Object> config = publishedPathways.get(pathwayKey(instance.getPathwayCode(), instance.getVersionNo()));
                PatientNodeState nodeState = ensureNodeState(instance, config, nodeCode, "RUNNING");
                nodeState.setStatus("COMPLETED");
                nodeState.setCompleteTime(nowText());
                persistenceService.updateNodeState(instance, nodeCode, configSupport.nodeName(config, nodeCode), "COMPLETED");
                if (hasVariation(request)) {
                    Map<String, Object> variationRequest = new LinkedHashMap<String, Object>(request);
                    variationRequest.put("node_code", nodeCode);
                    createVariation(instance, variationRequest);
                }
                String nextNodeCode = configSupport.nextNodeCode(config, nodeCode);
                if (nextNodeCode == null) {
                    nextNodeCode = builtInNextNode(nodeCode);
                }
                if (nextNodeCode != null) {
                    instance.setCurrentNodeCode(nextNodeCode);
                    enterNode(instance, config, nextNodeCode);
                }
                persistenceService.savePatientInstance(instance, null);
                Map<String, Object> auditDetail = new LinkedHashMap<String, Object>();
                auditDetail.put("node_code", nodeCode);
                auditDetail.put("next_node_code", nextNodeCode);
                auditDetail.put("status", "COMPLETED");
                audit("COMPLETE_NODE", "PATHWAY_NODE", nodeCode, instance, auditDetail,
                        string(request == null ? null : request.get("operator_id"), null));
                return instance;
            }
        }
        PatientPathwayInstance empty = new PatientPathwayInstance();
        empty.setInstanceId(instanceId);
        empty.setStatus("NOT_FOUND");
        return empty;
    }

    private void enterNode(PatientPathwayInstance instance, Map<String, Object> config, String nodeCode) {
        PatientNodeState nodeState = ensureNodeState(instance, config, nodeCode, "RUNNING");
        nodeState.setStatus("RUNNING");
        if (nodeState.getEnterTime() == null) {
            nodeState.setEnterTime(nowText());
        }
        persistenceService.updateNodeState(instance, nodeCode, configSupport.nodeName(config, nodeCode), "RUNNING");
        initializeTasks(instance, config, nodeState);
    }

    private void initializeTasks(PatientPathwayInstance instance, Map<String, Object> config, PatientNodeState nodeState) {
        List<Map<String, Object>> tasks = configSupport.nodeTasks(config, nodeState.getNodeCode());
        for (Map<String, Object> taskConfig : tasks) {
            String taskCode = string(taskConfig.get("task_code"), null);
            if (taskCode == null || findTask(nodeState, taskCode) != null) {
                continue;
            }
            PatientTaskState taskState = buildTaskState(instance, nodeState.getNodeCode(), taskConfig);
            nodeState.getTasks().add(taskState);
            persistenceService.saveTaskState(taskState);
        }
    }

    private PatientTaskState updateTask(String instanceId, String nodeCode, String taskCode,
                                        String status, Map<String, Object> request) {
        PatientPathwayInstance instance = findInstance(instanceId);
        Map<String, Object> config = publishedConfig(instance);
        PatientNodeState nodeState = ensureNodeState(instance, config, nodeCode, "RUNNING");
        initializeTasks(instance, config, nodeState);
        Map<String, Object> taskConfig = configSupport.nodeTask(config, nodeCode, taskCode);
        PatientTaskState taskState = findTask(nodeState, taskCode);
        if (taskState == null) {
            taskState = fallbackTaskState(instance, nodeCode, taskCode);
            nodeState.getTasks().add(taskState);
        }
        taskState.setStatus(status);
        taskState.setOperatorId(string(request == null ? null : request.get("operator_id"), null));
        taskState.setUpdatedTime(nowText());
        taskState.setResult(resultSnapshot(request, taskConfig, instance, nodeCode, taskCode, status));
        persistenceService.saveTaskState(taskState);
        Map<String, Object> auditDetail = new LinkedHashMap<String, Object>();
        auditDetail.put("node_code", nodeCode);
        auditDetail.put("task_code", taskCode);
        auditDetail.put("status", status);
        audit(("COMPLETED".equals(status) ? "COMPLETE_TASK" : "SKIP_TASK"), "PATHWAY_TASK",
                nodeCode + "." + taskCode, instance, auditDetail,
                string(request == null ? null : request.get("operator_id"), null));
        return taskState;
    }

    private PatientNodeState ensureNodeState(PatientPathwayInstance instance, Map<String, Object> config,
                                             String nodeCode, String defaultStatus) {
        Map<String, PatientNodeState> states = instanceNodeStates(instance.getInstanceId());
        PatientNodeState nodeState = states.get(nodeCode);
        if (nodeState == null) {
            nodeState = new PatientNodeState();
            nodeState.setInstanceId(instance.getInstanceId());
            nodeState.setNodeCode(nodeCode);
            nodeState.setNodeName(configSupport.nodeName(config, nodeCode));
            nodeState.setStatus(defaultStatus);
            nodeState.setEnterTime(nowText());
            states.put(nodeCode, nodeState);
        }
        return nodeState;
    }

    private PatientTaskState buildTaskState(PatientPathwayInstance instance, String nodeCode, Map<String, Object> taskConfig) {
        PatientTaskState taskState = new PatientTaskState();
        taskState.setInstanceId(instance.getInstanceId());
        taskState.setNodeCode(nodeCode);
        taskState.setTaskCode(string(taskConfig.get("task_code"), null));
        taskState.setTaskName(string(taskConfig.get("task_name"), taskState.getTaskCode()));
        taskState.setTaskType(string(taskConfig.get("task_type"), "TASK"));
        taskState.setRequired(booleanValue(taskConfig.get("required"), false));
        taskState.setStatus("PENDING");
        taskState.setUpdatedTime(nowText());
        return taskState;
    }

    private PatientTaskState fallbackTaskState(PatientPathwayInstance instance, String nodeCode, String taskCode) {
        PatientTaskState taskState = new PatientTaskState();
        taskState.setInstanceId(instance.getInstanceId());
        taskState.setNodeCode(nodeCode);
        taskState.setTaskCode(taskCode);
        taskState.setTaskName(taskCode);
        taskState.setTaskType("TASK");
        taskState.setRequired(false);
        taskState.setStatus("PENDING");
        taskState.setUpdatedTime(nowText());
        return taskState;
    }

    private PatientTaskState findTask(PatientNodeState nodeState, String taskCode) {
        for (PatientTaskState taskState : nodeState.getTasks()) {
            if (taskCode.equals(taskState.getTaskCode())) {
                return taskState;
            }
        }
        return null;
    }

    private PathwayVariationRecord createVariation(PatientPathwayInstance instance, Map<String, Object> request) {
        PathwayVariationRecord variation = new PathwayVariationRecord();
        variation.setVariationId("var-" + UUID.randomUUID().toString().replace("-", ""));
        variation.setInstanceId(instance.getInstanceId());
        // 路径编码与版本来自实例，便于跨实例按路径聚合变异统计；Oracle 落库表暂不存这两列。
        variation.setPathwayCode(instance.getPathwayCode());
        variation.setVersionNo(instance.getVersionNo());
        variation.setPatientId(instance.getPatientId());
        variation.setEncounterId(instance.getEncounterId());
        variation.setNodeCode(string(request == null ? null : request.get("node_code"), instance.getCurrentNodeCode()));
        variation.setVariationType(string(request == null ? null : request.get("variation_type"), "PATHWAY_DEVIATION"));
        variation.setReason(variationReason(request));
        variation.setOperatorId(string(request == null ? null : request.get("operator_id"), null));
        variation.setCreatedTime(nowText());
        copyOrganization(instance, variation);
        variations(instance.getInstanceId()).add(variation);
        persistenceService.saveVariationRecord(variation);
        Map<String, Object> auditDetail = new LinkedHashMap<String, Object>();
        auditDetail.put("variation_id", variation.getVariationId());
        auditDetail.put("node_code", variation.getNodeCode());
        auditDetail.put("variation_type", variation.getVariationType());
        auditDetail.put("reason", variation.getReason());
        audit("RECORD_VARIATION", "PATHWAY_VARIATION", variation.getVariationId(), instance, auditDetail,
                variation.getOperatorId());
        return variation;
    }

    public List<PatientPathwayInstance> listInstances(Map<String, String> filters) {
        String pathwayCode = filterValue(filters, "pathwayCode");
        String status = filterValue(filters, "status");
        String patientId = filterValue(filters, "patientId");
        String encounterId = filterValue(filters, "encounterId");
        String currentNodeCode = filterValue(filters, "currentNodeCode");
        String tenantId = filterValue(filters, "tenantId");
        String groupCode = filterValue(filters, "groupCode");
        String hospitalCode = filterValue(filters, "hospitalCode");
        String campusCode = filterValue(filters, "campusCode");
        String siteCode = filterValue(filters, "siteCode");
        String departmentCode = filterValue(filters, "departmentCode");
        String scopeLevel = filterValue(filters, "scopeLevel");
        String scopeCode = filterValue(filters, "scopeCode");
        int limit = filterInt(filters, "limit", 100);
        if (limit <= 0) {
            limit = 100;
        }

        List<PatientPathwayInstance> all = new ArrayList<PatientPathwayInstance>(activeInstances.values());
        Collections.sort(all, new Comparator<PatientPathwayInstance>() {
            @Override
            public int compare(PatientPathwayInstance left, PatientPathwayInstance right) {
                // instanceId 内含 UUID hex，没有时间戳；按 encounterId 字典序倒序兜底保持稳定顺序。
                String l = left.getEncounterId() == null ? "" : left.getEncounterId();
                String r = right.getEncounterId() == null ? "" : right.getEncounterId();
                return r.compareTo(l);
            }
        });

        List<PatientPathwayInstance> matched = new ArrayList<PatientPathwayInstance>();
        for (PatientPathwayInstance instance : all) {
            if (pathwayCode != null && !pathwayCode.equalsIgnoreCase(instance.getPathwayCode())) {
                continue;
            }
            if (status != null && !status.equalsIgnoreCase(instance.getStatus())) {
                continue;
            }
            if (patientId != null && !patientId.equals(instance.getPatientId())) {
                continue;
            }
            if (encounterId != null && !encounterId.equals(instance.getEncounterId())) {
                continue;
            }
            if (currentNodeCode != null && !currentNodeCode.equalsIgnoreCase(instance.getCurrentNodeCode())) {
                continue;
            }
            if (!matchesOrg(instance, tenantId, groupCode, hospitalCode, campusCode,
                    siteCode, departmentCode, scopeLevel, scopeCode)) {
                continue;
            }
            matched.add(instance);
            if (matched.size() >= limit) {
                break;
            }
        }
        return matched;
    }

    public Map<String, Object> summarizeNodeCompletion(Map<String, String> filters) {
        List<PatientPathwayInstance> instances = listInstances(merge(filters, "limit", String.valueOf(Integer.MAX_VALUE)));
        Map<String, NodeAggregate> aggregates = new LinkedHashMap<String, NodeAggregate>();
        for (PatientPathwayInstance instance : instances) {
            Map<String, PatientNodeState> states = nodeStates.get(instance.getInstanceId());
            if (states == null) {
                continue;
            }
            for (PatientNodeState nodeState : states.values()) {
                String nodeCode = nodeState.getNodeCode();
                NodeAggregate agg = aggregates.get(nodeCode);
                if (agg == null) {
                    agg = new NodeAggregate(nodeCode, nodeState.getNodeName());
                    aggregates.put(nodeCode, agg);
                }
                agg.recordEnter();
                agg.recordStatus(nodeState.getStatus());
                for (PatientTaskState taskState : nodeState.getTasks()) {
                    agg.recordTask(taskState.getTaskCode(), taskState.getTaskName(),
                            taskState.getStatus(), taskState.isRequired());
                }
            }
        }

        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        int totalEntered = 0;
        int totalCompleted = 0;
        List<String> orderedKeys = new ArrayList<String>(aggregates.keySet());
        Collections.sort(orderedKeys);
        for (String nodeCode : orderedKeys) {
            NodeAggregate agg = aggregates.get(nodeCode);
            nodes.add(agg.toView());
            totalEntered += agg.entered;
            totalCompleted += agg.completed;
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total_instances", instances.size());
        summary.put("total_nodes", nodes.size());
        summary.put("total_node_entries", totalEntered);
        summary.put("total_node_completions", totalCompleted);
        summary.put("average_node_completion_rate", totalEntered == 0
                ? 0.0 : Math.round((totalCompleted * 10000.0 / totalEntered)) / 100.0);
        summary.put("nodes", nodes);
        return summary;
    }

    public Map<String, Object> summarizeNodeStayDuration(Map<String, String> filters) {
        List<PatientPathwayInstance> instances = listInstances(merge(filters, "limit", String.valueOf(Integer.MAX_VALUE)));
        Map<String, NodeDurationAggregate> aggregates = new LinkedHashMap<String, NodeDurationAggregate>();
        OffsetDateTime now = OffsetDateTime.now();
        for (PatientPathwayInstance instance : instances) {
            Map<String, PatientNodeState> states = nodeStates.get(instance.getInstanceId());
            if (states == null) {
                continue;
            }
            Map<String, Object> config = publishedConfig(instance);
            for (PatientNodeState nodeState : states.values()) {
                String nodeCode = nodeState.getNodeCode();
                NodeDurationAggregate agg = aggregates.get(nodeCode);
                if (agg == null) {
                    agg = new NodeDurationAggregate(nodeCode, nodeState.getNodeName(),
                            expectedMinutes(config, nodeCode));
                    aggregates.put(nodeCode, agg);
                }
                agg.record(nodeState, stayMillis(nodeState, now));
            }
        }

        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        List<String> orderedKeys = new ArrayList<String>(aggregates.keySet());
        Collections.sort(orderedKeys);
        int totalEntries = 0;
        int totalCompleted = 0;
        int totalRunning = 0;
        long totalStayMs = 0L;
        for (String nodeCode : orderedKeys) {
            NodeDurationAggregate agg = aggregates.get(nodeCode);
            nodes.add(agg.toView());
            totalEntries += agg.entered;
            totalCompleted += agg.completed;
            totalRunning += agg.running;
            totalStayMs += agg.totalStayMs;
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total_instances", instances.size());
        summary.put("total_node_entries", totalEntries);
        summary.put("completed_node_entries", totalCompleted);
        summary.put("running_node_entries", totalRunning);
        summary.put("average_stay_ms", totalEntries == 0 ? 0.0 : Math.round((totalStayMs * 100.0 / totalEntries)) / 100.0);
        summary.put("average_stay_minutes", totalEntries == 0 ? 0.0
                : Math.round((totalStayMs / 60000.0 * 100.0 / totalEntries)) / 100.0);
        summary.put("nodes", nodes);
        return summary;
    }

    private static class NodeDurationAggregate {
        private final String nodeCode;
        private final String nodeName;
        private final Integer expectedMinutes;
        private int entered;
        private int completed;
        private int running;
        private int waiting;
        private int timeout;
        private long totalStayMs;
        private long minStayMs = Long.MAX_VALUE;
        private long maxStayMs;

        NodeDurationAggregate(String nodeCode, String nodeName, Integer expectedMinutes) {
            this.nodeCode = nodeCode;
            this.nodeName = nodeName;
            this.expectedMinutes = expectedMinutes;
        }

        void record(PatientNodeState nodeState, long stayMs) {
            entered++;
            totalStayMs += stayMs;
            minStayMs = Math.min(minStayMs, stayMs);
            maxStayMs = Math.max(maxStayMs, stayMs);
            if ("COMPLETED".equals(nodeState.getStatus())) {
                completed++;
            } else if ("RUNNING".equals(nodeState.getStatus())) {
                running++;
            } else {
                waiting++;
            }
            if (isTimeout(stayMs)) {
                timeout++;
            }
        }

        private boolean isTimeout(long stayMs) {
            return expectedMinutes != null && expectedMinutes > 0 && stayMs > expectedMinutes * 60000L;
        }

        Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("node_code", nodeCode);
            view.put("node_name", nodeName);
            view.put("expected_minutes", expectedMinutes);
            view.put("entered", entered);
            view.put("completed", completed);
            view.put("running", running);
            view.put("waiting", waiting);
            view.put("timeout_count", timeout);
            view.put("timeout_rate", entered == 0 ? 0.0 : Math.round((timeout * 10000.0 / entered)) / 100.0);
            view.put("average_stay_ms", entered == 0 ? 0.0 : Math.round((totalStayMs * 100.0 / entered)) / 100.0);
            view.put("average_stay_minutes", entered == 0 ? 0.0
                    : Math.round((totalStayMs / 60000.0 * 100.0 / entered)) / 100.0);
            view.put("min_stay_ms", entered == 0 ? 0L : minStayMs);
            view.put("max_stay_ms", maxStayMs);
            return view;
        }
    }

    private static class NodeAggregate {
        private final String nodeCode;
        private final String nodeName;
        private int entered;
        private int completed;
        private int running;
        private int waiting;
        private int otherStatus;
        private final Map<String, TaskAggregate> tasks = new LinkedHashMap<String, TaskAggregate>();

        NodeAggregate(String nodeCode, String nodeName) {
            this.nodeCode = nodeCode;
            this.nodeName = nodeName;
        }

        void recordEnter() {
            entered++;
        }

        void recordStatus(String status) {
            if ("COMPLETED".equals(status)) {
                completed++;
            } else if ("RUNNING".equals(status)) {
                running++;
            } else if ("WAITING".equals(status) || status == null) {
                waiting++;
            } else {
                otherStatus++;
            }
        }

        void recordTask(String taskCode, String taskName, String status, boolean required) {
            if (taskCode == null) {
                return;
            }
            TaskAggregate task = tasks.get(taskCode);
            if (task == null) {
                task = new TaskAggregate(taskCode, taskName, required);
                tasks.put(taskCode, task);
            }
            task.total++;
            if ("COMPLETED".equals(status)) {
                task.completed++;
            } else if ("SKIPPED".equals(status)) {
                task.skipped++;
            } else {
                task.pending++;
            }
        }

        Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("node_code", nodeCode);
            view.put("node_name", nodeName);
            view.put("entered", entered);
            view.put("completed", completed);
            view.put("running", running);
            view.put("waiting", waiting);
            view.put("other_status", otherStatus);
            view.put("completion_rate", entered == 0
                    ? 0.0 : Math.round((completed * 10000.0 / entered)) / 100.0);

            List<Map<String, Object>> taskViews = new ArrayList<Map<String, Object>>();
            List<String> sortedTaskKeys = new ArrayList<String>(tasks.keySet());
            Collections.sort(sortedTaskKeys);
            for (String key : sortedTaskKeys) {
                taskViews.add(tasks.get(key).toView());
            }
            view.put("tasks", taskViews);
            return view;
        }
    }

    private static class TaskAggregate {
        private final String taskCode;
        private final String taskName;
        private final boolean required;
        private int total;
        private int completed;
        private int skipped;
        private int pending;

        TaskAggregate(String taskCode, String taskName, boolean required) {
            this.taskCode = taskCode;
            this.taskName = taskName;
            this.required = required;
        }

        Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("task_code", taskCode);
            view.put("task_name", taskName);
            view.put("required", required);
            view.put("total", total);
            view.put("completed", completed);
            view.put("skipped", skipped);
            view.put("pending", pending);
            view.put("completion_rate", total == 0
                    ? 0.0 : Math.round((completed * 10000.0 / total)) / 100.0);
            return view;
        }
    }

    public Map<String, Object> summarizeInstances(Map<String, String> filters) {
        List<PatientPathwayInstance> instances = listInstances(merge(filters, "limit", String.valueOf(Integer.MAX_VALUE)));
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total", instances.size());
        summary.put("by_pathway_code", aggregateInstances(instances, "pathway_code"));
        summary.put("by_status", aggregateInstances(instances, "status"));
        summary.put("by_current_node", aggregateInstances(instances, "current_node_code"));
        summary.put("by_hospital_code", aggregateInstances(instances, "hospital_code"));
        summary.put("by_scope", aggregateInstances(instances, "scope"));

        // 联动变异统计：在同一过滤上下文下统计相关变异，便于看板一次拿到“路径在径数 + 变异数”全景。
        Map<String, String> variationFilters = new LinkedHashMap<String, String>();
        if (filters != null) {
            String pathwayCode = filters.get("pathwayCode");
            if (pathwayCode != null) {
                variationFilters.put("pathwayCode", pathwayCode);
            }
            String patientId = filters.get("patientId");
            if (patientId != null) {
                variationFilters.put("patientId", patientId);
            }
            String encounterId = filters.get("encounterId");
            if (encounterId != null) {
                variationFilters.put("encounterId", encounterId);
            }
            copyFilter(filters, variationFilters, "tenantId");
            copyFilter(filters, variationFilters, "groupCode");
            copyFilter(filters, variationFilters, "hospitalCode");
            copyFilter(filters, variationFilters, "campusCode");
            copyFilter(filters, variationFilters, "siteCode");
            copyFilter(filters, variationFilters, "departmentCode");
            copyFilter(filters, variationFilters, "scopeLevel");
            copyFilter(filters, variationFilters, "scopeCode");
        }
        variationFilters.put("limit", String.valueOf(Integer.MAX_VALUE));
        List<PathwayVariationRecord> variations = listVariations(variationFilters);
        summary.put("variation_total", variations.size());
        summary.put("variation_by_type", aggregate(variations, "variation_type"));
        return summary;
    }

    private List<Map<String, Object>> aggregateInstances(List<PatientPathwayInstance> instances, String dimension) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (PatientPathwayInstance instance : instances) {
            String key = instanceDimensionKey(instance, dimension);
            if (key == null) {
                continue;
            }
            Integer count = counts.get(key);
            counts.put(key, count == null ? 1 : count + 1);
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
                int byCount = right.getValue().compareTo(left.getValue());
                return byCount != 0 ? byCount : left.getKey().compareTo(right.getKey());
            }
        });
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : entries) {
            Map<String, Object> bucket = new LinkedHashMap<String, Object>();
            bucket.put(dimension, entry.getKey());
            bucket.put("count", entry.getValue());
            result.add(bucket);
        }
        return result;
    }

    private String instanceDimensionKey(PatientPathwayInstance instance, String dimension) {
        if ("pathway_code".equals(dimension)) {
            return instance.getPathwayCode();
        }
        if ("status".equals(dimension)) {
            return instance.getStatus();
        }
        if ("current_node_code".equals(dimension)) {
            return instance.getCurrentNodeCode();
        }
        if ("hospital_code".equals(dimension)) {
            return instance.getHospitalCode();
        }
        if ("scope".equals(dimension)) {
            return scopeKey(instance.getScopeLevel(), instance.getScopeCode());
        }
        return null;
    }

    public List<PathwayVariationRecord> listVariations(Map<String, String> filters) {
        String pathwayCode = filterValue(filters, "pathwayCode");
        String patientId = filterValue(filters, "patientId");
        String encounterId = filterValue(filters, "encounterId");
        String variationType = filterValue(filters, "variationType");
        String nodeCode = filterValue(filters, "nodeCode");
        String instanceId = filterValue(filters, "instanceId");
        String tenantId = filterValue(filters, "tenantId");
        String groupCode = filterValue(filters, "groupCode");
        String hospitalCode = filterValue(filters, "hospitalCode");
        String campusCode = filterValue(filters, "campusCode");
        String siteCode = filterValue(filters, "siteCode");
        String departmentCode = filterValue(filters, "departmentCode");
        String scopeLevel = filterValue(filters, "scopeLevel");
        String scopeCode = filterValue(filters, "scopeCode");
        int limit = filterInt(filters, "limit", 100);
        if (limit <= 0) {
            limit = 100;
        }

        List<PathwayVariationRecord> all = new ArrayList<PathwayVariationRecord>();
        for (List<PathwayVariationRecord> bucket : variationRecords.values()) {
            all.addAll(bucket);
        }
        Collections.sort(all, new Comparator<PathwayVariationRecord>() {
            @Override
            public int compare(PathwayVariationRecord left, PathwayVariationRecord right) {
                // 按 createdTime 倒序展示，最新变异优先暴露给质控看板。
                String l = left.getCreatedTime();
                String r = right.getCreatedTime();
                if (l == null && r == null) {
                    return 0;
                }
                if (l == null) {
                    return 1;
                }
                if (r == null) {
                    return -1;
                }
                return r.compareTo(l);
            }
        });

        List<PathwayVariationRecord> matched = new ArrayList<PathwayVariationRecord>();
        for (PathwayVariationRecord record : all) {
            if (pathwayCode != null && !pathwayCode.equalsIgnoreCase(record.getPathwayCode())) {
                continue;
            }
            if (patientId != null && !patientId.equals(record.getPatientId())) {
                continue;
            }
            if (encounterId != null && !encounterId.equals(record.getEncounterId())) {
                continue;
            }
            if (variationType != null && !variationType.equalsIgnoreCase(record.getVariationType())) {
                continue;
            }
            if (nodeCode != null && !nodeCode.equalsIgnoreCase(record.getNodeCode())) {
                continue;
            }
            if (instanceId != null && !instanceId.equals(record.getInstanceId())) {
                continue;
            }
            if (!matchesOrg(record, tenantId, groupCode, hospitalCode, campusCode,
                    siteCode, departmentCode, scopeLevel, scopeCode)) {
                continue;
            }
            matched.add(record);
            if (matched.size() >= limit) {
                break;
            }
        }
        return matched;
    }

    public Map<String, Object> summarizeVariations(Map<String, String> filters) {
        List<PathwayVariationRecord> records = listVariations(merge(filters, "limit", String.valueOf(Integer.MAX_VALUE)));
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total", records.size());
        summary.put("by_variation_type", aggregate(records, "variation_type"));
        summary.put("by_pathway_code", aggregate(records, "pathway_code"));
        summary.put("by_node_code", aggregate(records, "node_code"));
        summary.put("by_patient_id", aggregate(records, "patient_id"));
        summary.put("by_hospital_code", aggregate(records, "hospital_code"));
        summary.put("by_scope", aggregate(records, "scope"));
        return summary;
    }

    private List<Map<String, Object>> aggregate(List<PathwayVariationRecord> records, String dimension) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (PathwayVariationRecord record : records) {
            String key = dimensionKey(record, dimension);
            if (key == null) {
                continue;
            }
            Integer count = counts.get(key);
            counts.put(key, count == null ? 1 : count + 1);
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
                int byCount = right.getValue().compareTo(left.getValue());
                return byCount != 0 ? byCount : left.getKey().compareTo(right.getKey());
            }
        });
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : entries) {
            Map<String, Object> bucket = new LinkedHashMap<String, Object>();
            bucket.put(dimension, entry.getKey());
            bucket.put("count", entry.getValue());
            result.add(bucket);
        }
        return result;
    }

    private String dimensionKey(PathwayVariationRecord record, String dimension) {
        if ("variation_type".equals(dimension)) {
            return record.getVariationType();
        }
        if ("pathway_code".equals(dimension)) {
            return record.getPathwayCode();
        }
        if ("node_code".equals(dimension)) {
            return record.getNodeCode();
        }
        if ("patient_id".equals(dimension)) {
            return record.getPatientId();
        }
        if ("hospital_code".equals(dimension)) {
            return record.getHospitalCode();
        }
        if ("scope".equals(dimension)) {
            return scopeKey(record.getScopeLevel(), record.getScopeCode());
        }
        return null;
    }

    private void applyOrganization(PatientPathwayInstance instance, OrganizationContext orgContext,
                                   Map<String, Object> request) {
        if (orgContext != null) {
            instance.setTenantId(orgContext.getTenantId());
            instance.setGroupCode(orgContext.getGroupCode());
            instance.setHospitalCode(orgContext.getHospitalCode());
            instance.setCampusCode(orgContext.getCampusCode());
            instance.setSiteCode(orgContext.getSiteCode());
            instance.setDepartmentCode(orgContext.getDepartmentCode());
            instance.setLegacyOrgCode(orgContext.getLegacyOrgCode());
            instance.setScopeLevel(orgContext.getEffectiveScopeLevel());
            instance.setScopeCode(orgContext.getEffectiveScopeCode());
            instance.setOrgSource(orgContext.getSource());
            return;
        }

        String tenantId = bodyString(request, "tenant_id", "tenantId", "default");
        String groupCode = bodyString(request, "group_code", "groupCode", null);
        String hospitalCode = bodyString(request, "hospital_code", "hospitalCode", null);
        String campusCode = bodyString(request, "campus_code", "campusCode", null);
        String siteCode = bodyString(request, "site_code", "siteCode", null);
        String departmentCode = bodyString(request, "department_code", "departmentCode", null);
        String legacyOrgCode = bodyString(request, "org_code", "orgCode", null);
        if (hospitalCode == null && legacyOrgCode != null) {
            hospitalCode = legacyOrgCode;
        }
        if (hospitalCode == null) {
            hospitalCode = "ZYHOSPITAL";
        }
        if (legacyOrgCode == null) {
            legacyOrgCode = hospitalCode;
        }

        instance.setTenantId(tenantId);
        instance.setGroupCode(groupCode);
        instance.setHospitalCode(hospitalCode);
        instance.setCampusCode(campusCode);
        instance.setSiteCode(siteCode);
        instance.setDepartmentCode(departmentCode);
        instance.setLegacyOrgCode(legacyOrgCode);
        applyEffectiveScope(instance);
        instance.setOrgSource(hasBodyOrg(request) ? "BODY" : "DEFAULT");
    }

    private void applyEffectiveScope(PatientPathwayInstance instance) {
        if (present(instance.getDepartmentCode())) {
            instance.setScopeLevel("DEPARTMENT");
            instance.setScopeCode(instance.getDepartmentCode());
            return;
        }
        if (present(instance.getSiteCode())) {
            instance.setScopeLevel("SITE");
            instance.setScopeCode(instance.getSiteCode());
            return;
        }
        if (present(instance.getCampusCode())) {
            instance.setScopeLevel("CAMPUS");
            instance.setScopeCode(instance.getCampusCode());
            return;
        }
        if (present(instance.getHospitalCode())) {
            instance.setScopeLevel("HOSPITAL");
            instance.setScopeCode(instance.getHospitalCode());
            return;
        }
        if (present(instance.getGroupCode())) {
            instance.setScopeLevel("GROUP");
            instance.setScopeCode(instance.getGroupCode());
            return;
        }
        instance.setScopeLevel("PLATFORM");
        instance.setScopeCode("DEFAULT");
    }

    private void copyOrganization(PatientPathwayInstance instance, PathwayVariationRecord variation) {
        variation.setTenantId(instance.getTenantId());
        variation.setGroupCode(instance.getGroupCode());
        variation.setHospitalCode(instance.getHospitalCode());
        variation.setCampusCode(instance.getCampusCode());
        variation.setSiteCode(instance.getSiteCode());
        variation.setDepartmentCode(instance.getDepartmentCode());
        variation.setLegacyOrgCode(instance.getLegacyOrgCode());
        variation.setScopeLevel(instance.getScopeLevel());
        variation.setScopeCode(instance.getScopeCode());
        variation.setOrgSource(instance.getOrgSource());
    }

    private String instanceKey(PatientPathwayInstance instance, String encounterId, String pathwayCode) {
        return string(instance.getTenantId(), "default") + "::"
                + string(instance.getScopeLevel(), "HOSPITAL") + "::"
                + string(instance.getScopeCode(), string(instance.getHospitalCode(), "ZYHOSPITAL")) + "::"
                + encounterId + "::" + pathwayCode;
    }

    private boolean matchesOrg(PatientPathwayInstance instance, String tenantId, String groupCode,
                               String hospitalCode, String campusCode, String siteCode, String departmentCode,
                               String scopeLevel, String scopeCode) {
        return matches(tenantId, instance.getTenantId(), false)
                && matches(groupCode, instance.getGroupCode(), false)
                && matches(hospitalCode, instance.getHospitalCode(), false)
                && matches(campusCode, instance.getCampusCode(), false)
                && matches(siteCode, instance.getSiteCode(), false)
                && matches(departmentCode, instance.getDepartmentCode(), false)
                && matches(scopeLevel, instance.getScopeLevel(), true)
                && matches(scopeCode, instance.getScopeCode(), false);
    }

    private boolean matchesOrg(PathwayVariationRecord record, String tenantId, String groupCode,
                               String hospitalCode, String campusCode, String siteCode, String departmentCode,
                               String scopeLevel, String scopeCode) {
        return matches(tenantId, record.getTenantId(), false)
                && matches(groupCode, record.getGroupCode(), false)
                && matches(hospitalCode, record.getHospitalCode(), false)
                && matches(campusCode, record.getCampusCode(), false)
                && matches(siteCode, record.getSiteCode(), false)
                && matches(departmentCode, record.getDepartmentCode(), false)
                && matches(scopeLevel, record.getScopeLevel(), true)
                && matches(scopeCode, record.getScopeCode(), false);
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

    private String scopeKey(String scopeLevel, String scopeCode) {
        if (scopeLevel == null && scopeCode == null) {
            return null;
        }
        return string(scopeLevel, "UNKNOWN") + ":" + string(scopeCode, "UNKNOWN");
    }

    private void copyFilter(Map<String, String> source, Map<String, String> target, String key) {
        String value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }

    private String bodyString(Map<String, Object> request, String snakeKey, String camelKey, String defaultValue) {
        if (request == null) {
            return defaultValue;
        }
        String value = string(request.get(snakeKey), null);
        if (value == null) {
            value = string(request.get(camelKey), null);
        }
        return value == null ? defaultValue : value;
    }

    private boolean hasBodyOrg(Map<String, Object> request) {
        return bodyString(request, "tenant_id", "tenantId", null) != null
                || bodyString(request, "group_code", "groupCode", null) != null
                || bodyString(request, "hospital_code", "hospitalCode", null) != null
                || bodyString(request, "campus_code", "campusCode", null) != null
                || bodyString(request, "site_code", "siteCode", null) != null
                || bodyString(request, "department_code", "departmentCode", null) != null
                || bodyString(request, "org_code", "orgCode", null) != null;
    }

    private Map<String, String> merge(Map<String, String> filters, String key, String value) {
        Map<String, String> merged = new LinkedHashMap<String, String>();
        if (filters != null) {
            merged.putAll(filters);
        }
        merged.put(key, value);
        return merged;
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

    private boolean hasVariation(Map<String, Object> request) {
        if (request == null) {
            return false;
        }
        return request.get("variation_type") != null
                || request.get("variation_reason") != null
                || request.get("deviation_reason") != null
                || request.get("reason") != null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resultSnapshot(Map<String, Object> request, Map<String, Object> taskConfig,
                                               PatientPathwayInstance instance, String nodeCode,
                                               String taskCode, String status) {
        Map<String, Object> snapshot;
        if (request == null || request.isEmpty()) {
            snapshot = new LinkedHashMap<String, Object>();
        } else {
            Object result = request.get("result");
            if (result instanceof Map) {
                snapshot = new LinkedHashMap<String, Object>((Map<String, Object>) result);
            } else {
                snapshot = new LinkedHashMap<String, Object>(request);
            }
        }
        if ("COMPLETED".equals(status)) {
            enrichWithAdapterResult(snapshot, request, taskConfig, instance, nodeCode, taskCode);
        }
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private void enrichWithAdapterResult(Map<String, Object> snapshot, Map<String, Object> request,
                                         Map<String, Object> taskConfig, PatientPathwayInstance instance,
                                         String nodeCode, String taskCode) {
        if (taskConfig == null || !(taskConfig.get("source") instanceof Map)) {
            return;
        }
        Map<String, Object> source = (Map<String, Object>) taskConfig.get("source");
        String adapterCode = string(source.get("adapter_code"), null);
        String queryCode = string(source.get("query_code"), null);
        if (adapterCode == null || queryCode == null) {
            return;
        }

        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("patient_id", instance.getPatientId());
        params.put("encounter_id", instance.getEncounterId());
        params.put("pathway_code", instance.getPathwayCode());
        params.put("version_no", instance.getVersionNo());
        params.put("instance_id", instance.getInstanceId());
        params.put("node_code", nodeCode);
        params.put("task_code", taskCode);
        if (request != null && request.get("params") instanceof Map) {
            params.putAll((Map<String, Object>) request.get("params"));
        }

        Map<String, Object> adapterRequest = new LinkedHashMap<String, Object>();
        adapterRequest.put("adapter_code", adapterCode);
        adapterRequest.put("query_code", queryCode);
        adapterRequest.put("params", params);
        Map<String, Object> adapterResult = adapterHubService.query(adapterRequest);

        snapshot.put("source", source);
        snapshot.put("adapter_query", adapterResult);
        snapshot.put("adapter_status", adapterResult.get("status"));
        snapshot.put("adapter_row_count", adapterResult.get("row_count"));
    }

    private Map<String, PatientNodeState> instanceNodeStates(String instanceId) {
        Map<String, PatientNodeState> states = nodeStates.get(instanceId);
        if (states == null) {
            states = new ConcurrentHashMap<String, PatientNodeState>();
            nodeStates.put(instanceId, states);
        }
        return states;
    }

    private List<PathwayVariationRecord> variations(String instanceId) {
        List<PathwayVariationRecord> records = variationRecords.get(instanceId);
        if (records == null) {
            records = new ArrayList<PathwayVariationRecord>();
            variationRecords.put(instanceId, records);
        }
        return records;
    }

    private PatientPathwayInstance findInstance(String instanceId) {
        for (PatientPathwayInstance instance : activeInstances.values()) {
            if (instanceId.equals(instance.getInstanceId())) {
                return instance;
            }
        }
        throw new IllegalArgumentException("patient pathway instance not found: " + instanceId);
    }

    private Map<String, Object> publishedConfig(PatientPathwayInstance instance) {
        return publishedPathways.get(pathwayKey(instance.getPathwayCode(), instance.getVersionNo()));
    }

    private RecommendationCard buildAmiRecommendation(Map<String, Object> patientContext) {
        RecommendationCard card = new RecommendationCard();
        card.setRecommendationId("rec-" + UUID.randomUUID().toString().replace("-", ""));
        card.setScenario("PATHWAY_ENTRY");
        card.setPatientId(ClinicalFactUtils.patientId(patientContext));
        card.setEncounterId(ClinicalFactUtils.encounterId(patientContext));
        card.setTargetCode("AMI_STEMI");
        card.setTargetName("急性ST段抬高型心肌梗死诊疗路径");
        card.setScore(calculateAmiScore(patientContext));
        card.setConfidence("HIGH");
        card.setActionLevel("STRONG_ALERT");
        card.getSupportingFacts().add(fact("chief_complaint", "胸痛相关主诉命中。"));
        card.getSupportingFacts().add(fact("exam_finding", "心电图ST段抬高检查发现命中。"));
        if (ClinicalFactUtils.hasHistory(patientContext, "DIABETES")) {
            card.getSupportingFacts().add(fact("risk_factor", "糖尿病危险因素命中。"));
        }
        if (ClinicalFactUtils.hasHistory(patientContext, "HYPERTENSION")) {
            card.getSupportingFacts().add(fact("risk_factor", "高血压危险因素命中。"));
        }
        card.setMissingFacts(Arrays.asList(
                "肌钙蛋白结果",
                "溶栓禁忌证评估",
                "出血风险评估"
        ));
        card.setEvidenceRefs(Arrays.asList("EV_AMI_001"));
        card.setSuggestedActions(Arrays.asList(
                "请医生确认是否启动AMI/STEMI诊疗路径。",
                "请完善再灌注适应证与禁忌证评估。"
        ));
        return card;
    }

    private double calculateAmiScore(Map<String, Object> patientContext) {
        double graph = 92;
        double evidence = 95;
        double patient = 88;
        double llm = 80;
        double local = 70;
        if (ClinicalFactUtils.hasHistory(patientContext, "DIABETES")) {
            patient += 2;
        }
        if (ClinicalFactUtils.hasHistory(patientContext, "HYPERTENSION")) {
            patient += 2;
        }
        return round(graph * 0.45 + evidence * 0.25 + patient * 0.15 + llm * 0.10 + local * 0.05);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<String, Object> fact(String type, String text) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        map.put("text", text);
        return map;
    }

    private String builtInNextNode(String nodeCode) {
        if ("AMI_CHEST_PAIN_IDENTIFY".equals(nodeCode)) {
            return "AMI_REPERFUSION_EVAL";
        }
        if ("AMI_REPERFUSION_EVAL".equals(nodeCode)) {
            return "AMI_INPATIENT_TREATMENT";
        }
        return null;
    }

    private String pathwayKey(String pathwayCode, String versionNo) {
        return pathwayCode + "::" + versionNo;
    }

    private String pathwayCodeFromKey(String key) {
        int index = key.indexOf("::");
        return index < 0 ? key : key.substring(0, index);
    }

    private List<String> publishedVersions(String pathwayCode) {
        List<String> versions = new ArrayList<String>();
        String prefix = pathwayCode + "::";
        for (String key : publishedPathways.keySet()) {
            if (key.startsWith(prefix)) {
                versions.add(key.substring(prefix.length()));
            }
        }
        Collections.sort(versions);
        return versions;
    }

    private String activeVersion(String pathwayCode, String defaultValue) {
        String active = activePublishedVersions.get(pathwayCode);
        if (active != null && publishedPathways.containsKey(pathwayKey(pathwayCode, active))) {
            return active;
        }
        return defaultValue;
    }

    private Integer expectedMinutes(Map<String, Object> config, String nodeCode) {
        Map<String, Object> node = configSupport.findNode(config, nodeCode);
        if (node == null) {
            return null;
        }
        Object value = node.get("expected_minutes");
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long stayMillis(PatientNodeState nodeState, OffsetDateTime now) {
        OffsetDateTime enter = parseTime(nodeState.getEnterTime());
        if (enter == null) {
            return 0L;
        }
        OffsetDateTime end = "COMPLETED".equals(nodeState.getStatus())
                ? parseTime(nodeState.getCompleteTime()) : now;
        if (end == null || end.isBefore(enter)) {
            end = now;
        }
        long millis = Duration.between(enter, end).toMillis();
        return Math.max(0L, millis);
    }

    private OffsetDateTime parseTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void audit(String actionType, String targetType, String targetCode, PatientPathwayInstance instance,
                       Map<String, Object> detail, String operatorId) {
        try {
            Map<String, Object> auditDetail = detail == null
                    ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(detail);
            enrichOrgDetail(auditDetail, instance);
            persistenceService.saveAuditLog("PATHWAY", actionType, targetType, targetCode,
                    instance == null ? null : instance.getPatientId(),
                    instance == null ? null : instance.getEncounterId(),
                    operatorId,
                    auditDetail);
        } catch (RuntimeException ignored) {
            // 路径核心状态变更不能因为审计落库失败中断。
        }
    }

    private void enrichOrgDetail(Map<String, Object> detail, PatientPathwayInstance instance) {
        if (instance == null) {
            return;
        }
        detail.put("tenant_id", instance.getTenantId());
        detail.put("group_code", instance.getGroupCode());
        detail.put("hospital_code", instance.getHospitalCode());
        detail.put("campus_code", instance.getCampusCode());
        detail.put("site_code", instance.getSiteCode());
        detail.put("department_code", instance.getDepartmentCode());
        detail.put("legacy_org_code", instance.getLegacyOrgCode());
        detail.put("scope_level", instance.getScopeLevel());
        detail.put("scope_code", instance.getScopeCode());
        detail.put("org_source", instance.getOrgSource());
    }

    private String required(Map<String, Object> map, String key) {
        String value = string(map.get(key), null);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    private boolean present(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String variationReason(Map<String, Object> request) {
        String reason = string(request == null ? null : request.get("reason"), null);
        if (reason == null) {
            reason = string(request == null ? null : request.get("variation_reason"), null);
        }
        if (reason == null) {
            reason = string(request == null ? null : request.get("deviation_reason"), null);
        }
        return reason == null ? "路径执行发生变异，待补充原因。" : reason;
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }
}
