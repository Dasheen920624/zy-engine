package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 样例行数据 DTO：替代 AdapterDefinitionImportRequest 中 sample_rows 的 Map&lt;String, Object&gt;。
 */
@Schema(description = "样例行数据")
public class SampleRow {

    @NotBlank(message = "rowId 不能为空")
    @Schema(description = "行标识", example = "ROW_001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rowId;

    @Schema(description = "列数据（键值对形式，如 diagnosis_code=I21.0）")
    private Map<String, String> columns;

    public String getRowId() { return rowId; }
    public void setRowId(String rowId) { this.rowId = rowId; }
    public Map<String, String> getColumns() { return columns; }
    public void setColumns(Map<String, String> columns) { this.columns = columns; }
}
