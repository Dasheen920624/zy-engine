package com.medkernel.compliance.dataexport;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-19 · 数据出境评估包（数据出境安全评估办法）。
 * 外资合作 / 跨境科研项目必须通过的合规链路。
 */
@RestController
@RequestMapping("/api/v1/compliance/data-export")
public class DataExportAssessmentController {

    public record AssessmentRequest(String purpose, String receiver, String country, String dataCategory, String volume) {}

    public record AssessmentResult(
        String assessmentId,
        Boolean requiresGovApproval,
        List<String> requiredArtifacts,
        String maskingProfile,
        String estimatedReviewDays
    ) {}

    @PostMapping("/assess")
    public AssessmentResult assess(@RequestBody AssessmentRequest req) {
        boolean needGov = "敏感".equals(req.dataCategory()) || (req.volume() != null && req.volume().contains("万"));
        return new AssessmentResult(
            "EXP-" + System.currentTimeMillis(),
            needGov,
            List.of(
                "数据脱敏方案（EXPORT profile）",
                "数据接收方资质证明",
                "数据处理目的与必要性说明",
                "DPA（数据处理协议）",
                needGov ? "网信办出境安全评估申请表" : "自评估报告"
            ),
            "EXPORT",
            needGov ? "45 个工作日" : "7 个工作日（自评估）"
        );
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history() {
        return List.of(
            Map.of("id", "EXP-2026-001", "purpose", "胸痛 AMI 国际多中心研究", "country", "US", "status", "已通过网信办评估"),
            Map.of("id", "EXP-2026-002", "purpose", "卒中影像 AI 研究合作", "country", "JP", "status", "自评估通过")
        );
    }
}
