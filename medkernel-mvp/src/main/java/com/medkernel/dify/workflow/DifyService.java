package com.medkernel.dify.workflow;

import com.medkernel.common.ErrorCode;
import com.medkernel.common.TraceContext;
import com.medkernel.persistence.EnginePersistenceService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import javax.annotation.PostConstruct;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final int MAX_INVOCATION_RECORDS = 500;

    private final DifyProperties properties;
    private final EnginePersistenceService persistenceService;
    private final Map<String, DifyWorkflowTemplate> templates = new ConcurrentHashMap<String, DifyWorkflowTemplate>();
    private final List<Map<String, Object>> invocationRecords =
            Collections.synchronizedList(new ArrayList<Map<String, Object>>());

    public DifyService(DifyProperties properties, EnginePersistenceService persistenceService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
    }

    @PostConstruct
    public void rebuildFromPersistence() {
        List<DifyWorkflowTemplate> persisted = persistenceService.listDifyTemplates();
        for (DifyWorkflowTemplate template : persisted) {
            String key = key(template.getWorkflowCode(), template.getWorkflowVersion());
            templates.putIfAbsent(key, template);
        }
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
            persistenceService.saveDifyTemplate(template);
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
        Map<String, Object> safeRequest = request == null
                ? new LinkedHashMap<String, Object>() : request;
        long start = System.currentTimeMillis();
        String workflowCode = string(safeRequest.get("workflow_code"), "WF_UNKNOWN");
        String workflowVersion = string(safeRequest.get("workflow_version"), null);
        Map<String, Object> inputs = map(safeRequest.get("inputs"));
        if (inputs.isEmpty()) {
            inputs = new LinkedHashMap<String, Object>(safeRequest);
        }

        DifyWorkflowTemplate template = findTemplate(workflowCode, workflowVersion);
        if (template != null) {
            // 模板存在时按 input_mappings 抽取上下文、按 input_defaults 填充缺省值，并校验 required_inputs。
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
                audit(workflowCode, workflowVersion, inputs, result);
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
            difyRequest.put("user", string(safeRequest.get("user"), string(properties.getUser(), "medkernel-mvp")));

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
                    audit(workflowCode, workflowVersion, inputs, result);
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
            outputs.put("target_code", string(inputs.get("target_code"), string(inputs.get("pathway_code"), null)));
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

    /**
     * 单次 Dify 调用允许的最大超时（30 秒）。
     * 超过此值的配置都会被夹紧，避免恶意配置（如 MEDKERNEL_DIFY_TIMEOUT_MS=999999）耗尽连接池。
     */
    private static final int MAX_TIMEOUT_MS = 30_000;

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

    private void audit(String workflowCode, String workflowVersion, Map<String, Object> inputs, Map<String, Object> result) {
        recordInvocation(workflowCode, workflowVersion, inputs, result);

        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("workflow_version", workflowVersion);
        detail.put("status", result.get("status"));
        detail.put("provider", result.get("provider"));
        detail.put("message", result.get("message"));
        detail.put("error_code", result.get("error_code"));
        detail.put("elapsed_ms", result.get("elapsed_ms"));
        detail.put("attempts", result.get("attempts"));
        detail.put("retry_count", result.get("retry_count"));
        try {
            persistenceService.saveAuditLog("DIFY", "WORKFLOW_RUN", "WORKFLOW", workflowCode,
                    patientId(inputs), encounterId(inputs), string(inputs.get("operator_id"), null), detail);
        } catch (RuntimeException ignored) {
            // 审计写入失败不应影响Dify降级策略。
        }
    }

    public Map<String, Object> summarizeInvocations(Map<String, String> filters) {
        List<Map<String, Object>> records = filterInvocationRecords(filters);
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total_calls", records.size());
        summary.put("success_calls", countStatus(records, "SUCCESS"));
        summary.put("degraded_calls", countStatus(records, "DEGRADED"));
        summary.put("validation_error_calls", countStatus(records, "VALIDATION_ERROR"));
        summary.put("error_calls", countOtherStatuses(records));
        summary.put("average_elapsed_ms", averageElapsed(records));
        summary.put("by_workflow", aggregateByWorkflow(records));
        summary.put("by_status", aggregateByDimension(records, "status"));
        summary.put("by_provider", aggregateByDimension(records, "provider"));
        return summary;
    }

    private void recordInvocation(String workflowCode, String workflowVersion, Map<String, Object> inputs,
                                  Map<String, Object> result) {
        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("workflow_code", workflowCode);
        record.put("workflow_version", workflowVersion);
        record.put("status", result.get("status"));
        record.put("provider", result.get("provider"));
        record.put("error_code", result.get("error_code"));
        record.put("elapsed_ms", result.get("elapsed_ms"));
        record.put("trace_id", result.get("trace_id"));
        record.put("patient_id", patientId(inputs));
        record.put("encounter_id", encounterId(inputs));
        record.put("created_time", nowText());
        synchronized (invocationRecords) {
            invocationRecords.add(record);
            while (invocationRecords.size() > MAX_INVOCATION_RECORDS) {
                invocationRecords.remove(0);
            }
        }
    }

    private List<Map<String, Object>> filterInvocationRecords(Map<String, String> filters) {
        String workflowCode = filterValue(filters, "workflowCode");
        String workflowVersion = filterValue(filters, "workflowVersion");
        String status = filterValue(filters, "status");
        String provider = filterValue(filters, "provider");
        String patientId = filterValue(filters, "patientId");
        String encounterId = filterValue(filters, "encounterId");
        int limit = filterInt(filters, "limit", MAX_INVOCATION_RECORDS);
        if (limit <= 0) {
            limit = MAX_INVOCATION_RECORDS;
        }

        List<Map<String, Object>> snapshot;
        synchronized (invocationRecords) {
            snapshot = new ArrayList<Map<String, Object>>(invocationRecords);
        }
        Collections.reverse(snapshot);

        List<Map<String, Object>> matched = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> record : snapshot) {
            if (workflowCode != null && !workflowCode.equalsIgnoreCase(string(record.get("workflow_code"), null))) {
                continue;
            }
            if (workflowVersion != null && !workflowVersion.equalsIgnoreCase(string(record.get("workflow_version"), null))) {
                continue;
            }
            if (status != null && !status.equalsIgnoreCase(string(record.get("status"), null))) {
                continue;
            }
            if (provider != null && !provider.equalsIgnoreCase(string(record.get("provider"), null))) {
                continue;
            }
            if (patientId != null && !patientId.equals(string(record.get("patient_id"), null))) {
                continue;
            }
            if (encounterId != null && !encounterId.equals(string(record.get("encounter_id"), null))) {
                continue;
            }
            matched.add(record);
            if (matched.size() >= limit) {
                break;
            }
        }
        return matched;
    }

    private int countStatus(List<Map<String, Object>> records, String status) {
        int count = 0;
        for (Map<String, Object> record : records) {
            if (status.equals(record.get("status"))) {
                count++;
            }
        }
        return count;
    }

    private int countOtherStatuses(List<Map<String, Object>> records) {
        int count = 0;
        for (Map<String, Object> record : records) {
            Object status = record.get("status");
            if (!"SUCCESS".equals(status) && !"DEGRADED".equals(status) && !"VALIDATION_ERROR".equals(status)) {
                count++;
            }
        }
        return count;
    }

    private double averageElapsed(List<Map<String, Object>> records) {
        if (records.isEmpty()) {
            return 0.0;
        }
        long total = 0L;
        for (Map<String, Object> record : records) {
            total += longValue(record.get("elapsed_ms"), 0L);
        }
        return Math.round((total * 100.0 / records.size())) / 100.0;
    }

    private List<Map<String, Object>> aggregateByWorkflow(List<Map<String, Object>> records) {
        Map<String, WorkflowAggregate> aggregates = new LinkedHashMap<String, WorkflowAggregate>();
        for (Map<String, Object> record : records) {
            String workflowCode = string(record.get("workflow_code"), "WF_UNKNOWN");
            String workflowVersion = string(record.get("workflow_version"), null);
            String key = workflowCode + "::" + string(workflowVersion, "");
            WorkflowAggregate aggregate = aggregates.get(key);
            if (aggregate == null) {
                aggregate = new WorkflowAggregate(workflowCode, workflowVersion);
                aggregates.put(key, aggregate);
            }
            aggregate.record(string(record.get("status"), "UNKNOWN"), longValue(record.get("elapsed_ms"), 0L));
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (WorkflowAggregate aggregate : aggregates.values()) {
            result.add(aggregate.toView());
        }
        return result;
    }

    private List<Map<String, Object>> aggregateByDimension(List<Map<String, Object>> records, String dimension) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (Map<String, Object> record : records) {
            String key = string(record.get(dimension), "UNKNOWN");
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

    private static class WorkflowAggregate {
        private final String workflowCode;
        private final String workflowVersion;
        private int total;
        private int success;
        private int degraded;
        private int validationError;
        private int error;
        private long totalElapsedMs;

        WorkflowAggregate(String workflowCode, String workflowVersion) {
            this.workflowCode = workflowCode;
            this.workflowVersion = workflowVersion;
        }

        void record(String status, long elapsedMs) {
            total++;
            totalElapsedMs += elapsedMs;
            if ("SUCCESS".equals(status)) {
                success++;
            } else if ("DEGRADED".equals(status)) {
                degraded++;
            } else if ("VALIDATION_ERROR".equals(status)) {
                validationError++;
            } else {
                error++;
            }
        }

        Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("workflow_code", workflowCode);
            view.put("workflow_version", workflowVersion);
            view.put("total_calls", total);
            view.put("success_calls", success);
            view.put("degraded_calls", degraded);
            view.put("validation_error_calls", validationError);
            view.put("error_calls", error);
            view.put("average_elapsed_ms", total == 0 ? 0.0 : Math.round((totalElapsedMs * 100.0 / total)) / 100.0);
            return view;
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
            String targetField = string(entry.getKey(), null);
            String sourcePath = string(entry.getValue(), null);
            if (targetField == null || sourcePath == null || hasValue(mapped.get(targetField))) {
                continue;
            }
            Object value = readPath(inputs, sourcePath);
            if (value == null) {
                value = readPath(request, sourcePath);
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
        Object retryCount = entry.get("retry_count");
        if (retryCount instanceof Number) {
            template.setRetryCount(((Number) retryCount).intValue());
        }

        Object defaults = entry.get("input_defaults");
        if (defaults instanceof Map) {
            template.setInputDefaults(new LinkedHashMap<String, Object>((Map<String, Object>) defaults));
        }
        Object mappings = entry.get("input_mappings");
        if (mappings instanceof Map) {
            Map<String, String> mappingConfig = new LinkedHashMap<String, String>();
            for (Map.Entry<String, Object> mapping : ((Map<String, Object>) mappings).entrySet()) {
                if (mapping.getKey() != null && mapping.getValue() != null) {
                    mappingConfig.put(mapping.getKey(), String.valueOf(mapping.getValue()));
                }
            }
            template.setInputMappings(mappingConfig);
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
        String refDocCode = string(entry.get("reference_document_code"), null);
        template.setReferenceDocumentCode(refDocCode);
        template.setReferenceBindingType(string(entry.get("reference_binding_type"), null));
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

    private int retryCount(DifyWorkflowTemplate template) {
        if (template == null || template.getRetryCount() == null || template.getRetryCount() < 0) {
            return 0;
        }
        return Math.min(template.getRetryCount(), 3);
    }

    @SuppressWarnings("unchecked")
    private Object readPath(Map<String, Object> source, String path) {
        if (source == null || path == null) {
            return null;
        }
        String normalized = path.trim();
        if (normalized.startsWith("$.")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("request.")) {
            normalized = normalized.substring("request.".length());
        }
        if (normalized.startsWith("inputs.")) {
            normalized = normalized.substring("inputs.".length());
        }
        if (normalized.isEmpty()) {
            return null;
        }

        Object current = source;
        for (String token : normalized.split("\\.")) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(token);
            } else if (current instanceof List && isInteger(token)) {
                List<?> list = (List<?>) current;
                int index = Integer.parseInt(token);
                current = index >= 0 && index < list.size() ? list.get(index) : null;
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private boolean hasValue(Object value) {
        return value != null && (!(value instanceof String) || !((String) value).trim().isEmpty());
    }

    private boolean isInteger(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
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

    private long longValue(Object value, long defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }
}
