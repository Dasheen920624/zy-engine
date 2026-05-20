package com.medkernel.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 统一 API 响应信封。
 *
 * <p>字段契约（与前端 api/types.ts ApiResult 对齐）：
 * <ul>
 *   <li>success — 是否成功</li>
 *   <li>code — 错误码（"0" 表示成功）</li>
 *   <li>message — 人类可读消息</li>
 *   <li>message_key — i18n 消息键，前端可据此查找本地化文案</li>
 *   <li>trace_id — 链路追踪 ID</li>
 *   <li>data — 业务数据</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {
    private boolean success;
    private String code;
    private String message;
    /**
     * i18n 消息键。前端可据此查找本地化文案，避免硬编码中文。
     * 仅在失败响应中出现，成功时为 null（@JsonInclude NON_NULL 自动隐藏）。
     */
    @JsonProperty("message_key")
    private String messageKey;
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
        return failure(errorCode, message, null);
    }

    /**
     * 返回 404 资源未找到响应。
     */
    public static <T> ApiResult<T> notFound(String message) {
        return failure(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public static <T> ApiResult<T> failure(ErrorCode errorCode, String message, String messageKey) {
        ApiResult<T> result = new ApiResult<T>();
        result.success = false;
        result.code = errorCode.getCode();
        result.message = message == null ? errorCode.getMessage() : message;
        result.messageKey = messageKey == null ? errorCode.getMessageKey() : messageKey;
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

    @JsonProperty("message_key")
    public String getMessageKey() {
        return messageKey;
    }

    @JsonProperty("message_key")
    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
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

