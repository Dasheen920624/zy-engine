package com.medkernel.cdss;

/**
 * CDSS 风险分级。
 * 对标临床决策支持系统风险等级标准。
 */
public enum CdssRiskLevel {
    /** 信息提示 — 不阻断，仅展示 */
    INFO("INFO", "信息提示", false),
    /** 低风险提醒 — 不阻断，可忽略 */
    LOW("LOW", "低风险提醒", false),
    /** 中风险提醒 — 不阻断，需确认 */
    MEDIUM("MEDIUM", "中风险提醒", false),
    /** 高风险强提醒 — 不阻断，需二次确认 */
    HIGH("HIGH", "高风险强提醒", false),
    /** 危急阻断 — 阻断操作，需上级确认或覆盖 */
    CRITICAL("CRITICAL", "危急阻断", true);

    private final String code;
    private final String label;
    private final boolean blocking;

    CdssRiskLevel(String code, String label, boolean blocking) {
        this.code = code;
        this.label = label;
        this.blocking = blocking;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public boolean isBlocking() { return blocking; }

    public static CdssRiskLevel fromSeverity(String severity) {
        if (severity == null) return INFO;
        switch (severity.toUpperCase()) {
            case "CRITICAL": return CRITICAL;
            case "HIGH": return HIGH;
            case "WARNING":
            case "MEDIUM": return MEDIUM;
            case "LOW": return LOW;
            default: return INFO;
        }
    }
}
