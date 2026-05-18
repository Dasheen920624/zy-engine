package com.medkernel.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("[traceId={}] validation error: {}", TraceContext.getTraceId(), ex.getMessage());
        return ApiResult.failure(ErrorCode.VALIDATION_ERROR, summarize(ex.getMessage(), 200));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBadRequest(Exception ex) {
        log.warn("[traceId={}] bad request: {}", TraceContext.getTraceId(), ex.getMessage());
        return ApiResult.failure(ErrorCode.VALIDATION_ERROR, summarize(ex.getMessage(), 200));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[traceId={}] illegal argument: {}", TraceContext.getTraceId(), ex.getMessage());
        return ApiResult.failure(ErrorCode.VALIDATION_ERROR, summarize(ex.getMessage(), 200));
    }

    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleDbError(SQLException ex) {
        log.error("[traceId={}] database error", TraceContext.getTraceId(), ex);
        // 数据库内部错误不向客户端泄漏 SQL/堆栈，保持稳定的对外语义。
        return ApiResult.failure(ErrorCode.DB_ERROR, ErrorCode.DB_ERROR.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleIllegalState(IllegalStateException ex) {
        log.error("[traceId={}] illegal state: {}", TraceContext.getTraceId(), ex.getMessage(), ex);
        return ApiResult.failure(ErrorCode.UNKNOWN_ERROR, summarize(ex.getMessage(), 200));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception ex) {
        log.error("[traceId={}] unhandled error", TraceContext.getTraceId(), ex);
        // 兜底异常只回显类型与脱敏摘要，避免堆栈/内部路径泄漏。
        return ApiResult.failure(ErrorCode.UNKNOWN_ERROR, ErrorCode.UNKNOWN_ERROR.getMessage());
    }

    private static String summarize(String message, int max) {
        if (message == null) {
            return null;
        }
        String trimmed = message.replaceAll("\\s+", " ").trim();
        return trimmed.length() > max ? trimmed.substring(0, max) + "..." : trimmed;
    }
}
