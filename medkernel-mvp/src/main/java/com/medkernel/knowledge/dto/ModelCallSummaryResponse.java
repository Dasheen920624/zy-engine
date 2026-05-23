package com.medkernel.knowledge.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模型调用汇总响应 DTO。
 */
@Schema(description = "模型调用汇总响应")
public class ModelCallSummaryResponse {

    @Schema(description = "总调用数")
    private int totalCalls;

    @Schema(description = "成功数")
    private int successCount;

    @Schema(description = "失败数")
    private int failCount;

    @Schema(description = "平均耗时(ms)")
    private double avgElapsedMs;

    @Schema(description = "按调用类型统计")
    private Map<String, Integer> byCallType;

    @SuppressWarnings("unchecked")
    public static ModelCallSummaryResponse fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        ModelCallSummaryResponse resp = new ModelCallSummaryResponse();
        resp.totalCalls = toInt(map.get("total_calls"));
        resp.successCount = toInt(map.get("success_count"));
        resp.failCount = toInt(map.get("fail_count"));
        resp.avgElapsedMs = map.get("avg_elapsed_ms") instanceof Number
                ? ((Number) map.get("avg_elapsed_ms")).doubleValue() : 0.0;
        resp.byCallType = (Map<String, Integer>) map.get("by_call_type");
        return resp;
    }

    private static int toInt(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    // Getters and Setters
    public int getTotalCalls() { return totalCalls; }
    public void setTotalCalls(int totalCalls) { this.totalCalls = totalCalls; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }

    public double getAvgElapsedMs() { return avgElapsedMs; }
    public void setAvgElapsedMs(double avgElapsedMs) { this.avgElapsedMs = avgElapsedMs; }

    public Map<String, Integer> getByCallType() { return byCallType; }
    public void setByCallType(Map<String, Integer> byCallType) { this.byCallType = byCallType; }
}
