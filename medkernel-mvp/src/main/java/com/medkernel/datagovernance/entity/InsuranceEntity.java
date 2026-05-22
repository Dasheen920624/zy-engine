package com.medkernel.datagovernance.entity;

import java.time.LocalDateTime;

import com.medkernel.common.dataclass.DataClass;
import com.medkernel.common.dataclass.DataClassification;

/**
 * 医保主数据实体类
 * 对应数据库表：md_insurance
 *
 * <p>分级：{@link DataClassification#SENSITIVE} —— 医保信息含个人关联，
 * 属敏感个人信息。
 */
@DataClass(DataClassification.SENSITIVE)
public class InsuranceEntity {
    private Long id;
    private String tenantId;
    private String insuranceCode;
    private String insuranceName;
    private String insuranceType;
    private String regionCode;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

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

    public String getInsuranceCode() {
        return insuranceCode;
    }

    public void setInsuranceCode(String insuranceCode) {
        this.insuranceCode = insuranceCode;
    }

    public String getInsuranceName() {
        return insuranceName;
    }

    public void setInsuranceName(String insuranceName) {
        this.insuranceName = insuranceName;
    }

    public String getInsuranceType() {
        return insuranceType;
    }

    public void setInsuranceType(String insuranceType) {
        this.insuranceType = insuranceType;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
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
}