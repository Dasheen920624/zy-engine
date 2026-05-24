package com.medkernel.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 字段级错误明细，用于 Bean Validation 校验失败时回传给客户端定位问题字段。
 *
 * @param field   出错的字段路径（点号表示嵌套，如 "user.email"）
 * @param code    错误类型（如 "NotBlank"、"Size"，或业务错误码）
 * @param message 人类可读的错误描述
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String field,
    String code,
    String message
) {
}
