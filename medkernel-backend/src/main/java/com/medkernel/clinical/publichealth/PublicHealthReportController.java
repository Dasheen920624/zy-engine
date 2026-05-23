package com.medkernel.clinical.publichealth;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-03 · 公共卫生直报：传染病 / VTE / 死亡报告卡。
 *
 * <p>合规依据：
 * - 中华人民共和国传染病防治法
 * - 国家卫健委《医疗机构死亡报告管理规范》
 * - 医院 VTE 防治管理规范（DRG 与三甲考核刚需）
 */
@RestController
@RequestMapping("/api/v1/clinical/public-health")
public class PublicHealthReportController {

    public record DraftReport(
        String type,           // infectious / vte / death
        String patientMpi,
        String suggestedFields,
        String status          // draft / submitted
    ) {}

    @GetMapping("/drafts")
    public List<DraftReport> drafts() {
        return List.of(
            new DraftReport("infectious", "MPI-000123458", "病种=H1N1甲流 · 报卡时限=24h · 自动预填 80%", "draft"),
            new DraftReport("vte", "MPI-000123456", "VTE 评分=Caprini 5 分（高危）· 已自动开预防医嘱", "draft"),
            new DraftReport("death", "MPI-000123460", "ICD-10 主诊断+死亡时间已抓取，待医生签字", "draft")
        );
    }

    @PostMapping("/{type}/submit")
    public Map<String, Object> submit(@PathVariable String type) {
        return Map.of(
            "type", type,
            "result", "ok",
            "tracking", "NCIS-2026-08-" + (int) (Math.random() * 10000),
            "channel", switch (type) {
                case "infectious" -> "中国疾病预防控制信息系统 · 传染病直报模块";
                case "vte" -> "国家 VTE 防治项目办公室";
                case "death" -> "国家卫健委死亡报告管理系统";
                default -> "未知通道";
            }
        );
    }
}
