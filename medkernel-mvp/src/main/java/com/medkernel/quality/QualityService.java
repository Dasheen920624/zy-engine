package com.medkernel.quality;

import com.medkernel.dify.DifyService;
import com.medkernel.pathway.PathwayService;
import com.medkernel.rule.RuleService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class QualityService {
    private final PathwayService pathwayService;
    private final DifyService difyService;
    private final RuleService ruleService;

    private static final AtomicLong ALERT_ID_SEQ = new AtomicLong(1);
    private final Map<String, Map<String, Object>> alertStore = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> assignmentStore = new ConcurrentHashMap<String, Map<String, Object>>();

    public QualityService(PathwayService pathwayService, DifyService difyService, RuleService ruleService) {
        this.pathwayService = pathwayService;
        this.difyService = difyService;
        this.ruleService = ruleService;
    }

    public Map<String, Object> metrics(Map<String, String> filters) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("generated_time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()));
        result.put("pathway_code", filters == null ? null : filters.get("pathwayCode"));
        result.put("instance_summary", pathwayService.summarizeInstances(filters));
        result.put("variation_summary", pathwayService.summarizeVariations(filters));
        result.put("node_completion", pathwayService.summarizeNodeCompletion(filters));
        result.put("node_stay_duration", pathwayService.summarizeNodeStayDuration(filters));
        result.put("dify_workflow_stats", difyService.summarizeInvocations(toDifyFilters(filters)));
        return result;
    }

    public Map<String, Object> listAlerts(Map<String, String> filters) {
        // 从规则执行日志中提取质控相关命中记录
        Map<String, String> ruleFilters = new LinkedHashMap<String, String>();
        ruleFilters.put("scenarioCode", "EMR_QC");
        ruleFilters.put("hitFlag", "true");
        if (filters != null) {
            if (filters.get("dept") != null) ruleFilters.put("departmentCode", filters.get("dept"));
            if (filters.get("severity") != null) ruleFilters.put("severity", filters.get("severity"));
        }

        List<Map<String, Object>> evalResults = ruleService.listEvaluations(ruleFilters);
        List<Map<String, Object>> alerts = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> eval : evalResults) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) eval.get("results");
            if (results == null) continue;
            for (Map<String, Object> hit : results) {
                if (!Boolean.TRUE.equals(hit.get("hit_flag"))) continue;
                Map<String, Object> alert = new LinkedHashMap<String, Object>();
                String alertId = "QA-" + String.format("%05d", ALERT_ID_SEQ.getAndIncrement());
                alert.put("id", alertId);
                alert.put("time", eval.get("created_time"));
                alert.put("patient_id", hit.get("patient_id"));
                alert.put("encounter_id", hit.get("encounter_id"));
                alert.put("doctor", hit.get("operator_id"));
                alert.put("rule_code", hit.get("rule_code"));
                alert.put("rule_name", hit.get("rule_name"));
                alert.put("severity", hit.get("severity"));
                alert.put("message", hit.get("message"));
                alert.put("scenario_code", eval.get("scenario_code"));
                alert.put("dept", eval.get("department_code"));
                alert.put("status", getAlertStatus(alertId));
                alert.put("overtime", isOvertime(alert));
                alerts.add(alert);
            }
        }

        // 筛选
        String severityFilter = filters != null ? filters.get("severity") : null;
        String statusFilter = filters != null ? filters.get("status") : null;
        String deptFilter = filters != null ? filters.get("dept") : null;

        List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> alert : alerts) {
            if (severityFilter != null && !severityFilter.equalsIgnoreCase(string(alert.get("severity"), null))) continue;
            if (statusFilter != null && !statusFilter.equalsIgnoreCase(string(alert.get("status"), "PENDING"))) continue;
            if (deptFilter != null && !deptFilter.equalsIgnoreCase(string(alert.get("dept"), null))) continue;
            filtered.add(alert);
        }

        // 按严重度排序：CRITICAL > WARNING > INFO
        Collections.sort(filtered, (a, b) -> {
            int sa = severityOrder(string(a.get("severity"), "INFO"));
            int sb = severityOrder(string(b.get("severity"), "INFO"));
            return sa - sb;
        });

        // 分页
        int page = filters != null ? filterInt(filters, "page", 1) : 1;
        int size = filters != null ? filterInt(filters, "size", 20) : 20;
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        int total = filtered.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageData = fromIndex < total
                ? filtered.subList(fromIndex, toIndex) : Collections.<Map<String, Object>>emptyList();

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("items", pageData);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("total_pages", (total + size - 1) / size);
        return result;
    }

    public Map<String, Object> alertSummary(Map<String, String> filters) {
        Map<String, Object> result = listAlerts(mergeFilters(filters, "999999", "1"));
        List<Map<String, Object>> allAlerts = (List<Map<String, Object>>) result.get("items");

        int critical = 0, warning = 0, info = 0, overtime = 0;
        for (Map<String, Object> alert : allAlerts) {
            String sev = string(alert.get("severity"), "INFO");
            if ("CRITICAL".equalsIgnoreCase(sev)) critical++;
            else if ("WARNING".equalsIgnoreCase(sev) || "WARN".equalsIgnoreCase(sev)) warning++;
            else info++;
            if (Boolean.TRUE.equals(alert.get("overtime"))) overtime++;
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("critical", critical);
        summary.put("warning", warning);
        summary.put("info", info);
        summary.put("overtime", overtime);
        summary.put("total", critical + warning + info);
        return summary;
    }

    public Map<String, Object> assignProblem(String alertId, Map<String, Object> request) {
        Map<String, Object> assignment = new LinkedHashMap<String, Object>();
        assignment.put("id", "ASGN-" + String.format("%05d", ALERT_ID_SEQ.getAndIncrement()));
        assignment.put("alert_id", alertId);
        assignment.put("assignee", request.get("assignee"));
        assignment.put("assignee_role", request.get("assignee_role"));
        assignment.put("deadline", request.get("deadline"));
        assignment.put("note", request.get("note"));
        assignment.put("assigned_by", request.get("assigned_by"));
        assignment.put("assigned_time", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()));
        assignment.put("status", "ASSIGNED");

        assignmentStore.put(alertId, assignment);

        // 更新预警状态
        Map<String, Object> alert = alertStore.get(alertId);
        if (alert != null) {
            alert.put("status", "IN_PROGRESS");
        }

        return assignment;
    }

    private String getAlertStatus(String alertId) {
        Map<String, Object> assignment = assignmentStore.get(alertId);
        if (assignment != null) {
            return string(assignment.get("status"), "ASSIGNED");
        }
        return "PENDING";
    }

    private boolean isOvertime(Map<String, Object> alert) {
        // 简化逻辑：如果状态为 PENDING 且时间超过 24 小时则标记超时
        String status = string(alert.get("status"), "PENDING");
        if (!"PENDING".equals(status)) return false;
        String timeStr = string(alert.get("time"), null);
        if (timeStr == null) return false;
        try {
            OffsetDateTime alertTime = OffsetDateTime.parse(timeStr);
            return alertTime.isBefore(OffsetDateTime.now().minusHours(24));
        } catch (Exception e) {
            return false;
        }
    }

    private int severityOrder(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity) || "FATAL".equalsIgnoreCase(severity)) return 0;
        if ("WARNING".equalsIgnoreCase(severity) || "WARN".equalsIgnoreCase(severity)) return 1;
        return 2;
    }

    private Map<String, String> mergeFilters(Map<String, String> original, String size, String page) {
        Map<String, String> merged = new LinkedHashMap<String, String>();
        if (original != null) merged.putAll(original);
        merged.put("size", size);
        merged.put("page", page);
        return merged;
    }

    private String string(Object value, String defaultValue) {
        return value != null ? value.toString() : defaultValue;
    }

    private int filterInt(Map<String, String> filters, String key, int defaultValue) {
        String value = filters.get(key);
        if (value == null) return defaultValue;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return defaultValue; }
    }

    private Map<String, String> toDifyFilters(Map<String, String> filters) {
        Map<String, String> difyFilters = new LinkedHashMap<String, String>();
        if (filters == null) {
            return difyFilters;
        }
        difyFilters.put("workflowCode", filters.get("workflowCode"));
        difyFilters.put("workflowVersion", filters.get("workflowVersion"));
        difyFilters.put("status", filters.get("workflowStatus"));
        difyFilters.put("patientId", filters.get("patientId"));
        difyFilters.put("encounterId", filters.get("encounterId"));
        return difyFilters;
    }
}
