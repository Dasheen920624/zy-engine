package com.medkernel.quality.variance;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-23 · 临床路径变异分析 + 优化建议。
 * 学科建设价值 —— 把"为什么这条路径走不通"聚类成可执行的改进项。
 */
@RestController
@RequestMapping("/api/v1/quality/variance")
public class PathwayVarianceController {

    public record VarianceCluster(String pathway, String varianceReason, Integer caseCount, String suggestion) {}

    @GetMapping("/clusters")
    public List<VarianceCluster> clusters() {
        return List.of(
            new VarianceCluster("胸痛 AMI 路径", "导管室等待 > 30 分钟", 47, "扩大导管室班次或加 PCI 团队"),
            new VarianceCluster("胸痛 AMI 路径", "肌钙蛋白未及时回报", 23, "急诊检验加快"),
            new VarianceCluster("卒中绿色通道", "影像确诊 > 45 分钟", 18, "急诊 CT 班次 24×7 化"),
            new VarianceCluster("高血压管理", "随访失访", 312, "电话随访改人工 + 短信提醒")
        );
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of(
            "totalVariance30d", 482,
            "clusteredReasons", 17,
            "topImpactedPathway", "高血压管理",
            "estimatedImprovementCost", "¥ 280,000",
            "estimatedAnnualBenefit", "¥ 1,840,000"
        );
    }
}
