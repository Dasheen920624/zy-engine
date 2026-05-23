package com.medkernel.adapter.dto;

import java.util.List;
import java.util.Map;

/**
 * 适配器调用日志统计响应 DTO。
 */
public class AdapterCallLogSummaryResponse {
    private Integer total;
    private Integer successCount;
    private Integer errorCount;
    private Integer timeoutCount;
    private Double averageElapsedMs;
    private List<Map<String, Object>> byAdapter;
    private List<Map<String, Object>> byStatus;
    private List<Map<String, Object>> byHospitalCode;

    @SuppressWarnings("unchecked")
    public static AdapterCallLogSummaryResponse fromMap(Map<String, Object> map) {
        AdapterCallLogSummaryResponse dto = new AdapterCallLogSummaryResponse();
        dto.setTotal(intValue(map.get("total")));
        dto.setSuccessCount(intValue(map.get("success_count")));
        dto.setErrorCount(intValue(map.get("error_count")));
        dto.setTimeoutCount(intValue(map.get("timeout_count")));
        if (map.get("average_elapsed_ms") instanceof Number) {
            dto.setAverageElapsedMs(((Number) map.get("average_elapsed_ms")).doubleValue());
        }
        if (map.get("by_adapter") instanceof List) {
            dto.setByAdapter((List<Map<String, Object>>) map.get("by_adapter"));
        }
        if (map.get("by_status") instanceof List) {
            dto.setByStatus((List<Map<String, Object>>) map.get("by_status"));
        }
        if (map.get("by_hospital_code") instanceof List) {
            dto.setByHospitalCode((List<Map<String, Object>>) map.get("by_hospital_code"));
        }
        return dto;
    }

    private static int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public Integer getTimeoutCount() { return timeoutCount; }
    public void setTimeoutCount(Integer timeoutCount) { this.timeoutCount = timeoutCount; }
    public Double getAverageElapsedMs() { return averageElapsedMs; }
    public void setAverageElapsedMs(Double averageElapsedMs) { this.averageElapsedMs = averageElapsedMs; }
    public List<Map<String, Object>> getByAdapter() { return byAdapter; }
    public void setByAdapter(List<Map<String, Object>> byAdapter) { this.byAdapter = byAdapter; }
    public List<Map<String, Object>> getByStatus() { return byStatus; }
    public void setByStatus(List<Map<String, Object>> byStatus) { this.byStatus = byStatus; }
    public List<Map<String, Object>> getByHospitalCode() { return byHospitalCode; }
    public void setByHospitalCode(List<Map<String, Object>> byHospitalCode) { this.byHospitalCode = byHospitalCode; }
}
