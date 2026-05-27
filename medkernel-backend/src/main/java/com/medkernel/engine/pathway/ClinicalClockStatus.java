package com.medkernel.engine.pathway;

/**
 * 关键时钟状态。
 *
 * <p>覆盖运行中、已完成、超时、缺少数据和因路径变异暂停的时钟事实。
 */
public enum ClinicalClockStatus {
    RUNNING,
    COMPLETED,
    TIMEOUT,
    MISSING_DATA,
    VARIANCE
}
