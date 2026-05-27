package com.medkernel.engine.pathway;

/**
 * 患者路径推进事件类型。
 *
 * <p>用于区分节点完成、登记变异和人工退出三类受控推进动作。
 */
public enum PathwayAdvanceEventType {
    COMPLETE,
    VARIANCE,
    EXIT
}
