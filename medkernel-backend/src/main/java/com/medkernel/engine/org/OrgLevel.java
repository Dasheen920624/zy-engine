package com.medkernel.engine.org;

/**
 * 组织层级枚举。对应宪法 §6.3 / 详细规范 §4 的六级组织树。
 *
 * <p>注意 {@link #WARD} 在大多数医院作为可选层级出现（只有住院/重症等场景需要）。
 *
 * @see com.medkernel.shared.context.OrgScope
 */
public enum OrgLevel {
    /** 平台租户根 */
    TENANT,
    /** 集团 */
    GROUP,
    /** 医院 */
    HOSPITAL,
    /** 院区 / 分院 */
    CAMPUS,
    /** 社区卫生服务中心 / 街道卫生所 / 医联体成员机构 */
    SITE,
    /** 科室 */
    DEPARTMENT,
    /** 病区（可选） */
    WARD
}
