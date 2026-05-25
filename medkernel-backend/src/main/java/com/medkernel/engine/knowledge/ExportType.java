package com.medkernel.engine.knowledge;

/**
 * 知识异步导出作业类型。对应 {@code knowledge_export_job.export_type} CHECK 约束。
 */
public enum ExportType {
    /** 导出知识身份列表（不含版本明细） */
    IDENTITIES,
    /** 导出特定身份的全部版本（含 hash/状态/审核人） */
    VERSIONS,
    /** 导出 lineage（身份 + 版本 + supersession 历史） */
    LINEAGE,
    /** 导出引用关系（asset_version × source_fragment） */
    CITATIONS,
    /** 全租户快照（合规 / 备份用，需要 KNOWLEDGE_EXPORT 高权限） */
    FULL_TENANT
}
