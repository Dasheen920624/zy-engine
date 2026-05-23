package com.medkernel.adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 适配器调用日志统计响应 DTO。
 */
@Schema(description = "适配器调用日志统计响应")
public class AdapterCallLogSummaryResponse {

    @Schema(description = "总调用次数")
    private int total;

    @Schema(description = "成功次数")
    private int successCount;

    @Schema(description = "错误次数")
    private int errorCount;

    @Schema(description = "超时次数")
    private int timeoutCount;

    @Schema(description = "平均耗时（毫秒）")
    private double averageElapsedMs;

    @Schema(description = "按适配器统计列表")
    private List<AdapterAggregateItem> byAdapter;

    @Schema(description = "按状态统计列表")
    private List<StatusBucketItem> byStatus;

    @Schema(description = "按医院编码统计列表")
    private List<HospitalCodeBucketItem> byHospitalCode;

    @SuppressWarnings("unchecked")
    public static AdapterCallLogSummaryResponse fromMap(Map<String, Object> map) {
        AdapterCallLogSummaryResponse dto = new AdapterCallLogSummaryResponse();
        if (map.get("total") instanceof Number) {
            dto.setTotal(((Number) map.get("total")).intValue());
        }
        if (map.get("success_count") instanceof Number) {
            dto.setSuccessCount(((Number) map.get("success_count")).intValue());
        }
        if (map.get("error_count") instanceof Number) {
            dto.setErrorCount(((Number) map.get("error_count")).intValue());
        }
        if (map.get("timeout_count") instanceof Number) {
            dto.setTimeoutCount(((Number) map.get("timeout_count")).intValue());
        }
        if (map.get("average_elapsed_ms") instanceof Number) {
            dto.setAverageElapsedMs(((Number) map.get("average_elapsed_ms")).doubleValue());
        }
        if (map.get("by_adapter") instanceof List) {
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) map.get("by_adapter");
            List<AdapterAggregateItem> items = new java.util.ArrayList<AdapterAggregateItem>();
            for (Map<String, Object> item : rawList) {
                items.add(AdapterAggregateItem.fromMap(item));
            }
            dto.setByAdapter(items);
        }
        if (map.get("by_status") instanceof List) {
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) map.get("by_status");
            List<StatusBucketItem> items = new java.util.ArrayList<StatusBucketItem>();
            for (Map<String, Object> item : rawList) {
                items.add(StatusBucketItem.fromMap(item));
            }
            dto.setByStatus(items);
        }
        if (map.get("by_hospital_code") instanceof List) {
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) map.get("by_hospital_code");
            List<HospitalCodeBucketItem> items = new java.util.ArrayList<HospitalCodeBucketItem>();
            for (Map<String, Object> item : rawList) {
                items.add(HospitalCodeBucketItem.fromMap(item));
            }
            dto.setByHospitalCode(items);
        }
        return dto;
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public int getTimeoutCount() { return timeoutCount; }
    public void setTimeoutCount(int timeoutCount) { this.timeoutCount = timeoutCount; }
    public double getAverageElapsedMs() { return averageElapsedMs; }
    public void setAverageElapsedMs(double averageElapsedMs) { this.averageElapsedMs = averageElapsedMs; }
    public List<AdapterAggregateItem> getByAdapter() { return byAdapter; }
    public void setByAdapter(List<AdapterAggregateItem> byAdapter) { this.byAdapter = byAdapter; }
    public List<StatusBucketItem> getByStatus() { return byStatus; }
    public void setByStatus(List<StatusBucketItem> byStatus) { this.byStatus = byStatus; }
    public List<HospitalCodeBucketItem> getByHospitalCode() { return byHospitalCode; }
    public void setByHospitalCode(List<HospitalCodeBucketItem> byHospitalCode) { this.byHospitalCode = byHospitalCode; }

    /**
     * 按适配器统计项。
     */
    @Schema(description = "按适配器统计项")
    public static class AdapterAggregateItem {

        @Schema(description = "适配器编码")
        private String adapterCode;

        @Schema(description = "总调用次数")
        private int total;

        @Schema(description = "成功次数")
        private int successCount;

        @Schema(description = "错误次数")
        private int errorCount;

        @Schema(description = "超时次数")
        private int timeoutCount;

        @Schema(description = "平均耗时（毫秒）")
        private double averageElapsedMs;

        public static AdapterAggregateItem fromMap(Map<String, Object> map) {
            AdapterAggregateItem item = new AdapterAggregateItem();
            item.setAdapterCode(string(map.get("adapter_code")));
            if (map.get("total") instanceof Number) {
                item.setTotal(((Number) map.get("total")).intValue());
            }
            if (map.get("success_count") instanceof Number) {
                item.setSuccessCount(((Number) map.get("success_count")).intValue());
            }
            if (map.get("error_count") instanceof Number) {
                item.setErrorCount(((Number) map.get("error_count")).intValue());
            }
            if (map.get("timeout_count") instanceof Number) {
                item.setTimeoutCount(((Number) map.get("timeout_count")).intValue());
            }
            if (map.get("average_elapsed_ms") instanceof Number) {
                item.setAverageElapsedMs(((Number) map.get("average_elapsed_ms")).doubleValue());
            }
            return item;
        }

        private static String string(Object value) {
            return value != null ? value.toString() : null;
        }

        public String getAdapterCode() { return adapterCode; }
        public void setAdapterCode(String adapterCode) { this.adapterCode = adapterCode; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        public int getTimeoutCount() { return timeoutCount; }
        public void setTimeoutCount(int timeoutCount) { this.timeoutCount = timeoutCount; }
        public double getAverageElapsedMs() { return averageElapsedMs; }
        public void setAverageElapsedMs(double averageElapsedMs) { this.averageElapsedMs = averageElapsedMs; }
    }

    /**
     * 按状态统计项。
     */
    @Schema(description = "按状态统计项")
    public static class StatusBucketItem {

        @Schema(description = "状态")
        private String status;

        @Schema(description = "次数")
        private int count;

        public static StatusBucketItem fromMap(Map<String, Object> map) {
            StatusBucketItem item = new StatusBucketItem();
            item.setStatus(string(map.get("status")));
            if (map.get("count") instanceof Number) {
                item.setCount(((Number) map.get("count")).intValue());
            }
            return item;
        }

        private static String string(Object value) {
            return value != null ? value.toString() : null;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    /**
     * 按医院编码统计项。
     */
    @Schema(description = "按医院编码统计项")
    public static class HospitalCodeBucketItem {

        @Schema(description = "医院编码")
        private String hospitalCode;

        @Schema(description = "次数")
        private int count;

        public static HospitalCodeBucketItem fromMap(Map<String, Object> map) {
            HospitalCodeBucketItem item = new HospitalCodeBucketItem();
            item.setHospitalCode(string(map.get("hospital_code")));
            if (map.get("count") instanceof Number) {
                item.setCount(((Number) map.get("count")).intValue());
            }
            return item;
        }

        private static String string(Object value) {
            return value != null ? value.toString() : null;
        }

        public String getHospitalCode() { return hospitalCode; }
        public void setHospitalCode(String hospitalCode) { this.hospitalCode = hospitalCode; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
