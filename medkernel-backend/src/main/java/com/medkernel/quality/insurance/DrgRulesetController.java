package com.medkernel.quality.insurance;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GA-EXT-01 · 医保 DRG/DIP 月更同步 API。
 *
 * <p>真实生产将定时拉取国家医保局公开数据 + 省级补丁 + 院内修订，存到 db。
 * 当前为内存 mock，前端在 InsuranceAudit 页展示版本时间线。
 */
@RestController
@RequestMapping("/api/v1/quality/insurance/drg")
public class DrgRulesetController {

    private static final List<DrgRuleset> SEED = List.of(
        new DrgRuleset("2026.08", "2026-08-01", 818, "国家医保局 + 省级补丁", "staged"),
        new DrgRuleset("2026.07", "2026-07-01", 815, "国家医保局", "active"),
        new DrgRuleset("2026.06", "2026-06-01", 814, "国家医保局", "archived"),
        new DrgRuleset("2026.05", "2026-05-01", 812, "国家医保局", "archived")
    );

    @GetMapping("/rulesets")
    public List<DrgRuleset> list() {
        return SEED;
    }

    @PostMapping("/sync")
    public Map<String, Object> sync() {
        return Map.of(
            "result", "ok",
            "fetched", 1,
            "newVersion", "2026.08",
            "diff", Map.of("added", 3, "removed", 0, "changed", 12)
        );
    }
}
