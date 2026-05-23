package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 适配器调用日志清理响应 DTO。
 */
@Schema(description = "适配器调用日志清理响应")
public class AdapterCallLogCleanupResponse {

    @Schema(description = "已清理的日志条数")
    private int removed;

    @Schema(description = "清理的最大年龄（小时）")
    private int maxAgeHours;

    public static AdapterCallLogCleanupResponse of(int removed, int maxAgeHours) {
        AdapterCallLogCleanupResponse dto = new AdapterCallLogCleanupResponse();
        dto.setRemoved(removed);
        dto.setMaxAgeHours(maxAgeHours);
        return dto;
    }

    public int getRemoved() { return removed; }
    public void setRemoved(int removed) { this.removed = removed; }
    public int getMaxAgeHours() { return maxAgeHours; }
    public void setMaxAgeHours(int maxAgeHours) { this.maxAgeHours = maxAgeHours; }
}
