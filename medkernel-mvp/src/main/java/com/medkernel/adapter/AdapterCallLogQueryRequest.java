package com.medkernel.adapter;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 适配器调用日志查询请求 DTO
 */
@Schema(description = "适配器调用日志查询请求")
public class AdapterCallLogQueryRequest {

    @Schema(description = "适配器编码", example = "LIS_ADAPTER")
    private String adapterCode;

    @Schema(description = "查询编码", example = "LAB_RESULT_QUERY")
    private String queryCode;

    @Schema(description = "链路追踪ID", example = "trace-001")
    private String traceId;

    @Schema(description = "调用状态（SUCCESS/ERROR/TIMEOUT）", example = "SUCCESS")
    private String status;

    @Schema(description = "患者ID", example = "P001")
    private String patientId;

    @Schema(description = "返回条数限制", example = "100")
    private Integer limit;

    public AdapterCallLogQueryRequest() {
    }

    public String getAdapterCode() {
        return adapterCode;
    }

    public void setAdapterCode(String adapterCode) {
        this.adapterCode = adapterCode;
    }

    public String getQueryCode() {
        return queryCode;
    }

    public void setQueryCode(String queryCode) {
        this.queryCode = queryCode;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
