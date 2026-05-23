package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 互联互通适配器列表项响应 DTO。
 */
@Schema(description = "互联互通适配器列表项响应")
public class InteropAdapterListItemResponse {

    @Schema(description = "适配器编码", example = "HIS_HL7_ADAPTER")
    private String adapterCode;

    @Schema(description = "适配器名称", example = "HIS HL7适配器")
    private String adapterName;

    @Schema(description = "适配器类型（HL7/FHIR/CDA/DICOM/IHE/REST）", example = "HL7")
    private String adapterType;

    @Schema(description = "来源系统", example = "HIS")
    private String sourceSystem;

    @Schema(description = "通信协议", example = "MLLP")
    private String protocol;

    @Schema(description = "查询编码", example = "QUERY_PATIENT_INFO")
    private String queryCode;

    @Schema(description = "查询名称", example = "查询患者信息")
    private String queryName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "数据模式字段列表")
    private List<String> schema;

    @Schema(description = "来源", example = "BUILT_IN_INTEROP")
    private String source;

    @Schema(description = "是否有样例行数据")
    private Boolean hasSampleRows;

    @Schema(description = "HL7消息类型", example = "ADT^A01")
    private String hl7MessageType;

    @Schema(description = "FHIR资源类型", example = "Patient")
    private String fhirResourceType;

    @Schema(description = "DICOM SOP Class UID")
    private String dicomSopClass;

    // CDS Hooks 专用字段
    @Schema(description = "CDS Hooks 钩子ID", example = "patient-view")
    private String hookId;

    @Schema(description = "CDS Hooks 钩子类型", example = "patient-view")
    private String hookType;

    @Schema(description = "CDS Hooks 服务ID", example = "cds-alert-service")
    private String serviceId;

    @Schema(description = "CDS Hooks 服务标题", example = "用药提醒服务")
    private String serviceTitle;

    @Schema(description = "CDS Hooks 使用要求")
    private String usageRequirements;

    // SMART on FHIR 专用字段
    @Schema(description = "SMART应用ID", example = "smart-cardiology")
    private String appId;

    @Schema(description = "SMART应用名称", example = "心脏科SMART应用")
    private String appName;

    @Schema(description = "SMART应用类型", example = "SMART_ON_FHIR")
    private String appType;

    @Schema(description = "SMART应用客户端ID", example = "client-cardiology-001")
    private String clientId;

    @Schema(description = "SMART应用重定向URI")
    private String redirectUri;

    @Schema(description = "SMART应用权限范围")
    private String scope;

    @Schema(description = "SMART应用启动URL")
    private String launchUrl;

    @SuppressWarnings("unchecked")
    public static InteropAdapterListItemResponse fromMap(Map<String, Object> map) {
        InteropAdapterListItemResponse dto = new InteropAdapterListItemResponse();
        dto.setAdapterCode(string(map.get("adapter_code")));
        dto.setAdapterName(string(map.get("adapter_name")));
        dto.setAdapterType(string(map.get("adapter_type")));
        dto.setSourceSystem(string(map.get("source_system")));
        dto.setProtocol(string(map.get("protocol")));
        dto.setQueryCode(string(map.get("query_code")));
        dto.setQueryName(string(map.get("query_name")));
        dto.setDescription(string(map.get("description")));
        if (map.get("schema") instanceof List) {
            dto.setSchema((List<String>) map.get("schema"));
        }
        dto.setSource(string(map.get("source")));
        if (map.get("has_sample_rows") instanceof Boolean) {
            dto.setHasSampleRows((Boolean) map.get("has_sample_rows"));
        }
        dto.setHl7MessageType(string(map.get("hl7_message_type")));
        dto.setFhirResourceType(string(map.get("fhir_resource_type")));
        dto.setDicomSopClass(string(map.get("dicom_sop_class")));
        // CDS Hooks 字段
        dto.setHookId(string(map.get("hook_id")));
        dto.setHookType(string(map.get("hook_type")));
        dto.setServiceId(string(map.get("service_id")));
        dto.setServiceTitle(string(map.get("service_title")));
        dto.setUsageRequirements(string(map.get("usage_requirements")));
        // SMART on FHIR 字段
        dto.setAppId(string(map.get("app_id")));
        dto.setAppName(string(map.get("app_name")));
        dto.setAppType(string(map.get("app_type")));
        dto.setClientId(string(map.get("client_id")));
        dto.setRedirectUri(string(map.get("redirect_uri")));
        dto.setScope(string(map.get("scope")));
        dto.setLaunchUrl(string(map.get("launch_url")));
        return dto;
    }

    private static String string(Object value) {
        return value != null ? value.toString() : null;
    }

    public String getAdapterCode() { return adapterCode; }
    public void setAdapterCode(String adapterCode) { this.adapterCode = adapterCode; }
    public String getAdapterName() { return adapterName; }
    public void setAdapterName(String adapterName) { this.adapterName = adapterName; }
    public String getAdapterType() { return adapterType; }
    public void setAdapterType(String adapterType) { this.adapterType = adapterType; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getQueryCode() { return queryCode; }
    public void setQueryCode(String queryCode) { this.queryCode = queryCode; }
    public String getQueryName() { return queryName; }
    public void setQueryName(String queryName) { this.queryName = queryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getSchema() { return schema; }
    public void setSchema(List<String> schema) { this.schema = schema; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Boolean getHasSampleRows() { return hasSampleRows; }
    public void setHasSampleRows(Boolean hasSampleRows) { this.hasSampleRows = hasSampleRows; }
    public String getHl7MessageType() { return hl7MessageType; }
    public void setHl7MessageType(String hl7MessageType) { this.hl7MessageType = hl7MessageType; }
    public String getFhirResourceType() { return fhirResourceType; }
    public void setFhirResourceType(String fhirResourceType) { this.fhirResourceType = fhirResourceType; }
    public String getDicomSopClass() { return dicomSopClass; }
    public void setDicomSopClass(String dicomSopClass) { this.dicomSopClass = dicomSopClass; }
    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }
    public String getHookType() { return hookType; }
    public void setHookType(String hookType) { this.hookType = hookType; }
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getServiceTitle() { return serviceTitle; }
    public void setServiceTitle(String serviceTitle) { this.serviceTitle = serviceTitle; }
    public String getUsageRequirements() { return usageRequirements; }
    public void setUsageRequirements(String usageRequirements) { this.usageRequirements = usageRequirements; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public String getAppType() { return appType; }
    public void setAppType(String appType) { this.appType = appType; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getLaunchUrl() { return launchUrl; }
    public void setLaunchUrl(String launchUrl) { this.launchUrl = launchUrl; }
}
