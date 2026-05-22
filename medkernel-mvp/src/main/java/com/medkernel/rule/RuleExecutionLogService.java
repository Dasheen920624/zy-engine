package com.medkernel.rule;
import com.medkernel.common.TraceContext;
import com.medkernel.dto.RuleResult;
import com.medkernel.organization.OrganizationContext;
import com.medkernel.util.ClinicalFactUtils;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
@Service
public class RuleExecutionLogService {
    private static final int EXEC_LOG_RING_CAPACITY = 500;
    private static final String DEFAULT_TENANT_ID = "default";
    private static final String DEFAULT_HOSPITAL_CODE = "ZYHOSPITAL";
    private static final String DEFAULT_SCOPE_LEVEL = "HOSPITAL";
    private final Deque<RuleExecLogEntry> execLogs = new ConcurrentLinkedDeque<RuleExecLogEntry>();
    private final AtomicLong execLogSequence = new AtomicLong();
        public void recordExecLog(RuleResult result, String ruleVersion, Map<String, Object> patientContext,
                                   long elapsedMs, String resultStatus, String errorCode, String errorMessage,
                                   RuleDefinition definition, OrganizationContext orgContext) {
            RuleExecLogEntry entry = new RuleExecLogEntry();
            entry.setLogId("rxl-" + execLogSequence.incrementAndGet());
            entry.setTraceId(TraceContext.getTraceId());
            entry.setRuleCode(result.getRuleCode());
            entry.setRuleVersion(ruleVersion);
            entry.setPatientId(ClinicalFactUtils.patientId(patientContext));
            entry.setEncounterId(ClinicalFactUtils.encounterId(patientContext));
            entry.setHit(result.isHit());
            entry.setSeverity(result.getSeverity());
            entry.setMessage(result.getMessage());
            entry.setElapsedMs(elapsedMs);
            entry.setResultStatus(resultStatus);
            entry.setErrorCode(errorCode);
            entry.setErrorMessage(errorMessage);
            entry.setActions(new ArrayList<String>(result.getActions()));
            entry.setEvidence(new ArrayList<Map<String, Object>>(result.getEvidence()));
            entry.setCreatedTime(nowText());
            applyOrganization(entry, definition, orgContext);
            // 内存环形缓冲优先保留最近 EXEC_LOG_RING_CAPACITY 条记录，长期归档仍依赖 RE_RULE_EXEC_LOG。
            execLogs.addLast(entry);
            while (execLogs.size() > EXEC_LOG_RING_CAPACITY) {
                execLogs.pollFirst();
            }
        }
        public List<RuleExecLogEntry> listExecLogs(Map<String, String> filters) {
            String ruleCode = filterValue(filters, "ruleCode");
            String traceId = filterValue(filters, "traceId");
            String patientId = filterValue(filters, "patientId");
            String encounterId = filterValue(filters, "encounterId");
            String resultStatus = filterValue(filters, "resultStatus");
            String tenantId = filterValue(filters, "tenantId");
            String groupCode = filterValue(filters, "groupCode");
            String hospitalCode = filterValue(filters, "hospitalCode");
            String campusCode = filterValue(filters, "campusCode");
            String siteCode = filterValue(filters, "siteCode");
            String departmentCode = filterValue(filters, "departmentCode");
            String scopeLevel = upper(filterValue(filters, "scopeLevel"));
            String scopeCode = filterValue(filters, "scopeCode");
            Boolean hit = filterBoolean(filters, "hit");
            int limit = filterInt(filters, "limit", 100);
            if (limit <= 0 || limit > EXEC_LOG_RING_CAPACITY) {
                limit = EXEC_LOG_RING_CAPACITY;
            }
            List<RuleExecLogEntry> matched = new ArrayList<RuleExecLogEntry>();
            // ConcurrentLinkedDeque 的 descendingIterator 返回最新写入的元素，便于按时间倒序返回执行日志。
            java.util.Iterator<RuleExecLogEntry> iterator = execLogs.descendingIterator();
            while (iterator.hasNext() && matched.size() < limit) {
                RuleExecLogEntry entry = iterator.next();
                if (ruleCode != null && !ruleCode.equalsIgnoreCase(entry.getRuleCode())) {
                    continue;
                }
                if (traceId != null && !traceId.equals(entry.getTraceId())) {
                    continue;
                }
                if (patientId != null && !patientId.equals(entry.getPatientId())) {
                    continue;
                }
                if (encounterId != null && !encounterId.equals(entry.getEncounterId())) {
                    continue;
                }
                if (resultStatus != null && !resultStatus.equalsIgnoreCase(entry.getResultStatus())) {
                    continue;
                }
                if (hit != null && hit.booleanValue() != entry.isHit()) {
                    continue;
                }
                if (!matchesLogOrg(entry, tenantId, groupCode, hospitalCode, campusCode,
                        siteCode, departmentCode, scopeLevel, scopeCode)) {
                    continue;
                }
                matched.add(entry);
            }
            return matched;
        }
        public Map<String, Object> summarizeExecLogs(Map<String, String> filters) {
            Map<String, String> effective = new LinkedHashMap<String, String>();
            if (filters != null) {
                effective.putAll(filters);
            }
            effective.put("limit", String.valueOf(Integer.MAX_VALUE));
            List<RuleExecLogEntry> entries = listExecLogs(effective);
            int total = entries.size();
            int totalHits = 0;
            long totalElapsed = 0;
            int errorCount = 0;
            Map<String, RuleAggregate> byRule = new LinkedHashMap<String, RuleAggregate>();
            Map<String, Integer> bySeverity = new LinkedHashMap<String, Integer>();
            Map<String, Integer> byResultStatus = new LinkedHashMap<String, Integer>();
            for (RuleExecLogEntry entry : entries) {
                if (entry.isHit()) {
                    totalHits++;
                }
                totalElapsed += entry.getElapsedMs();
                if ("ERROR".equalsIgnoreCase(entry.getResultStatus())) {
                    errorCount++;
                }
                increment(bySeverity, entry.getSeverity());
                increment(byResultStatus, entry.getResultStatus());
                String ruleCode = entry.getRuleCode();
                if (ruleCode == null) {
                    continue;
                }
                RuleAggregate agg = byRule.get(ruleCode);
                if (agg == null) {
                    agg = new RuleAggregate(ruleCode);
                    byRule.put(ruleCode, agg);
                }
                agg.total++;
                if (entry.isHit()) {
                    agg.hits++;
                }
                if ("ERROR".equalsIgnoreCase(entry.getResultStatus())) {
                    agg.errors++;
                }
                agg.totalElapsed += entry.getElapsedMs();
            }
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            summary.put("total", total);
            summary.put("total_hits", totalHits);
            summary.put("hit_rate", total == 0 ? 0.0 : Math.round(totalHits * 10000.0 / total) / 100.0);
            summary.put("error_count", errorCount);
            summary.put("average_elapsed_ms", total == 0 ? 0.0 : Math.round(totalElapsed * 100.0 / total) / 100.0);
            summary.put("by_rule", aggregatesToList(byRule));
            summary.put("by_severity", bucketsToList(bySeverity, "severity"));
            summary.put("by_result_status", bucketsToList(byResultStatus, "result_status"));
            summary.put("by_hospital_code", aggregateExecLogs(entries, "hospital_code"));
            summary.put("by_scope", aggregateExecLogs(entries, "scope"));
            return summary;
        }
        private void increment(Map<String, Integer> counts, String key) {
            if (key == null) {
                return;
            }
            Integer value = counts.get(key);
            counts.put(key, value == null ? 1 : value + 1);
        }
        private List<Map<String, Object>> aggregatesToList(Map<String, RuleAggregate> aggregates) {
            List<RuleAggregate> list = new ArrayList<RuleAggregate>(aggregates.values());
            Collections.sort(list, new Comparator<RuleAggregate>() {
                @Override
                public int compare(RuleAggregate left, RuleAggregate right) {
                    int byTotal = Integer.compare(right.total, left.total);
                    return byTotal != 0 ? byTotal : left.ruleCode.compareTo(right.ruleCode);
                }
            });
            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            for (RuleAggregate agg : list) {
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
        private static class RuleAggregate {
            private final String ruleCode;
            private int total;
            private int hits;
            private int errors;
            private long totalElapsed;
            RuleAggregate(String ruleCode) {
                this.ruleCode = ruleCode;
            }
            Map<String, Object> toView() {
                Map<String, Object> view = new LinkedHashMap<String, Object>();
                view.put("rule_code", ruleCode);
                view.put("total", total);
                view.put("hits", hits);
                view.put("errors", errors);
                view.put("hit_rate", total == 0 ? 0.0 : Math.round(hits * 10000.0 / total) / 100.0);
                view.put("average_elapsed_ms", total == 0 ? 0.0 : Math.round(totalElapsed * 100.0 / total) / 100.0);
                return view;
            }
        }
        public RuleExecLogEntry getExecLog(String logId) {
            if (logId == null) {
                throw new IllegalArgumentException("logId is required");
            }
            for (RuleExecLogEntry entry : execLogs) {
                if (logId.equals(entry.getLogId())) {
                    return entry;
                }
            }
            throw new IllegalArgumentException("rule exec log not found: " + logId);
        }
        private void applyOrganization(RuleExecLogEntry entry, RuleDefinition definition,
                                       OrganizationContext orgContext) {
            if (definition != null) {
                entry.setTenantId(definition.getTenantId());
                entry.setGroupCode(definition.getGroupCode());
                entry.setHospitalCode(definition.getHospitalCode());
                entry.setCampusCode(definition.getCampusCode());
                entry.setSiteCode(definition.getSiteCode());
                entry.setDepartmentCode(definition.getDepartmentCode());
                entry.setScopeLevel(definition.getScopeLevel());
                entry.setScopeCode(definition.getScopeCode());
                entry.setOrgSource(definition.getOrgSource());
                return;
            }
            if (orgContext != null) {
                entry.setTenantId(orgContext.getTenantId());
                entry.setGroupCode(orgContext.getGroupCode());
                entry.setHospitalCode(orgContext.getHospitalCode());
                entry.setCampusCode(orgContext.getCampusCode());
                entry.setSiteCode(orgContext.getSiteCode());
                entry.setDepartmentCode(orgContext.getDepartmentCode());
                entry.setScopeLevel(orgContext.getEffectiveScopeLevel());
                entry.setScopeCode(orgContext.getEffectiveScopeCode());
                entry.setOrgSource(orgContext.getSource());
                return;
            }
            entry.setTenantId(DEFAULT_TENANT_ID);
            entry.setHospitalCode(DEFAULT_HOSPITAL_CODE);
            entry.setScopeLevel(DEFAULT_SCOPE_LEVEL);
            entry.setScopeCode(DEFAULT_HOSPITAL_CODE);
            entry.setOrgSource("DEFAULT");
        }
        private boolean matchesLogOrg(RuleExecLogEntry entry, String tenantId, String groupCode,
                                      String hospitalCode, String campusCode, String siteCode,
                                      String departmentCode, String scopeLevel, String scopeCode) {
            return matches(tenantId, entry.getTenantId(), false)
                    && matches(groupCode, entry.getGroupCode(), false)
                    && matches(hospitalCode, entry.getHospitalCode(), false)
                    && matches(campusCode, entry.getCampusCode(), false)
                    && matches(siteCode, entry.getSiteCode(), false)
                    && matches(departmentCode, entry.getDepartmentCode(), false)
                    && matches(scopeLevel, entry.getScopeLevel(), true)
                    && matches(scopeCode, entry.getScopeCode(), false);
        }
        private boolean matches(String expected, String actual, boolean ignoreCase) {
            if (expected == null) {
                return true;
            }
            if (actual == null) {
                return false;
            }
            return ignoreCase ? expected.equalsIgnoreCase(actual) : expected.equals(actual);
        }
        private List<Map<String, Object>> aggregateExecLogs(List<RuleExecLogEntry> entries, String dimension) {
            Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
            for (RuleExecLogEntry entry : entries) {
                String key = execLogDimension(entry, dimension);
                if (key == null) {
                    continue;
                }
                Integer value = counts.get(key);
                counts.put(key, value == null ? 1 : value + 1);
            }
            return bucketsToList(counts, dimension);
        }
        private String execLogDimension(RuleExecLogEntry entry, String dimension) {
            if ("hospital_code".equals(dimension)) {
                return entry.getHospitalCode();
            }
            if ("scope".equals(dimension)) {
                return string(entry.getScopeLevel(), "UNKNOWN") + ":" + string(entry.getScopeCode(), "UNKNOWN");
            }
            return null;
        }
        private String string(Object value, String defaultValue) {
            if (value == null) {
                return defaultValue;
            }
            String text = String.valueOf(value);
            return text.trim().isEmpty() ? defaultValue : text;
        }
        private String filterValue(Map<String, String> filters, String key) {
            if (filters == null) {
                return null;
            }
            String value = filters.get(key);
            return value == null || value.trim().isEmpty() ? null : value.trim();
        }
        private Boolean filterBoolean(Map<String, String> filters, String key) {
            String value = filterValue(filters, key);
            if (value == null) {
                return null;
            }
            return Boolean.valueOf(value);
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
        private String nowText() {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
        }
        private String upper(String value) {
            return value == null ? null : value.trim().toUpperCase();
        }
}
