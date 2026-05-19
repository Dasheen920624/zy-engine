package com.medkernel.security;

import com.medkernel.common.ErrorCode;

/**
 * 认证/授权异常，携带 ErrorCode 便于 GlobalExceptionHandler 处理。
 */
public class AuthException extends RuntimeException {

    private final ErrorCode errorCode;

    public AuthException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
