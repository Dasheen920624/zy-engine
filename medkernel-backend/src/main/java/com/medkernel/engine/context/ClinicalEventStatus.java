package com.medkernel.engine.context;

/**
 * 临床事件处理状态。
 *
 * <p>API-01 仅使用 RECEIVED / MAPPED 表示 snapshot 创建之前/之后；
 * 后续 API-02 接管完整状态机。
 */
public enum ClinicalEventStatus {
    RECEIVED,
    MAPPED,
    PROCESSED,
    FAILED
}
