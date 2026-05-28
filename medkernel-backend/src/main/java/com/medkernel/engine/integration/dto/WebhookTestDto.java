package com.medkernel.engine.integration.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 外部 Webhook 签名与连通性测试 DTO Record。
 *
 * <p>用于封装在 Webhook 订阅通道调试中传递测试报文及 JSR-380 输入校验规则。
 */
public record WebhookTestDto(
    @NotBlank(message = "WebhookID 不能为空")
    String webhookId,

    @NotBlank(message = "测试 Payload 内容不能为空")
    String payload
) {}
