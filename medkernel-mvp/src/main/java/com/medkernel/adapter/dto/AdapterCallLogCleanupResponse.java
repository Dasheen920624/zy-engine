package com.medkernel.adapter.dto;

/**
 * 适配器调用日志清理响应 DTO。
 */
public class AdapterCallLogCleanupResponse {

    private int removedCount;
    private int maxAgeHours;

    public static AdapterCallLogCleanupResponse of(int removedCount, int maxAgeHours) {
        AdapterCallLogCleanupResponse resp = new AdapterCallLogCleanupResponse();
        resp.removedCount = removedCount;
        resp.maxAgeHours = maxAgeHours;
        return resp;
    }

    // Getters and Setters
    public int getRemovedCount() { return removedCount; }
    public void setRemovedCount(int removedCount) { this.removedCount = removedCount; }

    public int getMaxAgeHours() { return maxAgeHours; }
    public void setMaxAgeHours(int maxAgeHours) { this.maxAgeHours = maxAgeHours; }
}
