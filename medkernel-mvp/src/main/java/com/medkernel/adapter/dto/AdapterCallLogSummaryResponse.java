package com.medkernel.adapter.dto;

import java.util.List;
import java.util.Map;

/**
 * 适配器调用日志汇总响应 DTO。
 */
public class AdapterCallLogSummaryResponse {

    private int total;
    private int successCount;
    private int errorCount;
    private int timeoutCount;
    private double averageElapsedMs;
    private List<Map<String, Object>> byAdapter;
    private List<Map<String, Object>> byStatus;
    private List<Map<String, Object>> byHospitalCode;

    @SuppressWarnings("unchecked")
    public static AdapterCallLogSummaryResponse fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        AdapterCallLogSummaryResponse resp = new AdapterCallLogSummaryResponse();
        resp.total = toInt(map.get("total"));
        resp.successCount = toInt(map.get("success_count"));
        resp.errorCount = toInt(map.get("error_count"));
        resp.timeoutCount = toInt(map.get("timeout_count"));
        resp.averageElapsedMs = map.get("average_elapsed_ms") instanceof Number
                ? ((Number) map.get("average_elapsed_ms")).doubleValue() : 0.0;
        resp.byAdapter = (List<Map<String, Object>>) map.get("by_adapter");
        resp.byStatus = (List<Map<String, Object>>) map.get("by_status");
        resp.byHospitalCode = (List<Map<String, Object>>) map.get("by_hospital_code");
        return resp;
    }

    private static int toInt(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    // Getters and Setters
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public int getTimeoutCount() { return timeoutCount; }
    public void setTimeoutCount(int timeoutCount) { this.timeoutCount = timeoutCount; }

    public double getAverageElapsedMs() { return averageElapsedMs; }
    public void setAverageElapsedMs(double averageElapsedMs) { this.averageElapsedMs = averageElapsedMs; }

    public List<Map<String, Object>> getByAdapter() { return byAdapter; }
    public void setByAdapter(List<Map<String, Object>> byAdapter) { this.byAdapter = byAdapter; }

    public List<Map<String, Object>> getByStatus() { return byStatus; }
    public void setByStatus(List<Map<String, Object>> byStatus) { this.byStatus = byStatus; }

    public List<Map<String, Object>> getByHospitalCode() { return byHospitalCode; }
    public void setByHospitalCode(List<Map<String, Object>> byHospitalCode) { this.byHospitalCode = byHospitalCode; }
}
