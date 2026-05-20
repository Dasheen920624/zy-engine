package com.medkernel.adapter.entity;

import com.medkernel.persistence.Ids;

import java.time.LocalDateTime;

/**
 * CDS Hooks服务配置实体
 * 对应表：adp_cds_hooks_service
 */
public class AdapterCdsHooksServiceEntity {

    private Long id;
    private String tenantId;
    private String hospitalCode;
    private String hookId;
    private String hookType;
    private String serviceId;
    private String serviceTitle;
    private String description;
    private String usageRequirements;
    private String prefetchData;
    private String responseTemplate;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public AdapterCdsHooksServiceEntity() {
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

    public String getHookId() {
        return hookId;
    }

    public void setHookId(String hookId) {
        this.hookId = hookId;
    }

    public String getHookType() {
        return hookType;
    }

    public void setHookType(String hookType) {
        this.hookType = hookType;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceTitle() {
        return serviceTitle;
    }

    public void setServiceTitle(String serviceTitle) {
        this.serviceTitle = serviceTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsageRequirements() {
        return usageRequirements;
    }

    public void setUsageRequirements(String usageRequirements) {
        this.usageRequirements = usageRequirements;
    }

    public String getPrefetchData() {
        return prefetchData;
    }

    public void setPrefetchData(String prefetchData) {
        this.prefetchData = prefetchData;
    }

    public String getResponseTemplate() {
        return responseTemplate;
    }

    public void setResponseTemplate(String responseTemplate) {
        this.responseTemplate = responseTemplate;
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
        return "AdapterCdsHooksServiceEntity{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", hospitalCode='" + hospitalCode + '\'' +
                ", hookId='" + hookId + '\'' +
                ", hookType='" + hookType + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", serviceTitle='" + serviceTitle + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}