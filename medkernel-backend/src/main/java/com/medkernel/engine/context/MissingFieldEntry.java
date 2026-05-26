package com.medkernel.engine.context;

/**
 * 标准上下文 schema 缺失字段条目。
 *
 * @param resourceType 资源类型（PATIENT/ENCOUNTER/...，{@code *} 表示整类缺失）
 * @param field        字段名；{@code *} 表示整体资源缺失
 * @param level        WARN（可降级）/ ERROR（业务必填）/ CRITICAL（拒绝创建）
 */
public record MissingFieldEntry(
    String resourceType,
    String field,
    String level
) {}
