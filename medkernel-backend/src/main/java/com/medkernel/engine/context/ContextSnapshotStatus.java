package com.medkernel.engine.context;

/**
 * 标准临床上下文 snapshot 状态机。
 *
 * <ul>
 *   <li>{@code DRAFT}：尚未生效（保留给未来 B1/B2 模型增强的 candidate 流程使用）</li>
 *   <li>{@code ACTIVE}：当前权威生效，可被引擎链路读取</li>
 *   <li>{@code SUPERSEDED}：已被新 snapshot 替代，仅供历史重放</li>
 *   <li>{@code REJECTED}：quality_status=INVALID 或人工驳回</li>
 * </ul>
 */
public enum ContextSnapshotStatus {
    DRAFT,
    ACTIVE,
    SUPERSEDED,
    REJECTED
}
