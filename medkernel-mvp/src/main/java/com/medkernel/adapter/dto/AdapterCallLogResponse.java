package com.medkernel.adapter.dto;

import com.medkernel.adapter.entity.AdapterCallLogEntity;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 适配器调用日志响应 DTO。
 */
@Schema(description = "适配器调用日志响应")
public class AdapterCallLogResponse {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "链路追踪ID", example = "trace-001")
    private String traceId;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "医院编码")
    private String hospitalCode;

    @Schema(description = "适配器编码", example = "LIS_ADAPTER")
    private String adapterCode;

    @Schema(description = "查询编码", example = "LAB_RESULT_QUERY")
    private String queryCode;

    @Schema(description = "请求参数（JSON字符串）")
    private String requestParams;

    @Schema(description = "响应数据（JSON字符串）")
    private String responseData;

    @Schema(description = "调用状态（SUCCESS/ERROR/TIMEOUT）", example = "SUCCESS")
    private String status;

    @Schema(description = "错误编码")
    private String errorCode;

    @Schema(description = "错误消息")
    private String errorMessage;

    @Schema(description = "耗时（毫秒）")
    private Long elapsedMs;

    @Schema(description = "患者ID", example = "P001")
    private String patientId;

    @Schema(description = "就诊ID")
    private String encounterId;

    @Schema(description = "操作者ID")
    private String operatorId;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    public static AdapterCallLogResponse fromEntity(AdapterCallLogEntity entity) {
        AdapterCallLogResponse dto = new AdapterCallLogResponse();
        dto.setId(entity.getId());
        dto.setTraceId(entity.getTraceId());
        dto.setTenantId(entity.getTenantId());
        dto.setHospitalCode(entity.getHospitalCode());
        dto.setAdapterCode(entity.getAdapterCode());
        dto.setQueryCode(entity.getQueryCode());
        dto.setRequestParams(entity.getRequestParams());
        dto.setResponseData(entity.getResponseData());
        dto.setStatus(entity.getStatus());
        dto.setErrorCode(entity.getErrorCode());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setElapsedMs(entity.getElapsedMs());
        dto.setPatientId(entity.getPatientId());
        dto.setEncounterId(entity.getEncounterId());
        dto.setOperatorId(entity.getOperatorId());
        dto.setCreatedTime(entity.getCreatedTime());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getHospitalCode() { return hospitalCode; }
    public void setHospitalCode(String hospitalCode) { this.hospitalCode = hospitalCode; }
    public String getAdapterCode() { return adapterCode; }
    public void setAdapterCode(String adapterCode) { this.adapterCode = adapterCode; }
    public String getQueryCode() { return queryCode; }
    public void setQueryCode(String queryCode) { this.queryCode = queryCode; }
    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
    public String getResponseData() { return responseData; }
    public void setResponseData(String responseData) { this.responseData = responseData; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
