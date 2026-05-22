package com.medkernel.datagovernance.entity;

import java.time.LocalDateTime;

import com.medkernel.common.dataclass.DataClass;
import com.medkernel.common.dataclass.DataClassification;
import com.medkernel.common.dataclass.Encrypted;
import com.medkernel.common.dataclass.Encrypted.MaskPolicy;

/**
 * 诊断主数据实体类
 * 对应数据库表：md_diagnosis
 *
 * <p>分级：{@link DataClassification#HEALTH_DATA} —— 诊断名称属健康医疗数据，
 * 适用《个人信息保护法》§28「敏感个人信息」。
 */
@DataClass(DataClassification.HEALTH_DATA)
public class DiagnosisEntity {
    private Long id;
    private String tenantId;
    private String diagnosisCode;
    @Encrypted(maskPolicy = MaskPolicy.FULL)
    private String diagnosisName;
    private String standardCode;
    private String standardSystem;
    private String category;
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

    public String getDiagnosisCode() {
        return diagnosisCode;
    }

    public void setDiagnosisCode(String diagnosisCode) {
        this.diagnosisCode = diagnosisCode;
    }

    public String getDiagnosisName() {
        return diagnosisName;
    }

    public void setDiagnosisName(String diagnosisName) {
        this.diagnosisName = diagnosisName;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }

    public String getStandardSystem() {
        return standardSystem;
    }

    public void setStandardSystem(String standardSystem) {
        this.standardSystem = standardSystem;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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