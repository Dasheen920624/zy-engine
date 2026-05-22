package com.medkernel.adapter;

import java.util.List;
import java.util.Map;

public class AdapterQueryDefinition {
    public String adapterCode;
    public String adapterName;
    public String adapterType;
    public String sourceSystem;
    public String queryCode;
    public String queryName;
    public String description;
    public List<String> schema;
    public List<Map<String, Object>> sampleRows;
    public String source;
}
