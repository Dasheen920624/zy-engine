package com.medkernel.security;

import com.medkernel.common.ApiResult;
import com.medkernel.common.ErrorCode;
import com.medkernel.common.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证异常处理器，将 AuthException 转换为标准 ApiResult。
 * 使用 @Order(-1) 确保优先于 GlobalExceptionHandler。
 * HTTP 状态按 ErrorCode 动态映射（AUDIT §2.5）：
 * <ul>
 *   <li>UNAUTHORIZED / LOGIN_FAILED → 401</li>
 *   <li>FORBIDDEN → 403</li>
 *   <li>USER_LOCKED → 423 (LOCKED)</li>
 *   <li>其他 → 401（保守兜底，保持原行为）</li>
 * </ul>
 */
@RestControllerAdvice
@Order(-1)
public class SecurityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResult<Void>> handleAuthException(AuthException ex) {
        log.warn("[traceId={}] auth error: code={}, message={}",
                TraceContext.getTraceId(), ex.getErrorCode(), ex.getMessage());
        HttpStatus status = mapStatus(ex.getErrorCode());
        ApiResult<Void> body = ApiResult.failure(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    private static HttpStatus mapStatus(ErrorCode code) {
        if (code == null) {
            return HttpStatus.UNAUTHORIZED;
        }
        switch (code) {
            case FORBIDDEN:
                return HttpStatus.FORBIDDEN;
            case USER_LOCKED:
                return HttpStatus.LOCKED; // 423
            case UNAUTHORIZED:
            case LOGIN_FAILED:
            default:
                return HttpStatus.UNAUTHORIZED;
        }
    }
}
