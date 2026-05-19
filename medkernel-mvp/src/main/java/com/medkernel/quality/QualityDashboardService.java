package com.medkernel.quality;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 院级质控驾驶舱服务
 * 聚合路径执行、规则命中、质控问题、医保风险 4 大 KPI
 */
@Service
public class QualityDashboardService {
    private final Random random = new Random(42); // 固定种子保证数据一致性

    /**
     * 获取驾驶舱 4 KPI 聚合数据
     */
    public Map<String, Object> getDashboardKpis(String tenantId, String period, String departmentCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("period", period);
        result.put("departmentCode", departmentCode);
        result.put("generatedTime", LocalDate.now().format(DateTimeFormatter.ISO_DATE));

        // 路径执行 KPI
        Map<String, Object> pathway = new LinkedHashMap<>();
        pathway.put("totalEnrolled", 1247);
        pathway.put("completed", 1103);
        pathway.put("variationRate", 11.5);
        pathway.put("enrolledChange", 8.2);
        pathway.put("completedChange", 5.1);
        pathway.put("variationRateChange", -2.3);
        result.put("pathway", pathway);

        // 规则命中 KPI
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("realtimeBlock", 8);
        rule.put("softReminder", 1427);
        rule.put("hitRate", 23.0);
        rule.put("blockChange", -12.5);
        rule.put("reminderChange", 15.3);
        rule.put("hitRateChange", 3.2);
        result.put("rule", rule);

        // 质控问题 KPI
        Map<String, Object> qc = new LinkedHashMap<>();
        qc.put("totalIssues", 384);
        qc.put("closedIssues", 312);
        qc.put("rectificationRate", 81.3);
        qc.put("totalChange", -5.8);
        qc.put("closedChange", 12.4);
        qc.put("rectificationRateChange", 4.6);
        result.put("qc", qc);

        // 医保风险 KPI
        Map<String, Object> insurance = new LinkedHashMap<>();
        insurance.put("potentialRefund", 24800);
        insurance.put("refundChange", -12.0);
        insurance.put("riskLevel", "MEDIUM");
        result.put("insurance", insurance);

        return result;
    }

    /**
     * 获取科室排名列表
     */
    public Map<String, Object> getDepartmentRanking(String tenantId, String period) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("period", period);

        List<Map<String, Object>> departments = new ArrayList<>();
        departments.add(createDeptRank("心内科", 387, 92.1, 89.3, 18.5, 5));
        departments.add(createDeptRank("神经内科", 256, 88.4, 75.2, 22.1, 3));
        departments.add(createDeptRank("骨科", 198, 95.2, 91.0, 15.3, 5));
        departments.add(createDeptRank("呼吸内科", 176, 85.7, 78.6, 25.8, 3));
        departments.add(createDeptRank("消化内科", 145, 90.3, 82.1, 19.7, 4));
        departments.add(createDeptRank("普外科", 132, 91.8, 86.5, 17.2, 4));
        departments.add(createDeptRank("妇产科", 98, 87.6, 73.9, 28.4, 3));
        departments.add(createDeptRank("儿科", 87, 82.3, 68.5, 31.2, 2));
        departments.add(createDeptRank("急诊科", 76, 79.5, 65.8, 35.6, 2));
        departments.add(createDeptRank("ICU", 45, 94.1, 92.7, 12.8, 5));

        result.put("departments", departments);
        result.put("total", departments.size());
        return result;
    }

    /**
     * 科室钻取详情
     */
    public Map<String, Object> getDepartmentDetail(String tenantId, String deptCode, String period) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("departmentCode", deptCode);
        result.put("period", period);

        // 科室 KPI（简化版，范围限定到科室）
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("pathway", Map.of("totalEnrolled", 387, "completed", 356, "variationRate", 8.0));
        kpis.put("rule", Map.of("realtimeBlock", 3, "softReminder", 285, "hitRate", 18.5));
        kpis.put("qc", Map.of("totalIssues", 72, "closedIssues", 64, "rectificationRate", 88.9));
        kpis.put("insurance", Map.of("potentialRefund", 4200, "refundChange", -8.5));
        result.put("kpis", kpis);

        // 变异 TOP10
        List<Map<String, Object>> variations = new ArrayList<>();
        variations.add(createVariation("AMI_STEMI", "PCI 时间窗超时", 7, "导管室排程冲突"));
        variations.add(createVariation("AMI_STEMI", "抗血小板调整", 5, "合并出血风险"));
        variations.add(createVariation("HF_DECOMP", "利尿剂剂量调整", 4, "肾功能受限"));
        variations.add(createVariation("AMI_NSTEMI", "介入时机选择", 3, "造影剂过敏史"));
        variations.add(createVariation("HF_DECOMP", "出院标准评估", 3, "BNP 下降不明显"));
        result.put("topVariations", variations);

        // 医生绩效（仅主任可见）
        List<Map<String, Object>> doctors = new ArrayList<>();
        doctors.add(createDoctorPerf("李明", 87, 93.1, 88.2, 18.3));
        doctors.add(createDoctorPerf("周强", 82, 95.2, 91.0, 15.1));
        doctors.add(createDoctorPerf("王芳", 76, 91.5, 85.7, 20.4));
        doctors.add(createDoctorPerf("张伟", 68, 89.3, 82.1, 22.8));
        doctors.add(createDoctorPerf("刘洋", 54, 86.7, 79.5, 25.6));
        result.put("doctorPerformance", doctors);

        return result;
    }

    /**
     * 趋势数据（最近 N 天）
     */
    public Map<String, Object> getTrendData(String tenantId, int days, String departmentCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("days", days);
        result.put("departmentCode", departmentCode);

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> trend = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.format(DateTimeFormatter.ISO_DATE));
            day.put("pathwayCompletionRate", 85.0 + random.nextDouble() * 15);
            day.put("ruleHitRate", 18.0 + random.nextDouble() * 12);
            day.put("qcRectificationRate", 75.0 + random.nextDouble() * 20);
            day.put("insuranceRiskAmount", 15000 + random.nextInt(20000));
            trend.add(day);
        }

        result.put("trend", trend);
        return result;
    }

    private Map<String, Object> createDeptRank(String name, int enrolled,
                                                double completionRate, double rectificationRate,
                                                double ruleHitRate, int stars) {
        Map<String, Object> dept = new LinkedHashMap<>();
        dept.put("name", name);
        dept.put("enrolled", enrolled);
        dept.put("completionRate", completionRate);
        dept.put("rectificationRate", rectificationRate);
        dept.put("ruleHitRate", ruleHitRate);
        dept.put("stars", stars);
        return dept;
    }

    private Map<String, Object> createVariation(String pathwayCode, String node,
                                                 int count, String reason) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("pathwayCode", pathwayCode);
        v.put("variationNode", node);
        v.put("count", count);
        v.put("reason", reason);
        return v;
    }

    private Map<String, Object> createDoctorPerf(String name, int cases,
                                                  double completionRate, double rectificationRate,
                                                  double ruleHitRate) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("name", name);
        d.put("cases", cases);
        d.put("completionRate", completionRate);
        d.put("rectificationRate", rectificationRate);
        d.put("ruleHitRate", ruleHitRate);
        return d;
    }
}
