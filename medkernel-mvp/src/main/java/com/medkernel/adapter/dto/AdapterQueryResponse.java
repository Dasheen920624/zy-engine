package com.medkernel.adapter.dto;

import java.util.List;
import java.util.Map;

/**
 * 适配器查询响应 DTO。
 */
public class AdapterQueryResponse {
    private String adapterCode;
    private String queryCode;
    private String traceId;
    private String status;
    private String adapterType;
    private String sourceSystem;
    private String queryName;
    private Boolean mock;
    private String message;
    private Long elapsedMs;
    private Integer rowCount;
    private List<Map<String, Object>> rows;
    private List<String> schema;
    private Map<String, Object> params;
    private List<Map<String, Object>> supportedQueries;

    @SuppressWarnings("unchecked")
    public static AdapterQueryResponse fromMap(Map<String, Object> map) {
        AdapterQueryResponse dto = new AdapterQueryResponse();
        dto.setAdapterCode(string(map.get("adapter_code")));
        dto.setQueryCode(string(map.get("query_code")));
        dto.setTraceId(string(map.get("trace_id")));
        dto.setStatus(string(map.get("status")));
        dto.setAdapterType(string(map.get("adapter_type")));
        dto.setSourceSystem(string(map.get("source_system")));
        dto.setQueryName(string(map.get("query_name")));
        dto.setMock(booleanValue(map.get("mock")));
        dto.setMessage(string(map.get("message")));
        dto.setElapsedMs(longValue(map.get("elapsed_ms")));
        dto.setRowCount(intValue(map.get("row_count")));
        if (map.get("rows") instanceof List) {
            dto.setRows((List<Map<String, Object>>) map.get("rows"));
        }
        if (map.get("schema") instanceof List) {
            dto.setSchema((List<String>) map.get("schema"));
        }
        if (map.get("params") instanceof Map) {
            dto.setParams((Map<String, Object>) map.get("params"));
        }
        if (map.get("supported_queries") instanceof List) {
            dto.setSupportedQueries((List<Map<String, Object>>) map.get("supported_queries"));
        }
        return dto;
    }

    private static String string(Object value) {
        return value != null ? value.toString() : null;
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return false;
    }

    private static long longValue(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try { return Long.parseLong((String) value); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private static int intValue(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    public String getAdapterCode() { return adapterCode; }
    public void setAdapterCode(String adapterCode) { this.adapterCode = adapterCode; }
    public String getQueryCode() { return queryCode; }
    public void setQueryCode(String queryCode) { this.queryCode = queryCode; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAdapterType() { return adapterType; }
    public void setAdapterType(String adapterType) { this.adapterType = adapterType; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getQueryName() { return queryName; }
    public void setQueryName(String queryName) { this.queryName = queryName; }
    public Boolean getMock() { return mock; }
    public void setMock(Boolean mock) { this.mock = mock; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
    public List<String> getSchema() { return schema; }
    public void setSchema(List<String> schema) { this.schema = schema; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public List<Map<String, Object>> getSupportedQueries() { return supportedQueries; }
    public void setSupportedQueries(List<Map<String, Object>> supportedQueries) { this.supportedQueries = supportedQueries; }
}
