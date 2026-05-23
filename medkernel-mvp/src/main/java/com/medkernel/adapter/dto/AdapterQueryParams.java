package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 适配器查询参数 DTO：替代 AdapterQueryRequest 中的 Map&lt;String, Object&gt; params。
 */
@Schema(description = "适配器查询参数")
public class AdapterQueryParams {

    @NotBlank(message = "queryType 不能为空")
    @Schema(description = "查询类型", example = "PATIENT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String queryType;

    @Schema(description = "过滤条件（键值对形式，如 patient_id=P_AMI_001）")
    private Map<String, String> filters;

    @Schema(description = "分页偏移量", example = "0")
    private Integer paginationOffset;

    @Schema(description = "分页限制条数", example = "20")
    private Integer paginationLimit;

    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    public Map<String, String> getFilters() { return filters; }
    public void setFilters(Map<String, String> filters) { this.filters = filters; }
    public Integer getPaginationOffset() { return paginationOffset; }
    public void setPaginationOffset(Integer paginationOffset) { this.paginationOffset = paginationOffset; }
    public Integer getPaginationLimit() { return paginationLimit; }
    public void setPaginationLimit(Integer paginationLimit) { this.paginationLimit = paginationLimit; }
}
