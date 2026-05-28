package com.medkernel.engine.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 外部 Webhook 订阅配置创建 DTO Record。
 *
 * <p>用于封装外部第三方系统订阅 Webhook 回调时的业务字段及 JSR-380 输入校验规则。
 */
public record WebhookCreateDto(
    @NotBlank(message = "WebhookID不能为空")
    String webhookId,

    @NotBlank(message = "订阅名称不能为空")
    String name,

    @NotBlank(message = "回调地址不能为空")
    @Pattern(regexp = "^https?://.*$", message = "回调地址必须是合法的 HTTP 或 HTTPS 地址")
    String callbackUrl,

    @NotBlank(message = "订阅场景事件列表不能为空")
    String eventsSubscribed
) {}
