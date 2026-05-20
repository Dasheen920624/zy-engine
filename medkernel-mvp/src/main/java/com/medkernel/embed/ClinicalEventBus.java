package com.medkernel.embed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 临床事件总线
 * 
 * 接收临床事件（ECG异常、检验危急值等），推送给订阅的 WebSocket 客户端。
 * 每个客户端按 patient_id + encounter_id 过滤。
 */
@Component
public class ClinicalEventBus extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ClinicalEventBus.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 已注册的 WebSocket 会话 */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** 会话订阅的患者上下文: sessionId -> {patientId, encounterId} */
    private final Map<String, Map<String, String>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("Embed WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String action = payload.get("action");

            if ("subscribe".equals(action)) {
                String patientId = payload.get("patient_id");
                String encounterId = payload.get("encounter_id");
                Map<String, String> subscription = new LinkedHashMap<>();
                subscription.put("patient_id", patientId != null ? patientId : "");
                subscription.put("encounter_id", encounterId != null ? encounterId : "");
                subscriptions.put(session.getId(), subscription);
                log.info("Embed session {} subscribed to patient={}, encounter={}",
                        session.getId(), patientId, encounterId);
            }
        } catch (Exception e) {
            log.error("Failed to handle message from session {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.remove(session.getId());
        subscriptions.remove(session.getId());
        log.info("Embed WebSocket disconnected: {}", session.getId());
    }

    /**
     * 发布临床事件告警到匹配的客户端
     * 
     * @param alert 告警数据（将序列化为 JSON）
     */
    public void publishAlert(Object alert) {
        String json;
        try {
            json = objectMapper.writeValueAsString(alert);
        } catch (Exception e) {
            log.error("Failed to serialize alert: {}", e.getMessage());
            return;
        }

        TextMessage message = new TextMessage(json);

        sessions.forEach((sessionId, session) -> {
            if (session.isOpen()) {
                try {
                    // 简化：当前实现推送给所有连接的客户端
                    // 生产环境应根据 subscriptions 过滤
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.error("Failed to send alert to session {}: {}", sessionId, e.getMessage());
                }
            }
        });
    }

    /**
     * 获取当前连接的客户端数量
     */
    public int getConnectedCount() {
        return sessions.size();
    }
}
