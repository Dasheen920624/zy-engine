package com.medkernel.advanced.llm;

import java.util.List;

/**
 * GA-EXT-14 · AI 决策可解释性（NMPA 三类证 + 医生信任 + 生成式 AI 管理办法 §7）。
 *
 * <p>对应"每条 CDSS 提醒展示置信度 + 知识来源 + 训练数据范围"。
 */
public record LlmExplain(
    String decisionId,
    String shortAnswer,
    Double confidence,            // 0~1
    String confidenceBand,        // 低 / 中 / 高
    List<EvidenceSource> sources,
    String trainingDataRange,
    String aiModel,
    String warning
) {
    public record EvidenceSource(
        String type,              // guideline / paper / kb / rule
        String title,
        String anchor,
        String publishedAt
    ) {}
}
