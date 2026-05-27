package com.medkernel.engine.pathway;

/**
 * 患者路径运行状态。
 *
 * <p>覆盖已入径、节点执行中、变异待处理、已完成和已退出五种运行事实。
 */
public enum PatientPathwayStatus {
    ENTERED,
    NODE_EXECUTING,
    VARIANCE,
    COMPLETED,
    EXITED
}
