package com.medkernel.adapter.dto;

/**
 * 适配器调用日志清理响应 DTO。
 */
public class AdapterCallLogCleanupResponse {
    private Integer removedCount;
    private Integer maxAgeHours;

    public static AdapterCallLogCleanupResponse of(int removedCount, int maxAgeHours) {
        AdapterCallLogCleanupResponse dto = new AdapterCallLogCleanupResponse();
        dto.setRemovedCount(removedCount);
        dto.setMaxAgeHours(maxAgeHours);
        return dto;
    }

    public Integer getRemovedCount() { return removedCount; }
    public void setRemovedCount(Integer removedCount) { this.removedCount = removedCount; }
    public Integer getMaxAgeHours() { return maxAgeHours; }
    public void setMaxAgeHours(Integer maxAgeHours) { this.maxAgeHours = maxAgeHours; }
}
