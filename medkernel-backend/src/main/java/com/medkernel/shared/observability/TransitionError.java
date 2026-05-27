package com.medkernel.shared.observability;

import java.time.Instant;

import com.medkernel.shared.api.error.ErrorCode.ErrorClass;

/**
 * 状态机跳转失败时的结构化错误。
 *
 * @param errorCode    {@link com.medkernel.shared.api.error.ErrorCode#code()}
 * @param errorClass   {@link ErrorClass#name()}
 * @param message      错误摘要 ≤ 512 字符
 * @param retryCount   当前重试次数
 * @param nextRetryAt  下次重试时间（仅 retryable 错误）
 */
public record TransitionError(
    String errorCode,
    String errorClass,
    String message,
    Integer retryCount,
    Instant nextRetryAt
) {

    public static TransitionError of(String errorCode, ErrorClass errorClass,
                                     String message, Integer retryCount, Instant nextRetryAt) {
        return new TransitionError(errorCode, errorClass.name(), truncate(message), retryCount, nextRetryAt);
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 512 ? s : s.substring(0, 512);
    }
}
