package com.medkernel.adapter.entity;

import com.medkernel.persistence.Ids;

import java.time.LocalDateTime;

/**
 * 适配器调用日志实体
 * 对应表：adp_call_log
 */
public class AdapterCallLogEntity {

    private Long id;
    private String traceId;
    private String tenantId;
    private String hospitalCode;
    private String adapterCode;
    private String queryCode;
    private String requestParams;
    private String responseData;
    private String status;
    private String errorCode;
    private String errorMessage;
    private Long elapsedMs;
    private String patientId;
    private String encounterId;
    private String operatorId;
    private LocalDateTime createdTime;

    public AdapterCallLogEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
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

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    /**
     * 初始化默认值
     */
    public void initDefaults() {
        if (this.id == null) {
            this.id = Ids.next();
        }
        if (this.tenantId == null) {
            this.tenantId = "default";
        }
        if (this.hospitalCode == null) {
            this.hospitalCode = "DEFAULT_HOSPITAL";
        }
        if (this.createdTime == null) {
            this.createdTime = LocalDateTime.now();
        }
    }

    /**
     * 检查调用是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    /**
     * 检查调用是否超时
     */
    public boolean isTimeout() {
        return "TIMEOUT".equals(status);
    }

    @Override
    public String toString() {
        return "AdapterCallLogEntity{" +
                "id=" + id +
                ", traceId='" + traceId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", hospitalCode='" + hospitalCode + '\'' +
                ", adapterCode='" + adapterCode + '\'' +
                ", queryCode='" + queryCode + '\'' +
                ", status='" + status + '\'' +
                ", elapsedMs=" + elapsedMs +
                '}';
    }
}