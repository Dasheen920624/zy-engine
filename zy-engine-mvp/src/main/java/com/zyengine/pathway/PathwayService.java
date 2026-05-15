package com.zyengine.pathway;

import com.zyengine.adapter.AdapterHubService;
import com.zyengine.dto.PatientPathwayInstance;
import com.zyengine.dto.PatientNodeState;
import com.zyengine.dto.PatientTaskState;
import com.zyengine.dto.PathwayVariationRecord;
import com.zyengine.dto.RecommendationCard;
import com.zyengine.dto.RuleResult;
import com.zyengine.persistence.EnginePersistenceService;
import com.zyengine.rule.RuleService;
import com.zyengine.util.ClinicalFactUtils;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PathwayService {
    private final RuleService ruleService;
    private final AdapterHubService adapterHubService;
    private final EnginePersistenceService persistenceService;
    private final PathwayConfigSupport configSupport = new PathwayConfigSupport();
    private final Map<String, PatientPathwayInstance> activeInstances = new ConcurrentHashMap<String, PatientPathwayInstance>();
    private final Map<String, Map<String, Object>> pathwayDrafts = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> publishedPathways = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, PatientNodeState>> nodeStates = new ConcurrentHashMap<String, Map<String, PatientNodeState>>();
    private final Map<String, List<PathwayVariationRecord>> variationRecords = new ConcurrentHashMap<String, List<PathwayVariationRecord>>();

    public PathwayService(RuleService ruleService, AdapterHubService adapterHubService,
                          EnginePersistenceService persistenceService) {
        this.ruleService = ruleService;
        this.adapterHubService = adapterHubService;
        this.persistenceService = persistenceService;
    }

    public Map<String, Object> createPathway(Map<String, Object> config) {
        String pathwayCode = required(config, "pathway_code");
        pathwayDrafts.put(pathwayCode, config);
        persistenceService.savePathwayDraft(pathwayCode, config);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("pathway_code", pathwayCode);
        result.put("status", "DRAFT");
        result.put("persistence", persistenceService.enabled() ? "ORACLE" : "MEMORY");
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
        persistenceService.savePathwayVersion(pathwayCode, versionNo, "PUBLISHED", config);
        persistenceService.updatePathwayStatus(pathwayCode, "PUBLISHED");

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("pathway_code", pathwayCode);
        result.put("version_no", versionNo);
        result.put("status", "PUBLISHED");
        result.put("persistence", persistenceService.enabled() ? "ORACLE" : "MEMORY");
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
        String encounterId = String.valueOf(request.get("encounter_id"));
        String pathwayCode = String.valueOf(request.get("pathway_code"));
        String versionNo = string(request.get("version_no"), "1.0.0");
        Map<String, Object> config = publishedPathways.get(pathwayKey(pathwayCode, versionNo));
        String key = encounterId + "::" + pathwayCode;
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
        activeInstances.put(key, instance);

        persistenceService.savePatientInstance(instance, String.valueOf(request.get("doctor_id")));
        enterNode(instance, config, firstNodeCode);
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
        variation.setPatientId(instance.getPatientId());
        variation.setEncounterId(instance.getEncounterId());
        variation.setNodeCode(string(request == null ? null : request.get("node_code"), instance.getCurrentNodeCode()));
        variation.setVariationType(string(request == null ? null : request.get("variation_type"), "PATHWAY_DEVIATION"));
        variation.setReason(variationReason(request));
        variation.setOperatorId(string(request == null ? null : request.get("operator_id"), null));
        variation.setCreatedTime(nowText());
        variations(instance.getInstanceId()).add(variation);
        persistenceService.saveVariationRecord(variation);
        return variation;
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
