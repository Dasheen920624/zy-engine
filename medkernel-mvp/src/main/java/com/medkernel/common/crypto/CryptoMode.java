package com.medkernel.common.crypto;

/**
 * 加密模式枚举（国密/国际双栈兼容）。
 *
 * <p>支持三种运行模式：
 * <ul>
 *   <li>{@link #SM_ONLY} — 纯国密模式：SM2/SM3/SM4，适用于等保三级/国密合规场景</li>
 *   <li>{@link #INTERNATIONAL_ONLY} — 纯国际模式：RSA/SHA-256/AES-256-GCM，适用于海外部署</li>
 *   <li>{@link #DUAL_STACK} — 双栈兼容模式：国密为主，国际为备，适用于过渡期</li>
 * </ul>
 *
 * <p>默认值：{@link #SM_ONLY}（等保合规优先）。
 *
 * @see SmCryptoService
 */
public enum CryptoMode {

    /** 纯国密模式：SM2 + SM3 + SM4 */
    SM_ONLY("sm_only", "纯国密模式", true, false),

    /** 纯国际模式：RSA + SHA-256 + AES-256-GCM */
    INTERNATIONAL_ONLY("international_only", "纯国际模式", false, true),

    /** 双栈兼容模式：国密为主，国际为备 */
    DUAL_STACK("dual_stack", "双栈兼容模式", true, true);

    private final String code;
    private final String description;
    private final boolean smEnabled;
    private final boolean internationalEnabled;

    CryptoMode(String code, String description, boolean smEnabled, boolean internationalEnabled) {
        this.code = code;
        this.description = description;
        this.smEnabled = smEnabled;
        this.internationalEnabled = internationalEnabled;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
    public boolean isSmEnabled() { return smEnabled; }
    public boolean isInternationalEnabled() { return internationalEnabled; }

    /**
     * 从代码字符串解析 CryptoMode。
     */
    public static CryptoMode fromCode(String code) {
        if (code == null) return SM_ONLY;
        for (CryptoMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return SM_ONLY;
    }
}
