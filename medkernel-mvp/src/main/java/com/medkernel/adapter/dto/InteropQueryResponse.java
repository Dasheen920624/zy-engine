package com.medkernel.adapter.dto;

import java.util.List;
import java.util.Map;

/**
 * 互联互通适配器查询响应 DTO。
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
    private Boolean mock;
    private String message;
    private Long elapsedMs;
    private Integer rowCount;
    private List<Map<String, Object>> rows;
    private List<String> schema;
    private String hl7MessageType;
    private String fhirResourceType;
    private String dicomSopClass;
    private Map<String, Object> params;
    private List<Map<String, Object>> supportedQueries;
    private String hookId;
    private String hookType;
    private String serviceId;
    private String serviceTitle;
    private String description;
    private List<Map<String, Object>> cards;
    private Map<String, Object> context;
    private String appId;
    private String appName;
    private String appType;
    private String clientId;
    private String redirectUri;
    private String scope;
    private String launchUrl;
    private Map<String, Object> launchContext;

    @SuppressWarnings("unchecked")
    public static InteropQueryResponse fromMap(Map<String, Object> map) {
        InteropQueryResponse dto = new InteropQueryResponse();
        dto.setAdapterCode(string(map.get("adapter_code")));
        dto.setQueryCode(string(map.get("query_code")));
        dto.setTraceId(string(map.get("trace_id")));
        dto.setStatus(string(map.get("status")));
        dto.setAdapterType(string(map.get("adapter_type")));
        dto.setSourceSystem(string(map.get("source_system")));
        dto.setProtocol(string(map.get("protocol")));
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
        dto.setHl7MessageType(string(map.get("hl7_message_type")));
        dto.setFhirResourceType(string(map.get("fhir_resource_type")));
        dto.setDicomSopClass(string(map.get("dicom_sop_class")));
        if (map.get("params") instanceof Map) {
            dto.setParams((Map<String, Object>) map.get("params"));
        }
        if (map.get("supported_queries") instanceof List) {
            dto.setSupportedQueries((List<Map<String, Object>>) map.get("supported_queries"));
        }
        dto.setHookId(string(map.get("hook_id")));
        dto.setHookType(string(map.get("hook_type")));
        dto.setServiceId(string(map.get("service_id")));
        dto.setServiceTitle(string(map.get("service_title")));
        dto.setDescription(string(map.get("description")));
        if (map.get("cards") instanceof List) {
            dto.setCards((List<Map<String, Object>>) map.get("cards"));
        }
        if (map.get("context") instanceof Map) {
            dto.setContext((Map<String, Object>) map.get("context"));
        }
        dto.setAppId(string(map.get("app_id")));
        dto.setAppName(string(map.get("app_name")));
        dto.setAppType(string(map.get("app_type")));
        dto.setClientId(string(map.get("client_id")));
        dto.setRedirectUri(string(map.get("redirect_uri")));
        dto.setScope(string(map.get("scope")));
        dto.setLaunchUrl(string(map.get("launch_url")));
        if (map.get("launch_context") instanceof Map) {
            dto.setLaunchContext((Map<String, Object>) map.get("launch_context"));
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
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
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
    public String getHl7MessageType() { return hl7MessageType; }
    public void setHl7MessageType(String hl7MessageType) { this.hl7MessageType = hl7MessageType; }
    public String getFhirResourceType() { return fhirResourceType; }
    public void setFhirResourceType(String fhirResourceType) { this.fhirResourceType = fhirResourceType; }
    public String getDicomSopClass() { return dicomSopClass; }
    public void setDicomSopClass(String dicomSopClass) { this.dicomSopClass = dicomSopClass; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public List<Map<String, Object>> getSupportedQueries() { return supportedQueries; }
    public void setSupportedQueries(List<Map<String, Object>> supportedQueries) { this.supportedQueries = supportedQueries; }
    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }
    public String getHookType() { return hookType; }
    public void setHookType(String hookType) { this.hookType = hookType; }
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getServiceTitle() { return serviceTitle; }
    public void setServiceTitle(String serviceTitle) { this.serviceTitle = serviceTitle; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Map<String, Object>> getCards() { return cards; }
    public void setCards(List<Map<String, Object>> cards) { this.cards = cards; }
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public String getAppType() { return appType; }
    public void setAppType(String appType) { this.appType = appType; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getLaunchUrl() { return launchUrl; }
    public void setLaunchUrl(String launchUrl) { this.launchUrl = launchUrl; }
    public Map<String, Object> getLaunchContext() { return launchContext; }
    public void setLaunchContext(Map<String, Object> launchContext) { this.launchContext = launchContext; }
}
