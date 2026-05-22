package com.medkernel.knowledge.dto;

/**
 * 同步知识包请求 DTO。
 */
public class SyncPackageRequest {

    private String syncMode;

    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
}
