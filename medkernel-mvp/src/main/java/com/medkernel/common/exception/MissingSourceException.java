package com.medkernel.common.exception;

import com.medkernel.common.ErrorCode;

/**
 * 医学、医保、质控资产缺少有效来源绑定。
 * GlobalExceptionHandler -> 400 MISSING_SOURCE。
 */
public class MissingSourceException extends BusinessException {

    public MissingSourceException(String message) {
        super(ErrorCode.MISSING_SOURCE, message);
    }

    public MissingSourceException(String message, Throwable cause) {
        super(ErrorCode.MISSING_SOURCE, message, cause);
    }
}
