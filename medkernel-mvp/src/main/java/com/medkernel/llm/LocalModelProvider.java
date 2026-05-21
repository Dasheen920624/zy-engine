package com.medkernel.llm;

import com.medkernel.common.TraceContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LocalModelProvider implements ModelProvider {

    @Override
    public String getProviderType() {
        return "LOCAL";
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public Map<String, Object> invoke(Map<String, Object> request) {
        Map<String, Object> safeRequest = request != null ? request : new LinkedHashMap<String, Object>();

        Map<String, Object> outputs = new LinkedHashMap<String, Object>();
        outputs.put("explanation", "已基于规则和图谱证据生成降级解释，请医生结合病历确认。");
        outputs.put("recommended_action", "保留路径推荐与人工确认，待外部模型恢复后可补充更完整说明。");

        Object targetCode = safeRequest.get("target_code");
        if (targetCode == null) {
            targetCode = safeRequest.get("pathway_code");
        }
        if (targetCode != null && !outputs.containsKey("target_code")) {
            outputs.put("target_code", String.valueOf(targetCode));
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("status", "DEGRADED");
        result.put("provider", "LOCAL");
        result.put("message", "外部模型不可用，已返回本地规则降级结果。");
        result.put("outputs", outputs);
        result.put("trace_id", TraceContext.getTraceId());
        return result;
    }

    @Override
    public String getProviderName() {
        return "LocalRuleProvider";
    }
}
