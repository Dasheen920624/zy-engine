package com.medkernel.engine.knowledge;

/**
 * 待审版本草稿创建请求。
 *
 * @param identityId 知识身份 ID
 * @param versionNo 版本号
 * @param versionLabel 版本便签
 * @param sourceDocumentId 来源文献 ID
 * @param sourceVersionId 来源文献版本 ID
 * @param content 知识内容明文，用于计算 SHA-256 哈希去重防重
 * @param anchors 引用锚点对应关系 JSON
 * @param riskLevel 风险级别
 */
public record DraftVersionCreateRequest(
    Long identityId,
    String versionNo,
    String versionLabel,
    Long sourceDocumentId,
    Long sourceVersionId,
    String content,
    String anchors,
    KnowledgeRiskLevel riskLevel
) {
}
