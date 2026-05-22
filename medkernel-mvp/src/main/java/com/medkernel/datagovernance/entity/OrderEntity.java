package com.medkernel.datagovernance.entity;

import java.time.LocalDateTime;

import com.medkernel.common.dataclass.DataClass;
import com.medkernel.common.dataclass.DataClassification;
import com.medkernel.common.dataclass.Encrypted;
import com.medkernel.common.dataclass.Encrypted.MaskPolicy;

/**
 * 医嘱主数据实体类
 * 对应数据库表：md_order
 *
 * <p>分级：{@link DataClassification#HEALTH_DATA} —— 医嘱名称可能含处方/诊疗信息，
 * 属健康医疗数据，适用《个人信息保护法》§28。
 */
@DataClass(DataClassification.HEALTH_DATA)
public class OrderEntity {
    private Long id;
    private String tenantId;
    private String orderCode;
    @Encrypted(maskPolicy = MaskPolicy.FULL)
    private String orderName;
    private String orderType;
    private String standardCode;
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

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
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