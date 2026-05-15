package com.zyengine.rule;

import com.zyengine.dto.RuleResult;
import com.zyengine.util.ClinicalFactUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleService {
    public List<RuleResult> evaluate(Map<String, Object> request) {
        Map<String, Object> patientContext = getPatientContext(request);
        List<RuleResult> results = new ArrayList<RuleResult>();
        results.add(evaluateStemiCandidate(patientContext));
        results.add(evaluateEcgTimely(patientContext));
        return results;
    }

    public RuleResult simulate(Map<String, Object> request) {
        Map<String, Object> patientContext = getPatientContext(request);
        return evaluateStemiCandidate(patientContext);
    }

    private RuleResult evaluateStemiCandidate(Map<String, Object> context) {
        boolean hit = ClinicalFactUtils.hasChestPain(context) && ClinicalFactUtils.hasStElevation(context);
        RuleResult result = new RuleResult();
        result.setRuleCode("R_AMI_STEMI_CANDIDATE");
        result.setHit(hit);
        result.setSeverity(hit ? "HIGH" : "INFO");
        result.setMessage(hit
                ? "疑似STEMI，请医生评估是否启动急性心肌梗死诊疗路径。"
                : "未命中STEMI候选入径规则。");
        if (hit) {
            result.setActions(Arrays.asList("CREATE_RECOMMENDATION", "PUSH_TO_DOCTOR"));
            result.getEvidence().add(evidence("chief_complaint", "胸痛相关主诉命中。"));
            result.getEvidence().add(evidence("exam_finding", "心电图ST段抬高检查发现命中。"));
        }
        return result;
    }

    private RuleResult evaluateEcgTimely(Map<String, Object> context) {
        RuleResult result = new RuleResult();
        result.setRuleCode("R_AMI_ECG_TIMELY");
        result.setHit(false);
        result.setSeverity("INFO");
        result.setMessage("样例患者心电图已完成，未触发心电图超时质控。");
        result.setActions(new ArrayList<String>());
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPatientContext(Map<String, Object> request) {
        Object value = request.get("patient_context");
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return request;
    }

    private Map<String, Object> evidence(String type, String text) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        map.put("text", text);
        return map;
    }
}
