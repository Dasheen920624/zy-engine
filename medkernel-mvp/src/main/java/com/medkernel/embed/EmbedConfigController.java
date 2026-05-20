package com.medkernel.embed;

import com.medkernel.common.ApiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 嵌入器配置和告警管理
 */
@RestController
@RequestMapping("/api/embed")
public class EmbedConfigController {

    private final ClinicalEventBus eventBus;

    @Value("${embed.ws.path:/ws/embed/alerts}")
    private String wsPath;

    @Value("${embed.reconnect.interval-ms:5000}")
    private int reconnectIntervalMs;

    @Value("${embed.max-alerts:10}")
    private int maxAlerts;

    @Value("${embed.auto-dismiss.success-ms:5000}")
    private int autoDismissSuccessMs;

    public EmbedConfigController(ClinicalEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * 获取嵌入器配置
     */
    @GetMapping("/config")
    public ApiResult<Map<String, Object>> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("ws_url", wsPath);
        config.put("reconnect_interval_ms", reconnectIntervalMs);
        config.put("max_alerts", maxAlerts);
        config.put("auto_dismiss_success_ms", autoDismissSuccessMs);
        return ApiResult.success(config);
    }

    /**
     * 获取历史告警（当前返回空列表，后续对接持久化）
     */
    @GetMapping("/alerts")
    public ApiResult<List<Map<String, Object>>> getAlerts(
            String patientId,
            String encounterId,
            Integer limit) {
        // TODO: 对接告警持久化存储
        return ApiResult.success(Collections.<Map<String, Object>>emptyList());
    }

    /**
     * 执行告警动作（如一键入径）
     */
    @GetMapping("/alerts/{alertId}/action")
    public ApiResult<Void> executeAction(String alertId, String actionType) {
        // TODO: 实现动作执行逻辑（入径、查看证据等）
        return ApiResult.success(null);
    }

    /**
     * 发布测试告警（开发用）
     */
    @GetMapping("/test-alert")
    public ApiResult<Map<String, Object>> publishTestAlert() {
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("alert_id", UUID.randomUUID().toString());
        alert.put("event_id", "test-" + System.currentTimeMillis());
        alert.put("severity", "info");
        alert.put("title", "🩺 AMI/STEMI 路径推荐 (HIGH)");
        alert.put("evidence", "ECG ST 段抬高 + 持续胸痛 ≥ 30min");
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("documentName", "2023 ACC/AHA AMI 指南");
        source.put("section", "§4.2");
        source.put("publishYear", 2023);
        alert.put("source", source);
        alert.put("confidence", 92);
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action1 = new LinkedHashMap<>();
        action1.put("text", "一键入径");
        action1.put("intent", "primary");
        action1.put("action_type", "ENROLL_PATHWAY");
        actions.add(action1);
        Map<String, Object> action2 = new LinkedHashMap<>();
        action2.put("text", "查看完整证据");
        action2.put("intent", "secondary");
        action2.put("action_type", "VIEW_EVIDENCE");
        actions.add(action2);
        Map<String, Object> action3 = new LinkedHashMap<>();
        action3.put("text", "暂不入径");
        action3.put("intent", "tertiary");
        action3.put("action_type", "DISMISS");
        actions.add(action3);
        alert.put("actions", actions);
        alert.put("created_at", java.time.Instant.now().toString());

        eventBus.publishAlert(alert);
        return ApiResult.success(alert);
    }
}
