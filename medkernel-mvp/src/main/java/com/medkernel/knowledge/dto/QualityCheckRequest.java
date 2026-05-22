package com.medkernel.knowledge.dto;

/**
 * 质检请求 DTO。
 */
public class QualityCheckRequest {

    private String assetType;
    private String assetCode;

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }
}
