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
    private boolean mock;
    private String message;
    private long elapsedMs;
    private int rowCount;
    private List<Map<String, Object>> rows;
    private List<String> schema;
    private Map<String, Object> params;
    private List<Map<String, Object>> supportedQueries;

    @SuppressWarnings("unchecked")
    public static AdapterQueryResponse fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        AdapterQueryResponse resp = new AdapterQueryResponse();
        resp.adapterCode = (String) map.get("adapter_code");
        resp.queryCode = (String) map.get("query_code");
        resp.traceId = (String) map.get("trace_id");
        resp.status = (String) map.get("status");
        resp.adapterType = (String) map.get("adapter_type");
        resp.sourceSystem = (String) map.get("source_system");
        resp.queryName = (String) map.get("query_name");
        resp.mock = map.get("mock") instanceof Boolean && (Boolean) map.get("mock");
        resp.message = (String) map.get("message");
        resp.elapsedMs = map.get("elapsed_ms") instanceof Number
                ? ((Number) map.get("elapsed_ms")).longValue() : 0L;
        resp.rowCount = map.get("row_count") instanceof Number
                ? ((Number) map.get("row_count")).intValue() : 0;
        resp.rows = (List<Map<String, Object>>) map.get("rows");
        resp.schema = (List<String>) map.get("schema");
        resp.params = (Map<String, Object>) map.get("params");
        resp.supportedQueries = (List<Map<String, Object>>) map.get("supported_queries");
        return resp;
    }

    // Getters and Setters
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

    public boolean isMock() { return mock; }
    public void setMock(boolean mock) { this.mock = mock; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }

    public List<String> getSchema() { return schema; }
    public void setSchema(List<String> schema) { this.schema = schema; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public List<Map<String, Object>> getSupportedQueries() { return supportedQueries; }
    public void setSupportedQueries(List<Map<String, Object>> supportedQueries) { this.supportedQueries = supportedQueries; }
}
