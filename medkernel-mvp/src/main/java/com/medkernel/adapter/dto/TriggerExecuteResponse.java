package com.medkernel.adapter.dto;

import java.util.Map;

/**
 * 触发点执行响应 DTO。
 */
public class TriggerExecuteResponse {
    private String triggerCode;
    private String status;
    private String message;
    private Object result;
    private Long elapsedMs;

    public static TriggerExecuteResponse fromMap(Map<String, Object> map) {
        TriggerExecuteResponse dto = new TriggerExecuteResponse();
        dto.setTriggerCode(string(map.get("triggerCode")));
        dto.setStatus(string(map.get("status")));
        dto.setMessage(string(map.get("message")));
        dto.setResult(map.get("result"));
        if (map.get("elapsedMs") instanceof Number) {
            dto.setElapsedMs(((Number) map.get("elapsedMs")).longValue());
        }
        return dto;
    }

    private static String string(Object value) {
        return value != null ? value.toString() : null;
    }

    public String getTriggerCode() { return triggerCode; }
    public void setTriggerCode(String triggerCode) { this.triggerCode = triggerCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
}
