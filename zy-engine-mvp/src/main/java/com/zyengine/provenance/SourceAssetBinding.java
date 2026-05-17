package com.zyengine.provenance;

import java.util.LinkedHashMap;
import java.util.Map;

public class SourceAssetBinding {
    private String tenantId;
    private String bindingId;
    private String assetType;
    private String assetCode;
    private String documentCode;
    private String citationId;
    private String bindingType;
    private String confidence;
    private String description;
    private String createdBy;
    private String createdTime;
    private String updatedTime;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getBindingId() {
        return bindingId;
    }

    public void setBindingId(String bindingId) {
        this.bindingId = bindingId;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getDocumentCode() {
        return documentCode;
    }

    public void setDocumentCode(String documentCode) {
        this.documentCode = documentCode;
    }

    public String getCitationId() {
        return citationId;
    }

    public void setCitationId(String citationId) {
        this.citationId = citationId;
    }

    public String getBindingType() {
        return bindingType;
    }

    public void setBindingType(String bindingType) {
        this.bindingType = bindingType;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("tenant_id", tenantId);
        view.put("binding_id", bindingId);
        view.put("asset_type", assetType);
        view.put("asset_code", assetCode);
        view.put("document_code", documentCode);
        view.put("citation_id", citationId);
        view.put("binding_type", bindingType);
        view.put("confidence", confidence);
        view.put("description", description);
        view.put("created_by", createdBy);
        view.put("created_time", createdTime);
        view.put("updated_time", updatedTime);
        return view;
    }
}
