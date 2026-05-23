package com.medkernel.adapter.dto;

import java.util.List;
import java.util.Map;

/**
 * 互操作适配器查询响应 DTO。
 */
public class InteropQueryResponse {

    private String adapterCode;
    private String queryCode;
    private String traceId;
    private String status;
    private String adapterType;
    private String sourceSystem;
    private String protocol;
    private String queryName;
    private boolean mock;
    private String message;
    private long elapsedMs;
    private int rowCount;
    private List<Map<String, Object>> rows;
    private List<String> schema;
    private Map<String, Object> params;
    private List<Map<String, Object>> supportedQueries;
    private String hl7MessageType;
    private String fhirResourceType;
    private String dicomSopClass;

    @SuppressWarnings("unchecked")
    public static InteropQueryResponse fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        InteropQueryResponse resp = new InteropQueryResponse();
        resp.adapterCode = (String) map.get("adapter_code");
        resp.queryCode = (String) map.get("query_code");
        resp.traceId = (String) map.get("trace_id");
        resp.status = (String) map.get("status");
        resp.adapterType = (String) map.get("adapter_type");
        resp.sourceSystem = (String) map.get("source_system");
        resp.protocol = (String) map.get("protocol");
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
        resp.hl7MessageType = (String) map.get("hl7_message_type");
        resp.fhirResourceType = (String) map.get("fhir_resource_type");
        resp.dicomSopClass = (String) map.get("dicom_sop_class");
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

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

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

    public String getHl7MessageType() { return hl7MessageType; }
    public void setHl7MessageType(String hl7MessageType) { this.hl7MessageType = hl7MessageType; }

    public String getFhirResourceType() { return fhirResourceType; }
    public void setFhirResourceType(String fhirResourceType) { this.fhirResourceType = fhirResourceType; }

    public String getDicomSopClass() { return dicomSopClass; }
    public void setDicomSopClass(String dicomSopClass) { this.dicomSopClass = dicomSopClass; }
}
