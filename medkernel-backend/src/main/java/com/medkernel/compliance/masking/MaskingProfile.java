package com.medkernel.compliance.masking;

/**
 * GA-EXT-10 · 数据脱敏 4 套规则 profile。
 *
 * <p>合规依据：
 * - GB/T 35273-2020 个人信息安全规范
 * - 个人信息保护法（PIPL）第 51 条 去标识化义务
 * - 数据出境安全评估办法
 */
public enum MaskingProfile {
    /** 开发环境：身份证 / 手机全脱，姓名半脱。 */
    DEV,

    /** 测试环境：身份证 / 手机全脱，姓名全脱。 */
    TEST,

    /** 培训环境：身份证 / 手机全脱，姓名 → 虚拟。 */
    TRAINING,

    /** 数据出境：身份证 / 手机 / 姓名 / 病历号 / DOB 全脱 → 假名化 hash。 */
    EXPORT
}
