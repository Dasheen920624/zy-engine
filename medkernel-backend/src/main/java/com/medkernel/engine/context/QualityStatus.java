package com.medkernel.engine.context;

/**
 * 资源质量状态。
 *
 * <ul>
 *   <li>{@code VALID}：必填项齐全</li>
 *   <li>{@code PARTIAL}：非关键字段缺失，仍可入库</li>
 *   <li>{@code INVALID}：致命缺失（如 Patient 缺失），拒绝入库</li>
 * </ul>
 */
public enum QualityStatus {
    VALID,
    PARTIAL,
    INVALID
}
