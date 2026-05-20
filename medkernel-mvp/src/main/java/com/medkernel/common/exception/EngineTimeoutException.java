package com.medkernel.common.exception;

import com.medkernel.common.ErrorCode;

/**
 * 引擎执行超时。GlobalExceptionHandler → 504 ENGINE_TIMEOUT。
 */
public class EngineTimeoutException extends BusinessException {

    public EngineTimeoutException(String message) {
        super(ErrorCode.ENGINE_TIMEOUT, message);
    }

    public EngineTimeoutException(String message, Throwable cause) {
        super(ErrorCode.ENGINE_TIMEOUT, message, cause);
    }
}
