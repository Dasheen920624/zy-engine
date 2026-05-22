package com.medkernel.dify.workflow;

import com.medkernel.common.ErrorCode;
import com.medkernel.common.TraceContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DifyWorkflowExecutor {

    private static final int MAX_TIMEOUT_MS = 30_000;

    private final DifyProperties properties;
    private final DifyTemplateManager templateManager;
    private final DifyInvocationRecorder invocationRecorder;

    DifyWorkflowExecutor(DifyProperties properties, DifyTemplateManager templateManager,
                         DifyInvocationRecorder invocationRecorder) {
        this.properties = properties;
        this.templateManager = templateManager;
        this.invocationRecorder = invocationRecorder;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> runWorkflow(Map<String, Object> request) {
        Map<String, Object> safeRequest = request == null
                ? new LinkedHashMap<String, Object>() : request;
        long start = System.currentTimeMillis();
        String workflowCode = DifyWorkflowUtils.string(safeRequest.get("workflow_code"), "WF_UNKNOWN");
        String workflowVersion = DifyWorkflowUtils.string(safeRequest.get("workflow_version"), null);
        Map<String, Object> inputs = DifyWorkflowUtils.map(safeRequest.get("inputs"));
        if (inputs.isEmpty()) {
            inputs = new LinkedHashMap<String, Object>(safeRequest);
        }

        DifyWorkflowTemplate template = templateManager.findTemplate(workflowCode, workflowVersion);
        if (template != null) {
            if (workflowVersion == null) {
                workflowVersion = template.getWorkflowVersion();
            }
            inputs = applyInputMappings(inputs, template, safeRequest);
            inputs = applyInputDefaults(inputs, template);
            List<String> missing = missingRequiredInputs(inputs, template);
            if (!missing.isEmpty()) {
                String message = "dify workflow inputs missing required fields: " + missing;
                Map<String, Object> result = baseResult(workflowCode, workflowVersion,
                        System.currentTimeMillis() - start);
                result.put("status", "VALIDATION_ERROR");
                result.put("provider", "LOCAL_VALIDATION");
                result.put("message", message);
                result.put("error_code", ErrorCode.VALIDATION_ERROR.getCode());
                result.put("template_applied", true);
                invocationRecorder.audit(workflowCode, workflowVersion, inputs, result);
                throw new IllegalArgumentException(message);
            }
        }

        try {
            if (!properties.ready()) {
                return degraded(workflowCode, workflowVersion, inputs, template, "DIFY_DISABLED",
                        properties.isEnabled() ? "Dify配置不完整，返回本地降级解释。" : "Dify未启用，返回本地降级解释。",
                        System.currentTimeMillis() - start);
            }

            Map<String, Object> difyRequest = new LinkedHashMap<String, Object>();
            difyRequest.put("inputs", inputs);
            difyRequest.put("response_mode", "blocking");
            difyRequest.put("user", DifyWorkflowUtils.string(safeRequest.get("user"),
                    DifyWorkflowUtils.string(properties.getUser(), "medkernel-mvp")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + properties.getApiKey());

            RestTemplate restTemplate = restTemplate(template);
            int maxAttempts = retryCount(template) + 1;
            RestClientException lastException = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity(workflowUrl(),
                            new HttpEntity<Map<String, Object>>(difyRequest, headers), Map.class);
                    Map<String, Object> body = response.getBody() == null
                            ? new LinkedHashMap<String, Object>() : response.getBody();
                    Map<String, Object> result = success(workflowCode, workflowVersion, body,
                            System.currentTimeMillis() - start);
                    result.put("attempts", attempt);
                    result.put("retry_count", retryCount(template));
                    invocationRecorder.audit(workflowCode, workflowVersion, inputs, result);
                    return result;
                } catch (RestClientException ex) {
                    lastException = ex;
                    if (attempt >= maxAttempts) {
                        break;
                    }
                }
            }
            if (lastException instanceof ResourceAccessException) {
                return degraded(workflowCode, workflowVersion, inputs, template, ErrorCode.DIFY_TIMEOUT.getCode(),
                        "Dify调用超时或网络不可达，已重试后返回本地降级解释。",
                        System.currentTimeMillis() - start, maxAttempts, retryCount(template));
            }
            return degraded(workflowCode, workflowVersion, inputs, template, "DIFY_ERROR",
                    "Dify调用失败，已重试后返回本地降级解释：" +
                            (lastException == null ? "UNKNOWN" : lastException.getClass().getSimpleName()),
                    System.currentTimeMillis() - start, maxAttempts, retryCount(template));
        } catch (ResourceAccessException ex) {
            return degraded(workflowCode, workflowVersion, inputs, template, ErrorCode.DIFY_TIMEOUT.getCode(),
                    "Dify调用超时或网络不可达，已返回本地降级解释。", System.currentTimeMillis() - start);
        } catch (RestClientException ex) {
            return degraded(workflowCode, workflowVersion, inputs, template, "DIFY_ERROR",
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
                                         DifyWorkflowTemplate template, String errorCode, String message, long elapsedMs) {
        return degraded(workflowCode, workflowVersion, inputs, template, errorCode, message, elapsedMs, null, null);
    }

    private Map<String, Object> degraded(String workflowCode, String workflowVersion, Map<String, Object> inputs,
                                         DifyWorkflowTemplate template, String errorCode, String message,
                                         long elapsedMs, Integer attempts, Integer retryCount) {
        Map<String, Object> outputs = new LinkedHashMap<String, Object>();
        if (template != null && template.getDegradedOutputs() != null && !template.getDegradedOutputs().isEmpty()) {
            outputs.putAll(template.getDegradedOutputs());
        } else {
            outputs.put("explanation", "已基于规则和图谱证据生成降级解释，请医生结合病历确认。");
            outputs.put("recommended_action", "保留路径推荐与人工确认，待Dify恢复后可补充更完整说明。");
        }
        if (!outputs.containsKey("target_code")) {
            outputs.put("target_code", DifyWorkflowUtils.string(inputs.get("target_code"),
                    DifyWorkflowUtils.string(inputs.get("pathway_code"), null)));
        }

        Map<String, Object> result = baseResult(workflowCode, workflowVersion, elapsedMs);
        result.put("status", "DEGRADED");
        result.put("provider", "LOCAL_FALLBACK");
        result.put("message", message);
        result.put("error_code", errorCode);
        result.put("outputs", outputs);
        result.put("template_applied", template != null);
        if (attempts != null) {
            result.put("attempts", attempts);
        }
        if (retryCount != null) {
            result.put("retry_count", retryCount);
        }
        invocationRecorder.audit(workflowCode, workflowVersion, inputs, result);
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

    private RestTemplate restTemplate(DifyWorkflowTemplate template) {
        int rawTimeout = template != null && template.getTimeoutMs() != null && template.getTimeoutMs() > 0
                ? template.getTimeoutMs() : properties.getTimeoutMs();
        int timeout = Math.min(Math.max(rawTimeout, 0), MAX_TIMEOUT_MS);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
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

    private int retryCount(DifyWorkflowTemplate template) {
        if (template == null || template.getRetryCount() == null || template.getRetryCount() < 0) {
            return 0;
        }
        return Math.min(template.getRetryCount(), 3);
    }

    private Map<String, Object> applyInputDefaults(Map<String, Object> inputs, DifyWorkflowTemplate template) {
        Map<String, Object> merged = new LinkedHashMap<String, Object>();
        if (template.getInputDefaults() != null) {
            merged.putAll(template.getInputDefaults());
        }
        if (inputs != null) {
            for (Map.Entry<String, Object> entry : inputs.entrySet()) {
                if (entry.getValue() != null) {
                    merged.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return merged;
    }

    private Map<String, Object> applyInputMappings(Map<String, Object> inputs, DifyWorkflowTemplate template,
                                                   Map<String, Object> request) {
        Map<String, Object> mapped = new LinkedHashMap<String, Object>();
        if (inputs != null) {
            mapped.putAll(inputs);
        }
        if (template.getInputMappings() == null || template.getInputMappings().isEmpty()) {
            return mapped;
        }
        for (Map.Entry<String, String> entry : template.getInputMappings().entrySet()) {
            String targetField = DifyWorkflowUtils.string(entry.getKey(), null);
            String sourcePath = DifyWorkflowUtils.string(entry.getValue(), null);
            if (targetField == null || sourcePath == null || DifyWorkflowUtils.hasValue(mapped.get(targetField))) {
                continue;
            }
            Object value = DifyWorkflowUtils.readPath(inputs, sourcePath);
            if (value == null) {
                value = DifyWorkflowUtils.readPath(request, sourcePath);
            }
            if (value != null) {
                mapped.put(targetField, value);
            }
        }
        return mapped;
    }

    private List<String> missingRequiredInputs(Map<String, Object> inputs, DifyWorkflowTemplate template) {
        List<String> missing = new ArrayList<String>();
        if (template.getRequiredInputs() == null) {
            return missing;
        }
        for (String field : template.getRequiredInputs()) {
            if (field == null || field.trim().isEmpty()) {
                continue;
            }
            Object value = inputs == null ? null : inputs.get(field);
            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                missing.add(field);
            }
        }
        return missing;
    }
}
