package com.medkernel.adapter.entity;

import com.medkernel.persistence.Ids;

import java.time.LocalDateTime;

/**
 * 适配器标准映射实体
 * 对应表：adp_standard_mapping
 */
public class AdapterStandardMappingEntity {

    private Long id;
    private String tenantId;
    private String hospitalCode;
    private String adapterCode;
    private String sourceField;
    private String targetStandard;
    private String targetField;
    private String mappingRule;
    private String transformFunction;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public AdapterStandardMappingEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getTargetStandard() {
        return targetStandard;
    }

    public void setTargetStandard(String targetStandard) {
        this.targetStandard = targetStandard;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }

    public String getMappingRule() {
        return mappingRule;
    }

    public void setMappingRule(String mappingRule) {
        this.mappingRule = mappingRule;
    }

    public String getTransformFunction() {
        return transformFunction;
    }

    public void setTransformFunction(String transformFunction) {
        this.transformFunction = transformFunction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
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
        if (this.status == null) {
            this.status = "ACTIVE";
        }
        if (this.createdTime == null) {
            this.createdTime = LocalDateTime.now();
        }
        this.updatedTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "AdapterStandardMappingEntity{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", hospitalCode='" + hospitalCode + '\'' +
                ", adapterCode='" + adapterCode + '\'' +
                ", sourceField='" + sourceField + '\'' +
                ", targetStandard='" + targetStandard + '\'' +
                ", targetField='" + targetField + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}