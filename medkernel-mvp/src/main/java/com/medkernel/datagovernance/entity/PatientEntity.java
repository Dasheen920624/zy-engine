package com.medkernel.datagovernance.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.medkernel.common.dataclass.DataClass;
import com.medkernel.common.dataclass.DataClassification;
import com.medkernel.common.dataclass.Encrypted;
import com.medkernel.common.dataclass.Encrypted.MaskPolicy;

/**
 * 患者主数据实体类。
 * 对应数据库表：md_patient
 *
 * <p>分级：{@link DataClassification#HEALTH_DATA} —— 个人健康医疗数据，
 * 适用《个人信息保护法》§28「敏感个人信息」 + 等保 2.0 三级 8.1.4.8 数据存储保密性。
 *
 * <h2>加密字段</h2>
 * <ul>
 *   <li>{@code patientName}：姓名脱敏「张**」</li>
 *   <li>{@code idCardNo}：身份证脱敏 4+4</li>
 *   <li>{@code phone}：手机号脱敏 3+4</li>
 *   <li>{@code address}：地址保留前 6 字符</li>
 * </ul>
 *
 * <h2>不加密字段（解释）</h2>
 * <ul>
 *   <li>{@code id} / {@code patientId} / {@code tenantId}：内部 ID，跨表连接需明文</li>
 *   <li>{@code gender} / {@code birthDate}：去标识化后仍可统计，单独不构成 PII 泄露</li>
 *   <li>{@code status} / {@code createdTime} / {@code updatedTime}：业务元数据</li>
 * </ul>
 */
@DataClass(DataClassification.HEALTH_DATA)
public class PatientEntity {
    private Long id;
    private String tenantId;
    private String patientId;
    @Encrypted(maskPolicy = MaskPolicy.NAME)
    private String patientName;
    private String gender;
    private LocalDate birthDate;
    @Encrypted(maskPolicy = MaskPolicy.ID_CARD)
    private String idCardNo;
    @Encrypted(maskPolicy = MaskPolicy.PHONE)
    private String phone;
    @Encrypted(maskPolicy = MaskPolicy.ADDRESS)
    private String address;
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

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getIdCardNo() {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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