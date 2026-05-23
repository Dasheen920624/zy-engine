package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 适配器查询响应 DTO。
 */
@Schema(description = "适配器查询响应")
public class AdapterQueryResponse {

    @Schema(description = "适配器编码", example = "LIS_ADAPTER")
    private String adapterCode;

    @Schema(description = "查询编码", example = "LAB_RESULT_QUERY")
    private String queryCode;

    @Schema(description = "链路追踪ID")
    private String traceId;

    @Schema(description = "调用状态（SUCCESS/UNSUPPORTED）", example = "SUCCESS")
    private String status;

    @Schema(description = "适配器类型", example = "REST")
    private String adapterType;

    @Schema(description = "来源系统", example = "LIS")
    private String sourceSystem;

    @Schema(description = "查询名称", example = "查询检验结果")
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

        @Schema(description = "查询编码")
        private String queryCode;

        @Schema(description = "查询名称")
        private String queryName;

        public static SupportedQueryItem fromMap(Map<String, Object> map) {
            SupportedQueryItem item = new SupportedQueryItem();
            item.setAdapterCode(string(map.get("adapter_code")));
            item.setAdapterName(string(map.get("adapter_name")));
            item.setAdapterType(string(map.get("adapter_type")));
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
        public String getQueryCode() { return queryCode; }
        public void setQueryCode(String queryCode) { this.queryCode = queryCode; }
        public String getQueryName() { return queryName; }
        public void setQueryName(String queryName) { this.queryName = queryName; }
    }
}
