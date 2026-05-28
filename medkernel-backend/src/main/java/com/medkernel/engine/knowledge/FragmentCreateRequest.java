package com.medkernel.engine.knowledge;

/**
 * 引用锚点片段创建请求。
 *
 * @param sourceVersionId 来源文献版本 ID
 * @param anchorPath 层级路径，同一版本下唯一（如 section-3.2.1）
 * @param anchorLabel 锚点标签/标题
 * @param textExcerpt 物理文本片段
 */
public record FragmentCreateRequest(
    Long sourceVersionId,
    String anchorPath,
    String anchorLabel,
    String textExcerpt
) {
}
