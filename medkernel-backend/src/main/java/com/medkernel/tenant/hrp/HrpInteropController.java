package com.medkernel.tenant.hrp;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-07 · HRP / 财务 / 物资接口（集团版增量价值）。
 * 集团版商业本质：把临床数据与运营数据关联，看科室运营 ROI。
 */
@RestController
@RequestMapping("/api/v1/tenant/hrp")
public class HrpInteropController {

    @GetMapping("/connections")
    public List<Map<String, Object>> connections() {
        return List.of(
            Map.of("system", "HRP", "vendor", "用友 HRP 8.0", "status", "ok", "lastSync", "30 秒前"),
            Map.of("system", "财务", "vendor", "金蝶 K3 Cloud", "status", "ok", "lastSync", "1 分钟前"),
            Map.of("system", "物资", "vendor", "卫宁 SPD 4.5", "status", "warn", "lastSync", "5 分钟前 · 限流")
        );
    }

    @GetMapping("/dept-roi")
    public List<Map<String, Object>> deptRoi() {
        return List.of(
            Map.of("dept", "心内科", "revenue", 12_834_000, "drug", 4_283_000, "consumable", 1_283_000, "roi", 0.42),
            Map.of("dept", "神经内科", "revenue", 9_283_000, "drug", 3_123_000, "consumable", 928_000, "roi", 0.38),
            Map.of("dept", "急诊", "revenue", 6_283_000, "drug", 1_823_000, "consumable", 1_023_000, "roi", 0.31)
        );
    }
}
