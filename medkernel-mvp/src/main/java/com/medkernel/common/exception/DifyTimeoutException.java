package com.medkernel.common.exception;

import com.medkernel.common.ErrorCode;

/**
 * Dify 工作流超时（含降级链路）。GlobalExceptionHandler → 504 DIFY_TIMEOUT。
 */
public class DifyTimeoutException extends BusinessException {

    public DifyTimeoutException(String message) {
        super(ErrorCode.DIFY_TIMEOUT, message);
    }

    public DifyTimeoutException(String message, Throwable cause) {
        super(ErrorCode.DIFY_TIMEOUT, message, cause);
    }
}
