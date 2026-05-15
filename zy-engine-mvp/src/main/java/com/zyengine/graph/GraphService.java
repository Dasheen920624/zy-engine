package com.zyengine.graph;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GraphService {
    public List<GraphCandidate> diseaseCandidates(Map<String, Object> request) {
        List<GraphCandidate> candidates = new ArrayList<GraphCandidate>();
        boolean chestPain = contains(request.get("symptom_codes"), "CHEST_PAIN");
        boolean stElevation = contains(request.get("finding_codes"), "ST_ELEVATION_CONTIGUOUS_LEADS");
        boolean diabetes = contains(request.get("risk_factor_codes"), "DIABETES");
        boolean hypertension = contains(request.get("risk_factor_codes"), "HYPERTENSION");

        if (chestPain || stElevation) {
            GraphCandidate candidate = new GraphCandidate();
            candidate.setDiseaseCode("AMI_STEMI");
            candidate.setDiseaseName("急性ST段抬高型心肌梗死");
            candidate.setRawGraphScore(stElevation ? 92 : 72);
            if (chestPain) {
                candidate.getMatchedRelations().add(relation("HAS_CORE_SYMPTOM", "CHEST_PAIN", "胸痛", 1.0));
            }
            if (stElevation) {
                candidate.getMatchedRelations().add(relation("HAS_EXAM_FINDING", "ST_ELEVATION_CONTIGUOUS_LEADS", "相邻导联ST段抬高", 1.0));
            }
            if (diabetes) {
                candidate.getMatchedRelations().add(relation("HAS_RISK_FACTOR", "DIABETES", "糖尿病", 0.45));
            }
            if (hypertension) {
                candidate.getMatchedRelations().add(relation("HAS_RISK_FACTOR", "HYPERTENSION", "高血压", 0.45));
            }
            candidate.setEvidenceRefs(Arrays.asList("EV_AMI_001"));
            candidates.add(candidate);
        }
        return candidates;
    }

    public List<Map<String, Object>> evidence(Map<String, Object> request) {
        Map<String, Object> evidence = new HashMap<String, Object>();
        evidence.put("evidence_id", "EV_AMI_001");
        evidence.put("title", "AMI院内路径专家审核证据");
        evidence.put("source", "院内AMI路径 V1.0");
        evidence.put("target_code", request.get("target_code"));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.add(evidence);
        return list;
    }

    private Map<String, Object> relation(String type, String code, String name, double weight) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        map.put("code", code);
        map.put("name", name);
        map.put("weight", weight);
        return map;
    }

    private boolean contains(Object values, String expected) {
        if (values instanceof Iterable) {
            for (Object value : (Iterable<?>) values) {
                if (expected.equals(String.valueOf(value))) {
                    return true;
                }
            }
        }
        return false;
    }
}

