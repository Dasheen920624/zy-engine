package com.medkernel.adapter.dto;

import java.util.List;
import java.util.Map;

/**
 * 适配器定义响应 DTO。
 */
public class AdapterDefinitionResponse {

    private String adapterCode;
    private String adapterName;
    private String adapterType;
    private String sourceSystem;
    private String queryCode;
    private String queryName;
    private String description;
    private List<String> schema;
    private String source;
    private boolean hasSampleRows;

    @SuppressWarnings("unchecked")
    public static AdapterDefinitionResponse fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        AdapterDefinitionResponse resp = new AdapterDefinitionResponse();
        resp.adapterCode = (String) map.get("adapter_code");
        resp.adapterName = (String) map.get("adapter_name");
        resp.adapterType = (String) map.get("adapter_type");
        resp.sourceSystem = (String) map.get("source_system");
        resp.queryCode = (String) map.get("query_code");
        resp.queryName = (String) map.get("query_name");
        resp.description = (String) map.get("description");
        resp.schema = (List<String>) map.get("schema");
        resp.source = (String) map.get("source");
        resp.hasSampleRows = map.get("has_sample_rows") instanceof Boolean && (Boolean) map.get("has_sample_rows");
        return resp;
    }

    // Getters and Setters
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

    public boolean isHasSampleRows() { return hasSampleRows; }
    public void setHasSampleRows(boolean hasSampleRows) { this.hasSampleRows = hasSampleRows; }
}
