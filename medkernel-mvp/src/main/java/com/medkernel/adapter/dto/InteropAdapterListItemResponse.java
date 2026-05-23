package com.medkernel.adapter.dto;

import java.util.List;
import java.util.Map;

/**
 * 互联互通适配器列表项响应 DTO。
 */
public class InteropAdapterListItemResponse {
    private String adapterCode;
    private String adapterName;
    private String adapterType;
    private String sourceSystem;
    private String protocol;
    private String queryCode;
    private String queryName;
    private String description;
    private List<String> schema;
    private String source;
    private Boolean hasSampleRows;
    private String hl7MessageType;
    private String fhirResourceType;
    private String dicomSopClass;
    private String hookId;
    private String hookType;
    private String serviceId;
    private String serviceTitle;
    private String usageRequirements;
    private String appId;
    private String appName;
    private String appType;
    private String clientId;
    private String redirectUri;
    private String scope;
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
        dto.setHookId(string(map.get("hook_id")));
        dto.setHookType(string(map.get("hook_type")));
        dto.setServiceId(string(map.get("service_id")));
        dto.setServiceTitle(string(map.get("service_title")));
        dto.setUsageRequirements(string(map.get("usage_requirements")));
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
