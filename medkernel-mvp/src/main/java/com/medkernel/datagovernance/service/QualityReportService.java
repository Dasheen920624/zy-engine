package com.medkernel.datagovernance.service;

import com.medkernel.datagovernance.entity.QualityCheckEntity;
import com.medkernel.datagovernance.entity.QualityRuleEntity;
import com.medkernel.datagovernance.repository.QualityCheckRepository;
import com.medkernel.datagovernance.repository.QualityRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据质量报告服务
 */
@Service
public class QualityReportService {
    private static final Logger log = LoggerFactory.getLogger(QualityReportService.class);

    private final QualityRuleRepository qualityRuleRepository;
    private final QualityCheckRepository qualityCheckRepository;

    public QualityReportService(QualityRuleRepository qualityRuleRepository,
                                QualityCheckRepository qualityCheckRepository) {
        this.qualityRuleRepository = qualityRuleRepository;
        this.qualityCheckRepository = qualityCheckRepository;
    }

    /**
     * 生成数据质量报告
     */
    public Map<String, Object> generateReport(String tenantId) {
        Map<String, Object> report = new LinkedHashMap<>();
        
        // 报告基本信息
        report.put("report_time", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()));
        report.put("tenant_id", tenantId);
        
        // 规则统计
        List<QualityRuleEntity> rules = qualityRuleRepository.findAllByTenantId(tenantId);
        Map<String, Object> ruleStats = new LinkedHashMap<>();
        ruleStats.put("total", rules.size());
        ruleStats.put("active", rules.stream().filter(r -> "ACTIVE".equals(r.getStatus())).count());
        ruleStats.put("inactive", rules.stream().filter(r -> !"ACTIVE".equals(r.getStatus())).count());
        
        // 按类型统计
        Map<String, Long> byType = rules.stream()
                .collect(Collectors.groupingBy(QualityRuleEntity::getRuleType, Collectors.counting()));
        ruleStats.put("by_type", byType);
        
        // 按严重程度统计
        Map<String, Long> bySeverity = rules.stream()
                .collect(Collectors.groupingBy(QualityRuleEntity::getSeverity, Collectors.counting()));
        ruleStats.put("by_severity", bySeverity);
        
        report.put("rule_statistics", ruleStats);
        
        // 检查记录统计
        List<QualityCheckEntity> checks = qualityCheckRepository.findAllByTenantId(tenantId);
        Map<String, Object> checkStats = new LinkedHashMap<>();
        checkStats.put("total_checks", checks.size());
        checkStats.put("passed", checks.stream().filter(c -> "PASS".equals(c.getCheckResult())).count());
        checkStats.put("failed", checks.stream().filter(c -> "FAIL".equals(c.getCheckResult())).count());
        
        // 通过率
        double passRate = checks.isEmpty() ? 0 : 
                (double) checks.stream().filter(c -> "PASS".equals(c.getCheckResult())).count() / checks.size() * 100;
        checkStats.put("pass_rate", String.format("%.2f%%", passRate));
        
        // 按规则统计失败数
        Map<String, Long> failuresByRule = checks.stream()
                .filter(c -> "FAIL".equals(c.getCheckResult()))
                .collect(Collectors.groupingBy(QualityCheckEntity::getRuleCode, Collectors.counting()));
        checkStats.put("failures_by_rule", failuresByRule);
        
        // 按实体统计失败数
        Map<String, Long> failuresByEntity = checks.stream()
                .filter(c -> "FAIL".equals(c.getCheckResult()))
                .collect(Collectors.groupingBy(QualityCheckEntity::getTargetEntity, Collectors.counting()));
        checkStats.put("failures_by_entity", failuresByEntity);
        
        report.put("check_statistics", checkStats);
        
        // 最近失败记录
        List<Map<String, Object>> recentFailures = checks.stream()
                .filter(c -> "FAIL".equals(c.getCheckResult()))
                .sorted((a, b) -> b.getCheckTime().compareTo(a.getCheckTime()))
                .limit(10)
                .map(c -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("check_id", c.getCheckId());
                    item.put("rule_code", c.getRuleCode());
                    item.put("target_entity", c.getTargetEntity());
                    item.put("target_id", c.getTargetId());
                    item.put("error_message", c.getErrorMessage());
                    item.put("check_time", c.getCheckTime());
                    return item;
                })
                .collect(Collectors.toList());
        report.put("recent_failures", recentFailures);
        
        return report;
    }

    /**
     * 获取数据质量监控指标
     */
    public Map<String, Object> getMonitorMetrics(String tenantId) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        List<QualityCheckEntity> checks = qualityCheckRepository.findAllByTenantId(tenantId);
        
        // 今日检查统计
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<QualityCheckEntity> todayChecks = checks.stream()
                .filter(c -> c.getCheckTime().isAfter(todayStart))
                .collect(Collectors.toList());
        
        Map<String, Object> todayStats = new LinkedHashMap<>();
        todayStats.put("total", todayChecks.size());
        todayStats.put("passed", todayChecks.stream().filter(c -> "PASS".equals(c.getCheckResult())).count());
        todayStats.put("failed", todayChecks.stream().filter(c -> "FAIL".equals(c.getCheckResult())).count());
        metrics.put("today", todayStats);
        
        // 本周检查统计
        LocalDateTime weekStart = todayStart.minusDays(7);
        List<QualityCheckEntity> weekChecks = checks.stream()
                .filter(c -> c.getCheckTime().isAfter(weekStart))
                .collect(Collectors.toList());
        
        Map<String, Object> weekStats = new LinkedHashMap<>();
        weekStats.put("total", weekChecks.size());
        weekStats.put("passed", weekChecks.stream().filter(c -> "PASS".equals(c.getCheckResult())).count());
        weekStats.put("failed", weekChecks.stream().filter(c -> "FAIL".equals(c.getCheckResult())).count());
        metrics.put("week", weekStats);
        
        // 本月检查统计
        LocalDateTime monthStart = todayStart.withDayOfMonth(1);
        List<QualityCheckEntity> monthChecks = checks.stream()
                .filter(c -> c.getCheckTime().isAfter(monthStart))
                .collect(Collectors.toList());
        
        Map<String, Object> monthStats = new LinkedHashMap<>();
        monthStats.put("total", monthChecks.size());
        monthStats.put("passed", monthChecks.stream().filter(c -> "PASS".equals(c.getCheckResult())).count());
        monthStats.put("failed", monthChecks.stream().filter(c -> "FAIL".equals(c.getCheckResult())).count());
        metrics.put("month", monthStats);
        
        // 趋势数据（最近7天）
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = todayStart.minusDays(i);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            
            List<QualityCheckEntity> dayChecks = checks.stream()
                    .filter(c -> c.getCheckTime().isAfter(dayStart) && c.getCheckTime().isBefore(dayEnd))
                    .collect(Collectors.toList());
            
            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", dayStart.toLocalDate().toString());
            dayData.put("total", dayChecks.size());
            dayData.put("passed", dayChecks.stream().filter(c -> "PASS".equals(c.getCheckResult())).count());
            dayData.put("failed", dayChecks.stream().filter(c -> "FAIL".equals(c.getCheckResult())).count());
            trend.add(dayData);
        }
        metrics.put("trend", trend);
        
        return metrics;
    }
}