package com.medkernel.common.exception;

import com.medkernel.common.ErrorCode;

/**
 * 第三方适配器超时。GlobalExceptionHandler → 504 ADAPTER_TIMEOUT。
 */
public class AdapterTimeoutException extends BusinessException {

    public AdapterTimeoutException(String message) {
        super(ErrorCode.ADAPTER_TIMEOUT, message);
    }

    public AdapterTimeoutException(String message, Throwable cause) {
        super(ErrorCode.ADAPTER_TIMEOUT, message, cause);
    }
}
