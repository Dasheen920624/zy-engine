package com.medkernel.common;

/**
 * 多医院默认兜底常量集中点。仅在请求未显式声明组织维度时使用，
 * 业务 SQL 必须通过参数注入（PreparedStatement）而非字符串拼接。
 *
 * 红线提醒：禁止把这些字面量直接拼到 SQL 中，否则违反"不允许硬编码单医院逻辑"。
 */
public final class OrgDefaults {

    public static final String DEFAULT_TENANT_ID = "default";
    public static final String DEFAULT_HOSPITAL_CODE = "ZYHOSPITAL";
    public static final String DEFAULT_SCOPE_LEVEL = "HOSPITAL";

    private OrgDefaults() {
    }
}
