package com.medkernel.dto;

import com.medkernel.adapter.dto.SampleRow;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 适配器定义导入请求 DTO：替代 AdapterHubController.importDefinitions 的 Object。
 */
@Schema(description = "适配器定义导入请求")
public class AdapterDefinitionImportRequest {

    @NotEmpty(message = "definitions 列表不能为空")
    @Schema(description = "适配器定义列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AdapterDefinitionItem> definitions;

    public List<AdapterDefinitionItem> getDefinitions() { return definitions; }
    public void setDefinitions(List<AdapterDefinitionItem> definitions) { this.definitions = definitions; }

    /**
     * 单个适配器定义项
     */
    @Schema(description = "适配器定义项")
    public static class AdapterDefinitionItem {

        @NotBlank(message = "adapter_code 不能为空")
        @Schema(description = "适配器编码", example = "HIS_ADAPTER", requiredMode = Schema.RequiredMode.REQUIRED)
        private String adapter_code;

        @Schema(description = "适配器名称", example = "HIS诊断适配器")
        private String adapter_name;

        @Schema(description = "适配器类型", example = "REST")
        private String adapter_type;

        @Schema(description = "来源系统", example = "HIS")
        private String source_system;

        @NotBlank(message = "query_code 不能为空")
        @Schema(description = "查询编码", example = "QUERY_DIAGNOSES", requiredMode = Schema.RequiredMode.REQUIRED)
        private String query_code;

        @Schema(description = "查询名称", example = "查询诊断")
        private String query_name;

        @Schema(description = "描述")
        private String description;

        @Schema(description = "查询结果字段列表")
        private List<String> schema;

        @Valid
        @Schema(description = "样例行数据")
        private List<SampleRow> sample_rows;

        @Schema(description = "来源标识", example = "IMPORTED")
        private String source;

        public String getAdapter_code() { return adapter_code; }
        public void setAdapter_code(String adapter_code) { this.adapter_code = adapter_code; }
        public String getAdapter_name() { return adapter_name; }
        public void setAdapter_name(String adapter_name) { this.adapter_name = adapter_name; }
        public String getAdapter_type() { return adapter_type; }
        public void setAdapter_type(String adapter_type) { this.adapter_type = adapter_type; }
        public String getSource_system() { return source_system; }
        public void setSource_system(String source_system) { this.source_system = source_system; }
        public String getQuery_code() { return query_code; }
        public void setQuery_code(String query_code) { this.query_code = query_code; }
        public String getQuery_name() { return query_name; }
        public void setQuery_name(String query_name) { this.query_name = query_name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getSchema() { return schema; }
        public void setSchema(List<String> schema) { this.schema = schema; }
        public List<SampleRow> getSample_rows() { return sample_rows; }
        public void setSample_rows(List<SampleRow> sample_rows) { this.sample_rows = sample_rows; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}
