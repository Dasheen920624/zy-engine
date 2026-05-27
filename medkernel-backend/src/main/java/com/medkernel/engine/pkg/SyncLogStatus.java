package com.medkernel.engine.pkg;

/**
 * 同步日志执行状态枚举。
 */
public enum SyncLogStatus {
    /** 运行中 */
    RUNNING,
    /** 成功 */
    SUCCESS,
    /** 失败 */
    FAILED,
    /** 重试中 */
    RETRYING
}
