package com.medkernel.knowledge.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 同步汇总响应 DTO。
 */
@Schema(description = "同步汇总响应")
public class SyncSummaryResponse {

    @Schema(description = "总同步数")
    private int totalSyncs;

    @Schema(description = "成功数")
    private int successCount;

    @Schema(description = "失败数")
    private int failCount;

    @Schema(description = "待处理数")
    private int pendingCount;

    @Schema(description = "最近同步时间")
    private String lastSyncTime;

    public static SyncSummaryResponse fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        SyncSummaryResponse resp = new SyncSummaryResponse();
        resp.totalSyncs = toInt(map.get("total_syncs"));
        resp.successCount = toInt(map.get("success_count"));
        resp.failCount = toInt(map.get("fail_count"));
        resp.pendingCount = toInt(map.get("pending_count"));
        resp.lastSyncTime = map.get("last_sync_time") != null ? String.valueOf(map.get("last_sync_time")) : null;
        return resp;
    }

    private static int toInt(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    // Getters and Setters
    public int getTotalSyncs() { return totalSyncs; }
    public void setTotalSyncs(int totalSyncs) { this.totalSyncs = totalSyncs; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public String getLastSyncTime() { return lastSyncTime; }
    public void setLastSyncTime(String lastSyncTime) { this.lastSyncTime = lastSyncTime; }
}
