package com.medkernel.engine.knowledge;

/**
 * 知识身份列表筛选条件。所有字段都是可空（null 表示不过滤）。
 *
 * @param domain      领域分类
 * @param specialtyId 专科 ID
 * @param status      身份状态
 * @param keyword     关键词（在 subject / identity_code 模糊匹配，大小写不敏感）
 */
public record KnowledgeIdentityFilter(
    KnowledgeDomain domain,
    String specialtyId,
    KnowledgeIdentityStatus status,
    String keyword
) {

    public static KnowledgeIdentityFilter empty() {
        return new KnowledgeIdentityFilter(null, null, null, null);
    }
}
