package com.zyengine.pathway;

import com.zyengine.dto.PatientPathwayInstance;
import com.zyengine.dto.RecommendationCard;
import com.zyengine.dto.RuleResult;
import com.zyengine.persistence.EnginePersistenceService;
import com.zyengine.rule.RuleService;
import com.zyengine.util.ClinicalFactUtils;
import org.springframework.stereotype.Service;

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
    private final EnginePersistenceService persistenceService;
    private final PathwayConfigSupport configSupport = new PathwayConfigSupport();
    private final Map<String, PatientPathwayInstance> activeInstances = new ConcurrentHashMap<String, PatientPathwayInstance>();
    private final Map<String, Map<String, Object>> pathwayDrafts = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> publishedPathways = new ConcurrentHashMap<String, Map<String, Object>>();

    public PathwayService(RuleService ruleService, EnginePersistenceService persistenceService) {
        this.ruleService = ruleService;
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
        persistenceService.updateNodeState(instance, firstNodeCode, "RUNNING");
        return instance;
    }

    public PatientPathwayInstance completeNode(String instanceId, String nodeCode) {
        for (PatientPathwayInstance instance : activeInstances.values()) {
            if (instanceId.equals(instance.getInstanceId())) {
                persistenceService.updateNodeState(instance, nodeCode, "COMPLETED");
                Map<String, Object> config = publishedPathways.get(pathwayKey(instance.getPathwayCode(), instance.getVersionNo()));
                String nextNodeCode = configSupport.nextNodeCode(config, nodeCode);
                if (nextNodeCode == null) {
                    nextNodeCode = builtInNextNode(nodeCode);
                }
                if (nextNodeCode != null) {
                    instance.setCurrentNodeCode(nextNodeCode);
                    persistenceService.updateNodeState(instance, nextNodeCode, "RUNNING");
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
}
