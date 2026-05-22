package com.medkernel.dify.workflow;

import com.medkernel.persistence.EnginePersistenceService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class DifyTemplateManager {

    private final EnginePersistenceService persistenceService;
    private final Map<String, DifyWorkflowTemplate> templates = new ConcurrentHashMap<String, DifyWorkflowTemplate>();

    DifyTemplateManager(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    void rebuildFromPersistence() {
        List<DifyWorkflowTemplate> persisted = persistenceService.listDifyTemplates();
        for (DifyWorkflowTemplate template : persisted) {
            String key = DifyWorkflowUtils.key(template.getWorkflowCode(), template.getWorkflowVersion());
            templates.putIfAbsent(key, template);
        }
    }

    List<DifyWorkflowTemplate> importTemplates(Object request) {
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
            throw new IllegalArgumentException("dify workflow templates invalid: " + errors);
        }

        List<DifyWorkflowTemplate> imported = new ArrayList<DifyWorkflowTemplate>();
        for (DifyWorkflowTemplate template : staged) {
            templates.put(DifyWorkflowUtils.key(template.getWorkflowCode(), template.getWorkflowVersion()), template);
            persistenceService.saveDifyTemplate(template);
            imported.add(template);
        }
        return imported;
    }

    List<DifyWorkflowTemplate> listTemplates() {
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

    DifyWorkflowTemplate getTemplate(String workflowCode, String workflowVersion) {
        if (workflowVersion != null && !workflowVersion.trim().isEmpty()) {
            DifyWorkflowTemplate template = templates.get(DifyWorkflowUtils.key(workflowCode, workflowVersion));
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

    DifyWorkflowTemplate findTemplate(String workflowCode, String workflowVersion) {
        if (workflowCode == null || templates.isEmpty()) {
            return null;
        }
        if (workflowVersion != null && !workflowVersion.trim().isEmpty()) {
            return templates.get(DifyWorkflowUtils.key(workflowCode, workflowVersion));
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
        template.setWorkflowCode(DifyWorkflowUtils.requireField(entry, "workflow_code"));
        template.setWorkflowName(DifyWorkflowUtils.string(entry.get("workflow_name"), template.getWorkflowCode()));
        template.setWorkflowVersion(DifyWorkflowUtils.string(entry.get("workflow_version"), "1.0.0"));
        template.setDescription(DifyWorkflowUtils.string(entry.get("description"), null));
        template.setDifyAppCode(DifyWorkflowUtils.string(entry.get("dify_app_code"), null));
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
        String refDocCode = DifyWorkflowUtils.string(entry.get("reference_document_code"), null);
        template.setReferenceDocumentCode(refDocCode);
        template.setReferenceBindingType(DifyWorkflowUtils.string(entry.get("reference_binding_type"), null));
        return template;
    }
}
