package com.medkernel.common;

import com.medkernel.common.exception.AdapterTimeoutException;
import com.medkernel.common.exception.ConfigNotFoundException;
import com.medkernel.common.exception.DataMissingException;
import com.medkernel.common.exception.DifyTimeoutException;
import com.medkernel.common.exception.EngineTimeoutException;
import com.medkernel.common.exception.ValidationException;
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

/**
 * 全局异常处理。错误码 → HTTP 状态映射严格对齐 06_后端开发规范 §5.2 表格。
 *
 * <p>顺序：先匹配业务异常（{@link ValidationException} 等），再匹配 Spring 框架异常，
 * 最后兜底 {@link Exception}。{@link com.medkernel.security.SecurityExceptionHandler}
 * 通过 {@code @Order(-1)} 在本处理器之前接管 {@link com.medkernel.security.AuthException}。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 业务异常（6 类，与 ErrorCode 对齐） ─────────────────────────────

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBusinessValidation(ValidationException ex) {
        log.warn("[traceId={}] business validation: {}", TraceContext.getTraceId(), ex.getMessage());
        return ApiResult.failure(ex.getErrorCode(), summarize(ex.getMessage(), 200));
    }

    @ExceptionHandler(ConfigNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResult<Void> handleConfigNotFound(ConfigNotFoundException ex) {
        log.warn("[traceId={}] config not found: {}", TraceContext.getTraceId(), ex.getMessage());
        return ApiResult.failure(ex.getErrorCode(), summarize(ex.getMessage(), 200));
    }

    @ExceptionHandler(DataMissingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleDataMissing(DataMissingException ex) {
        log.warn("[traceId={}] data missing: {}", TraceContext.getTraceId(), ex.getMessage());
        return ApiResult.failure(ex.getErrorCode(), summarize(ex.getMessage(), 200));
    }

    @ExceptionHandler(EngineTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ApiResult<Void> handleEngineTimeout(EngineTimeoutException ex) {
        log.warn("[traceId={}] engine timeout: {}", TraceContext.getTraceId(), ex.getMessage());
        return ApiResult.failure(ex.getErrorCode(), summarize(ex.getMessage(), 200));
    }

    @ExceptionHandler(AdapterTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ApiResult<Void> handleAdapterTimeout(AdapterTimeoutException ex) {
        log.warn("[traceId={}] adapter timeout: {}", TraceContext.getTraceId(), ex.getMessage());
        return ApiResult.failure(ex.getErrorCode(), summarize(ex.getMessage(), 200));
    }

    @ExceptionHandler(DifyTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ApiResult<Void> handleDifyTimeout(DifyTimeoutException ex) {
        log.warn("[traceId={}] dify timeout: {}", TraceContext.getTraceId(), ex.getMessage());
        return ApiResult.failure(ex.getErrorCode(), summarize(ex.getMessage(), 200));
    }

    // ─── Spring 框架异常 ───────────────────────────────────────────────

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
