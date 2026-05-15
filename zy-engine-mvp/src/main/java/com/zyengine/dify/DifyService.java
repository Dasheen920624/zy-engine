package com.zyengine.dify;

import com.zyengine.common.ErrorCode;
import com.zyengine.common.TraceContext;
import com.zyengine.persistence.EnginePersistenceService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DifyService {
    private final DifyProperties properties;
    private final EnginePersistenceService persistenceService;

    public DifyService(DifyProperties properties, EnginePersistenceService persistenceService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> runWorkflow(Map<String, Object> request) {
        long start = System.currentTimeMillis();
        String workflowCode = string(request.get("workflow_code"), "WF_UNKNOWN");
        String workflowVersion = string(request.get("workflow_version"), null);
        Map<String, Object> inputs = map(request.get("inputs"));
        if (inputs.isEmpty()) {
            inputs = new LinkedHashMap<String, Object>(request);
        }

        try {
            if (!properties.ready()) {
                return degraded(workflowCode, workflowVersion, inputs, "DIFY_DISABLED",
                        properties.isEnabled() ? "Dify配置不完整，返回本地降级解释。" : "Dify未启用，返回本地降级解释。",
                        System.currentTimeMillis() - start);
            }

            Map<String, Object> difyRequest = new LinkedHashMap<String, Object>();
            difyRequest.put("inputs", inputs);
            difyRequest.put("response_mode", "blocking");
            difyRequest.put("user", string(request.get("user"), string(properties.getUser(), "zy-engine-mvp")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + properties.getApiKey());

            RestTemplate restTemplate = restTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(workflowUrl(),
                    new HttpEntity<Map<String, Object>>(difyRequest, headers), Map.class);
            Map<String, Object> body = response.getBody() == null
                    ? new LinkedHashMap<String, Object>() : response.getBody();
            Map<String, Object> result = success(workflowCode, workflowVersion, body, System.currentTimeMillis() - start);
            audit(workflowCode, workflowVersion, inputs, result);
            return result;
        } catch (ResourceAccessException ex) {
            return degraded(workflowCode, workflowVersion, inputs, ErrorCode.DIFY_TIMEOUT.getCode(),
                    "Dify调用超时或网络不可达，已返回本地降级解释。", System.currentTimeMillis() - start);
        } catch (RestClientException ex) {
            return degraded(workflowCode, workflowVersion, inputs, "DIFY_ERROR",
                    "Dify调用失败，已返回本地降级解释：" + ex.getClass().getSimpleName(), System.currentTimeMillis() - start);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> success(String workflowCode, String workflowVersion, Map<String, Object> body, long elapsedMs) {
        Object data = body.get("data");
        Map<String, Object> dataMap = data instanceof Map ? (Map<String, Object>) data : new LinkedHashMap<String, Object>();
        Object outputs = dataMap.get("outputs");
        if (!(outputs instanceof Map)) {
            outputs = body.get("outputs");
        }

        Map<String, Object> result = baseResult(workflowCode, workflowVersion, elapsedMs);
        result.put("status", "SUCCESS");
        result.put("provider", "DIFY");
        result.put("message", "Dify工作流调用成功。");
        result.put("outputs", outputs instanceof Map ? outputs : new LinkedHashMap<String, Object>());
        result.put("raw_response", body);
        return result;
    }

    private Map<String, Object> degraded(String workflowCode, String workflowVersion, Map<String, Object> inputs,
                                         String errorCode, String message, long elapsedMs) {
        Map<String, Object> outputs = new LinkedHashMap<String, Object>();
        outputs.put("explanation", "已基于规则和图谱证据生成降级解释，请医生结合病历确认。");
        outputs.put("recommended_action", "保留路径推荐与人工确认，待Dify恢复后可补充更完整说明。");
        outputs.put("target_code", string(inputs.get("target_code"), string(inputs.get("pathway_code"), null)));

        Map<String, Object> result = baseResult(workflowCode, workflowVersion, elapsedMs);
        result.put("status", "DEGRADED");
        result.put("provider", "LOCAL_FALLBACK");
        result.put("message", message);
        result.put("error_code", errorCode);
        result.put("outputs", outputs);
        audit(workflowCode, workflowVersion, inputs, result);
        return result;
    }

    private Map<String, Object> baseResult(String workflowCode, String workflowVersion, long elapsedMs) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("workflow_code", workflowCode);
        result.put("workflow_version", workflowVersion);
        result.put("trace_id", TraceContext.getTraceId());
        result.put("elapsed_ms", elapsedMs);
        return result;
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        return new RestTemplate(factory);
    }

    private String workflowUrl() {
        String baseUrl = properties.getBaseUrl().trim();
        if (baseUrl.endsWith("/workflows/run")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/workflows/run";
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "v1/workflows/run";
        }
        return baseUrl + "/v1/workflows/run";
    }

    private void audit(String workflowCode, String workflowVersion, Map<String, Object> inputs, Map<String, Object> result) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("workflow_version", workflowVersion);
        detail.put("status", result.get("status"));
        detail.put("provider", result.get("provider"));
        detail.put("message", result.get("message"));
        detail.put("error_code", result.get("error_code"));
        detail.put("elapsed_ms", result.get("elapsed_ms"));
        try {
            persistenceService.saveAuditLog("DIFY", "WORKFLOW_RUN", "WORKFLOW", workflowCode,
                    patientId(inputs), encounterId(inputs), string(inputs.get("operator_id"), null), detail);
        } catch (RuntimeException ignored) {
            // 审计写入失败不应影响Dify降级策略。
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<String, Object>();
    }

    @SuppressWarnings("unchecked")
    private String patientId(Map<String, Object> inputs) {
        String direct = string(inputs.get("patient_id"), null);
        if (direct != null) {
            return direct;
        }
        Object patient = inputs.get("patient");
        if (patient instanceof Map) {
            return string(((Map<String, Object>) patient).get("patient_id"), null);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String encounterId(Map<String, Object> inputs) {
        String direct = string(inputs.get("encounter_id"), null);
        if (direct != null) {
            return direct;
        }
        Object encounter = inputs.get("encounter");
        if (encounter instanceof Map) {
            return string(((Map<String, Object>) encounter).get("encounter_id"), null);
        }
        return null;
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }
}
