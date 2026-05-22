package com.medkernel.datagovernance.entity;

import java.time.LocalDateTime;

import com.medkernel.common.dataclass.DataClass;
import com.medkernel.common.dataclass.DataClassification;
import com.medkernel.common.dataclass.Encrypted;
import com.medkernel.common.dataclass.Encrypted.MaskPolicy;

/**
 * 医生主数据实体类
 * 对应数据库表：md_doctor
 *
 * <p>分级：{@link DataClassification#SENSITIVE} —— 含医生姓名和执业证号，
 * 属个人敏感信息，适用《个人信息保护法》§28。
 */
@DataClass(DataClassification.SENSITIVE)
public class DoctorEntity {
    private Long id;
    private String tenantId;
    private String doctorId;
    @Encrypted(maskPolicy = MaskPolicy.NAME)
    private String doctorName;
    private String gender;
    private String title;
    private String specialtyCode;
    private String departmentCode;
    @Encrypted(maskPolicy = MaskPolicy.ID_CARD)
    private String licenseNo;
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

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSpecialtyCode() {
        return specialtyCode;
    }

    public void setSpecialtyCode(String specialtyCode) {
        this.specialtyCode = specialtyCode;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getLicenseNo() {
        return licenseNo;
    }

    public void setLicenseNo(String licenseNo) {
        this.licenseNo = licenseNo;
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