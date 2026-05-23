package com.medkernel.quality.insurance;

/**
 * GA-EXT-01 · 医保 DRG/DIP 规则集版本（带月更同步元数据）。
 */
public record DrgRuleset(
    String version,         // 如 "2026.08"
    String effectiveFrom,   // 生效日 YYYY-MM-DD
    Integer groupCount,     // DRG/DIP 分组数
    String source,          // 国家医保局 / 省医保局 / 院内修订
    String status           // active / staged / archived
) {}
