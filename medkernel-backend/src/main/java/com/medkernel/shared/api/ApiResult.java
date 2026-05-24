package com.medkernel.shared.api;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.medkernel.shared.api.error.ErrorCode;
import com.medkernel.shared.context.RequestContext;

/**
 * MedKernel v1.0 GA · GA-ENG-BASE-03 标准 API 响应包络。
 *
 * <p>所有 REST 端点的返回值都必须使用本类型包装；裸 POJO / Map 在 PR 检查阶段视作违反产品宪法第 7 条。
 *
 * <p>JSON 形态（成功）：
 * <pre>{@code
 * {
 *   "success": true,
 *   "code": "OK",
 *   "message": "操作成功",
 *   "data": { ... },
 *   "traceId": "8c4e1e2f-...",
 *   "timestamp": "2026-05-25T10:23:45.123Z"
 * }
 * }</pre>
 *
 * <p>JSON 形态（失败 / 含字段校验）：
 * <pre>{@code
 * {
 *   "success": false,
 *   "code": "ENG-API-002",
 *   "message": "请求参数校验失败",
 *   "errors": [
 *     { "field": "patientMpi", "code": "NotBlank", "message": "患者主索引必填" }
 *   ],
 *   "traceId": "8c4e1e2f-...",
 *   "timestamp": "2026-05-25T10:23:45.123Z"
 * }
 * }</pre>
 *
 * @param <T> 业务数据类型；失败时为 null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResult<T>(
    boolean success,
    String code,
    String message,
    T data,
    List<ApiError> errors,
    String traceId,
    Instant timestamp
) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, ErrorCode.OK.code(), ErrorCode.OK.defaultMessage(),
            data, null, RequestContext.currentTraceId(), Instant.now());
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return new ApiResult<>(true, ErrorCode.OK.code(), message,
            data, null, RequestContext.currentTraceId(), Instant.now());
    }

    public static <T> ApiResult<T> empty() {
        return ok(null);
    }

    public static <T> ApiResult<T> error(ErrorCode code, String message) {
        return new ApiResult<>(false, code.code(), message,
            null, null, RequestContext.currentTraceId(), Instant.now());
    }

    public static <T> ApiResult<T> error(ErrorCode code, String message, List<ApiError> errors) {
        return new ApiResult<>(false, code.code(), message,
            null, errors == null || errors.isEmpty() ? null : List.copyOf(errors),
            RequestContext.currentTraceId(), Instant.now());
    }

    public static <T> ApiResult<T> error(String code, String message) {
        return new ApiResult<>(false, code, message,
            null, null, RequestContext.currentTraceId(), Instant.now());
    }
}
