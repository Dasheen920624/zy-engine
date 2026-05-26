package com.medkernel.engine.context;

/**
 * 临床事件类型，对应 detail spec §1.5.4 CDS Hooks 触发点。
 *
 * <p>API-02 临床事件 API 在本枚举上添加更细的事件子类型时，必须先与五方言迁移
 * {@code ck_clinical_event_type} 约束同步。
 */
public enum ClinicalEventType {
    DIAGNOSIS,
    ORDER,
    REPORT,
    DISCHARGE,
    FOLLOWUP,
    ADMISSION
}
