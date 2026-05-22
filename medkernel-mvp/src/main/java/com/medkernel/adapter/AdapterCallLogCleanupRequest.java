package com.medkernel.adapter;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Min;

/**
 * 适配器调用日志清理请求 DTO
 */
@Schema(description = "适配器调用日志清理请求")
public class AdapterCallLogCleanupRequest {

    @Min(1)
    @Schema(description = "清理超过此小时数的日志，默认168（7天）", example = "168")
    private int maxAgeHours = 168;

    public AdapterCallLogCleanupRequest() {
    }

    public int getMaxAgeHours() {
        return maxAgeHours;
    }

    public void setMaxAgeHours(int maxAgeHours) {
        this.maxAgeHours = maxAgeHours;
    }
}
