package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 适配器定义响应 DTO。
 */
@Schema(description = "适配器定义响应")
public class AdapterDefinitionResponse {

    @Schema(description = "适配器编码", example = "LIS_ADAPTER")
    private String adapterCode;

    @Schema(description = "适配器名称", example = "LIS适配器")
    private String adapterName;

    @Schema(description = "适配器类型", example = "REST")
    private String adapterType;

    @Schema(description = "来源系统", example = "LIS")
    private String sourceSystem;

    @Schema(description = "查询编码", example = "LAB_RESULT_QUERY")
    private String queryCode;

    @Schema(description = "查询名称", example = "查询检验结果")
    private String queryName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "数据模式字段列表")
    private List<String> schema;

    @Schema(description = "来源", example = "IMPORTED")
    private String source;

    @Schema(description = "是否有样例行数据")
    private Boolean hasSampleRows;

    @SuppressWarnings("unchecked")
    public static AdapterDefinitionResponse fromMap(Map<String, Object> map) {
        AdapterDefinitionResponse dto = new AdapterDefinitionResponse();
        dto.setAdapterCode(string(map.get("adapter_code")));
        dto.setAdapterName(string(map.get("adapter_name")));
        dto.setAdapterType(string(map.get("adapter_type")));
        dto.setSourceSystem(string(map.get("source_system")));
        dto.setQueryCode(string(map.get("query_code")));
        dto.setQueryName(string(map.get("query_name")));
        dto.setDescription(string(map.get("description")));
        if (map.get("schema") instanceof List) {
            dto.setSchema((List<String>) map.get("schema"));
        }
        dto.setSource(string(map.get("source")));
        if (map.get("has_sample_rows") instanceof Boolean) {
            dto.setHasSampleRows((Boolean) map.get("has_sample_rows"));
        }
        return dto;
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
    public String getQueryCode() { return queryCode; }
    public void setQueryCode(String queryCode) { this.queryCode = queryCode; }
    public String getQueryName() { return queryName; }
    public void setQueryName(String queryName) { this.queryName = queryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getSchema() { return schema; }
    public void setSchema(List<String> schema) { this.schema = schema; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Boolean getHasSampleRows() { return hasSampleRows; }
    public void setHasSampleRows(Boolean hasSampleRows) { this.hasSampleRows = hasSampleRows; }
}
