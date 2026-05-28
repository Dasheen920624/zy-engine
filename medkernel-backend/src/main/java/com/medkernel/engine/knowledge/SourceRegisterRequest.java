package com.medkernel.engine.knowledge;

/**
 * 来源文献注册请求。
 *
 * @param sourceCode 来源代码，在租户下唯一
 * @param sourceType 来源类型
 * @param authorityLevel 权威级别
 * @param title 标题
 * @param publisher 发布者
 * @param license 许可证
 * @param language 语言，默认 zh-CN
 */
public record SourceRegisterRequest(
    String sourceCode,
    SourceType sourceType,
    SourceAuthorityLevel authorityLevel,
    String title,
    String publisher,
    String license,
    String language
) {
}
