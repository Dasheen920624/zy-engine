package com.medkernel.patientindex.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者主索引实体类
 * 对应数据库表：mpi_patient_index
 */
public class MpiPatientIndexEntity {
    private Long id;
    private String tenantId;
    private String mpiId;
    private String patientName;
    private String patientNameHash;
    private String gender;
    private LocalDate birthDate;
    private String birthDateHash;
    private String idCardNoHash;
    private String phoneHash;
    private String status;
    private String mergeTargetMpiId;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // Getters and Setters
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

    public String getMpiId() {
        return mpiId;
    }

    public void setMpiId(String mpiId) {
        this.mpiId = mpiId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientNameHash() {
        return patientNameHash;
    }

    public void setPatientNameHash(String patientNameHash) {
        this.patientNameHash = patientNameHash;
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

    public String getBirthDateHash() {
        return birthDateHash;
    }

    public void setBirthDateHash(String birthDateHash) {
        this.birthDateHash = birthDateHash;
    }

    public String getIdCardNoHash() {
        return idCardNoHash;
    }

    public void setIdCardNoHash(String idCardNoHash) {
        this.idCardNoHash = idCardNoHash;
    }

    public String getPhoneHash() {
        return phoneHash;
    }

    public void setPhoneHash(String phoneHash) {
        this.phoneHash = phoneHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMergeTargetMpiId() {
        return mergeTargetMpiId;
    }

    public void setMergeTargetMpiId(String mergeTargetMpiId) {
        this.mergeTargetMpiId = mergeTargetMpiId;
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
