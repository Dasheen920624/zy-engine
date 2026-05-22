package com.medkernel.adapter;

import com.medkernel.adapter.entity.AdapterCallLogEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 适配器执行日志服务
 * 内存环形缓冲存储适配器调用日志，支持查询、统计和清理
 */
@Service
public class AdapterExecutionLogService {

    private static final int RING_CAPACITY = 500;

    private final Deque<AdapterCallLogEntity> callLogs = new ConcurrentLinkedDeque<AdapterCallLogEntity>();

    /**
     * 记录调用日志到环形缓冲
     */
    public void recordCallLog(AdapterCallLogEntity logEntry) {
        if (logEntry == null) {
            return;
        }
        if (logEntry.getCreatedTime() == null) {
            logEntry.setCreatedTime(LocalDateTime.now());
        }
        callLogs.addLast(logEntry);
        while (callLogs.size() > RING_CAPACITY) {
            callLogs.pollFirst();
        }
    }

    /**
     * 查询日志，支持 adapterCode, queryCode, traceId, status, tenantId, hospitalCode, patientId 过滤，按时间倒序
     */
    public List<AdapterCallLogEntity> listCallLogs(Map<String, String> filters) {
        String adapterCode = filterValue(filters, "adapterCode");
        String queryCode = filterValue(filters, "queryCode");
        String traceId = filterValue(filters, "traceId");
        String status = filterValue(filters, "status");
        String tenantId = filterValue(filters, "tenantId");
        String hospitalCode = filterValue(filters, "hospitalCode");
        String patientId = filterValue(filters, "patientId");
        int limit = filterInt(filters, "limit", 100);
        if (limit <= 0 || limit > RING_CAPACITY) {
            limit = RING_CAPACITY;
        }

        List<AdapterCallLogEntity> matched = new ArrayList<AdapterCallLogEntity>();
        // ConcurrentLinkedDeque 的 descendingIterator 返回最新写入的元素，便于按时间倒序返回
        Iterator<AdapterCallLogEntity> iterator = callLogs.descendingIterator();
        while (iterator.hasNext() && matched.size() < limit) {
            AdapterCallLogEntity entry = iterator.next();
            if (adapterCode != null && !adapterCode.equalsIgnoreCase(entry.getAdapterCode())) {
                continue;
            }
            if (queryCode != null && !queryCode.equalsIgnoreCase(entry.getQueryCode())) {
                continue;
            }
            if (traceId != null && !traceId.equals(entry.getTraceId())) {
                continue;
            }
            if (status != null && !status.equalsIgnoreCase(entry.getStatus())) {
                continue;
            }
            if (tenantId != null && !tenantId.equals(entry.getTenantId())) {
                continue;
            }
            if (hospitalCode != null && !hospitalCode.equals(entry.getHospitalCode())) {
                continue;
            }
            if (patientId != null && !patientId.equals(entry.getPatientId())) {
                continue;
            }
            matched.add(entry);
        }
        return matched;
    }

    /**
     * 获取单条日志（按 traceId 查找）
     */
    public AdapterCallLogEntity getCallLog(String traceId) {
        if (traceId == null) {
            throw new IllegalArgumentException("traceId is required");
        }
        for (AdapterCallLogEntity entry : callLogs) {
            if (traceId.equals(entry.getTraceId())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 统计：total, success_count, error_count, timeout_count, average_elapsed_ms, by_adapter, by_status, by_hospital_code
     */
    public Map<String, Object> summarizeCallLogs(Map<String, String> filters) {
        Map<String, String> effective = new LinkedHashMap<String, String>();
        if (filters != null) {
            effective.putAll(filters);
        }
        effective.put("limit", String.valueOf(Integer.MAX_VALUE));
        List<AdapterCallLogEntity> entries = listCallLogs(effective);

        int total = entries.size();
        int successCount = 0;
        int errorCount = 0;
        int timeoutCount = 0;
        long totalElapsed = 0;

        Map<String, AdapterAggregate> byAdapter = new LinkedHashMap<String, AdapterAggregate>();
        Map<String, Integer> byStatus = new LinkedHashMap<String, Integer>();
        Map<String, Integer> byHospitalCode = new LinkedHashMap<String, Integer>();

        for (AdapterCallLogEntity entry : entries) {
            String entryStatus = entry.getStatus();
            if ("SUCCESS".equalsIgnoreCase(entryStatus)) {
                successCount++;
            } else if ("ERROR".equalsIgnoreCase(entryStatus)) {
                errorCount++;
            } else if ("TIMEOUT".equalsIgnoreCase(entryStatus)) {
                timeoutCount++;
            }
            if (entry.getElapsedMs() != null) {
                totalElapsed += entry.getElapsedMs();
            }
            increment(byStatus, entryStatus);
            increment(byHospitalCode, entry.getHospitalCode());

            String adapterCode = entry.getAdapterCode();
            if (adapterCode != null) {
                AdapterAggregate agg = byAdapter.get(adapterCode);
                if (agg == null) {
                    agg = new AdapterAggregate(adapterCode);
                    byAdapter.put(adapterCode, agg);
                }
                agg.total++;
                if ("SUCCESS".equalsIgnoreCase(entryStatus)) {
                    agg.successCount++;
                }
                if ("ERROR".equalsIgnoreCase(entryStatus)) {
                    agg.errorCount++;
                }
                if ("TIMEOUT".equalsIgnoreCase(entryStatus)) {
                    agg.timeoutCount++;
                }
                if (entry.getElapsedMs() != null) {
                    agg.totalElapsed += entry.getElapsedMs();
                }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total", total);
        summary.put("success_count", successCount);
        summary.put("error_count", errorCount);
        summary.put("timeout_count", timeoutCount);
        summary.put("average_elapsed_ms", total == 0 ? 0.0 : Math.round(totalElapsed * 100.0 / total) / 100.0);
        summary.put("by_adapter", aggregatesToList(byAdapter));
        summary.put("by_status", bucketsToList(byStatus, "status"));
        summary.put("by_hospital_code", bucketsToList(byHospitalCode, "hospital_code"));
        return summary;
    }

    /**
     * 清理超过 maxAgeHours 小时的日志
     */
    public int cleanupOldLogs(int maxAgeHours) {
        if (maxAgeHours <= 0) {
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minus(maxAgeHours, ChronoUnit.HOURS);
        List<AdapterCallLogEntity> toRemove = new ArrayList<AdapterCallLogEntity>();
        for (AdapterCallLogEntity entry : callLogs) {
            if (entry.getCreatedTime() != null && entry.getCreatedTime().isBefore(cutoff)) {
                toRemove.add(entry);
            }
        }
        callLogs.removeAll(toRemove);
        return toRemove.size();
    }

    private void increment(Map<String, Integer> counts, String key) {
        if (key == null) {
            return;
        }
        Integer value = counts.get(key);
        counts.put(key, value == null ? 1 : value + 1);
    }

    private List<Map<String, Object>> aggregatesToList(Map<String, AdapterAggregate> aggregates) {
        List<AdapterAggregate> list = new ArrayList<AdapterAggregate>(aggregates.values());
        Collections.sort(list, new Comparator<AdapterAggregate>() {
            @Override
            public int compare(AdapterAggregate left, AdapterAggregate right) {
                int byTotal = Integer.compare(right.total, left.total);
                return byTotal != 0 ? byTotal : left.adapterCode.compareTo(right.adapterCode);
            }
        });
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (AdapterAggregate agg : list) {
            result.add(agg.toView());
        }
        return result;
    }

    private List<Map<String, Object>> bucketsToList(Map<String, Integer> counts, String key) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
                int byCount = right.getValue().compareTo(left.getValue());
                return byCount != 0 ? byCount : left.getKey().compareTo(right.getKey());
            }
        });
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : entries) {
            Map<String, Object> bucket = new LinkedHashMap<String, Object>();
            bucket.put(key, entry.getKey());
            bucket.put("count", entry.getValue());
            result.add(bucket);
        }
        return result;
    }

    private String filterValue(Map<String, String> filters, String key) {
        if (filters == null) {
            return null;
        }
        String value = filters.get(key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private int filterInt(Map<String, String> filters, String key, int defaultValue) {
        String value = filterValue(filters, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static class AdapterAggregate {
        private final String adapterCode;
        private int total;
        private int successCount;
        private int errorCount;
        private int timeoutCount;
        private long totalElapsed;

        AdapterAggregate(String adapterCode) {
            this.adapterCode = adapterCode;
        }

        Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("adapter_code", adapterCode);
            view.put("total", total);
            view.put("success_count", successCount);
            view.put("error_count", errorCount);
            view.put("timeout_count", timeoutCount);
            view.put("average_elapsed_ms", total == 0 ? 0.0 : Math.round(totalElapsed * 100.0 / total) / 100.0);
            return view;
        }
    }
}
