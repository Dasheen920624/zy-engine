package com.medkernel.quality;

/**
 * 评估对象类型。
 * 定义可被评估的业务对象类型。
 */
public enum EvalSubjectType {
    /** 病历质量 */
    EMR("EMR", "病历质量"),
    /** 医保合规 */
    INSURANCE("INSURANCE", "医保合规"),
    /** 临床路径执行 */
    PATHWAY("PATHWAY", "临床路径执行"),
    /** 科室综合 */
    DEPARTMENT("DEPARTMENT", "科室综合"),
    /** 配置质量 */
    CONFIG("CONFIG", "配置质量");

    private final String code;
    private final String label;

    EvalSubjectType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static EvalSubjectType fromCode(String code) {
        if (code == null) return null;
        for (EvalSubjectType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        return null;
    }
}
