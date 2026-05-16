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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DifyService {
    private final DifyProperties properties;
    private final EnginePersistenceService persistenceService;
    private final Map<String, DifyWorkflowTemplate> templates = new ConcurrentHashMap<String, DifyWorkflowTemplate>();

    public DifyService(DifyProperties properties, EnginePersistenceService persistenceService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
    }

    public List<DifyWorkflowTemplate> importTemplates(Object request) {
        List<Map<String, Object>> entries = normalizeTemplates(request);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("dify workflow templates list is empty");
        }
        List<String> errors = new ArrayList<String>();
        List<DifyWorkflowTemplate> staged = new ArrayList<DifyWorkflowTemplate>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                staged.add(toTemplate(entries.get(index)));
            } catch (IllegalArgumentException ex) {
                errors.add("templates[" + index + "]: " + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            // 与其他配置导入一致，校验失败整体回退。
            throw new IllegalArgumentException("dify workflow templates invalid: " + errors);
        }

        List<DifyWorkflowTemplate> imported = new ArrayList<DifyWorkflowTemplate>();
        for (DifyWorkflowTemplate template : staged) {
            templates.put(key(template.getWorkflowCode(), template.getWorkflowVersion()), template);
            imported.add(template);
        }
        return imported;
    }

    public List<DifyWorkflowTemplate> listTemplates() {
        List<DifyWorkflowTemplate> list = new ArrayList<DifyWorkflowTemplate>(templates.values());
        Collections.sort(list, new Comparator<DifyWorkflowTemplate>() {
            @Override
            public int compare(DifyWorkflowTemplate left, DifyWorkflowTemplate right) {
                int byCode = left.getWorkflowCode().compareTo(right.getWorkflowCode());
                return byCode != 0 ? byCode : left.getWorkflowVersion().compareTo(right.getWorkflowVersion());
            }
        });
        return list;
    }

    public DifyWorkflowTemplate getTemplate(String workflowCode, String workflowVersion) {
        if (workflowVersion != null && !workflowVersion.trim().isEmpty()) {
            DifyWorkflowTemplate template = templates.get(key(workflowCode, workflowVersion));
            if (template == null) {
                throw new IllegalArgumentException("dify workflow template not found: " + workflowCode + "@" + workflowVersion);
            }
            return template;
        }
        DifyWorkflowTemplate latest = null;
        for (DifyWorkflowTemplate template : templates.values()) {
            if (workflowCode.equals(template.getWorkflowCode())) {
                if (latest == null || template.getWorkflowVersion().compareTo(latest.getWorkflowVersion()) > 0) {
                    latest = template;
                }
            }
        }
        if (latest == null) {
            throw new IllegalArgumentException("dify workflow template not found: " + workflowCode);
        }
        return latest;
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

        DifyWorkflowTemplate template = findTemplate(workflowCode, workflowVersion);
        if (template != null) {
            // 模板存在时按 input_defaults 填充缺省值，并校验 required_inputs。版本号也同步用模板里的。
            if (workflowVersion == null) {
                workflowVersion = template.getWorkflowVersion();
            }
            inputs = applyInputDefaults(inputs, template);
            List<String> missing = missingRequiredInputs(inputs, template);
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("dify workflow inputs missing required fields: " + missing);
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
            difyRequest.put("user", string(request.get("user"), string(properties.getUser(), "zy-engine-mvp")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + properties.getApiKey());

            RestTemplate restTemplate = restTemplate(template);
            ResponseEntity<Map> response = restTemplate.postForEntity(workflowUrl(),
                    new HttpEntity<Map<String, Object>>(difyRequest, headers), Map.class);
            Map<String, Object> body = response.getBody() == null
                    ? new LinkedHashMap<String, Object>() : response.getBody();
            Map<String, Object> result = success(workflowCode, workflowVersion, body, System.currentTimeMillis() - start);
            audit(workflowCode, workflowVersion, inputs, result);
            return result;
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
        Map<String, Object> outputs = new LinkedHashMap<String, Object>();
        if (template != null && template.getDegradedOutputs() != null && !template.getDegradedOutputs().isEmpty()) {
            outputs.putAll(template.getDegradedOutputs());
        } else {
            outputs.put("explanation", "已基于规则和图谱证据生成降级解释，请医生结合病历确认。");
            outputs.put("recommended_action", "保留路径推荐与人工确认，待Dify恢复后可补充更完整说明。");
        }
        if (!outputs.containsKey("target_code")) {
            outputs.put("target_code", string(inputs.get("target_code"), string(inputs.get("pathway_code"), null)));
        }

        Map<String, Object> result = baseResult(workflowCode, workflowVersion, elapsedMs);
        result.put("status", "DEGRADED");
        result.put("provider", "LOCAL_FALLBACK");
        result.put("message", message);
        result.put("error_code", errorCode);
        result.put("outputs", outputs);
        result.put("template_applied", template != null);
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

    private RestTemplate restTemplate(DifyWorkflowTemplate template) {
        int timeout = template != null && template.getTimeoutMs() != null && template.getTimeoutMs() > 0
                ? template.getTimeoutMs() : properties.getTimeoutMs();
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

    private DifyWorkflowTemplate findTemplate(String workflowCode, String workflowVersion) {
        if (workflowCode == null || templates.isEmpty()) {
            return null;
        }
        if (workflowVersion != null && !workflowVersion.trim().isEmpty()) {
            return templates.get(key(workflowCode, workflowVersion));
        }
        DifyWorkflowTemplate latest = null;
        for (DifyWorkflowTemplate template : templates.values()) {
            if (workflowCode.equals(template.getWorkflowCode())) {
                if (latest == null || template.getWorkflowVersion().compareTo(latest.getWorkflowVersion()) > 0) {
                    latest = template;
                }
            }
        }
        return latest;
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeTemplates(Object request) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (request instanceof List) {
            for (Object item : (List<?>) request) {
                if (item instanceof Map) {
                    list.add((Map<String, Object>) item);
                }
            }
            return list;
        }
        if (request instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) request;
            Object nested = map.get("templates");
            if (nested instanceof List) {
                return normalizeTemplates(nested);
            }
            if (map.containsKey("workflow_code")) {
                list.add(map);
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private DifyWorkflowTemplate toTemplate(Map<String, Object> entry) {
        DifyWorkflowTemplate template = new DifyWorkflowTemplate();
        template.setWorkflowCode(requireField(entry, "workflow_code"));
        template.setWorkflowName(string(entry.get("workflow_name"), template.getWorkflowCode()));
        template.setWorkflowVersion(string(entry.get("workflow_version"), "1.0.0"));
        template.setDescription(string(entry.get("description"), null));
        template.setDifyAppCode(string(entry.get("dify_app_code"), null));
        Object timeout = entry.get("timeout_ms");
        if (timeout instanceof Number) {
            template.setTimeoutMs(((Number) timeout).intValue());
        }

        Object defaults = entry.get("input_defaults");
        if (defaults instanceof Map) {
            template.setInputDefaults(new LinkedHashMap<String, Object>((Map<String, Object>) defaults));
        }
        Object required = entry.get("required_inputs");
        if (required instanceof Collection) {
            List<String> requiredList = new ArrayList<String>();
            for (Object item : (Collection<?>) required) {
                if (item != null) {
                    requiredList.add(String.valueOf(item));
                }
            }
            template.setRequiredInputs(requiredList);
        }
        Object degraded = entry.get("degraded_outputs");
        if (degraded instanceof Map) {
            template.setDegradedOutputs(new LinkedHashMap<String, Object>((Map<String, Object>) degraded));
        }
        return template;
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

    private String requireField(Map<String, Object> entry, String field) {
        String value = string(entry.get(field), null);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String key(String workflowCode, String workflowVersion) {
        return workflowCode + "::" + (workflowVersion == null ? "1.0.0" : workflowVersion);
    }
}
