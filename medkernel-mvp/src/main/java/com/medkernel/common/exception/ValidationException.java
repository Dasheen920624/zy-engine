package com.medkernel.common.exception;

import com.medkernel.common.ErrorCode;

/**
 * 参数/组织/hash 校验失败。GlobalExceptionHandler → 400 VALIDATION_ERROR。
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(ErrorCode.VALIDATION_ERROR, message, cause);
    }
}
