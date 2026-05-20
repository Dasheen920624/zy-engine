package com.medkernel.embed;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 * 注册 /ws/embed/alerts 端点
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ClinicalEventBus clinicalEventBus;

    public WebSocketConfig(ClinicalEventBus clinicalEventBus) {
        this.clinicalEventBus = clinicalEventBus;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(clinicalEventBus, "/ws/embed/alerts")
                .setAllowedOrigins("*"); // 生产环境应限制来源
    }
}
