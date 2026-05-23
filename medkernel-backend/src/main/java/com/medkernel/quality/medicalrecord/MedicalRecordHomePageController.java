package com.medkernel.quality.medicalrecord;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-04 · 病案首页质控 + DRG 入组率看板。
 * 合规：国家卫健委病案首页填写规范 + 三甲复评必查项。
 */
@RestController
@RequestMapping("/api/v1/quality/medical-record")
public class MedicalRecordHomePageController {

    public record HomePageDefect(String mrn, String defectType, String severity, String dept) {}

    @GetMapping("/defects")
    public List<HomePageDefect> defects() {
        return List.of(
            new HomePageDefect("MRN-002201", "主诊断与手术不一致", "high", "外科"),
            new HomePageDefect("MRN-002205", "手术日期缺失", "high", "外科"),
            new HomePageDefect("MRN-002218", "出院诊断编码错误", "medium", "心内科"),
            new HomePageDefect("MRN-002231", "并发症未填写", "low", "全科")
        );
    }

    @GetMapping("/drg-entry-rate")
    public Map<String, Object> drgEntryRate() {
        return Map.of(
            "month", "2026-08",
            "totalDischarges", 12834,
            "drgEntered", 12431,
            "entryRate", 0.968,
            "topUnreached", List.of(
                Map.of("dept", "急诊", "rate", 0.78),
                Map.of("dept", "ICU", "rate", 0.85)
            )
        );
    }
}
