package com.medkernel.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResult<T> {
    private boolean success;
    private String code;
    private String message;
    /**
     * 链路追踪 ID。即便没有启用全局 SNAKE_CASE 也固定以 trace_id 字段出现，
     * 与前端 api/types.ts ApiResult.trace_id 契约对齐（AUDIT §1.4）。
     */
    @JsonProperty("trace_id")
    private String traceId;
    private T data;

    public static <T> ApiResult<T> success(T data) {
        ApiResult<T> result = new ApiResult<T>();
        result.success = true;
        result.code = ErrorCode.SUCCESS.getCode();
        result.message = "success";
        result.traceId = TraceContext.getTraceId();
        result.data = data;
        return result;
    }

    public static <T> ApiResult<T> failure(ErrorCode errorCode, String message) {
        ApiResult<T> result = new ApiResult<T>();
        result.success = false;
        result.code = errorCode.getCode();
        result.message = message == null ? errorCode.getMessage() : message;
        result.traceId = TraceContext.getTraceId();
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("trace_id")
    public String getTraceId() {
        return traceId;
    }

    @JsonProperty("trace_id")
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

