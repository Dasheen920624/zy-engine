package com.medkernel.common.exception;

import com.medkernel.common.ErrorCode;

/**
 * 业务异常基类。
 *
 * <p>所有可恢复或可对客户端透出 message 的业务异常应继承本类，由
 * {@link com.medkernel.common.GlobalExceptionHandler} 按 {@link ErrorCode} 统一转 ApiResult。
 *
 * <p>不允许直接 throw {@link RuntimeException} 兜底；详见 06_后端开发规范 §5.1。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
