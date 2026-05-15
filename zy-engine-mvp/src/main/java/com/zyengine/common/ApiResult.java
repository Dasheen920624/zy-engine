package com.zyengine.common;

public class ApiResult<T> {
    private boolean success;
    private String code;
    private String message;
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

    public String getTraceId() {
        return traceId;
    }

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

