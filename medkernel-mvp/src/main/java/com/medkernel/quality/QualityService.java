package com.medkernel.quality;

import com.medkernel.dify.DifyService;
import com.medkernel.pathway.PathwayService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class QualityService {
    private final PathwayService pathwayService;
    private final DifyService difyService;

    public QualityService(PathwayService pathwayService, DifyService difyService) {
        this.pathwayService = pathwayService;
        this.difyService = difyService;
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
