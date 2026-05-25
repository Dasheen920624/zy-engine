package com.medkernel.engine.knowledge;

import java.util.List;

/**
 * 知识身份 lineage 复合响应。
 *
 * <p>对应详细规范 §1797-1806 "GET /api/v1/knowledge-identities/{id}/lineage"：
 * 在一个响应中同时返回身份元数据、按时间倒序的所有版本、按时间正序的状态转换历史。
 *
 * <p>前端按 supersession 时间轴绘制时间线，节点之间链接到具体 version；
 * 关键操作（激活 / 撤回 / 还原）有 transitioned_by 留痕。
 */
public record KnowledgeLineage(
    KnowledgeIdentity identity,
    List<KnowledgeAssetVersion> versions,
    List<KnowledgeSupersession> supersessions
) {

    public KnowledgeLineage {
        versions = versions == null ? List.of() : List.copyOf(versions);
        supersessions = supersessions == null ? List.of() : List.copyOf(supersessions);
    }
}
