package com.medkernel.adapter.entity;

import com.medkernel.common.dataclass.DataClass;
import com.medkernel.common.dataclass.DataClassification;
import com.medkernel.common.dataclass.Encrypted;
import com.medkernel.common.dataclass.Encrypted.MaskPolicy;
import com.medkernel.persistence.Ids;

import java.time.LocalDateTime;

/**
 * 适配器连接配置实体
 * 对应表：adp_connection_config
 *
 * <p>分级：{@link DataClassification#SENSITIVE} —— 配置值可能含数据库密码/API Key，
 * 必须加密存储。
 */
@DataClass(DataClassification.SENSITIVE)
public class AdapterConnectionConfigEntity {

    private Long id;
    private String tenantId;
    private String hospitalCode;
    private String adapterCode;
    private String configKey;
    @Encrypted(maskPolicy = MaskPolicy.FULL)
    private String configValue;
    private String configType;
    private String description;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public AdapterConnectionConfigEntity() {
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

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        if (this.configType == null) {
            this.configType = "STRING";
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
        return "AdapterConnectionConfigEntity{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", hospitalCode='" + hospitalCode + '\'' +
                ", adapterCode='" + adapterCode + '\'' +
                ", configKey='" + configKey + '\'' +
                ", configType='" + configType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}