package com.medkernel.common.dataclass;

/**
 * 数据分级（《数据安全法》2021.9 §21 实施落地）。
 *
 * <p>四级分类，由低到高敏感度递增：
 * <ol>
 *   <li>{@link #PUBLIC} —— 公开数据（医院公告 / 字典代码 / 公开规则）</li>
 *   <li>{@link #INTERNAL} —— 内部数据（系统配置 / 内部 ID / 业务流水）</li>
 *   <li>{@link #SENSITIVE} —— 敏感个人信息（姓名 / 手机号 / 地址 / 邮箱）</li>
 *   <li>{@link #HEALTH_DATA} —— 健康医疗数据（身份证 / 病历 / 诊断 / 处方 /
 *       影像 / 检验报告等《个人信息保护法》§28 定义的「敏感个人信息」）</li>
 * </ol>
 *
 * <h2>合规对应</h2>
 * <table>
 *   <caption>分级与合规要求</caption>
 *   <tr><th>分级</th><th>存储要求</th><th>访问控制</th><th>审计</th></tr>
 *   <tr><td>PUBLIC</td><td>明文</td><td>无</td><td>不必</td></tr>
 *   <tr><td>INTERNAL</td><td>明文</td><td>登录后</td><td>登录</td></tr>
 *   <tr><td>SENSITIVE</td><td>明文 + 行级权限</td><td>角色 + 脱敏</td><td>全量</td></tr>
 *   <tr><td>HEALTH_DATA</td><td>SM4 加密</td><td>角色 + 脱敏 + 双因素</td><td>全量 + 链式</td></tr>
 * </table>
 *
 * <h2>枚举顺序约定</h2>
 *
 * <p>{@link #ordinal()} 数值递增即敏感度递增；
 * 可以用 {@code a.ordinal() >= b.ordinal()} 比较敏感度。
 * <strong>禁止在中间插入新值或调整顺序</strong>（会破坏比较语义）。
 *
 * @see DataClass
 * @see Encrypted
 */
public enum DataClassification {

    /** 公开数据：无访问限制。 */
    PUBLIC,

    /** 内部数据：登录后可访问。 */
    INTERNAL,

    /** 敏感个人信息：姓名 / 手机号 / 地址等，需脱敏 + 行级权限。 */
    SENSITIVE,

    /** 健康医疗数据：身份证 / 病历 / 诊断等，强制 SM4 加密存储。 */
    HEALTH_DATA;

    /**
     * 是否需要强制加密存储。
     * 当前规则：{@link #HEALTH_DATA} 必须加密；其余级别不强制。
     */
    public boolean requiresEncryption() {
        return this == HEALTH_DATA;
    }

    /**
     * 是否需要脱敏展示给非授权用户。
     * 当前规则：{@link #SENSITIVE} 及以上需脱敏。
     */
    public boolean requiresMasking() {
        return this.ordinal() >= SENSITIVE.ordinal();
    }
}
