package com.medkernel.engine.knowledge;

import java.time.Instant;

/**
 * 来源文献版本注册请求。
 *
 * @param sourceDocumentId 来源文献 ID
 * @param versionNo 版本号
 * @param publishedAt 发布时间
 * @param contentHash 内容哈希值
 * @param fileUri 文件统一资源标识符
 * @param language 语言，默认 zh-CN
 */
public record SourceVersionRegisterRequest(
    Long sourceDocumentId,
    String versionNo,
    Instant publishedAt,
    String contentHash,
    String fileUri,
    String language
) {
}
