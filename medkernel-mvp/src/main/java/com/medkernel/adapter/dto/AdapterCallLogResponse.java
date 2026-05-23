package com.medkernel.adapter.dto;

import com.medkernel.adapter.entity.AdapterCallLogEntity;

import java.time.LocalDateTime;

/**
 * 适配器调用日志响应 DTO。
 */
public class AdapterCallLogResponse {
    private Long id;
    private String traceId;
    private String tenantId;
    private String hospitalCode;
    private String adapterCode;
    private String queryCode;
    private String status;
    private String errorCode;
    private String errorMessage;
    private Long elapsedMs;
    private String patientId;
    private String encounterId;
    private String operatorId;
    private LocalDateTime createdTime;

    public static AdapterCallLogResponse fromEntity(AdapterCallLogEntity entity) {
        AdapterCallLogResponse dto = new AdapterCallLogResponse();
        dto.setId(entity.getId());
        dto.setTraceId(entity.getTraceId());
        dto.setTenantId(entity.getTenantId());
        dto.setHospitalCode(entity.getHospitalCode());
        dto.setAdapterCode(entity.getAdapterCode());
        dto.setQueryCode(entity.getQueryCode());
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
