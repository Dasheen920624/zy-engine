package com.medkernel.dify.workflow;

import com.medkernel.persistence.EnginePersistenceService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DifyInvocationRecorder {

    private static final int MAX_INVOCATION_RECORDS = 500;

    private final EnginePersistenceService persistenceService;
    private final List<Map<String, Object>> invocationRecords =
            Collections.synchronizedList(new ArrayList<Map<String, Object>>());

    DifyInvocationRecorder(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    void audit(String workflowCode, String workflowVersion, Map<String, Object> inputs, Map<String, Object> result) {
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
                    DifyWorkflowUtils.patientId(inputs), DifyWorkflowUtils.encounterId(inputs),
                    DifyWorkflowUtils.string(inputs.get("operator_id"), null), detail);
        } catch (RuntimeException ignored) {
            // 审计写入失败不应影响Dify降级策略。
        }
    }

    Map<String, Object> summarizeInvocations(Map<String, String> filters) {
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
        record.put("patient_id", DifyWorkflowUtils.patientId(inputs));
        record.put("encounter_id", DifyWorkflowUtils.encounterId(inputs));
        record.put("created_time", DifyWorkflowUtils.nowText());
        synchronized (invocationRecords) {
            invocationRecords.add(record);
            while (invocationRecords.size() > MAX_INVOCATION_RECORDS) {
                invocationRecords.remove(0);
            }
        }
    }

    private List<Map<String, Object>> filterInvocationRecords(Map<String, String> filters) {
        String workflowCode = DifyWorkflowUtils.filterValue(filters, "workflowCode");
        String workflowVersion = DifyWorkflowUtils.filterValue(filters, "workflowVersion");
        String status = DifyWorkflowUtils.filterValue(filters, "status");
        String provider = DifyWorkflowUtils.filterValue(filters, "provider");
        String patientId = DifyWorkflowUtils.filterValue(filters, "patientId");
        String encounterId = DifyWorkflowUtils.filterValue(filters, "encounterId");
        int limit = DifyWorkflowUtils.filterInt(filters, "limit", MAX_INVOCATION_RECORDS);
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
            if (workflowCode != null && !workflowCode.equalsIgnoreCase(DifyWorkflowUtils.string(record.get("workflow_code"), null))) {
                continue;
            }
            if (workflowVersion != null && !workflowVersion.equalsIgnoreCase(DifyWorkflowUtils.string(record.get("workflow_version"), null))) {
                continue;
            }
            if (status != null && !status.equalsIgnoreCase(DifyWorkflowUtils.string(record.get("status"), null))) {
                continue;
            }
            if (provider != null && !provider.equalsIgnoreCase(DifyWorkflowUtils.string(record.get("provider"), null))) {
                continue;
            }
            if (patientId != null && !patientId.equals(DifyWorkflowUtils.string(record.get("patient_id"), null))) {
                continue;
            }
            if (encounterId != null && !encounterId.equals(DifyWorkflowUtils.string(record.get("encounter_id"), null))) {
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
            total += DifyWorkflowUtils.longValue(record.get("elapsed_ms"), 0L);
        }
        return Math.round((total * 100.0 / records.size())) / 100.0;
    }

    private List<Map<String, Object>> aggregateByWorkflow(List<Map<String, Object>> records) {
        Map<String, WorkflowAggregate> aggregates = new LinkedHashMap<String, WorkflowAggregate>();
        for (Map<String, Object> record : records) {
            String workflowCode = DifyWorkflowUtils.string(record.get("workflow_code"), "WF_UNKNOWN");
            String workflowVersion = DifyWorkflowUtils.string(record.get("workflow_version"), null);
            String key = workflowCode + "::" + DifyWorkflowUtils.string(workflowVersion, "");
            WorkflowAggregate aggregate = aggregates.get(key);
            if (aggregate == null) {
                aggregate = new WorkflowAggregate(workflowCode, workflowVersion);
                aggregates.put(key, aggregate);
            }
            aggregate.record(DifyWorkflowUtils.string(record.get("status"), "UNKNOWN"),
                    DifyWorkflowUtils.longValue(record.get("elapsed_ms"), 0L));
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
            String key = DifyWorkflowUtils.string(record.get(dimension), "UNKNOWN");
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
}
