package com.medkernel.security;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.common.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证异常处理器，将 AuthException 转换为标准 ApiResult。
 * 使用 @Order(-1) 确保优先于 GlobalExceptionHandler。
 */
@RestControllerAdvice
@Order(-1)
public class SecurityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResult<Void> handleAuthException(AuthException ex) {
        log.warn("[traceId={}] auth error: {}", TraceContext.getTraceId(), ex.getMessage());
        return ApiResult.failure(ex.getErrorCode(), ex.getMessage());
    }
}
