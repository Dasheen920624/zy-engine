package com.medkernel.engine.evaluation;

/**
 * 评估运行类型枚举。
 *
 * <p>取值含义：{@code MANUAL_SAMPLE} 人工抽检、{@code UPSTREAM_RESULT} 上游结果、
 * {@code BATCH_IMPORT} 批量导入。
 */
public enum EvaluationRunType {
    MANUAL_SAMPLE,
    UPSTREAM_RESULT,
    BATCH_IMPORT
}
