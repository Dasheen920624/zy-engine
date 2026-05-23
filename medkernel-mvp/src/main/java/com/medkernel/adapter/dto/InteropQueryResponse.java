package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 互联互通适配器查询响应 DTO。
 */
@Schema(description = "互联互通适配器查询响应")
public class InteropQueryResponse {

    @Schema(description = "适配器编码", example = "HIS_HL7_ADAPTER")
    private String adapterCode;

    @Schema(description = "查询编码", example = "QUERY_PATIENT_INFO")
    private String queryCode;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "调用状态（SUCCESS/UNSUPPORTED）", example = "SUCCESS")
    private String status;

    @Schema(description = "适配器类型", example = "HL7")
    private String adapterType;

    @Schema(description = "来源系统", example = "HIS")
    private String sourceSystem;

    @Schema(description = "通信协议", example = "MLLP")
    private String protocol;

    @Schema(description = "查询名称", example = "查询患者信息")
    private String queryName;

    @Schema(description = "是否为模拟数据")
    private Boolean mock;

    @Schema(description = "消息")
    private String message;

    @Schema(description = "耗时（毫秒）")
    private Long elapsedMs;

    @Schema(description = "返回行数")
    private Integer rowCount;

    @Schema(description = "返回数据行")
    private List<Map<String, Object>> rows;

    @Schema(description = "数据模式字段列表")
    private List<String> schema;

    @Schema(description = "请求参数（仅UNSUPPORTED时返回）")
    private Map<String, Object> params;

    @Schema(description = "支持的查询列表（仅UNSUPPORTED时返回）")
    private List<SupportedQueryItem> supportedQueries;

    @Schema(description = "HL7消息类型", example = "ADT^A01")
    private String hl7MessageType;

    @Schema(description = "FHIR资源类型", example = "Patient")
    private String fhirResourceType;

    @Schema(description = "DICOM SOP Class UID")
    private String dicomSopClass;

    // CDS Hooks 专用字段
    @Schema(description = "CDS Hooks 钩子ID", example = "patient-view")
    private String hookId;

    @Schema(description = "CDS Hooks 钩子类型", example = "patient-view")
    private String hookType;

    @Schema(description = "CDS Hooks 服务ID", example = "cds-alert-service")
    private String serviceId;

    @Schema(description = "CDS Hooks 服务标题", example = "用药提醒服务")
    private String serviceTitle;

    @Schema(description = "CDS Hooks 服务描述")
    private String description;

    @Schema(description = "CDS Hooks 决策支持卡片列表")
    private List<Map<String, Object>> cards;

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
        if (map.get("mock") instanceof Boolean) {
            dto.setMock((Boolean) map.get("mock"));
        }
        dto.setMessage(string(map.get("message")));
        if (map.get("elapsed_ms") instanceof Number) {
            dto.setElapsedMs(((Number) map.get("elapsed_ms")).longValue());
        }
        if (map.get("row_count") instanceof Number) {
            dto.setRowCount(((Number) map.get("row_count")).intValue());
        }
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
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) map.get("supported_queries");
            List<SupportedQueryItem> items = new java.util.ArrayList<SupportedQueryItem>();
            for (Map<String, Object> item : rawList) {
                items.add(SupportedQueryItem.fromMap(item));
            }
            dto.setSupportedQueries(items);
        }
        dto.setHl7MessageType(string(map.get("hl7_message_type")));
        dto.setFhirResourceType(string(map.get("fhir_resource_type")));
        dto.setDicomSopClass(string(map.get("dicom_sop_class")));
        // CDS Hooks 字段
        dto.setHookId(string(map.get("hook_id")));
        dto.setHookType(string(map.get("hook_type")));
        dto.setServiceId(string(map.get("service_id")));
        dto.setServiceTitle(string(map.get("service_title")));
        dto.setDescription(string(map.get("description")));
        if (map.get("cards") instanceof List) {
            dto.setCards((List<Map<String, Object>>) map.get("cards"));
        }
        return dto;
    }

    private static String string(Object value) {
        return value != null ? value.toString() : null;
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
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public List<SupportedQueryItem> getSupportedQueries() { return supportedQueries; }
    public void setSupportedQueries(List<SupportedQueryItem> supportedQueries) { this.supportedQueries = supportedQueries; }
    public String getHl7MessageType() { return hl7MessageType; }
    public void setHl7MessageType(String hl7MessageType) { this.hl7MessageType = hl7MessageType; }
    public String getFhirResourceType() { return fhirResourceType; }
    public void setFhirResourceType(String fhirResourceType) { this.fhirResourceType = fhirResourceType; }
    public String getDicomSopClass() { return dicomSopClass; }
    public void setDicomSopClass(String dicomSopClass) { this.dicomSopClass = dicomSopClass; }
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

    /**
     * 支持的查询项。
     */
    @Schema(description = "支持的查询项")
    public static class SupportedQueryItem {

        @Schema(description = "适配器编码")
        private String adapterCode;

        @Schema(description = "适配器名称")
        private String adapterName;

        @Schema(description = "适配器类型")
        private String adapterType;

        @Schema(description = "来源系统")
        private String sourceSystem;

        @Schema(description = "通信协议")
        private String protocol;

        @Schema(description = "查询编码")
        private String queryCode;

        @Schema(description = "查询名称")
        private String queryName;

        public static SupportedQueryItem fromMap(Map<String, Object> map) {
            SupportedQueryItem item = new SupportedQueryItem();
            item.setAdapterCode(string(map.get("adapter_code")));
            item.setAdapterName(string(map.get("adapter_name")));
            item.setAdapterType(string(map.get("adapter_type")));
            item.setSourceSystem(string(map.get("source_system")));
            item.setProtocol(string(map.get("protocol")));
            item.setQueryCode(string(map.get("query_code")));
            item.setQueryName(string(map.get("query_name")));
            return item;
        }

        private static String string(Object value) {
            return value != null ? value.toString() : null;
        }

        public String getAdapterCode() { return adapterCode; }
        public void setAdapterCode(String adapterCode) { this.adapterCode = adapterCode; }
        public String getAdapterName() { return adapterName; }
        public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
        public String getAdapterType() { return adapterType; }
        public void setAdapterType(String adapterType) { this.adapterType = adapterType; }
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
        public String getQueryCode() { return queryCode; }
        public void setQueryCode(String queryCode) { this.queryCode = queryCode; }
        public String getQueryName() { return queryName; }
        public void setQueryName(String queryName) { this.queryName = queryName; }
    }
}
